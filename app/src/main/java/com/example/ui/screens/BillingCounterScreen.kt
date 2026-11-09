package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.InvoiceEntity
import com.example.data.local.ProductEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.BkashPink
import com.example.ui.theme.DueRed
import com.example.ui.theme.DueRedContainer
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.util.PrintAndShareManager
import com.example.voice.VoiceState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingCounterScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val cartSubtotal by viewModel.cartSubtotal.collectAsStateWithLifecycle()
    val cartGrandTotal by viewModel.cartGrandTotal.collectAsStateWithLifecycle()
    val voiceShortcuts by viewModel.voiceShortcuts.collectAsStateWithLifecycle()
    val voiceState by viewModel.voiceEngine.voiceState.collectAsStateWithLifecycle()
    val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showCartSheet by remember { mutableStateOf(false) }
    var completedInvoiceToView by remember { mutableStateOf<InvoiceEntity?>(null) }

    val filteredProducts = remember(products, searchQuery) {
        if (searchQuery.isBlank()) products
        else products.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.nameBn.contains(searchQuery, ignoreCase = true) ||
            it.barcode.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Voice Assistant Header & Live Bar
            VoiceAssistantBar(
                voiceState = voiceState,
                onMicClick = {
                    if (voiceState is VoiceState.Listening) {
                        viewModel.voiceEngine.stopListening()
                    } else {
                        viewModel.voiceEngine.startListening("bn-BD")
                    }
                },
                onQuickSpeech = { phrase ->
                    viewModel.voiceEngine.processSpokenCommand(phrase)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Voice Shortcut Pills
            Text(
                text = "Voice Shortcuts (কথা বলে যোগ করুন):",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(voiceShortcuts) { shortcut ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .clickable {
                                viewModel.voiceEngine.processSpokenCommand("${shortcut.triggerWord} ${shortcut.defaultQuantity.toInt()}")
                            }
                            .testTag("shortcut_chip_${shortcut.triggerWord}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("🎤", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "\"${shortcut.triggerWord} ${shortcut.defaultQuantity.toInt()}\"",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_search_input"),
                placeholder = { Text("Search product name, বাংলা or barcode...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Products Grid / List
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ShoppingBag,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No products found", style = MaterialTheme.typography.titleMedium)
                        Text("Add products from the Stock tab or speak via mic", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        ProductBillingCard(
                            product = product,
                            onAddToCart = { viewModel.addItemToCart(product, 1.0) }
                        )
                    }
                }
            }
        }

        // Floating Cart Summary Bar
        if (cartItems.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 8.dp,
                color = EmeraldGreenDark
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCartSheet = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "${cartItems.sumOf { it.quantity.toInt() }} Items in Bill",
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "৳${"%.2f".format(cartGrandTotal)}",
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Button(
                        onClick = { showCartSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = EmeraldGreenPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("view_bill_button")
                    ) {
                        Text("View Bill", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Active Cart & Checkout Bottom Sheet
    if (showCartSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCartSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            CartCheckoutSheet(
                viewModel = viewModel,
                onClose = { showCartSheet = false },
                onCheckoutSuccess = { invoice ->
                    showCartSheet = false
                    completedInvoiceToView = invoice
                }
            )
        }
    }

    // Invoice Completed Dialog (Print & Share Options)
    completedInvoiceToView?.let { invoice ->
        InvoiceCompletionDialog(
            invoice = invoice,
            shop = shopProfile,
            onDismiss = { completedInvoiceToView = null }
        )
    }
}

@Composable
fun VoiceAssistantBar(
    voiceState: VoiceState,
    onMicClick: () -> Unit,
    onQuickSpeech: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (voiceState) {
                is VoiceState.Listening -> DueRedContainer
                is VoiceState.Processing -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        modifier = Modifier.fillMaxWidth().testTag("voice_assistant_bar")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingActionButton(
                onClick = onMicClick,
                shape = CircleShape,
                containerColor = if (voiceState is VoiceState.Listening) DueRed else EmeraldGreenPrimary,
                contentColor = Color.White,
                modifier = Modifier.size(46.dp).testTag("voice_mic_button")
            ) {
                if (voiceState is VoiceState.Listening) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop")
                } else {
                    Icon(Icons.Default.Mic, contentDescription = "Start Voice Assistant")
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                val label = when (voiceState) {
                    is VoiceState.Listening -> "Listening... (কথা বলুন Bangla / English)"
                    is VoiceState.Processing -> (voiceState as VoiceState.Processing).spokenText
                    is VoiceState.Executed -> "Heard: ${(voiceState as VoiceState.Executed).text}"
                    is VoiceState.Error -> (voiceState as VoiceState.Error).message
                    else -> "AI Voice Assistant (বলুন 'star 100' বা 'Add expense 250')"
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Supports fast Bengali & English commands",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (voiceState is VoiceState.Processing) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }
    }
}

@Composable
fun ProductBillingCard(
    product: ProductEntity,
    onAddToCart: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().testTag("product_card_${product.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (product.nameBn.isNotBlank()) {
                    Text(
                        text = product.nameBn,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "৳${"%.1f".format(product.sellingPrice)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = EmeraldGreenPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = " / ${product.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val isLow = product.stockQty <= product.lowStockThreshold
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isLow) DueRedContainer else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (isLow) "Low: ${product.stockQty.toInt()} left" else "Stock: ${product.stockQty.toInt()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isLow) DueRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Button(
                onClick = onAddToCart,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("add_item_btn_${product.id}")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Text(" Add")
            }
        }
    }
}

@Composable
fun CartCheckoutSheet(
    viewModel: MainViewModel,
    onClose: () -> Unit,
    onCheckoutSuccess: (InvoiceEntity) -> Unit
) {
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val subtotal by viewModel.cartSubtotal.collectAsStateWithLifecycle()
    val grandTotal by viewModel.cartGrandTotal.collectAsStateWithLifecycle()
    val customerName by viewModel.cartCustomerName.collectAsStateWithLifecycle()
    val customerPhone by viewModel.cartCustomerPhone.collectAsStateWithLifecycle()
    val discount by viewModel.cartDiscount.collectAsStateWithLifecycle()
    val paymentMode by viewModel.paymentMode.collectAsStateWithLifecycle()
    val paidAmount by viewModel.cartPaidAmount.collectAsStateWithLifecycle()

    var discountInput by remember { mutableStateOf(if (discount > 0) discount.toString() else "") }
    var paidInput by remember { mutableStateOf(if (paidAmount != null) paidAmount.toString() else "") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Current Invoice / Bill", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            TextButton(onClick = { viewModel.clearCart() }) {
                Text("Clear All", color = DueRed)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Customer Details
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = customerName,
                onValueChange = { viewModel.setCartCustomer(it, customerPhone) },
                label = { Text("Customer Name (Optional)") },
                modifier = Modifier.weight(1f).testTag("cart_customer_name"),
                singleLine = true
            )
            OutlinedTextField(
                value = customerPhone,
                onValueChange = { viewModel.setCartCustomer(customerName, it) },
                label = { Text("Phone / WhatsApp") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.weight(1f).testTag("cart_customer_phone"),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Cart Line Items List
        LazyColumn(
            modifier = Modifier.weight(1f, fill = false).height(200.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(cartItems) { index, item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.name, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(
                            "৳${item.unitPrice} × ${if (item.quantity % 1.0 == 0.0) item.quantity.toInt() else item.quantity} ${item.unit} = ৳${"%.1f".format(item.totalPrice)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.updateCartItemQuantity(index, item.quantity - 1.0) }) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                        }
                        Text(
                            text = "${if (item.quantity % 1.0 == 0.0) item.quantity.toInt() else item.quantity}",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        IconButton(onClick = { viewModel.updateCartItemQuantity(index, item.quantity + 1.0) }) {
                            Icon(Icons.Default.Add, contentDescription = "Increase")
                        }
                        IconButton(onClick = { viewModel.removeCartItem(index) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DueRed)
                        }
                    }
                }
                HorizontalDivider()
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Calculations & Payment Mode
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Subtotal:", style = MaterialTheme.typography.bodyMedium)
            Text("৳${"%.2f".format(subtotal)}", fontWeight = FontWeight.Bold)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = discountInput,
                onValueChange = {
                    discountInput = it
                    viewModel.setCartDiscount(it.toDoubleOrNull() ?: 0.0)
                },
                label = { Text("Discount (৳)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text("Total Payable:", style = MaterialTheme.typography.labelMedium)
                Text(
                    "৳${"%.2f".format(grandTotal)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = EmeraldGreenPrimary,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Payment Method Selector
        Text("Payment Mode (টাকার ধরন):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = paymentMode == "CASH",
                onClick = { viewModel.setCartPaymentMode("CASH") },
                label = { Text("Cash (নগদ)") }
            )
            FilterChip(
                selected = paymentMode == "BKASH",
                onClick = { viewModel.setCartPaymentMode("BKASH") },
                label = { Text("bKash (বিকাশ)") }
            )
            FilterChip(
                selected = paymentMode == "DUE",
                onClick = {
                    viewModel.setCartPaymentMode("DUE")
                    viewModel.setCartPaidAmount(0.0)
                },
                label = { Text("Baki (বাকী)") }
            )
        }

        if (paymentMode != "DUE") {
            OutlinedTextField(
                value = paidInput,
                onValueChange = {
                    paidInput = it
                    viewModel.setCartPaidAmount(it.toDoubleOrNull())
                },
                label = { Text("Paid Amount (Default = Full ৳${grandTotal.toInt()})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.checkoutInvoice(onCheckoutSuccess) },
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("complete_bill_button")
        ) {
            Icon(Icons.Default.Receipt, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Complete & Print Bill (বিল সম্পন্ন করুন)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun InvoiceCompletionDialog(
    invoice: InvoiceEntity,
    shop: com.example.data.local.ShopProfileEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val formattedMemo = remember(invoice, shop) { PrintAndShareManager.formatWhatsAppMemo(invoice, shop) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreenPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Bill Created #${invoice.invoiceNumber}")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Total: ৳${"%.2f".format(invoice.totalAmount)} | Paid: ৳${"%.2f".format(invoice.paidAmount)}",
                    fontWeight = FontWeight.Bold
                )
                if (invoice.dueAmount > 0) {
                    Text(
                        text = "⚠️ Due Recorded: ৳${"%.2f".format(invoice.dueAmount)} for ${invoice.customerName}",
                        color = DueRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                // Format 1: 58mm / 80mm Thermal Receipt
                Button(
                    onClick = { PrintAndShareManager.printInvoice(context, invoice, shop, isThermal = true) },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenDark),
                    modifier = Modifier.fillMaxWidth().testTag("print_thermal_btn")
                ) {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Print 58mm Thermal Receipt")
                }

                // Format 2: Full A4 Invoice PDF
                OutlinedButton(
                    onClick = { PrintAndShareManager.printInvoice(context, invoice, shop, isThermal = false) },
                    modifier = Modifier.fillMaxWidth().testTag("print_a4_btn")
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Print Standard A4 Invoice")
                }

                // Format 3: WhatsApp Digital Memo
                Button(
                    onClick = { PrintAndShareManager.shareViaWhatsApp(context, formattedMemo, invoice.customerPhone) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    modifier = Modifier.fillMaxWidth().testTag("share_whatsapp_btn")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send WhatsApp Memo", color = Color.White)
                }

                // SMS Memo Option
                if (invoice.customerPhone.isNotBlank()) {
                    OutlinedButton(
                        onClick = { PrintAndShareManager.shareViaSms(context, invoice.customerPhone, formattedMemo) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Send via SMS to Customer")
                    }
                }

                // bKash quick dialer
                if (shop.bkashNumber.isNotBlank()) {
                    OutlinedButton(
                        onClick = { PrintAndShareManager.dialBkash(context, shop.bkashNumber) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BkashPink),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("bKash Payment (*247#) to ${shop.bkashNumber}")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
