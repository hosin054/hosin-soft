package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StoreSettings
import com.example.ui.util.Formatters
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * 3D Isometric Hero Infographic Banner
 * Renders a stylized 3D isometric financial scene using Canvas with gradients,
 * floating 3D coins, 3D cylinder bars, and high-tech glow accents.
 */
@Composable
fun Isometric3DHeroBanner(
    storeName: String,
    salesToday: Double,
    profitToday: Double,
    canViewProfits: Boolean,
    settings: StoreSettings?,
    onOpenReports: () -> Unit,
    onTelegramShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Subtle breathing/floating animation for 3D elements
    val infiniteTransition = rememberInfiniteTransition(label = "floating")
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coinFloat"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFF1D4ED8), Color(0xFF818CF8)))),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF020617)),
                        center = Offset(200f, 100f),
                        radius = 800f
                    )
                )
        ) {
            // Background 3D Isometric Canvas Graphic
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val w = size.width
                val h = size.height

                // Draw isometric background grid lines
                val gridColor = Color(0xFF38BDF8).copy(alpha = 0.08f)
                for (i in -4..10) {
                    val startX = i * 60f
                    drawLine(
                        color = gridColor,
                        start = Offset(startX, h),
                        end = Offset(startX + 220f, 0f),
                        strokeWidth = 1.5f
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset(startX, 0f),
                        end = Offset(startX + 220f, h),
                        strokeWidth = 1.5f
                    )
                }

                // Ambient glow behind 3D bars
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF2563EB).copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(w - 110f, h - 80f),
                        radius = 120f
                    )
                )

                // 3D Bar 1 (Left Cyan Bar)
                draw3DIsometricBar(
                    baseX = w - 160f,
                    baseY = h - 35f,
                    barWidth = 26f,
                    barDepth = 14f,
                    height = 80f + (floatAnim * 0.4f),
                    frontColor = Color(0xFF0284C7),
                    sideColor = Color(0xFF0369A1),
                    topColor = Color(0xFF38BDF8)
                )

                // 3D Bar 2 (Middle Royal Blue Bar)
                draw3DIsometricBar(
                    baseX = w - 115f,
                    baseY = h - 25f,
                    barWidth = 28f,
                    barDepth = 15f,
                    height = 125f + (floatAnim * 0.6f),
                    frontColor = Color(0xFF2563EB),
                    sideColor = Color(0xFF1D4ED8),
                    topColor = Color(0xFF60A5FA)
                )

                // 3D Bar 3 (Right Emerald Growth Bar)
                draw3DIsometricBar(
                    baseX = w - 65f,
                    baseY = h - 20f,
                    barWidth = 30f,
                    barDepth = 16f,
                    height = 155f - (floatAnim * 0.3f),
                    frontColor = Color(0xFF059669),
                    sideColor = Color(0xFF047857),
                    topColor = Color(0xFF34D399)
                )

                // 3D Floating Gold Coin
                val coinX = w - 100f
                val coinY = 45f + floatAnim

                // Coin shadow on canvas
                drawOval(
                    color = Color.Black.copy(alpha = 0.35f),
                    topLeft = Offset(coinX - 26f, h - 30f),
                    size = Size(52f, 14f)
                )

                // Golden Coin 3D Outer Rim
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFDE047), Color(0xFFEAB308), Color(0xFFCA8A04)),
                        center = Offset(coinX - 8f, coinY - 8f),
                        radius = 28f
                    ),
                    radius = 24f,
                    center = Offset(coinX, coinY)
                )
                // Coin Bevel Ring
                drawCircle(
                    color = Color(0xFFFEF08A),
                    radius = 19f,
                    center = Offset(coinX, coinY),
                    style = Stroke(width = 2.5f)
                )
                // Coin Center Star / Emblem
                drawCircle(
                    color = Color(0xFFB45309).copy(alpha = 0.6f),
                    radius = 8f,
                    center = Offset(coinX, coinY)
                )
                // Glowing star highlight
                drawCircle(
                    color = Color.White.copy(alpha = 0.9f),
                    radius = 3.5f,
                    center = Offset(coinX - 7f, coinY - 7f)
                )

                // Dynamic glowing curve connecting bars
                val path = Path().apply {
                    moveTo(w - 180f, h - 50f)
                    cubicTo(
                        w - 150f, h - 100f,
                        w - 100f, h - 140f,
                        w - 50f, h - 165f
                    )
                }
                drawPath(
                    path = path,
                    color = Color(0xFF38BDF8).copy(alpha = 0.7f),
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                )
            }

            // Foreground Content Layout
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Top Tag & Brand
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B).copy(alpha = 0.85f),
                        border = BorderStroke(0.5.dp, Color(0xFF38BDF8).copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF22C55E), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "حسين سوفت PRO • سحابي ولحظي",
                                color = Color(0xFFE2E8F0),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Telegram quick badge
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF0284C7).copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                        modifier = Modifier.clickable(onClick = onTelegramShare)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "تليجرام",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "تليجرام",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Big Headline & Financial Highlights
                Text(
                    text = storeName.ifBlank { "لوحة التحكم المالية الذكية" },
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "مبيعات اليوم المحققة:",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = Formatters.formatMoney(salesToday, settings),
                        color = Color(0xFF38BDF8),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    if (canViewProfits) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "(الربح: ${Formatters.formatMoney(profitToday, settings)})",
                            color = Color(0xFF4ADE80),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Interactive Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onOpenReports,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("التقارير والمخططات", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onTelegramShare,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                        border = BorderStroke(1.dp, Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إرسال تقرير لتلغرام", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/**
 * Helper to draw a pseudo-3D isometric bar with front face, side perspective face, and top face
 */
private fun DrawScope.draw3DIsometricBar(
    baseX: Float,
    baseY: Float,
    barWidth: Float,
    barDepth: Float,
    height: Float,
    frontColor: Color,
    sideColor: Color,
    topColor: Color
) {
    val topY = baseY - height

    // Front Face (Vertical Rectangle)
    drawRoundRect(
        color = frontColor,
        topLeft = Offset(baseX, topY),
        size = Size(barWidth, height),
        cornerRadius = CornerRadius(3f, 3f)
    )

    // Side 3D Face (Parallelogram on the right)
    val sidePath = Path().apply {
        moveTo(baseX + barWidth, topY)
        lineTo(baseX + barWidth + barDepth, topY - (barDepth * 0.5f))
        lineTo(baseX + barWidth + barDepth, baseY - (barDepth * 0.5f))
        lineTo(baseX + barWidth, baseY)
        close()
    }
    drawPath(path = sidePath, color = sideColor)

    // Top 3D Face (Isometric Top Diamond)
    val topPath = Path().apply {
        moveTo(baseX, topY)
        lineTo(baseX + barDepth, topY - (barDepth * 0.5f))
        lineTo(baseX + barWidth + barDepth, topY - (barDepth * 0.5f))
        lineTo(baseX + barWidth, topY)
        close()
    }
    drawPath(path = topPath, color = topColor)
}

/**
 * Interactive 3D Bar Infographic Chart
 * Displays animated financial bars with tap-to-inspect tooltips and percentages
 */
data class ChartBarData(
    val label: String,
    val value: Double,
    val color: Color,
    val secondaryColor: Color = color.copy(alpha = 0.7f),
    val icon: ImageVector? = null
)

@Composable
fun Interactive3DBarChart(
    title: String,
    subtitle: String,
    items: List<ChartBarData>,
    currencySymbol: String = "ر.س",
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val maxValue = remember(items) { items.maxOfOrNull { it.value }?.takeIf { it > 0.0 } ?: 1.0 }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Interactive Info Tag
                selectedIndex?.let { idx ->
                    val sel = items[idx]
                    val pct = if (maxValue > 0) (sel.value / items.sumOf { it.value } * 100.0) else 0.0
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = sel.color.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, sel.color)
                    ) {
                        Text(
                            text = "${sel.label}: ${String.format("%.1f", pct)}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = sel.color,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // The Chart Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(items) {
                            detectTapGestures { offset ->
                                val slotWidth = size.width / items.size
                                val clickedIndex = (offset.x / slotWidth).toInt().coerceIn(0, items.size - 1)
                                selectedIndex = if (selectedIndex == clickedIndex) null else clickedIndex
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height - 24f // leave room for baseline
                    val barCount = items.size
                    val slotWidth = w / barCount
                    val barWidth = min(slotWidth * 0.55f, 44f)

                    // Draw subtle grid lines
                    val lineStroke = 1f
                    val gridColor = Color.Gray.copy(alpha = 0.15f)
                    for (step in 1..3) {
                        val y = h * (step / 4f)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = lineStroke
                        )
                    }

                    // Draw baseline
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.3f),
                        start = Offset(0f, h),
                        end = Offset(w, h),
                        strokeWidth = 2f
                    )

                    items.forEachIndexed { i, barData ->
                        val isSelected = selectedIndex == i
                        val fraction = (barData.value / maxValue).toFloat().coerceIn(0.04f, 1f)
                        val barHeight = h * fraction
                        val centerX = (i * slotWidth) + (slotWidth / 2f)
                        val barLeft = centerX - (barWidth / 2f)
                        val barTop = h - barHeight

                        // Glow if selected
                        if (isSelected) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(barData.color.copy(alpha = 0.4f), Color.Transparent),
                                    center = Offset(centerX, barTop),
                                    radius = barWidth * 1.6f
                                )
                            )
                        }

                        // 3D Cylinder/Bar body with vertical gradient
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    barData.color,
                                    barData.secondaryColor
                                )
                            ),
                            topLeft = Offset(barLeft, barTop),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(6f, 6f)
                        )

                        // 3D Highlights on top edge
                        drawRoundRect(
                            color = Color.White.copy(alpha = if (isSelected) 0.6f else 0.3f),
                            topLeft = Offset(barLeft + 2f, barTop + 2f),
                            size = Size(barWidth - 4f, 4f),
                            cornerRadius = CornerRadius(2f, 2f)
                        )

                        // 3D Shadow depth bevel on right border
                        drawLine(
                            color = Color.Black.copy(alpha = 0.2f),
                            start = Offset(barLeft + barWidth, barTop + 4f),
                            end = Offset(barLeft + barWidth, h),
                            strokeWidth = 2.5f
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Labels and Values under each bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                items.forEachIndexed { i, barData ->
                    val isSelected = selectedIndex == i
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedIndex = if (isSelected) null else i }
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = barData.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isSelected) barData.color else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${String.format("%,.0f", barData.value)} $currencySymbol",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) barData.color else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Interactive Donut Chart for Payment Methods & Category Breakdown
 */
