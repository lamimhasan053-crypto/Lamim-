package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProductEntity::class,
        ContactEntity::class,
        InvoiceEntity::class,
        LedgerEntity::class,
        VoiceShortcutEntity::class,
        ShopProfileEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun contactDao(): ContactDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun voiceShortcutDao(): VoiceShortcutDao
    abstract fun shopProfileDao(): ShopProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hishab_khata.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Pre-populate with initial retail products and voice shortcuts
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getInstance(context)
                            seedInitialData(database)
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun seedInitialData(database: AppDatabase) {
            // Seed Shop Profile
            database.shopProfileDao().updateProfile(
                ShopProfileEntity(
                    id = 1,
                    shopName = "Bismillah General Store",
                    ownerName = "Shopkeeper",
                    phone = "01712345678",
                    bkashNumber = "01712345678",
                    bkashType = "PERSONAL",
                    address = "Mirpur-10, Dhaka",
                    receiptFooter = "Thanks for shopping with us! Please come again."
                )
            )

            // Seed Sample Grocery Products
            val sampleProducts = listOf(
                ProductEntity(name = "Star Cigarette", nameBn = "স্টার সিগারেট", barcode = "8941001", purchasePrice = 850.0, sellingPrice = 920.0, stockQty = 500.0, unit = "packet", lowStockThreshold = 50.0),
                ProductEntity(name = "Miniket Rice", nameBn = "মিনিকেট চাল", barcode = "8941002", purchasePrice = 68.0, sellingPrice = 75.0, stockQty = 120.0, unit = "kg", lowStockThreshold = 20.0),
                ProductEntity(name = "Soybean Oil (1L)", nameBn = "সয়াবিন তেল (১ লি.)", barcode = "8941003", purchasePrice = 175.0, sellingPrice = 188.0, stockQty = 45.0, unit = "ltr", lowStockThreshold = 10.0),
                ProductEntity(name = "White Sugar", nameBn = "সাদা চিনি", barcode = "8941004", purchasePrice = 130.0, sellingPrice = 142.0, stockQty = 80.0, unit = "kg", lowStockThreshold = 15.0),
                ProductEntity(name = "Red Lentil (Mosur)", nameBn = "মসুর ডাল", barcode = "8941005", purchasePrice = 125.0, sellingPrice = 140.0, stockQty = 60.0, unit = "kg", lowStockThreshold = 10.0),
                ProductEntity(name = "Taaza Tea (400g)", nameBn = "তাজা চা (৪০০ গ্রাম)", barcode = "8941006", purchasePrice = 210.0, sellingPrice = 230.0, stockQty = 25.0, unit = "packet", lowStockThreshold = 5.0)
            )
            database.productDao().insertAll(sampleProducts)

            // Seed Contacts (Customers & Suppliers)
            database.contactDao().insertContact(
                ContactEntity(name = "Karim Bhai", phone = "01811223344", type = "CUSTOMER", currentBalance = 500.0, address = "Shop neighbor")
            )
            database.contactDao().insertContact(
                ContactEntity(name = "Rahim Mia", phone = "01999887766", type = "CUSTOMER", currentBalance = 0.0, address = "House 12, Road 4")
            )
            database.contactDao().insertContact(
                ContactEntity(name = "Akbar Wholesale Ltd", phone = "01755443322", type = "SUPPLIER", currentBalance = -4500.0, address = "Chawkbazar")
            )

            // Seed Voice Shortcuts
            val shortcuts = listOf(
                VoiceShortcutEntity(triggerWord = "star", targetProductName = "Star Cigarette", defaultQuantity = 100.0, defaultPrice = 920.0, note = "Star tobacco 100 pcs at standard rate"),
                VoiceShortcutEntity(triggerWord = "sugar", targetProductName = "White Sugar", defaultQuantity = 1.0, defaultPrice = 142.0, note = "1 kg white sugar"),
                VoiceShortcutEntity(triggerWord = "oil", targetProductName = "Soybean Oil (1L)", defaultQuantity = 1.0, defaultPrice = 188.0, note = "1 liter soybean oil"),
                VoiceShortcutEntity(triggerWord = "rice", targetProductName = "Miniket Rice", defaultQuantity = 5.0, defaultPrice = 75.0, note = "5 kg miniket rice")
            )
            database.voiceShortcutDao().insertAll(shortcuts)
        }
    }
}
