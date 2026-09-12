package com.example.ui.screens.importexport

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Customer
import com.example.data.model.Product
import com.example.data.model.Supplier
import com.example.ui.MainViewModel
import com.example.ui.util.BackupHelper
import com.example.ui.util.Formatters
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportExportScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val products by viewModel.allProducts.collectAsStateWithLifecycle()
    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()
    val invoices by viewModel.allInvoices.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Export, 1 = Import

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("استيراد وتصدير البيانات (Excel / CSV)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("تصدير البيانات (Export)") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("استيراد وتعيين الأعمدة (Import)") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                // Export Section
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "اختر الجدول المراد تصديره إلى ملف CSV / Excel جاهز:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    ExportActionCard(
                        title = "تصدير قائمة المنتجات والمخزون",
                        subtitle = "${products.size} صنف مسجل",
                        icon = Icons.Default.Inventory2,
                        onClick = {
                            val sb = StringBuilder()
                            sb.append("ID,SKU,Barcode,Name,Category,MainUnit,SubUnit,ConversionFactor,PurchasePrice,CashPrice,CreditPrice,CurrentStock\n")
                            for (p in products) {
                                sb.append("${p.id},\"${p.sku}\",\"${p.barcode}\",\"${p.name}\",\"${p.category}\",\"${p.mainUnit}\",\"${p.subUnit}\",${p.conversionFactor},${p.purchasePrice},${p.cashSalePrice},${p.creditSalePrice},${p.currentStockSubUnits}\n")
                            }
                            BackupHelper.shareText(context, sb.toString(), "ملف_المنتجات.csv")
                        }
                    )

                    ExportActionCard(
                        title = "تصدير سجل العملاء والأرصدة",
                        subtitle = "${customers.size} عميل مسجل",
                        icon = Icons.Default.Person,
                        onClick = {
                            val sb = StringBuilder()
                            sb.append("ID,Name,Phone,Address,CreditLimit,CurrentBalance,TotalSales,TotalPaid\n")
                            for (c in customers) {
                                sb.append("${c.id},\"${c.name}\",\"${c.phone}\",\"${c.address}\",${c.creditLimit},${c.currentBalance},${c.totalSales},${c.totalPaid}\n")
                            }
                            BackupHelper.shareText(context, sb.toString(), "ملف_العملاء.csv")
                        }
                    )

                    ExportActionCard(
                        title = "تصدير سجل الموردين",
                        subtitle = "${suppliers.size} مورد مسجل",
                        icon = Icons.Default.LocalShipping,
                        onClick = {
                            val sb = StringBuilder()
                            sb.append("ID,Name,Phone,Address,CurrentBalance,TotalPurchases,TotalPaid\n")
                            for (s in suppliers) {
                                sb.append("${s.id},\"${s.name}\",\"${s.phone}\",\"${s.address}\",${s.currentBalance},${s.totalPurchases},${s.totalPaid}\n")
                            }
                            BackupHelper.shareText(context, sb.toString(), "ملف_الموردين.csv")
                        }
                    )

                    ExportActionCard(
                        title = "تصدير سجل الفواتير والمبيعات",
                        subtitle = "${invoices.size} فاتورة مسجلة",
                        icon = Icons.Default.ReceiptLong,
                        onClick = {
                            val sb = StringBuilder()
                            sb.append("InvoiceNo,Type,Party,Payment,Subtotal,Discount,Tax,Total,Paid,Remaining,Profit,Date\n")
                            for (inv in invoices) {
                                sb.append("${inv.invoiceNumber},${inv.invoiceType},\"${inv.partyName}\",${inv.paymentType},${inv.subtotal},${inv.discountAmount},${inv.taxAmount},${inv.totalAmount},${inv.paidAmount},${inv.remainingAmount},${inv.profit},${Formatters.formatDate(inv.createdAt)}\n")
                            }
                            BackupHelper.shareText(context, sb.toString(), "ملف_الفواتير.csv")
                        }
                    )
                }
            } else {
                // Import Section
                ImportSectionContent(viewModel)
            }
        }
    }
}

@Composable
fun ExportActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onClick, shape = RoundedCornerShape(8.dp)) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("تصدير CSV")
            }
        }
    }
}

