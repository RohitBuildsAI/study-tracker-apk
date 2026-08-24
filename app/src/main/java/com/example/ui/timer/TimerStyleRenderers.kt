package com.example.ui.timer

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TimerStyle
import com.example.util.DateTimeUtils
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CountdownTimerStyleDisplay(
    style: TimerStyle,
    displaySeconds: Long,
    elapsedSeconds: Long,
    targetSeconds: Long,
    progress: Float,
    isPaused: Boolean,
    subjectColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("countdown_display_${style.name.lowercase()}"),
        contentAlignment = Alignment.Center
    ) {
        when (style) {
            TimerStyle.FlipClock -> FlipClockCountdown(
                displaySeconds = displaySeconds,
                isPaused = isPaused,
                subjectColor = subjectColor
            )
            TimerStyle.RetroSplit -> RetroSplitCountdown(
                displaySeconds = displaySeconds,
                progress = progress,
                isPaused = isPaused,
                subjectColor = subjectColor
            )
            TimerStyle.VintageTick -> VintageTickCountdown(
                displaySeconds = displaySeconds,
                elapsedSeconds = elapsedSeconds,
                isPaused = isPaused,
                subjectColor = subjectColor
            )
            TimerStyle.ChronosAnalog -> ChronosAnalogCountdown(
                displaySeconds = displaySeconds,
                progress = progress,
                isPaused = isPaused,
                subjectColor = subjectColor
            )
            TimerStyle.GhostOutline -> GhostOutlineCountdown(
                displaySeconds = displaySeconds,
                progress = progress,
                isPaused = isPaused,
                subjectColor = subjectColor
            )
            TimerStyle.CleanDigital -> CleanDigitalCountdown(
                displaySeconds = displaySeconds,
                progress = progress,
                isPaused = isPaused,
                subjectColor = subjectColor
            )
        }
    }
}

// -------------------------------------------------------------------------
// 1. FLIP CLOCK
// -------------------------------------------------------------------------
@Composable
fun FlipClockCountdown(
    displaySeconds: Long,
    isPaused: Boolean,
    subjectColor: Color
) {
    val mins = (displaySeconds / 60).coerceAtLeast(0)
    val secs = (displaySeconds % 60).coerceAtLeast(0)

    val min1 = (mins / 10).toInt()
    val min2 = (mins % 10).toInt()
    val sec1 = (secs / 10).toInt()
    val sec2 = (secs % 10).toInt()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Minutes Group
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FlipDigitCard(digit = min1.toString(), isPaused = isPaused, subjectColor = subjectColor)
                FlipDigitCard(digit = min2.toString(), isPaused = isPaused, subjectColor = subjectColor)
            }

            // Colon Divider with mechanical dots
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isPaused) Color(0xFFF59E0B) else subjectColor)
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isPaused) Color(0xFFF59E0B) else subjectColor)
                )
            }

            // Seconds Group
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FlipDigitCard(digit = sec1.toString(), isPaused = isPaused, subjectColor = subjectColor)
                FlipDigitCard(digit = sec2.toString(), isPaused = isPaused, subjectColor = subjectColor)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF1E293B).copy(alpha = 0.85f),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Text(
                text = if (isPaused) "FLIP PAUSED" else "MECHANICAL FLIP",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isPaused) Color(0xFFFBBF24) else Color(0xFF94A3B8),
                letterSpacing = 2.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun FlipDigitCard(
    digit: String,
    isPaused: Boolean,
    subjectColor: Color,
    cardWidth: Dp = 56.dp,
    cardHeight: Dp = 82.dp
) {
    Box(
        modifier = Modifier
            .size(width = cardWidth, height = cardHeight)
            .shadow(6.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF18181B))
            .border(1.dp, Color(0xFF27272A), RoundedCornerShape(10.dp))
    ) {
        // Upper card half
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF27272A), Color(0xFF18181B))
                    )
                )
        )

        // Lower card half
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF141417), Color(0xFF09090B))
                    )
                )
        )

        // Digit Text
        Text(
            text = digit,
            style = MaterialTheme.typography.displayMedium.copy(
                fontSize = 44.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp
            ),
            color = if (isPaused) Color(0xFFF59E0B) else Color(0xFFF4F4F5),
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center)
        )

        // Center split horizontal seam
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.Center)
                .background(Color(0xFF09090B))
        )

        // Left hinge peg
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 6.dp)
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                .background(Color(0xFF71717A))
        )

        // Right hinge peg
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 6.dp)
                .align(Alignment.CenterEnd)
                .clip(RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp))
                .background(Color(0xFF71717A))
        )
    }
}

