package com.example.ui.schedule

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.StudyTask
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.ui.components.TimelineTaskCard
import com.example.util.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    tasks: List<StudyTask>,
    selectedDate: String,
    onSelectDate: (String) -> Unit,
    onAddTask: () -> Unit,
    onStartStudy: (StudyTask) -> Unit,
    onToggleComplete: (StudyTask) -> Unit,
    onEditTask: (StudyTask) -> Unit,
    onDuplicateTask: (StudyTask) -> Unit,
    onDeleteTask: (StudyTask) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredTasks = remember(tasks, selectedFilter) {
        when (selectedFilter) {
            "NOT_STARTED" -> tasks.filter { it.status == TaskStatus.NOT_STARTED }
            "IN_PROGRESS" -> tasks.filter { it.status == TaskStatus.IN_PROGRESS }
            "COMPLETED" -> tasks.filter { it.status == TaskStatus.COMPLETED }
            "HIGH_PRIORITY" -> tasks.filter { it.priority == TaskPriority.HIGH }
            else -> tasks
        }
    }

    val pastAndNextDays = remember {
        DateTimeUtils.getPastNDays(5) + listOf(
            DateTimeUtils.getTodayIsoString() // Add today & upcoming
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("schedule_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Study Schedule",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = DateTimeUtils.formatDisplayDate(selectedDate),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Quick Date Selector Strip
            item {
                val todayIso = DateTimeUtils.getTodayIsoString()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val quickDates = listOf(
                        "Today" to todayIso,
                        "Yesterday" to DateTimeUtils.getPastNDays(2).first(),
                        "Tomorrow" to {
                            val c = java.util.Calendar.getInstance()
                            c.add(java.util.Calendar.DAY_OF_YEAR, 1)
                            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(c.time)
                        }()
                    )

                    quickDates.forEach { (label, dateIso) ->
                        val isSelected = selectedDate == dateIso
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectDate(dateIso) },
                            label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                            shape = CircleShape,
                            modifier = Modifier.testTag("date_chip_$label")
                        )
                    }
                }
            }

            // Status Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf(
                        "ALL" to "All (${tasks.size})",
                        "NOT_STARTED" to "Not Started (${tasks.count { it.status == TaskStatus.NOT_STARTED }})",
                        "IN_PROGRESS" to "In Progress (${tasks.count { it.status == TaskStatus.IN_PROGRESS }})",
                        "COMPLETED" to "Completed (${tasks.count { it.status == TaskStatus.COMPLETED }})",
                        "HIGH_PRIORITY" to "🔥 High Priority (${tasks.count { it.priority == TaskPriority.HIGH }})"
                    )

                    filters.forEach { (key, label) ->
                        val isSelected = selectedFilter == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = key },
                            label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                            shape = CircleShape,
                            modifier = Modifier.testTag("filter_chip_$key")
                        )
                    }
                }
            }

            // Tasks List
            if (filteredTasks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp)
                            .testTag("schedule_empty_state"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventAvailable,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "No tasks found for this filter",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Add a study task to schedule your learning sessions.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = onAddTask,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Task")
                            }
                        }
                    }
                }
            } else {
                items(filteredTasks, key = { it.id }) { task ->
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

        // Floating Action Button (Geometric Balance Rounded FAB)
        FloatingActionButton(
            onClick = onAddTask,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 85.dp)
                .testTag("schedule_fab_add_task"),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(18.dp),
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Task", modifier = Modifier.size(28.dp))
        }
    }
}
