package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.StoreSettings
import com.example.ui.util.Formatters

/**
 * 1. Glassmorphism Card
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
    borderColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

/**
 * 2. Pulsing Status Badge
 */
@Composable
fun PulsingStatusBadge(
    text: String,
    isActive: Boolean = true,
    activeColor: Color = Color(0xFF10B981),
    inactiveColor: Color = Color(0xFFEF4444),
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val scaleAnim by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val color = if (isActive) activeColor else inactiveColor

    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Outer glow ring
                Box(
                    modifier = Modifier
                        .size((10 * scaleAnim).dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = alphaAnim * 0.4f))
                )
                // Core solid dot
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

/**
 * 3. Animated Rolling Number Text
 */
@Composable
fun AnimatedRollingNumber(
    value: Double,
    settings: StoreSettings? = null,
    prefix: String = "",
    suffix: String = "",
    fontSize: androidx.compose.ui.unit.TextUnit = 18.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val animatedValue by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "rollingNumber"
    )

    val formatted = Formatters.formatMoney(animatedValue.toDouble(), settings)
    Text(
        text = "$prefix$formatted$suffix",
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color
    )
}

/**
 * 4. 3D Styled Interactive Donut Chart for Financial Visualizations
 */
data class DonutSegment(
    val title: String,
    val amount: Double,
    val color: Color
)

