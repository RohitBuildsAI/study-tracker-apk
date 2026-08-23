package com.example.ui.timer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.SubjectBadge
import com.example.ui.components.parseColorSafe
import com.example.util.DateTimeUtils

@Composable
fun ActiveStudyTimerDialog(
    timerState: ActiveTimerState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onAddExtraMinutes: (Int) -> Unit,
    onStartBreak: (Int) -> Unit,
    onFinishAndSave: (markCompleted: Boolean, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var showNotesInput by remember { mutableStateOf(false) }
    var sessionNotes by remember { mutableStateOf("") }

    val subjectColor = parseColorSafe(timerState.subjectColorHex)
    val taskTitle = timerState.task?.title ?: "Quick Study: ${timerState.subjectName}"

    // Subtle pulsing animation when active
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alphaPulse"
    )

    Dialog(
        onDismissRequest = { /* Prevent accidental background tap dismiss while studying */ },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("active_study_timer_screen"),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Bar
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
                                else -> "⏱️ Study Countdown"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = when (timerState.mode) {
                                TimerMode.POMODORO_BREAK -> Color(0xFF10B981)
                                TimerMode.POMODORO_WORK -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSecondaryContainer
                            },
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    // Extra options (Notes)
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
                }

                // Task Information
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SubjectBadge(
                        subjectName = timerState.subjectName,
                        colorHex = timerState.subjectColorHex
                    )

                    Text(
                        text = taskTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    if (timerState.targetSeconds > 0) {
                        Text(
                            text = "Target: ${DateTimeUtils.formatDurationSeconds(timerState.targetSeconds)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Center Timer Display with Circular Arc
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
                            topLeft = androidx.compose.ui.geometry.Offset(topLeft, topLeft),
                            size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )

                        // Progress
                        val progressSweep = (timerState.progress * 360f).coerceIn(0f, 360f)
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
                                topLeft = androidx.compose.ui.geometry.Offset(topLeft, topLeft),
                                size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
                                style = Stroke(width = strokePx, cap = StrokeCap.Round)
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val displaySeconds = if (timerState.mode == TimerMode.TASK_COUNTDOWN || timerState.mode == TimerMode.POMODORO_WORK || timerState.mode == TimerMode.POMODORO_BREAK) {
                            timerState.remainingSeconds
                        } else {
                            timerState.elapsedSeconds
                        }

                        Text(
                            text = DateTimeUtils.formatDurationSeconds(displaySeconds),
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontSize = 42.sp,
                                letterSpacing = 1.sp
                            ),
                            fontWeight = FontWeight.ExtraBold,
                            color = if (timerState.isPaused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (timerState.isPaused) "PAUSED" else if (timerState.mode == TimerMode.STOPWATCH) "Elapsed" else "Remaining",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (timerState.isPaused) Color(0xFFF59E0B) else subjectColor
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Elapsed: ${DateTimeUtils.formatDurationSeconds(timerState.elapsedSeconds)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

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
                            .testTag("target_reached_banner"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
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
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.testTag("add_5min_btn")
                    ) {
                        Text("+5 min", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { onAddExtraMinutes(15) },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.testTag("add_15min_btn")
                    ) {
                        Text("+15 min", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { onStartBreak(5) },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.testTag("take_break_btn")
                    ) {
                        Text("☕ 5m Break", fontWeight = FontWeight.SemiBold)
                    }
                }

                // Primary Bottom Controls: Pause/Resume & Finish
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play/Pause Button
                    if (timerState.isPaused) {
                        Button(
                            onClick = onResume,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
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
                                .height(56.dp)
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
                            .height(56.dp)
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
