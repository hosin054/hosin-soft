package com.example.ui.screens.products

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Product
import com.example.ui.MainViewModel
import com.example.ui.components.BarcodeScannerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormScreen(
    productId: Long,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val existingProduct = remember(allProducts, productId) {
        if (productId != 0L) allProducts.find { it.id == productId } else null
    }

    var sku by remember(existingProduct) { mutableStateOf(existingProduct?.sku ?: "") }
    var barcode by remember(existingProduct) { mutableStateOf(existingProduct?.barcode ?: "") }
    var name by remember(existingProduct) { mutableStateOf(existingProduct?.name ?: "") }
    var description by remember(existingProduct) { mutableStateOf(existingProduct?.description ?: "") }
    var category by remember(existingProduct) { mutableStateOf(existingProduct?.category ?: "عام") }
    var brand by remember(existingProduct) { mutableStateOf(existingProduct?.brand ?: "") }
    var mainUnit by remember(existingProduct) { mutableStateOf(existingProduct?.mainUnit ?: "كرتون") }
    var subUnit by remember(existingProduct) { mutableStateOf(existingProduct?.subUnit ?: "حبة") }
    var conversionFactor by remember(existingProduct) { mutableStateOf(existingProduct?.conversionFactor?.toInt()?.toString() ?: "1") }
    var purchasePrice by remember(existingProduct) { mutableStateOf(existingProduct?.purchasePrice?.toString() ?: "0.0") }
    var cashSalePrice by remember(existingProduct) { mutableStateOf(existingProduct?.cashSalePrice?.toString() ?: "0.0") }
    var creditSalePrice by remember(existingProduct) { mutableStateOf(existingProduct?.creditSalePrice?.toString() ?: "0.0") }
    var initialStockMainUnits by remember(existingProduct) {
        val factor = existingProduct?.conversionFactor ?: 1.0
        val mainQty = if (factor > 0) (existingProduct?.currentStockSubUnits ?: 0.0) / factor else 0.0
        mutableStateOf(mainQty.toString())
    }
    var minStockSubUnits by remember(existingProduct) { mutableStateOf(existingProduct?.minStockSubUnits?.toInt()?.toString() ?: "5") }
    var supplierName by remember(existingProduct) { mutableStateOf(existingProduct?.supplierName ?: "") }

    var showScanner by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (productId == 0L) "إضافة منتج جديد" else "تعديل بيانات المنتج") },
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("اسم المنتج *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("الباركود") },
                    trailingIcon = {
                        IconButton(onClick = { showScanner = true }) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "مسح")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text("رمز الصنف SKU") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("التصنيف") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("العلامة التجارية") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Units configuration
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "نظام الوحدات ومعامل التحويل",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = mainUnit,
                            onValueChange = { mainUnit = it },
                            label = { Text("الوحدة الكبرى (مثال: كرتون)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = subUnit,
                            onValueChange = { subUnit = it },
                            label = { Text("الوحدة الصغرى (مثال: حبة)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = conversionFactor,
                        onValueChange = { conversionFactor = it },
                        label = { Text("معامل التحويل (كم حبة في الكرتون الواحد؟)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Prices
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "الأسعار (للوحدة الكبرى: $mainUnit)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = purchasePrice,
                        onValueChange = { purchasePrice = it },
                        label = { Text("سعر الشراء (التكلفة)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = cashSalePrice,
                            onValueChange = { cashSalePrice = it },
                            label = { Text("سعر البيع النقدي") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = creditSalePrice,
                            onValueChange = { creditSalePrice = it },
                            label = { Text("سعر البيع الآجل") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Stock & Supplier
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = initialStockMainUnits,
                    onValueChange = { initialStockMainUnits = it },
                    label = { Text("الكمية الحالية ($mainUnit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = minStockSubUnits,
                    onValueChange = { minStockSubUnits = it },
                    label = { Text("حد الطلب الأدنى ($subUnit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = supplierName,
                onValueChange = { supplierName = it },
                label = { Text("اسم المورد الافتراضي (اختياري)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("وصف المنتج أو ملاحظات (اختياري)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val factor = conversionFactor.toDoubleOrNull() ?: 1.0
                    val mainQty = initialStockMainUnits.toDoubleOrNull() ?: 0.0
                    val subUnitsTotal = mainQty * maxOf(1.0, factor)

                    val p = Product(
                        id = productId,
                        sku = sku.trim(),
                        barcode = barcode.trim(),
                        name = name.trim(),
                        description = description.trim(),
                        category = category.trim().ifBlank { "عام" },
                        brand = brand.trim(),
                        mainUnit = mainUnit.trim().ifBlank { "كرتون" },
                        subUnit = subUnit.trim().ifBlank { "حبة" },
                        conversionFactor = maxOf(1.0, factor),
                        purchasePrice = purchasePrice.toDoubleOrNull() ?: 0.0,
                        cashSalePrice = cashSalePrice.toDoubleOrNull() ?: 0.0,
                        creditSalePrice = creditSalePrice.toDoubleOrNull() ?: 0.0,
                        currentStockSubUnits = subUnitsTotal,
                        minStockSubUnits = minStockSubUnits.toDoubleOrNull() ?: 5.0,
                        supplierName = supplierName.trim(),
                        isActive = true
                    )
                    viewModel.saveProduct(p) {
                        onBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("حفظ المنتج", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showScanner) {
        BarcodeScannerDialog(
            onDismiss = { showScanner = false },
            onBarcodeScanned = { scanned ->
                barcode = scanned
                showScanner = false
            }
        )
    }
}
