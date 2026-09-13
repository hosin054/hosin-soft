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
fun CustomerDetailScreen(
    customerId: Long,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val allCustomers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val allInvoices by viewModel.allInvoices.collectAsStateWithLifecycle()
    val allVouchers by viewModel.allVouchers.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    val customer = allCustomers.find { it.id == customerId }
    val customerInvoices = allInvoices.filter { it.partyId == customerId && it.invoiceType == "SALE" }
    val customerVouchers = allVouchers.filter { it.partyId == customerId && it.voucherType == "RECEIPT" }

    var showReceiveDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(customer?.name ?: "كشف حساب العميل") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    if (customer != null) {
                        IconButton(onClick = {
                            val text = StringBuilder()
                            text.append("📋 *كشف حساب العميل: ${customer.name}*\n")
                            text.append("المتجر: ${settings?.storeName ?: "حسين سوفت"}\n")
                            if (customer.phone.isNotBlank()) text.append("📱 الهاتف: ${customer.phone}\n")
                            text.append("━━━━━━━━━━━━━━━━━━━\n")
                            text.append("💰 الرصيد الحالي المستحق: ${Formatters.formatMoney(customer.currentBalance, settings)}\n")
                            text.append("🛒 إجمالي المسحوبات: ${Formatters.formatMoney(customer.totalSales, settings)}\n")
                            text.append("💵 إجمالي المدفوعات: ${Formatters.formatMoney(customer.totalPaid, settings)}\n")
                            text.append("━━━━━━━━━━━━━━━━━━━\n")
                            text.append("🚀 حسين سوفت لإدارة المتاجر")
                            BackupHelper.shareToTelegram(context, text.toString(), "كشف حساب ${customer.name}")
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال إلى تلغرام", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        IconButton(onClick = {
                            val text = StringBuilder()
                            text.append("كشف حساب العميل: ${customer.name}\n")
                            text.append("الهاتف: ${customer.phone}\n")
                            text.append("الرصيد المستحق: ${Formatters.formatMoney(customer.currentBalance, settings)}\n")
                            text.append("إجمالي المبيعات: ${Formatters.formatMoney(customer.totalSales, settings)}\n")
                            text.append("إجمالي المسدد: ${Formatters.formatMoney(customer.totalPaid, settings)}\n")
                            BackupHelper.shareText(context, text.toString(), "كشف حساب ${customer.name}")
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "مشاركة كشف الحساب", tint = MaterialTheme.colorScheme.onPrimary)
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
        if (customer == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("لم يتم العثور على بيانات العميل")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Profile & Balance Card
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
                                    Text(text = customer.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(text = "هاتف: ${customer.phone.ifBlank { "غير مسجل" }}", fontSize = 12.sp)
                                    if (customer.address.isNotBlank()) {
                                        Text(text = "العنوان: ${customer.address}", fontSize = 12.sp)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "الرصيد المالي:", fontSize = 11.sp)
                                    Text(
                                        text = Formatters.formatMoney(customer.currentBalance, settings),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = if (customer.currentBalance > 0) MaterialTheme.colorScheme.error else Color(0xFF15803D)
                                    )
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "إجمالي المبيعات: ${Formatters.formatMoney(customer.totalSales, settings)}", fontSize = 11.sp)
                                Text(text = "إجمالي المسدد: ${Formatters.formatMoney(customer.totalPaid, settings)}", fontSize = 11.sp)
                            }
                            if (customer.creditLimit > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "الحد الائتماني: ${Formatters.formatMoney(customer.creditLimit, settings)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { showReceiveDialog = true },
                                enabled = currentUser.canManageCustomers,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (currentUser.canManageCustomers) "تسجيل سند قبض دفعة من العميل" else "تسجيل سند قبض (غير مصرح)")
                            }
                        }
                    }
                }

                // Invoices History
                item {
                    Text(text = "فواتير المبيعات للعميل (${customerInvoices.size})", fontWeight = FontWeight.Bold)
                }

                if (customerInvoices.isEmpty()) {
                    item {
                        Text(text = "لا توجد فواتير مبيعات مسجلة لهذا العميل", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(customerInvoices) { inv ->
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

                // Receipt Vouchers History
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "سندات القبض المسددة (${customerVouchers.size})", fontWeight = FontWeight.Bold)
                }

                if (customerVouchers.isEmpty()) {
                    item {
                        Text(text = "لا توجد سندات قبض مسجلة لهذا العميل", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(customerVouchers) { v ->
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
                                    Text(text = "${v.description.ifBlank { "قبض دفعة حساب" }} • ${Formatters.formatDate(v.createdAt)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = Formatters.formatMoney(v.amount, settings),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF15803D)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Receive Payment Dialog
    if (showReceiveDialog && customer != null) {
        var amountInput by remember { mutableStateOf(if (customer.currentBalance > 0) customer.currentBalance.toString() else "0.0") }
        var paymentMethod by remember { mutableStateOf("نقداً") }
        var notes by remember { mutableStateOf("سداد دفعة من الحساب") }

        AlertDialog(
            onDismissRequest = { showReceiveDialog = false },
            title = { Text("سند قبض من ${customer.name}") },
            text = {
                Column {
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("المبلغ المقبوض *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = { paymentMethod = it },
                        label = { Text("طريقة الدفع (نقداً، تحويل بنكي، شبكة)") },
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
                            viewModel.createReceiptVoucher(
                                partyType = "CUSTOMER",
                                partyId = customer.id,
                                partyName = customer.name,
                                amount = amt,
                                paymentMethod = paymentMethod.trim(),
                                description = notes.trim(),
                                notes = "",
                                onSuccess = {
                                    showReceiveDialog = false
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
                TextButton(onClick = { showReceiveDialog = false }) { Text("إلغاء") }
            }
        )
    }
}
