package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.data.local.ShopProfileEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.BkashPink
import com.example.ui.theme.DueRed
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.PaidGreen
import com.example.ui.theme.PaidGreenContainer
import com.example.util.PrintAndShareManager

@Composable
fun SettingsAndReportsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val profile by viewModel.shopProfile.collectAsStateWithLifecycle()
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    val currentUser by viewModel.authManager.currentUser.collectAsStateWithLifecycle()
    val isSigningIn by viewModel.authManager.isSigningIn.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    var shopName by remember(profile) { mutableStateOf(profile.shopName) }
    var ownerName by remember(profile) { mutableStateOf(profile.ownerName) }
    var phone by remember(profile) { mutableStateOf(profile.phone) }
    var bkashNumber by remember(profile) { mutableStateOf(profile.bkashNumber) }
    var bkashType by remember(profile) { mutableStateOf(profile.bkashType) }
    var address by remember(profile) { mutableStateOf(profile.address) }
    var receiptFooter by remember(profile) { mutableStateOf(profile.receiptFooter) }

    // Sales metrics
    val totalSales = remember(invoices) { invoices.sumOf { it.totalAmount } }
    val cashSales = remember(invoices) { invoices.filter { it.paymentMode == "CASH" }.sumOf { it.paidAmount } }
    val bkashSales = remember(invoices) { invoices.filter { it.paymentMode == "BKASH" }.sumOf { it.paidAmount } }
    val dueSales = remember(invoices) { invoices.sumOf { it.dueAmount } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sales Summary Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = EmeraldGreenDark),
            modifier = Modifier.fillMaxWidth().testTag("sales_report_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Total Business Sales (মোট বিক্রয়)", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                Text(
                    "৳${"%.2f".format(totalSales)}",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Cash (নগদ)", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                        Text("৳${"%.1f".format(cashSales)}", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("bKash (বিকাশ)", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                        Text("৳${"%.1f".format(bkashSales)}", color = Color(0xFFFF80AB), fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Due (বাকি বিক্রয়)", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                        Text("৳${"%.1f".format(dueSales)}", color = Color(0xFFFFD1D1), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Firebase Auth & Cloud Sync Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth().testTag("auth_sync_card")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = EmeraldGreenPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cloud Sync & Account", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }

                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (currentUser != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Signed In with Google:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(currentUser?.displayName ?: "User", fontWeight = FontWeight.Bold)
                            Text(currentUser?.email ?: "", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        }

                        Button(
                            onClick = { viewModel.triggerCloudSync() },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                            modifier = Modifier.testTag("sync_now_btn")
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync Now")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.authManager.signOut() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sign Out")
                    }
                } else {
                    Text(
                        "Sign in with Google to backup your invoices, products, and khata ledger to Firestore Cloud.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (activity != null) viewModel.authManager.signInWithGoogle(activity)
                        },
                        enabled = !isSigningIn,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("sign_in_google_btn")
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.AccountCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign in with Google")
                        }
                    }
                }
            }
        }

        // Offline Mode & Privacy Badge
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = PaidGreenContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = PaidGreen)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Offline-First & Local Storage Active", fontWeight = FontWeight.Bold, color = PaidGreen)
                    Text("All data is stored locally in SQLite Room DB. Works without internet.", fontSize = 11.sp, color = Color(0xFF065F46))
                }
            }
        }

        // Google Drive Backup & Export
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Google Drive Data Backup", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("Export complete ledger, invoices, and product data as JSON to Google Drive.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { if (activity != null) viewModel.exportBackup(activity) },
                    modifier = Modifier.fillMaxWidth().testTag("export_backup_btn")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Backup & Export to Google Drive")
                }
            }
        }

        // Shop Profile & bKash Setup Form
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Store, contentDescription = null, tint = EmeraldGreenPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Shop Profile & bKash Setup", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }

                OutlinedTextField(
                    value = shopName,
                    onValueChange = { shopName = it },
                    label = { Text("Shop Name (দোকানের নাম)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("settings_shop_name")
                )

                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("Proprietor / Owner Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Shop Contact Phone") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // bKash Number & Type
                OutlinedTextField(
                    value = bkashNumber,
                    onValueChange = { bkashNumber = it },
                    label = { Text("bKash Number for Customer Payments") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("settings_bkash_number")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = bkashType == "PERSONAL",
                        onClick = { bkashType = "PERSONAL" },
                        label = { Text("Personal bKash (Send Money)") }
                    )
                    FilterChip(
                        selected = bkashType == "MERCHANT",
                        onClick = { bkashType = "MERCHANT" },
                        label = { Text("Merchant bKash (Make Payment)") }
                    )
                }

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Shop Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = receiptFooter,
                    onValueChange = { receiptFooter = it },
                    label = { Text("Printed Receipt Footer Note") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        viewModel.updateShopProfile(
                            ShopProfileEntity(
                                id = 1,
                                shopName = shopName,
                                ownerName = ownerName,
                                phone = phone,
                                bkashNumber = bkashNumber,
                                bkashType = bkashType,
                                address = address,
                                receiptFooter = receiptFooter
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                    modifier = Modifier.fillMaxWidth().testTag("save_settings_btn")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Store Settings")
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
