package com.example.ui.screens.vouchers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import com.example.data.model.Voucher
import com.example.ui.MainViewModel
import com.example.ui.util.BackupHelper
import com.example.ui.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VouchersScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val allVouchers by viewModel.allVouchers.collectAsStateWithLifecycle()
    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Receipt (قبض), 1 = Payment (صرف)
    var showAddDialog by remember { mutableStateOf(false) }

    val receipts = remember(allVouchers) { allVouchers.filter { it.voucherType == "RECEIPT" } }
    val payments = remember(allVouchers) { allVouchers.filter { it.voucherType == "PAYMENT" } }

    val totalReceipts = receipts.sumOf { it.amount }
    val totalPayments = payments.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("السندات المالية (قبض وصرف)") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = if (selectedTab == 0) Color(0xFF15803D) else Color(0xFFB45309),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة سند")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(14.dp)
        ) {
            // Header Stats
            Row(modifier = Modifier.fillMaxWidth()) {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "إجمالي المقبوضات", fontSize = 11.sp)
                        Text(
                            text = Formatters.formatMoney(totalReceipts, settings),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF15803D)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "إجمالي المدفوعات", fontSize = 11.sp)
                        Text(
                            text = Formatters.formatMoney(totalPayments, settings),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFFB45309)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("سندات القبض (${receipts.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("سندات الصرف (${payments.size})") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val currentList = if (selectedTab == 0) receipts else payments

            if (currentList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (selectedTab == 0) "لا توجد سندات قبض مسجلة" else "لا توجد سندات صرف مسجلة",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(currentList) { voucher ->
                        VoucherItemCard(
                            voucher = voucher,
                            settings = settings,
                            onShare = {
                                val text = """
                                    سند ${if (voucher.voucherType == "RECEIPT") "قبض" else "صرف"}
                                    رقم السند: ${voucher.voucherNumber}
                                    التاريخ: ${Formatters.formatDate(voucher.createdAt)}
                                    الطرف: ${voucher.partyName}
                                    المبلغ: ${Formatters.formatMoney(voucher.amount, settings)}
                                    طريقة الدفع: ${voucher.paymentMethod}
                                    البيان: ${voucher.description}
                                    الموظف: ${voucher.createdBy}
                                """.trimIndent()
                                BackupHelper.shareText(context, text, "سند ${voucher.voucherNumber}")
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(70.dp)) }
                }
            }
        }
    }

    // Add Voucher Dialog
    if (showAddDialog) {
        var partyType by remember { mutableStateOf(if (selectedTab == 0) "CUSTOMER" else "SUPPLIER") }
        var partyName by remember { mutableStateOf("") }
        var selectedPartyId by remember { mutableStateOf<Long?>(null) }
        var amount by remember { mutableStateOf("") }
        var paymentMethod by remember { mutableStateOf("نقداً") }
        var description by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (selectedTab == 0) "إصدار سند قبض جديد" else "إصدار سند صرف جديد") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Party Type
                    Row(modifier = Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = partyType == "CUSTOMER",
                            onClick = { partyType = "CUSTOMER"; partyName = ""; selectedPartyId = null },
                            label = { Text("عميل") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        FilterChip(
                            selected = partyType == "SUPPLIER",
                            onClick = { partyType = "SUPPLIER"; partyName = ""; selectedPartyId = null },
                            label = { Text("مورد") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        FilterChip(
                            selected = partyType == "OTHER",
                            onClick = { partyType = "OTHER"; partyName = ""; selectedPartyId = null },
                            label = { Text("أخرى") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (partyType == "CUSTOMER") {
                        var expanded by remember { mutableStateOf(false) }
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = if (partyName.isNotBlank()) partyName else "اختر العميل...")
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            customers.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text("${c.name} (رصيد: ${Formatters.formatMoney(c.currentBalance, settings)})") },
                                    onClick = {
                                        partyName = c.name
                                        selectedPartyId = c.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    } else if (partyType == "SUPPLIER") {
                        var expanded by remember { mutableStateOf(false) }
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = if (partyName.isNotBlank()) partyName else "اختر المورد...")
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            suppliers.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text("${s.name} (مستحق: ${Formatters.formatMoney(s.currentBalance, settings)})") },
                                    onClick = {
                                        partyName = s.name
                                        selectedPartyId = s.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = partyName,
                            onValueChange = { partyName = it },
                            label = { Text("اسم الجهة / الشخص *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("المبلغ *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = { paymentMethod = it },
                        label = { Text("طريقة الدفع (نقداً، شبكة، تحويل)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("البيان / تفاصيل السند") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amount.toDoubleOrNull() ?: 0.0
                        if (amt > 0 && partyName.isNotBlank()) {
                            if (selectedTab == 0) {
                                viewModel.createReceiptVoucher(
                                    partyType = partyType,
                                    partyId = selectedPartyId,
                                    partyName = partyName.trim(),
                                    amount = amt,
                                    paymentMethod = paymentMethod.trim(),
                                    description = description.trim(),
                                    notes = "",
                                    onSuccess = { showAddDialog = false }
                                )
                            } else {
                                viewModel.createPaymentVoucher(
                                    partyType = partyType,
                                    partyId = selectedPartyId,
                                    partyName = partyName.trim(),
                                    amount = amt,
                                    paymentMethod = paymentMethod.trim(),
                                    description = description.trim(),
                                    notes = "",
                                    onSuccess = { showAddDialog = false }
                                )
                            }
                        } else {
                            viewModel.showMessage("يرجى إدخال البيانات المطلوبة")
                        }
                    }
                ) {
                    Text("حفظ السند")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
fun VoucherItemCard(
    voucher: Voucher,
    settings: com.example.data.model.StoreSettings?,
    onShare: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (voucher.voucherType == "RECEIPT") Color(0xFF15803D).copy(alpha = 0.15f)
                        else Color(0xFFB45309).copy(alpha = 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (voucher.voucherType == "RECEIPT") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = if (voucher.voucherType == "RECEIPT") Color(0xFF15803D) else Color(0xFFB45309),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${voucher.voucherNumber} • ${voucher.partyName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "${voucher.paymentMethod} • ${Formatters.formatDate(voucher.createdAt)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (voucher.description.isNotBlank()) {
                    Text(
                        text = voucher.description,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Formatters.formatMoney(voucher.amount, settings),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (voucher.voucherType == "RECEIPT") Color(0xFF15803D) else Color(0xFFB45309)
                )
                IconButton(onClick = onShare, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
