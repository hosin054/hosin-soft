package com.example.ui.screens.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.example.data.model.CurrencyRate
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.util.BackupHelper
import com.example.ui.util.Formatters
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: MainViewModel,
    onNavigateToClosingAccounts: () -> Unit = {},
    onNavigateToJournal: () -> Unit = {},
    onNavigateToChartOfAccounts: () -> Unit = {},
    onNavigateToFinancialTools: () -> Unit = {}
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val invoices by viewModel.allInvoices.collectAsStateWithLifecycle()
    val expenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val products by viewModel.allProducts.collectAsStateWithLifecycle()
    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()
    val currencyRates by viewModel.allCurrencyRates.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var dateFilter by remember { mutableStateOf(1) } // 0 = Today, 1 = This Month, 2 = This Year, 3 = All
    var showAllCurrenciesOverview by remember { mutableStateOf(false) }

    val baseCurrency = remember(currencyRates) {
        currencyRates.find { it.isBase } ?: currencyRates.firstOrNull() ?: CurrencyRate(
            code = "SAR", name = "ريال سعودي", symbol = settings?.currencySymbol ?: "ر.س", rateToBase = 1.0, isBase = true
        )
    }

    var selectedCurrencyId by remember(currencyRates) {
        mutableStateOf(baseCurrency.id)
    }

    val selectedCurrency = remember(currencyRates, selectedCurrencyId) {
        currencyRates.find { it.id == selectedCurrencyId } ?: baseCurrency
    }

    val cal = Calendar.getInstance()
    val fromTimestamp = remember(dateFilter) {
        when (dateFilter) {
            0 -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.timeInMillis
            }
            1 -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.timeInMillis
            }
            2 -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.timeInMillis
            }
            else -> 0L
        }
    }

    val periodSales = invoices.filter { it.invoiceType == "SALE" && it.status == "COMPLETED" && it.createdAt >= fromTimestamp }
    val periodPurchases = invoices.filter { it.invoiceType == "PURCHASE" && it.status == "COMPLETED" && it.createdAt >= fromTimestamp }
    val periodExpenses = expenses.filter { it.createdAt >= fromTimestamp }

    val totalSalesBase = periodSales.sumOf { it.totalAmount }
    val totalProfitGrossBase = periodSales.sumOf { it.profit }
    val totalExpensesBase = periodExpenses.sumOf { it.amount }
    val netProfitBase = totalProfitGrossBase - totalExpensesBase
    val totalPurchasesBase = periodPurchases.sumOf { it.totalAmount }
    val totalTaxBase = periodSales.sumOf { it.taxAmount }

    val totalCustomersDebtBase = customers.sumOf { maxOf(0.0, it.currentBalance) }
    val totalSuppliersPayableBase = suppliers.sumOf { maxOf(0.0, it.currentBalance) }

    // Breakdown by payment method
    val cashSalesBase = periodSales.filter { it.paymentMethod == "كاش" || (it.paymentMethod.isBlank() && it.paymentType == "CASH") }.sumOf { it.paidAmount }
    val electronicSalesBase = periodSales.filter { it.paymentMethod == "إلكتروني / شبكة" }.sumOf { it.paidAmount }
    val bankTransferSalesBase = periodSales.filter { it.paymentMethod == "تحويل بنكي" }.sumOf { it.paidAmount }
    val creditSalesBase = periodSales.sumOf { it.remainingAmount }

    fun toDisplayMoney(amountInBase: Double, curr: CurrencyRate = selectedCurrency): String {
        val converted = if (curr.rateToBase > 0) amountInBase / curr.rateToBase else amountInBase
        return "${String.format(Locale.ENGLISH, "%,.2f", converted)} ${curr.symbol}"
    }

    fun toDisplayProfit(amountInBase: Double, curr: CurrencyRate = selectedCurrency): String {
        if (!currentUser.canViewProfits) return "*** محجوب ***"
        return toDisplayMoney(amountInBase, curr)
    }

    if (!currentUser.canViewReports) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("التقارير المالية والأرباح") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "عفواً، لا تمتلك صلاحية عرض التقارير",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "حسابك الحالي (${currentUser.fullName} - ${viewModel.getRoleArabicName(currentUser.role)}) غير مصرح له بالاطلاع على التقارير المالية. يرجى مراجعة مسؤول النظام لمنحك الصلاحية.",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
        return
    }

    val periodLabel = when (dateFilter) {
        0 -> "اليوم"
        1 -> "الشهر الحالي"
        2 -> "العام الحالي"
        else -> "كافة الفترات"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("التقارير المالية والأرباح") },
                actions = {
                    IconButton(onClick = {
                        val text = """
                            تقرير الأرباح والمالية - ${settings?.storeName}
                            الفترة: $periodLabel
                            عملة التقرير: ${selectedCurrency.name} (${selectedCurrency.symbol})
                            ------------------------
                            إجمالي المبيعات: ${toDisplayMoney(totalSalesBase)}
                            مجمل ربح المبيعات: ${toDisplayProfit(totalProfitGrossBase)}
                            إجمالي المصروفات: ${toDisplayMoney(totalExpensesBase)}
                            صافي الربح النهائي: ${toDisplayProfit(netProfitBase)}
                            ------------------------
                            المقبوض نقداً (كاش): ${toDisplayMoney(cashSalesBase)}
                            المقبوض شبكة/إلكتروني: ${toDisplayMoney(electronicSalesBase)}
                            المقبوض تحويل بنكي: ${toDisplayMoney(bankTransferSalesBase)}
                            المبيعات الآجلة (ديون): ${toDisplayMoney(creditSalesBase)}
                            ------------------------
                            إجمالي المشتريات والتوريد: ${toDisplayMoney(totalPurchasesBase)}
                            ديون العملاء (لنا): ${toDisplayMoney(totalCustomersDebtBase)}
                            مستحقات الموردين (علينا): ${toDisplayMoney(totalSuppliersPayableBase)}
                            ------------------------
                            تم الاستخراج بنجاح بواسطة نظام المبيعات
                        """.trimIndent()
                        BackupHelper.shareText(context, text, "تقرير مالي $periodLabel - ${selectedCurrency.name}")
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "مشاركة التقرير", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(onClick = {
                        val text = """
📊 *تقرير الأرباح والمالية - ${settings?.storeName}*
الفترة: $periodLabel
العملة: ${selectedCurrency.name} (${selectedCurrency.symbol})
━━━━━━━━━━━━━━━━━━━
💰 إجمالي المبيعات: ${toDisplayMoney(totalSalesBase)}
${if (currentUser.canViewProfits) "💵 مجمل ربح المبيعات: ${toDisplayProfit(totalProfitGrossBase)}\n" else ""}📦 إجمالي المصروفات: ${toDisplayMoney(totalExpensesBase)}
${if (currentUser.canViewProfits) "📈 صافي الربح النهائي: ${toDisplayProfit(netProfitBase)}\n" else ""}━━━━━━━━━━━━━━━━━━━
💳 المقبوضات حسب وسيلة الدفع:
• كاش: ${toDisplayMoney(cashSalesBase)}
• شبكة / مدى: ${toDisplayMoney(electronicSalesBase)}
• تحويل بنكي: ${toDisplayMoney(bankTransferSalesBase)}
• مبيعات آجلة: ${toDisplayMoney(creditSalesBase)}
━━━━━━━━━━━━━━━━━━━
🛒 إجمالي المشتريات: ${toDisplayMoney(totalPurchasesBase)}
• ديون العملاء (لنا): ${toDisplayMoney(totalCustomersDebtBase)}
• مستحقات الموردين (علينا): ${toDisplayMoney(totalSuppliersPayableBase)}
🚀 *حسين سوفت لإدارة المتاجر ونقاط البيع*
                        """.trimIndent()
                        BackupHelper.shareToTelegram(context, text, "تقرير مالي $periodLabel - ${selectedCurrency.name}")
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال إلى تلغرام", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Closing Accounts & Financial Statements Entry Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("الحسابات الختامية والمركز المالي", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "ميزان المراجعة بالمجاميع والأرصدة • الميزانية العمومية • قائمة الدخل • حساب المتاجرة",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onNavigateToClosingAccounts,
                                modifier = Modifier.weight(1.3f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Balance, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("فتح الحسابات الختامية", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onNavigateToJournal,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.PostAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("سندات القيد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onNavigateToChartOfAccounts,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.AccountTree, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("شجرة الحسابات", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = onNavigateToFinancialTools,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.AutoGraph, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("الأدوات والوردية", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Period Filter Chips
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = dateFilter == 0,
                        onClick = { dateFilter = 0 },
                        label = { Text("اليوم") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    FilterChip(
                        selected = dateFilter == 1,
                        onClick = { dateFilter = 1 },
                        label = { Text("الشهر") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    FilterChip(
                        selected = dateFilter == 2,
                        onClick = { dateFilter = 2 },
                        label = { Text("السنة") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    FilterChip(
                        selected = dateFilter == 3,
                        onClick = { dateFilter = 3 },
                        label = { Text("الكل") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Currency Selection for Financial Report
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "عملة حساب وعرض الأرباح:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            TextButton(onClick = { showAllCurrenciesOverview = !showAllCurrenciesOverview }) {
                                Text(if (showAllCurrenciesOverview) "إخفاء جدول العملات" else "عرض بجميع العملات", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        var currencyDropdownExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { currencyDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${selectedCurrency.name} (${selectedCurrency.symbol}) - ${selectedCurrency.code}",
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (!selectedCurrency.isBase) {
                                            Text(
                                                text = "سعر الصرف: ${selectedCurrency.rateToBase} ${baseCurrency.symbol}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                }
                            }
                            DropdownMenu(
                                expanded = currencyDropdownExpanded,
                                onDismissRequest = { currencyDropdownExpanded = false }
                            ) {
                                currencyRates.forEach { rate ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("${rate.name} (${rate.symbol})")
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Text(
                                                    if (rate.isBase) "الأساسية" else "سعر الصرف: ${rate.rateToBase}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedCurrencyId = rate.id
                                            currencyDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // All Currencies Overview Table (when toggled or selected)
            if (showAllCurrenciesOverview) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "مقارنة الأرباح والمبيعات بجميع العملات المعرفة",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Header row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("العملة", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.2f))
                                Text("سعر الصرف", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                Text("المبيعات", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.3f))
                                Text("صافي الربح", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.3f))
                            }

                            currencyRates.forEach { curr ->
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${curr.name} (${curr.symbol})",
                                        fontSize = 11.sp,
                                        fontWeight = if (curr.id == selectedCurrency.id) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.weight(1.2f)
                                    )
                                    Text(
                                        text = if (curr.isBase) "1.0 (أساسي)" else String.format(Locale.ENGLISH, "%.4f", curr.rateToBase),
                                        fontSize = 11.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = toDisplayMoney(totalSalesBase, curr),
                                        fontSize = 11.sp,
                                        modifier = Modifier.weight(1.3f)
                                    )
                                    Text(
                                        text = toDisplayProfit(netProfitBase, curr),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (currentUser.canViewProfits && netProfitBase >= 0) Color(0xFF15803D) else if (currentUser.canViewProfits) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1.3f)
                                    )
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                            }
                        }
                    }
                }
            }

            // Net Profit Highlight Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (!currentUser.canViewProfits) MaterialTheme.colorScheme.surfaceVariant
                        else if (netProfitBase >= 0) Color(0xFF15803D).copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "صافي الربح / الخسارة للفترة", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "بعملة: ${selectedCurrency.name}",
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = toDisplayProfit(netProfitBase),
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            color = if (!currentUser.canViewProfits) MaterialTheme.colorScheme.onSurfaceVariant
                            else if (netProfitBase >= 0) Color(0xFF15803D)
                            else MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (currentUser.canViewProfits) "(مجمل ربح المبيعات: ${toDisplayMoney(totalProfitGrossBase)} - المصروفات: ${toDisplayMoney(totalExpensesBase)})" else "صلاحية مشاهدة الأرباح غير مفعلة لهذا الحساب",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (currentUser.canViewProfits && !selectedCurrency.isBase) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "ما يعادل بالعملة الأساسية للمتجر: ${Formatters.formatMoney(netProfitBase, settings)}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Interactive Donut Chart for Payment Methods
            item {
                val slices = buildList {
                    if (cashSalesBase > 0) add(DonutSlice("كاش", cashSalesBase, Color(0xFF16A34A)))
                    if (electronicSalesBase > 0) add(DonutSlice("شبكة / مدى", electronicSalesBase, Color(0xFF0284C7)))
                    if (bankTransferSalesBase > 0) add(DonutSlice("تحويل بنكي", bankTransferSalesBase, Color(0xFF7C3AED)))
                    if (creditSalesBase > 0) add(DonutSlice("مبيعات آجلة", creditSalesBase, Color(0xFFEA580C)))
                }
                if (slices.isNotEmpty()) {
                    InteractiveDonutChart(
                        title = "مخطط توزيع المقبوضات التفاعلي (Donut Chart)",
                        slices = slices,
                        currencySymbol = selectedCurrency.symbol
                    )
                }
            }

            // Payment Methods Breakdown Card (الكاش والإلكتروني والآجل)
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "تفصيل المقبوضات حسب طريقة الدفع", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        ReportRow(
                            title = "💵 المدفوع نقداً (كاش)",
                            value = toDisplayMoney(cashSalesBase),
                            color = Color(0xFF166534),
                            isBold = true
                        )
                        ReportRow(
                            title = "💳 المدفوع إلكتروني / شبكة / مدى",
                            value = toDisplayMoney(electronicSalesBase),
                            color = Color(0xFF0284C7),
                            isBold = true
                        )
                        ReportRow(
                            title = "🏦 المدفوع تحويل بنكي",
                            value = toDisplayMoney(bankTransferSalesBase),
                            color = Color(0xFF7C3AED),
                            isBold = true
                        )
                        ReportRow(
                            title = "⏳ مبيعات آجلة غير محصلة (ذمم)",
                            value = toDisplayMoney(creditSalesBase),
                            color = MaterialTheme.colorScheme.error,
                            isBold = true
                        )
                    }
                }
            }

            // Interactive 3D Bar Chart
            item {
                val barData = buildList {
                    add(ChartBarData("المبيعات", totalSalesBase, Color(0xFF2563EB), Color(0xFF1D4ED8)))
                    if (currentUser.canViewProfits) {
                        add(ChartBarData("مجمل الربح", totalProfitGrossBase, Color(0xFF059669), Color(0xFF047857)))
                    }
                    add(ChartBarData("المصروفات", totalExpensesBase, Color(0xFFDC2626), Color(0xFFB91C1C)))
                    if (currentUser.canViewProfits) {
                        add(ChartBarData("صافي الربح", maxOf(0.0, netProfitBase), Color(0xFF16A34A), Color(0xFF15803D)))
                    }
                }
                Interactive3DBarChart(
                    title = "مقارنة المؤشرات المالية التفاعلية (3D)",
                    subtitle = "اضغط على أي عمود لمعاينة النسبة والمبلغ والتحليل المالي",
                    items = barData,
                    currencySymbol = selectedCurrency.symbol
                )
            }

            // Detailed Financial Table
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "تفاصيل القوائم المالية", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                text = "القيم بـ ${selectedCurrency.symbol}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        ReportRow(title = "إجمالي المبيعات", value = toDisplayMoney(totalSalesBase), isBold = true)
                        ReportRow(title = "عدد فواتير المبيعات", value = "${periodSales.size} فاتورة")
                        ReportRow(title = "مجمل ربح المبيعات", value = toDisplayProfit(totalProfitGrossBase), color = if (currentUser.canViewProfits) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurfaceVariant)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        ReportRow(title = "إجمالي المصروفات التشغيلية", value = toDisplayMoney(totalExpensesBase), color = MaterialTheme.colorScheme.error)
                        ReportRow(title = "عدد بنود المصروفات", value = "${periodExpenses.size} حركة")
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        ReportRow(
                            title = "صافي الربح النهائي",
                            value = toDisplayProfit(netProfitBase),
                            isBold = true,
                            color = if (!currentUser.canViewProfits) MaterialTheme.colorScheme.onSurfaceVariant else if (netProfitBase >= 0) Color(0xFF15803D) else MaterialTheme.colorScheme.error
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        ReportRow(title = "إجمالي المشتريات والتوريدات", value = toDisplayMoney(totalPurchasesBase))
                        ReportRow(title = "ضريبة القيمة المضافة المحصلة", value = toDisplayMoney(totalTaxBase))
                    }
                }
            }

            // Balances & Debt Snapshot
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "موقف الذمم والديون اللحظي", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        ReportRow(title = "ديون العملاء المطلوبة (لنا)", value = toDisplayMoney(totalCustomersDebtBase), color = Color(0xFFC2410C), isBold = true)
                        ReportRow(title = "مستحقات الموردين الواجب سدادها (علينا)", value = toDisplayMoney(totalSuppliersPayableBase), color = MaterialTheme.colorScheme.error, isBold = true)
                    }
                }
            }

            // Telegram Action Card
            item {
                TelegramActionCard(
                    onSendDailyReport = {
                        val text = """
📊 *تقرير الأرباح والمالية - ${settings?.storeName}*
الفترة: $periodLabel
العملة: ${selectedCurrency.name} (${selectedCurrency.symbol})
━━━━━━━━━━━━━━━━━━━
💰 إجمالي المبيعات: ${toDisplayMoney(totalSalesBase)}
${if (currentUser.canViewProfits) "💵 مجمل ربح المبيعات: ${toDisplayProfit(totalProfitGrossBase)}\n" else ""}📦 إجمالي المصروفات: ${toDisplayMoney(totalExpensesBase)}
${if (currentUser.canViewProfits) "📈 صافي الربح النهائي: ${toDisplayProfit(netProfitBase)}\n" else ""}━━━━━━━━━━━━━━━━━━━
💳 المقبوضات حسب وسيلة الدفع:
• كاش: ${toDisplayMoney(cashSalesBase)}
• شبكة / مدى: ${toDisplayMoney(electronicSalesBase)}
• تحويل بنكي: ${toDisplayMoney(bankTransferSalesBase)}
• مبيعات آجلة: ${toDisplayMoney(creditSalesBase)}
━━━━━━━━━━━━━━━━━━━
🛒 إجمالي المشتريات: ${toDisplayMoney(totalPurchasesBase)}
• ديون العملاء (لنا): ${toDisplayMoney(totalCustomersDebtBase)}
• مستحقات الموردين (علينا): ${toDisplayMoney(totalSuppliersPayableBase)}
🚀 *حسين سوفت لإدارة المتاجر ونقاط البيع*
                        """.trimIndent()
                        BackupHelper.shareToTelegram(context, text, "تقرير مالي $periodLabel - ${selectedCurrency.name}")
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun ReportRow(
    title: String,
    value: String,
    isBold: Boolean = false,
    color: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, fontSize = 13.sp, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
        Text(text = value, fontSize = 13.sp, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal, color = color)
    }
}
