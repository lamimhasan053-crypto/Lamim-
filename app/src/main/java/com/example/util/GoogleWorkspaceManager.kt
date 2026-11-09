package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import com.example.data.local.AppDatabase
import com.example.data.local.ContactEntity
import com.example.data.local.ProductEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object GoogleWorkspaceManager {

    /**
     * Parses raw document text copied from Google Docs, Keep, or Sheets
     * Supported formats:
     * 1) CSV / TSV: Name, PurchasePrice, SellingPrice, Stock, Unit
     * 2) Natural line: "Soybean Oil (1L): purchase 170, selling 185, stock 40"
     */
    suspend fun parseAndImportDocumentText(text: String, database: AppDatabase): Int = withContext(Dispatchers.IO) {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        var importedCount = 0

        for (line in lines) {
            // Check for comma or tab separated values
            val delimiter = if (line.contains("\t")) "\t" else if (line.contains(",")) "," else null
            if (delimiter != null) {
                val parts = line.split(delimiter).map { it.trim() }
                if (parts.size >= 3) {
                    val name = parts[0]
                    val purchase = parts[1].toDoubleOrNull() ?: continue
                    val selling = parts[2].toDoubleOrNull() ?: continue
                    val stock = if (parts.size >= 4) parts[3].toDoubleOrNull() ?: 10.0 else 10.0
                    val unit = if (parts.size >= 5 && parts[4].isNotBlank()) parts[4] else "pc"

                    database.productDao().insertProduct(
                        ProductEntity(
                            name = name,
                            purchasePrice = purchase,
                            sellingPrice = selling,
                            stockQty = stock,
                            unit = unit
                        )
                    )
                    importedCount++
                    continue
                }
            }

            // Natural colon or regex format: "Sugar: 130 purchase, 145 sale, 50 stock"
            if (line.contains(":") || line.contains("-")) {
                val namePart = line.substringBefore(":").substringBefore("-").trim()
                val numbers = Regex("(\\d+(\\.\\d+)?)").findAll(line).mapNotNull { it.value.toDoubleOrNull() }.toList()
                if (namePart.isNotBlank() && numbers.size >= 2) {
                    val purchase = numbers[0]
                    val selling = numbers[1]
                    val stock = if (numbers.size >= 3) numbers[2] else 10.0
                    database.productDao().insertProduct(
                        ProductEntity(
                            name = namePart,
                            purchasePrice = purchase,
                            sellingPrice = selling,
                            stockQty = stock,
                            unit = "pc"
                        )
                    )
                    importedCount++
                }
            }
        }
        importedCount
    }

    /**
     * Creates a full JSON backup of the store for Google Drive storage
     */
    suspend fun generateBackupJson(database: AppDatabase): String = withContext(Dispatchers.IO) {
        val products = database.productDao().getAllProducts().first()
        val contacts = database.contactDao().getAllContacts().first()
        val invoices = database.invoiceDao().getAllInvoices().first()
        val shortcuts = database.voiceShortcutDao().getAllShortcuts().first()
        val profile = database.shopProfileDao().getProfileSync()

        val root = JSONObject()
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())

        val productsArr = JSONArray()
        products.forEach {
            val p = JSONObject()
            p.put("name", it.name)
            p.put("nameBn", it.nameBn)
            p.put("barcode", it.barcode)
            p.put("purchasePrice", it.purchasePrice)
            p.put("sellingPrice", it.sellingPrice)
            p.put("stockQty", it.stockQty)
            p.put("unit", it.unit)
            productsArr.put(p)
        }
        root.put("products", productsArr)

        val contactsArr = JSONArray()
        contacts.forEach {
            val c = JSONObject()
            c.put("name", it.name)
            c.put("phone", it.phone)
            c.put("type", it.type)
            c.put("currentBalance", it.currentBalance)
            c.put("address", it.address)
            contactsArr.put(c)
        }
        root.put("contacts", contactsArr)

        root.toString(2)
    }

    fun exportToGoogleDrive(context: Context, backupJson: String) {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "HishabKhata_Backup_${System.currentTimeMillis()}.json")
                putExtra(Intent.EXTRA_TEXT, backupJson)
            }
            context.startActivity(Intent.createChooser(sendIntent, "Backup to Google Drive or Share"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error exporting backup: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Adds an event to Google Calendar for collecting dues
     */
    fun addDueReminderToGoogleCalendar(
        context: Context,
        contactName: String,
        amount: Double,
        dueDateMillis: Long = System.currentTimeMillis() + 86400000L * 3 // Default 3 days later
    ) {
        try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, "Collect ৳${amount.toInt()} Due from $contactName")
                putExtra(CalendarContract.Events.DESCRIPTION, "Customer due collection reminder from HishabKhata.")
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, dueDateMillis)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, dueDateMillis + 3600000L)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Calendar not available: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