@Composable
fun Interactive3DDonutChart(
    segments: List<DonutSegment>,
    centerTitle: String,
    centerValue: String,
    modifier: Modifier = Modifier
) {
    val total = remember(segments) { segments.sumOf { it.amount } }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (total <= 0.0) {
                        drawCircle(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            style = Stroke(width = 32f)
                        )
                        return@Canvas
                    }

                    var currentAngle = -90f
                    val strokeWidth = 34f

                    segments.forEachIndexed { index, seg ->
                        val sweepAngle = ((seg.amount / total) * 360f).toFloat()
                        val isSelected = selectedIndex == index

                        // Draw segment arc
                        drawArc(
                            color = if (isSelected) seg.color else seg.color.copy(alpha = 0.88f),
                            startAngle = currentAngle + 1.5f,
                            sweepAngle = sweepAngle - 3f,
                            useCenter = false,
                            style = Stroke(
                                width = if (isSelected) strokeWidth + 6f else strokeWidth,
                                cap = StrokeCap.Round
                            ),
                            size = Size(size.width - strokeWidth, size.height - strokeWidth),
                            topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                        )
                        currentAngle += sweepAngle
                    }
                }

                // Center Title & Value
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (selectedIndex != null) segments[selectedIndex!!].title else centerTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (selectedIndex != null) Formatters.formatMoney(segments[selectedIndex!!].amount) else centerValue,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (selectedIndex != null) segments[selectedIndex!!].color else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive Legend Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                segments.take(4).forEachIndexed { idx, seg ->
                    val isSelected = selectedIndex == idx
                    Surface(
                        color = if (isSelected) seg.color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        border = if (isSelected) BorderStroke(1.dp, seg.color) else null,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                selectedIndex = if (isSelected) null else idx
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(seg.color)
                            )
                            Text(
                                text = seg.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 5. Visual Debit Card / Account Display Card
 */
@Composable
fun VisualAccountCard(
    accountName: String,
    accountType: String, // "CASH", "BANK", "CREDIT"
    balance: Double,
    accountNumber: String = "•••• 4082",
    settings: StoreSettings? = null,
    onTransfer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val gradient = when (accountType) {
        "BANK" -> Brush.linearGradient(listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)))
        "CASH" -> Brush.linearGradient(listOf(Color(0xFF134E5E), Color(0xFF71B280)))
        else -> Brush.linearGradient(listOf(Color(0xFF1F1C2C), Color(0xFF928DAB)))
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: Chip & Bank/Cash icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Gold Chip Graphic
                    Surface(
                        color = Color(0xFFD4AF37),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .width(36.dp)
                            .height(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawLine(Color(0xFF8A6E1E), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 2f)
                                drawLine(Color(0xFF8A6E1E), Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), 2f)
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (accountType == "BANK") Icons.Default.AccountBalance else Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = accountName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                // Middle: Masked Account Number
                Text(
                    text = accountNumber,
                    color = Color.White.copy(alpha = 0.75f),
                    letterSpacing = 3.sp,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp
                )

                // Bottom Row: Balance & Transfer Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "الرصيد المتاح",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                        Text(
                            text = Formatters.formatMoney(balance, settings),
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    }

                    FilledTonalButton(
                        onClick = onTransfer,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تحويل", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

/**
 * 6. Cash Denominations Drawer Counter
 * Cashier counts banknotes: 500, 200, 100, 50, 20, 10, 5, 1
 */
@Composable
fun CashDenominationsCounter(
    counts: Map<Int, Int>,
    onCountChange: (denom: Int, count: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val denominations = listOf(500, 200, 100, 50, 20, 10, 5, 1)
    val totalCalculated = remember(counts) {
        denominations.sumOf { d -> (counts[d] ?: 0) * d }
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("حاسبة فئات النقدية (الجرد الفعلي)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    text = "المجموع: ${Formatters.formatMoney(totalCalculated.toDouble())}",
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF16A34A),
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Grid of Denominations (2 columns)
            val chunked = denominations.chunked(2)
            chunked.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { denom ->
                        val current = counts[denom] ?: 0
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("فئة $denom", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text(
                                        text = "= ${denom * current}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { if (current > 0) onCountChange(denom, current - 1) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                    Text(
                                        text = "$current",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    IconButton(
                                        onClick = { onCountChange(denom, current + 1) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 7. Spotlight Universal Search Command Bar Dialog
 */
@Composable
fun SpotlightCommandDialog(
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }

    val quickCommands = listOf(
        CommandItem("نقطة البيع (POS)", "pos", Icons.Default.PointOfSale, "مبيعات سريعة وكاشير"),
        CommandItem("سند قيد محاسبي", "journal_vouchers", Icons.Default.PostAdd, "قيد مزدوج مدين ودائن"),
        CommandItem("الحسابات الختامية", "closing_accounts", Icons.Default.AccountBalance, "ميزان مراجعة وميزانية عمومية"),
        CommandItem("شجرة الحسابات (الدليل الشجري)", "chart_of_accounts", Icons.Default.AccountTree, "دليل الحسابات الشجري التفصيلي"),
        CommandItem("الأدوات المحاسبية والوردية", "financial_tools", Icons.Default.AutoGraph, "شيكات، أصول ثابتة، مراكز تكلفة، إقفال وردية"),
        CommandItem("حركة الصندوق والمصاريف", "cash", Icons.Default.AccountBalanceWallet, "يومية الصندوق والسيولة"),
        CommandItem("إدارة المنتجات والمخزون", "products", Icons.Default.Inventory2, "أصناف، باركود، أسعار وأرصدة"),
        CommandItem("العملاء والموردين", "parties", Icons.Default.People, "كشوف حسابات وأرصدة الديون"),
        CommandItem("فواتير المشتريات", "purchases", Icons.Default.ShoppingCart, "سجل مشتريات وتوريد بضاعة"),
        CommandItem("التقارير المالية والضريبية", "reports", Icons.Default.Assessment, "أرباح ومبيعات وإقرار ضريبي")
    )

    val filtered = remember(query) {
        if (query.isBlank()) quickCommands
        else quickCommands.filter { it.title.contains(query, ignoreCase = true) || it.desc.contains(query, ignoreCase = true) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.70f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("ابحث في كامل النظام (أمر، شاشة، فاتورة، قيد)...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "الأوامر والوجهات السريعة",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filtered.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onNavigate(item.route)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(item.desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Default.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class CommandItem(
    val title: String,
    val route: String,
    val icon: ImageVector,
    val desc: String
)
