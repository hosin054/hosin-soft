package com.example.ui.screens.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.ui.MainViewModel
import com.example.ui.util.BackupHelper
import com.example.ui.util.Formatters
import com.example.ui.util.InvoicePrinter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailScreen(
    invoiceId: Long,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val allInvoices by viewModel.allInvoices.collectAsStateWithLifecycle()
    val allItems by viewModel.allInvoiceItems.collectAsStateWithLifecycle()

    val invoice = allInvoices.find { it.id == invoiceId }
    val items = allItems.filter { it.invoiceId == invoiceId }

    var showCancelDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(invoice?.invoiceNumber ?: "تفاصيل الفاتورة") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    if (invoice != null && invoice.status != "CANCELLED") {
                        IconButton(onClick = {
                            val text = InvoicePrinter.generateReceiptText(invoice, items, settings)
                            BackupHelper.shareText(context, text, "فاتورة ${invoice.invoiceNumber}")
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "مشاركة / طباعة", tint = MaterialTheme.colorScheme.onPrimary)
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
        if (invoice == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("لم يتم العثور على الفاتورة")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header card
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (invoice.status == "CANCELLED") MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (invoice.invoiceType == "SALE") "فاتورة مبيعات" else "فاتورة مشتريات",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = if (invoice.status == "CANCELLED") "ملغاة" else "مكتملة",
                                    fontWeight = FontWeight.Bold,
                                    color = if (invoice.status == "CANCELLED") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "رقم الفاتورة: ${invoice.invoiceNumber}", fontSize = 13.sp)
                            Text(text = "التاريخ: ${Formatters.formatDate(invoice.createdAt)}", fontSize = 13.sp)
                            Text(text = "الطرف: ${invoice.partyName}", fontSize = 13.sp)
                            Text(text = "طريقة الدفع: ${if (invoice.paymentType == "CASH") "نقداً" else "آجل"}", fontSize = 13.sp)
                            Text(text = "المستخدم: ${invoice.createdBy}", fontSize = 13.sp)
                            if (invoice.notes.isNotBlank()) {
                                Text(text = "ملاحظات: ${invoice.notes}", fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Items list header
                item {
                    Text(text = "الأصناف المحتواة في الفاتورة", fontWeight = FontWeight.Bold)
                }

                items(items) { item ->
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
                                Text(text = item.productName, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${item.quantity} ${item.unitName} × ${Formatters.formatMoney(item.unitPrice, settings)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = Formatters.formatMoney(item.total, settings),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Financial Summary
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("المجموع الفرعي:")
                                Text(Formatters.formatMoney(invoice.subtotal, settings))
                            }
                            if (invoice.discountAmount > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("الخصم:")
                                    Text("- ${Formatters.formatMoney(invoice.discountAmount, settings)}", color = MaterialTheme.colorScheme.error)
                                }
                            }
                            if (invoice.taxAmount > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("الضريبة:")
                                    Text("+ ${Formatters.formatMoney(invoice.taxAmount, settings)}")
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("الإجمالي النهائي:", fontWeight = FontWeight.Bold)
                                Text(Formatters.formatMoney(invoice.totalAmount, settings), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("المدفوع:")
                                Text(Formatters.formatMoney(invoice.paidAmount, settings))
                            }
                            if (invoice.remainingAmount > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("المتبقي الآجل:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                    Text(Formatters.formatMoney(invoice.remainingAmount, settings), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                }
                            }
                            if (invoice.invoiceType == "SALE") {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("ربح الفاتورة المحاسبي:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(Formatters.formatMoney(invoice.profit, settings), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF15803D))
                                }
                            }
                        }
                    }
                }

                // Action buttons
                item {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                val text = InvoicePrinter.generateReceiptText(invoice, items, settings)
                                BackupHelper.shareText(context, text, "فاتورة ${invoice.invoiceNumber}")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("طباعة / مشاركة")
                        }

                        if (invoice.status != "CANCELLED") {
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { showCancelDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إلغاء الفاتورة")
                            }
                        }
                    }
                }
            }
        }
    }

    // Cancel Invoice Confirmation Dialog
    if (showCancelDialog && invoice != null) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("تأكيد إلغاء الفاتورة") },
            text = {
                Text(
                    "هل أنت متأكد من إلغاء الفاتورة ${invoice.invoiceNumber}؟\nسيتم عكس جميع الكميات المخصومة من المخزون واسترداد المبالغ المالية لحساب الصندوق والطرف المرتبط تلقائياً."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelInvoice(invoice)
                        showCancelDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("نعم، إلغاء وعكس الفاتورة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("تراجع")
                }
            }
        )
    }
}
