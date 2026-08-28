package com.example.ui.timer

import com.example.data.model.StudyTask

enum class TimerMode {
    TASK_COUNTDOWN,
    POMODORO_WORK,
    POMODORO_BREAK,
    STOPWATCH
}

data class ActiveTimerState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val task: StudyTask? = null,
    val subjectName: String = "",
    val subjectColorHex: String = "#3B82F6",
    val mode: TimerMode = TimerMode.TASK_COUNTDOWN,
    val elapsedSeconds: Int = 0,
    val targetSeconds: Int = 3600, // e.g. 60m
    val isCompletedDialogShown: Boolean = false,
    val pomodoroCyclesCompleted: Int = 0,
    val savedStudyTargetSeconds: Int = 1500, // Default to 25m work duration
    val savedStudyElapsedSeconds: Int = 0, // Saved study progress prior to taking break
    val savedStudyMode: TimerMode = TimerMode.TASK_COUNTDOWN,
    val cumulativeStudySeconds: Int = 0, // Total study seconds accumulated before/across breaks
    val initialTaskCompletedSeconds: Int = 0 // Existing completed seconds when resuming this task
) {
    val isBreak: Boolean
        get() = mode == TimerMode.POMODORO_BREAK

    val remainingSeconds: Int
        get() = maxOf(0, targetSeconds - elapsedSeconds)

    val progress: Float
        get() = if (targetSeconds > 0) {
            (elapsedSeconds.toFloat() / targetSeconds.toFloat()).coerceIn(0f, 1f)
        } else 0f
}
