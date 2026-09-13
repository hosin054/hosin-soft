package com.example.ui.screens.accounting

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Customer
import com.example.data.model.JournalVoucher
import com.example.data.model.JournalVoucherLine
import com.example.data.model.Supplier
import com.example.ui.MainViewModel
import com.example.ui.util.BackupHelper
import com.example.ui.util.Formatters

private data class PredefinedAccount(
    val code: String,
    val name: String,
    val partyType: String
)

private val STANDARD_ACCOUNTS = listOf(
    PredefinedAccount("101", "الصندوق الرئيسي (نقدية بالصندوق)", "CASH"),
    PredefinedAccount("102", "البنك والمدفوعات الإلكترونية / شبكة", "BANK"),
    PredefinedAccount("103", "ذمم العملاء (مدينون)", "CUSTOMER"),
    PredefinedAccount("104", "مخزون البضاعة (بضاعة آخر المدة)", "GENERAL"),
    PredefinedAccount("105", "الأصول الثابتة وتجهيزات المحل", "GENERAL"),
    PredefinedAccount("201", "ذمم الموردين (دائنون)", "SUPPLIER"),
    PredefinedAccount("202", "قروض والتزامات دائنة أخرى", "GENERAL"),
    PredefinedAccount("301", "رأس المال المستثمر", "CAPITAL"),
    PredefinedAccount("302", "جاري المالك / مسحوبات شخصية", "GENERAL"),
    PredefinedAccount("401", "إيرادات المبيعات", "REVENUE"),
    PredefinedAccount("403", "إيرادات وأرباح متنوعة أخرى", "REVENUE"),
    PredefinedAccount("501", "تكلفة البضاعة المباعة", "EXPENSE"),
    PredefinedAccount("502", "مصاريف الإيجار", "EXPENSE"),
    PredefinedAccount("503", "مصاريف الرواتب والأجور", "EXPENSE"),
    PredefinedAccount("504", "مصاريف الكهرباء والمياه والإنترنت", "EXPENSE"),
    PredefinedAccount("505", "مصاريف النقل والشحن والوقود", "EXPENSE"),
    PredefinedAccount("506", "مصاريف الصيانة والتشغيل", "EXPENSE"),
    PredefinedAccount("507", "مصروفات إدارية وعمومية أخرى", "EXPENSE")
)