@Composable
fun ImportSectionContent(viewModel: MainViewModel) {
    var importType by remember { mutableStateOf("PRODUCTS") }
    var rawCsvText by remember {
        mutableStateOf("الاسم,الباركود,التصنيف,سعر الشراء,سعر البيع,الكمية\nعصير تفاح طبيعي 1 لتر,6281009901,عصائر,4.5,6.0,30\nبسكويت شاي مالح,6281009902,بسكويت,2.0,3.0,50")
    }
    var validationErrors by remember { mutableStateOf<List<String>>(emptyList()) }
    var parsedCount by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "استيراد بيانات من نص CSV أو إكسل مع التحقق الذكي:",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            FilterChip(
                selected = importType == "PRODUCTS",
                onClick = { importType = "PRODUCTS" },
                label = { Text("استيراد منتجات") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            FilterChip(
                selected = importType == "CUSTOMERS",
                onClick = { importType = "CUSTOMERS" },
                label = { Text("استيراد عملاء") },
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = rawCsvText,
            onValueChange = {
                rawCsvText = it
                validationErrors = emptyList()
                parsedCount = 0
            },
            label = { Text("محتوى CSV (الصف الأول هو ترويسة الأعمدة)") },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        )

        // Validation & Execute Buttons
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    val lines = rawCsvText.lines().filter { it.isNotBlank() }
                    val errors = mutableListOf<String>()
                    if (lines.size <= 1) {
                        errors.add("الملف فارغ أو لا يحتوي على صفوف بيانات بعد الترويسة")
                    } else {
                        // Check header
                        val headers = lines[0].split(",").map { it.trim().replace("\"", "") }
                        for (i in 1 until lines.size) {
                            val cols = lines[i].split(",").map { it.trim().replace("\"", "") }
                            if (cols.isEmpty() || cols[0].isBlank()) {
                                errors.add("السطر ${i + 1}: حقل الاسم إلزامي ومفقود")
                            }
                        }
                    }
                    validationErrors = errors
                    parsedCount = if (lines.size > 1) lines.size - 1 else 0
                    if (errors.isEmpty() && parsedCount > 0) {
                        viewModel.showMessage("تم فحص الملف بنجاح، $parsedCount سجل صالح للاستيراد")
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.FactCheck, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("فحص والتحقق")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    val lines = rawCsvText.lines().filter { it.isNotBlank() }
                    if (lines.size <= 1) {
                        viewModel.showMessage("لا توجد بيانات للاستيراد")
                        return@Button
                    }
                    var imported = 0
                    if (importType == "PRODUCTS") {
                        for (i in 1 until lines.size) {
                            val cols = lines[i].split(",").map { it.trim().replace("\"", "") }
                            if (cols.isNotEmpty() && cols[0].isNotBlank()) {
                                val p = Product(
                                    name = cols.getOrElse(0) { "منتج $i" },
                                    barcode = cols.getOrElse(1) { "" },
                                    category = cols.getOrElse(2) { "عام" },
                                    purchasePrice = cols.getOrElse(3) { "0" }.toDoubleOrNull() ?: 0.0,
                                    cashSalePrice = cols.getOrElse(4) { "0" }.toDoubleOrNull() ?: 0.0,
                                    creditSalePrice = cols.getOrElse(4) { "0" }.toDoubleOrNull() ?: 0.0,
                                    currentStockSubUnits = cols.getOrElse(5) { "0" }.toDoubleOrNull() ?: 0.0
                                )
                                viewModel.saveProduct(p) {}
                                imported++
                            }
                        }
                    } else {
                        for (i in 1 until lines.size) {
                            val cols = lines[i].split(",").map { it.trim().replace("\"", "") }
                            if (cols.isNotEmpty() && cols[0].isNotBlank()) {
                                val c = Customer(
                                    name = cols.getOrElse(0) { "عميل $i" },
                                    phone = cols.getOrElse(1) { "" },
                                    address = cols.getOrElse(2) { "" }
                                )
                                viewModel.saveCustomer(c) {}
                                imported++
                            }
                        }
                    }
                    viewModel.showMessage("تم استيراد $imported سجل بنجاح!")
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Upload, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("بدء الاستيراد الآن")
            }
        }

        if (validationErrors.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = "تنبيهات الفحص:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    for (err in validationErrors) {
                        Text(text = "• $err", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
