package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ContactEntity
import com.example.data.local.LedgerEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.BkashPink
import com.example.ui.theme.DueRed
import com.example.ui.theme.DueRedContainer
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.PaidGreen
import com.example.ui.theme.PaidGreenContainer
import com.example.util.GoogleWorkspaceManager
import com.example.util.PrintAndShareManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun KhataScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val selectedContact by viewModel.selectedContact.collectAsStateWithLifecycle()
    val ledgerEntries by viewModel.contactLedgerEntries.collectAsStateWithLifecycle()
    val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()

    var activeTab by remember { mutableIntStateOf(0) } // 0 = Customers (পাবো), 1 = Suppliers (দেবো)
    var searchQuery by remember { mutableStateOf("") }
    var showAddContactDialog by remember { mutableStateOf(false) }
    var showTransactionDialog by remember { mutableStateOf<String?>(null) } // "GAVE" or "GOT"

    if (selectedContact != null) {
        BackHandler { viewModel.clearSelectedContact() }
        ContactDetailView(
            contact = selectedContact!!,
            entries = ledgerEntries,
            shop = shopProfile,
            onBack = { viewModel.clearSelectedContact() },
            onAddTransaction = { type -> showTransactionDialog = type }
        )
    } else {
        val targetType = if (activeTab == 0) "CUSTOMER" else "SUPPLIER"
        val filtered = remember(contacts, targetType, searchQuery) {
            contacts.filter { it.type == targetType && (searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery)) }
        }

        val totalCustomerDue = remember(contacts) {
            contacts.filter { it.type == "CUSTOMER" && it.currentBalance > 0 }.sumOf { it.currentBalance }
        }
        val totalSupplierPayable = remember(contacts) {
            contacts.filter { it.type == "SUPPLIER" && it.currentBalance < 0 }.sumOf { Math.abs(it.currentBalance) }
        }

        Box(modifier = modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Summary Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldGreenDark),
                    modifier = Modifier.fillMaxWidth().testTag("khata_summary_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total You Will Get (পাবো)", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            Text(
                                "৳${"%.1f".format(totalCustomerDue)}",
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total You Will Give (দেবো)", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            Text(
                                "৳${"%.1f".format(totalSupplierPayable)}",
                                color = Color(0xFFFFD1D1),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tabs: Customers vs Suppliers
                TabRow(selectedTabIndex = activeTab) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("Customers (কাস্টমার)", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Suppliers (মহাজন)", fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().testTag("khata_search_input"),
                    placeholder = { Text("Search by name or phone...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Contact List
                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No ${if (activeTab == 0) "customers" else "suppliers"} found")
                            Text("Tap (+) button below to add", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered, key = { it.id }) { contact ->
                            ContactKhataCard(
                                contact = contact,
                                onClick = { viewModel.selectContact(contact) }
                            )
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = { showAddContactDialog = true },
                containerColor = EmeraldGreenPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("add_contact_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Contact")
            }
        }
    }

    if (showAddContactDialog) {
        AddContactDialog(
            defaultType = if (activeTab == 0) "CUSTOMER" else "SUPPLIER",
            onDismiss = { showAddContactDialog = false },
            onConfirm = { name, phone, type, balance, address ->
                viewModel.addContact(name, phone, type, balance, address)
                showAddContactDialog = false
            }
        )
    }

    showTransactionDialog?.let { type ->
        selectedContact?.let { contact ->
            AddLedgerTransactionDialog(
                type = type,
                contactName = contact.name,
                onDismiss = { showTransactionDialog = null },
                onConfirm = { amount, note ->
                    viewModel.addLedgerTransaction(contact, type, amount, note)
                    showTransactionDialog = null
                }
            )
        }
    }
}

@Composable
fun ContactKhataCard(
    contact: ContactEntity,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.testTag("contact_card_${contact.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(contact.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (contact.phone.isNotBlank()) {
                        Text(contact.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val balance = contact.currentBalance
                val label = if (balance > 0) "৳${"%.1f".format(balance)} পাবো" else if (balance < 0) "৳${"%.1f".format(Math.abs(balance))} দেবো" else "পরিশোধ"
                val color = if (balance > 0) DueRed else if (balance < 0) BkashPink else PaidGreen

                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
                Text(
                    text = "Tap to open khata",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun ContactDetailView(
    contact: ContactEntity,
    entries: List<LedgerEntity>,
    shop: com.example.data.local.ShopProfileEntity,
    onBack: () -> Unit,
    onAddTransaction: (String) -> Unit
) {
    val context = LocalContext.current
    val balance = contact.currentBalance
    val reminderMessage = remember(contact, shop) {
        "আসসালামু আলাইকুম ${contact.name},\n${shop.shopName} এ আপনার বকেয়া হিসাব ৳${balance.toInt()} টাকা। অনুগ্রহ করে দ্রুত পরিশোধ করবেন।\nবিকাশ নম্বর (${shop.bkashType}): ${shop.bkashNumber}\nধন্যবাদ!"
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(contact.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (contact.phone.isNotBlank()) {
                    Text(contact.phone, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Balance Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = if (balance > 0) DueRedContainer else PaidGreenContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (balance > 0) "Net Due from Customer (আপনি পাবেন)" else if (balance < 0) "Net Due to Supplier (আপনি দেবেন)" else "Account Settled (হিসাব শূন্য)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "৳${"%.2f".format(Math.abs(balance))}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (balance > 0) DueRed else PaidGreen
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Action Buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (contact.phone.isNotBlank() && balance > 0) {
                        Button(
                            onClick = { PrintAndShareManager.shareViaWhatsApp(context, reminderMessage, contact.phone) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("WhatsApp তাগাদা", color = Color.White, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { PrintAndShareManager.shareViaSms(context, contact.phone, reminderMessage) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("SMS তাগাদা", fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = { GoogleWorkspaceManager.addDueReminderToGoogleCalendar(context, contact.name, Math.abs(balance)) },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Calendar", fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Big Transaction Action Buttons
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { onAddTransaction("GAVE") },
                colors = ButtonDefaults.buttonColors(containerColor = DueRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(48.dp).testTag("gave_money_button")
            ) {
                Text("টাকা দিলাম (Gave)", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { onAddTransaction("GOT") },
                colors = ButtonDefaults.buttonColors(containerColor = PaidGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(48.dp).testTag("got_money_button")
            ) {
                Text("টাকা পেলাম (Got)", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Ledger History Timeline
        Text("Transaction History (খতিয়ান হিসাব)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        if (entries.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No ledger transactions recorded yet", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    LedgerEntryCard(entry = entry)
                }
            }
        }
    }
}

@Composable
fun LedgerEntryCard(entry: LedgerEntity) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (entry.note.isNotBlank()) entry.note else if (entry.type == "GAVE") "টাকা দেওয়া / বাকি" else "টাকা পাওয়া / পরিশোধ",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(entry.timestamp))
                Text(dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }

            Text(
                text = "${if (entry.type == "GAVE") "+" else "-"}৳${"%.1f".format(entry.amount)}",
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium,
                color = if (entry.type == "GAVE") DueRed else PaidGreen
            )
        }
    }
}

@Composable
fun AddContactDialog(
    defaultType: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, type: String, balance: Double, address: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(defaultType) }
    var balance by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Contact (নতুন হিসাব যোগ করুন)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (নাম) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile / WhatsApp Phone") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == "CUSTOMER",
                        onClick = { type = "CUSTOMER" },
                        label = { Text("Customer (কাস্টমার)") }
                    )
                    FilterChip(
                        selected = type == "SUPPLIER",
                        onClick = { type = "SUPPLIER" },
                        label = { Text("Supplier (মহাজন)") }
                    )
                }
                OutlinedTextField(
                    value = balance,
                    onValueChange = { balance = it },
                    label = { Text("Opening Balance (শুরুর বাকি - ঐচ্ছিক)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address (ঠিকানা)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, phone, type, balance.toDoubleOrNull() ?: 0.0, address)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save Contact")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddLedgerTransactionDialog(
    type: String,
    contactName: String,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, note: String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (type == "GAVE") "টাকা দিলাম (Gave Money / Due) to $contactName"
                else "টাকা পেলাম (Got Payment) from $contactName"
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (টাকার পরিমাণ) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Reason (বিবরণ - ঐচ্ছিক)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull()
                    if (amt != null && amt > 0) {
                        onConfirm(amt, note)
                    }
                },
                enabled = (amount.toDoubleOrNull() ?: 0.0) > 0,
                colors = ButtonDefaults.buttonColors(containerColor = if (type == "GAVE") DueRed else PaidGreen)
            ) {
                Text("Record Entry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
