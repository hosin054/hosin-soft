package com.example.ui.screens.parties

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.model.Supplier
import com.example.ui.MainViewModel
import com.example.ui.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartiesScreen(
    viewModel: MainViewModel,
    onNavigateToCustomer: (Long) -> Unit,
    onNavigateToSupplier: (Long) -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Customers, 1 = Suppliers
    var searchQuery by remember { mutableStateOf("") }

    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showAddSupplierDialog by remember { mutableStateOf(false) }

    val filteredCustomers = remember(searchQuery, customers) {
        if (searchQuery.isBlank()) customers
        else customers.filter { it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery) }
    }

    val filteredSuppliers = remember(searchQuery, suppliers) {
        if (searchQuery.isBlank()) suppliers
        else suppliers.filter { it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("العملاء والموردين") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) showAddCustomerDialog = true else showAddSupplierDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(14.dp)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("العملاء (${customers.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("الموردين (${suppliers.size})") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(if (selectedTab == 0) "بحث باسم العميل أو الهاتف..." else "بحث باسم المورد أو الهاتف...") },
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

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedTab == 0) {
                // Customers
                if (filteredCustomers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("لا يوجد عملاء مسجلين", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredCustomers) { c ->
                            CustomerCard(
                                customer = c,
                                settings = settings,
                                onClick = { onNavigateToCustomer(c.id) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(70.dp)) }
                    }
                }
            } else {
                // Suppliers
                if (filteredSuppliers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("لا يوجد موردين مسجلين", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredSuppliers) { s ->
                            SupplierCard(
                                supplier = s,
                                settings = settings,
                                onClick = { onNavigateToSupplier(s.id) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(70.dp)) }
                    }
                }
            }
        }
    }

    // Add Customer Dialog
    if (showAddCustomerDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var creditLimit by remember { mutableStateOf("0.0") }
        var openingBalance by remember { mutableStateOf("0.0") }

        AlertDialog(
            onDismissRequest = { showAddCustomerDialog = false },
            title = { Text("إضافة عميل جديد") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم العميل *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("رقم الهاتف") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("العنوان") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = creditLimit, onValueChange = { creditLimit = it }, label = { Text("الحد الائتماني (0 = بدون حد)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = openingBalance, onValueChange = { openingBalance = it }, label = { Text("رصيد افتتاحي سابق (إن وجد)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ob = openingBalance.toDoubleOrNull() ?: 0.0
                        val c = Customer(
                            name = name.trim(),
                            phone = phone.trim(),
                            address = address.trim(),
                            creditLimit = creditLimit.toDoubleOrNull() ?: 0.0,
                            previousBalance = ob,
                            currentBalance = ob
                        )
                        viewModel.saveCustomer(c) {
                            showAddCustomerDialog = false
                        }
                    }
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomerDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // Add Supplier Dialog
    if (showAddSupplierDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var openingBalance by remember { mutableStateOf("0.0") }

        AlertDialog(
            onDismissRequest = { showAddSupplierDialog = false },
            title = { Text("إضافة مورد جديد") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم المورد *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("رقم الهاتف") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("العنوان") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = openingBalance, onValueChange = { openingBalance = it }, label = { Text("رصيد افتتاحي مستحق للمورد") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ob = openingBalance.toDoubleOrNull() ?: 0.0
                        val s = Supplier(
                            name = name.trim(),
                            phone = phone.trim(),
                            address = address.trim(),
                            currentBalance = ob
                        )
                        viewModel.saveSupplier(s) {
                            showAddSupplierDialog = false
                        }
                    }
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSupplierDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
fun CustomerCard(
    customer: Customer,
    settings: com.example.data.model.StoreSettings?,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = customer.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    text = "الهاتف: ${customer.phone.ifBlank { "غير مسجل" }} • المبيعات: ${Formatters.formatMoney(customer.totalSales, settings)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Formatters.formatMoney(customer.currentBalance, settings),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (customer.currentBalance > 0) MaterialTheme.colorScheme.error else Color(0xFF15803D)
                )
                Text(
                    text = if (customer.currentBalance > 0) "مستحق عليه" else "خالص",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SupplierCard(
    supplier: Supplier,
    settings: com.example.data.model.StoreSettings?,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = supplier.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    text = "الهاتف: ${supplier.phone.ifBlank { "غير مسجل" }} • المشتريات: ${Formatters.formatMoney(supplier.totalPurchases, settings)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Formatters.formatMoney(supplier.currentBalance, settings),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (supplier.currentBalance > 0) MaterialTheme.colorScheme.error else Color(0xFF15803D)
                )
                Text(
                    text = if (supplier.currentBalance > 0) "مستحق له" else "خالص",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
