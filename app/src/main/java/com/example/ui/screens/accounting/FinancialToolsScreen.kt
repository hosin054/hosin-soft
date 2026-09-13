package com.example.ui.screens.accounting

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.ui.MainViewModel
import com.example.ui.components.CashDenominationsCounter
import com.example.ui.components.GlassCard
import com.example.ui.components.PulsingStatusBadge
import com.example.ui.components.VisualAccountCard
import com.example.ui.util.BackupHelper
import com.example.ui.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialToolsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val shifts by viewModel.allShifts.collectAsState()
    val cheques by viewModel.allCheques.collectAsState()
    val assets by viewModel.allFixedAssets.collectAsState()
    val costCenters by viewModel.allCostCenters.collectAsState()
    val transfers by viewModel.allAccountTransfers.collectAsState()
    val cashTransactions by viewModel.allCashTransactions.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "إقفال الوردية" to Icons.Default.AccessTime,
        "الشيكات وأوراق القبض" to Icons.Default.ReceiptLong,
        "الأصول الثابتة والإهلاك" to Icons.Default.Domain,
        "مراكز التكلفة" to Icons.Default.AccountTree,
        "التحويل المالي" to Icons.Default.SwapHoriz
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("الأدوات المحاسبية والعمليات المتقدمة", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Scrollable Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, (title, icon) ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontSize = 12.sp, fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal) },
                        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                when (selectedTabIndex) {
                    0 -> ShiftManagementTab(viewModel, shifts, cashTransactions, settings)
                    1 -> ChequesManagementTab(viewModel, cheques, settings)
                    2 -> FixedAssetsTab(viewModel, assets, settings)
                    3 -> CostCentersTab(viewModel, costCenters)
                    4 -> TransfersTab(viewModel, transfers, cashTransactions, settings)
                }
            }
        }
    }
}

