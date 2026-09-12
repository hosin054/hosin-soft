package com.example.ui.screens.cash

import androidx.compose.foundation.background
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
import com.example.ui.MainViewModel
import com.example.ui.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashScreen(
    viewModel: MainViewModel
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val cashTx by viewModel.allCashTransactions.collectAsStateWithLifecycle()

    var showOpeningCashDialog by remember { mutableStateOf(false) }

    val currentBalance = cashTx.firstOrNull()?.balanceAfter ?: 0.0
    val totalIn = cashTx.filter { it.amount > 0 }.sumOf { it.amount }
    val totalOut = cashTx.filter { it.amount < 0 }.sumOf { -it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("حركة الخزينة والصندوق") },
                actions = {
                    TextButton(onClick = { showOpeningCashDialog = true }) {
                        Text("ضبط الرصيد الافتتاحي", color = MaterialTheme.colorScheme.onPrimary)
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
                .padding(14.dp)
        ) {
            // Main Cash Balance Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "الرصيد النقدي المتوفر حالياً في الصندوق",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = Formatters.formatMoney(currentBalance, settings),
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color.White)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "إجمالي المقبوضات: ${Formatters.formatMoney(totalIn, settings)}",
                            fontSize = 11.sp,
                            color = Color(0xFF15803D),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "إجمالي المدفوعات: ${Formatters.formatMoney(totalOut, settings)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "سجل حركات الصندوق (${cashTx.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (cashTx.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا توجد حركات نقدية مسجلة بعد", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cashTx) { tx ->
                        val isPositive = tx.amount > 0
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            if (isPositive) Color(0xFF15803D).copy(alpha = 0.12f) else MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPositive) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = if (isPositive) Color(0xFF15803D) else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${tx.transactionType} • ${tx.description}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "${Formatters.formatDate(tx.createdAt)} • ${tx.createdBy}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${if (isPositive) "+" else ""}${Formatters.formatMoney(tx.amount, settings)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isPositive) Color(0xFF15803D) else MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "الرصيد: ${Formatters.formatMoney(tx.balanceAfter, settings)}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(30.dp)) }
                }
            }
        }
    }

    // Opening Cash Dialog
    if (showOpeningCashDialog) {
        var amountInput by remember { mutableStateOf(currentBalance.toString()) }

        AlertDialog(
            onDismissRequest = { showOpeningCashDialog = false },
            title = { Text("تعديل الرصيد الافتتاحي للصندوق") },
            text = {
                Column {
                    Text("أدخل المبلغ الفعلي المتواجد في الخزينة/الدرج لضبط رصيد البداية:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("المبلغ النقدي") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountInput.toDoubleOrNull()
                        if (amt != null && amt >= 0) {
                            viewModel.setOpeningCash(amt)
                            showOpeningCashDialog = false
                        } else {
                            viewModel.showMessage("المبلغ غير صالح")
                        }
                    }
                ) {
                    Text("حفظ وتحديث")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOpeningCashDialog = false }) { Text("إلغاء") }
            }
        )
    }
}
