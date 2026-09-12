package com.example.ui.screens.dashboard

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.data.model.Invoice
import com.example.data.model.User
import com.example.ui.MainViewModel
import com.example.ui.navigation.Screen
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
            // Active User Quick Bar
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            pinError = null
                            pinInput = ""
                            showSwitchUserDialog = true
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentUser.fullName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = viewModel.getRoleArabicName(currentUser.role).split(" ").firstOrNull() ?: currentUser.role,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "اسم الدخول: @${currentUser.username} • اضغط هنا للتبديل السريع",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                pinError = null
                                pinInput = ""
                                showSwitchUserDialog = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تبديل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Low stock alert banner if any
            if (lowStock.isNotEmpty() || outOfStock.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp),
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

            // Quick Actions Bar
            item {
                Text(
                    text = "العمليات السريعة",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuickActionItem(
                        icon = Icons.Default.PointOfSale,
                        label = "بيع للزبون",
                        color = MaterialTheme.colorScheme.primary,
                        onClick = { onNavigateTo(Screen.Pos.route) }
                    )
                    QuickActionItem(
                        icon = Icons.Default.ShoppingCart,
                        label = "شراء للمحل",
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = { onNavigateTo(Screen.PurchaseForm.route) }
                    )
                    QuickActionItem(
                        icon = Icons.Default.ReceiptLong,
                        label = "سند قبض",
                        color = Color(0xFF15803D),
                        onClick = { onNavigateTo(Screen.Vouchers.route) }
                    )
                    QuickActionItem(
                        icon = Icons.Default.Payments,
                        label = "سند صرف",
                        color = Color(0xFFB45309),
                        onClick = { onNavigateTo(Screen.Vouchers.route) }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuickActionItem(
                        icon = Icons.Default.RequestQuote,
                        label = "عروض الأسعار",
                        color = Color(0xFF0284C7),
                        onClick = { onNavigateTo(Screen.Quotations.route) }
                    )
                    QuickActionItem(
                        icon = Icons.Default.QrCode,
                        label = "ملصقات الأسعار",
                        color = Color(0xFF7C3AED),
                        onClick = { onNavigateTo(Screen.BarcodeLabels.createRoute(0L)) }
                    )
                    QuickActionItem(
                        icon = Icons.Default.Inventory,
                        label = "المخزون والجرد",
                        color = Color(0xFFD97706),
                        onClick = { onNavigateTo(Screen.Inventory.route) }
                    )
                    QuickActionItem(
                        icon = Icons.Default.Assessment,
                        label = "التقارير المالية",
                        color = Color(0xFF4F46E5),
                        onClick = { onNavigateTo(Screen.Reports.route) }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    QuickActionItem(
                        icon = Icons.Default.CurrencyExchange,
                        label = "أسعار العملات",
                        color = Color(0xFF0D9488),
                        onClick = { onNavigateTo(Screen.Currencies.route) }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    QuickActionItem(
                        icon = Icons.Default.ManageAccounts,
                        label = "الموظفين والصلاحيات",
                        color = Color(0xFF673AB7),
                        onClick = { onNavigateTo(Screen.Users.route) }
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

            // Visual Trend Comparison Bar
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "مقارنة حركة الشهر الحالية",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val maxVal = maxOf(1.0, salesThisMonth, purchasesThisMonth, expensesThisMonth)

                        BarItem(
                            label = "المبيعات",
                            value = Formatters.formatMoney(salesThisMonth, settings),
                            fraction = (salesThisMonth / maxVal).toFloat(),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        BarItem(
                            label = "المشتريات",
                            value = Formatters.formatMoney(purchasesThisMonth, settings),
                            fraction = (purchasesThisMonth / maxVal).toFloat(),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        BarItem(
                            label = "المصروفات",
                            value = Formatters.formatMoney(expensesThisMonth, settings),
                            fraction = (expensesThisMonth / maxVal).toFloat(),
                            color = Color(0xFFDC2626)
                        )
                    }
                }
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
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(color.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(color.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 12.sp)
            Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color.LightGray.copy(alpha = 0.3f), CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                    .height(8.dp)
                    .background(color, CircleShape)
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
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (invoice.invoiceType == "SALE") MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.secondaryContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (invoice.invoiceType == "SALE") Icons.Default.TrendingUp else Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = if (invoice.invoiceType == "SALE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${invoice.invoiceNumber} - ${invoice.partyName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "${if (invoice.paymentType == "CASH") "نقداً" else "آجل"} • ${Formatters.formatDate(invoice.createdAt)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Formatters.formatMoney(invoice.totalAmount, settings),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (invoice.status == "CANCELLED") Color.Gray else MaterialTheme.colorScheme.primary
                )
                if (invoice.status == "CANCELLED") {
                    Text(
                        text = "ملغاة",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
