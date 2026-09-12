package com.example.ui.screens.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.ui.MainViewModel
import com.example.ui.util.BackupHelper
import com.example.ui.util.Formatters
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val invoices by viewModel.allInvoices.collectAsStateWithLifecycle()
    val expenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val products by viewModel.allProducts.collectAsStateWithLifecycle()
    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()

    var dateFilter by remember { mutableStateOf(1) } // 0 = Today, 1 = This Month, 2 = This Year, 3 = All

    val cal = Calendar.getInstance()
    val now = cal.timeInMillis

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
                cal.timeInMillis
            }
            else -> 0L
        }
    }

    val periodSales = invoices.filter { it.invoiceType == "SALE" && it.status == "COMPLETED" && it.createdAt >= fromTimestamp }
    val periodPurchases = invoices.filter { it.invoiceType == "PURCHASE" && it.status == "COMPLETED" && it.createdAt >= fromTimestamp }
    val periodExpenses = expenses.filter { it.createdAt >= fromTimestamp }

    val totalSales = periodSales.sumOf { it.totalAmount }
    val totalProfitGross = periodSales.sumOf { it.profit }
    val totalExpenses = periodExpenses.sumOf { it.amount }
    val netProfit = totalProfitGross - totalExpenses
    val totalPurchases = periodPurchases.sumOf { it.totalAmount }
    val totalTax = periodSales.sumOf { it.taxAmount }

    val totalCustomersDebt = customers.sumOf { maxOf(0.0, it.currentBalance) }
    val totalSuppliersPayable = suppliers.sumOf { maxOf(0.0, it.currentBalance) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("التقارير المالية والأرباح والخسائر") },
                actions = {
                    IconButton(onClick = {
                        val periodLabel = when (dateFilter) {
                            0 -> "اليوم"
                            1 -> "الشهر الحالي"
                            2 -> "العام الحالي"
                            else -> "كافة الفترات"
                        }
                        val text = """
                            تقرير مالي - ${settings?.storeName}
                            الفترة: $periodLabel
                            ------------------------
                            إجمالي المبيعات: ${Formatters.formatMoney(totalSales, settings)}
                            مجمل ربح المبيعات: ${Formatters.formatMoney(totalProfitGross, settings)}
                            إجمالي المصروفات: ${Formatters.formatMoney(totalExpenses, settings)}
                            صافي الربح النهائي: ${Formatters.formatMoney(netProfit, settings)}
                            ------------------------
                            إجمالي المشتريات: ${Formatters.formatMoney(totalPurchases, settings)}
                            ضريبة القيمة المضافة: ${Formatters.formatMoney(totalTax, settings)}
                            ديون العملاء (لنا): ${Formatters.formatMoney(totalCustomersDebt, settings)}
                            مستحقات الموردين (علينا): ${Formatters.formatMoney(totalSuppliersPayable, settings)}
                            ------------------------
                            تم الاستخراج بواسطة نظام حسين سوفت
                        """.trimIndent()
                        BackupHelper.shareText(context, text, "تقرير مالي $periodLabel")
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "مشاركة التقرير", tint = MaterialTheme.colorScheme.onPrimary)
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
            // Period Filter Tabs
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

            // Net Profit Highlight Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (netProfit >= 0) Color(0xFF15803D).copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "صافي الربح / الخسارة للفترة", fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = Formatters.formatMoney(netProfit, settings),
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            color = if (netProfit >= 0) Color(0xFF15803D) else MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "(مجمل ربح المبيعات: ${Formatters.formatMoney(totalProfitGross, settings)} - المصروفات: ${Formatters.formatMoney(totalExpenses, settings)})",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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
                        Text(text = "تفاصيل القوائم المالية", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        ReportRow(title = "إجمالي المبيعات", value = Formatters.formatMoney(totalSales, settings), isBold = true)
                        ReportRow(title = "عدد فواتير المبيعات", value = "${periodSales.size} فاتورة")
                        ReportRow(title = "مجمل ربح المبيعات", value = Formatters.formatMoney(totalProfitGross, settings), color = Color(0xFF15803D))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        ReportRow(title = "إجمالي المصروفات التشغيلية", value = Formatters.formatMoney(totalExpenses, settings), color = MaterialTheme.colorScheme.error)
                        ReportRow(title = "عدد بنود المصروفات", value = "${periodExpenses.size} حركة")
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        ReportRow(title = "صافي الربح النهائي", value = Formatters.formatMoney(netProfit, settings), isBold = true, color = if (netProfit >= 0) Color(0xFF15803D) else MaterialTheme.colorScheme.error)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        ReportRow(title = "إجمالي المشتريات والتوريدات", value = Formatters.formatMoney(totalPurchases, settings))
                        ReportRow(title = "ضريبة القيمة المضافة المحصلة", value = Formatters.formatMoney(totalTax, settings))
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

                        ReportRow(title = "ديون العملاء المطلوبة (لنا)", value = Formatters.formatMoney(totalCustomersDebt, settings), color = Color(0xFFC2410C), isBold = true)
                        ReportRow(title = "مستحقات الموردين الواجب سدادها (علينا)", value = Formatters.formatMoney(totalSuppliersPayable, settings), color = MaterialTheme.colorScheme.error, isBold = true)
                    }
                }
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
