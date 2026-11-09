package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ProductEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.DueRed
import com.example.ui.theme.DueRedContainer
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.PaidGreen
import com.example.ui.theme.PaidGreenContainer
import com.example.ui.theme.WarningAmber

@Composable
fun StockScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var filterLowStockOnly by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showImportDocDialog by remember { mutableStateOf(false) }

    val filteredProducts = remember(products, searchQuery, filterLowStockOnly) {
        products.filter { p ->
            val matchesQuery = searchQuery.isBlank() ||
                p.name.contains(searchQuery, ignoreCase = true) ||
                p.nameBn.contains(searchQuery, ignoreCase = true) ||
                p.barcode.contains(searchQuery)
            val matchesStock = !filterLowStockOnly || (p.stockQty <= p.lowStockThreshold)
            matchesQuery && matchesStock
        }
    }

    val lowStockCount = remember(products) {
        products.count { it.stockQty <= it.lowStockThreshold }
    }
    val totalInventoryValue = remember(products) {
        products.sumOf { it.stockQty * it.purchasePrice }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Inventory Value & Low Stock Alert Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldGreenDark),
                modifier = Modifier.fillMaxWidth().testTag("inventory_summary_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Stock Purchase Value", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                        Text(
                            "৳${"%.1f".format(totalInventoryValue)}",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Items in Stock", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                        Text(
                            "${products.size} Products",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Google Docs Importer Button & Low stock filter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = filterLowStockOnly,
                    onClick = { filterLowStockOnly = !filterLowStockOnly },
                    label = { Text("Low Stock Alert ($lowStockCount)") },
                    leadingIcon = {
                        if (lowStockCount > 0) Icon(Icons.Default.Warning, contentDescription = null, tint = WarningAmber)
                    }
                )

                OutlinedButton(
                    onClick = { showImportDocDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("import_doc_btn")
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Import from Google Doc", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().testTag("stock_search_input"),
                placeholder = { Text("Search products, barcode...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Products List
            if (filteredProducts.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No stock items found")
                        Text("Tap (+) to add or import from Docs", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        ProductStockCard(
                            product = product,
                            onEdit = { productToEdit = product },
                            onStockIn = { viewModel.adjustStock(product.id, 10.0) },
                            onStockOut = { viewModel.adjustStock(product.id, -1.0) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = EmeraldGreenPrimary,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).testTag("add_product_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Product")
        }
    }

    // Add / Edit Product Dialog
    if (showAddDialog || productToEdit != null) {
        val editing = productToEdit
        AddEditProductDialog(
            initial = editing,
            onDismiss = {
                showAddDialog = false
                productToEdit = null
            },
            onSave = { id, name, nameBn, barcode, purchase, selling, stock, unit, lowStock ->
                viewModel.saveProduct(id, name, nameBn, barcode, purchase, selling, stock, unit, lowStock)
                showAddDialog = false
                productToEdit = null
            }
        )
    }

    // Google Docs Text Importer Dialog
    if (showImportDocDialog) {
        ImportGoogleDocDialog(
            onDismiss = { showImportDocDialog = false },
            onImport = { rawText ->
                viewModel.importGoogleDocText(rawText) { count ->
                    showImportDocDialog = false
                    Toast.makeText(context, "Successfully imported $count products from document!", Toast.LENGTH_LONG).show()
                }
            }
        )
    }
}

@Composable
fun ProductStockCard(
    product: ProductEntity,
    onEdit: () -> Unit,
    onStockIn: () -> Unit,
    onStockOut: () -> Unit
) {
    val isLowStock = product.stockQty <= product.lowStockThreshold
    val profit = product.sellingPrice - product.purchasePrice
    val profitMargin = if (product.purchasePrice > 0) (profit / product.purchasePrice) * 100 else 0.0

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().testTag("stock_item_${product.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (product.nameBn.isNotBlank()) {
                        Text(product.nameBn, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    if (product.barcode.isNotBlank()) {
                        Text("Barcode: ${product.barcode}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }

                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Product", modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pricing & Margins
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Purchase: ৳${product.purchasePrice}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    Text("Ceiling Sale: ৳${product.sellingPrice}", fontWeight = FontWeight.Bold, color = EmeraldGreenPrimary)
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = PaidGreenContainer
                ) {
                    Text(
                        text = "+${"%.1f".format(profitMargin)}% Profit",
                        color = PaidGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Current Stock & Quick Buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onStockOut,
                        modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Stock Out", modifier = Modifier.size(16.dp))
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text(
                            text = "${if (product.stockQty % 1.0 == 0.0) product.stockQty.toInt() else product.stockQty}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = if (isLowStock) DueRed else MaterialTheme.colorScheme.onSurface
                        )
                        Text(product.unit, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                    }

                    IconButton(
                        onClick = onStockIn,
                        modifier = Modifier.size(32.dp).background(EmeraldGreenPrimary, CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Stock In", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditProductDialog(
    initial: ProductEntity?,
    onDismiss: () -> Unit,
    onSave: (id: Long, name: String, nameBn: String, barcode: String, purchase: Double, selling: Double, stock: Double, unit: String, lowStock: Double) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var nameBn by remember { mutableStateOf(initial?.nameBn ?: "") }
    var barcode by remember { mutableStateOf(initial?.barcode ?: "") }
    var purchase by remember { mutableStateOf(initial?.purchasePrice?.toString() ?: "") }
    var selling by remember { mutableStateOf(initial?.sellingPrice?.toString() ?: "") }
    var stock by remember { mutableStateOf(initial?.stockQty?.toString() ?: "10") }
    var unit by remember { mutableStateOf(initial?.unit ?: "pc") }
    var lowStock by remember { mutableStateOf(initial?.lowStockThreshold?.toString() ?: "5") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Product (পণ্য যোগ করুন)" else "Edit Product") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name (English) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nameBn,
                    onValueChange = { nameBn = it },
                    label = { Text("Product Name (বাংলা)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = purchase,
                        onValueChange = { purchase = it },
                        label = { Text("Purchase Price ৳ *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = selling,
                        onValueChange = { selling = it },
                        label = { Text("Ceiling Sale Price ৳ *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stock,
                        onValueChange = { stock = it },
                        label = { Text("Stock Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit (pc/kg/ltr)") },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Barcode / Code (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = purchase.toDoubleOrNull() ?: 0.0
                    val s = selling.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && s > 0) {
                        onSave(
                            initial?.id ?: 0L,
                            name,
                            nameBn,
                            barcode,
                            p,
                            s,
                            stock.toDoubleOrNull() ?: 0.0,
                            unit,
                            lowStock.toDoubleOrNull() ?: 5.0
                        )
                    }
                },
                enabled = name.isNotBlank() && (selling.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Save Product")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ImportGoogleDocDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    var rawText by remember {
        mutableStateOf(
            "Miniket Rice, 68, 75, 120, kg\nWhite Sugar, 130, 142, 80, kg\nSoybean Oil (1L), 175, 188, 45, ltr\nDal Masur, 125, 140, 60, kg"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Description, contentDescription = null, tint = EmeraldGreenPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import from Google Docs / Text")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Paste your catalog lines from Google Docs, Google Keep, or Sheets:\nFormat: Name, PurchasePrice, SellingPrice, Stock, Unit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    placeholder = { Text("Product, Purchase, Sale, Qty, Unit") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onImport(rawText) },
                enabled = rawText.isNotBlank()
            ) {
                Text("Import to Database")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
