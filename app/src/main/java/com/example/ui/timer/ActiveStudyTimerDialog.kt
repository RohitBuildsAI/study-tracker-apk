package com.example.ui.timer

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.TimerStyle
import com.example.ui.components.SubjectBadge
import com.example.ui.components.parseColorSafe
import com.example.util.DateTimeUtils

@Composable
fun ActiveStudyTimerDialog(
    timerState: ActiveTimerState,
    timerStyle: TimerStyle = TimerStyle.CleanDigital,
    initialFocusMode: Boolean = false,
    onSelectTimerStyle: (TimerStyle) -> Unit = {},
    onPause: () -> Unit,
    onResume: () -> Unit,
    onAddExtraMinutes: (Int) -> Unit,
    onStartBreak: (Int) -> Unit,
    onFinishAndSave: (markCompleted: Boolean, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var showNotesInput by remember { mutableStateOf(false) }
    var showStylePickerModal by remember { mutableStateOf(false) }
    var sessionNotes by remember { mutableStateOf("") }
    var isFullScreenMode by remember { mutableStateOf(false) }
    var isFocusMode by remember { mutableStateOf(initialFocusMode) }
    var isKeepScreenOn by remember { mutableStateOf(true) } // Enabled by default to prevent sleeping

    val context = LocalContext.current

    // Keep screen awake ("Never Sleep") effect
    DisposableEffect(isKeepScreenOn) {
        val window = (context as? Activity)?.window
        if (isKeepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val subjectColor = parseColorSafe(timerState.subjectColorHex)
    val taskTitle = timerState.task?.title ?: "Quick Study: ${timerState.subjectName}"

    val displaySeconds = if (timerState.mode == TimerMode.TASK_COUNTDOWN ||
        timerState.mode == TimerMode.POMODORO_WORK ||
        timerState.mode == TimerMode.POMODORO_BREAK
    ) {
        timerState.remainingSeconds
    } else {
        timerState.elapsedSeconds
    }

    Dialog(
        onDismissRequest = { /* Prevent accidental background tap dismiss while studying */ },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        if (isFocusMode) {
            FocusModeScreen(
                timerState = timerState,
                timerStyle = timerStyle,
                onPause = onPause,
                onResume = onResume,
                onFinish = { markCompleted, notes -> onFinishAndSave(markCompleted, notes) },
                onExitFocusMode = { isFocusMode = false }
            )
        } else if (isFullScreenMode) {
            // =========================================================================
            // IMMERSIVE FULL SCREEN / LARGE COUNTDOWN MODE
            // =========================================================================
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("fullscreen_countdown_screen"),
                color = Color(0xFF090D16) // Deep OLED immersive background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Bar: Exit Fullscreen, Subject Tag, Never Sleep Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Exit Full Screen Button
                        FilledTonalIconButton(
                            onClick = { isFullScreenMode = false },
                            modifier = Modifier.testTag("exit_fullscreen_btn"),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color(0xFF1E293B),
                                contentColor = Color(0xFFF1F5F9)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.FullscreenExit,
                                contentDescription = "Exit Full Screen"
                            )
                        }

                        // Subject & Mode Indicator Pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = subjectColor.copy(alpha = 0.25f),
                                border = BorderStroke(1.dp, subjectColor.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(subjectColor, CircleShape)
                                    )
                                    Text(
                                        text = timerState.subjectName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            // Style Pill
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.clickable { showStylePickerModal = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(text = timerStyle.iconEmoji, fontSize = 12.sp)
                                    Text(
                                        text = timerStyle.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }

                        // Never Sleep ("Keep Screen On") Toggle Button
                        FilledTonalIconButton(
                            onClick = { isKeepScreenOn = !isKeepScreenOn },
                            modifier = Modifier.testTag("fullscreen_never_sleep_btn"),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (isKeepScreenOn) Color(0xFF10B981).copy(alpha = 0.25f) else Color(0xFF1E293B),
                                contentColor = if (isKeepScreenOn) Color(0xFF34D399) else Color(0xFF94A3B8)
                            )
                        ) {
                            Icon(
                                imageVector = if (isKeepScreenOn) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isKeepScreenOn) "Never Sleep: On" else "Never Sleep: Off"
                            )
                        }
                    }

                    // Center Section: Giant Large Clock Visualizer & Task Title
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = taskTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF8FAFC),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        if (timerState.targetSeconds > 0) {
                            Text(
                                text = "Target: ${DateTimeUtils.formatDurationSeconds(timerState.targetSeconds)}  •  Elapsed: ${DateTimeUtils.formatDurationSeconds(timerState.elapsedSeconds)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Large Scale Countdown Visualizer
                        CountdownTimerStyleDisplay(
                            style = timerStyle,
                            displaySeconds = displaySeconds.toLong(),
                            elapsedSeconds = timerState.elapsedSeconds.toLong(),
                            targetSeconds = timerState.targetSeconds.toLong(),
                            progress = timerState.progress,
                            isPaused = timerState.isPaused,
                            subjectColor = subjectColor,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Bottom Controls Bar in Fullscreen
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Quick Add Extra Time Buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = { onAddExtraMinutes(5) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFF1E293B),
                                    contentColor = Color(0xFFE2E8F0)
                                )
                            ) {
                                Text("+5 min", fontWeight = FontWeight.Bold)
                            }
                            FilledTonalButton(
                                onClick = { onAddExtraMinutes(15) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFF1E293B),
                                    contentColor = Color(0xFFE2E8F0)
                                )
                            ) {
                                Text("+15 min", fontWeight = FontWeight.Bold)
                            }
                            FilledTonalButton(
                                onClick = { onStartBreak(5) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFF1E293B),
                                    contentColor = Color(0xFFFBBF24)
                                )
                            ) {
                                Text("☕ 5m Break", fontWeight = FontWeight.Bold)
                            }
                        }

                        // Play/Pause & Finish Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (timerState.isPaused) {
                                Button(
                                    onClick = onResume,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .testTag("fullscreen_resume_btn"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Resume", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = onPause,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .testTag("fullscreen_pause_btn"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569))
                                ) {
                                    Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Pause", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = { onFinishAndSave(true, sessionNotes) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .testTag("fullscreen_finish_btn"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = subjectColor,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Finish", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            // =========================================================================
            // STANDARD TIMER DIALOG / WINDOW VIEW
            // =========================================================================
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("active_study_timer_screen"),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Header Bar
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("timer_minimize_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close / Minimize",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Mode indicator
                            Surface(
                                shape = CircleShape,
                                color = when (timerState.mode) {
                                    TimerMode.POMODORO_BREAK -> Color(0xFF10B981).copy(alpha = 0.15f)
                                    TimerMode.POMODORO_WORK -> MaterialTheme.colorScheme.primaryContainer
                                    else -> MaterialTheme.colorScheme.secondaryContainer
                                }
                            ) {
                                Text(
                                    text = when (timerState.mode) {
                                        TimerMode.POMODORO_BREAK -> "☕ Break Time"
                                        TimerMode.POMODORO_WORK -> "🍅 Focus Cycle"
                                        else -> "⏱️ Countdown"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when (timerState.mode) {
                                        TimerMode.POMODORO_BREAK -> Color(0xFF10B981)
                                        TimerMode.POMODORO_WORK -> MaterialTheme.colorScheme.onPrimaryContainer
                                        else -> MaterialTheme.colorScheme.onSecondaryContainer
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }

                            // Action buttons: Focus Mode, Notes & Fullscreen
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Dedicated Focus Mode Button
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .clickable { isFocusMode = true }
                                        .testTag("enter_focus_mode_btn")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FilterCenterFocus,
                                            contentDescription = "Focus Mode",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = "Focus",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { showNotesInput = !showNotesInput },
                                    modifier = Modifier.testTag("timer_notes_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NoteAlt,
                                        contentDescription = "Session Notes",
                                        tint = if (sessionNotes.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Fullscreen Button
                                IconButton(
                                    onClick = { isFullScreenMode = true },
                                    modifier = Modifier.testTag("make_fullscreen_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fullscreen,
                                        contentDescription = "Make Fullscreen (Large)",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // Style Switcher Ribbon & Never Sleep Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Style Switcher Ribbon
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showStylePickerModal = true }
                                    .testTag("active_timer_style_selector")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = timerStyle.iconEmoji,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Style: ${timerStyle.title}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "Switch Style",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Keep Screen Awake ("Never Sleep") Interactive Pill
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isKeepScreenOn) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isKeepScreenOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .clickable { isKeepScreenOn = !isKeepScreenOn }
                                    .testTag("never_sleep_toggle_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isKeepScreenOn) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = if (isKeepScreenOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (isKeepScreenOn) "Never Sleep: ON" else "Never Sleep: OFF",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isKeepScreenOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Task Information
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SubjectBadge(
                            subjectName = timerState.subjectName,
                            colorHex = timerState.subjectColorHex
                        )

                        Text(
                            text = taskTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        if (timerState.targetSeconds > 0) {
                            Text(
                                text = "Target: ${DateTimeUtils.formatDurationSeconds(timerState.targetSeconds)}  •  Elapsed: ${DateTimeUtils.formatDurationSeconds(timerState.elapsedSeconds)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // SELECTED COUNTDOWN TIMER STYLE DISPLAY
                    CountdownTimerStyleDisplay(
                        style = timerStyle,
                        displaySeconds = displaySeconds.toLong(),
                        elapsedSeconds = timerState.elapsedSeconds.toLong(),
                        targetSeconds = timerState.targetSeconds.toLong(),
                        progress = timerState.progress,
                        isPaused = timerState.isPaused,
                        subjectColor = subjectColor,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Optional Notes Input if expanded
                    AnimatedVisibility(visible = showNotesInput) {
                        OutlinedTextField(
                            value = sessionNotes,
                            onValueChange = { sessionNotes = it },
                            placeholder = { Text("What did you accomplish in this session?") },
                            label = { Text("Session Notes") },
                            maxLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Completion Dialog Banner (When goal is reached)
                    if (timerState.isCompletedDialogShown) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .testTag("target_reached_banner"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, Color(0xFF10B981))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "🎉 Target Duration Reached!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF065F46)
                                )
                                Text(
                                    text = "Great progress! Would you like to continue studying or finish and save this task?",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = Color(0xFF065F46)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onAddExtraMinutes(10) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = subjectColor),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("+10 min")
                                    }
                                    Button(
                                        onClick = { onStartBreak(5) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("5m Break")
                                    }
                                    Button(
                                        onClick = { onFinishAndSave(true, sessionNotes) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Complete")
                                    }
                                }
                            }
                        }
                    }

                    // Quick Add Time Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onAddExtraMinutes(5) },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.testTag("add_5min_btn")
                        ) {
                            Text("+5 min", fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { onAddExtraMinutes(15) },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.testTag("add_15min_btn")
                        ) {
                            Text("+15 min", fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { onStartBreak(5) },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.testTag("take_break_btn")
                        ) {
                            Text("☕ 5m Break", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Primary Bottom Controls: Pause/Resume & Finish
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play/Pause Button
                        if (timerState.isPaused) {
                            Button(
                                onClick = onResume,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .testTag("resume_timer_btn"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Resume",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Button(
                                onClick = onPause,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .testTag("pause_timer_btn"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Pause",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Finish / Stop Button
                        Button(
                            onClick = { onFinishAndSave(true, sessionNotes) },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("finish_timer_btn"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Finish",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Interactive Style Selection Dialog
    if (showStylePickerModal) {
        Dialog(
            onDismissRequest = { showStylePickerModal = false },
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
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Countdown Clock Style",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Choose your preferred timer visualizer",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showStylePickerModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TimerStyle.entries.forEach { styleOption ->
                            val isSelected = timerStyle == styleOption
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectTimerStyle(styleOption)
                                        showStylePickerModal = false
                                    }
                                    .testTag("select_style_${styleOption.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(text = styleOption.iconEmoji, fontSize = 22.sp)
                                        Column {
                                            Text(
                                                text = styleOption.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = styleOption.subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary
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