// -------------------------------------------------------------------------
// 2. RETRO SPLIT (80s Segmented Neon Digital)
// -------------------------------------------------------------------------
@Composable
fun RetroSplitCountdown(
    displaySeconds: Long,
    progress: Float,
    isPaused: Boolean,
    subjectColor: Color
) {
    val mins = (displaySeconds / 60).coerceAtLeast(0)
    val secs = (displaySeconds % 60).coerceAtLeast(0)
    val timeFormatted = String.format("%02d:%02d", mins, secs)

    val neonColor = if (isPaused) Color(0xFFF59E0B) else Color(0xFF06B6D4)
    val phosphorColor = if (isPaused) Color(0xFFFEF3C7) else Color(0xFFE0F2FE)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(2.dp, Color(0xFF1E293B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier.padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Retro Header Label with LED Indicator
            Row(
                modifier = Modifier.fillMaxWidth(0.9f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VFD // RETRO-SPLIT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = Color(0xFF64748B)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isPaused) Color(0xFFEF4444) else Color(0xFF10B981))
                    )
                    Text(
                        text = if (isPaused) "HOLD" else "ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isPaused) Color(0xFFEF4444) else Color(0xFF10B981)
                    )
                }
            }

            // Segmented Main Readout Display Frame
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF020617))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                // Dim Ghost 88:88 background for authentic VFD effect
                Text(
                    text = "88:88",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 52.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp
                    ),
                    color = Color(0xFF1E293B).copy(alpha = 0.45f)
                )

                // Active glowing digits
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 52.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp,
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = neonColor,
                            blurRadius = 14f
                        )
                    ),
                    color = phosphorColor
                )
            }

            // Segmented Progress Bar (12 segmented blocks)
            Row(
                modifier = Modifier.fillMaxWidth(0.9f),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                val totalBlocks = 16
                val activeBlocks = (progress * totalBlocks).toInt().coerceIn(0, totalBlocks)

                for (i in 0 until totalBlocks) {
                    val isActive = i < activeBlocks
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(
                                if (isActive) neonColor else Color(0xFF1E293B)
                            )
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// 3. VINTAGE TICK (Classic Pocket Watch / Grandfather Clock)
// -------------------------------------------------------------------------
@Composable
fun VintageTickCountdown(
    displaySeconds: Long,
    elapsedSeconds: Long,
    isPaused: Boolean,
    subjectColor: Color
) {
    val mins = (displaySeconds / 60).coerceAtLeast(0)
    val secs = (displaySeconds % 60).coerceAtLeast(0)
    val timeFormatted = String.format("%02d:%02d", mins, secs)

    // Tick rotation based on current second
    val tickAngle = (secs * 6f) // 360 / 60 = 6 deg per sec

    Box(
        modifier = Modifier.size(260.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = (size.minDimension / 2) - 8.dp.toPx()

            // Outer Antique Brass Bezel
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFD97706), Color(0xFF78350F)),
                    center = center,
                    radius = radius + 8.dp.toPx()
                ),
                radius = radius + 6.dp.toPx(),
                center = center,
                style = Stroke(width = 6.dp.toPx())
            )

            // Inner Parchment Dial Surface
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A)),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            // Minute / Second Tick marks around dial
            for (i in 0 until 60) {
                val isMajor = i % 5 == 0
                val angle = (i * 6f) * (PI.toFloat() / 180f)
                val tickLength = if (isMajor) 14.dp.toPx() else 6.dp.toPx()
                val tickWidth = if (isMajor) 2.5.dp.toPx() else 1.dp.toPx()
                val tickColor = if (isMajor) Color(0xFF451A03) else Color(0xFF92400E)

                val startX = center.x + (radius - tickLength) * sin(angle)
                val startY = center.y - (radius - tickLength) * cos(angle)
                val endX = center.x + (radius - 2.dp.toPx()) * sin(angle)
                val endY = center.y - (radius - 2.dp.toPx()) * cos(angle)

                drawLine(
                    color = tickColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = tickWidth,
                    cap = StrokeCap.Round
                )
            }

            // Antique Ticking Hand
            rotate(degrees = tickAngle, pivot = center) {
                // Counterbalance
                drawLine(
                    color = Color(0xFF78350F),
                    start = center,
                    end = Offset(center.x, center.y + 16.dp.toPx()),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Second hand
                drawLine(
                    color = if (isPaused) Color(0xFFDC2626) else Color(0xFFB45309),
                    start = center,
                    end = Offset(center.x, center.y - radius + 18.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Center Brass Nut
            drawCircle(
                color = Color(0xFF451A03),
                radius = 6.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color(0xFFFBBF24),
                radius = 3.dp.toPx(),
                center = center
            )
        }

        // Centered Antique Time Readout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 40.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF451A03).copy(alpha = 0.9f)
            ) {
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 24.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = Color(0xFFFEF3C7),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = if (isPaused) "PAUSED" else "VINTAGE TICK",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF78350F),
                letterSpacing = 1.sp
            )
        }
    }
}

