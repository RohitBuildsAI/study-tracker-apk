package com.example.ui.timer

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TimerStyle
import com.example.ui.components.getSubjectIcon
import com.example.ui.components.parseColorSafe
import com.example.util.DateTimeUtils
import com.example.util.MotivationalQuote
import com.example.util.QuoteProvider

/**
 * Dedicated Focus Mode Screen
 * Hides all non-essential UI elements (navigation, toolbars, secondary cards, complex side controls)
 * and shows ONLY the active task timer and a motivational quote.
 */
@Composable
fun FocusModeScreen(
    timerState: ActiveTimerState,
    timerStyle: TimerStyle = TimerStyle.CleanDigital,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onResumeStudy: () -> Unit = {},
    onFinish: (markCompleted: Boolean, notes: String) -> Unit,
    onExitFocusMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isKeepScreenOn by remember { mutableStateOf(true) }
    var currentQuote by remember { mutableStateOf(QuoteProvider.getRandomQuote()) }
    var showEarlyFinishDialog by remember { mutableStateOf(false) }

    val handleFinishClick: () -> Unit = {
        val currentElapsed = if (timerState.isBreak) timerState.savedStudyElapsedSeconds else timerState.elapsedSeconds
        val currentTarget = if (timerState.isBreak) timerState.savedStudyTargetSeconds else timerState.targetSeconds
        if (currentTarget > 0 && currentElapsed < currentTarget) {
            showEarlyFinishDialog = true
        } else {
            onFinish(true, "")
        }
    }

    // Keep screen awake while in Focus Mode
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

    val subjectColor = if (timerState.isBreak) Color(0xFFF59E0B) else parseColorSafe(timerState.subjectColorHex)
    val taskTitle = if (timerState.isBreak) {
        "☕ Break Time (Rest & Recharge)"
    } else {
        timerState.task?.title ?: "Quick Study: ${timerState.subjectName}"
    }

    val displaySeconds = if (timerState.mode == TimerMode.TASK_COUNTDOWN ||
        timerState.mode == TimerMode.POMODORO_WORK ||
        timerState.mode == TimerMode.POMODORO_BREAK
    ) {
        timerState.remainingSeconds
    } else {
        timerState.elapsedSeconds
    }

    // Subtle pulsing ambient light effect
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_focus_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("focus_mode_screen"),
        color = Color(0xFF07090E) // Deep OLED dark distraction-free canvas
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Subtle ambient subject color glow at the top center
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                subjectColor.copy(alpha = pulseAlpha),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // =========================================================================
                // 1. MINIMALIST TOP BAR: Exit Focus Mode, Subject Tag, Screen Awake Toggle
                // =========================================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Exit Focus Mode Button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E293B).copy(alpha = 0.8f),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier
                            .clickable { onExitFocusMode() }
                            .testTag("exit_focus_mode_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Exit Focus Mode",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Exit Focus",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }

                    // Subject Badge Pill
                    Surface(
                        shape = CircleShape,
                        color = subjectColor.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, subjectColor.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
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

                    // Keep Screen Awake Toggle
                    Surface(
                        shape = CircleShape,
                        color = if (isKeepScreenOn) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF1E293B),
                        border = BorderStroke(
                            1.dp,
                            if (isKeepScreenOn) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFF334155)
                        ),
                        modifier = Modifier
                            .clickable { isKeepScreenOn = !isKeepScreenOn }
                            .testTag("focus_screen_awake_toggle")
                    ) {
                        Box(
                            modifier = Modifier.padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isKeepScreenOn) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isKeepScreenOn) "Screen Awake: On" else "Screen Awake: Off",
                                tint = if (isKeepScreenOn) Color(0xFF34D399) else Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // =========================================================================
                // 2. CENTER SECTION: ACTIVE TASK TIMER (Centerpiece)
                // =========================================================================
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Task Title
                    Text(
                        text = taskTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF1F5F9),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Active Task Timer Visualizer Display
                    CountdownTimerStyleDisplay(
                        style = timerStyle,
                        displaySeconds = displaySeconds.toLong(),
                        elapsedSeconds = timerState.elapsedSeconds.toLong(),
                        targetSeconds = timerState.targetSeconds.toLong(),
                        progress = timerState.progress,
                        isPaused = timerState.isPaused,
                        subjectColor = subjectColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("focus_mode_active_timer")
                    )

                    // Target / Elapsed subtitle indicator
                    if (timerState.targetSeconds > 0) {
                        val remainingStudySec = maxOf(0, timerState.savedStudyTargetSeconds - timerState.savedStudyElapsedSeconds)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (timerState.isBreak) {
                                "Break: ${DateTimeUtils.formatDurationSeconds(timerState.targetSeconds)}  •  Study Remaining: ${DateTimeUtils.formatDurationSeconds(remainingStudySec)}"
                            } else {
                                "Target: ${DateTimeUtils.formatDurationSeconds(timerState.targetSeconds)}  •  Elapsed: ${DateTimeUtils.formatDurationSeconds(timerState.elapsedSeconds)}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // =========================================================================
                // 3. MOTIVATIONAL QUOTE CARD
                // =========================================================================
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .clickable {
                            currentQuote = QuoteProvider.getRandomQuote()
                        }
                        .testTag("focus_mode_motivational_quote")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Quotation icon / decorative symbol
                        Text(
                            text = "“",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = subjectColor.copy(alpha = 0.8f),
                            lineHeight = 20.sp
                        )

                        // Motivational Quote Text
                        Text(
                            text = currentQuote.quote,
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE2E8F0),
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )

                        // Author & Category Attribution
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(16.dp)
                                    .height(1.dp)
                                    .background(Color(0xFF475569))
                            )
                            Text(
                                text = currentQuote.author,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8)
                            )
                            Box(
                                modifier = Modifier
                                    .width(16.dp)
                                    .height(1.dp)
                                    .background(Color(0xFF475569))
                            )
                        }

                        Text(
                            text = "Tap quote to refresh",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF475569),
                            fontSize = 10.sp
                        )
                    }
                }

                // =========================================================================
                // 4. MINIMALIST ESSENTIAL CONTROLS (Pause/Resume & Finish)
                // =========================================================================
                if (timerState.isBreak) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Start Study Countdown Primary Action
                        Button(
                            onClick = onResumeStudy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("focus_start_study_countdown_btn"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Start Study Countdown",
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Start Study Countdown",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Play / Pause Button during break
                            if (timerState.isPaused) {
                                Button(
                                    onClick = onResume,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("focus_resume_btn"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF334155),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Resume Break",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Resume Break",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else {
                                Button(
                                    onClick = onPause,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("focus_pause_btn"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF334155),
                                        contentColor = Color(0xFFF1F5F9)
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Pause,
                                        contentDescription = "Pause Break",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Pause Break",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Complete & Finish Button
                            Button(
                                onClick = handleFinishClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("focus_finish_btn"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF475569),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Finish Session",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Finish",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play / Pause Button
                        if (timerState.isPaused) {
                            Button(
                                onClick = onResume,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .testTag("focus_resume_btn"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF10B981),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Resume Timer",
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Resume",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Button(
                                onClick = onPause,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .testTag("focus_pause_btn"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF334155),
                                    contentColor = Color(0xFFF1F5F9)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = "Pause Timer",
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Pause",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Complete & Finish Button
                        Button(
                            onClick = handleFinishClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("focus_finish_btn"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = subjectColor,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Finish Session",
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Finish",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Early Finish Options in Focus Mode
        if (showEarlyFinishDialog) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showEarlyFinishDialog = false }
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF1E293B),
                    contentColor = Color.White,
                    tonalElevation = 8.dp,
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Save Study Progress?",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            val studiedSec = if (timerState.isBreak) timerState.savedStudyElapsedSeconds else timerState.elapsedSeconds
                            val targetSec = if (timerState.isBreak) timerState.savedStudyTargetSeconds else timerState.targetSeconds
                            val remSec = maxOf(0, targetSec - studiedSec)

                            Text(
                                text = "You've studied ${DateTimeUtils.formatDurationSeconds(studiedSec)} of your ${DateTimeUtils.formatDurationSeconds(targetSec)} countdown.\n(${DateTimeUtils.formatDurationSeconds(remSec)} remaining).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center
                            )
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 1. Save Progress & Resume Later
                            Button(
                                onClick = {
                                    showEarlyFinishDialog = false
                                    onFinish(false, "")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("focus_save_and_resume_later_btn"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF38BDF8),
                                    contentColor = Color(0xFF0F172A)
                                )
                            ) {
                                Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save Progress & Resume Later", fontWeight = FontWeight.Bold)
                            }

                            // 2. Mark Task Completed
                            OutlinedButton(
                                onClick = {
                                    showEarlyFinishDialog = false
                                    onFinish(true, "")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("focus_mark_completed_early_btn"),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFF475569))
                            ) {
                                Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Mark Task as Completed", color = Color.White)
                            }

                            // 3. Keep Studying
                            TextButton(
                                onClick = { showEarlyFinishDialog = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Keep Studying", color = Color(0xFF94A3B8))
                            }
                        }
                    }
                }
            }
        }
    }
}