// ==========================================
// 1. Shift Management Tab (الوردية وجرد النقدية)
// ==========================================
@Composable
private fun ShiftManagementTab(
    viewModel: MainViewModel,
    shifts: List<ShiftRecord>,
    cashTransactions: List<CashTransaction>,
    settings: StoreSettings?
) {
    val openShift = shifts.firstOrNull { it.status == "OPEN" }
    var showOpenShiftDialog by remember { mutableStateOf(false) }
    var showCloseShiftDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Shift Status Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (openShift != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, if (openShift != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (openShift != null) "وردية الكاشير الحالية: ${openShift.shiftNumber}" else "لا توجد وردية مفتوحة حالياً",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            if (openShift != null) {
                                Text(
                                    text = "بدأت: ${Formatters.formatDate(openShift.startTime)} بواسطة ${openShift.openedBy}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        PulsingStatusBadge(
                            text = if (openShift != null) "وردية نشطة" else "الوردية مغلقة",
                            isActive = openShift != null
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (openShift != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("العهدة الافتتاحية", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                Text(Formatters.formatMoney(openShift.openingCash, settings), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Button(
                                onClick = { showCloseShiftDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.LockClock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إقفال وتسوية الوردية", fontSize = 12.sp)
                            }
                        }
                    } else {
                        Button(
                            onClick = { showOpenShiftDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("فتح وردية جديدة وكتابة العهدة النقدية")
                        }
                    }
                }
            }
        }

        item {
            Text("سجل الورديات السابقة والإقفالات", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        if (shifts.isEmpty()) {
            item {
                Text("لا توجد سجلات ورديات سابقة", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            }
        } else {
            items(shifts.filter { it.status == "CLOSED" }) { shift ->
                ShiftHistoryCard(shift, settings)
            }
        }
    }

    if (showOpenShiftDialog) {
        OpenShiftDialog(
            onDismiss = { showOpenShiftDialog = false },
            onOpen = { cash ->
                viewModel.openShift(cash) {
                    showOpenShiftDialog = false
                }
            }
        )
    }

    if (showCloseShiftDialog && openShift != null) {
        CloseShiftDialog(
            shift = openShift,
            onDismiss = { showCloseShiftDialog = false },
            onClose = { counts, actual, notes ->
                viewModel.closeShift(openShift, counts, actual, notes) {
                    showCloseShiftDialog = false
                }
            }
        )
    }
}

@Composable
private fun ShiftHistoryCard(shift: ShiftRecord, settings: StoreSettings?) {
    val hasVariance = Math.abs(shift.variance) > 0.01
    val isShortage = shift.variance < -0.01

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "وردية #${shift.shiftNumber} (${shift.openedBy})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = if (!hasVariance) "مطابق تماماً ✓" else if (isShortage) "عجز: ${Formatters.formatMoney(shift.variance, settings)}" else "فائض: +${Formatters.formatMoney(shift.variance, settings)}",
                    color = if (!hasVariance) Color(0xFF16A34A) else if (isShortage) Color(0xFFDC2626) else Color(0xFF2563EB),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("المتوقع: ${Formatters.formatMoney(shift.expectedCash, settings)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                Text("الفعلي بالجرد: ${Formatters.formatMoney(shift.actualCash, settings)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            if (shift.notes.isNotBlank()) {
                Text("ملاحظات: ${shift.notes}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun OpenShiftDialog(onDismiss: () -> Unit, onOpen: (Double) -> Unit) {
    var cashText by remember { mutableStateOf("0") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("فتح وردية كاشير جديدة", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = cashText,
                    onValueChange = { cashText = it },
                    label = { Text("العهدة النقدية الافتتاحية في الدرج") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onOpen(cashText.toDoubleOrNull() ?: 0.0) }) { Text("فتح الوردية") }
                }
            }
        }
    }
}

@Composable
private fun CloseShiftDialog(
    shift: ShiftRecord,
    onDismiss: () -> Unit,
    onClose: (Map<Int, Int>, Double, String) -> Unit
) {
    val counts = remember { mutableStateMapOf<Int, Int>() }
    var notes by remember { mutableStateOf("") }
    val totalCash = remember(counts) {
        listOf(500, 200, 100, 50, 20, 10, 5, 1).sumOf { d -> (counts[d] ?: 0) * d }.toDouble()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("إقفال الوردية وجرد النقدية بالدرج", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(10.dp))

                // Cash Counter
                CashDenominationsCounter(
                    counts = counts,
                    onCountChange = { d, c -> counts[d] = c }
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات الإقفال (أسباب الفروقات إن وجدت)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onClose(counts, totalCash, notes) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Text("اعتماد الإقفال")
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. Cheques & PDC Tab (أوراق القبض والدفع)
// ==========================================
@Composable
private fun ChequesManagementTab(
    viewModel: MainViewModel,
    cheques: List<Cheque>,
    settings: StoreSettings?
) {
    var filterType by remember { mutableStateOf("ALL") } // ALL, RECEIVABLE, PAYABLE
    var showAddDialog by remember { mutableStateOf(false) }

    val filtered = remember(cheques, filterType) {
        if (filterType == "ALL") cheques
        else cheques.filter { it.chequeType == filterType }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = filterType == "ALL",
                    onClick = { filterType = "ALL" },
                    label = { Text("الكل (${cheques.size})", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = filterType == "RECEIVABLE",
                    onClick = { filterType = "RECEIVABLE" },
                    label = { Text("أوراق قبض (لنا)", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = filterType == "PAYABLE",
                    onClick = { filterType = "PAYABLE" },
                    label = { Text("أوراق دفع (علينا)", fontSize = 11.sp) }
                )
            }

            Button(
                onClick = { showAddDialog = true },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("شيك جديد", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا توجد شيكات مسجلة", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { cheque ->
                    ChequeItemCard(
                        cheque = cheque,
                        settings = settings,
                        onStatusChange = { newStatus ->
                            viewModel.updateChequeStatus(cheque, newStatus)
                        },
                        onDelete = {
                            viewModel.deleteCheque(cheque)
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddChequeDialog(
            onDismiss = { showAddDialog = false },
            onSave = { chq ->
                viewModel.saveCheque(chq) {
                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
private fun ChequeItemCard(
    cheque: Cheque,
    settings: StoreSettings?,
    onStatusChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    val isReceivable = cheque.chequeType == "RECEIVABLE"
    val statusColor = when (cheque.status) {
        "COLLECTED" -> Color(0xFF16A34A)
        "PENDING" -> Color(0xFFF59E0B)
        "BOUNCED" -> Color(0xFFDC2626)
        else -> Color.Gray
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (isReceivable) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isReceivable) "ورقة قبض" else "ورقة دفع",
                            color = if (isReceivable) Color(0xFF047857) else Color(0xFFB91C1C),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "شيك رقم: ${cheque.chequeNumber}",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Text(
                    text = Formatters.formatMoney(cheque.amount, settings),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("المستفيد/الساحب: ${cheque.partyName}", fontSize = 12.sp)
                Text("البنك: ${cheque.bankName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("تاريخ الاستحقاق: ${Formatters.formatDate(cheque.dueDate)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (cheque.status != "COLLECTED") {
                        FilledTonalButton(
                            onClick = { onStatusChange("COLLECTED") },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("تحصيل ✓", fontSize = 10.sp)
                        }
                    }
                    if (cheque.status != "BOUNCED") {
                        OutlinedButton(
                            onClick = { onStatusChange("BOUNCED") },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("مرتجع ✗", fontSize = 10.sp, color = Color(0xFFDC2626))
                        }
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddChequeDialog(onDismiss: () -> Unit, onSave: (Cheque) -> Unit) {
    var chequeNumber by remember { mutableStateOf("") }
    var chequeType by remember { mutableStateOf("RECEIVABLE") }
    var bankName by remember { mutableStateOf("") }
    var partyName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("تسجيل ورقة قبض أو دفع جديدة", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("RECEIVABLE" to "ورقة قبض (استلام من عميل)", "PAYABLE" to "ورقة دفع (صرف لمورد)").forEach { (t, label) ->
                        FilterChip(
                            selected = chequeType == t,
                            onClick = { chequeType = t },
                            label = { Text(label, fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = chequeNumber, onValueChange = { chequeNumber = it }, label = { Text("رقم الشيك") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = bankName, onValueChange = { bankName = it }, label = { Text("اسم البنك المسحوب عليه") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = partyName, onValueChange = { partyName = it }, label = { Text("الطرف (العميل أو المورد)") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text("المبلغ") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (chequeNumber.isNotBlank() && amt > 0) {
                            onSave(
                                Cheque(
                                    chequeNumber = chequeNumber,
                                    chequeType = chequeType,
                                    bankName = bankName,
                                    partyName = partyName,
                                    amount = amt,
                                    dueDate = System.currentTimeMillis() + (7L * 24 * 3600 * 1000)
                                )
                            )
                        }
                    }) {
                        Text("حفظ الشيك")
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. Fixed Assets Tab (الأصول الثابتة والإهلاك)
// ==========================================
@Composable
private fun FixedAssetsTab(
    viewModel: MainViewModel,
    assets: List<FixedAsset>,
    settings: StoreSettings?
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("الأصول الرأسمالية وجدول الإهلاك الشهري", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Button(
                onClick = { showAddDialog = true },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("إضافة أصل", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (assets.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا توجد أصول ثابتة مسجلة بعد", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(assets, key = { it.id }) { asset ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(asset.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    text = "القيمة الدفترية: ${Formatters.formatMoney(asset.bookValue, settings)}",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("تكلفة الشراء: ${Formatters.formatMoney(asset.purchasePrice, settings)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                Text("مجمع الإهلاك: ${Formatters.formatMoney(asset.accumulatedDepreciation, settings)}", fontSize = 11.sp, color = Color(0xFFDC2626))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "قسط الإهلاك: ${Formatters.formatMoney(asset.monthlyDepreciation, settings)}/شهر",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Button(
                                        onClick = { viewModel.depreciateFixedAsset(asset) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("تسجيل إهلاك الشهر ⚡", fontSize = 10.sp)
                                    }
                                    IconButton(onClick = { viewModel.deleteFixedAsset(asset) }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddAssetDialog(
            onDismiss = { showAddDialog = false },
            onSave = { ast ->
                viewModel.saveFixedAsset(ast) {
                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
private fun AddAssetDialog(onDismiss: () -> Unit, onSave: (FixedAsset) -> Unit) {
    var name by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var salvageText by remember { mutableStateOf("0") }
    var yearsText by remember { mutableStateOf("3") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("إضافة أصل رأسمالي جديد", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم الأصل (مثلاً: ماكينة باركود، سيارة توزيع)") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = priceText, onValueChange = { priceText = it }, label = { Text("تكلفة الشراء") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = salvageText, onValueChange = { salvageText = it }, label = { Text("القيمة التخريدية (الخردة)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = yearsText, onValueChange = { yearsText = it }, label = { Text("العمر الإنتاجي (سنوات)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val price = priceText.toDoubleOrNull() ?: 0.0
                        val salvage = salvageText.toDoubleOrNull() ?: 0.0
                        val years = yearsText.toIntOrNull() ?: 3
                        if (name.isNotBlank() && price > 0) {
                            onSave(FixedAsset(name = name, purchasePrice = price, salvageValue = salvage, usefulLifeYears = years))
                        }
                    }) {
                        Text("حفظ الأصل")
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. Cost Centers Tab (مراكز التكلفة)
// ==========================================
@Composable
private fun CostCentersTab(
    viewModel: MainViewModel,
    costCenters: List<CostCenter>
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("مراكز التكلفة والمشاريع والفروع", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Button(
                onClick = { showAddDialog = true },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("مركز جديد", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (costCenters.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا توجد مراكز تكلفة مسجلة", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(costCenters, key = { it.id }) { cc ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(cc.code, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(cc.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                if (cc.description.isNotBlank()) {
                                    Text(cc.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { viewModel.deleteCostCenter(cc) }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var code by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("إضافة مركز تكلفة جديد", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("رمز المركز (مثلاً: CC-01)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم المركز (مثلاً: فرع المبيعات 1)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("الوصف أو النطاق") }, modifier = Modifier.fillMaxWidth())

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddDialog = false }) { Text("إلغاء") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            if (code.isNotBlank() && name.isNotBlank()) {
                                viewModel.saveCostCenter(CostCenter(code = code, name = name, description = desc)) {
                                    showAddDialog = false
                                }
                            }
                        }) {
                            Text("حفظ")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. Transfers Tab (التحويل المالي بين الصناديق والبنوك)
// ==========================================
@Composable
private fun TransfersTab(
    viewModel: MainViewModel,
    transfers: List<AccountTransfer>,
    cashTransactions: List<CashTransaction>,
    settings: StoreSettings?
) {
    val currentCash = cashTransactions.firstOrNull()?.balanceAfter ?: 0.0
    var fromAccount by remember { mutableStateOf("الصندوق الرئيسي (النقدية)") }
    var toAccount by remember { mutableStateOf("البنك / حساب الشبكة ومدى") }
    var amountText by remember { mutableStateOf("") }
    var feeText by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Visual Bank & Cash Cards
            VisualAccountCard(
                accountName = "الصندوق الرئيسي (النقدية)",
                accountType = "CASH",
                balance = currentCash,
                accountNumber = "CASH-MAIN-DRAWER",
                settings = settings,
                onTransfer = {
                    fromAccount = "الصندوق الرئيسي (النقدية)"
                    toAccount = "البنك / حساب الشبكة ومدى"
                }
            )
        }

        item {
            // Transfer Form Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("إجراء تحويل مالي بين الحسابات", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = fromAccount,
                            onValueChange = { fromAccount = it },
                            label = { Text("من حساب") },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                val temp = fromAccount
                                fromAccount = toAccount
                                toAccount = temp
                            },
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "تبديل")
                        }
                        OutlinedTextField(
                            value = toAccount,
                            onValueChange = { toAccount = it },
                            label = { Text("إلى حساب") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            label = { Text("المبلغ المحول") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = feeText,
                            onValueChange = { feeText = it },
                            label = { Text("عمولة التحويل") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("بيان / ملاحظات التحويل") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            val fee = feeText.toDoubleOrNull() ?: 0.0
                            if (amt > 0) {
                                viewModel.performTransfer(fromAccount, toAccount, amt, fee, notes) {
                                    amountText = ""
                                    notes = ""
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تنفيذ التحويل وتوليد سند القيد المحاسبي")
                    }
                }
            }
        }

        item {
            Text("سجل التحويلات المالية", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        if (transfers.isEmpty()) {
            item {
                Text("لا توجد تحويلات مالية مسجلة بعد", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
            }
        } else {
            items(transfers) { trf ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${trf.fromAccount} ← ${trf.toAccount}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "${Formatters.formatDate(trf.transferDate)} • رقم: ${trf.transferNumber}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Text(
                            text = Formatters.formatMoney(trf.amount, settings),
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
