package com.example.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyGoal
import com.example.data.model.StudySession
import com.example.data.model.StudyTask
import com.example.data.model.Subject
import com.example.data.model.TaskStatus
import com.example.data.preferences.UserSettings
import com.example.ui.components.*
import com.example.util.DateTimeUtils

@Composable
fun AnalyticsScreen(
    subjects: List<Subject>,
    sessions: List<StudySession>,
    tasks: List<StudyTask>,
    dailyGoals: List<DailyGoal>,
    userSettings: UserSettings,
    onAddSubjectClick: () -> Unit,
    onDeleteSubject: (Subject) -> Unit
) {
    // 1. Overall Calculations
    val totalSeconds = remember(sessions) { sessions.sumOf { it.durationSeconds } }
    val totalMinutes = (totalSeconds + 59) / 60
    val totalHours = totalMinutes / 60

    // Weekly & Monthly Average (Past 7 days & 30 days)
    val past7Days = remember { DateTimeUtils.getPastNDays(7) }
    val past7DaysSessions = remember(sessions, past7Days) {
        sessions.filter { it.date in past7Days }
    }
    val past7DaysMinutes = past7DaysSessions.sumOf { (it.durationSeconds + 59) / 60 }
    val weeklyDailyAverageMinutes = past7DaysMinutes / 7

    // Bar chart data for past 7 days
    val barChartData = remember(sessions, dailyGoals, past7Days) {
        past7Days.map { dateIso ->
            val daySessions = sessions.filter { it.date == dateIso }
            val mins = daySessions.sumOf { (it.durationSeconds + 59) / 60 }
            val goal = dailyGoals.find { it.date == dateIso }?.targetMinutes ?: userSettings.defaultDailyGoalMinutes
            BarChartData(
                dateIso = dateIso,
                dayLabel = DateTimeUtils.getDayOfWeekShort(dateIso),
                studyMinutes = mins,
                goalMinutes = goal
            )
        }
    }

    // Subject breakdown distribution
    val subjectDistribution = remember(sessions, subjects, totalMinutes) {
        if (totalMinutes == 0) emptyList()
        else {
            subjects.mapNotNull { sub ->
                val subSessions = sessions.filter { it.subjectId == sub.id || it.subjectName == sub.name }
                val subMins = subSessions.sumOf { (it.durationSeconds + 59) / 60 }
                if (subMins > 0) {
                    SubjectDistributionData(
                        subjectName = sub.name,
                        colorHex = sub.colorHex,
                        totalMinutes = subMins,
                        percentage = subMins.toFloat() / totalMinutes.toFloat()
                    )
                } else null
            }.sortedByDescending { it.totalMinutes }
        }
    }

    // Most studied subject
    val mostStudiedSubject = subjectDistribution.firstOrNull()?.subjectName ?: "None yet"

    // Most productive day of the week
    val mostProductiveDay = remember(sessions) {
        if (sessions.isEmpty()) "None"
        else {
            val groupedByDay = sessions.groupBy { DateTimeUtils.getDayOfWeekShort(it.date) }
            val top = groupedByDay.maxByOrNull { entry -> entry.value.sumOf { it.durationSeconds } }
            top?.key ?: "None"
        }
    }

    // Goal completion rate
    val goalCompletionRate = remember(dailyGoals) {
        if (dailyGoals.isEmpty()) 0
        else {
            val metCount = dailyGoals.count { it.isGoalMet || it.completedMinutes >= it.targetMinutes }
            ((metCount.toFloat() / dailyGoals.size.toFloat()) * 100).toInt()
        }
    }

    val completedTasksTotal = tasks.count { it.status == TaskStatus.COMPLETED }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("analytics_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Productivity Analytics",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Track subject progress, habits, and study milestones",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Metrics Grid (2x2)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricStatCard(
                        title = "Total Study Time",
                        value = "${totalHours}h ${totalMinutes % 60}m",
                        subtitle = "${sessions.size} sessions recorded",
                        icon = Icons.Default.HourglassTop,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )

                    MetricStatCard(
                        title = "Daily Average (7d)",
                        value = DateTimeUtils.formatDurationMinutes(weeklyDailyAverageMinutes),
                        subtitle = "${past7DaysMinutes / 60}h total this week",
                        icon = Icons.Default.ShowChart,
                        accentColor = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricStatCard(
                        title = "Top Subject",
                        value = mostStudiedSubject,
                        subtitle = "Most time invested",
                        icon = Icons.Default.EmojiEvents,
                        accentColor = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f)
                    )

                    MetricStatCard(
                        title = "Goal Success Rate",
                        value = "$goalCompletionRate%",
                        subtitle = "${dailyGoals.count { it.isGoalMet }} days achieved",
                        icon = Icons.Default.CheckCircleOutline,
                        accentColor = Color(0xFF06B6D4),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Bar Chart (7 Days)
        item {
            DailyStudyBarChart(data = barChartData)
        }

        // Subject Breakdown Donut Chart
        item {
            SubjectDistributionPieCard(distribution = subjectDistribution)
        }

        // Subject Tracking Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Subject Tracking",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Button(
                    onClick = onAddSubjectClick,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_subject_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Subject", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Subject Cards List
        items(subjects, key = { it.id }) { subject ->
            val subSessions = sessions.filter { it.subjectId == subject.id || it.subjectName == subject.name }
            val subMinutes = subSessions.sumOf { (it.durationSeconds + 59) / 60 }
            val subTasks = tasks.filter { it.subjectId == subject.id || it.subjectName == subject.name }
            val subCompletedTasks = subTasks.count { it.status == TaskStatus.COMPLETED }
            val avgSessionMins = if (subSessions.isNotEmpty()) subMinutes / subSessions.size else 0

            val weeklyTargetMins = (subject.targetHoursPerWeek * 60).toInt()
            val weeklyProgress = if (weeklyTargetMins > 0) {
                (subMinutes.toFloat() / weeklyTargetMins.toFloat()).coerceIn(0f, 1f)
            } else 0f

            val subjectColor = parseColorSafe(subject.colorHex)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("subject_card_${subject.id}"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(subjectColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getSubjectIcon(subject.iconName),
                                    contentDescription = null,
                                    tint = subjectColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = subject.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$subCompletedTasks tasks completed • ${subSessions.size} sessions",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = DateTimeUtils.formatDurationMinutes(subMinutes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = subjectColor
                        )
                    }

                    // Progress towards weekly target
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Weekly Target Progress",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${DateTimeUtils.formatDurationMinutes(subMinutes)} / ${subject.targetHoursPerWeek.toInt()}h",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        SimpleHorizontalProgressBar(
                            progress = weeklyProgress,
                            color = subjectColor,
                            height = 6.dp
                        )
                    }
                }
            }
        }
    }
}
