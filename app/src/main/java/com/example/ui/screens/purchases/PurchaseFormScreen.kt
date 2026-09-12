package com.example.ui.screens.purchases

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.example.data.model.Supplier
import com.example.ui.MainViewModel
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseFormScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onInvoiceCreated: (Long) -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val products by viewModel.allProducts.collectAsStateWithLifecycle()
    val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()
    val purchaseItems by viewModel.purchaseItems.collectAsStateWithLifecycle()
    val selectedSupplier by viewModel.selectedSupplier.collectAsStateWithLifecycle()

    var referenceNumber by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var paymentType by remember { mutableStateOf(if (selectedSupplier != null) "CREDIT" else "CASH") }

    var showSupplierPicker by remember { mutableStateOf(false) }
    var showProductPicker by remember { mutableStateOf(false) }
    var showBarcodeScanner by remember { mutableStateOf(false) }

    val totalAmount = purchaseItems.sumOf { it.total }
    var paidAmountInput by remember(totalAmount, paymentType) {
        mutableStateOf(if (paymentType == "CASH") String.format(java.util.Locale.ENGLISH, "%.2f", totalAmount) else "0.0")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("فاتورة شراء وتوريد جديدة") },
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
        },
        bottomBar = {
            if (purchaseItems.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "عدد الأصناف: ${purchaseItems.size}")
                            Text(
                                text = "الإجمالي: ${Formatters.formatMoney(totalAmount, settings)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                val paid = if (paymentType == "CASH") totalAmount else (paidAmountInput.toDoubleOrNull() ?: 0.0)
                                viewModel.completePurchase(
                                    paymentType = paymentType,
                                    paidAmount = paid,
                                    referenceNumber = referenceNumber.trim(),
                                    notes = notes.trim(),
                                    onSuccess = { invId ->
                                        onInvoiceCreated(invId)
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("حفظ فاتورة الشراء وتحديث المخزون", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(14.dp)
        ) {
            // Supplier selection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .clickable { showSupplierPicker = true }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedSupplier != null) "المورد: ${selectedSupplier?.name}" else "المورد: نقدي (اضغط للتحديد)",
                        fontWeight = FontWeight.Medium
                    )
                }
                if (selectedSupplier != null) {
                    IconButton(onClick = { viewModel.selectSupplier(null) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إلغاء", modifier = Modifier.size(16.dp))
                    }
                } else {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = referenceNumber,
                    onValueChange = { referenceNumber = it },
                    label = { Text("رقم فاتورة المورد (اختياري)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Payment Mode chips
            Row(modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = paymentType == "CASH",
                    onClick = {
                        paymentType = "CASH"
                        paidAmountInput = String.format(java.util.Locale.ENGLISH, "%.2f", totalAmount)
                    },
                    label = { Text("شراء نقدي") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = paymentType == "CREDIT",
                    onClick = {
                        paymentType = "CREDIT"
                        paidAmountInput = "0.0"
                    },
                    label = { Text("شراء آجل (ذمم)") },
                    modifier = Modifier.weight(1f)
                )
            }

            if (paymentType == "CREDIT") {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = paidAmountInput,
                    onValueChange = { paidAmountInput = it },
                    label = { Text("المبلغ المدفوع مقدماً للمورد") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Add Product Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showProductPicker = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إضافة صنف (قائمة)")
                }
                FilledTonalButton(
                    onClick = { showBarcodeScanner = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مسح باركود (كاميرا)")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "أصناف الفاتورة (${purchaseItems.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (purchaseItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لم تقم بإضافة أصناف بعد", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(purchaseItems) { index, item ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = item.product.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        text = "${item.quantity} ${item.unitName} × ${Formatters.formatMoney(item.unitPrice, settings)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = Formatters.formatMoney(item.total, settings),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(onClick = { viewModel.removePurchaseItem(index) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Supplier Picker
    if (showSupplierPicker) {
        AlertDialog(
            onDismissRequest = { showSupplierPicker = false },
            title = { Text("اختر المورد") },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                    item {
                        ListItem(
                            headlineContent = { Text("مورد نقدي") },
                            modifier = Modifier.clickable {
                                viewModel.selectSupplier(null)
                                showSupplierPicker = false
                            }
                        )
                        HorizontalDivider()
                    }
                    items(suppliers.size) { i ->
                        val s = suppliers[i]
                        ListItem(
                            headlineContent = { Text(s.name) },
                            supportingContent = { Text("الرصيد: ${Formatters.formatMoney(s.currentBalance, settings)}") },
                            modifier = Modifier.clickable {
                                viewModel.selectSupplier(s)
                                showSupplierPicker = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSupplierPicker = false }) { Text("إلغاء") }
            }
        )
    }

    // Product Picker Dialog
    if (showProductPicker) {
        var prodSearch by remember { mutableStateOf("") }
        var selectedProduct by remember { mutableStateOf<Product?>(null) }
        var isMainUnit by remember { mutableStateOf(true) }
        var quantityInput by remember { mutableStateOf("1.0") }
        var costInput by remember { mutableStateOf("0.0") }

        val filtered = products.filter {
            prodSearch.isBlank() || it.name.contains(prodSearch, ignoreCase = true) || it.barcode.contains(prodSearch)
        }

        AlertDialog(
            onDismissRequest = { showProductPicker = false },
            title = { Text("إضافة صنف للشراء") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (selectedProduct == null) {
                        OutlinedTextField(
                            value = prodSearch,
                            onValueChange = { prodSearch = it },
                            placeholder = { Text("بحث عن منتج...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(modifier = Modifier.height(220.dp)) {
                            items(filtered.size) { i ->
                                val p = filtered[i]
                                ListItem(
                                    headlineContent = { Text(p.name) },
                                    supportingContent = { Text("سعر الشراء: ${Formatters.formatMoney(p.purchasePrice, settings)}") },
                                    modifier = Modifier.clickable {
                                        selectedProduct = p
                                        costInput = p.purchasePrice.toString()
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    } else {
                        val p = selectedProduct!!
                        Text(text = "المنتج: ${p.name}", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Unit toggle
                        Row(modifier = Modifier.fillMaxWidth()) {
                            FilterChip(
                                selected = isMainUnit,
                                onClick = {
                                    isMainUnit = true
                                    costInput = p.purchasePrice.toString()
                                },
                                label = { Text(p.mainUnit) },
                                modifier = Modifier.weight(1f)
                            )
                            if (p.conversionFactor > 1.0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                FilterChip(
                                    selected = !isMainUnit,
                                    onClick = {
                                        isMainUnit = false
                                        val subCost = p.purchasePrice / p.conversionFactor
                                        costInput = String.format(java.util.Locale.ENGLISH, "%.2f", subCost)
                                    },
                                    label = { Text(p.subUnit) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = quantityInput,
                            onValueChange = { quantityInput = it },
                            label = { Text("الكمية المشتراة") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = costInput,
                            onValueChange = { costInput = it },
                            label = { Text("سعر الشراء للوحدة") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                if (selectedProduct != null) {
                    Button(
                        onClick = {
                            val qty = quantityInput.toDoubleOrNull() ?: 1.0
                            val cost = costInput.toDoubleOrNull() ?: 0.0
                            viewModel.addToPurchase(selectedProduct!!, isMainUnit = isMainUnit, quantity = qty, costPrice = cost)
                            showProductPicker = false
                        }
                    ) {
                        Text("إضافة")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showProductPicker = false }) { Text("إلغاء") }
            }
        )
    }

    if (showBarcodeScanner) {
        BarcodeScannerDialog(
            onDismiss = { showBarcodeScanner = false },
            onBarcodeScanned = { barcode ->
                val matched = products.find { it.barcode.equals(barcode, ignoreCase = true) || it.sku.equals(barcode, ignoreCase = true) }
                if (matched != null) {
                    viewModel.addToPurchase(matched, isMainUnit = true, quantity = 1.0, costPrice = matched.purchasePrice)
                    viewModel.showMessage("تمت إضافة الصنف: ${matched.name}")
                } else {
                    viewModel.showMessage("لم يتم العثور على منتج بالباركود: $barcode")
                }
            }
        )
    }
}
