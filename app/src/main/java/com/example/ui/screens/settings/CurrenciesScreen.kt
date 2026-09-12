package com.example.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.data.model.CurrencyRate
import com.example.ui.MainViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrenciesScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val currencies by viewModel.allCurrencyRates.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var currencyToEdit by remember { mutableStateOf<CurrencyRate?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<CurrencyRate?>(null) }

    // Currency Converter State
    var converterAmount by remember { mutableStateOf("100") }
    var sourceCurrencyId by remember { mutableStateOf<Long?>(null) }
    var targetCurrencyId by remember { mutableStateOf<Long?>(null) }

    val baseCurrency = remember(currencies) {
        currencies.find { it.isBase } ?: currencies.firstOrNull()
    }

    LaunchedEffect(currencies) {
        if (sourceCurrencyId == null && currencies.isNotEmpty()) {
            sourceCurrencyId = currencies.first().id
        }
        if (targetCurrencyId == null && currencies.size > 1) {
            targetCurrencyId = currencies[1].id
        } else if (targetCurrencyId == null && currencies.isNotEmpty()) {
            targetCurrencyId = currencies.first().id
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("أسعار صرف العملات والمحوّل") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    if (currentUser.canManageSettings) {
                        IconButton(onClick = { viewModel.resetCurrenciesToDefault() }) {
                            Icon(
                                Icons.Default.Restore,
                                contentDescription = "استعادة الافتراضي",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            if (currentUser.canManageSettings) {
                FloatingActionButton(
                    onClick = {
                        currencyToEdit = null
                        showAddDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة عملة جديدة")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Info Card
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "إدارة أسعار الصرف المتعددة",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "العملة الأساسية المعتمدة لتقارير المتجر هي: ${baseCurrency?.name ?: settings?.currencySymbol ?: "ريال سعودي"}. يتم تحويل أي مدفوعات بالعملات الأخرى أو عرض الأرباح بناءً على أسعار الصرف المدخلة هنا.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Real-time Currency Converter Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "حاسبة التحويل الفوري بين العملات",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = converterAmount,
                            onValueChange = { converterAmount = it },
                            label = { Text("المبلغ المراد تحويله") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val sourceCurr = currencies.find { it.id == sourceCurrencyId }
                        val targetCurr = currencies.find { it.id == targetCurrencyId }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Source currency selector
                            var sourceExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { sourceExpanded = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("من: ${sourceCurr?.code ?: "اختر"}", maxLines = 1, fontSize = 12.sp)
                                }
                                DropdownMenu(
                                    expanded = sourceExpanded,
                                    onDismissRequest = { sourceExpanded = false }
                                ) {
                                    currencies.forEach { c ->
                                        DropdownMenuItem(
                                            text = { Text("${c.name} (${c.code})") },
                                            onClick = {
                                                sourceCurrencyId = c.id
                                                sourceExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Swap button
                            IconButton(
                                onClick = {
                                    val temp = sourceCurrencyId
                                    sourceCurrencyId = targetCurrencyId
                                    targetCurrencyId = temp
                                }
                            ) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = "تبديل")
                            }

                            // Target currency selector
                            var targetExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { targetExpanded = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("إلى: ${targetCurr?.code ?: "اختر"}", maxLines = 1, fontSize = 12.sp)
                                }
                                DropdownMenu(
                                    expanded = targetExpanded,
                                    onDismissRequest = { targetExpanded = false }
                                ) {
                                    currencies.forEach { c ->
                                        DropdownMenuItem(
                                            text = { Text("${c.name} (${c.code})") },
                                            onClick = {
                                                targetCurrencyId = c.id
                                                targetExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Calculated Result Display
                        val amount = converterAmount.toDoubleOrNull() ?: 0.0
                        val sourceRate = sourceCurr?.rateToBase ?: 1.0
                        val targetRate = targetCurr?.rateToBase ?: 1.0

                        // Conversion: source -> base -> target
                        val amountInBase = amount * sourceRate
                        val resultInTarget = if (targetRate > 0) amountInBase / targetRate else 0.0

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$amount ${sourceCurr?.symbol ?: ""} يعادل:",
                                    fontSize = 12.sp,
                                    color = Color(0xFF1E40AF)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${String.format(Locale.ENGLISH, "%,.2f", resultInTarget)} ${targetCurr?.symbol ?: ""}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E3A8A)
                                )
                                if (sourceCurr != null && targetCurr != null && sourceCurr != targetCurr) {
                                    val unitRate = if (targetRate > 0) sourceRate / targetRate else 0.0
                                    Text(
                                        text = "(1 ${sourceCurr.code} = ${String.format(Locale.ENGLISH, "%.4f", unitRate)} ${targetCurr.code})",
                                        fontSize = 11.sp,
                                        color = Color(0xFF3B82F6)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Currency Rates List Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "العملات المعرفة وأسعار صرفها",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "${currencies.size} عملة",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Currency List Items
            items(currencies, key = { it.id }) { currency ->
                CurrencyCard(
                    currency = currency,
                    baseCurrency = baseCurrency,
                    canManage = currentUser.canManageSettings,
                    onEdit = {
                        currencyToEdit = currency
                        showAddDialog = true
                    },
                    onDelete = { showDeleteConfirm = currency },
                    onSetBase = {
                        viewModel.saveCurrencyRate(currency.copy(isBase = true, rateToBase = 1.0))
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(70.dp))
            }
        }
    }

    // Add / Edit Currency Dialog
    if (showAddDialog) {
        var nameInput by remember { mutableStateOf(currencyToEdit?.name ?: "") }
        var codeInput by remember { mutableStateOf(currencyToEdit?.code ?: "") }
        var symbolInput by remember { mutableStateOf(currencyToEdit?.symbol ?: "") }
        var rateInput by remember {
            mutableStateOf(
                if (currencyToEdit != null) currencyToEdit!!.rateToBase.toString() else "1.0"
            )
        }
        var isBaseInput by remember { mutableStateOf(currencyToEdit?.isBase ?: false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(if (currencyToEdit == null) "إضافة عملة جديدة" else "تعديل بيانات العملة")
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("اسم العملة (مثال: ريال يمني)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = codeInput,
                            onValueChange = { codeInput = it.uppercase() },
                            label = { Text("رمز الكود (YER)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = symbolInput,
                            onValueChange = { symbolInput = it },
                            label = { Text("الرمز (ر.ي)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = rateInput,
                        onValueChange = { rateInput = it },
                        label = {
                            Text(
                                if (isBaseInput) "سعر الصرف (1.0 دائماً للعملة الأساسية)"
                                else "سعر الصرف: 1 وحدة من هذه العملة = كم بالأساسية؟"
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        enabled = !isBaseInput,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "مثال: إذا كانت الأساسية ريال سعودي، وتريد إضافة الدولار (1 $ = 3.75 ر.س) اكتب 3.75. وإذا كانت الأساسية ريال يمني وكان (1 ريال سعودي = 140 ريال يمني) فإن سعر صرف السعودي يكون 140.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = isBaseInput,
                            onCheckedChange = { checked ->
                                isBaseInput = checked
                                if (checked) rateInput = "1.0"
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "تعيين كـ (العملة الأساسية للمتجر)", fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedRate = if (isBaseInput) 1.0 else (rateInput.toDoubleOrNull() ?: 1.0)
                        if (nameInput.isBlank() || codeInput.isBlank()) return@Button

                        val toSave = CurrencyRate(
                            id = currencyToEdit?.id ?: 0L,
                            name = nameInput.trim(),
                            code = codeInput.trim().uppercase(),
                            symbol = if (symbolInput.isNotBlank()) symbolInput.trim() else codeInput.trim(),
                            rateToBase = parsedRate,
                            isBase = isBaseInput,
                            updatedAt = System.currentTimeMillis()
                        )
                        viewModel.saveCurrencyRate(toSave) {
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm != null) {
        val target = showDeleteConfirm!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("حذف العملة") },
            text = {
                Text("هل أنت متأكد من حذف العملة (${target.name} - ${target.code})؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCurrencyRate(target)
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("نعم، حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun CurrencyCard(
    currency: CurrencyRate,
    baseCurrency: CurrencyRate?,
    canManage: Boolean = true,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetBase: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (currency.isBase) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (currency.isBase) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = currency.symbol.take(4),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (currency.isBase) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currency.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${currency.code})",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (currency.isBase) {
                            Text(
                                text = "⭐ العملة الأساسية للمتجر (1.0)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                text = "1 ${currency.code} = ${String.format(Locale.ENGLISH, "%.4f", currency.rateToBase)} ${baseCurrency?.symbol ?: ""}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (canManage) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary)
                        }
                        if (!currency.isBase) {
                            IconButton(onClick = onDelete) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            if (!currency.isBase) {
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "سعر التحويل العكسي: 1 ${baseCurrency?.symbol ?: ""} = ${
                            if (currency.rateToBase > 0) String.format(Locale.ENGLISH, "%.4f", 1.0 / currency.rateToBase) else "0"
                        } ${currency.symbol}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (canManage) {
                        TextButton(onClick = onSetBase) {
                            Text("جعلها أساسية", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
