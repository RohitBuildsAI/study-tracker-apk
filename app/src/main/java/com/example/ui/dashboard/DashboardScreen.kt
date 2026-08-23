package com.example.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.data.model.StudyTask
import com.example.data.model.TaskStatus
import com.example.data.preferences.UserSettings
import com.example.ui.components.*
import com.example.util.DateTimeUtils
import com.example.util.MotivationalQuotes

@Composable
fun DashboardScreen(
    tasks: List<StudyTask>,
    dailyGoal: DailyGoal?,
    userSettings: UserSettings,
    onAddTaskClick: () -> Unit,
    onQuickStudyClick: () -> Unit,
    onStartStudy: (StudyTask) -> Unit,
    onToggleComplete: (StudyTask) -> Unit,
    onEditTask: (StudyTask) -> Unit,
    onDuplicateTask: (StudyTask) -> Unit,
    onDeleteTask: (StudyTask) -> Unit,
    onNavigateToSchedule: () -> Unit
) {
    val greeting = remember { DateTimeUtils.getGreeting() }
    val todayDateFormatted = remember { DateTimeUtils.formatDisplayDate(DateTimeUtils.getTodayIsoString()) }
    val quote = remember { MotivationalQuotes.getDailyQuote() }

    val completedTasksCount = tasks.count { it.status == TaskStatus.COMPLETED }
    val remainingTasksCount = tasks.size - completedTasksCount

    val targetGoalMinutes = dailyGoal?.targetMinutes ?: userSettings.defaultDailyGoalMinutes
    val completedMinutes = dailyGoal?.completedMinutes ?: 0
    val remainingMinutes = maxOf(0, targetGoalMinutes - completedMinutes)
    val progressRatio = if (targetGoalMinutes > 0) {
        (completedMinutes.toFloat() / targetGoalMinutes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Greeting & Date
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = todayDateFormatted,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$greeting, Alex",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Streak Badge Pill
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("🔥", fontSize = 16.sp)
                            Text(
                                text = "${userSettings.currentStreak} Days",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // 2. Motivational Banner
        item {
            BannerStreakCard(
                currentStreak = userSettings.currentStreak,
                longestStreak = userSettings.longestStreak,
                quote = "\"${quote.first}\" — ${quote.second}"
            )
        }

        // 3. Daily Goal & Progress Hero Card (Geometric Balance Style)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("daily_goal_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    // Top Right Percentage Badge Circle
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(3.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${(progressRatio * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "DAILY PROGRESS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = DateTimeUtils.formatDurationMinutes(completedMinutes),
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontSize = 34.sp,
                                        letterSpacing = (-0.5).sp
                                    ),
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Completed of ${DateTimeUtils.formatDurationMinutes(targetGoalMinutes)} Goal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(end = 40.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    Text(
                                        text = "$completedTasksCount Completed",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.outlineVariant)
                                    )
                                    Text(
                                        text = "$remainingTasksCount Remaining",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Progress Indicator Ring
                        CircularGoalProgressIndicator(
                            progress = progressRatio,
                            completedText = DateTimeUtils.formatDurationMinutes(completedMinutes),
                            goalText = DateTimeUtils.formatDurationMinutes(targetGoalMinutes),
                            primaryColor = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.outlineVariant,
                            size = 140.dp,
                            strokeWidth = 12.dp,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }

        // 4. Quick Action Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onAddTaskClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("add_task_btn"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "+ Add Task",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onQuickStudyClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("quick_study_btn"),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(Icons.Default.FlashOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Quick Study",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // 5. Tasks Metrics Grid (Completed vs Remaining)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricStatCard(
                    title = "COMPLETED TASKS",
                    value = "$completedTasksCount",
                    subtitle = "Done today",
                    icon = Icons.Default.CheckCircle,
                    accentColor = Color(0xFF10B981),
                    modifier = Modifier.weight(1f),
                    testTag = "completed_tasks_metric"
                )

                MetricStatCard(
                    title = "REMAINING TASKS",
                    value = "$remainingTasksCount",
                    subtitle = "Scheduled",
                    icon = Icons.Default.Schedule,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    testTag = "remaining_tasks_metric"
                )
            }
        }

        // 6. Today's Schedule Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NEXT UP",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (tasks.isNotEmpty()) {
                    TextButton(onClick = onNavigateToSchedule) {
                        Text(
                            text = "View All",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 7. Today's Tasks List or Empty State
        if (tasks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("empty_tasks_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "No study tasks scheduled today",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Add your first study task to start tracking your time and achieving your goals!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = onAddTaskClick,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Create First Task")
                        }
                    }
                }
            }
        } else {
            items(tasks, key = { it.id }) { task ->
                TimelineTaskCard(
                    task = task,
                    onStartStudy = { onStartStudy(task) },
                    onToggleComplete = { onToggleComplete(task) },
                    onEdit = { onEditTask(task) },
                    onDuplicate = { onDuplicateTask(task) },
                    onDelete = { onDeleteTask(task) }
                )
            }
        }
    }
}
