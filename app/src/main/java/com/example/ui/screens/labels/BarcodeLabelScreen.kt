package com.example.ui.screens.labels

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Product
import com.example.ui.MainViewModel
import com.example.ui.util.Formatters
import com.example.ui.util.InvoicePrinter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeLabelScreen(
    viewModel: MainViewModel,
    initialProductId: Long = 0L,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val products by viewModel.allProducts.collectAsStateWithLifecycle()

    var selectedProduct by remember(products, initialProductId) {
        mutableStateOf(products.find { it.id == initialProductId } ?: products.firstOrNull())
    }

    var showProductPicker by remember { mutableStateOf(false) }

    // Customization states
    var customName by remember(selectedProduct) { mutableStateOf(selectedProduct?.name ?: "") }
    var customBarcode by remember(selectedProduct) { mutableStateOf(selectedProduct?.barcode?.ifBlank { selectedProduct?.sku } ?: "123456789012") }
    var isMainUnit by remember(selectedProduct) { mutableStateOf(true) }
    var customPrice by remember(selectedProduct, isMainUnit) {
        val prod = selectedProduct
        val price = if (prod == null) {
            0.0
        } else if (isMainUnit) {
            prod.cashSalePrice
        } else {
            if (prod.conversionFactor > 0) prod.cashSalePrice / prod.conversionFactor else prod.cashSalePrice
        }
        mutableStateOf(String.format(java.util.Locale.ENGLISH, "%.2f", price))
    }
    var labelType by remember { mutableStateOf(0) } // 0 = Shelf Tag, 1 = Barcode Sticker, 2 = QR Tag

    val storeName = settings?.storeName ?: "حسين سوفت"
    val currency = settings?.currencySymbol ?: "ر.س"
    val unitName = if (isMainUnit) (selectedProduct?.mainUnit ?: "حبة") else (selectedProduct?.subUnit ?: "حبة")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("طباعة وتصميم ملصقات الأسعار") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val priceNum = customPrice.toDoubleOrNull() ?: 0.0
                        val text = InvoicePrinter.generateShelfLabelText(
                            productName = customName,
                            barcode = customBarcode,
                            price = priceNum,
                            unitName = unitName,
                            storeName = storeName,
                            currency = currency
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "ملصق تسعير: $customName")
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(intent, "مشاركة بيانات الملصق عبر:"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "مشاركة الملصق", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Product Selector Button
            OutlinedCard(
                onClick = { showProductPicker = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "المنتج المحدد:", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = selectedProduct?.name ?: "اضغط لاختيار منتج",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Button(onClick = { showProductPicker = true }) {
                        Text("تغيير المنتج")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Label Type Chips
            Text(text = "نوع ونمط الملصق:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = labelType == 0,
                    onClick = { labelType = 0 },
                    label = { Text("بطاقة تسعير الرف") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = labelType == 1,
                    onClick = { labelType = 1 },
                    label = { Text("ملصق باركود للمنتج") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = labelType == 2,
                    onClick = { labelType = 2 },
                    label = { Text("رمز QR الذكي") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Unit Selector if product has subUnit
            if (selectedProduct != null && selectedProduct!!.conversionFactor > 1.0) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isMainUnit,
                        onClick = { isMainUnit = true },
                        label = { Text("الوحدة الكبرى (${selectedProduct!!.mainUnit})") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !isMainUnit,
                        onClick = { isMainUnit = false },
                        label = { Text("الوحدة الصغرى (${selectedProduct!!.subUnit})") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Editable customization
            OutlinedTextField(
                value = customName,
                onValueChange = { customName = it },
                label = { Text("اسم الصنف على الملصق") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = customPrice,
                    onValueChange = { customPrice = it },
                    label = { Text("السعر المعروض") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = customBarcode,
                    onValueChange = { customBarcode = it },
                    label = { Text("رقم الباركود / الكود") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // LIVE VISUAL PREVIEW
            Text(text = "معاينة الملصق المباشرة (Live Label Preview):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                when (labelType) {
                    0 -> ShelfTagPreview(
                        storeName = storeName,
                        productName = customName,
                        price = customPrice,
                        currency = currency,
                        unitName = unitName,
                        barcode = customBarcode
                    )
                    1 -> BarcodeStickerPreview(
                        productName = customName,
                        price = customPrice,
                        currency = currency,
                        barcode = customBarcode
                    )
                    2 -> QrCodeTagPreview(
                        storeName = storeName,
                        productName = customName,
                        price = customPrice,
                        currency = currency,
                        barcode = customBarcode
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Actions
            Button(
                onClick = {
                    val priceNum = customPrice.toDoubleOrNull() ?: 0.0
                    val text = InvoicePrinter.generateShelfLabelText(
                        productName = customName,
                        barcode = customBarcode,
                        price = priceNum,
                        unitName = unitName,
                        storeName = storeName,
                        currency = currency
                    )
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "طباعة ملصق تسعير: $customName")
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(intent, "إرسال إلى الطابعة أو التطبيقات:"))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Print, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("طباعة / إرسال الملصق للأجهزة")
            }
        }
    }

    // Product Picker Dialog
    if (showProductPicker) {
        var pSearch by remember { mutableStateOf("") }
        val filtered = remember(pSearch, products) {
            if (pSearch.isBlank()) products else products.filter {
                it.name.contains(pSearch, ignoreCase = true) || it.barcode.contains(pSearch, ignoreCase = true)
            }
        }

        AlertDialog(
            onDismissRequest = { showProductPicker = false },
            title = { Text("اختر المنتج لتوليد الملصق") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp)) {
                    OutlinedTextField(
                        value = pSearch,
                        onValueChange = { pSearch = it },
                        placeholder = { Text("بحث عن منتج...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filtered.size) { idx ->
                            val prod = filtered[idx]
                            Card(
                                onClick = {
                                    selectedProduct = prod
                                    showProductPicker = false
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = prod.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(text = "كود: ${prod.barcode.ifBlank { prod.sku }}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                    Text(
                                        text = Formatters.formatMoney(prod.cashSalePrice, settings),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProductPicker = false }) {
                    Text("إغلاق")
                }
            }
        )
    }
}

@Composable
fun ShelfTagPreview(
    storeName: String,
    productName: String,
    price: String,
    currency: String,
    unitName: String,
    barcode: String
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .width(280.dp)
            .border(2.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Store Brand
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A), RoundedCornerShape(4.dp))
                    .padding(vertical = 4.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = storeName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text(text = "سعر الرف", color = Color(0xFFFBBF24), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Product Name
            Text(
                text = productName.ifBlank { "اسم المنتج التجريبي" },
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                color = Color(0xFF1E293B),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Price Big Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFEF3C7), RoundedCornerShape(6.dp))
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = price.ifBlank { "0.00" },
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFB45309),
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(text = currency, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                    Text(text = "/ $unitName", fontSize = 10.sp, color = Color(0xFF78350F))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Barcode graphic representation
            BarcodeCanvas(
                code = barcode.ifBlank { "628100123456" },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = barcode.ifBlank { "628100123456" },
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF334155)
            )
        }
    }
}

@Composable
fun BarcodeStickerPreview(
    productName: String,
    price: String,
    currency: String,
    barcode: String
) {
    Card(
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .width(240.dp)
            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(6.dp))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = productName.ifBlank { "منتج تجريبي" },
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(6.dp))

            BarcodeCanvas(
                code = barcode.ifBlank { "123456789" },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = barcode.ifBlank { "123456789" },
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Black
                )
                Text(
                    text = "$price $currency",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
fun QrCodeTagPreview(
    storeName: String,
    productName: String,
    price: String,
    currency: String,
    barcode: String
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .width(260.dp)
            .border(1.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = storeName, fontSize = 10.sp, color = Color.Gray)
            Text(
                text = productName.ifBlank { "اسم المنتج" },
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Simulated QR Pattern
            QrPatternCanvas(
                seed = (barcode + productName).hashCode(),
                modifier = Modifier.size(90.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$price $currency",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = Color(0xFF0F172A)
            )
            Text(
                text = "كود: $barcode",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun BarcodeCanvas(code: String, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val totalWidth = size.width
        val barHeight = size.height
        val barCount = 48
        val barWidth = totalWidth / (barCount * 1.5f)

        var currentX = 10f
        val hash = code.hashCode()

        for (i in 0 until barCount) {
            val isThick = ((hash shr (i % 30)) and 1) == 1 || (i % 5 == 0)
            val isGap = (i % 7 == 3)
            val thickness = if (isThick) barWidth * 1.8f else barWidth * 0.9f

            if (!isGap) {
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(currentX, 0f),
                    size = Size(thickness, barHeight)
                )
            }
            currentX += thickness + (barWidth * 0.7f)
            if (currentX >= totalWidth - 10f) break
        }
    }
}

@Composable
fun QrPatternCanvas(seed: Int, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val gridSize = 11
        val cellSize = size.width / gridSize

        // Draw background
        drawRect(Color.White, Offset.Zero, size)

        // Draw Finder Patterns (Corners)
        drawFinderPattern(Offset(0f, 0f), cellSize * 3)
        drawFinderPattern(Offset(size.width - cellSize * 3, 0f), cellSize * 3)
        drawFinderPattern(Offset(0f, size.height - cellSize * 3), cellSize * 3)

        // Draw inner pseudo-random QR modules
        var currentSeed = seed
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                val inTopLeft = row < 3 && col < 3
                val inTopRight = row < 3 && col >= gridSize - 3
                val inBottomLeft = row >= gridSize - 3 && col < 3

                if (!inTopLeft && !inTopRight && !inBottomLeft) {
                    currentSeed = (currentSeed * 1103515245 + 12345) and 0x7fffffff
                    if (currentSeed % 2 == 0) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(col * cellSize, row * cellSize),
                            size = Size(cellSize, cellSize)
                        )
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFinderPattern(topLeft: Offset, patternSize: Float) {
    // Outer black
    drawRect(Color.Black, topLeft, Size(patternSize, patternSize))
    // Inner white
    val innerMargin = patternSize / 4f
    drawRect(Color.White, Offset(topLeft.x + innerMargin, topLeft.y + innerMargin), Size(patternSize - (innerMargin * 2), patternSize - (innerMargin * 2)))
    // Center black dot
    val centerMargin = patternSize / 3f
    drawRect(Color.Black, Offset(topLeft.x + centerMargin, topLeft.y + centerMargin), Size(patternSize - (centerMargin * 2), patternSize - (centerMargin * 2)))
}
