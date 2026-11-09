package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.VoiceShortcutEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.DueRed
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.voice.VoiceAction
import com.example.voice.VoiceState

@Composable
fun VoiceShortcutsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val shortcuts by viewModel.voiceShortcuts.collectAsStateWithLifecycle()
    val voiceState by viewModel.voiceEngine.voiceState.collectAsStateWithLifecycle()

    var testInputText by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header Card with Mic tester
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldGreenDark),
                modifier = Modifier.fillMaxWidth().testTag("voice_console_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Bilingual Voice Assistant",
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Bangla & English speech supported (fast or slow)",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }

                        FloatingActionButton(
                            onClick = {
                                if (voiceState is VoiceState.Listening) {
                                    viewModel.voiceEngine.stopListening()
                                } else {
                                    viewModel.voiceEngine.startListening("bn-BD")
                                }
                            },
                            shape = CircleShape,
                            containerColor = if (voiceState is VoiceState.Listening) DueRed else Color.White,
                            contentColor = if (voiceState is VoiceState.Listening) Color.White else EmeraldGreenPrimary,
                            modifier = Modifier.size(52.dp).testTag("shortcut_mic_fab")
                        ) {
                            if (voiceState is VoiceState.Listening) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop")
                            } else {
                                Icon(Icons.Default.Mic, contentDescription = "Mic")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live Status Box
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            val status = when (voiceState) {
                                is VoiceState.Listening -> "🔴 Listening... Speak now in English or বাংলা!"
                                is VoiceState.Processing -> "⏳ Processing: ${(voiceState as VoiceState.Processing).spokenText}"
                                is VoiceState.Executed -> "✅ Action: ${(voiceState as VoiceState.Executed).text}"
                                is VoiceState.Error -> "⚠️ ${(voiceState as VoiceState.Error).message}"
                                else -> "Tap microphone or choose sample below to test commands"
                            }
                            Text(status, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                            if (voiceState is VoiceState.Executed) {
                                val action = (voiceState as VoiceState.Executed).action
                                val detail = when (action) {
                                    is VoiceAction.AddInvoiceItem -> "Added ${action.quantity}x ${action.productName} to current bill"
                                    is VoiceAction.LogExpense -> "Logged expense of ৳${action.amount} (${action.description})"
                                    is VoiceAction.LogDue -> "Recorded due of ৳${action.amount} for ${action.contactName}"
                                    is VoiceAction.Informational -> action.message
                                    is VoiceAction.Error -> action.message
                                }
                                Text("Result: $detail", color = Color(0xFFA7F3D0), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sample Quick Test Chips
            Text(
                "Try saying (বা ট্যাপ করে টেস্ট করুন):",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            val samplePhrases = listOf(
                "star 100",
                "Add this expense 250",
                "করিম ৫০০ টাকা বাকি",
                "sugar 2",
                "soybean 1",
                "দোকান খরচ ১০০ টাকা"
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(samplePhrases) { phrase ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier
                            .clickable { viewModel.voiceEngine.processSpokenCommand(phrase) }
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "\"$phrase\"",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Text Command Simulator
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = testInputText,
                    onValueChange = { testInputText = it },
                    placeholder = { Text("Type custom voice command (e.g. star 100)...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("voice_test_input"),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (testInputText.isNotBlank()) {
                            viewModel.voiceEngine.processSpokenCommand(testInputText)
                            testInputText = ""
                        }
                    },
                    modifier = Modifier.size(48.dp).background(EmeraldGreenPrimary, CircleShape)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Custom Shortcuts List Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Configured Voice Shortcuts (${shortcuts.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Add Shortcut")
                }
            }

            // Shortcuts List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(shortcuts, key = { it.id }) { shortcut ->
                    ShortcutCard(
                        shortcut = shortcut,
                        onDelete = { viewModel.deleteVoiceShortcut(shortcut) },
                        onTest = {
                            viewModel.voiceEngine.processSpokenCommand("${shortcut.triggerWord} ${shortcut.defaultQuantity.toInt()}")
                        }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = EmeraldGreenPrimary,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).testTag("add_shortcut_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Voice Shortcut")
        }
    }

    if (showAddDialog) {
        AddShortcutDialog(
            onDismiss = { showAddDialog = false },
            onSave = { trigger, product, qty, price ->
                viewModel.saveVoiceShortcut(trigger, product, qty, price)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ShortcutCard(
    shortcut: VoiceShortcutEntity,
    onDelete: () -> Unit,
    onTest: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().testTag("shortcut_card_${shortcut.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Say: \"${shortcut.triggerWord}\"",
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("➔ ${shortcut.targetProductName}", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(4.dp))
                val priceText = if (shortcut.defaultPrice > 0) "Price: ৳${shortcut.defaultPrice}" else "Product Selling Price"
                Text(
                    text = "Default Qty: ${shortcut.defaultQuantity.toInt()} | $priceText",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onTest) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = "Test", tint = EmeraldGreenPrimary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DueRed)
                }
            }
        }
    }
}

@Composable
fun AddShortcutDialog(
    onDismiss: () -> Unit,
    onSave: (trigger: String, product: String, qty: Double, price: Double) -> Unit
) {
    var trigger by remember { mutableStateOf("") }
    var product by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("1") }
    var price by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Voice Shortcut (ভয়েস শর্টকাট)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = trigger,
                    onValueChange = { trigger = it },
                    label = { Text("Trigger Word (e.g. star, sugar, তেল) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("shortcut_trigger_input")
                )
                OutlinedTextField(
                    value = product,
                    onValueChange = { product = it },
                    label = { Text("Target Product Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("shortcut_product_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qty,
                        onValueChange = { qty = it },
                        label = { Text("Default Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Price (0=Product Price)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (trigger.isNotBlank() && product.isNotBlank()) {
                        onSave(trigger, product, qty.toDoubleOrNull() ?: 1.0, price.toDoubleOrNull() ?: 0.0)
                    }
                },
                enabled = trigger.isNotBlank() && product.isNotBlank()
            ) {
                Text("Save Shortcut")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