// -------------------------------------------------------------------------
// 4. CHRONOS ANALOG (Precision Chronograph Watch Face)
// -------------------------------------------------------------------------
@Composable
fun ChronosAnalogCountdown(
    displaySeconds: Long,
    progress: Float,
    isPaused: Boolean,
    subjectColor: Color
) {
    val mins = (displaySeconds / 60).coerceAtLeast(0)
    val secs = (displaySeconds % 60).coerceAtLeast(0)
    val timeFormatted = String.format("%02d:%02d", mins, secs)

    // Hand rotations
    val secondAngle = (secs * 6f)
    val minuteAngle = ((mins % 60) * 6f) + (secs * 0.1f)

    Box(
        modifier = Modifier.size(260.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = (size.minDimension / 2) - 8.dp.toPx()

            // Outer dark bezel with carbon texture
            drawCircle(
                color = Color(0xFF0F172A),
                radius = radius + 6.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color(0xFF1E293B),
                radius = radius + 6.dp.toPx(),
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )

            // Outer dial track
            drawCircle(
                color = Color(0xFF020617),
                radius = radius,
                center = center
            )

            // Progress Arc Outer Ring
            val sweep = (progress * 360f).coerceIn(0f, 360f)
            if (sweep > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to Color(0xFF38BDF8),
                        0.5f to subjectColor,
                        1.0f to Color(0xFF818CF8)
                    ),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Chrono Indexes (12 hour markers + minute ticks)
            for (i in 0 until 60) {
                val isHour = i % 5 == 0
                val angle = (i * 6f) * (PI.toFloat() / 180f)
                val len = if (isHour) 12.dp.toPx() else 5.dp.toPx()
                val w = if (isHour) 3.dp.toPx() else 1.dp.toPx()
                val col = if (isHour) Color(0xFFF1F5F9) else Color(0xFF475569)

                val startX = center.x + (radius - len - 6.dp.toPx()) * sin(angle)
                val startY = center.y - (radius - len - 6.dp.toPx()) * cos(angle)
                val endX = center.x + (radius - 6.dp.toPx()) * sin(angle)
                val endY = center.y - (radius - 6.dp.toPx()) * cos(angle)

                drawLine(
                    color = col,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = w,
                    cap = StrokeCap.Square
                )
            }

            // Chrono Minute Hand
            rotate(degrees = minuteAngle, pivot = center) {
                drawLine(
                    color = Color(0xFFE2E8F0),
                    start = center,
                    end = Offset(center.x, center.y - radius * 0.65f),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Chrono Second Hand (Hi-vis accent)
            rotate(degrees = secondAngle, pivot = center) {
                drawLine(
                    color = if (isPaused) Color(0xFFF59E0B) else Color(0xFFEF4444),
                    start = Offset(center.x, center.y + 20.dp.toPx()),
                    end = Offset(center.x, center.y - radius * 0.85f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = if (isPaused) Color(0xFFF59E0B) else Color(0xFFEF4444),
                    radius = 3.5.dp.toPx(),
                    center = Offset(center.x, center.y - radius * 0.65f)
                )
            }

            // Center Pin Hub
            drawCircle(
                color = Color(0xFFF8FAFC),
                radius = 5.dp.toPx(),
                center = center
            )
        }

        // Digital Lap / Time Overlay at lower sub-dial
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 65.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.9f),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = Color(0xFF38BDF8),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Text(
                text = "CHRONOS",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = Color(0xFF94A3B8),
                letterSpacing = 1.sp
            )
        }
    }
}

// -------------------------------------------------------------------------
// 5. GHOST OUTLINE (Futuristic Cyber Wireframe Glow)
// -------------------------------------------------------------------------
@Composable
fun GhostOutlineCountdown(
    displaySeconds: Long,
    progress: Float,
    isPaused: Boolean,
    subjectColor: Color
) {
    val mins = (displaySeconds / 60).coerceAtLeast(0)
    val secs = (displaySeconds % 60).coerceAtLeast(0)
    val timeFormatted = String.format("%02d:%02d", mins, secs)

    val neonAccent = if (isPaused) Color(0xFFF59E0B) else Color(0xFF8B5CF6)
    val cyanGlow = if (isPaused) Color(0xFFFBBF24) else Color(0xFF06B6D4)

    val infiniteTransition = rememberInfiniteTransition(label = "ghost_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier.size(260.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = (size.minDimension / 2) - 12.dp.toPx()

            // Outer pulsing dashed wireframe ring
            rotate(degrees = rotationAngle, pivot = center) {
                drawCircle(
                    color = neonAccent.copy(alpha = 0.35f),
                    radius = radius + 6.dp.toPx(),
                    center = center,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                    )
                )
            }

            // Inner hairline outline ring
            drawCircle(
                color = Color(0xFF334155).copy(alpha = 0.5f),
                radius = radius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // Progress Arc with glowing gradient
            val sweep = (progress * 360f).coerceIn(0f, 360f)
            if (sweep > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to neonAccent,
                        0.7f to cyanGlow,
                        1.0f to neonAccent
                    ),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // Center Ghost Hologram Readout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = timeFormatted,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 46.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = neonAccent,
                        blurRadius = 16f
                    )
                ),
                color = if (isPaused) Color(0xFFFBBF24) else Color(0xFFF8FAFC)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = CircleShape,
                color = neonAccent.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, neonAccent.copy(alpha = 0.4f))
            ) {
                Text(
                    text = if (isPaused) "PAUSED" else "GHOST OUTLINE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isPaused) Color(0xFFFBBF24) else cyanGlow,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// 6. CLEAN DIGITAL (Modern Material 3 Minimalist)
// -------------------------------------------------------------------------
@Composable
fun CleanDigitalCountdown(
    displaySeconds: Long,
    progress: Float,
    isPaused: Boolean,
    subjectColor: Color
) {
    val mins = (displaySeconds / 60).coerceAtLeast(0)
    val secs = (displaySeconds % 60).coerceAtLeast(0)
    val timeFormatted = String.format("%02d:%02d", mins, secs)

    Box(
        modifier = Modifier.size(260.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 18.dp.toPx()
            val arcSize = size.minDimension - strokePx
            val topLeft = strokePx / 2

            // Track
            drawArc(
                color = Color.LightGray.copy(alpha = 0.2f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(topLeft, topLeft),
                size = Size(arcSize, arcSize),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Progress Arc
            val progressSweep = (progress * 360f).coerceIn(0f, 360f)
            if (progressSweep > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to subjectColor,
                        0.7f to Color(0xFF06B6D4),
                        1.0f to subjectColor
                    ),
                    startAngle = -90f,
                    sweepAngle = progressSweep,
                    useCenter = false,
                    topLeft = Offset(topLeft, topLeft),
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = timeFormatted,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 44.sp,
                    letterSpacing = 1.sp
                ),
                fontWeight = FontWeight.ExtraBold,
                color = if (isPaused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = CircleShape,
                color = if (isPaused) Color(0xFFF59E0B).copy(alpha = 0.15f) else subjectColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = if (isPaused) "PAUSED" else "CLEAN DIGITAL",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isPaused) Color(0xFFF59E0B) else subjectColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }
    }
}
