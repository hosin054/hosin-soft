package com.example.ui.screens.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Customer
import com.example.data.model.Product
import com.example.ui.MainViewModel
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.navigation.Screen
import com.example.ui.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: MainViewModel,
    onNavigateToInvoice: (Long) -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val products by viewModel.allProducts.collectAsStateWithLifecycle()
    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val selectedCustomer by viewModel.selectedCustomer.collectAsStateWithLifecycle()
    val discount by viewModel.posDiscount.collectAsStateWithLifecycle()
    val tax by viewModel.posTax.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var showCheckoutDialog by remember { mutableStateOf(false) }

    // Filter products
    val filteredProducts = remember(searchQuery, products) {
        if (searchQuery.isBlank()) {
            products.take(12)
        } else {
            products.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.barcode.contains(searchQuery, ignoreCase = true) ||
                        it.sku.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Calculations
    val subtotal = cartItems.sumOf { it.total }
    val taxRate = if (settings?.taxEnabled == true) (settings?.taxRate ?: 0.0) / 100.0 else 0.0
    val calculatedTax = if (settings?.taxEnabled == true) maxOf(0.0, subtotal - discount) * taxRate else 0.0
    val totalAmount = maxOf(0.0, subtotal - discount + calculatedTax)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("نقطة البيع (POS) والمبيعات") },
                actions = {
                    IconButton(onClick = { showBarcodeScanner = true }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "مسح باركود", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    if (cartItems.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearCart() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "تفريغ السلة", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            if (cartItems.isNotEmpty()) {
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
                            Text(text = "عدد الأصناف: ${cartItems.size}")
                            Text(
                                text = "الإجمالي: ${Formatters.formatMoney(totalAmount, settings)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { showCheckoutDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("متابعة الدفع وحفظ الفاتورة", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                .padding(12.dp)
        ) {
            // Search & Barcode row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث باسم المنتج أو الباركود...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalIconButton(onClick = { showBarcodeScanner = true }) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "مسح باركود")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Customer Selector Tag
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .clickable { showCustomerPicker = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedCustomer != null) "العميل: ${selectedCustomer?.name} (الرصيد: ${Formatters.formatMoney(selectedCustomer?.currentBalance ?: 0.0, settings)})"
                        else "العميل: نقدي (اضغط للتحديد)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (selectedCustomer != null) {
                    IconButton(
                        onClick = { viewModel.selectCustomer(null) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إلغاء التحديد", modifier = Modifier.size(16.dp))
                    }
                } else {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Cart Items Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سلة المبيعات (${cartItems.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                if (cartItems.isNotEmpty()) {
                    Text(
                        text = "المجموع: ${Formatters.formatMoney(subtotal, settings)}",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Cart Items List
            if (cartItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "السلة فارغة، اختر من المنتجات بالأسفل أو امسح الباركود",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(cartItems) { index, item ->
                        PosCartItemRow(
                            item = item,
                            settings = settings,
                            onQuantityChange = { viewModel.updateCartItemQuantity(index, it) },
                            onToggleUnit = { viewModel.toggleCartItemUnit(index) },
                            onRemove = { viewModel.removeFromCart(index) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Available Products Grid/List
            Text(
                text = "المنتجات المتاحة للبيع",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (filteredProducts.isEmpty()) {
                    item {
                        Text(
                            text = "لا توجد منتجات مطابقة",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                } else {
                    items(filteredProducts.size) { i ->
                        val p = filteredProducts[i]
                        PosProductItemCard(
                            product = p,
                            settings = settings,
                            onAddToCart = { isMain ->
                                viewModel.addToCart(p, isMainUnit = isMain, quantity = 1.0)
                            }
                        )
                    }
                }
            }
        }
    }

    // Barcode Scanner Dialog
    if (showBarcodeScanner) {
        BarcodeScannerDialog(
            onDismiss = { showBarcodeScanner = false },
            onBarcodeScanned = { barcode ->
                showBarcodeScanner = false
                val matched = products.find { it.barcode.equals(barcode, ignoreCase = true) || it.sku.equals(barcode, ignoreCase = true) }
                if (matched != null) {
                    viewModel.addToCart(matched, isMainUnit = true, quantity = 1.0)
                    viewModel.showMessage("تمت إضافة ${matched.name}")
                } else {
                    viewModel.showMessage("لم يتم العثور على منتج بالباركود: $barcode")
                }
            }
        )
    }

    // Customer Picker Dialog
    if (showCustomerPicker) {
        AlertDialog(
            onDismissRequest = { showCustomerPicker = false },
            title = { Text("اختر العميل") },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                    item {
                        ListItem(
                            headlineContent = { Text("عميل نقدي (بدون حساب)") },
                            modifier = Modifier.clickable {
                                viewModel.selectCustomer(null)
                                showCustomerPicker = false
                            }
                        )
                        HorizontalDivider()
                    }
                    items(customers.size) { i ->
                        val c = customers[i]
                        ListItem(
                            headlineContent = { Text(c.name) },
                            supportingContent = {
                                Text("الرصيد: ${Formatters.formatMoney(c.currentBalance, settings)} | الهاتف: ${c.phone}")
                            },
                            modifier = Modifier.clickable {
                                viewModel.selectCustomer(c)
                                showCustomerPicker = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCustomerPicker = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Checkout Dialog
    if (showCheckoutDialog) {
        var paymentType by remember { mutableStateOf(if (selectedCustomer != null) "CREDIT" else "CASH") }
        var paidAmountInput by remember {
            mutableStateOf(if (paymentType == "CASH") String.format(java.util.Locale.ENGLISH, "%.2f", totalAmount) else "0.0")
        }
        var discountInput by remember { mutableStateOf("0.0") }
        var notesInput by remember { mutableStateOf("") }

        val finalDiscount = discountInput.toDoubleOrNull() ?: 0.0
        val finalTotal = maxOf(0.0, subtotal - finalDiscount + calculatedTax)
        val finalPaid = if (paymentType == "CASH") finalTotal else (paidAmountInput.toDoubleOrNull() ?: 0.0)
        val remaining = maxOf(0.0, finalTotal - finalPaid)

        AlertDialog(
            onDismissRequest = { showCheckoutDialog = false },
            title = { Text("إتمام فاتورة المبيعات") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Payment type toggle
                    Row(modifier = Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = paymentType == "CASH",
                            onClick = {
                                paymentType = "CASH"
                                paidAmountInput = String.format(java.util.Locale.ENGLISH, "%.2f", finalTotal)
                            },
                            label = { Text("بيع نقدي") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = paymentType == "CREDIT",
                            onClick = {
                                paymentType = "CREDIT"
                                paidAmountInput = "0.0"
                            },
                            label = { Text("بيع آجل (ذمم)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "العميل: ${selectedCustomer?.name ?: "عميل نقدي"}", fontWeight = FontWeight.Medium)

                    if (paymentType == "CREDIT" && selectedCustomer == null) {
                        Text(
                            text = "تنبيه: يجب تحديد عميل لإجراء البيع الآجل!",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = discountInput,
                        onValueChange = { discountInput = it },
                        label = { Text("الخصم الإضافي") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (paymentType == "CREDIT") {
                        OutlinedTextField(
                            value = paidAmountInput,
                            onValueChange = { paidAmountInput = it },
                            label = { Text("المبلغ المدفوع مقدماً") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("ملاحظات (اختياري)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "المجموع الفرعي:", fontSize = 12.sp)
                                Text(text = Formatters.formatMoney(subtotal, settings), fontSize = 12.sp)
                            }
                            if (calculatedTax > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "الضريبة (${settings?.taxRate}%):", fontSize = 12.sp)
                                    Text(text = Formatters.formatMoney(calculatedTax, settings), fontSize = 12.sp)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "الإجمالي النهائي:", fontWeight = FontWeight.Bold)
                                Text(text = Formatters.formatMoney(finalTotal, settings), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            if (paymentType == "CREDIT") {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "المتبقي الآجل:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                    Text(text = Formatters.formatMoney(remaining, settings), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setPosDiscount(finalDiscount)
                        viewModel.setPosTax(calculatedTax)
                        viewModel.completeSale(
                            paymentType = paymentType,
                            paidAmount = finalPaid,
                            notes = notesInput.trim(),
                            onSuccess = { invoiceId ->
                                showCheckoutDialog = false
                                onNavigateToInvoice(invoiceId)
                            }
                        )
                    }
                ) {
                    Text("تأكيد وحفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCheckoutDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun PosCartItemRow(
    item: com.example.data.repository.AccountingRepository.CartItem,
    settings: com.example.data.model.StoreSettings?,
    onQuantityChange: (Double) -> Unit,
    onToggleUnit: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "السعر: ${Formatters.formatMoney(item.unitPrice, settings)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    SuggestionChip(
                        onClick = onToggleUnit,
                        label = { Text(text = item.unitName, fontSize = 10.sp) },
                        modifier = Modifier.height(24.dp)
                    )
                }
            }

            // Quantity controls
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledIconButton(
                    onClick = { onQuantityChange(item.quantity - 1.0) },
                    modifier = Modifier.size(28.dp),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "نقص", modifier = Modifier.size(16.dp))
                }
                Text(
                    text = "${item.quantity.toInt()}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                FilledIconButton(
                    onClick = { onQuantityChange(item.quantity + 1.0) },
                    modifier = Modifier.size(28.dp),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "زيادة", modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = Formatters.formatMoney(item.total, settings),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )

            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun PosProductItemCard(
    product: Product,
    settings: com.example.data.model.StoreSettings?,
    onAddToCart: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = product.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                val subUnits = product.currentStockSubUnits
                val mainStock = if (product.conversionFactor > 0) subUnits / product.conversionFactor else subUnits
                Text(
                    text = "المخزون: %.1f %s (%.0f %s)".format(mainStock, product.mainUnit, subUnits, product.subUnit),
                    fontSize = 11.sp,
                    color = if (subUnits <= product.minStockSubUnits) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Button to add main unit
            Button(
                onClick = { onAddToCart(true) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = "${product.mainUnit} (${Formatters.formatMoney(product.cashSalePrice, settings)})",
                    fontSize = 10.sp
                )
            }

            // Button to add sub unit if conversion > 1
            if (product.conversionFactor > 1.0) {
                Spacer(modifier = Modifier.width(6.dp))
                val subPrice = product.cashSalePrice / product.conversionFactor
                OutlinedButton(
                    onClick = { onAddToCart(false) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "${product.subUnit} (${Formatters.formatMoney(subPrice, settings)})",
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
