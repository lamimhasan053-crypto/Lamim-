package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val nameBn: String = "",
    val barcode: String = "",
    val purchasePrice: Double,
    val sellingPrice: Double,
    val stockQty: Double,
    val unit: String = "pc", // pc, kg, gm, packet, ltr
    val lowStockThreshold: Double = 5.0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val phone: String = "",
    val type: String = "CUSTOMER", // "CUSTOMER" or "SUPPLIER"
    val currentBalance: Double = 0.0, // Positive = Due from customer / We will get (পাবো); Negative = We owe supplier (দেবো)
    val address: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val invoiceNumber: String,
    val customerName: String = "",
    val customerPhone: String = "",
    val itemsJson: String, // serialized list of invoice line items
    val subtotal: Double,
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    val totalAmount: Double,
    val paidAmount: Double,
    val dueAmount: Double = 0.0,
    val paymentMode: String = "CASH", // "CASH", "BKASH", "DUE"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "ledger_entries")
data class LedgerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val contactId: Long,
    val contactName: String,
    val type: String, // "GAVE" (টাকা দেওয়া / বাকি দেওয়া), "GOT" (টাকা পাওয়া / বাকি পরিশোধ)
    val amount: Double,
    val note: String = "",
    val invoiceId: Long? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "voice_shortcuts")
data class VoiceShortcutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val triggerWord: String, // e.g. "star", "sugar", "আলু"
    val targetProductName: String, // e.g. "Star Cigarette", "Sugar", "Potato"
    val defaultQuantity: Double = 1.0,
    val defaultPrice: Double = 0.0, // 0 means use product's selling price
    val note: String = ""
)

@Entity(tableName = "shop_profile")
data class ShopProfileEntity(
    @PrimaryKey val id: Int = 1,
    val shopName: String = "My Store",
    val ownerName: String = "Shopkeeper",
    val phone: String = "01700000000",
    val bkashNumber: String = "01700000000",
    val bkashType: String = "PERSONAL", // "MERCHANT" or "PERSONAL"
    val address: String = "Dhaka, Bangladesh",
    val receiptFooter: String = "Thank you! Please visit again."
)
