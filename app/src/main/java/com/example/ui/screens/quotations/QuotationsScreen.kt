package com.example.ui.screens.quotations

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.ui.MainViewModel
import com.example.ui.util.Formatters
import com.example.ui.util.InvoicePrinter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotationsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPos: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val allInvoices by viewModel.allInvoices.collectAsStateWithLifecycle()
    val allItems by viewModel.allInvoiceItems.collectAsStateWithLifecycle()

    val quotations = remember(allInvoices) {
        allInvoices.filter { it.invoiceType == "QUOTATION" }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedQuotation by remember { mutableStateOf<Invoice?>(null) }

    val filteredQuotations = remember(searchQuery, quotations) {
        if (searchQuery.isBlank()) {
            quotations
        } else {
            quotations.filter {
                it.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                        it.partyName.contains(searchQuery, ignoreCase = true) ||
                        it.notes.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("عروض الأسعار للعملاء") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToPos) {
                        Icon(Icons.Default.Add, contentDescription = "إنشاء عرض أسعار جديد", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToPos,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("عرض أسعار جديد")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("بحث برقم العرض أو اسم العميل...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredQuotations.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.RequestQuote,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "لا توجد عروض أسعار مسجلة بعد" else "لا توجد نتائج مطابقة للبحث",
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onNavigateToPos) {
                            Text("إنشاء أول عرض أسعار من نقطة البيع")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredQuotations, key = { it.id }) { quote ->
                        val quoteItems = remember(allItems, quote.id) {
                            allItems.filter { it.invoiceId == quote.id }
                        }
                        QuotationCard(
                            quotation = quote,
                            itemsCount = quoteItems.size,
                            currency = settings?.currencySymbol ?: "ر.س",
                            onViewDetails = { selectedQuotation = quote },
                            onConvertToSale = {
                                viewModel.loadQuotationToCart(quote.id) {
                                    onNavigateToPos()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Detail Dialog
    selectedQuotation?.let { quote ->
        val quoteItems = remember(allItems, quote.id) {
            allItems.filter { it.invoiceId == quote.id }
        }

        QuotationDetailDialog(
            quotation = quote,
            items = quoteItems,
            settings = settings,
            onDismiss = { selectedQuotation = null },
            onShare = {
                val text = InvoicePrinter.generateQuotationText(quote, quoteItems, settings)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "عرض أسعار رقم ${quote.invoiceNumber}")
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(intent, "مشاركة عرض الأسعار عبر:"))
            },
            onConvertToSale = {
                selectedQuotation = null
                viewModel.loadQuotationToCart(quote.id) {
                    onNavigateToPos()
                }
            }
        )
    }
}

@Composable
fun QuotationCard(
    quotation: Invoice,
    itemsCount: Int,
    currency: String,
    onViewDetails: () -> Unit,
    onConvertToSale: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = quotation.invoiceNumber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                AssistChip(
                    onClick = {},
                    label = { Text("سارٍ", fontSize = 11.sp) },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "العميل: ${quotation.partyName}",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )

            Text(
                text = "التاريخ: ${Formatters.formatDate(quotation.createdAt)} • $itemsCount أصناف",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline
            )

            if (quotation.notes.isNotBlank()) {
                Text(
                    text = "ملاحظات: ${quotation.notes}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "الإجمالي التقديري", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = "${String.format(java.util.Locale.ENGLISH, "%.2f", quotation.totalAmount)} $currency",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onViewDetails,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("عرض", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onConvertToSale,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تحويل للبيع", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun QuotationDetailDialog(
    quotation: Invoice,
    items: List<InvoiceItem>,
    settings: com.example.data.model.StoreSettings?,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onConvertToSale: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("تفاصيل عرض الأسعار", fontWeight = FontWeight.Bold)
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "مشاركة")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "رقم العرض: ${quotation.invoiceNumber}", fontWeight = FontWeight.Bold)
                        Text(text = "العميل: ${quotation.partyName}", fontSize = 13.sp)
                        Text(text = "التاريخ: ${Formatters.formatDate(quotation.createdAt)}", fontSize = 12.sp)
                        if (quotation.notes.isNotBlank()) {
                            Text(text = "الشروط: ${quotation.notes}", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "قائمة الأصناف المشمولة:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(items) { item ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = item.productName, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    Text(
                                        text = "${item.quantity} ${item.unitName} × ${Formatters.formatMoney(item.unitPrice, settings)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Text(
                                    text = Formatters.formatMoney(item.total, settings),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "المجموع الفرعي:")
                    Text(text = Formatters.formatMoney(quotation.subtotal, settings))
                }
                if (quotation.discountAmount > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "الخصم:")
                        Text(text = Formatters.formatMoney(quotation.discountAmount, settings))
                    }
                }
                if (quotation.taxAmount > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "الضريبة:")
                        Text(text = Formatters.formatMoney(quotation.taxAmount, settings))
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "الإجمالي التقديري:", fontWeight = FontWeight.Bold)
                    Text(
                        text = Formatters.formatMoney(quotation.totalAmount, settings),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConvertToSale) {
                Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("تحويل إلى مبيعات")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}
