package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.VoiceShortcutEntity
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.Locale

private const val TAG = "VoiceAssistant"

sealed interface VoiceAction {
    data class AddInvoiceItem(
        val productName: String,
        val quantity: Double,
        val unitPrice: Double,
        val note: String = ""
    ) : VoiceAction

    data class LogExpense(
        val amount: Double,
        val description: String
    ) : VoiceAction

    data class LogDue(
        val contactName: String,
        val amount: Double,
        val isCustomer: Boolean = true
    ) : VoiceAction

    data class Informational(val message: String) : VoiceAction
    data class Error(val message: String) : VoiceAction
}

sealed interface VoiceState {
    data object Idle : VoiceState
    data object Listening : VoiceState
    data class Processing(val spokenText: String) : VoiceState
    data class Executed(val text: String, val action: VoiceAction) : VoiceState
    data class Error(val message: String) : VoiceState
}

class VoiceAssistantEngine(
    private val context: Context,
    private val database: AppDatabase,
    private val scope: CoroutineScope
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    // Lazy initialization of Gemini 3.8 Flash with structured schema
    private val geminiModel by lazy {
        val actionSchema = Schema.obj(
            properties = mapOf(
                "actionType" to Schema.enumeration(
                    values = listOf("ADD_ITEM", "LOG_EXPENSE", "LOG_DUE", "UNKNOWN"),
                    description = "The detected action type"
                ),
                "productName" to Schema.string(description = "Product name if adding item", nullable = true),
                "quantity" to Schema.double(description = "Quantity of item", nullable = true),
                "unitPrice" to Schema.double(description = "Unit price or total amount", nullable = true),
                "contactName" to Schema.string(description = "Customer or supplier name for dues", nullable = true),
                "amount" to Schema.double(description = "Money amount for expenses or dues", nullable = true),
                "description" to Schema.string(description = "Expense or transaction description", nullable = true)
            ),
            optionalProperties = listOf("productName", "quantity", "unitPrice", "contactName", "amount", "description")
        )

        val config = generationConfig {
            responseMimeType = "application/json"
            responseSchema = actionSchema
        }

        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = config
        )
    }

    fun startListening(languageLocale: String = "bn-BD") {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _voiceState.value = VoiceState.Error("Speech recognition not available on this device")
            return
        }

        stopListening()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _voiceState.value = VoiceState.Listening
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        _voiceState.value = VoiceState.Processing("Processing speech...")
                    }

                    override fun onError(error: Int) {
                        val msg = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please try again."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timed out."
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                            else -> "Voice input error ($error)"
                        }
                        _voiceState.value = VoiceState.Error(msg)
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spoken = matches?.firstOrNull() ?: ""
                        if (spoken.isNotBlank()) {
                            processSpokenCommand(spoken)
                        } else {
                            _voiceState.value = VoiceState.Idle
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageLocale)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageLocale)
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _voiceState.value = VoiceState.Error("Failed to start voice listener: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recognizer", e)
        }
    }

    fun resetState() {
        _voiceState.value = VoiceState.Idle
    }

    fun processSpokenCommand(rawSpoken: String) {
        _voiceState.value = VoiceState.Processing(rawSpoken)
        scope.launch {
            val normalized = normalizeBengaliNumerals(rawSpoken.trim())
            // 1. Try Instant Local Rule / Shortcut Matcher (Zero latency & works 100% offline)
            val localAction = tryLocalMatcher(normalized)
            if (localAction != null) {
                _voiceState.value = VoiceState.Executed(rawSpoken, localAction)
                return@launch
            }

            // 2. Try Gemini AI Logic for natural conversational speech
            try {
                val aiAction = tryGeminiNlu(normalized)
                if (aiAction != null) {
                    _voiceState.value = VoiceState.Executed(rawSpoken, aiAction)
                    return@launch
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gemini NLU parsing bypassed: ${e.message}")
            }

            // Fallback
            _voiceState.value = VoiceState.Executed(
                rawSpoken,
                VoiceAction.Informational("Heard: \"$rawSpoken\". No matching shortcut found.")
            )
        }
    }

    private suspend fun tryLocalMatcher(text: String): VoiceAction? = withContext(Dispatchers.IO) {
        val lower = text.lowercase(Locale.getDefault())
        val tokens = lower.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return@withContext null

        // Check if begins with custom shortcut (e.g. "star 100", "star", "sugar 5")
        val shortcuts = database.voiceShortcutDao().getAllShortcuts()
        val allShortcuts = database.voiceShortcutDao().findByTrigger(tokens[0])
            ?: (if (tokens.size >= 2) database.voiceShortcutDao().findByTrigger("${tokens[0]} ${tokens[1]}") else null)

        if (allShortcuts != null) {
            // Find numeric quantity in subsequent tokens or use default
            val qtyToken = tokens.drop(1).firstOrNull { it.matches(Regex("\\d+(\\.\\d+)?")) }
            val quantity = qtyToken?.toDoubleOrNull() ?: allShortcuts.defaultQuantity

            // Look up product to get selling price if defaultPrice is 0
            val product = database.productDao().searchProducts(allShortcuts.targetProductName)
            val price = if (allShortcuts.defaultPrice > 0) allShortcuts.defaultPrice else 0.0

            return@withContext VoiceAction.AddInvoiceItem(
                productName = allShortcuts.targetProductName,
                quantity = quantity,
                unitPrice = price,
                note = "Voice Shortcut: ${allShortcuts.triggerWord}"
            )
        }

        // Expense pattern: "Add this expense 250", "Expense 250 rent", "খরচ ২৫০ টাকা", "২৫০ টাকা খরচ"
        if (lower.contains("expense") || lower.contains("খরচ")) {
            val amount = extractFirstNumber(lower) ?: 0.0
            val desc = lower.replace("add", "").replace("this", "").replace("expense", "")
                .replace("টাকা", "").replace("খরচ", "").replace(amount.toInt().toString(), "").trim()
            return@withContext VoiceAction.LogExpense(
                amount = if (amount > 0) amount else 100.0,
                description = if (desc.isNotBlank()) desc.replaceFirstChar { it.uppercase() } else "Shop Expense"
            )
        }

        // Due / Credit pattern: "Karim 500 baki", "বাকি ৫০০", "due 500"
        if (lower.contains("baki") || lower.contains("বাকি") || lower.contains("due")) {
            val amount = extractFirstNumber(lower) ?: 0.0
            val name = lower.replace("baki", "").replace("বাকি", "").replace("due", "")
                .replace("টাকা", "").replace(amount.toInt().toString(), "").trim()
            return@withContext VoiceAction.LogDue(
                contactName = if (name.isNotBlank()) name.replaceFirstChar { it.uppercase() } else "Customer",
                amount = if (amount > 0) amount else 500.0,
                isCustomer = true
            )
        }

        null
    }

    private suspend fun tryGeminiNlu(text: String): VoiceAction? = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                You are a voice assistant for a retail grocery store in Bangladesh.
                Parse the merchant's spoken command into a structured action:
                Spoken text: "$text"
                
                Identify if it means:
                - ADD_ITEM: adding products to invoice with productName, quantity, and unitPrice.
                - LOG_EXPENSE: recording an expense with amount and description.
                - LOG_DUE: customer or supplier taking goods on credit or loan with contactName and amount.
                - UNKNOWN: not understood.
            """.trimIndent()

            val response = withTimeout(10_000L) {
                geminiModel.generateContent(prompt)
            }
            val jsonStr = response.text ?: return@withContext null
            val json = JSONObject(jsonStr)
            val actionType = json.optString("actionType", "UNKNOWN")

            when (actionType) {
                "ADD_ITEM" -> {
                    val name = json.optString("productName", "").ifBlank { "Item" }
                    val qty = json.optDouble("quantity", 1.0)
                    val price = json.optDouble("unitPrice", 0.0)
                    VoiceAction.AddInvoiceItem(productName = name, quantity = qty, unitPrice = price)
                }
                "LOG_EXPENSE" -> {
                    val amount = json.optDouble("amount", 0.0)
                    val desc = json.optString("description", "Daily Expense")
                    VoiceAction.LogExpense(amount = amount, description = desc)
                }
                "LOG_DUE" -> {
                    val contact = json.optString("contactName", "Customer")
                    val amount = json.optDouble("amount", 0.0)
                    VoiceAction.LogDue(contactName = contact, amount = amount)
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini NLU error", e)
            null
        }
    }

    private fun extractFirstNumber(text: String): Double? {
        val match = Regex("(\\d+(\\.\\d+)?)").find(text)
        return match?.value?.toDoubleOrNull()
    }

    private fun normalizeBengaliNumerals(input: String): String {
        val bnToEn = mapOf(
            '০' to '0', '১' to '1', '২' to '2', '৩' to '3', '৪' to '4',
            '৫' to '5', '৬' to '6', '৭' to '7', '৮' to '8', '৯' to '9'
        )
        val sb = StringBuilder()
        for (ch in input) {
            sb.append(bnToEn[ch] ?: ch)
        }
        return sb.toString()
    }
}
