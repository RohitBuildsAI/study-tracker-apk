package com.example

import com.example.ui.timer.ActiveTimerState
import com.example.ui.timer.TimerMode
import org.junit.Assert.*
import org.junit.Test

class StudyTimerBreakResumeTest {

    @Test
    fun testBreakAndResumePreservesCountdownProgress() {
        // 1. Initial 45-minute countdown session
        val totalTarget = 45 * 60 // 2700s
        var timerState = ActiveTimerState(
            isRunning = true,
            mode = TimerMode.TASK_COUNTDOWN,
            targetSeconds = totalTarget,
            elapsedSeconds = 0
        )
        assertEquals(2700, timerState.remainingSeconds)

        // 2. User studies for 15 minutes (900s)
        timerState = timerState.copy(elapsedSeconds = 900)
        assertEquals(1800, timerState.remainingSeconds) // 30 mins remaining

        // 3. User takes a 5-minute break
        // Logic from startBreakTimer():
        val isFromStudy = timerState.mode != TimerMode.POMODORO_BREAK
        val hasCompletedCurrentTarget = isFromStudy && timerState.targetSeconds > 0 && timerState.elapsedSeconds >= timerState.targetSeconds
        val savedElapsed = if (isFromStudy) {
            if (hasCompletedCurrentTarget) 0 else timerState.elapsedSeconds
        } else {
            timerState.savedStudyElapsedSeconds
        }
        val savedTarget = timerState.targetSeconds

        timerState = timerState.copy(
            mode = TimerMode.POMODORO_BREAK,
            elapsedSeconds = 0,
            targetSeconds = 5 * 60,
            savedStudyTargetSeconds = savedTarget,
            savedStudyElapsedSeconds = savedElapsed,
            savedStudyMode = TimerMode.TASK_COUNTDOWN
        )

        assertTrue(timerState.isBreak)
        assertEquals(300, timerState.targetSeconds) // 5 min break
        assertEquals(900, timerState.savedStudyElapsedSeconds)
        assertEquals(2700, timerState.savedStudyTargetSeconds)

        // 4. Break is completed or user clicks "Resume Study Countdown"
        // Logic from resumeStudyAfterBreak():
        timerState = timerState.copy(
            mode = timerState.savedStudyMode,
            elapsedSeconds = timerState.savedStudyElapsedSeconds,
            targetSeconds = timerState.savedStudyTargetSeconds
        )

        assertFalse(timerState.isBreak)
        assertEquals(TimerMode.TASK_COUNTDOWN, timerState.mode)
        assertEquals(900, timerState.elapsedSeconds)
        assertEquals(2700, timerState.targetSeconds)
        // Verify remaining countdown continues from 1800s (30m) - does NOT restart from 2700s (45m)!
        assertEquals(1800, timerState.remainingSeconds)
    }

    @Test
    fun testCompletedPomodoroStartsFreshCycleAfterBreak() {
        // 1. Initial 25-minute Pomodoro
        val workSecs = 25 * 60 // 1500s
        var timerState = ActiveTimerState(
            isRunning = true,
            mode = TimerMode.POMODORO_WORK,
            targetSeconds = workSecs,
            elapsedSeconds = 1500 // Target completed
        )

        // 2. Take break after completed cycle
        val isFromStudy = timerState.mode != TimerMode.POMODORO_BREAK
        val hasCompletedCurrentTarget = isFromStudy && timerState.targetSeconds > 0 && timerState.elapsedSeconds >= timerState.targetSeconds
        assertTrue(hasCompletedCurrentTarget)

        val newCumulative = timerState.cumulativeStudySeconds + timerState.elapsedSeconds
        val savedElapsed = if (hasCompletedCurrentTarget) 0 else timerState.elapsedSeconds

        timerState = timerState.copy(
            mode = TimerMode.POMODORO_BREAK,
            elapsedSeconds = 0,
            targetSeconds = 5 * 60,
            savedStudyTargetSeconds = 1500,
            savedStudyElapsedSeconds = savedElapsed,
            savedStudyMode = TimerMode.POMODORO_WORK,
            cumulativeStudySeconds = newCumulative,
            pomodoroCyclesCompleted = 1
        )

        assertEquals(0, timerState.savedStudyElapsedSeconds)
        assertEquals(1500, timerState.cumulativeStudySeconds)

        // 3. Resume study for next cycle
        timerState = timerState.copy(
            mode = timerState.savedStudyMode,
            elapsedSeconds = timerState.savedStudyElapsedSeconds,
            targetSeconds = timerState.savedStudyTargetSeconds
        )

        assertEquals(0, timerState.elapsedSeconds)
        assertEquals(1500, timerState.targetSeconds)
        assertEquals(1500, timerState.remainingSeconds) // Fresh 25m cycle starts
    }
}
