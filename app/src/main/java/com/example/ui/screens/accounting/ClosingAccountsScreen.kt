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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.MainViewModel
import com.example.ui.util.BackupHelper
import com.example.ui.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClosingAccountsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val bundle by viewModel.closingAccountsBundle.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("ميزان المراجعة", "الميزانية العمومية", "قائمة الدخل", "حساب المتاجرة")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("الحسابات الختامية والمركز المالي", fontWeight = FontWeight.Bold)
                        Text(
                            "ميزان المراجعة • الميزانية العمومية • الأرباح والخسائر",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
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
                        report.append("📊 *تقرير الحسابات الختامية والمركز المالي*\n")
                        report.append("المتجر: ${settings?.storeName ?: "حسين سوفت"}\n")
                        report.append("التاريخ: ${Formatters.formatDate(bundle.generatedDate)}\n")
                        report.append("━━━━━━━━━━━━━━━━━━━\n")

                        when (selectedTab) {
                            0 -> {
                                val tb = bundle.trialBalance
                                report.append("📑 *ميزان المراجعة بالمجاميع والأرصدة*\n")
                                report.append("إجمالي المجاميع: ${Formatters.formatMoney(tb.totalDebitSum, settings)}\n")
                                report.append("إجمالي الأرصدة المدينة: ${Formatters.formatMoney(tb.totalDebitBalance, settings)}\n")
                                report.append("إجمالي الأرصدة الدائنة: ${Formatters.formatMoney(tb.totalCreditBalance, settings)}\n")
                                report.append("حالة التوازن: ${if (tb.isBalanced) "متزن تماماً (الفرق 0.00)" else "غير متزن بمقدار " + Formatters.formatMoney(tb.difference, settings)}\n\n")
                                tb.accounts.forEach { acc ->
                                    report.append("• ${acc.code} ${acc.name}: مدين ${Formatters.formatMoney(acc.debitBalance, settings)} | دائن ${Formatters.formatMoney(acc.creditBalance, settings)}\n")
                                }
                            }
                            1 -> {
                                val bs = bundle.balanceSheet
                                report.append("🏛 *الميزانية العمومية (قائمة المركز المالي)*\n")
                                report.append("• إجمالي الأصول: ${Formatters.formatMoney(bs.totalAssets, settings)}\n")
                                report.append("  - النقدية بالصندوق: ${Formatters.formatMoney(bs.cashOnHand, settings)}\n")
                                report.append("  - البنك والشبكة: ${Formatters.formatMoney(bs.bankAndCards, settings)}\n")
                                report.append("  - ذمم العملاء (مدينون): ${Formatters.formatMoney(bs.accountsReceivable, settings)}\n")
                                report.append("  - بضاعة آخر المدة بالتكلفة: ${Formatters.formatMoney(bs.inventoryEndingValue, settings)}\n")
                                if (bs.fixedAssets > 0) report.append("  - الأصول الثابتة: ${Formatters.formatMoney(bs.fixedAssets, settings)}\n")
                                report.append("• إجمالي الخصوم: ${Formatters.formatMoney(bs.totalLiabilities, settings)}\n")
                                report.append("  - ذمم الموردين (دائنون): ${Formatters.formatMoney(bs.accountsPayable, settings)}\n")
                                report.append("• حقوق الملكية: ${Formatters.formatMoney(bs.totalEquity, settings)}\n")
                                report.append("  - رأس المال: ${Formatters.formatMoney(bs.capital, settings)}\n")
                                report.append("  - صافي أرباح الفترة: ${Formatters.formatMoney(bs.netProfitCurrentPeriod, settings)}\n")
                                report.append("━━━━━━━━━━━━━━━━━━━\n")
                                report.append("إجمالي الخصوم وحقوق الملكية: ${Formatters.formatMoney(bs.totalLiabilitiesAndEquity, settings)}\n")
                                report.append("الاتزان: ${if (bs.isBalanced) "متزنة تماماً ✓" else "فرق: " + Formatters.formatMoney(bs.difference, settings)}\n")
                            }
                            2 -> {
                                val isRep = bundle.incomeStatement
                                report.append("📈 *قائمة الدخل وحساب الأرباح والخسائر*\n")
                                report.append("• إجمالي المبيعات: ${Formatters.formatMoney(isRep.grossSales, settings)}\n")
                                report.append("• خصومات ومردودات: -${Formatters.formatMoney(isRep.salesDiscountsAndReturns, settings)}\n")
                                report.append("• صافي المبيعات: ${Formatters.formatMoney(isRep.netSales, settings)}\n")
                                report.append("• تكلفة المبيعات: -${Formatters.formatMoney(isRep.costOfGoodsSold, settings)}\n")
                                report.append("• مجمل الربح التجاري: ${Formatters.formatMoney(isRep.grossProfit, settings)} (${String.format("%.1f", isRep.grossProfitMargin)}%)\n")
                                report.append("• المصروفات التشغيلية: -${Formatters.formatMoney(isRep.totalOperatingExpenses, settings)}\n")
                                report.append("━━━━━━━━━━━━━━━━━━━\n")
                                report.append("🏆 صافي الربح النهائي: ${Formatters.formatMoney(isRep.netProfit, settings)} (${String.format("%.1f", isRep.netProfitMargin)}%)\n")
                            }
                            3 -> {
                                val tr = bundle.tradingAccount
                                report.append("🛒 *حساب المتاجرة المحاسبي*\n")
                                report.append("• إجمالي المشتريات: ${Formatters.formatMoney(tr.purchasesTotal, settings)}\n")
                                report.append("• صافي المبيعات: ${Formatters.formatMoney(tr.salesTotal, settings)}\n")
                                report.append("• بضاعة آخر المدة: ${Formatters.formatMoney(tr.endingInventory, settings)}\n")
                                report.append("• مجمل ربح المتاجرة: ${Formatters.formatMoney(tr.grossProfit, settings)}\n")
                            }
                        }
                        report.append("━━━━━━━━━━━━━━━━━━━\n")
                        report.append("🚀 صادر عن نظام حسين سوفت المحاسبي")
                        BackupHelper.shareToTelegram(context, report.toString(), tabTitles[selectedTab])
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال التقرير إلى تلغرام", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Scrollable or Primary Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier.testTag("closing_tab_$index")
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp)
            ) {
                when (selectedTab) {
                    0 -> TrialBalanceView(bundle.trialBalance, settings)
                    1 -> BalanceSheetView(bundle.balanceSheet, settings)
                    2 -> IncomeStatementView(bundle.incomeStatement, settings)
                    3 -> TradingAccountView(bundle.tradingAccount, settings)
                }
            }
        }
    }
}

