package com.example.ui.screens.parties

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.util.BackupHelper
import com.example.ui.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierDetailScreen(
    supplierId: Long,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val allSuppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()
    val allInvoices by viewModel.allInvoices.collectAsStateWithLifecycle()
    val allVouchers by viewModel.allVouchers.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    val supplier = allSuppliers.find { it.id == supplierId }
    val supplierPurchases = allInvoices.filter { it.partyId == supplierId && it.invoiceType == "PURCHASE" }
    val supplierPayments = allVouchers.filter { it.partyId == supplierId && it.voucherType == "PAYMENT" }

    var showPayDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(supplier?.name ?: "كشف حساب المورد") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    if (supplier != null) {
                        IconButton(onClick = {
                            val text = StringBuilder()
                            text.append("📋 *كشف حساب المورد: ${supplier.name}*\n")
                            text.append("المتجر: ${settings?.storeName ?: "حسين سوفت"}\n")
                            if (supplier.phone.isNotBlank()) text.append("📱 الهاتف: ${supplier.phone}\n")
                            text.append("━━━━━━━━━━━━━━━━━━━\n")
                            text.append("💰 الرصيد المستحق له: ${Formatters.formatMoney(supplier.currentBalance, settings)}\n")
                            text.append("🛒 إجمالي التوريدات (المشتريات): ${Formatters.formatMoney(supplier.totalPurchases, settings)}\n")
                            text.append("💵 إجمالي المسدد له: ${Formatters.formatMoney(supplier.totalPaid, settings)}\n")
                            text.append("━━━━━━━━━━━━━━━━━━━\n")
                            text.append("🚀 حسين سوفت لإدارة المتاجر")
                            BackupHelper.shareToTelegram(context, text.toString(), "كشف حساب مورد ${supplier.name}")
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال إلى تلغرام", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        IconButton(onClick = {
                            val text = StringBuilder()
                            text.append("كشف حساب المورد: ${supplier.name}\n")
                            text.append("الهاتف: ${supplier.phone}\n")
                            text.append("الرصيد المستحق له: ${Formatters.formatMoney(supplier.currentBalance, settings)}\n")
                            text.append("إجمالي المشتريات: ${Formatters.formatMoney(supplier.totalPurchases, settings)}\n")
                            text.append("إجمالي المسدد له: ${Formatters.formatMoney(supplier.totalPaid, settings)}\n")
                            BackupHelper.shareText(context, text.toString(), "كشف حساب مورد ${supplier.name}")
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        if (supplier == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("لم يتم العثور على بيانات المورد")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Profile Card
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = supplier.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(text = "هاتف: ${supplier.phone.ifBlank { "غير مسجل" }}", fontSize = 12.sp)
                                    if (supplier.address.isNotBlank()) {
                                        Text(text = "العنوان: ${supplier.address}", fontSize = 12.sp)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "المستحق له:", fontSize = 11.sp)
                                    Text(
                                        text = Formatters.formatMoney(supplier.currentBalance, settings),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = if (supplier.currentBalance > 0) MaterialTheme.colorScheme.error else Color(0xFF15803D)
                                    )
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "إجمالي المشتريات: ${Formatters.formatMoney(supplier.totalPurchases, settings)}", fontSize = 11.sp)
                                Text(text = "إجمالي المسدد له: ${Formatters.formatMoney(supplier.totalPaid, settings)}", fontSize = 11.sp)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { showPayDialog = true },
                                enabled = currentUser.canManageSuppliers,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (currentUser.canManageSuppliers) "تسجيل سند صرف دفعة للمورد" else "تسجيل سند صرف (غير مصرح)")
                            }
                        }
                    }
                }

                // Purchases History
                item {
                    Text(text = "فواتير المشتريات (${supplierPurchases.size})", fontWeight = FontWeight.Bold)
                }

                if (supplierPurchases.isEmpty()) {
                    item {
                        Text(text = "لا توجد فواتير مشتريات مسجلة لهذا المورد", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(supplierPurchases) { inv ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = inv.invoiceNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(text = "${if (inv.paymentType == "CASH") "نقدي" else "آجل"} • ${Formatters.formatDate(inv.createdAt)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = Formatters.formatMoney(inv.totalAmount, settings), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    if (inv.remainingAmount > 0) {
                                        Text(text = "المتبقي: ${Formatters.formatMoney(inv.remainingAmount, settings)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }

                // Payment Vouchers History
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "سندات الصرف المدفوعة للمورد (${supplierPayments.size})", fontWeight = FontWeight.Bold)
                }

                if (supplierPayments.isEmpty()) {
                    item {
                        Text(text = "لا توجد سندات صرف مسجلة لهذا المورد", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(supplierPayments) { v ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "${v.voucherNumber} - ${v.paymentMethod}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(text = "${v.description.ifBlank { "صرف دفعة حساب" }} • ${Formatters.formatDate(v.createdAt)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = Formatters.formatMoney(v.amount, settings),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Payment Dialog
    if (showPayDialog && supplier != null) {
        var amountInput by remember { mutableStateOf(if (supplier.currentBalance > 0) supplier.currentBalance.toString() else "0.0") }
        var paymentMethod by remember { mutableStateOf("نقداً") }
        var notes by remember { mutableStateOf("سداد دفعة للمورد") }

        AlertDialog(
            onDismissRequest = { showPayDialog = false },
            title = { Text("سند صرف إلى ${supplier.name}") },
            text = {
                Column {
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("المبلغ المدفوع *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = { paymentMethod = it },
                        label = { Text("طريقة الصرف (نقداً من الصندوق، تحويل بنكي)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("البيان / ملاحظات") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountInput.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            viewModel.createPaymentVoucher(
                                partyType = "SUPPLIER",
                                partyId = supplier.id,
                                partyName = supplier.name,
                                amount = amt,
                                paymentMethod = paymentMethod.trim(),
                                description = notes.trim(),
                                notes = "",
                                onSuccess = {
                                    showPayDialog = false
                                }
                            )
                        } else {
                            viewModel.showMessage("المبلغ غير صحيح")
                        }
                    }
                ) {
                    Text("إصدار السند")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPayDialog = false }) { Text("إلغاء") }
            }
        )
    }
}
