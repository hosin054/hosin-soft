package com.example.ui.screens.importexport

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
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
    val context = LocalContext.current
    var importType by remember { mutableStateOf("PRODUCTS") }
    
    val sampleProductsCsv = """اسم المنتج,الباركود,التصنيف,الوحدة الكبرى,الوحدة الصغرى,معامل التحويل,سعر الشراء,سعر البيع النقدي,سعر البيع الآجل,الكمية الحالية,حد التنبيه
عصير تفاح طبيعي 1 لتر,6281009901,عصائر ومشروبات,كرتون,علبة,12,45.0,5.0,5.5,120,20
بسكويت شاي مالح 100جم,6281009902,حلويات وبسكويت,كرتون,باكيت,24,36.0,2.0,2.25,240,48
أرز بسمتي فاخر 5 كجم,6281009903,مواد غذائية,كيس,كيلو,5,35.0,42.0,45.0,50,10
زيت زيتون بكر ممتاز 500مل,6281009904,زيوت ودهون,كرتون,زجاجة,12,120.0,14.0,15.0,36,6"""

    val sampleCustomersCsv = """اسم العميل,رقم الهاتف,العنوان / المدينة,سقف الائتمان,الرصيد الافتتاحي
شركة الأمل للتجارة والتوزيع,0551234567,الرياض - حي الملز,10000,0
مؤسسة النور للمقاولات,0509876543,جدة - طريق الملك,5000,500
سوبرماركت البركة,0543216789,الدمام - السوق المركزي,15000,1200"""

    var rawCsvText by remember { mutableStateOf(sampleProductsCsv) }
    var validationErrors by remember { mutableStateOf<List<String>>(emptyList()) }
    var parsedCount by remember { mutableStateOf(0) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val reader = BufferedReader(InputStreamReader(stream, "UTF-8"))
                    val content = reader.readText().removePrefix("\uFEFF") // Strip UTF-8 BOM if present
                    rawCsvText = content
                    validationErrors = emptyList()
                    viewModel.showMessage("تم قراءة الملف بنجاح! اضغط 'فحص والتحقق' للتأكد")
                }
            } catch (e: Exception) {
                viewModel.showMessage("خطأ أثناء قراءة الملف: ${e.localizedMessage}")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "استيراد وتعيين البيانات من ملف Excel أو CSV:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            FilterChip(
                selected = importType == "PRODUCTS",
                onClick = {
                    importType = "PRODUCTS"
                    rawCsvText = sampleProductsCsv
                    validationErrors = emptyList()
                    parsedCount = 0
                },
                label = { Text("استيراد منتجات") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            FilterChip(
                selected = importType == "CUSTOMERS",
                onClick = {
                    importType = "CUSTOMERS"
                    rawCsvText = sampleCustomersCsv
                    validationErrors = emptyList()
                    parsedCount = 0
                },
                label = { Text("استيراد عملاء") },
                modifier = Modifier.weight(1f)
            )
        }

        // Action tools (Choose file, Load sample, Export Template)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = { filePickerLauncher.launch("*/*") },
                modifier = Modifier.weight(1.2f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("اختيار ملف", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = {
                    rawCsvText = if (importType == "PRODUCTS") sampleProductsCsv else sampleCustomersCsv
                    validationErrors = emptyList()
                    parsedCount = 0
                    viewModel.showMessage("تم تعبئة النموذج التجريبي")
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("نموذج تجريبي", fontSize = 11.sp)
            }

            OutlinedButton(
                onClick = {
                    val filename = if (importType == "PRODUCTS") "قالب_استيراد_المنتجات.csv" else "قالب_استيراد_العملاء.csv"
                    val content = if (importType == "PRODUCTS") sampleProductsCsv else sampleCustomersCsv
                    // Prepend UTF-8 BOM so Excel opens Arabic text smoothly
                    val bomContent = "\uFEFF$content"
                    BackupHelper.shareText(context, bomContent, filename)
                },
                modifier = Modifier.weight(1.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("مشاركة القالب", fontSize = 11.sp)
            }
        }

        OutlinedTextField(
            value = rawCsvText,
            onValueChange = {
                rawCsvText = it
                validationErrors = emptyList()
                parsedCount = 0
            },
            label = { Text("محتوى الملف / البيانات (الصف الأول ترويسة الأعمدة)") },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        )

        // Validation & Execute Buttons
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    val lines = rawCsvText.lines().map { it.trim() }.filter { it.isNotBlank() }
                    val errors = mutableListOf<String>()
                    if (lines.size <= 1) {
                        errors.add("الملف فارغ أو لا يحتوي على صفوف بيانات بعد الترويسة")
                    } else {
                        val headerCols = parseCsvLine(lines[0])
                        for (i in 1 until lines.size) {
                            val cols = parseCsvLine(lines[i])
                            if (cols.isEmpty() || cols[0].isBlank()) {
                                errors.add("السطر ${i + 1}: حقل الاسم إلزامي ومفقود")
                            }
                        }
                    }
                    validationErrors = errors
                    parsedCount = if (lines.size > 1) lines.size - 1 else 0
                    if (errors.isEmpty() && parsedCount > 0) {
                        viewModel.showMessage("تم فحص البيانات بنجاح: $parsedCount سجل صالح للاستيراد")
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
                    val lines = rawCsvText.lines().map { it.trim() }.filter { it.isNotBlank() }
                    if (lines.size <= 1) {
                        viewModel.showMessage("لا توجد بيانات للاستيراد")
                        return@Button
                    }
                    val headerCols = parseCsvLine(lines[0]).map { it.lowercase().trim() }
                    var imported = 0
                    
                    if (importType == "PRODUCTS") {
                        // Smart column mapping
                        fun findIdx(vararg keys: String): Int {
                            for ((i, h) in headerCols.withIndex()) {
                                if (keys.any { h.contains(it) }) return i
                            }
                            return -1
                        }

                        val nameIdx = findIdx("اسم", "صنف", "منتج", "name")
                        val barcodeIdx = findIdx("باركود", "barcode", "كود")
                        val catIdx = findIdx("تصنيف", "قسم", "category")
                        val mainUnitIdx = findIdx("كبرى", "رئيسية", "mainunit")
                        val subUnitIdx = findIdx("صغرى", "تجزئة", "subunit")
                        val convIdx = findIdx("تحويل", "معامل", "conversion")
                        val buyIdx = findIdx("شراء", "تكلفة", "purchase")
                        val cashIdx = findIdx("نقدي", "سعر البيع", "cash")
                        val creditIdx = findIdx("آجل", "credit")
                        val stockIdx = findIdx("كمية", "رصيد", "مخزون", "stock")
                        val alertIdx = findIdx("تنبيه", "طلب", "alert")

                        for (i in 1 until lines.size) {
                            val cols = parseCsvLine(lines[i])
                            val pName = if (nameIdx >= 0 && nameIdx < cols.size) cols[nameIdx] else cols.getOrElse(0) { "" }
                            if (pName.isNotBlank()) {
                                val p = Product(
                                    name = pName,
                                    barcode = if (barcodeIdx >= 0 && barcodeIdx < cols.size) cols[barcodeIdx] else cols.getOrElse(1) { "" },
                                    category = if (catIdx >= 0 && catIdx < cols.size) cols[catIdx] else cols.getOrElse(2) { "عام" },
                                    mainUnit = if (mainUnitIdx >= 0 && mainUnitIdx < cols.size) cols[mainUnitIdx] else "حبة",
                                    subUnit = if (subUnitIdx >= 0 && subUnitIdx < cols.size) cols[subUnitIdx] else "حبة",
                                    conversionFactor = if (convIdx >= 0 && convIdx < cols.size) cols[convIdx].toDoubleOrNull() ?: 1.0 else 1.0,
                                    purchasePrice = if (buyIdx >= 0 && buyIdx < cols.size) cols[buyIdx].toDoubleOrNull() ?: 0.0 else cols.getOrElse(3) { "0" }.toDoubleOrNull() ?: 0.0,
                                    cashSalePrice = if (cashIdx >= 0 && cashIdx < cols.size) cols[cashIdx].toDoubleOrNull() ?: 0.0 else cols.getOrElse(4) { "0" }.toDoubleOrNull() ?: 0.0,
                                    creditSalePrice = if (creditIdx >= 0 && creditIdx < cols.size) cols[creditIdx].toDoubleOrNull() ?: 0.0 else cols.getOrElse(4) { "0" }.toDoubleOrNull() ?: 0.0,
                                    currentStockSubUnits = if (stockIdx >= 0 && stockIdx < cols.size) cols[stockIdx].toDoubleOrNull() ?: 0.0 else cols.getOrElse(5) { "0" }.toDoubleOrNull() ?: 0.0,
                                    minStockSubUnits = if (alertIdx >= 0 && alertIdx < cols.size) cols[alertIdx].toDoubleOrNull() ?: 5.0 else 5.0
                                )
                                viewModel.saveProduct(p) {}
                                imported++
                            }
                        }
                    } else {
                        // Customers mapping
                        fun findIdx(vararg keys: String): Int {
                            for ((i, h) in headerCols.withIndex()) {
                                if (keys.any { h.contains(it) }) return i
                            }
                            return -1
                        }

                        val nameIdx = findIdx("اسم", "العميل", "name")
                        val phoneIdx = findIdx("هاتف", "جوال", "phone")
                        val addressIdx = findIdx("عنوان", "مدينة", "address")
                        val limitIdx = findIdx("سقف", "ائتمان", "limit")
                        val balIdx = findIdx("رصيد", "balance")

                        for (i in 1 until lines.size) {
                            val cols = parseCsvLine(lines[i])
                            val cName = if (nameIdx >= 0 && nameIdx < cols.size) cols[nameIdx] else cols.getOrElse(0) { "" }
                            if (cName.isNotBlank()) {
                                val c = Customer(
                                    name = cName,
                                    phone = if (phoneIdx >= 0 && phoneIdx < cols.size) cols[phoneIdx] else cols.getOrElse(1) { "" },
                                    address = if (addressIdx >= 0 && addressIdx < cols.size) cols[addressIdx] else cols.getOrElse(2) { "" },
                                    creditLimit = if (limitIdx >= 0 && limitIdx < cols.size) cols[limitIdx].toDoubleOrNull() ?: 0.0 else 0.0,
                                    currentBalance = if (balIdx >= 0 && balIdx < cols.size) cols[balIdx].toDoubleOrNull() ?: 0.0 else 0.0
                                )
                                viewModel.saveCustomer(c) {}
                                imported++
                            }
                        }
                    }
                    viewModel.showMessage("تم استيراد $imported سجل بنجاح إلى قاعدة البيانات!")
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

private fun parseCsvLine(line: String): List<String> {
    val result = mutableListOf<String>()
    var current = StringBuilder()
    var inQuotes = false
    val delimiter = if (line.contains(";") && !line.contains(",")) ';' else ','
    var i = 0
    while (i < line.length) {
        val c = line[i]
        if (c == '"') {
            if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                current.append('"')
                i++
            } else {
                inQuotes = !inQuotes
            }
        } else if (c == delimiter && !inQuotes) {
            result.add(current.toString().trim())
            current = StringBuilder()
        } else {
            current.append(c)
        }
        i++
    }
    result.add(current.toString().trim())
    return result
}
