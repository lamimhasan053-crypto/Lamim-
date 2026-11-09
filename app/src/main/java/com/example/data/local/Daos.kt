package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE stockQty <= lowStockThreshold ORDER BY stockQty ASC")
    fun getLowStockProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE LOWER(name) LIKE '%' || LOWER(:query) || '%' OR LOWER(nameBn) LIKE '%' || LOWER(:query) || '%'")
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("UPDATE products SET stockQty = stockQty + :delta, updatedAt = :time WHERE id = :productId")
    suspend fun adjustStock(productId: Long, delta: Double, time: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteProduct(product: ProductEntity)
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE type = :type ORDER BY name ASC")
    fun getContactsByType(type: String): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    suspend fun getContactById(id: Long): ContactEntity?

    @Query("SELECT * FROM contacts WHERE LOWER(name) LIKE '%' || LOWER(:query) || '%' OR phone LIKE '%' || :query || '%'")
    fun searchContacts(query: String): Flow<List<ContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity): Long

    @Update
    suspend fun updateContact(contact: ContactEntity)

    @Query("UPDATE contacts SET currentBalance = currentBalance + :balanceDelta, updatedAt = :time WHERE id = :contactId")
    suspend fun updateBalance(contactId: Long, balanceDelta: Double, time: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteContact(contact: ContactEntity)
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY timestamp DESC")
    fun getAllInvoices(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    suspend fun getInvoiceById(id: Long): InvoiceEntity?

    @Query("SELECT * FROM invoices ORDER BY id DESC LIMIT 1")
    suspend fun getLatestInvoice(): InvoiceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity): Long

    @Delete
    suspend fun deleteInvoice(invoice: InvoiceEntity)
}

@Dao
interface LedgerDao {
    @Query("SELECT * FROM ledger_entries ORDER BY timestamp DESC")
    fun getAllLedgerEntries(): Flow<List<LedgerEntity>>

    @Query("SELECT * FROM ledger_entries WHERE contactId = :contactId ORDER BY timestamp DESC")
    fun getEntriesForContact(contactId: Long): Flow<List<LedgerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: LedgerEntity): Long

    @Delete
    suspend fun deleteEntry(entry: LedgerEntity)
}

@Dao
interface VoiceShortcutDao {
    @Query("SELECT * FROM voice_shortcuts ORDER BY triggerWord ASC")
    fun getAllShortcuts(): Flow<List<VoiceShortcutEntity>>

    @Query("SELECT * FROM voice_shortcuts WHERE LOWER(triggerWord) = LOWER(:word) LIMIT 1")
    suspend fun findByTrigger(word: String): VoiceShortcutEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShortcut(shortcut: VoiceShortcutEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(shortcuts: List<VoiceShortcutEntity>)

    @Delete
    suspend fun deleteShortcut(shortcut: VoiceShortcutEntity)
}

@Dao
interface ShopProfileDao {
    @Query("SELECT * FROM shop_profile WHERE id = 1 LIMIT 1")
    fun getProfile(): Flow<ShopProfileEntity?>

    @Query("SELECT * FROM shop_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfileSync(): ShopProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateProfile(profile: ShopProfileEntity)
}
