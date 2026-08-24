package com.example.ui.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.StudyTask
import com.example.data.model.Subject
import com.example.data.model.TaskPriority
import com.example.ui.components.SubjectSelectableChip
import com.example.util.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
    val focusManager = LocalFocusManager.current

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
    var reminderMinutesBefore by remember { mutableIntStateOf(taskToEdit?.reminderMinutesBefore ?: 15) }
    var isPomodoro by remember { mutableStateOf(taskToEdit?.isPomodoro ?: false) }

    var titleError by remember { mutableStateOf(false) }

    // Picker Dialog States
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Duration presets
    val durationPresets = listOf(15, 30, 45, 60, 90, 120)

    // Helper to auto-sync duration when start and end times change
    fun updateDurationFromTimes(start: String, end: String) {
        if (start.isNotBlank() && end.isNotBlank()) {
            val calcMinutes = DateTimeUtils.calculateDurationBetweenTimes(start, end)
            if (calcMinutes != null && calcMinutes > 0) {
                targetDurationMinutes = calcMinutes
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.92f)
                .testTag("add_edit_task_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .imePadding()
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (taskToEdit == null) "Create Study Task" else "Edit Task",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Set schedule and learning goals",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_task_dialog_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Scrollable Form
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
                        placeholder = { Text("e.g. Physics Chapter 4, Math Calculus") },
                        isError = titleError,
                        supportingText = if (titleError) {
                            { Text("Task name is required", color = MaterialTheme.colorScheme.error) }
                        } else null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("task_name_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Date Selection Card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                            .testTag("select_task_date_btn")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = "Task Date",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = DateTimeUtils.formatDisplayDate(taskDate),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            FilledTonalButton(
                                onClick = { showDatePicker = true },
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Change", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

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

                    // Start Time & End Time with Interactive Confirmation Pickers
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Schedule Time Slot",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (startTime.isNotBlank() || endTime.isNotBlank()) {
                                TextButton(
                                    onClick = {
                                        startTime = ""
                                        endTime = ""
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text("Clear Times", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Start Time Field / Picker Button
                            OutlinedTextField(
                                value = startTime,
                                onValueChange = {
                                    startTime = it
                                    updateDurationFromTimes(it, endTime)
                                },
                                label = { Text("Start Time") },
                                placeholder = { Text("e.g. 06:00 PM") },
                                trailingIcon = {
                                    IconButton(onClick = { showStartTimePicker = true }) {
                                        Icon(
                                            imageVector = Icons.Default.AccessTime,
                                            contentDescription = "Pick Start Time",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("start_time_input"),
                                shape = RoundedCornerShape(14.dp)
                            )

                            // End Time Field / Picker Button
                            OutlinedTextField(
                                value = endTime,
                                onValueChange = {
                                    endTime = it
                                    updateDurationFromTimes(startTime, it)
                                },
                                label = { Text("End Time") },
                                placeholder = { Text("e.g. 07:00 PM") },
                                trailingIcon = {
                                    IconButton(onClick = { showEndTimePicker = true }) {
                                        Icon(
                                            imageVector = Icons.Default.AccessTimeFilled,
                                            contentDescription = "Pick End Time",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("end_time_input"),
                                shape = RoundedCornerShape(14.dp)
                            )
                        }

                        // Quick Time Presets (Now, +1h, etc.)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AssistChip(
                                onClick = {
                                    val now = Calendar.getInstance()
                                    val formattedNow = DateTimeUtils.formatTime(
                                        now.get(Calendar.HOUR_OF_DAY),
                                        now.get(Calendar.MINUTE)
                                    )
                                    startTime = formattedNow
                                    endTime = DateTimeUtils.addMinutesToTime(formattedNow, targetDurationMinutes) ?: ""
                                },
                                label = { Text("Start Now") },
                                leadingIcon = {
                                    Icon(Icons.Default.PlayCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                shape = CircleShape
                            )

                            AssistChip(
                                onClick = {
                                    showStartTimePicker = true
                                },
                                label = { Text("Pick Clock Time") },
                                leadingIcon = {
                                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                shape = CircleShape
                            )

                            if (startTime.isNotBlank() && endTime.isBlank()) {
                                AssistChip(
                                    onClick = {
                                        endTime = DateTimeUtils.addMinutesToTime(startTime, targetDurationMinutes) ?: ""
                                    },
                                    label = { Text("Auto End (+${targetDurationMinutes}m)") },
                                    shape = CircleShape
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
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = DateTimeUtils.formatDurationMinutes(targetDurationMinutes),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            durationPresets.forEach { mins ->
                                val isSelected = targetDurationMinutes == mins
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        targetDurationMinutes = mins
                                        if (startTime.isNotBlank()) {
                                            endTime = DateTimeUtils.addMinutesToTime(startTime, mins) ?: endTime
                                        }
                                    },
                                    label = { Text("${mins}m", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                                    shape = CircleShape
                                )
                            }
                        }
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
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) priorityColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) priorityColor else MaterialTheme.colorScheme.outlineVariant
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
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pomodoro Mode",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
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
                    }

                    // 15-Minute Study Reminder Toggle & Settings
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (reminderEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (reminderEnabled) 1.5.dp else 1.dp,
                            color = if (reminderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (reminderEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                        contentDescription = "Reminder Notification",
                                        tint = if (reminderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Column {
                                        Text(
                                            text = "Study Reminder Notification",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Remind me $reminderMinutesBefore minutes before scheduled start",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = reminderEnabled,
                                    onCheckedChange = { reminderEnabled = it },
                                    modifier = Modifier.testTag("reminder_switch")
                                )
                            }

                            if (reminderEnabled) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                // Quick Reminder Time Selection Chips
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Remind Before Start:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(5, 10, 15, 30).forEach { mins ->
                                            val isSelected = reminderMinutesBefore == mins
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { reminderMinutesBefore = mins },
                                                label = {
                                                    Text(
                                                        text = if (mins == 15) "15 min (Default)" else "$mins min",
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                    )
                                                },
                                                leadingIcon = if (isSelected) {
                                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                                } else null,
                                                shape = CircleShape
                                            )
                                        }
                                    }
                                }

                                // Information / Calculated Timing Badge
                                if (startTime.isNotBlank()) {
                                    val reminderTime = DateTimeUtils.getReminderTimeString(startTime, reminderMinutesBefore)
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AlarmOn,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = if (reminderTime != null) {
                                                    "Alarm will trigger at $reminderTime ($reminderMinutesBefore min prior to $startTime)"
                                                } else {
                                                    "Alarm will notify $reminderMinutesBefore min before $startTime"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "Set a Start Time above to enable scheduled alert",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                            TextButton(
                                                onClick = { showStartTimePicker = true },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text("Set Start Time", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Notes / Description
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description / Notes (Optional)") },
                        placeholder = { Text("e.g. Exercises 1-15, revise summary notes") },
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("task_description_input"),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                titleError = true
                                return@Button
                            }
                            focusManager.clearFocus()

                            val sub = selectedSubject ?: subjects.firstOrNull() ?: Subject(
                                name = "General Study",
                                colorHex = "#6750A4"
                            )

                            // Validate/harmonize duration if start and end are provided
                            val finalStart = startTime.trim()
                            val finalEnd = endTime.trim()
                            if (finalStart.isNotBlank() && finalEnd.isNotBlank()) {
                                val calcMins = DateTimeUtils.calculateDurationBetweenTimes(finalStart, finalEnd)
                                if (calcMins != null && calcMins > 0) {
                                    targetDurationMinutes = calcMins
                                }
                            }

                            onSave(
                                title.trim(),
                                sub,
                                description.trim(),
                                taskDate,
                                finalStart,
                                finalEnd,
                                targetDurationMinutes,
                                selectedPriority,
                                reminderEnabled,
                                reminderMinutesBefore,
                                isPomodoro
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("save_task_submit_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = if (taskToEdit == null) Icons.Default.Add else Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (taskToEdit == null) "Create Task" else "Save Changes",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Material 3 TimePicker Dialog for START TIME
    if (showStartTimePicker) {
        val initialParsed = DateTimeUtils.parseTimeStringToHourMinute(startTime)
        val initialHour = initialParsed?.first ?: Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val initialMinute = initialParsed?.second ?: 0
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = false
        )

        TimePickerDialogCustom(
            title = "Select Start Time",
            onDismiss = { showStartTimePicker = false },
            onConfirm = {
                val formatted = DateTimeUtils.formatTime(timePickerState.hour, timePickerState.minute)
                startTime = formatted
                showStartTimePicker = false
                // Auto-suggest end time if blank
                if (endTime.isBlank()) {
                    endTime = DateTimeUtils.addMinutesToTime(formatted, targetDurationMinutes) ?: ""
                } else {
                    updateDurationFromTimes(formatted, endTime)
                }
            }
        ) {
            TimePicker(
                state = timePickerState,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }

    // Material 3 TimePicker Dialog for END TIME
    if (showEndTimePicker) {
        val initialParsed = DateTimeUtils.parseTimeStringToHourMinute(endTime)
            ?: DateTimeUtils.parseTimeStringToHourMinute(startTime)?.let {
                Pair((it.first + (targetDurationMinutes / 60)) % 24, (it.second + (targetDurationMinutes % 60)) % 60)
            }
        val initialHour = initialParsed?.first ?: ((Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + 1) % 24)
        val initialMinute = initialParsed?.second ?: 0
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = false
        )

        TimePickerDialogCustom(
            title = "Select End Time",
            onDismiss = { showEndTimePicker = false },
            onConfirm = {
                val formatted = DateTimeUtils.formatTime(timePickerState.hour, timePickerState.minute)
                endTime = formatted
                showEndTimePicker = false
                updateDurationFromTimes(startTime, formatted)
            }
        ) {
            TimePicker(
                state = timePickerState,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }

    // Material 3 DatePicker Dialog for TASK DATE
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = try {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(taskDate)?.time ?: System.currentTimeMillis()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            val selected = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(millis))
                            taskDate = selected
                        }
                        showDatePicker = false
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Confirm Date", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDatePicker = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun TimePickerDialogCustom(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                content()

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Confirm Time", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
