package com.example.ui.records

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DailyGoal
import com.example.data.model.StudySession
import com.example.data.model.StudyTask
import com.example.data.model.TaskStatus
import com.example.data.preferences.UserSettings
import com.example.ui.components.InteractiveMonthCalendar
import com.example.ui.components.MetricStatCard
import com.example.ui.components.SubjectBadge
import com.example.ui.components.parseColorSafe
import com.example.util.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen(
    sessions: List<StudySession>,
    tasks: List<StudyTask>,
    dailyGoals: List<DailyGoal>,
    userSettings: UserSettings,
    selectedCalendarDate: String,
    onSelectCalendarDate: (String) -> Unit
) {
    val context = LocalContext.current
    var viewMode by remember { mutableIntStateOf(0) } // 0 = History Log, 1 = Calendar View
    var searchQuery by remember { mutableStateOf("") }

    // Summary calculations
    val totalStudySeconds = remember(sessions) { sessions.sumOf { it.durationSeconds } }
    val totalStudyMinutes = (totalStudySeconds + 59) / 60
    val totalSessionsCount = sessions.size

    // Best study day
    val bestDay = remember(sessions) {
        sessions.groupBy { it.date }
            .maxByOrNull { entry -> entry.value.sumOf { it.durationSeconds } }
    }
    val bestDayText = if (bestDay != null) {
        val mins = (bestDay.value.sumOf { it.durationSeconds } + 59) / 60
        "${DateTimeUtils.formatShortDate(bestDay.key)} (${DateTimeUtils.formatDurationMinutes(mins)})"
    } else "None yet"

    // Filtered sessions
    val filteredSessions = remember(sessions, searchQuery) {
        if (searchQuery.isBlank()) sessions
        else sessions.filter {
            it.taskTitle.contains(searchQuery, ignoreCase = true) ||
            it.subjectName.contains(searchQuery, ignoreCase = true) ||
            it.notes.contains(searchQuery, ignoreCase = true)
        }
    }

    // Study dates activity map
    val studyDatesActivity = remember(sessions) {
        sessions.groupBy { it.date }
            .mapValues { entry -> entry.value.sumOf { (it.durationSeconds + 59) / 60 } }
    }

    // Selected calendar date info
    val sessionsForSelectedDate = remember(sessions, selectedCalendarDate) {
        sessions.filter { it.date == selectedCalendarDate }
    }
    val tasksForSelectedDate = remember(tasks, selectedCalendarDate) {
        tasks.filter { it.date == selectedCalendarDate }
    }
    val goalForSelectedDate = remember(dailyGoals, selectedCalendarDate) {
        dailyGoals.find { it.date == selectedCalendarDate }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("records_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header & Export
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Study Records",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Comprehensive history and calendar logs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalIconButton(
                    onClick = {
                        val report = buildStudySummaryReport(sessions, tasks, userSettings)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("StudyTrack Report", report)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Study summary copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("export_records_btn")
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Export Report")
                }
            }
        }

        // View Mode Selector (History Log vs Calendar View)
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = viewMode == 0,
                    onClick = { viewMode = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("History Log")
                }
                SegmentedButton(
                    selected = viewMode == 1,
                    onClick = { viewMode = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Calendar View")
                }
            }
        }

        if (viewMode == 0) {
            // === HISTORY LOG VIEW ===
            // Stats Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricStatCard(
                        title = "Total Study Time",
                        value = DateTimeUtils.formatDurationMinutes(totalStudyMinutes),
                        subtitle = "$totalSessionsCount total sessions",
                        icon = Icons.Default.Timer,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )

                    MetricStatCard(
                        title = "Best Day",
                        value = if (bestDay != null) DateTimeUtils.formatShortDate(bestDay.key) else "--",
                        subtitle = if (bestDay != null) "${(bestDay.value.sumOf { it.durationSeconds } + 59) / 60}m studied" else "No records",
                        icon = Icons.Default.Star,
                        accentColor = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search records by task or subject...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_records_input"),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            if (filteredSessions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = if (searchQuery.isNotBlank()) "No records match '$searchQuery'" else "No study sessions recorded yet",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Start a study session to track and save your learning progress here!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredSessions, key = { it.id }) { session ->
                    val subjectColor = parseColorSafe(session.subjectColorHex)
                    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                    val timeSpan = "${timeFormat.format(Date(session.startTimeEpoch))} – ${timeFormat.format(Date(session.endTimeEpoch))}"

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("session_card_${session.id}"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SubjectBadge(
                                    subjectName = session.subjectName,
                                    colorHex = session.subjectColorHex
                                )

                                Text(
                                    text = DateTimeUtils.formatShortDate(session.date),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Text(
                                text = session.taskTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (session.notes.isNotBlank()) {
                                Text(
                                    text = "Notes: ${session.notes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = timeSpan,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = subjectColor.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = DateTimeUtils.formatDurationSeconds(session.durationSeconds),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = subjectColor,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // === CALENDAR VIEW ===
            item {
                InteractiveMonthCalendar(
                    selectedDateIso = selectedCalendarDate,
                    onDateSelected = onSelectCalendarDate,
                    studyDatesActivity = studyDatesActivity
                )
            }

            // Details for Selected Date
            item {
                val formattedSelectedDate = DateTimeUtils.formatDisplayDate(selectedCalendarDate)
                val dayStudyMins = sessionsForSelectedDate.sumOf { (it.durationSeconds + 59) / 60 }
                val targetGoalMins = goalForSelectedDate?.targetMinutes ?: userSettings.defaultDailyGoalMinutes
                val completedTasksCount = tasksForSelectedDate.count { it.status == TaskStatus.COMPLETED }
                val missedTasksCount = tasksForSelectedDate.count { it.status == TaskStatus.MISSED }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calendar_date_detail_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Activity on $formattedSelectedDate",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // 3 Key Stats
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Study Time",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = DateTimeUtils.formatDurationMinutes(dayStudyMins),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Daily Goal",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = DateTimeUtils.formatDurationMinutes(targetGoalMins),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Tasks Done",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$completedTasksCount / ${tasksForSelectedDate.size}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Sessions list on that day
                        if (sessionsForSelectedDate.isEmpty()) {
                            Text(
                                text = "No study sessions recorded on this day.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Studied Subjects:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            sessionsForSelectedDate.forEach { session ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SubjectBadge(
                                        subjectName = session.subjectName,
                                        colorHex = session.subjectColorHex
                                    )
                                    Text(
                                        text = DateTimeUtils.formatDurationSeconds(session.durationSeconds),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
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

private fun buildStudySummaryReport(
    sessions: List<StudySession>,
    tasks: List<StudyTask>,
    userSettings: UserSettings
): String {
    val totalSeconds = sessions.sumOf { it.durationSeconds }
    val totalMins = (totalSeconds + 59) / 60
    val totalHours = totalMins / 60
    val remMins = totalMins % 60
    val completedTasks = tasks.count { it.status == TaskStatus.COMPLETED }

    val subjectStats = sessions.groupBy { it.subjectName }
        .map { (name, list) ->
            val mins = list.sumOf { (it.durationSeconds + 59) / 60 }
            "$name: ${mins / 60}h ${mins % 60}m"
        }.joinToString("\n- ")

    return """
        📊 StudyTrack Productivity Report
        ---------------------------------
        🔥 Current Streak: ${userSettings.currentStreak} Days (Best: ${userSettings.longestStreak} Days)
        ⏱️ Total Study Time: ${totalHours}h ${remMins}m
        ✅ Tasks Completed: $completedTasks of ${tasks.size}
        📚 Total Sessions: ${sessions.size}
        
        Subject Breakdown:
        - ${if (subjectStats.isNotBlank()) subjectStats else "No sessions recorded"}
        
        Generated by StudyTrack App
    """.trimIndent()
}