data class DonutSlice(
    val label: String,
    val value: Double,
    val color: Color
)

@Composable
fun InteractiveDonutChart(
    title: String,
    slices: List<DonutSlice>,
    currencySymbol: String = "ر.س",
    modifier: Modifier = Modifier
) {
    val total = remember(slices) { slices.sumOf { it.value } }
    var selectedSliceIndex by remember { mutableStateOf<Int?>(null) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Donut Canvas
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(slices, total) {
                                detectTapGestures { offset ->
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val dx = offset.x - center.x
                                    val dy = offset.y - center.y
                                    var angle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                    if (angle < 0) angle += 360f

                                    // Check which slice this angle falls into
                                    var startAngle = -90f
                                    if (startAngle < 0) startAngle += 360f

                                    var current = 0f
                                    var foundIndex: Int? = null
                                    for ((idx, slice) in slices.withIndex()) {
                                        val sweep = if (total > 0) (slice.value / total * 360f).toFloat() else 0f
                                        val end = current + sweep
                                        // Normalize angle relative to start (-90deg)
                                        val relAngle = (angle + 90f) % 360f
                                        if (relAngle >= current && relAngle < end) {
                                            foundIndex = idx
                                            break
                                        }
                                        current = end
                                    }
                                    selectedSliceIndex = if (selectedSliceIndex == foundIndex) null else foundIndex
                                }
                            }
                    ) {
                        val strokeWidth = 32.dp.toPx()
                        val diameter = min(size.width, size.height) - strokeWidth
                        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                        val arcSize = Size(diameter, diameter)

                        if (total <= 0) {
                            // Empty state ring
                            drawArc(
                                color = Color.Gray.copy(alpha = 0.2f),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth)
                            )
                        } else {
                            var currentAngle = -90f
                            slices.forEachIndexed { idx, slice ->
                                val sweepAngle = (slice.value / total * 360f).toFloat()
                                val isSelected = selectedSliceIndex == idx

                                val sliceStroke = if (isSelected) strokeWidth + 6.dp.toPx() else strokeWidth
                                drawArc(
                                    color = slice.color,
                                    startAngle = currentAngle,
                                    sweepAngle = sweepAngle - 2f, // 2deg gap for sleek look
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = sliceStroke, cap = StrokeCap.Round)
                                )
                                currentAngle += sweepAngle
                            }
                        }
                    }

                    // Center label inside donut
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (selectedSliceIndex != null && selectedSliceIndex!! in slices.indices) {
                            val sel = slices[selectedSliceIndex!!]
                            val pct = if (total > 0) (sel.value / total * 100.0) else 0.0
                            Text(
                                text = "${String.format("%.1f", pct)}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = sel.color
                            )
                            Text(
                                text = sel.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "الإجمالي",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format("%,.0f", total)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = currencySymbol,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Legend List on the side
                Column(
                    modifier = Modifier.weight(1.2f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    slices.forEachIndexed { idx, slice ->
                        val isSelected = selectedSliceIndex == idx
                        val pct = if (total > 0) (slice.value / total * 100.0) else 0.0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) slice.color.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable { selectedSliceIndex = if (isSelected) null else idx }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(slice.color, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = slice.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                            Text(
                                text = "${String.format("%,.0f", slice.value)} $currencySymbol",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) slice.color else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Modern Telegram Action Infographic Card
 */
@Composable
fun TelegramActionCard(
    onSendDailyReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(Color(0xFF229ED9), Color(0xFF0284C7)))),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF229ED9),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "تلغرام",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "تحديث وتنبيهات تلغرام اللحظية",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "أرسل ملخص المبيعات، الفواتير، والتقارير المالية مباشرة إلى قناتك أو محادثتك",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onSendDailyReport,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF229ED9)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("إرسال الآن", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
