package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.DateTimeUtils

data class BarChartData(
    val dateIso: String,
    val dayLabel: String,
    val studyMinutes: Int,
    val goalMinutes: Int = 180,
    val sessionCount: Int = 0
)

data class SubjectDistributionData(
    val subjectName: String,
    val colorHex: String,
    val totalMinutes: Int,
    val percentage: Float,
    val sessionCount: Int = 0,
    val weeklyGoalHours: Float = 0f
)

/**
 * Interactive Bar Chart visualizing daily study progress against goals,
 * featuring interactive tooltips, goal threshold lines, and animated heights.
 */
@Composable
fun DailyStudyBarChart(
    data: List<BarChartData>,
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    goalLineColor: Color = Color(0xFFF59E0B),
    cardTitle: String = "Daily Study Progress",
    selectedRange: String = "7D",
    onRangeChange: ((String) -> Unit)? = null
) {
    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }

    val totalMinutesInPeriod = remember(data) { data.sumOf { it.studyMinutes } }
    val avgMinutesInPeriod = remember(data) {
        if (data.isNotEmpty()) totalMinutesInPeriod / data.size else 0
    }
    val goalsMetCount = remember(data) {
        data.count { it.studyMinutes >= it.goalMinutes && it.studyMinutes > 0 }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_study_bar_chart"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: Title and Time Range Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = cardTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Total: ${DateTimeUtils.formatDurationMinutes(totalMinutesInPeriod)} • Avg: ${avgMinutesInPeriod}m/day",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (onRangeChange != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        listOf("7D", "14D", "30D").forEach { range ->
                            val isSelected = selectedRange == range
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                modifier = Modifier
                                    .clickable { onRangeChange(range) }
                                    .testTag("chart_range_$range")
                            ) {
                                Text(
                                    text = range,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Chart Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(primaryColor)
                    )
                    Text(
                        text = "Studied",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF10B981))
                    )
                    Text(
                        text = "Goal Achieved ($goalsMetCount/${data.size}d)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(2.dp)
                            .background(goalLineColor)
                    )
                    Text(
                        text = "Daily Goal Line",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val maxMinutes = maxOf(240, data.maxOfOrNull { maxOf(it.studyMinutes, it.goalMinutes) } ?: 180)

            // Interactive Tooltip Banner if a bar is selected
            selectedBarIndex?.let { idx ->
                val selectedItem = data.getOrNull(idx)
                if (selectedItem != null) {
                    val isGoalMet = selectedItem.studyMinutes >= selectedItem.goalMinutes && selectedItem.studyMinutes > 0
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${selectedItem.dayLabel} (${selectedItem.dateIso})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Target: ${DateTimeUtils.formatDurationMinutes(selectedItem.goalMinutes)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = DateTimeUtils.formatDurationMinutes(selectedItem.studyMinutes),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isGoalMet) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                                )
                                if (isGoalMet) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF10B981).copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "🎯 Goal Met",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF047857),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val activePrimaryColor = primaryColor
            val selectionHighlightColor = MaterialTheme.colorScheme.primary

            // Main Bar Chart Canvas Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(data) {
                            detectTapGestures { offset ->
                                val barCount = data.size
                                if (barCount > 0) {
                                    val barSpacing = size.width / barCount
                                    val clickedIndex = (offset.x / barSpacing).toInt().coerceIn(0, barCount - 1)
                                    selectedBarIndex = if (selectedBarIndex == clickedIndex) null else clickedIndex
                                }
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height - 24.dp.toPx()
                    val barCount = data.size
                    if (barCount == 0) return@Canvas

                    val barSpacing = width / barCount
                    val barWidth = (barSpacing * 0.52f).coerceAtMost(36.dp.toPx())

                    // 1. Draw Grid Lines (Y-Axis levels)
                    val gridSteps = 4
                    for (i in 0..gridSteps) {
                        val y = height * (1f - (i.toFloat() / gridSteps))
                        drawLine(
                            color = Color(0xFFE2E8F0).copy(alpha = 0.5f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // 2. Draw Daily Goal Target Line across
                    val avgGoal = if (data.isNotEmpty()) data.map { it.goalMinutes }.average().toFloat() else 180f
                    val goalY = height * (1f - (avgGoal / maxMinutes.toFloat()).coerceIn(0f, 1f))
                    drawLine(
                        color = goalLineColor.copy(alpha = 0.75f),
                        start = Offset(0f, goalY),
                        end = Offset(width, goalY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )

                    // 3. Draw Bars with gradients & goal highlighting
                    data.forEachIndexed { index, item ->
                        val x = index * barSpacing + (barSpacing - barWidth) / 2
                        val isSelected = selectedBarIndex == index
                        val barHeightRatio = (item.studyMinutes.toFloat() / maxMinutes.toFloat()).coerceIn(0.015f, 1f)
                        val barHeight = height * barHeightRatio
                        val y = height - barHeight

                        val isGoalMet = item.studyMinutes >= item.goalMinutes && item.studyMinutes > 0
                        val barGradient = if (isGoalMet) {
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF34D399), Color(0xFF059669))
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(
                                    activePrimaryColor.copy(alpha = if (isSelected) 1f else 0.85f),
                                    activePrimaryColor.copy(alpha = if (isSelected) 0.85f else 0.65f)
                                )
                            )
                        }

                        // Background pillar slot
                        drawRoundRect(
                            color = Color(0xFFF1F5F9).copy(alpha = 0.6f),
                            topLeft = Offset(x, 0f),
                            size = Size(barWidth, height),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )

                        // Studied value bar
                        drawRoundRect(
                            brush = barGradient,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )

                        // Selection highlight outline
                        if (isSelected) {
                            drawRoundRect(
                                color = selectionHighlightColor,
                                topLeft = Offset(x - 2.dp.toPx(), y - 2.dp.toPx()),
                                size = Size(barWidth + 4.dp.toPx(), barHeight + 4.dp.toPx()),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }

                // X-axis Day Labels below
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    data.forEachIndexed { index, item ->
                        val isSelected = selectedBarIndex == index
                        Text(
                            text = item.dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "💡 Tap any bar to view exact hours, target goals, and breakdown",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Dedicated Horizontal / Ranked Bar Chart visualizing Time Spent per Subject.
 * Provides clear comparisons of hours studied, percentage of total time, and weekly target progress.
 */
@Composable
fun SubjectTimeSpentBarChart(
    distribution: List<SubjectDistributionData>,
    modifier: Modifier = Modifier,
    cardTitle: String = "Time Spent per Subject"
) {
    var selectedSubjectName by remember { mutableStateOf<String?>(null) }
    var sortBy by remember { mutableStateOf("TIME") } // "TIME" or "NAME"

    val sortedDistribution = remember(distribution, sortBy) {
        when (sortBy) {
            "NAME" -> distribution.sortedBy { it.subjectName }
            else -> distribution.sortedByDescending { it.totalMinutes }
        }
    }

    val maxSubjectMinutes = remember(distribution) {
        maxOf(60, distribution.maxOfOrNull { it.totalMinutes } ?: 60)
    }

    val totalTimeMinutes = remember(distribution) { distribution.sumOf { it.totalMinutes } }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("subject_time_bar_chart"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = cardTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${distribution.size} subjects active • ${DateTimeUtils.formatDurationMinutes(totalTimeMinutes)} total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Sort toggle
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.clickable {
                        sortBy = if (sortBy == "TIME") "NAME" else "TIME"
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (sortBy == "TIME") "Top First" else "A-Z",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (distribution.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No subject study sessions recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Stacked / Horizontal Bar Visualizers per subject
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    sortedDistribution.forEach { item ->
                        val isSelected = selectedSubjectName == item.subjectName
                        val subjectColor = parseColorSafe(item.colorHex)
                        val barRatio = (item.totalMinutes.toFloat() / maxSubjectMinutes.toFloat()).coerceIn(0.04f, 1f)
                        val animatedRatio by animateFloatAsState(
                            targetValue = barRatio,
                            animationSpec = tween(durationMillis = 600),
                            label = "barRatio_${item.subjectName}"
                        )

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) subjectColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) subjectColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSubjectName = if (isSelected) null else item.subjectName
                                }
                                .testTag("subject_bar_item_${item.subjectName}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Subject label, percentage, and time spent
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(subjectColor)
                                        )
                                        Text(
                                            text = item.subjectName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Surface(
                                            shape = CircleShape,
                                            color = subjectColor.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "${(item.percentage * 100).toInt()}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = subjectColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = DateTimeUtils.formatDurationMinutes(item.totalMinutes),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = subjectColor
                                    )
                                }

                                // Horizontal Bar Track
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(Color(0xFFE2E8F0).copy(alpha = 0.5f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(animatedRatio)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(
                                                        subjectColor.copy(alpha = 0.75f),
                                                        subjectColor
                                                    )
                                                )
                                            )
                                    )
                                }

                                // Expanded Extra Details when tapped
                                AnimatedVisibility(visible = isSelected) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (item.sessionCount > 0) "${item.sessionCount} sessions completed" else "Active subject",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (item.weeklyGoalHours > 0) {
                                            Text(
                                                text = "Weekly Goal: ${item.weeklyGoalHours.toInt()}h",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
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
}

@Composable
fun SubjectDistributionPieCard(
    distribution: List<SubjectDistributionData>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("subject_distribution_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = "Subject Study Share (Donut)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (distribution.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No study sessions recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Donut Chart
                    Box(
                        modifier = Modifier.size(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidthPx = 18.dp.toPx()
                            val arcSize = size.minDimension - strokeWidthPx
                            val topLeft = strokeWidthPx / 2
                            var startAngle = -90f

                            distribution.forEach { item ->
                                val sweep = item.percentage * 360f
                                val color = parseColorSafe(item.colorHex)
                                if (sweep > 0) {
                                    drawArc(
                                        color = color,
                                        startAngle = startAngle,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        topLeft = Offset(topLeft, topLeft),
                                        size = Size(arcSize, arcSize),
                                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Butt)
                                    )
                                    startAngle += sweep
                                }
                            }
                        }
                    }

                    // Legend list
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        distribution.take(5).forEach { item ->
                            val color = parseColorSafe(item.colorHex)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Text(
                                        text = item.subjectName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }

                                Text(
                                    text = DateTimeUtils.formatDurationMinutes(item.totalMinutes),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
