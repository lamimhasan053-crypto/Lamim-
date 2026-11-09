package com.example.ui

import android.app.Activity
import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.AuthManager
import com.example.data.cloud.FirestoreSyncManager
import com.example.data.local.AppDatabase
import com.example.data.local.ContactEntity
import com.example.data.local.InvoiceEntity
import com.example.data.local.LedgerEntity
import com.example.data.local.ProductEntity
import com.example.data.local.ShopProfileEntity
import com.example.data.local.VoiceShortcutEntity
import com.example.data.model.InvoiceItem
import com.example.util.GoogleWorkspaceManager
import com.example.util.PrintAndShareManager
import com.example.voice.VoiceAction
import com.example.voice.VoiceAssistantEngine
import com.example.voice.VoiceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val database = AppDatabase.getInstance(application)
    val authManager = AuthManager(application, viewModelScope)
    val syncManager = FirestoreSyncManager(application, database)
    val voiceEngine = VoiceAssistantEngine(application, database, viewModelScope)

    // Data streams
    val products = database.productDao().getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contacts = database.contactDao().getAllContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val invoices = database.invoiceDao().getAllInvoices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val voiceShortcuts = database.voiceShortcutDao().getAllShortcuts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shopProfile: StateFlow<ShopProfileEntity> = database.shopProfileDao().getProfile()
        .map { it ?: ShopProfileEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ShopProfileEntity())

    // Active Cart / Invoice State
    private val _cartItems = MutableStateFlow<List<InvoiceItem>>(emptyList())
    val cartItems: StateFlow<List<InvoiceItem>> = _cartItems.asStateFlow()

    private val _cartCustomerName = MutableStateFlow("")
    val cartCustomerName: StateFlow<String> = _cartCustomerName.asStateFlow()

    private val _cartCustomerPhone = MutableStateFlow("")
    val cartCustomerPhone: StateFlow<String> = _cartCustomerPhone.asStateFlow()

    private val _cartDiscount = MutableStateFlow(0.0)
    val cartDiscount: StateFlow<Double> = _cartDiscount.asStateFlow()

    private val _cartTax = MutableStateFlow(0.0)
    val cartTax: StateFlow<Double> = _cartTax.asStateFlow()

    private val _cartPaidAmount = MutableStateFlow<Double?>(null) // null = paid in full
    val cartPaidAmount: StateFlow<Double?> = _cartPaidAmount.asStateFlow()

    private val _paymentMode = MutableStateFlow("CASH") // CASH, BKASH, DUE
    val paymentMode: StateFlow<String> = _paymentMode.asStateFlow()

    // Calculated cart totals
    val cartSubtotal: StateFlow<Double> = combine(_cartItems) { items ->
        items[0].sumOf { it.totalPrice }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cartGrandTotal: StateFlow<Double> = combine(cartSubtotal, _cartDiscount, _cartTax) { sub, disc, tax ->
        maxOf(0.0, sub - disc + tax)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Search queries
    val searchQuery = MutableStateFlow("")
    val contactSearchQuery = MutableStateFlow("")

    // Selected Contact for Khata details
    private val _selectedContact = MutableStateFlow<ContactEntity?>(null)
    val selectedContact: StateFlow<ContactEntity?> = _selectedContact.asStateFlow()

    private val _contactLedgerEntries = MutableStateFlow<List<LedgerEntity>>(emptyList())
    val contactLedgerEntries: StateFlow<List<LedgerEntity>> = _contactLedgerEntries.asStateFlow()

    // Last completed invoice for printing / sharing dialog
    private val _lastCompletedInvoice = MutableStateFlow<InvoiceEntity?>(null)
    val lastCompletedInvoice: StateFlow<InvoiceEntity?> = _lastCompletedInvoice.asStateFlow()

    // Sync state
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        authManager.attemptAutoSignIn()
        // Listen to voice assistant actions
        viewModelScope.launch {
            voiceEngine.voiceState.collect { state ->
                if (state is VoiceState.Executed) {
                    handleVoiceAction(state.action)
                }
            }
        }
    }

    private fun handleVoiceAction(action: VoiceAction) {
        when (action) {
            is VoiceAction.AddInvoiceItem -> {
                addItemToCartByName(action.productName, action.quantity, action.unitPrice)
                Toast.makeText(getApplication(), "Added ${action.quantity}x ${action.productName}", Toast.LENGTH_SHORT).show()
            }
            is VoiceAction.LogExpense -> {
                viewModelScope.launch {
                    val expContact = database.contactDao().getContactById(1L)
                    database.ledgerDao().insertEntry(
                        LedgerEntity(
                            contactId = 0L,
                            contactName = "Daily Expense",
                            type = "GAVE",
                            amount = action.amount,
                            note = action.description
                        )
                    )
                    Toast.makeText(getApplication(), "Logged expense: ৳${action.amount} (${action.description})", Toast.LENGTH_LONG).show()
                }
            }
            is VoiceAction.LogDue -> {
                viewModelScope.launch {
                    val contact = database.contactDao().searchContacts(action.contactName)
                    // If contact doesn't exist, create one
                    val existing = database.contactDao().searchContacts(action.contactName)
                    val cid = database.contactDao().insertContact(
                        ContactEntity(
                            name = action.contactName,
                            type = "CUSTOMER",
                            currentBalance = action.amount
                        )
                    )
                    database.ledgerDao().insertEntry(
                        LedgerEntity(
                            contactId = cid,
                            contactName = action.contactName,
                            type = "GAVE",
                            amount = action.amount,
                            note = "Due recorded via Voice"
                        )
                    )
                    Toast.makeText(getApplication(), "Recorded due: ৳${action.amount} for ${action.contactName}", Toast.LENGTH_LONG).show()
                }
            }
            is VoiceAction.Informational -> {
                Toast.makeText(getApplication(), action.message, Toast.LENGTH_SHORT).show()
            }
            is VoiceAction.Error -> {
                Toast.makeText(getApplication(), action.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun addItemToCart(product: ProductEntity, quantity: Double = 1.0) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.productId == product.id }
        if (index >= 0) {
            val existing = current[index]
            val newQty = existing.quantity + quantity
            current[index] = existing.copy(quantity = newQty, totalPrice = newQty * existing.unitPrice)
        } else {
            current.add(
                InvoiceItem(
                    productId = product.id,
                    name = product.name,
                    quantity = quantity,
                    unit = product.unit,
                    unitPrice = product.sellingPrice,
                    totalPrice = quantity * product.sellingPrice
                )
            )
        }
        _cartItems.value = current
    }

    fun addItemToCartByName(name: String, quantity: Double, unitPrice: Double = 0.0) {
        viewModelScope.launch {
            val matched = products.value.firstOrNull { it.name.contains(name, ignoreCase = true) || it.nameBn.contains(name, ignoreCase = true) }
            val price = if (unitPrice > 0) unitPrice else (matched?.sellingPrice ?: 100.0)
            val pid = matched?.id ?: 0L
            val unit = matched?.unit ?: "pc"

            val current = _cartItems.value.toMutableList()
            val index = current.indexOfFirst { it.name.equals(name, ignoreCase = true) }
            if (index >= 0) {
                val existing = current[index]
                val newQty = existing.quantity + quantity
                current[index] = existing.copy(quantity = newQty, totalPrice = newQty * price)
            } else {
                current.add(
                    InvoiceItem(
                        productId = pid,
                        name = matched?.name ?: name,
                        quantity = quantity,
                        unit = unit,
                        unitPrice = price,
                        totalPrice = quantity * price
                    )
                )
            }
            _cartItems.value = current
        }
    }

    fun updateCartItemQuantity(index: Int, newQty: Double) {
        if (newQty <= 0) {
            removeCartItem(index)
            return
        }
        val current = _cartItems.value.toMutableList()
        if (index in current.indices) {
            val item = current[index]
            current[index] = item.copy(quantity = newQty, totalPrice = newQty * item.unitPrice)
            _cartItems.value = current
        }
    }

    fun removeCartItem(index: Int) {
        val current = _cartItems.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _cartItems.value = current
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _cartCustomerName.value = ""
        _cartCustomerPhone.value = ""
        _cartDiscount.value = 0.0
        _cartTax.value = 0.0
        _cartPaidAmount.value = null
        _paymentMode.value = "CASH"
    }

    fun setCartCustomer(name: String, phone: String) {
        _cartCustomerName.value = name
        _cartCustomerPhone.value = phone
    }

    fun setCartDiscount(discount: Double) {
        _cartDiscount.value = discount
    }

    fun setCartPaymentMode(mode: String) {
        _paymentMode.value = mode
    }

    fun setCartPaidAmount(amount: Double?) {
        _cartPaidAmount.value = amount
    }

    fun checkoutInvoice(onSuccess: (InvoiceEntity) -> Unit) {
        val items = _cartItems.value
        if (items.isEmpty()) {
            Toast.makeText(getApplication(), "Cart is empty!", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            val subtotal = items.sumOf { it.totalPrice }
            val discount = _cartDiscount.value
            val tax = _cartTax.value
            val grandTotal = maxOf(0.0, subtotal - discount + tax)
            val paid = _cartPaidAmount.value ?: grandTotal
            val due = maxOf(0.0, grandTotal - paid)

            val invoiceNumber = "INV-${System.currentTimeMillis() % 100000}"
            val invoice = InvoiceEntity(
                invoiceNumber = invoiceNumber,
                customerName = _cartCustomerName.value,
                customerPhone = _cartCustomerPhone.value,
                itemsJson = InvoiceItem.listToJson(items),
                subtotal = subtotal,
                discount = discount,
                tax = tax,
                totalAmount = grandTotal,
                paidAmount = paid,
                dueAmount = due,
                paymentMode = _paymentMode.value
            )

            val invoiceId = database.invoiceDao().insertInvoice(invoice)
            val savedInvoice = invoice.copy(id = invoiceId)

            // Reduce stock for items
            for (item in items) {
                if (item.productId > 0) {
                    database.productDao().adjustStock(item.productId, -item.quantity)
                }
            }

            // If there is due and customer info, record in Khata
            if (due > 0 && _cartCustomerName.value.isNotBlank()) {
                val customerName = _cartCustomerName.value
                val customerPhone = _cartCustomerPhone.value
                // Check if contact exists
                val contact = database.contactDao().searchContacts(customerName)
                val cid = database.contactDao().insertContact(
                    ContactEntity(
                        name = customerName,
                        phone = customerPhone,
                        type = "CUSTOMER",
                        currentBalance = due
                    )
                )
                database.ledgerDao().insertEntry(
                    LedgerEntity(
                        contactId = cid,
                        contactName = customerName,
                        type = "GAVE",
                        amount = due,
                        note = "Invoice #$invoiceNumber due",
                        invoiceId = invoiceId
                    )
                )
            }

            _lastCompletedInvoice.value = savedInvoice
            clearCart()
            onSuccess(savedInvoice)
            // Trigger background cloud sync if signed in
            syncManager.syncAllToCloud()
        }
    }

    // Contact & Khata Operations
    fun selectContact(contact: ContactEntity) {
        _selectedContact.value = contact
        viewModelScope.launch {
            database.ledgerDao().getEntriesForContact(contact.id).collect {
                _contactLedgerEntries.value = it
            }
        }
    }

    fun clearSelectedContact() {
        _selectedContact.value = null
        _contactLedgerEntries.value = emptyList()
    }

    fun addContact(name: String, phone: String, type: String, initialBalance: Double, address: String = "") {
        viewModelScope.launch {
            val id = database.contactDao().insertContact(
                ContactEntity(
                    name = name,
                    phone = phone,
                    type = type,
                    currentBalance = initialBalance,
                    address = address
                )
            )
            if (initialBalance != 0.0) {
                database.ledgerDao().insertEntry(
                    LedgerEntity(
                        contactId = id,
                        contactName = name,
                        type = if (initialBalance > 0) "GAVE" else "GOT",
                        amount = Math.abs(initialBalance),
                        note = "Opening Balance"
                    )
                )
            }
            syncManager.syncAllToCloud()
        }
    }

    fun addLedgerTransaction(contact: ContactEntity, type: String, amount: Double, note: String) {
        viewModelScope.launch {
            database.ledgerDao().insertEntry(
                LedgerEntity(
                    contactId = contact.id,
                    contactName = contact.name,
                    type = type,
                    amount = amount,
                    note = note
                )
            )
            // If GAVE, balance increases (+); If GOT, balance decreases (-)
            val delta = if (type == "GAVE") amount else -amount
            database.contactDao().updateBalance(contact.id, delta)
            // Update local selected state
            _selectedContact.value = contact.copy(currentBalance = contact.currentBalance + delta)
            syncManager.syncAllToCloud()
        }
    }

    // Product & Stock Operations
    fun saveProduct(
        id: Long = 0L,
        name: String,
        nameBn: String,
        barcode: String,
        purchasePrice: Double,
        sellingPrice: Double,
        stockQty: Double,
        unit: String,
        lowStock: Double
    ) {
        viewModelScope.launch {
            val entity = ProductEntity(
                id = id,
                name = name,
                nameBn = nameBn,
                barcode = barcode,
                purchasePrice = purchasePrice,
                sellingPrice = sellingPrice,
                stockQty = stockQty,
                unit = unit,
                lowStockThreshold = lowStock
            )
            if (id == 0L) {
                database.productDao().insertProduct(entity)
            } else {
                database.productDao().updateProduct(entity)
            }
            syncManager.syncAllToCloud()
        }
    }

    fun adjustStock(productId: Long, delta: Double) {
        viewModelScope.launch {
            database.productDao().adjustStock(productId, delta)
            syncManager.syncAllToCloud()
        }
    }

    // Shortcuts
    fun saveVoiceShortcut(trigger: String, targetProduct: String, defaultQty: Double, defaultPrice: Double) {
        viewModelScope.launch {
            database.voiceShortcutDao().insertShortcut(
                VoiceShortcutEntity(
                    triggerWord = trigger.trim().lowercase(Locale.getDefault()),
                    targetProductName = targetProduct.trim(),
                    defaultQuantity = defaultQty,
                    defaultPrice = defaultPrice
                )
            )
            Toast.makeText(getApplication(), "Shortcut '$trigger' saved!", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteVoiceShortcut(shortcut: VoiceShortcutEntity) {
        viewModelScope.launch {
            database.voiceShortcutDao().deleteShortcut(shortcut)
        }
    }

    // Shop Profile
    fun updateShopProfile(profile: ShopProfileEntity) {
        viewModelScope.launch {
            database.shopProfileDao().updateProfile(profile)
            syncManager.syncAllToCloud()
            Toast.makeText(getApplication(), "Shop profile updated!", Toast.LENGTH_SHORT).show()
        }
    }

    // Google Workspace Actions
    fun importGoogleDocText(text: String, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val count = GoogleWorkspaceManager.parseAndImportDocumentText(text, database)
            onComplete(count)
            if (count > 0) syncManager.syncAllToCloud()
        }
    }

    fun exportBackup(activity: Activity) {
        viewModelScope.launch {
            val backup = GoogleWorkspaceManager.generateBackupJson(database)
            GoogleWorkspaceManager.exportToGoogleDrive(activity, backup)
        }
    }

    fun triggerCloudSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            val ok = syncManager.syncAllToCloud()
            _isSyncing.value = false
            val msg = if (ok) "Cloud sync successful!" else "Sign in with Google to sync data"
            Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
        }
    }
}
