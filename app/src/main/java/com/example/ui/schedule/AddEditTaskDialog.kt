package com.example.ui.schedule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.StudyTask
import com.example.data.model.Subject
import com.example.data.model.TaskPriority
import com.example.ui.components.SubjectSelectableChip
import com.example.util.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskDialog(
    taskToEdit: StudyTask?,
    subjects: List<Subject>,
    initialDate: String,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        subject: Subject,
        description: String,
        date: String,
        startTime: String,
        endTime: String,
        targetDurationMinutes: Int,
        priority: TaskPriority,
        reminderEnabled: Boolean,
        reminderMinutesBefore: Int,
        isPomodoro: Boolean
    ) -> Unit,
    onOpenAddSubject: () -> Unit
) {
    var title by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var selectedSubject by remember {
        mutableStateOf(
            subjects.find { it.id == taskToEdit?.subjectId } ?: subjects.firstOrNull()
        )
    }
    var description by remember { mutableStateOf(taskToEdit?.description ?: "") }
    var taskDate by remember { mutableStateOf(taskToEdit?.date ?: initialDate) }
    var startTime by remember { mutableStateOf(taskToEdit?.startTime ?: "") }
    var endTime by remember { mutableStateOf(taskToEdit?.endTime ?: "") }
    var targetDurationMinutes by remember { mutableIntStateOf(taskToEdit?.targetDurationMinutes ?: 60) }
    var selectedPriority by remember { mutableStateOf(taskToEdit?.priority ?: TaskPriority.MEDIUM) }
    var reminderEnabled by remember { mutableStateOf(taskToEdit?.reminderEnabled ?: false) }
    var reminderMinutesBefore by remember { mutableIntStateOf(taskToEdit?.reminderMinutesBefore ?: 10) }
    var isPomodoro by remember { mutableStateOf(taskToEdit?.isPomodoro ?: false) }

    var titleError by remember { mutableStateOf(false) }

    // Preset duration buttons
    val durationPresets = listOf(15, 30, 45, 60, 90, 120)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .testTag("add_edit_task_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Dialog Title Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (taskToEdit == null) "Create Study Task" else "Edit Task",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_task_dialog_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Scrollable Form Fields
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Task Name
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (it.isNotBlank()) titleError = false
                        },
                        label = { Text("Task Name *") },
                        placeholder = { Text("e.g. Quadratic Equations, Chapter 4") },
                        isError = titleError,
                        supportingText = if (titleError) {
                            { Text("Task name is required", color = MaterialTheme.colorScheme.error) }
                        } else null,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("task_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Subject Selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Subject / Category *",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            TextButton(
                                onClick = onOpenAddSubject,
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("+ New Subject", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            subjects.forEach { subject ->
                                SubjectSelectableChip(
                                    subject = subject,
                                    isSelected = selectedSubject?.id == subject.id,
                                    onSelect = { selectedSubject = subject }
                                )
                            }
                        }
                    }

                    // Target Duration & Presets
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Target Duration",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = DateTimeUtils.formatDurationMinutes(targetDurationMinutes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            durationPresets.forEach { mins ->
                                FilterChip(
                                    selected = targetDurationMinutes == mins,
                                    onClick = { targetDurationMinutes = mins },
                                    label = { Text("${mins}m") },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    // Start Time & End Time (Optional)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            label = { Text("Start Time") },
                            placeholder = { Text("e.g. 6:00 PM") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("start_time_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = endTime,
                            onValueChange = { endTime = it },
                            label = { Text("End Time") },
                            placeholder = { Text("e.g. 7:00 PM") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("end_time_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Priority Selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Priority",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TaskPriority.values().forEach { priority ->
                                val isSelected = selectedPriority == priority
                                val priorityColor = when (priority) {
                                    TaskPriority.HIGH -> Color(0xFFEF4444)
                                    TaskPriority.MEDIUM -> Color(0xFFF59E0B)
                                    TaskPriority.LOW -> Color(0xFF10B981)
                                }

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedPriority = priority }
                                        .testTag("priority_${priority.name.lowercase()}"),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) priorityColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) priorityColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = priority.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) priorityColor else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Pomodoro toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Pomodoro Mode",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "25m Study / 5m Break cycles",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isPomodoro,
                            onCheckedChange = { isPomodoro = it },
                            modifier = Modifier.testTag("pomodoro_switch")
                        )
                    }

                    // Reminder toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Task Reminder",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Notify 10 minutes before start",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { reminderEnabled = it },
                            modifier = Modifier.testTag("reminder_switch")
                        )
                    }

                    // Notes / Description
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description / Notes") },
                        placeholder = { Text("e.g. Solve exercise 4.2 questions 1 to 10") },
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("task_description_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                titleError = true
                                return@Button
                            }
                            val sub = selectedSubject ?: subjects.firstOrNull() ?: Subject(
                                name = "General Study",
                                colorHex = "#3B82F6"
                            )
                            onSave(
                                title.trim(),
                                sub,
                                description.trim(),
                                taskDate,
                                startTime.trim(),
                                endTime.trim(),
                                targetDurationMinutes,
                                selectedPriority,
                                reminderEnabled,
                                reminderMinutesBefore,
                                isPomodoro
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("save_task_submit_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (taskToEdit == null) "Create Task" else "Save Changes")
                    }
                }
            }
        }
    }
}
