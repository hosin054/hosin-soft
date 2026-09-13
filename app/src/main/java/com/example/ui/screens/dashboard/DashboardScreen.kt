package com.example.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.Invoice
import com.example.data.model.User
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.navigation.Screen
import com.example.ui.util.BackupHelper
import com.example.ui.util.Formatters
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateTo: (String) -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val products by viewModel.allProducts.collectAsStateWithLifecycle()
    val lowStock by viewModel.lowStockProducts.collectAsStateWithLifecycle()
    val outOfStock by viewModel.outOfStockProducts.collectAsStateWithLifecycle()
    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()
    val invoices by viewModel.allInvoices.collectAsStateWithLifecycle()
    val expenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val vouchers by viewModel.allVouchers.collectAsStateWithLifecycle()
    val cashTx by viewModel.allCashTransactions.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()

    var showSwitchUserDialog by remember { mutableStateOf(false) }
    var targetUserToSwitch by remember { mutableStateOf<User?>(null) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    // Calculate dates
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val startOfToday = cal.timeInMillis

    cal.set(Calendar.DAY_OF_MONTH, 1)
    val startOfMonth = cal.timeInMillis

    // Aggregations
    val salesToday = invoices
        .filter { it.invoiceType == "SALE" && it.status == "COMPLETED" && it.createdAt >= startOfToday }
        .sumOf { it.totalAmount }

    val salesThisMonth = invoices
        .filter { it.invoiceType == "SALE" && it.status == "COMPLETED" && it.createdAt >= startOfMonth }
        .sumOf { it.totalAmount }

    val purchasesThisMonth = invoices
        .filter { it.invoiceType == "PURCHASE" && it.status == "COMPLETED" && it.createdAt >= startOfMonth }
        .sumOf { it.totalAmount }

    val expensesThisMonth = expenses
        .filter { it.createdAt >= startOfMonth }
        .sumOf { it.amount }

    val grossProfit = invoices
        .filter { it.invoiceType == "SALE" && it.status == "COMPLETED" && it.createdAt >= startOfMonth }
        .sumOf { it.profit }

    val netProfit = maxOf(0.0, grossProfit - expensesThisMonth)

    val customersTotalDebt = customers.sumOf { maxOf(0.0, it.currentBalance) }
    val suppliersTotalPayable = suppliers.sumOf { maxOf(0.0, it.currentBalance) }

    val stockValueAtCost = products.sumOf {
        val costPerSubUnit = if (it.conversionFactor > 0) it.purchasePrice / it.conversionFactor else it.purchasePrice
        it.currentStockSubUnits * costPerSubUnit
    }

    val stockValueAtSale = products.sumOf {
        val salePerSubUnit = if (it.conversionFactor > 0) it.cashSalePrice / it.conversionFactor else it.cashSalePrice
        it.currentStockSubUnits * salePerSubUnit
    }

    val currentCash = cashTx.firstOrNull()?.balanceAfter ?: 0.0

    val context = LocalContext.current
    val profitToday = invoices
        .filter { it.invoiceType == "SALE" && it.status == "COMPLETED" && it.createdAt >= startOfToday }
        .sumOf { it.profit }

    val expensesToday = expenses
        .filter { it.createdAt >= startOfToday }
        .sumOf { it.amount }

    val sendTelegramSummary = {
        val dateStr = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.ENGLISH).format(java.util.Date())
        val text = """
📊 *تقرير حسين سوفت المالي اللحظي* 🏢
المتجر: ${settings?.storeName ?: "حسين سوفت"}
التاريخ والوقت: $dateStr
الموظف النشط: ${currentUser.fullName} (${viewModel.getRoleArabicName(currentUser.role)})
━━━━━━━━━━━━━━━━━━━
💰 *مبيعات اليوم:* ${Formatters.formatMoney(salesToday, settings)}
${if (currentUser.canViewProfits) "💵 *أرباح اليوم التقديرية:* ${Formatters.formatMoney(profitToday, settings)}\n" else ""}📦 *مصروفات اليوم:* ${Formatters.formatMoney(expensesToday, settings)}
━━━━━━━━━━━━━━━━━━━
📈 *مبيعات الشهر الحالي:* ${Formatters.formatMoney(salesThisMonth, settings)}
${if (currentUser.canViewProfits) "📊 *صافي أرباح الشهر:* ${Formatters.formatMoney(netProfit, settings)}\n" else ""}🛒 *مشتريات الشهر:* ${Formatters.formatMoney(purchasesThisMonth, settings)}
━━━━━━━━━━━━━━━━━━━
💳 *موقف الذمم والديون:*
• ديون العملاء (لنا): ${Formatters.formatMoney(customersTotalDebt, settings)}
• مستحقات الموردين (علينا): ${Formatters.formatMoney(suppliersTotalPayable, settings)}
• رصيد الصندوق الحالي: ${Formatters.formatMoney(currentCash, settings)}
━━━━━━━━━━━━━━━━━━━
⚠️ *حالة المخزون:* ${outOfStock.size} أصناف نافذة، ${lowStock.size} أصناف منخفضة
🚀 *نظام حسين سوفت لإدارة نقاط البيع والمتاجر الذكية*
        """.trimIndent()
        BackupHelper.shareToTelegram(context, text, "تقرير مالي يومي - تلغرام")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = settings?.storeName ?: "حسين سوفت",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "نظام الحسابات ونقاط البيع المتكامل",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        pinError = null
                        pinInput = ""
                        showSwitchUserDialog = true
                    }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "المستخدم والحساب", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(onClick = { onNavigateTo(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = "الإعدادات", tint = MaterialTheme.colorScheme.onPrimary)
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
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Store & Active User Hero Banner
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            pinError = null
                            pinInput = ""
                            showSwitchUserDialog = true
                        }
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Top gradient accent line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.5.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                    )
                                )
                                .align(Alignment.TopCenter)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = currentUser.fullName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = viewModel.getRoleArabicName(currentUser.role).split(" ").firstOrNull() ?: currentUser.role,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "@${currentUser.username} • متجر: ${settings?.storeName ?: "حسين سوفت"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    pinError = null
                                    pinInput = ""
                                    showSwitchUserDialog = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تبديل الحساب", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Low stock alert banner if any
            if (lowStock.isNotEmpty() || outOfStock.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateTo(Screen.Inventory.route) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "تنبيه المخزون!",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "يوجد ${outOfStock.size} أصناف منتهية و ${lowStock.size} أصناف منخفضة المخزون",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // 3D Isometric Hero Infographic Banner
            item {
                Isometric3DHeroBanner(
                    storeName = settings?.storeName ?: "حسين سوفت",
                    salesToday = salesToday,
                    profitToday = profitToday,
                    canViewProfits = currentUser.canViewProfits,
                    settings = settings,
                    onOpenReports = { onNavigateTo(Screen.Reports.route) },
                    onTelegramShare = sendTelegramSummary
                )
            }

            // Quick Actions Bar
            item {
                Text(
                    text = "العمليات السريعة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionItem(
                        icon = Icons.Default.PointOfSale,
                        label = "بيع للزبون",
                        color = MaterialTheme.colorScheme.primary,
                        onClick = { onNavigateTo(Screen.Pos.route) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionItem(
                        icon = Icons.Default.ShoppingCart,
                        label = "شراء للمحل",
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = { onNavigateTo(Screen.PurchaseForm.route) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionItem(
                        icon = Icons.Default.ReceiptLong,
                        label = "سند قبض",
                        color = Color(0xFF059669),
                        onClick = { onNavigateTo(Screen.Vouchers.route) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionItem(
                        icon = Icons.Default.Payments,
                        label = "سند صرف",
                        color = Color(0xFFD97706),
                        onClick = { onNavigateTo(Screen.Vouchers.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionItem(
                        icon = Icons.Default.RequestQuote,
                        label = "عروض أسعار",
                        color = Color(0xFF0284C7),
                        onClick = { onNavigateTo(Screen.Quotations.route) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionItem(
                        icon = Icons.Default.QrCode,
                        label = "ملصقات باركود",
                        color = Color(0xFF7C3AED),
                        onClick = { onNavigateTo(Screen.BarcodeLabels.createRoute(0L)) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionItem(
                        icon = Icons.Default.Inventory,
                        label = "المخزون والجرد",
                        color = Color(0xFFEA580C),
                        onClick = { onNavigateTo(Screen.Inventory.route) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionItem(
                        icon = Icons.Default.Assessment,
                        label = "التقارير",
                        color = Color(0xFF4F46E5),
                        onClick = { onNavigateTo(Screen.Reports.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionItem(
                        icon = Icons.Default.CurrencyExchange,
                        label = "أسعار العملات",
                        color = Color(0xFF0D9488),
                        onClick = { onNavigateTo(Screen.Currencies.route) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionItem(
                        icon = Icons.Default.ManageAccounts,
                        label = "الموظفين",
                        color = Color(0xFF673AB7),
                        onClick = { onNavigateTo(Screen.Users.route) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionItem(
                        icon = Icons.Default.PriceCheck,
                        label = "المصروفات",
                        color = Color(0xFFDC2626),
                        onClick = { onNavigateTo(Screen.Expenses.route) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionItem(
                        icon = Icons.Default.People,
                        label = "الحسابات",
                        color = Color(0xFF0284C7),
                        onClick = { onNavigateTo(Screen.Parties.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionItem(
                        icon = Icons.Default.PostAdd,
                        label = "سند قيد",
                        color = Color(0xFF0F766E),
                        onClick = { onNavigateTo(Screen.JournalVouchers.route) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionItem(
                        icon = Icons.Default.AccountBalance,
                        label = "الختامية",
                        color = Color(0xFF1E3A8A),
                        onClick = { onNavigateTo(Screen.ClosingAccounts.route) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionItem(
                        icon = Icons.Default.AccountBalanceWallet,
                        label = "الصندوق",
                        color = Color(0xFF16A34A),
                        onClick = { onNavigateTo(Screen.Cash.route) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionItem(
                        icon = Icons.Default.HistoryEdu,
                        label = "سجل العمليات",
                        color = Color(0xFF78350F),
                        onClick = { onNavigateTo(Screen.AuditLogs.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Main KPI Cards
            item {
                Text(
                    text = "نظرة عامة على النشاط المالي",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    KpiCard(
                        title = "مبيعات اليوم",
                        value = Formatters.formatMoney(salesToday, settings),
                        icon = Icons.Default.TrendingUp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    KpiCard(
                        title = "مبيعات الشهر",
                        value = Formatters.formatMoney(salesThisMonth, settings),
                        icon = Icons.Default.CalendarToday,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    KpiCard(
                        title = "صافي أرباح الشهر",
                        value = if (currentUser.canViewProfits) Formatters.formatMoney(netProfit, settings) else "*** محجوب ***",
                        subtitle = if (currentUser.canViewProfits) "مجمل الربح: ${Formatters.formatMoney(grossProfit, settings)}" else "صلاحية الأرباح غير مفعلة لهذا الحساب",
                        icon = Icons.Default.MonetizationOn,
                        color = Color(0xFF15803D),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    KpiCard(
                        title = "رصيد الصندوق",
                        value = Formatters.formatMoney(currentCash, settings),
                        icon = Icons.Default.AccountBalanceWallet,
                        color = Color(0xFF0F766E),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    KpiCard(
                        title = "ديون العملاء (لنا)",
                        value = Formatters.formatMoney(customersTotalDebt, settings),
                        subtitle = "${customers.count { it.currentBalance > 0 }} عميل مدين",
                        icon = Icons.Default.PersonSearch,
                        color = Color(0xFFC2410C),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    KpiCard(
                        title = "مستحق للموردين (علينا)",
                        value = Formatters.formatMoney(suppliersTotalPayable, settings),
                        subtitle = "${suppliers.count { it.currentBalance > 0 }} مورد مستحق",
                        icon = Icons.Default.LocalShipping,
                        color = Color(0xFFB91C1C),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    KpiCard(
                        title = "قيمة المخزون (بالتكلفة)",
                        value = Formatters.formatMoney(stockValueAtCost, settings),
                        subtitle = "بسعر البيع: ${Formatters.formatMoney(stockValueAtSale, settings)}",
                        icon = Icons.Default.Inventory2,
                        color = Color(0xFF4338CA),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    KpiCard(
                        title = "المصروفات الشهرية",
                        value = Formatters.formatMoney(expensesThisMonth, settings),
                        subtitle = "${expenses.size} حركة مصروف",
                        icon = Icons.Default.PriceCheck,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Interactive 3D Infographic Bar Chart
            item {
                val chartItems = buildList {
                    add(ChartBarData(label = "المبيعات", value = salesThisMonth, color = Color(0xFF2563EB), secondaryColor = Color(0xFF1D4ED8)))
                    if (currentUser.canViewProfits) {
                        add(ChartBarData(label = "الأرباح", value = netProfit, color = Color(0xFF16A34A), secondaryColor = Color(0xFF15803D)))
                    }
                    add(ChartBarData(label = "المشتريات", value = purchasesThisMonth, color = Color(0xFF8B5CF6), secondaryColor = Color(0xFF7C3AED)))
                    add(ChartBarData(label = "المصروفات", value = expensesThisMonth, color = Color(0xFFEF4444), secondaryColor = Color(0xFFDC2626)))
                }

                Interactive3DBarChart(
                    title = "مخطط الأداء المالي التفاعلي (3D)",
                    subtitle = "اضغط على أي عمود لتحليل النسبة والمبلغ ومقارنة الحركة",
                    items = chartItems,
                    currencySymbol = settings?.currencySymbol ?: "ر.س"
                )
            }

            // Telegram Action Card
            item {
                TelegramActionCard(
                    onSendDailyReport = sendTelegramSummary
                )
            }

            // Recent Invoices Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "آخر الفواتير",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { onNavigateTo(Screen.Reports.route) }) {
                        Text("عرض كافة التقارير")
                    }
                }
            }

            // Recent Invoices Items
            if (invoices.isEmpty()) {
                item {
                    Text(
                        text = "لا توجد فواتير حتى الآن، ابدأ بإنشاء أول فاتورة مبيعات أو مشتريات",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                items(invoices.take(5)) { inv ->
                    InvoiceListItem(
                        invoice = inv,
                        settings = settings,
                        onClick = { onNavigateTo(Screen.InvoiceDetail.createRoute(inv.id)) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        // --- Fast Switch User Account Dialog ---
        if (showSwitchUserDialog) {
            AlertDialog(
                onDismissRequest = { showSwitchUserDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.People, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تبديل حساب الموظف", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "المستخدم الحالي: ${currentUser.fullName} (${viewModel.getRoleArabicName(currentUser.role)})",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        allUsers.forEach { user ->
                            val isSelected = user.id == currentUser.id
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showSwitchUserDialog = false
                                        if (isSelected) {
                                            viewModel.showMessage("أنت بالفعل مسجل الدخول بحساب ${user.fullName}")
                                        } else if (user.passwordHash.isBlank()) {
                                            viewModel.switchUser(user, "") { _, _ -> }
                                        } else {
                                            targetUserToSwitch = user
                                            pinInput = ""
                                            pinError = null
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (isSelected) Icons.Default.CheckCircle else Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = user.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            text = "${viewModel.getRoleArabicName(user.role)} • @${user.username}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (user.passwordHash.isNotBlank()) {
                                        Icon(Icons.Default.Lock, contentDescription = "محمي", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showSwitchUserDialog = false
                        onNavigateTo(Screen.Users.route)
                    }) {
                        Text("إدارة الموظفين والصلاحيات")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSwitchUserDialog = false }) {
                        Text("إغلاق")
                    }
                }
            )
        }

        // --- Switch User PIN Confirmation Dialog ---
        if (targetUserToSwitch != null) {
            val target = targetUserToSwitch!!
            AlertDialog(
                onDismissRequest = { targetUserToSwitch = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("الرمز السري لحساب ${target.fullName}", fontSize = 15.sp)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "أدخل الرمز السري الخاص بالموظف ${target.fullName} (${viewModel.getRoleArabicName(target.role)}):",
                            fontSize = 13.sp
                        )
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = {
                                pinInput = it
                                pinError = null
                            },
                            label = { Text("الرمز السري") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (pinError != null) {
                            Text(
                                text = pinError!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.switchUser(target, pinInput.trim()) { success, msg ->
                                if (success) {
                                    targetUserToSwitch = null
                                } else {
                                    pinError = msg
                                }
                            }
                        }
                    ) {
                        Text("دخول")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { targetUserToSwitch = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}

@Composable
fun QuickActionItem(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp, pressedElevation = 3.dp),
        modifier = modifier.height(88.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp, horizontal = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(color.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Subtle top accent line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(color.copy(alpha = 0.85f))
                    .align(Alignment.TopCenter)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, start = 14.dp, end = 14.dp, bottom = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = color.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = color,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BarItem(
    label: String,
    value: String,
    fraction: Float,
    color: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(color, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(MaterialTheme.colorScheme.outlineVariant, CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0.03f, 1f))
                    .height(10.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(color.copy(alpha = 0.75f), color)
                        ),
                        CircleShape
                    )
            )
        }
    }
}

@Composable
fun InvoiceListItem(
    invoice: Invoice,
    settings: com.example.data.model.StoreSettings?,
    onClick: () -> Unit
) {
    val isSale = invoice.invoiceType == "SALE"
    val accentColor = if (isSale) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSale) Icons.Default.TrendingUp else Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = invoice.invoiceNumber,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (invoice.paymentType == "CASH") Color(0xFF059669).copy(alpha = 0.12f) else Color(0xFFD97706).copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = if (invoice.paymentType == "CASH") "نقداً" else "آجل",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (invoice.paymentType == "CASH") Color(0xFF059669) else Color(0xFFD97706),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${invoice.partyName} • ${Formatters.formatDate(invoice.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Formatters.formatMoney(invoice.totalAmount, settings),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (invoice.status == "CANCELLED") Color.Gray else accentColor
                )
                if (invoice.status == "CANCELLED") {
                    Text(
                        text = "ملغاة",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
