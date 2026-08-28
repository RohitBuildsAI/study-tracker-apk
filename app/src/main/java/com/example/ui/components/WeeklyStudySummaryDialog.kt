package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DailyGoal
import com.example.data.model.StudySession
import com.example.data.model.StudyTask
import com.example.data.model.Subject
import com.example.data.preferences.UserSettings
import com.example.util.DateTimeUtils
import com.example.util.WeeklyStudySummary
import com.example.util.WeeklySummaryGenerator
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyStudySummaryDialog(
    sessions: List<StudySession>,
    tasks: List<StudyTask>,
    dailyGoals: List<DailyGoal>,
    subjects: List<Subject>,
    userSettings: UserSettings,
    initialWeekOffset: Int = 0,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var weekOffset by remember { mutableIntStateOf(initialWeekOffset) }
    var activeViewMode by remember { mutableIntStateOf(0) } // 0: Formatted Text Summary, 1: Visual Cards

    val weeklySummary = remember(weekOffset, sessions, tasks, dailyGoals, subjects, userSettings) {
        WeeklySummaryGenerator.generateWeeklySummary(
            weekOffset = weekOffset,
            sessions = sessions,
            tasks = tasks,
            dailyGoals = dailyGoals,
            subjects = subjects,
            userSettings = userSettings
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .testTag("weekly_study_summary_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row
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
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Summarize,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Weekly Study Summary",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = weeklySummary.weekDateRangeLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_weekly_summary_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Week Offset Navigation (Previous Week / Next Week)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalIconButton(
                            onClick = { weekOffset -= 1 },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("prev_week_summary_btn")
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Week", modifier = Modifier.size(20.dp))
                        }

                        Text(
                            text = when (weekOffset) {
                                0 -> "This Week"
                                -1 -> "Last Week"
                                1 -> "Next Week"
                                else -> "${if (weekOffset < 0) "${-weekOffset} weeks ago" else "In $weekOffset weeks"}"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        FilledTonalIconButton(
                            onClick = { weekOffset += 1 },
                            enabled = weekOffset < 4, // Limit future weeks
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("next_week_summary_btn")
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Week", modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // Key Stat Badges Row (4 Key Highlights)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WeeklySummaryMiniBadge(
                        title = "TOTAL TIME",
                        value = DateTimeUtils.formatDurationMinutes(weeklySummary.totalStudyMinutes),
                        subtitle = "${weeklySummary.totalSessionsCount} sessions",
                        icon = Icons.Default.Timer,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )

                    WeeklySummaryMiniBadge(
                        title = "GOAL RATE",
                        value = "${String.format(Locale.getDefault(), "%.0f", weeklySummary.goalCompletionRatePercent)}%",
                        subtitle = "${weeklySummary.daysGoalMetCount}/${weeklySummary.totalDaysInPeriod} days",
                        icon = Icons.Default.TrackChanges,
                        accentColor = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )

                    WeeklySummaryMiniBadge(
                        title = "TASKS DONE",
                        value = "${weeklySummary.tasksCompletedCount}/${weeklySummary.tasksScheduledCount}",
                        subtitle = "${String.format(Locale.getDefault(), "%.0f", weeklySummary.taskCompletionRatePercent)}%",
                        icon = Icons.Default.CheckCircleOutline,
                        accentColor = Color(0xFF06B6D4),
                        modifier = Modifier.weight(1f)
                    )
                }

                // View Mode Tabs: Text Summary vs Visual Breakdown
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = activeViewMode == 0,
                        onClick = { activeViewMode = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Text Summary", style = MaterialTheme.typography.labelMedium)
                    }
                    SegmentedButton(
                        selected = activeViewMode == 1,
                        onClick = { activeViewMode = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Icon(Icons.Default.ViewAgenda, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Visual Cards", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Main Content Area (Scrollable)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (activeViewMode == 0) {
                        // === TEXT-BASED SUMMARY VIEW (Selectable, Monospace, Clean Box) ===
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Terminal,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Generated Report (Formatted Text)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = "Tap & select to copy parts",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                SelectionContainer {
                                    Text(
                                        text = weeklySummary.summaryText,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .testTag("weekly_summary_text_content")
                                            .padding(bottom = 8.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // === VISUAL CARDS BREAKDOWN VIEW ===
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 1. Goal Progress Overview Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Daily Goal Completion Rate",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${String.format(Locale.getDefault(), "%.1f", weeklySummary.goalCompletionRatePercent)}%",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF10B981)
                                        )
                                    }

                                    LinearProgressIndicator(
                                        progress = { (weeklySummary.goalCompletionRatePercent / 100f).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(CircleShape),
                                        color = Color(0xFF10B981),
                                        trackColor = MaterialTheme.colorScheme.outlineVariant
                                    )

                                    Text(
                                        text = "${weeklySummary.daysGoalMetCount} out of ${weeklySummary.totalDaysInPeriod} days met the daily study target (${DateTimeUtils.formatDurationMinutes(weeklySummary.totalStudyMinutes)} studied vs ${DateTimeUtils.formatDurationMinutes(weeklySummary.totalTargetMinutes)} planned).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // 2. Day by Day Breakdown
                            Text(
                                text = "Daily Performance",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            weeklySummary.dailyBreakdowns.forEach { day ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (day.isGoalMet) Color(0xFF10B981).copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (day.isGoalMet) Color(0xFF10B981).copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "${day.dayName.take(3)}, ${DateTimeUtils.formatShortDate(day.dateIso)}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (day.isGoalMet) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = Color(0xFF10B981)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "Goal Met",
                                                            tint = Color.White,
                                                            modifier = Modifier
                                                                .size(14.dp)
                                                                .padding(2.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Text(
                                                text = "Goal: ${DateTimeUtils.formatDurationMinutes(day.targetMinutes)} • ${day.sessionCount} sessions${if (day.totalTasks > 0) " • ${day.completedTasks}/${day.totalTasks} tasks" else ""}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Text(
                                            text = DateTimeUtils.formatDurationMinutes(day.studiedMinutes),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (day.isGoalMet) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            // 3. Subject Progress Distribution
                            if (weeklySummary.subjectBreakdowns.isNotEmpty()) {
                                Text(
                                    text = "Subjects Studied This Week",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                weeklySummary.subjectBreakdowns.forEach { sub ->
                                    val subColor = parseColorSafe(sub.colorHex)
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .clip(CircleShape)
                                                            .background(subColor)
                                                    )
                                                    Text(
                                                        text = sub.subjectName,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                Text(
                                                    text = "${DateTimeUtils.formatDurationMinutes(sub.studiedMinutes)} (${String.format(Locale.getDefault(), "%.1f%%", sub.percentageOfTotal)})",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = subColor
                                                )
                                            }

                                            if (sub.targetMinutes > 0) {
                                                val progress = (sub.studiedMinutes.toFloat() / sub.targetMinutes.toFloat()).coerceIn(0f, 1f)
                                                LinearProgressIndicator(
                                                    progress = { progress },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(5.dp)
                                                        .clip(CircleShape),
                                                    color = subColor,
                                                    trackColor = MaterialTheme.colorScheme.outlineVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Action Buttons (Copy Summary, Share Summary, Close)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, weeklySummary.summaryText)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Share Weekly Study Summary")
                            context.startActivity(shareIntent)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("share_weekly_summary_btn"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("StudyTrack Weekly Summary", weeklySummary.summaryText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Weekly study summary copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("copy_weekly_summary_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Summary", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklySummaryMiniBadge(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