private data class TempLineState(
    var accountCode: String = "101",
    var accountName: String = "الصندوق الرئيسي (نقدية بالصندوق)",
    var partyType: String = "CASH",
    var partyId: Long? = null,
    var debitStr: String = "",
    var creditStr: String = "",
    var description: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalVouchersScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val vouchers by viewModel.allJournalVouchers.collectAsState()
    val allLines by viewModel.allJournalVoucherLines.collectAsState()
    val customers by viewModel.allCustomers.collectAsState()
    val suppliers by viewModel.allSuppliers.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var voucherToDelete by remember { mutableStateOf<JournalVoucher?>(null) }
    var expandedVoucherId by remember { mutableStateOf<Long?>(null) }

    val filteredVouchers = remember(vouchers, searchQuery) {
        if (searchQuery.isBlank()) vouchers
        else vouchers.filter {
            it.voucherNumber.contains(searchQuery, ignoreCase = true) ||
            it.narration.contains(searchQuery, ignoreCase = true) ||
            it.reference.contains(searchQuery, ignoreCase = true)
        }
    }

    val totalDebits = vouchers.sumOf { it.totalDebit }
    val totalCredits = vouchers.sumOf { it.totalCredit }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("سندات القيد اليومية", fontWeight = FontWeight.Bold)
                        Text("القيود المحاسبية المزدوجة والتسويات", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val report = StringBuilder()
                        report.append("📑 *سجل سندات القيد المحاسبية - ${settings?.storeName ?: "حسين سوفت"}*\n")
                        report.append("عدد السندات: ${vouchers.size}\n")
                        report.append("إجمالي المدين: ${Formatters.formatMoney(totalDebits, settings)}\n")
                        report.append("إجمالي الدائن: ${Formatters.formatMoney(totalCredits, settings)}\n")
                        report.append("━━━━━━━━━━━━━━━━━━━\n")
                        vouchers.take(15).forEach { jv ->
                            report.append("• ${jv.voucherNumber} | ${Formatters.formatDate(jv.voucherDate)} | ${Formatters.formatMoney(jv.totalDebit, settings)}\n")
                            report.append("  البيان: ${jv.narration}\n")
                        }
                        report.append("━━━━━━━━━━━━━━━━━━━\n")
                        report.append("🚀 حسين سوفت لإدارة المتاجر")
                        BackupHelper.shareToTelegram(context, report.toString(), "سجل سندات القيد")
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال إلى تلغرام", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("سند قيد جديد") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_journal_voucher_fab")
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 14.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Summary Stats Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("عدد السندات", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("${vouchers.size}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Card(
                    modifier = Modifier.weight(1.3f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("إجمالي المدين", fontSize = 11.sp, color = Color(0xFF1B5E20))
                        Text(Formatters.formatMoney(totalDebits, settings), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1B5E20))
                    }
                }
                Card(
                    modifier = Modifier.weight(1.3f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("إجمالي الدائن", fontSize = 11.sp, color = Color(0xFF0D47A1))
                        Text(Formatters.formatMoney(totalCredits, settings), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0D47A1))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("journal_search_input"),
                placeholder = { Text("بحث برقم السند أو البيان أو المرجع...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Vouchers List
            if (filteredVouchers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("لا توجد سندات قيد مسجلة", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("اضغط على 'سند قيد جديد' لإضافة قيد مزدوج", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredVouchers, key = { it.id }) { voucher ->
                        val isExpanded = expandedVoucherId == voucher.id
                        val linesForVoucher = allLines.filter { it.voucherId == voucher.id }

                        JournalVoucherCard(
                            voucher = voucher,
                            lines = linesForVoucher,
                            isExpanded = isExpanded,
                            onToggleExpand = {
                                expandedVoucherId = if (isExpanded) null else voucher.id
                            },
                            onSendTelegram = {
                                val text = StringBuilder()
                                text.append("📑 *سند قيد رقم: ${voucher.voucherNumber}*\n")
                                text.append("التاريخ: ${Formatters.formatDate(voucher.voucherDate)}\n")
                                if (voucher.reference.isNotBlank()) text.append("المرجع: ${voucher.reference}\n")
                                text.append("البيان العام: ${voucher.narration}\n")
                                text.append("المبلغ الإجمالي: ${Formatters.formatMoney(voucher.totalDebit, settings)}\n")
                                text.append("━━━━━━━━━━━━━━━━━━━\n")
                                linesForVoucher.forEach { l ->
                                    val side = if (l.debit > 0) "مدين [${Formatters.formatMoney(l.debit, settings)}]" else "دائن [${Formatters.formatMoney(l.credit, settings)}]"
                                    text.append("• ${l.accountName} - $side\n")
                                    if (l.description.isNotBlank()) text.append("  ${l.description}\n")
                                }
                                text.append("━━━━━━━━━━━━━━━━━━━\n")
                                text.append("بواسطة: ${voucher.createdBy} | حسين سوفت")
                                BackupHelper.shareToTelegram(context, text.toString(), "سند قيد ${voucher.voucherNumber}")
                            },
                            onShare = {
                                val text = StringBuilder()
                                text.append("سند قيد رقم: ${voucher.voucherNumber}\n")
                                text.append("التاريخ: ${Formatters.formatDate(voucher.voucherDate)}\n")
                                text.append("البيان: ${voucher.narration}\n")
                                text.append("الإجمالي: ${Formatters.formatMoney(voucher.totalDebit, settings)}\n\n")
                                linesForVoucher.forEach { l ->
                                    val amt = if (l.debit > 0) "منه (مدين): ${l.debit}" else "له (دائن): ${l.credit}"
                                    text.append("${l.accountName} | $amt | ${l.description}\n")
                                }
                                BackupHelper.shareText(context, text.toString(), "سند قيد ${voucher.voucherNumber}")
                            },
                            onDelete = { voucherToDelete = voucher }
                        )
                    }
                }
            }
        }
    }

    // Confirmation Dialog for Delete
    if (voucherToDelete != null) {
        val target = voucherToDelete!!
        AlertDialog(
            onDismissRequest = { voucherToDelete = null },
            title = { Text("تأكيد حذف سند القيد", fontWeight = FontWeight.Bold) },
            text = {
                Text("هل أنت متأكد من حذف سند القيد رقم (${target.voucherNumber}) بقيمة ${Formatters.formatMoney(target.totalDebit, settings)}؟ سيتم إلغاء تأثيرات هذا القيد المحاسبية على الصندوق والأرصدة.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteJournalVoucher(target)
                        voucherToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("نعم، حذف القيد")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { voucherToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Create New Journal Voucher Dialog
    if (showCreateDialog) {
        CreateJournalVoucherDialog(
            customers = customers,
            suppliers = suppliers,
            onDismiss = { showCreateDialog = false },
            onConfirm = { date, ref, narration, lines ->
                viewModel.createJournalVoucher(
                    date = date,
                    reference = ref,
                    narration = narration,
                    lines = lines,
                    onSuccess = {
                        showCreateDialog = false
                    }
                )
            }
        )
    }
}

@Composable
private fun JournalVoucherCard(
    voucher: JournalVoucher,
    lines: List<JournalVoucherLine>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSendTelegram: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() }
            .testTag("journal_card_${voucher.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = voucher.voucherNumber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = Formatters.formatDate(voucher.voucherDate),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Formatters.formatMoney(voucher.totalDebit),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = Color(0xFF1B5E20)
                    )
                    Surface(
                        color = if (voucher.isBalanced) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (voucher.isBalanced) "قيد متزن ✓" else "غير متزن ⚠",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (voucher.isBalanced) Color(0xFF2E7D32) else Color(0xFFC62828),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (voucher.narration.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "البيان: ${voucher.narration}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (voucher.reference.isNotBlank()) {
                Text(
                    text = "المرجع: ${voucher.reference}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expanded Lines View
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "تفاصيل أطراف القيد المحاسبي (${lines.size} طرف):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Ledger Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                            .padding(vertical = 6.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("الحساب / الطرف", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                        Text("مدين (منه)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20), modifier = Modifier.weight(1f))
                        Text("دائن (له)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1), modifier = Modifier.weight(1f))
                    }

                    // Ledger Lines
                    lines.forEach { line ->
                        Column(modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = line.accountName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(2f)
                                )
                                Text(
                                    text = if (line.debit > 0) Formatters.formatMoney(line.debit) else "-",
                                    fontSize = 12.sp,
                                    fontWeight = if (line.debit > 0) FontWeight.Bold else FontWeight.Normal,
                                    color = if (line.debit > 0) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = if (line.credit > 0) Formatters.formatMoney(line.credit) else "-",
                                    fontSize = 12.sp,
                                    fontWeight = if (line.credit > 0) FontWeight.Bold else FontWeight.Normal,
                                    color = if (line.credit > 0) Color(0xFF0D47A1) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (line.description.isNotBlank()) {
                                Text(
                                    text = "  • ${line.description}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Actions in expanded view
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onSendTelegram,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تيليجرام", fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedButton(
                            onClick = onShare,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مشاركة", fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Expand Hint
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "إخفاء التفاصيل ▲" else "عرض القيود التفصيلية ▼",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateJournalVoucherDialog(
    customers: List<Customer>,
    suppliers: List<Supplier>,
    onDismiss: () -> Unit,
    onConfirm: (Long, String, String, List<JournalVoucherLine>) -> Unit
) {
    var reference by remember { mutableStateOf("") }
    var narration by remember { mutableStateOf("") }
    val voucherDate by remember { mutableStateOf(System.currentTimeMillis()) }

    val lines = remember {
        mutableStateListOf(
            TempLineState(
                accountCode = "101",
                accountName = "الصندوق الرئيسي (نقدية بالصندوق)",
                partyType = "CASH",
                debitStr = "",
                creditStr = "",
                description = ""
            ),
            TempLineState(
                accountCode = "401",
                accountName = "إيرادات المبيعات",
                partyType = "REVENUE",
                debitStr = "",
                creditStr = "",
                description = ""
            )
        )
    }

    val totalDebit = lines.sumOf { it.debitStr.toDoubleOrNull() ?: 0.0 }
    val totalCredit = lines.sumOf { it.creditStr.toDoubleOrNull() ?: 0.0 }
    val difference = Math.abs(totalDebit - totalCredit)
    val isBalanced = difference < 0.001 && totalDebit > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PostAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("تسجيل سند قيد جديد", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header fields
                item {
                    OutlinedTextField(
                        value = narration,
                        onValueChange = { narration = it },
                        label = { Text("البيان العام للقيد *") },
                        placeholder = { Text("مثال: إيداع رأس مال، سداد ذمم، شراء أصل ثابت...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("journal_narration_input"),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = reference,
                        onValueChange = { reference = it },
                        label = { Text("رقم المرجع / السند اليدوي (اختياري)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Balance Tracker Header
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isBalanced) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("إجمالي المدين: ${Formatters.formatMoney(totalDebit)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                                Text("إجمالي الدائن: ${Formatters.formatMoney(totalCredit)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isBalanced) "✓ القيد متزن تماماً" else "⚠ الفرق: ${Formatters.formatMoney(difference)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isBalanced) Color(0xFF2E7D32) else Color(0xFFE65100)
                                )
                                if (!isBalanced && difference > 0.0) {
                                    TextButton(
                                        onClick = {
                                            // Auto balance: add or adjust last row
                                            if (totalDebit > totalCredit) {
                                                lines.add(
                                                    TempLineState(
                                                        accountCode = "102",
                                                        accountName = "البنك والمدفوعات الإلكترونية / شبكة",
                                                        partyType = "BANK",
                                                        creditStr = Formatters.formatRaw(totalDebit - totalCredit)
                                                    )
                                                )
                                            } else {
                                                lines.add(
                                                    TempLineState(
                                                        accountCode = "101",
                                                        accountName = "الصندوق الرئيسي (نقدية بالصندوق)",
                                                        partyType = "CASH",
                                                        debitStr = Formatters.formatRaw(totalCredit - totalDebit)
                                                    )
                                                )
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("موازنة تلقائية", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Line items
                items(lines.size) { index ->
                    val line = lines[index]
                    LineItemEditor(
                        index = index,
                        line = line,
                        customers = customers,
                        suppliers = suppliers,
                        canDelete = lines.size > 2,
                        onDelete = { lines.removeAt(index) },
                        onUpdate = { updated -> lines[index] = updated }
                    )
                }

                // Add line button
                item {
                    Button(
                        onClick = {
                            lines.add(
                                TempLineState(
                                    accountCode = "507",
                                    accountName = "مصروفات إدارية وعمومية أخرى",
                                    partyType = "EXPENSE"
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ إضافة طرف قيد آخر")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (narration.isBlank()) {
                        narration = "قيد يومية تسوية"
                    }
                    val finalLines = lines.map {
                        JournalVoucherLine(
                            voucherId = 0,
                            accountCode = it.accountCode,
                            accountName = it.accountName,
                            partyType = it.partyType,
                            partyId = it.partyId,
                            debit = it.debitStr.toDoubleOrNull() ?: 0.0,
                            credit = it.creditStr.toDoubleOrNull() ?: 0.0,
                            description = it.description
                        )
                    }
                    onConfirm(voucherDate, reference, narration, finalLines)
                },
                enabled = isBalanced,
                modifier = Modifier.testTag("save_journal_voucher_button")
            ) {
                Text("حفظ وترحيل القيد")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LineItemEditor(
    index: Int,
    line: TempLineState,
    customers: List<Customer>,
    suppliers: List<Supplier>,
    canDelete: Boolean,
    onDelete: () -> Unit,
    onUpdate: (TempLineState) -> Unit
) {
    var expandedAccountMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("الطرف (${index + 1})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                if (canDelete) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "حذف الطرف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Account Selector Box
            ExposedDropdownMenuBox(
                expanded = expandedAccountMenu,
                onExpandedChange = { expandedAccountMenu = !expandedAccountMenu }
            ) {
                OutlinedTextField(
                    value = line.accountName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("اختر الحساب المحاسبي") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAccountMenu) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedAccountMenu,
                    onDismissRequest = { expandedAccountMenu = false }
                ) {
                    STANDARD_ACCOUNTS.forEach { acc ->
                        DropdownMenuItem(
                            text = { Text("${acc.code} - ${acc.name}") },
                            onClick = {
                                onUpdate(
                                    line.copy(
                                        accountCode = acc.code,
                                        accountName = acc.name,
                                        partyType = acc.partyType,
                                        partyId = null
                                    )
                                )
                                expandedAccountMenu = false
                            }
                        )
                    }
                }
            }

            // Customer Selector if account is CUSTOMER
            if (line.partyType == "CUSTOMER" && customers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                var expandedCustMenu by remember { mutableStateOf(false) }
                val selectedCust = customers.find { it.id == line.partyId }
                ExposedDropdownMenuBox(
                    expanded = expandedCustMenu,
                    onExpandedChange = { expandedCustMenu = !expandedCustMenu }
                ) {
                    OutlinedTextField(
                        value = selectedCust?.name ?: "اختر العميل المحدد...",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("العميل المرتبط") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCustMenu) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCustMenu,
                        onDismissRequest = { expandedCustMenu = false }
                    ) {
                        customers.forEach { c ->
                            DropdownMenuItem(
                                text = { Text("${c.name} (رصيده: ${Formatters.formatMoney(c.currentBalance)})") },
                                onClick = {
                                    onUpdate(line.copy(partyId = c.id, accountName = "عميل: ${c.name}"))
                                    expandedCustMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Supplier Selector if account is SUPPLIER
            if (line.partyType == "SUPPLIER" && suppliers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                var expandedSuppMenu by remember { mutableStateOf(false) }
                val selectedSupp = suppliers.find { it.id == line.partyId }
                ExposedDropdownMenuBox(
                    expanded = expandedSuppMenu,
                    onExpandedChange = { expandedSuppMenu = !expandedSuppMenu }
                ) {
                    OutlinedTextField(
                        value = selectedSupp?.name ?: "اختر المورد المحدد...",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("المورد المرتبط") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSuppMenu) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedSuppMenu,
                        onDismissRequest = { expandedSuppMenu = false }
                    ) {
                        suppliers.forEach { s ->
                            DropdownMenuItem(
                                text = { Text("${s.name} (رصيده: ${Formatters.formatMoney(s.currentBalance)})") },
                                onClick = {
                                    onUpdate(line.copy(partyId = s.id, accountName = "مورد: ${s.name}"))
                                    expandedSuppMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Debit & Credit inputs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = line.debitStr,
                    onValueChange = { newVal ->
                        // If user types in debit, zero out credit for single-line clarity
                        val cleaned = newVal.filter { it.isDigit() || it == '.' }
                        onUpdate(line.copy(debitStr = cleaned, creditStr = if (cleaned.isNotEmpty()) "" else line.creditStr))
                    },
                    label = { Text("مدين (منه)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1B5E20),
                        focusedLabelColor = Color(0xFF1B5E20)
                    )
                )

                OutlinedTextField(
                    value = line.creditStr,
                    onValueChange = { newVal ->
                        // If user types in credit, zero out debit
                        val cleaned = newVal.filter { it.isDigit() || it == '.' }
                        onUpdate(line.copy(creditStr = cleaned, debitStr = if (cleaned.isNotEmpty()) "" else line.debitStr))
                    },
                    label = { Text("دائن (له)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0D47A1),
                        focusedLabelColor = Color(0xFF0D47A1)
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Line Description
            OutlinedTextField(
                value = line.description,
                onValueChange = { onUpdate(line.copy(description = it)) },
                label = { Text("بيان الطرف (اختياري)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}
