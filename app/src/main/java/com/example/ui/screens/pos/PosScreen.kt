package com.example.ui.screens.pos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
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
    val currencyRates by viewModel.allCurrencyRates.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var showSaveQuotationDialog by remember { mutableStateOf(false) }

    var mobileTab by remember { mutableStateOf(0) } // 0 = Products, 1 = Cart
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val categories = remember(products) {
        products.map { it.category }.filter { it.isNotBlank() }.distinct()
    }

    // Filter products (show all matching, not just 12)
    val filteredProducts = remember(searchQuery, selectedCategory, products) {
        products.filter {
            val matchesSearch = searchQuery.isBlank() ||
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.barcode.contains(searchQuery, ignoreCase = true) ||
                    it.sku.contains(searchQuery, ignoreCase = true)
            val matchesCat = selectedCategory == null || it.category == selectedCategory
            matchesSearch && matchesCat
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
                        IconButton(onClick = { showSaveQuotationDialog = true }) {
                            Icon(Icons.Default.RequestQuote, contentDescription = "حفظ كعرض أسعار", tint = MaterialTheme.colorScheme.onPrimary)
                        }
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
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isTablet = maxWidth >= 720.dp

            if (isTablet) {
                // ==================== TABLET DUAL-PANE VIEW ====================
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left Pane: Products catalog (unconstrained scroll)
                    Column(
                        modifier = Modifier
                            .weight(1.15f)
                            .fillMaxHeight()
                    ) {
                        // Search & Scanner
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("بحث عن منتج بالاسم أو الباركود...") },
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

                        if (categories.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = selectedCategory == null,
                                    onClick = { selectedCategory = null },
                                    label = { Text("الكل (${products.size})") }
                                )
                                categories.forEach { cat ->
                                    FilterChip(
                                        selected = selectedCategory == cat,
                                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                                        label = { Text(cat) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "قائمة المنتجات (${filteredProducts.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (filteredProducts.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("لا توجد منتجات مطابقة للبحث", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredProducts) { p ->
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

                    // Right Pane: Cart & Quick Checkout
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(0.85f)
                            .fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            // Customer Selector
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                    .clickable { showCustomerPicker = true }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (selectedCustomer != null) "العميل: ${selectedCustomer?.name}" else "العميل: نقدي (اضغط للتحديد)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                                if (selectedCustomer != null) {
                                    IconButton(onClick = { viewModel.selectCustomer(null) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                } else {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "سلة المبيعات (${cartItems.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (cartItems.isNotEmpty()) {
                                    TextButton(onClick = { viewModel.clearCart() }, contentPadding = PaddingValues(0.dp)) {
                                        Text("تفريغ", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            if (cartItems.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("السلة فارغة، اختر الأصناف من اليسار", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                    }
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

                            Spacer(modifier = Modifier.height(8.dp))

                            // Summary & Checkout
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("المجموع الفرعي:", fontSize = 13.sp)
                                Text(Formatters.formatMoney(subtotal, settings), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            if (taxRate > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("الضريبة (${(taxRate * 100).toInt()}%):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(Formatters.formatMoney(calculatedTax, settings), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("الإجمالي النهائي:", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    Formatters.formatMoney(totalAmount, settings),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    if (currentUser.canSell) showCheckoutDialog = true
                                    else viewModel.showMessage("حسابك الحالي لا يمتلك صلاحية إجراء المبيعات")
                                },
                                enabled = cartItems.isNotEmpty() && currentUser.canSell,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("متابعة الدفع وحفظ الفاتورة", fontWeight = FontWeight.Bold)
                            }
                            if (cartItems.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedButton(
                                    onClick = { showSaveQuotationDialog = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.RequestQuote, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("حفظ كعرض أسعار", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                // ==================== MOBILE PHONE VIEW (FULL SCREEN SCROLL TABS) ====================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    if (!currentUser.canSell) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تنبيه: حساب (${currentUser.fullName}) لا يمتلك صلاحية مبيعات.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    // Customer Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .clickable { showCustomerPicker = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedCustomer != null) "العميل: ${selectedCustomer?.name} (${Formatters.formatMoney(selectedCustomer?.currentBalance ?: 0.0, settings)})"
                                else "العميل: نقدي (اضغط للتحديد)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                        if (selectedCustomer != null) {
                            IconButton(onClick = { viewModel.selectCustomer(null) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        } else {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Mobile Primary Navigation Tabs
                    PrimaryTabRow(selectedTabIndex = mobileTab) {
                        Tab(
                            selected = mobileTab == 0,
                            onClick = { mobileTab = 0 },
                            text = { Text("المنتجات (${filteredProducts.size})", fontSize = 13.sp) },
                            icon = { Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = mobileTab == 1,
                            onClick = { mobileTab = 1 },
                            text = { Text("سلة البيع (${cartItems.size})", fontSize = 13.sp) },
                            icon = {
                                BadgedBox(badge = {
                                    if (cartItems.isNotEmpty()) {
                                        Badge { Text("${cartItems.size}") }
                                    }
                                }) {
                                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (mobileTab == 0) {
                        // Search & Barcode
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

                        if (categories.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = selectedCategory == null,
                                    onClick = { selectedCategory = null },
                                    label = { Text("الكل", fontSize = 12.sp) }
                                )
                                categories.forEach { cat ->
                                    FilterChip(
                                        selected = selectedCategory == cat,
                                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                                        label = { Text(cat, fontSize = 12.sp) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (filteredProducts.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("لا توجد منتجات مطابقة للبحث", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            // Full-height scrollable LazyColumn for all products on mobile!
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(filteredProducts) { p ->
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

                        // Sticky quick checkout summary on product tab if cart has items
                        if (cartItems.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${cartItems.size} أصناف في السلة",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = Formatters.formatMoney(totalAmount, settings),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { mobileTab = 1 },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("عرض السلة", fontSize = 12.sp)
                                        }
                                        Button(
                                            onClick = {
                                                if (currentUser.canSell) showCheckoutDialog = true
                                                else viewModel.showMessage("حسابك لا يمتلك صلاحية مبيعات")
                                            },
                                            enabled = currentUser.canSell,
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("دفع فوري", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Mobile Tab 1: Cart screen (full height scrollable list + summary)
                        if (cartItems.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(56.dp))
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("سلة المبيعات فارغة", fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("اضغط على الزر بالأسفل لاختيار الأصناف", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(onClick = { mobileTab = 0 }) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("تصفح وإضافة الأصناف")
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "أصناف السلة (${cartItems.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                TextButton(onClick = { viewModel.clearCart() }) {
                                    Text("تفريغ السلة", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                }
                            }

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

                            Spacer(modifier = Modifier.height(8.dp))

                            // Summary card & checkout
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("المجموع:", fontSize = 13.sp)
                                        Text(Formatters.formatMoney(subtotal, settings), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    if (taxRate > 0) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("الضريبة (${(taxRate * 100).toInt()}%):", fontSize = 12.sp)
                                            Text(Formatters.formatMoney(calculatedTax, settings), fontSize = 12.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("الإجمالي النهائي:", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(
                                            Formatters.formatMoney(totalAmount, settings),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            if (currentUser.canSell) showCheckoutDialog = true
                                            else viewModel.showMessage("حسابك الحالي لا يمتلك صلاحية مبيعات")
                                        },
                                        enabled = currentUser.canSell,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(46.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("متابعة الدفع وحفظ الفاتورة", fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedButton(
                                        onClick = { showSaveQuotationDialog = true },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(38.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.RequestQuote, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("حفظ كعرض أسعار", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
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
        var custSearch by remember { mutableStateOf("") }
        val filteredCusts = remember(custSearch, customers) {
            if (custSearch.isBlank()) customers
            else customers.filter {
                it.name.contains(custSearch, ignoreCase = true) ||
                        it.phone.contains(custSearch)
            }
        }

        AlertDialog(
            onDismissRequest = { showCustomerPicker = false },
            title = { Text("اختر العميل") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = custSearch,
                        onValueChange = { custSearch = it },
                        placeholder = { Text("بحث باسم العميل أو الهاتف...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)) {
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
                        items(filteredCusts.size) { i ->
                            val c = filteredCusts[i]
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
                            HorizontalDivider()
                        }
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
        var paymentMethod by remember { mutableStateOf(if (paymentType == "CREDIT") "آجل" else "كاش") }

        // Currency selection
        val baseCurrency = remember(currencyRates) {
            currencyRates.find { it.isBase } ?: currencyRates.firstOrNull()
        }
        var selectedCurrencyId by remember {
            mutableStateOf(baseCurrency?.id ?: currencyRates.firstOrNull()?.id)
        }
        val chosenCurrency = remember(currencyRates, selectedCurrencyId) {
            currencyRates.find { it.id == selectedCurrencyId } ?: baseCurrency ?: com.example.data.model.CurrencyRate(
                code = "SAR", name = "ريال سعودي", symbol = settings?.currencySymbol ?: "ر.س", rateToBase = 1.0, isBase = true
            )
        }

        var customExchangeRateInput by remember(chosenCurrency) {
            mutableStateOf(String.format(java.util.Locale.ENGLISH, "%.4f", chosenCurrency.rateToBase))
        }
        val effectiveRate = customExchangeRateInput.toDoubleOrNull() ?: chosenCurrency.rateToBase

        var discountInput by remember { mutableStateOf("0.0") }
        var notesInput by remember { mutableStateOf("") }

        val finalDiscount = discountInput.toDoubleOrNull() ?: 0.0
        val finalTotal = maxOf(0.0, subtotal - finalDiscount + calculatedTax)

        // Required amount in the chosen currency
        val requiredInChosenCurrency = if (effectiveRate > 0) finalTotal / effectiveRate else finalTotal

        var paidInCurrencyInput by remember(chosenCurrency, paymentType, finalTotal) {
            mutableStateOf(
                if (paymentType == "CASH") String.format(java.util.Locale.ENGLISH, "%.2f", requiredInChosenCurrency)
                else "0.0"
            )
        }

        val paidInChosenCurrency = paidInCurrencyInput.toDoubleOrNull() ?: 0.0
        // Equivalent in base currency
        val paidInBase = paidInChosenCurrency * effectiveRate
        val finalPaidInBase = if (paymentType == "CASH") minOf(finalTotal, paidInBase) else paidInBase
        val remainingInBase = maxOf(0.0, finalTotal - finalPaidInBase)
        val remainingInChosen = if (effectiveRate > 0) remainingInBase / effectiveRate else 0.0

        val changeDueInChosen = maxOf(0.0, paidInChosenCurrency - requiredInChosenCurrency)
        val changeDueInBase = changeDueInChosen * effectiveRate
        val isUnderpaid = paymentType == "CASH" && paidInCurrencyInput.isNotBlank() && paidInChosenCurrency < (requiredInChosenCurrency - 0.001)

        AlertDialog(
            onDismissRequest = { showCheckoutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Payment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إتمام ودفع فاتورة المبيعات", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Payment Type (Cash vs Credit)
                    item {
                        Text(text = "نوع الفاتورة:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            FilterChip(
                                selected = paymentType == "CASH",
                                onClick = {
                                    paymentType = "CASH"
                                    if (paymentMethod == "آجل") paymentMethod = "كاش"
                                    paidInCurrencyInput = String.format(java.util.Locale.ENGLISH, "%.2f", requiredInChosenCurrency)
                                },
                                label = { Text("دفع فوري (نقدي/إلكتروني)") },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            FilterChip(
                                selected = paymentType == "CREDIT",
                                onClick = {
                                    paymentType = "CREDIT"
                                    paymentMethod = "آجل"
                                    paidInCurrencyInput = "0.0"
                                },
                                label = { Text("بيع آجل (ذمم)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Customer reminder for credit
                    if (paymentType == "CREDIT" && selectedCustomer == null) {
                        item {
                            Text(
                                text = "⚠️ تنبيه: يجب تحديد عميل لإجراء البيع الآجل!",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 2. Payment Method (Cash, Electronic / Card, Bank Transfer)
                    if (paymentType == "CASH") {
                        item {
                            Text(text = "طريقة الدفع:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                FilterChip(
                                    selected = paymentMethod == "كاش",
                                    onClick = { paymentMethod = "كاش" },
                                    leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    label = { Text("كاش / نقداً", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = paymentMethod == "إلكتروني / شبكة",
                                    onClick = { paymentMethod = "إلكتروني / شبكة" },
                                    leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    label = { Text("شبكة / مدى", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = paymentMethod == "تحويل بنكي",
                                    onClick = { paymentMethod = "تحويل بنكي" },
                                    leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    label = { Text("تحويل بنكي", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // 3. Payment Currency Selection
                    item {
                        Text(text = "العملة المستلم بها المبلغ:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        var currMenuExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { currMenuExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${chosenCurrency.name} (${chosenCurrency.symbol}) - كود: ${chosenCurrency.code}",
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }
                            DropdownMenu(
                                expanded = currMenuExpanded,
                                onDismissRequest = { currMenuExpanded = false }
                            ) {
                                currencyRates.forEach { rate ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("${rate.name} (${rate.symbol})")
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    if (rate.isBase) "أساسية" else "1 ${rate.code} = ${rate.rateToBase} ${baseCurrency?.symbol ?: ""}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedCurrencyId = rate.id
                                            customExchangeRateInput = String.format(java.util.Locale.ENGLISH, "%.4f", rate.rateToBase)
                                            currMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (!chosenCurrency.isBase) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = customExchangeRateInput,
                                onValueChange = { customExchangeRateInput = it },
                                label = { Text("سعر الصرف (1 ${chosenCurrency.code} = كم ${baseCurrency?.symbol ?: ""})") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // 4. Amount Tendered / Paid in that Currency
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(10.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "المبلغ المطلوب بهذه العملة:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${String.format(java.util.Locale.ENGLISH, "%,.2f", requiredInChosenCurrency)} ${chosenCurrency.symbol}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = paidInCurrencyInput,
                            onValueChange = { paidInCurrencyInput = it },
                            label = {
                                Text(if (paymentType == "CREDIT") "المبلغ المدفوع مقدماً (${chosenCurrency.symbol})" else "المبلغ المستلم من الزبون (${chosenCurrency.symbol})")
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Quick Tender Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            SuggestionChip(
                                onClick = {
                                    paidInCurrencyInput = String.format(java.util.Locale.ENGLISH, "%.2f", requiredInChosenCurrency)
                                },
                                label = { Text("المبلغ التام", fontSize = 10.sp) },
                                modifier = Modifier.height(28.dp)
                            )
                            SuggestionChip(
                                onClick = {
                                    val curr = paidInCurrencyInput.toDoubleOrNull() ?: 0.0
                                    paidInCurrencyInput = String.format(java.util.Locale.ENGLISH, "%.0f", curr + 10)
                                },
                                label = { Text("+10", fontSize = 10.sp) },
                                modifier = Modifier.height(28.dp)
                            )
                            SuggestionChip(
                                onClick = {
                                    val curr = paidInCurrencyInput.toDoubleOrNull() ?: 0.0
                                    paidInCurrencyInput = String.format(java.util.Locale.ENGLISH, "%.0f", curr + 50)
                                },
                                label = { Text("+50", fontSize = 10.sp) },
                                modifier = Modifier.height(28.dp)
                            )
                            SuggestionChip(
                                onClick = {
                                    val curr = paidInCurrencyInput.toDoubleOrNull() ?: 0.0
                                    paidInCurrencyInput = String.format(java.util.Locale.ENGLISH, "%.0f", curr + 100)
                                },
                                label = { Text("+100", fontSize = 10.sp) },
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }

                    // Change Due or Underpaid Badge
                    if (paymentType == "CASH") {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isUnderpaid) MaterialTheme.colorScheme.errorContainer else Color(0xFFDCFCE7)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isUnderpaid) "المستلم أقل من المطلوب!" else "الباقي للزبون:",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isUnderpaid) MaterialTheme.colorScheme.error else Color(0xFF166534)
                                        )
                                        Text(
                                            text = if (isUnderpaid) {
                                                "${String.format(java.util.Locale.ENGLISH, "%,.2f", requiredInChosenCurrency - paidInChosenCurrency)} ${chosenCurrency.symbol}"
                                            } else {
                                                "${String.format(java.util.Locale.ENGLISH, "%,.2f", changeDueInChosen)} ${chosenCurrency.symbol}"
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (isUnderpaid) MaterialTheme.colorScheme.error else Color(0xFF166534)
                                        )
                                    }
                                    if (!chosenCurrency.isBase && !isUnderpaid && changeDueInChosen > 0) {
                                        Text(
                                            text = "يعادل بالعملة الأساسية: ${Formatters.formatMoney(changeDueInBase, settings)}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF166534).copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Additional Discount & Notes
                    item {
                        if (currentUser.canGiveDiscount) {
                            OutlinedTextField(
                                value = discountInput,
                                onValueChange = { discountInput = it },
                                label = { Text("الخصم الإضافي (${settings?.currencySymbol ?: "ر.س"})") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        OutlinedTextField(
                            value = notesInput,
                            onValueChange = { notesInput = it },
                            label = { Text("ملاحظات الفاتورة (اختياري)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Financial Breakdown Summary Card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                    Text(text = "الإجمالي بالعملة الأساسية:", fontWeight = FontWeight.Bold)
                                    Text(text = Formatters.formatMoney(finalTotal, settings), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "طريقة الدفع والعملة:", fontSize = 12.sp)
                                    Text(text = "$paymentMethod (${chosenCurrency.name})", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                                if (paymentType == "CREDIT") {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = "المتبقي الآجل للعميل:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                        Text(text = Formatters.formatMoney(remainingInBase, settings), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                    }
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
                            paymentMethod = paymentMethod,
                            paidCurrency = chosenCurrency.name,
                            paidCurrencyAmount = paidInChosenCurrency,
                            exchangeRate = effectiveRate,
                            paidAmount = finalPaidInBase,
                            notes = notesInput.trim(),
                            onSuccess = { invoiceId ->
                                showCheckoutDialog = false
                                onNavigateToInvoice(invoiceId)
                            }
                        )
                    },
                    enabled = !(paymentType == "CREDIT" && selectedCustomer == null)
                ) {
                    Text("تأكيد وحفظ الفاتورة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCheckoutDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Save Quotation Dialog
    if (showSaveQuotationDialog) {
        var quotationNotes by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveQuotationDialog = false },
            title = { Text("حفظ كعرض أسعار") },
            text = {
                Column {
                    Text(
                        text = "سيتم حفظ أصناف السلة الحالية كعرض أسعار رسمي للعميل (${selectedCustomer?.name ?: "عميل عام"}). يمكنك طباعته أو مشاركته أو تحويله لفاتورة بيع لاحقاً.",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = quotationNotes,
                        onValueChange = { quotationNotes = it },
                        label = { Text("ملاحظات وشروط العرض") },
                        placeholder = { Text("مثال: العرض سارٍ لمدة 15 يوماً من تاريخه") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveQuotation(quotationNotes.trim()) {
                            showSaveQuotationDialog = false
                        }
                    }
                ) {
                    Text("حفظ عرض الأسعار")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveQuotationDialog = false }) {
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "السعر: ${Formatters.formatMoney(item.unitPrice, settings)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    SuggestionChip(
                        onClick = onToggleUnit,
                        label = { Text(text = item.unitName, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(26.dp)
                    )
                }
            }

            // Quantity controls
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledIconButton(
                    onClick = { onQuantityChange(item.quantity - 1.0) },
                    modifier = Modifier.size(30.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "نقص",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "${item.quantity.toInt()}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                FilledIconButton(
                    onClick = { onQuantityChange(item.quantity + 1.0) },
                    modifier = Modifier.size(30.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "زيادة",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Formatters.formatMoney(item.total, settings),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "حذف",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
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
    val subUnits = product.currentStockSubUnits
    val mainStock = if (product.conversionFactor > 0) subUnits / product.conversionFactor else subUnits
    val isOutOfStock = subUnits <= 0
    val isLowStock = !isOutOfStock && subUnits <= product.minStockSubUnits

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isOutOfStock) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "نفذ",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    } else if (isLowStock) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFD97706).copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "منخفض",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD97706),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "المخزون: %.1f %s (%.0f %s)".format(mainStock, product.mainUnit, subUnits, product.subUnit),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOutOfStock || isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Button to add main unit
            Button(
                onClick = { onAddToCart(true) },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(
                    text = "${product.mainUnit} (${Formatters.formatMoney(product.cashSalePrice, settings)})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Button to add sub unit if conversion > 1
            if (product.conversionFactor > 1.0) {
                Spacer(modifier = Modifier.width(6.dp))
                val subPrice = product.cashSalePrice / product.conversionFactor
                OutlinedButton(
                    onClick = { onAddToCart(false) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = "${product.subUnit} (${Formatters.formatMoney(subPrice, settings)})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
