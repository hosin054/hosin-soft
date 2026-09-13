package com.example.ui.screens.accounting

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.data.model.ChartOfAccount
import com.example.ui.MainViewModel
import com.example.ui.components.GlassCard
import com.example.ui.components.PulsingStatusBadge
import com.example.ui.util.BackupHelper
import com.example.ui.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartOfAccountsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accounts by viewModel.allChartOfAccounts.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("ALL") } // ALL, ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<ChartOfAccount?>(null) }
    var isSendingTelegram by remember { mutableStateOf(false) }

    val filteredAccounts = remember(accounts, searchQuery, selectedTypeFilter) {
        accounts.filter { acc ->
            val matchType = selectedTypeFilter == "ALL" || acc.accountType == selectedTypeFilter
            val matchQuery = searchQuery.isBlank() ||
                    acc.code.contains(searchQuery, ignoreCase = true) ||
                    acc.name.contains(searchQuery, ignoreCase = true)
            matchType && matchQuery
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("شجرة الحسابات (الدليل المحاسبي)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = "${accounts.size} حساب مسجل",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isSendingTelegram = true
                            val sb = StringBuilder()
                            sb.appendLine("🌳 *دليل الحسابات الشجري - ${settings?.storeName ?: "حسين سوفت"}*")
                            sb.appendLine("التاريخ: ${Formatters.formatDate(System.currentTimeMillis())}")
                            sb.appendLine("───────────────")
                            accounts.sortedBy { it.code }.forEach { a ->
                                val indent = "  ".repeat((a.level - 1).coerceAtLeast(0))
                                sb.appendLine("$indent• `${a.code}` ${a.name} (${translateType(a.accountType)})")
                            }
                            BackupHelper.shareToTelegram(context, sb.toString(), "شجرة الحسابات - تلغرام")
                            isSendingTelegram = false
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "تليجرام", tint = Color(0xFF0284C7))
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingAccount = null
                    showAddDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("إضافة حساب جديد") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("btn_add_account")
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث برقم الكود أو اسم الحساب...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            // Account Type Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChipItem("الكل", selectedTypeFilter == "ALL") { selectedTypeFilter = "ALL" }
                FilterChipItem("الأصول (1)", selectedTypeFilter == "ASSET") { selectedTypeFilter = "ASSET" }
                FilterChipItem("الخصوم (2)", selectedTypeFilter == "LIABILITY") { selectedTypeFilter = "LIABILITY" }
                FilterChipItem("الملكية (3)", selectedTypeFilter == "EQUITY") { selectedTypeFilter = "EQUITY" }
                FilterChipItem("الإيراد (4)", selectedTypeFilter == "REVENUE") { selectedTypeFilter = "REVENUE" }
                FilterChipItem("المصروف (5)", selectedTypeFilter == "EXPENSE") { selectedTypeFilter = "EXPENSE" }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tree List of Accounts
            if (filteredAccounts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد حسابات مطابقة للبحث",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredAccounts, key = { it.id }) { acc ->
                        ChartAccountItem(
                            account = acc,
                            onEdit = {
                                editingAccount = acc
                                showAddDialog = true
                            },
                            onDelete = {
                                viewModel.deleteChartAccount(acc)
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }

    if (showAddDialog) {
        AccountEditDialog(
            account = editingAccount,
            existingAccounts = accounts,
            onDismiss = { showAddDialog = false },
            onSave = { acc ->
                viewModel.saveChartAccount(acc) {
                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
private fun RowScope.FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChartAccountItem(
    account: ChartOfAccount,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val indentPadding = ((account.level - 1) * 16).coerceAtLeast(0).dp
    val typeColor = when (account.accountType) {
        "ASSET" -> Color(0xFF10B981)
        "LIABILITY" -> Color(0xFFEF4444)
        "EQUITY" -> Color(0xFF8B5CF6)
        "REVENUE" -> Color(0xFF3B82F6)
        "EXPENSE" -> Color(0xFFF59E0B)
        else -> Color.Gray
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (account.level == 1) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        ),
        border = if (account.level == 1) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indentPadding)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Level / Hierarchy Node Icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(typeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${account.level}",
                    fontWeight = FontWeight.Bold,
                    color = typeColor,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = account.code,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = typeColor,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = account.name,
                        fontWeight = if (account.level <= 2) FontWeight.ExtraBold else FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = translateType(account.accountType),
                        fontSize = 11.sp,
                        color = typeColor,
                        fontWeight = FontWeight.Medium
                    )
                    if (account.isSubAccount) {
                        Text(
                            text = "• حساب فرعي يقبل القيود",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "تعديل", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
            }

            // Don't delete root main accounts (1, 2, 3, 4, 5)
            if (account.level > 1) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", modifier = Modifier.size(18.dp), tint = Color(0xFFEF4444))
                }
            }
        }
    }
}

@Composable
private fun AccountEditDialog(
    account: ChartOfAccount?,
    existingAccounts: List<ChartOfAccount>,
    onDismiss: () -> Unit,
    onSave: (ChartOfAccount) -> Unit
) {
    var code by remember { mutableStateOf(account?.code ?: "") }
    var name by remember { mutableStateOf(account?.name ?: "") }
    var accountType by remember { mutableStateOf(account?.accountType ?: "ASSET") }
    var parentCode by remember { mutableStateOf(account?.parentCode ?: "") }
    var isSubAccount by remember { mutableStateOf(account?.isSubAccount ?: true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (account == null) "إضافة حساب جديد للشجرة" else "تعديل بيانات الحساب",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("رمز الحساب (الكود، مثلاً 1106)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الحساب (مثلاً: بنك الراجحي، سيارة توزيع)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = parentCode,
                    onValueChange = { parentCode = it },
                    label = { Text("رمز الحساب الأب (مثلاً 11 للأصول المتداولة)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Account Type Selector
                Text("نوع الحساب الرئيسي:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("ASSET" to "أصل", "LIABILITY" to "خصم", "EQUITY" to "ملكية", "REVENUE" to "إيراد", "EXPENSE" to "مصروف").forEach { (type, label) ->
                        val selected = accountType == type
                        Surface(
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { accountType = type }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isSubAccount = !isSubAccount }
                ) {
                    Checkbox(checked = isSubAccount, onCheckedChange = { isSubAccount = it })
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حساب فرعي تحليلي (يقبل إدراج قيود وسندات)", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (code.isBlank() || name.isBlank()) return@Button
                            val level = when {
                                parentCode.isBlank() -> 1
                                parentCode.length == 1 -> 2
                                else -> 3
                            }
                            val updated = (account ?: ChartOfAccount(code = code, name = name, accountType = accountType)).copy(
                                code = code.trim(),
                                name = name.trim(),
                                accountType = accountType,
                                parentCode = parentCode.trim(),
                                level = level,
                                isSubAccount = isSubAccount
                            )
                            onSave(updated)
                        }
                    ) {
                        Text("حفظ الحساب")
                    }
                }
            }
        }
    }
}

private fun translateType(type: String): String {
    return when (type) {
        "ASSET" -> "الأصول"
        "LIABILITY" -> "الخصوم والالتزامات"
        "EQUITY" -> "حقوق الملكية"
        "REVENUE" -> "الإيرادات"
        "EXPENSE" -> "المصروفات والتكاليف"
        else -> type
    }
}
