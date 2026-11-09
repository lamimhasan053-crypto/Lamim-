package com.example.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import com.example.data.local.InvoiceEntity
import com.example.data.local.ShopProfileEntity
import com.example.data.model.InvoiceItem
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PrintAndShareManager {

    fun formatWhatsAppMemo(invoice: InvoiceEntity, shop: ShopProfileEntity): String {
        val items = InvoiceItem.listFromJson(invoice.itemsJson)
        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(invoice.timestamp))
        val sb = StringBuilder()
        sb.append("🧾 *${shop.shopName.ifBlank { "HishabKhata Invoice" }}*\n")
        if (shop.address.isNotBlank()) sb.append("📍 ${shop.address}\n")
        if (shop.phone.isNotBlank()) sb.append("📞 ${shop.phone}\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("Invoice: *#${invoice.invoiceNumber}*\n")
        sb.append("Date: $dateStr\n")
        if (invoice.customerName.isNotBlank()) {
            sb.append("Customer: *${invoice.customerName}*")
            if (invoice.customerPhone.isNotBlank()) sb.append(" (${invoice.customerPhone})")
            sb.append("\n")
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        items.forEachIndexed { index, item ->
            val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else "%.1f".format(item.quantity)
            sb.append("${index + 1}. *${item.name}*\n")
            sb.append("   ${qtyStr} ${item.unit} × ৳${"%.1f".format(item.unitPrice)} = *৳${"%.1f".format(item.totalPrice)}*\n")
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("Subtotal: ৳${"%.2f".format(invoice.subtotal)}\n")
        if (invoice.discount > 0) sb.append("Discount: -৳${"%.2f".format(invoice.discount)}\n")
        if (invoice.tax > 0) sb.append("Tax/VAT: +৳${"%.2f".format(invoice.tax)}\n")
        sb.append("*Grand Total: ৳${"%.2f".format(invoice.totalAmount)}*\n")
        sb.append("Paid (${invoice.paymentMode}): ৳${"%.2f".format(invoice.paidAmount)}\n")
        if (invoice.dueAmount > 0) {
            sb.append("⚠️ *Due (বাকি): ৳${"%.2f".format(invoice.dueAmount)}*\n")
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        if (shop.bkashNumber.isNotBlank()) {
            sb.append("📱 *Pay via bKash (${shop.bkashType}):* ${shop.bkashNumber}\n")
            if (shop.bkashType.equals("MERCHANT", ignoreCase = true)) {
                sb.append("Dial *247# -> Make Payment to ${shop.bkashNumber}\n")
            } else {
                sb.append("Dial *247# -> Send Money to ${shop.bkashNumber}\n")
            }
            sb.append("Counter Ref: ${invoice.invoiceNumber}\n")
            sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        }
        sb.append("${shop.receiptFooter.ifBlank { "Thank you for your business!" }}\n")
        return sb.toString()
    }

    fun shareViaWhatsApp(context: Context, text: String, phone: String = "") {
        try {
            val cleanPhone = phone.replace("+", "").replace("-", "").replace(" ", "").trim()
            val uri = if (cleanPhone.isNotBlank()) {
                val fullPhone = if (cleanPhone.startsWith("01")) "88$cleanPhone" else cleanPhone
                Uri.parse("https://api.whatsapp.com/send?phone=$fullPhone&text=${Uri.encode(text)}")
            } else {
                Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(text)}")
            }
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to standard share intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Memo"))
        }
    }

    fun shareViaSms(context: Context, phone: String, message: String) {
        try {
            val cleanPhone = phone.trim()
            val smsUri = if (cleanPhone.isNotBlank()) Uri.parse("smsto:$cleanPhone") else Uri.parse("smsto:")
            val intent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open SMS app: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun dialBkash(context: Context, bkashNumber: String) {
        try {
            val ussd = "*247%23" // *247#
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$ussd"))
            context.startActivity(intent)
            Toast.makeText(context, "bKash: $bkashNumber (Copied to Clipboard)", Toast.LENGTH_LONG).show()
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("bKash Number", bkashNumber)
            clipboard.setPrimaryClip(clip)
        } catch (e: Exception) {
            Toast.makeText(context, "Dialer error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Android PrintManager - Thermal Receipt (58mm / 80mm format) & A4 PDF
    fun printInvoice(context: Context, invoice: InvoiceEntity, shop: ShopProfileEntity, isThermal: Boolean) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            Toast.makeText(context, "Printing not supported on this device", Toast.LENGTH_SHORT).show()
            return
        }

        val jobName = "Invoice_${invoice.invoiceNumber}"
        val printAdapter = InvoicePrintDocumentAdapter(context, invoice, shop, isThermal)
        val printAttributes = if (isThermal) {
            PrintAttributes.Builder()
                .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
                .setMediaSize(PrintAttributes.MediaSize.ISO_A7) // A7 ~ 74x105mm, close to thermal roll
                .build()
        } else {
            PrintAttributes.Builder()
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .build()
        }

        printManager.print(jobName, printAdapter, printAttributes)
    }

    private class InvoicePrintDocumentAdapter(
        private val context: Context,
        private val invoice: InvoiceEntity,
        private val shop: ShopProfileEntity,
        private val isThermal: Boolean
    ) : PrintDocumentAdapter() {

        private var pdfDocument: PdfDocument? = null

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback?,
            extras: Bundle?
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback?.onLayoutCancelled()
                return
            }

            val pdi = PrintDocumentInfo.Builder("Invoice_${invoice.invoiceNumber}.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build()
            callback?.onLayoutFinished(pdi, true)
        }

        override fun onWrite(
            pages: Array<out PageRange>?,
            destination: ParcelFileDescriptor?,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback?
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback?.onWriteCancelled()
                return
            }

            val pageWidth = if (isThermal) 384 else 595 // 58mm ~ 384px, A4 ~ 595px (72dpi)
            val pageHeight = if (isThermal) 680 else 842 // A4 ~ 842px

            pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument!!.startPage(pageInfo)
            val canvas = page.canvas

            drawInvoice(canvas, pageWidth, pageHeight)

            pdfDocument!!.finishPage(page)

            try {
                destination?.fileDescriptor?.let { fd ->
                    FileOutputStream(fd).use { out ->
                        pdfDocument!!.writeTo(out)
                    }
                }
                callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback?.onWriteFailed(e.message)
            } finally {
                pdfDocument?.close()
                pdfDocument = null
            }
        }

        private fun drawInvoice(canvas: Canvas, width: Int, height: Int) {
            val paint = Paint().apply {
                isAntiAlias = true
                color = Color.BLACK
            }
            val items = InvoiceItem.listFromJson(invoice.itemsJson)
            var y = 35f

            // Header
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = if (isThermal) 16f else 22f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(shop.shopName.ifBlank { "HishabKhata Store" }, width / 2f, y, paint)

            paint.typeface = Typeface.DEFAULT
            paint.textSize = if (isThermal) 10f else 13f
            if (shop.address.isNotBlank()) {
                y += 18f
                canvas.drawText(shop.address, width / 2f, y, paint)
            }
            if (shop.phone.isNotBlank()) {
                y += 16f
                canvas.drawText("Tel: ${shop.phone}", width / 2f, y, paint)
            }

            y += 20f
            paint.strokeWidth = 1f
            canvas.drawLine(15f, y, width - 15f, y, paint)

            // Invoice details
            y += 18f
            paint.textAlign = Paint.Align.LEFT
            paint.textSize = if (isThermal) 10f else 12f
            canvas.drawText("Invoice: #${invoice.invoiceNumber}", 15f, y, paint)
            paint.textAlign = Paint.Align.RIGHT
            val dateStr = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date(invoice.timestamp))
            canvas.drawText(dateStr, width - 15f, y, paint)

            if (invoice.customerName.isNotBlank()) {
                y += 16f
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText("Customer: ${invoice.customerName} (${invoice.customerPhone})", 15f, y, paint)
            }

            y += 15f
            canvas.drawLine(15f, y, width - 15f, y, paint)

            // Table headers
            y += 16f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("Item", 15f, y, paint)
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("Qty", width * 0.55f, y, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Total (৳)", width - 15f, y, paint)

            y += 8f
            paint.typeface = Typeface.DEFAULT
            canvas.drawLine(15f, y, width - 15f, y, paint)

            // Items
            y += 16f
            for (item in items) {
                paint.textAlign = Paint.Align.LEFT
                val name = if (item.name.length > 20 && isThermal) item.name.take(18) + ".." else item.name
                canvas.drawText(name, 15f, y, paint)

                paint.textAlign = Paint.Align.CENTER
                val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else "%.1f".format(item.quantity)
                canvas.drawText(qtyStr, width * 0.55f, y, paint)

                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText("%.1f".format(item.totalPrice), width - 15f, y, paint)

                y += 18f
                if (y > height - 120) break
            }

            y += 5f
            canvas.drawLine(15f, y, width - 15f, y, paint)

            // Totals
            y += 18f
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("Subtotal:", width * 0.45f, y, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("৳%.2f".format(invoice.subtotal), width - 15f, y, paint)

            if (invoice.discount > 0) {
                y += 16f
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText("Discount:", width * 0.45f, y, paint)
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText("-৳%.2f".format(invoice.discount), width - 15f, y, paint)
            }

            y += 18f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = if (isThermal) 12f else 15f
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("Total Amount:", width * 0.45f, y, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("৳%.2f".format(invoice.totalAmount), width - 15f, y, paint)

            paint.typeface = Typeface.DEFAULT
            paint.textSize = if (isThermal) 10f else 12f
            y += 16f
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("Paid (${invoice.paymentMode}):", width * 0.45f, y, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("৳%.2f".format(invoice.paidAmount), width - 15f, y, paint)

            if (invoice.dueAmount > 0) {
                y += 16f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText("Due (বাকি):", width * 0.45f, y, paint)
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText("৳%.2f".format(invoice.dueAmount), width - 15f, y, paint)
            }

            y += 20f
            paint.typeface = Typeface.DEFAULT
            canvas.drawLine(15f, y, width - 15f, y, paint)

            y += 18f
            paint.textAlign = Paint.Align.CENTER
            if (shop.bkashNumber.isNotBlank()) {
                canvas.drawText("bKash (${shop.bkashType}): ${shop.bkashNumber}", width / 2f, y, paint)
                y += 15f
            }
            canvas.drawText(shop.receiptFooter.ifBlank { "Thank You! Come Again." }, width / 2f, y, paint)
        }
    }
}
