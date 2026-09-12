package com.example.ui.screens.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StoreSettings
import com.example.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    viewModel: MainViewModel,
    onSetupComplete: () -> Unit
) {
    var storeName by remember { mutableStateOf("متجر حسين") }
    var ownerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var currencyName by remember { mutableStateOf("ريال سعودي") }
    var currencySymbol by remember { mutableStateOf("ر.س") }
    var decimalPlaces by remember { mutableStateOf("2") }
    var taxEnabled by remember { mutableStateOf(false) }
    var taxRate by remember { mutableStateOf("15.0") }
    var securityPin by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("معالج الإعداد الأولي – حسين سوفت") },
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "مرحباً بك في نظام حسين سوفت",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "يرجى ملء بيانات متجرك لبدء استخدام النظام المحاسبي",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Form Fields
            OutlinedTextField(
                value = storeName,
                onValueChange = { storeName = it },
                label = { Text("اسم المتجر / المنشأة *") },
                leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = ownerName,
                onValueChange = { ownerName = it },
                label = { Text("اسم المالك / المسؤول (اختياري)") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("رقم الهاتف / الجوال (اختياري)") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("العنوان / المدينة (اختياري)") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "إعدادات العملة والضريبة",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = currencyName,
                    onValueChange = { currencyName = it },
                    label = { Text("اسم العملة") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedTextField(
                    value = currencySymbol,
                    onValueChange = { currencySymbol = it },
                    label = { Text("رمز العملة") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = decimalPlaces,
                onValueChange = { decimalPlaces = it },
                label = { Text("عدد المنازل العشرية (مثال: 2)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "تفعيل ضريبة القيمة المضافة", fontWeight = FontWeight.Medium)
                        Switch(
                            checked = taxEnabled,
                            onCheckedChange = { taxEnabled = it }
                        )
                    }
                    if (taxEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = taxRate,
                            onValueChange = { taxRate = it },
                            label = { Text("نسبة الضريبة الافتراضية (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = securityPin,
                onValueChange = { securityPin = it },
                label = { Text("رقم سري لحماية التطبيق (اختياري)") },
                placeholder = { Text("اتركه فارغاً إذا كنت لا ترغب بقفل") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (storeName.isBlank()) {
                        viewModel.showMessage("يرجى إدخال اسم المتجر")
                        return@Button
                    }
                    val settings = StoreSettings(
                        id = 1,
                        storeName = storeName.trim(),
                        ownerName = ownerName.trim(),
                        phone = phone.trim(),
                        address = address.trim(),
                        currencyName = currencyName.trim(),
                        currencySymbol = currencySymbol.trim(),
                        decimalPlaces = decimalPlaces.toIntOrNull() ?: 2,
                        taxEnabled = taxEnabled,
                        taxRate = taxRate.toDoubleOrNull() ?: 15.0,
                        securityPin = securityPin.trim(),
                        isConfigured = true
                    )
                    viewModel.saveStoreSettings(settings)
                    onSetupComplete()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("حفظ والبدء في استخدام حسين سوفت", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    val settings = StoreSettings(
                        id = 1,
                        storeName = storeName.ifBlank { "متجر حسين للمواد الغذائية" },
                        ownerName = ownerName.ifBlank { "حسين عبدالله" },
                        phone = phone.ifBlank { "0501234567" },
                        address = address.ifBlank { "الرياض - المملكة العربية السعودية" },
                        currencyName = currencyName,
                        currencySymbol = currencySymbol,
                        decimalPlaces = decimalPlaces.toIntOrNull() ?: 2,
                        taxEnabled = taxEnabled,
                        taxRate = taxRate.toDoubleOrNull() ?: 15.0,
                        securityPin = securityPin.trim(),
                        isConfigured = true
                    )
                    viewModel.saveStoreSettings(settings)
                    viewModel.loadDemoData {
                        onSetupComplete()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("حفظ وتحميل بيانات تجريبية Demo (اختياري)")
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
