package com.example.data.cloud

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.ContactEntity
import com.example.data.local.InvoiceEntity
import com.example.data.local.ProductEntity
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val TAG = "FirestoreSync"

class FirestoreSyncManager(private val context: Context, private val database: AppDatabase) {

    private val firestore: FirebaseFirestore by lazy {
        val dbId = context.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(dbId)
    }

    private fun getCurrentUserId(): String? = Firebase.auth.currentUser?.uid

    suspend fun syncAllToCloud(): Boolean = withContext(Dispatchers.IO) {
        val uid = getCurrentUserId() ?: return@withContext false
        try {
            // 1. Sync Shop Profile
            val profile = database.shopProfileDao().getProfileSync()
            if (profile != null) {
                val profileMap = mapOf(
                    "userId" to uid,
                    "shopName" to profile.shopName,
                    "ownerName" to profile.ownerName,
                    "phone" to profile.phone,
                    "bkashNumber" to profile.bkashNumber,
                    "address" to profile.address,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                firestore.collection("users").document(uid)
                    .set(profileMap, SetOptions.merge()).await()
            }

            // 2. Sync Products
            val products = database.productDao().getAllProducts().first()
            val prodCollection = firestore.collection("users").document(uid).collection("products")
            for (p in products) {
                val docId = "prod_${p.id}"
                val pMap = mapOf(
                    "userId" to uid,
                    "name" to p.name,
                    "nameBn" to p.nameBn,
                    "barcode" to p.barcode,
                    "purchasePrice" to p.purchasePrice,
                    "sellingPrice" to p.sellingPrice,
                    "stockQty" to p.stockQty,
                    "unit" to p.unit,
                    "lowStockThreshold" to p.lowStockThreshold,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                prodCollection.document(docId).set(pMap, SetOptions.merge()).await()
            }

            // 3. Sync Contacts
            val contacts = database.contactDao().getAllContacts().first()
            val contactCollection = firestore.collection("users").document(uid).collection("contacts")
            for (c in contacts) {
                val docId = "contact_${c.id}"
                val cMap = mapOf(
                    "userId" to uid,
                    "name" to c.name,
                    "phone" to c.phone,
                    "type" to c.type,
                    "currentBalance" to c.currentBalance,
                    "address" to c.address,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                contactCollection.document(docId).set(cMap, SetOptions.merge()).await()
            }

            // 4. Sync Invoices
            val invoices = database.invoiceDao().getAllInvoices().first()
            val invoiceCollection = firestore.collection("users").document(uid).collection("invoices")
            for (inv in invoices) {
                val docId = "inv_${inv.id}"
                val invMap = mapOf(
                    "userId" to uid,
                    "invoiceNumber" to inv.invoiceNumber,
                    "customerName" to inv.customerName,
                    "customerPhone" to inv.customerPhone,
                    "items" to listOf(inv.itemsJson),
                    "subtotal" to inv.subtotal,
                    "discount" to inv.discount,
                    "tax" to inv.tax,
                    "totalAmount" to inv.totalAmount,
                    "paidAmount" to inv.paidAmount,
                    "dueAmount" to inv.dueAmount,
                    "paymentMode" to inv.paymentMode,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                invoiceCollection.document(docId).set(invMap, SetOptions.merge()).await()
            }

            Log.d(TAG, "Sync to Firestore completed successfully for user: $uid")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Firestore sync failed", e)
            false
        }
    }
}