@Composable
private fun TrialBalanceView(
    data: TrialBalanceData,
    settings: StoreSettings?
) {
    var selectedCategoryFilter by remember { mutableStateOf("الكل") }
    val categories = listOf("الكل", "أصول", "خصوم", "حقوق الملكية", "إيرادات", "مصروفات")

    val filteredAccounts = remember(data.accounts, selectedCategoryFilter) {
        if (selectedCategoryFilter == "الكل") data.accounts
        else data.accounts.filter { it.category == selectedCategoryFilter }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // Balance Header Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (data.isBalanced) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (data.isBalanced) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (data.isBalanced) Color(0xFF2E7D32) else Color(0xFFC62828),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (data.isBalanced) "ميزان المراجعة متزن ومطابق 100%" else "ميزان المراجعة غير متزن",
                                fontWeight = FontWeight.Bold,
                                color = if (data.isBalanced) Color(0xFF1B5E20) else Color(0xFFB71C1C),
                                fontSize = 15.sp
                            )
                        }
                        if (!data.isBalanced) {
                            Text(
                                text = "الفرق: ${Formatters.formatMoney(data.difference, settings)}",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB71C1C),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = (if (data.isBalanced) Color(0xFF81C784) else Color(0xFFEF9A9A)))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("إجمالي الأرصدة المدينة:", fontSize = 11.sp)
                            Text(
                                text = Formatters.formatMoney(data.totalDebitBalance, settings),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF1B5E20)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("إجمالي الأرصدة الدائنة:", fontSize = 11.sp)
                            Text(
                                text = Formatters.formatMoney(data.totalCreditBalance, settings),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF0D47A1)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("إجمالي المجاميع (مدين/دائن):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = Formatters.formatMoney(data.totalDebitSum, settings),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Category Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategoryFilter == cat,
                        onClick = { selectedCategoryFilter = cat },
                        label = { Text(cat, fontSize = 11.sp) }
                    )
                }
            }
        }

        // Table Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("كود / الحساب", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                    Text("رصيد مدين", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20), modifier = Modifier.weight(1.2f))
                    Text("رصيد دائن", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1), modifier = Modifier.weight(1.2f))
                }
            }
        }

        // Account Rows
        items(filteredAccounts) { acc ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(2f)) {
                            Text(text = "${acc.code} - ${acc.name}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(text = "التصنيف: ${acc.category}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Text(
                            text = if (acc.debitBalance > 0) Formatters.formatMoney(acc.debitBalance, settings) else "-",
                            fontWeight = if (acc.debitBalance > 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (acc.debitBalance > 0) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1.2f)
                        )

                        Text(
                            text = if (acc.creditBalance > 0) Formatters.formatMoney(acc.creditBalance, settings) else "-",
                            fontWeight = if (acc.creditBalance > 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (acc.creditBalance > 0) Color(0xFF0D47A1) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1.2f)
                        )
                    }

                    // Extra details row: Debit sum vs Credit sum
                    if (acc.debitTotal > 0 && acc.creditTotal > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("المجاميع: مدين ${Formatters.formatMoney(acc.debitTotal, settings)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("دائن ${Formatters.formatMoney(acc.creditTotal, settings)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceSheetView(
    data: BalanceSheetData,
    settings: StoreSettings?
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // Accounting Equation Hero Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("المعادلة المحاسبية الذهبية للنشاط", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "الأصول = الخصوم + حقوق الملكية",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("إجمالي الأصول", fontSize = 11.sp)
                            Text(Formatters.formatMoney(data.totalAssets, settings), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1B5E20))
                        }
                        Text("=", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("الخصوم + الملكية", fontSize = 11.sp)
                            Text(Formatters.formatMoney(data.totalLiabilitiesAndEquity, settings), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0D47A1))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = if (data.isBalanced) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (data.isBalanced) "✓ الميزانية متزنة ومطابقة تماماً" else "⚠ عدم تطابق بمقدار: ${Formatters.formatMoney(data.difference, settings)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (data.isBalanced) Color(0xFF2E7D32) else Color(0xFFC62828),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Assets Card
        item {
            FinancialStatementSectionCard(
                title = "الأصول (الموجودات)",
                totalLabel = "إجمالي الأصول",
                totalAmount = data.totalAssets,
                settings = settings,
                headerColor = Color(0xFF1B5E20),
                items = listOfNotNull(
                    "النقدية بالصندوق الرئيسي" to data.cashOnHand,
                    "البنك وحسابات الشبكة الإلكترونية" to data.bankAndCards,
                    "ذمم العملاء والمدينون" to data.accountsReceivable,
                    "مخزون البضاعة (بضاعة آخر المدة بالتكلفة)" to data.inventoryEndingValue,
                    if (data.fixedAssets > 0) "الأصول الثابتة وتجهيزات المحل" to data.fixedAssets else null
                )
            )
        }

        // Liabilities Card
        item {
            FinancialStatementSectionCard(
                title = "الخصوم (الالتزامات للغير)",
                totalLabel = "إجمالي الخصوم",
                totalAmount = data.totalLiabilities,
                settings = settings,
                headerColor = Color(0xFFC62828),
                items = listOfNotNull(
                    "ذمم الموردين والدائنون" to data.accountsPayable,
                    if (data.otherLiabilities > 0) "التزامات وقروض دائنة أخرى" to data.otherLiabilities else null
                )
            )
        }

        // Equity Card
        item {
            FinancialStatementSectionCard(
                title = "حقوق الملكية (رأس المال والأرباح)",
                totalLabel = "إجمالي حقوق الملكية",
                totalAmount = data.totalEquity,
                settings = settings,
                headerColor = Color(0xFF0D47A1),
                items = listOfNotNull(
                    "رأس المال المستثمر" to data.capital,
                    if (data.ownerDrawings > 0) "يُطرح: جاري المالك والمسحوبات الشخصية" to -data.ownerDrawings else null,
                    "صافي أرباح / خسائر الفترة الحالية" to data.netProfitCurrentPeriod
                )
            )
        }
    }
}

@Composable
private fun IncomeStatementView(
    data: IncomeStatementData,
    settings: StoreSettings?
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // Hero Net Profit Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (data.netProfit >= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (data.netProfit >= 0) "صافي الربح النهائي للفترة" else "صافي الخسارة للفترة",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (data.netProfit >= 0) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = Formatters.formatMoney(data.netProfit, settings),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        color = if (data.netProfit >= 0) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "هامش صافي الربح: ${String.format("%.1f", data.netProfitMargin)}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Commercial Sales & Cost
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("نشاط المبيعات وتكلفة البضاعة", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))

                    DetailItemRow("إجمالي إيرادات المبيعات", data.grossSales, settings)
                    if (data.salesDiscountsAndReturns > 0) {
                        DetailItemRow("يطرح: خصومات ومردودات المبيعات", -data.salesDiscountsAndReturns, settings, isNegative = true)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    DetailItemRow("صافي إيرادات المبيعات", data.netSales, settings, isBold = true)
                    DetailItemRow("يطرح: تكلفة البضاعة المباعة (COGS)", -data.costOfGoodsSold, settings, isNegative = true)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    DetailItemRow(
                        "مجمل الربح التجاري (${String.format("%.1f", data.grossProfitMargin)}%)",
                        data.grossProfit,
                        settings,
                        isBold = true,
                        customColor = Color(0xFF1B5E20)
                    )
                }
            }
        }

        // Operating Expenses
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("المصروفات التشغيلية والعمومية", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                        Text("-${Formatters.formatMoney(data.totalOperatingExpenses, settings)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (data.expensesByCategory.isEmpty()) {
                        Text("لا توجد مصروفات مسجلة", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        data.expensesByCategory.forEach { (cat, amount) ->
                            DetailItemRow(cat, amount, settings)
                        }
                    }

                    if (data.otherRevenues > 0 || data.otherExpenses > 0) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        if (data.otherRevenues > 0) DetailItemRow("إيرادات وأرباح أخرى", data.otherRevenues, settings, customColor = Color(0xFF1B5E20))
                        if (data.otherExpenses > 0) DetailItemRow("مصروفات وخسائر أخرى", -data.otherExpenses, settings, isNegative = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun TradingAccountView(
    data: TradingAccountData,
    settings: StoreSettings?
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("حساب المتاجرة المحاسبي", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                    Text("لمقابلة المشتريات بالمبيعات وبضاعة آخر المدة لتحديد مجمل الربح", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Side Debit (منه)
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("جانب منه (مدين)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1B5E20))
                                Spacer(modifier = Modifier.height(6.dp))
                                DetailItemRow("المشتريات", data.purchasesTotal, settings)
                                DetailItemRow("مجمل الربح", data.grossProfit, settings, isBold = true, customColor = Color(0xFF1B5E20))
                                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                                DetailItemRow("المجموع", data.totalTradingDebit, settings, isBold = true)
                            }
                        }

                        // Side Credit (له)
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("جانب له (دائن)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0D47A1))
                                Spacer(modifier = Modifier.height(6.dp))
                                DetailItemRow("المبيعات", data.salesTotal, settings)
                                DetailItemRow("بضاعة آخر المدة", data.endingInventory, settings, isBold = true)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                                DetailItemRow("المجموع", data.totalTradingCredit, settings, isBold = true)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FinancialStatementSectionCard(
    title: String,
    totalLabel: String,
    totalAmount: Double,
    settings: StoreSettings?,
    headerColor: Color,
    items: List<Pair<String, Double>>
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = headerColor)
            Spacer(modifier = Modifier.height(8.dp))

            items.forEach { (name, amount) ->
                DetailItemRow(name, amount, settings)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            DetailItemRow(totalLabel, totalAmount, settings, isBold = true, customColor = headerColor)
        }
    }
}

@Composable
private fun DetailItemRow(
    title: String,
    amount: Double,
    settings: StoreSettings?,
    isBold: Boolean = false,
    isNegative: Boolean = false,
    customColor: Color? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isBold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = Formatters.formatMoney(amount, settings),
            fontSize = if (isBold) 13.sp else 12.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = customColor ?: when {
                isNegative -> MaterialTheme.colorScheme.error
                isBold -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
