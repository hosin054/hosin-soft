package com.example.ui.screens.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.StoreSettings
import com.example.ui.MainViewModel
import com.example.ui.navigation.Screen
import com.example.ui.util.BackupHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateTo: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var storeName by remember(settings) { mutableStateOf(settings?.storeName ?: "") }
    var ownerName by remember(settings) { mutableStateOf(settings?.ownerName ?: "") }
    var phone by remember(settings) { mutableStateOf(settings?.phone ?: "") }
    var address by remember(settings) { mutableStateOf(settings?.address ?: "") }
    var currencySymbol by remember(settings) { mutableStateOf(settings?.currencySymbol ?: "ر.س") }
    var decimalPlaces by remember(settings) { mutableStateOf((settings?.decimalPlaces ?: 2).toString()) }
    var taxEnabled by remember(settings) { mutableStateOf(settings?.taxEnabled ?: false) }
    var taxRate by remember(settings) { mutableStateOf((settings?.taxRate ?: 15.0).toString()) }
    var securityPin by remember(settings) { mutableStateOf(settings?.securityPin ?: "") }

    var showPinDialog by remember { mutableStateOf(false) }
    var showDemoConfirmDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إعدادات النظام والنسخ الاحتياطي") },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Store Profile Section
            Text(text = "بيانات المتجر والمنشأة", fontWeight = FontWeight.Bold, fontSize = 14.sp)

            OutlinedTextField(
                value = storeName,
                onValueChange = { storeName = it },
                label = { Text("اسم المتجر") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = ownerName,
                onValueChange = { ownerName = it },
                label = { Text("اسم المالك / المسؤول") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("الهاتف") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("العنوان") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider()

            // Financial & Tax Settings
            Text(text = "إعدادات العملة والضريبة", fontWeight = FontWeight.Bold, fontSize = 14.sp)

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = currencySymbol,
                    onValueChange = { currencySymbol = it },
                    label = { Text("رمز العملة") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = decimalPlaces,
                    onValueChange = { decimalPlaces = it },
                    label = { Text("المنازل العشرية") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "تفعيل ضريبة القيمة المضافة")
                Switch(checked = taxEnabled, onCheckedChange = { taxEnabled = it })
            }

            if (taxEnabled) {
                OutlinedTextField(
                    value = taxRate,
                    onValueChange = { taxRate = it },
                    label = { Text("نسبة الضريبة (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = {
                    val updated = (settings ?: StoreSettings()).copy(
                        storeName = storeName.trim(),
                        ownerName = ownerName.trim(),
                        phone = phone.trim(),
                        address = address.trim(),
                        currencySymbol = currencySymbol.trim(),
                        decimalPlaces = decimalPlaces.toIntOrNull() ?: 2,
                        taxEnabled = taxEnabled,
                        taxRate = taxRate.toDoubleOrNull() ?: 15.0,
                        isConfigured = true
                    )
                    viewModel.saveStoreSettings(updated)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("حفظ التعديلات")
            }

            // Manage Currency Exchange Rates
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateTo(Screen.Currencies.route) }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "أسعار صرف العملات وحاسبة التحويل",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "إدخال وتعديل أسعار صرف العملات مقابل العملة الأساسية",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                    Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider()

            // Security & PIN
            Text(text = "الأمان وحماية التطبيق", fontWeight = FontWeight.Bold, fontSize = 14.sp)

            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPinDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "الرقم السري لقفل التطبيق", fontWeight = FontWeight.Medium)
                            Text(
                                text = if (settings?.securityPin.isNullOrBlank()) "غير مفعل (حماية معطلة)" else "مفعل ومحمي برقم سري",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(text = "تعديل", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }

            // Audit Logs Link
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateTo(Screen.AuditLogs.route) }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "سجل العمليات والرقابة (Audit Trail)", fontWeight = FontWeight.Medium)
                            Text(text = "تتبع كافة الإضافات والتعديلات والحذف", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Icon(Icons.Default.ChevronLeft, contentDescription = null)
                }
            }

            // Import/Export Link
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateTo(Screen.ImportExport.route) }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ImportExport, contentDescription = null, tint = Color(0xFF0284C7))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "استيراد وتصدير إكسل و CSV", fontWeight = FontWeight.Medium)
                            Text(text = "تصدير القوائم واستيراد الأصناف مع فحص الحقول", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Icon(Icons.Default.ChevronLeft, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider()

            // Backup & Restore
            Text(text = "النسخ الاحتياطي الشامل للبيانات", fontWeight = FontWeight.Bold, fontSize = 14.sp)

            Button(
                onClick = {
                    scope.launch {
                        try {
                            val json = BackupHelper.createBackupJson(
                                com.example.data.database.AppDatabase.getDatabase(context)
                            )
                            BackupHelper.shareText(context, json, "نسخة_احتياطية_حسين_سوفت.json")
                        } catch (e: Exception) {
                            viewModel.showMessage("خطأ أثناء إنشاء النسخة الاحتياطية: ${e.message}")
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("تصدير ومشاركة نسخة احتياطية كاملة (JSON)")
            }

            OutlinedButton(
                onClick = { showDemoConfirmDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("إعادة تحميل البيانات التجريبية Demo")
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // PIN dialog
    if (showPinDialog) {
        var newPin by remember { mutableStateOf(settings?.securityPin ?: "") }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("ضبط الرقم السري PIN") },
            text = {
                Column {
                    Text("اترك الحقل فارغاً لإلغاء القفل، أو أدخل 4 أرقام لتفعيل حماية التطبيق عند الفتح:", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { newPin = it },
                        label = { Text("الرقم السري") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = (settings ?: StoreSettings()).copy(securityPin = newPin.trim())
                        viewModel.saveStoreSettings(updated)
                        showPinDialog = false
                    }
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // Demo Confirm Dialog
    if (showDemoConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDemoConfirmDialog = false },
            title = { Text("تأكيد تحميل بيانات تجريبية") },
            text = { Text("سيتم إضافة أصناف وعملاء وموردين تجريبيين للتجربة والاختبار. هل ترغب بالاستمرار؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.loadDemoData {
                            showDemoConfirmDialog = false
                        }
                    }
                ) {
                    Text("نعم، تحميل")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDemoConfirmDialog = false }) { Text("إلغاء") }
            }
        )
    }
}
