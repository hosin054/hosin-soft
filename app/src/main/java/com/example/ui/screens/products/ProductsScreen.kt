package com.example.ui.screens.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Product
import com.example.ui.MainViewModel
import com.example.ui.navigation.Screen
import com.example.ui.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    viewModel: MainViewModel,
    onNavigateToForm: (Long) -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToBarcodeLabels: (Long) -> Unit = {}
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val products by viewModel.allProducts.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var onlyLowStock by remember { mutableStateOf(false) }

    var productToAdjust by remember { mutableStateOf<Product?>(null) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }

    val categories = remember(products) {
        products.map { it.category }.distinct().filter { it.isNotBlank() }
    }

    val filteredProducts = remember(searchQuery, selectedCategory, onlyLowStock, products) {
        products.filter { p ->
            val matchSearch = searchQuery.isBlank() ||
                    p.name.contains(searchQuery, ignoreCase = true) ||
                    p.barcode.contains(searchQuery, ignoreCase = true) ||
                    p.sku.contains(searchQuery, ignoreCase = true)
            val matchCat = selectedCategory == null || p.category == selectedCategory
            val matchStock = !onlyLowStock || p.currentStockSubUnits <= p.minStockSubUnits
            matchSearch && matchCat && matchStock
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إدارة المنتجات والأصناف") },
                actions = {
                    IconButton(onClick = { onNavigateToBarcodeLabels(0L) }) {
                        Icon(Icons.Default.QrCode, contentDescription = "ملصقات الباركود", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(onClick = onNavigateToInventory) {
                        Icon(Icons.Default.Inventory2, contentDescription = "سجل المخزون", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            if (currentUser.canManageInventory) {
                FloatingActionButton(
                    onClick = { onNavigateToForm(0L) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة منتج")
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
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث باسم المنتج، الباركود، أو SKU...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filters row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedCategory == null && !onlyLowStock,
                    onClick = {
                        selectedCategory = null
                        onlyLowStock = false
                    },
                    label = { Text("الكل (${products.size})") }
                )
                Spacer(modifier = Modifier.width(6.dp))
                FilterChip(
                    selected = onlyLowStock,
                    onClick = {
                        onlyLowStock = !onlyLowStock
                        selectedCategory = null
                    },
                    label = { Text("النواقص (${products.count { it.currentStockSubUnits <= it.minStockSubUnits }})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Products list
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد منتجات مسجلة مطابقة للبحث",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredProducts) { product ->
                        ProductCardItem(
                            product = product,
                            settings = settings,
                            canViewProfits = currentUser.canViewProfits,
                            canManageInventory = currentUser.canManageInventory,
                            onEdit = { onNavigateToForm(product.id) },
                            onAdjustStock = { productToAdjust = product },
                            onPrintLabel = { onNavigateToBarcodeLabels(product.id) },
                            onDelete = { productToDelete = product }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(70.dp))
                    }
                }
            }
        }
    }

    // Stock Adjustment Dialog
    if (productToAdjust != null) {
        val p = productToAdjust!!
        var newStockInput by remember { mutableStateOf(p.currentStockSubUnits.toString()) }
        var reasonInput by remember { mutableStateOf("جرد دوري") }

        AlertDialog(
            onDismissRequest = { productToAdjust = null },
            title = { Text("تسوية مخزون: ${p.name}") },
            text = {
                Column {
                    Text("المخزون الحالي: ${p.currentStockSubUnits} ${p.subUnit}")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newStockInput,
                        onValueChange = { newStockInput = it },
                        label = { Text("الكمية الفعلية الجديدة (${p.subUnit})") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reasonInput,
                        onValueChange = { reasonInput = it },
                        label = { Text("سبب التسوية") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = newStockInput.toDoubleOrNull()
                        if (qty != null && qty >= 0) {
                            viewModel.adjustStock(p, qty, reasonInput.trim())
                            productToAdjust = null
                        } else {
                            viewModel.showMessage("الكمية المدخلة غير صحيحة")
                        }
                    }
                ) {
                    Text("حفظ التسوية")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToAdjust = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Delete Product Dialog
    if (productToDelete != null) {
        val p = productToDelete!!
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("حذف المنتج") },
            text = { Text("هل أنت متأكد من حذف المنتج '${p.name}'؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(p)
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun ProductCardItem(
    product: Product,
    settings: com.example.data.model.StoreSettings?,
    canViewProfits: Boolean,
    canManageInventory: Boolean,
    onEdit: () -> Unit,
    onAdjustStock: () -> Unit,
    onPrintLabel: () -> Unit,
    onDelete: () -> Unit
) {
    val isOut = product.currentStockSubUnits <= 0
    val isLow = !isOut && product.currentStockSubUnits <= product.minStockSubUnits
    val mainStock = if (product.conversionFactor > 0) product.currentStockSubUnits / product.conversionFactor else product.currentStockSubUnits

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "التصنيف: ${product.category} • الباركود: ${product.barcode.ifBlank { "بدون" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isOut) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "نفذ المخزون!",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else if (isLow) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFD97706).copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "مخزون منخفض!",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Unit details & conversions pill banner
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الوحدة: 1 ${product.mainUnit} = ${product.conversionFactor.toInt()} ${product.subUnit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "المخزون: %.1f %s (%.0f %s)".format(mainStock, product.mainUnit, product.currentStockSubUnits, product.subUnit),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isOut || isLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pricing row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (canViewProfits) "الشراء: ${Formatters.formatMoney(product.purchasePrice, settings)}" else "الشراء: *** (محجوب)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "البيع نقداً: ${Formatters.formatMoney(product.cashSalePrice, settings)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "البيع آجل: ${Formatters.formatMoney(product.creditSalePrice, settings)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onPrintLabel) {
                    Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ملصق سعر", style = MaterialTheme.typography.labelMedium)
                }
                if (canManageInventory) {
                    TextButton(onClick = onAdjustStock) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تسوية جرد", style = MaterialTheme.typography.labelMedium)
                    }
                    TextButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تعديل", style = MaterialTheme.typography.labelMedium)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
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
}
