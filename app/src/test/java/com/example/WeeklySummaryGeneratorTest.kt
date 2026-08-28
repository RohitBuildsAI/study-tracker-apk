package com.example

import com.example.data.model.DailyGoal
import com.example.data.model.StudySession
import com.example.data.model.StudyTask
import com.example.data.model.Subject
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.preferences.UserSettings
import com.example.util.WeeklySummaryGenerator
import org.junit.Assert.*
import org.junit.Test

class WeeklySummaryGeneratorTest {

    @Test
    fun testWeeklySummaryCalculation() {
        val weekDates = WeeklySummaryGenerator.getWeekDates(0)
        assertEquals(7, weekDates.size)

        val monday = weekDates[0]
        val tuesday = weekDates[1]

        val subjects = listOf(
            Subject(id = 1, name = "Math", colorHex = "#4F46E5", targetHoursPerWeek = 5f),
            Subject(id = 2, name = "Science", colorHex = "#10B981", targetHoursPerWeek = 3f)
        )

        val sessions = listOf(
            StudySession(id = 1, subjectId = 1, subjectName = "Math", date = monday, durationSeconds = 3600), // 60 mins
            StudySession(id = 2, subjectId = 2, subjectName = "Science", date = monday, durationSeconds = 1800), // 30 mins
            StudySession(id = 3, subjectId = 1, subjectName = "Math", date = tuesday, durationSeconds = 3600) // 60 mins
        )

        val tasks = listOf(
            StudyTask(id = 1, title = "Math Homework", subjectId = 1, subjectName = "Math", date = monday, durationMinutes = 60, status = TaskStatus.COMPLETED, priority = TaskPriority.HIGH),
            StudyTask(id = 2, title = "Science Lab", subjectId = 2, subjectName = "Science", date = monday, durationMinutes = 30, status = TaskStatus.PENDING, priority = TaskPriority.MEDIUM),
            StudyTask(id = 3, title = "Math Quiz prep", subjectId = 1, subjectName = "Math", date = tuesday, durationMinutes = 60, status = TaskStatus.COMPLETED, priority = TaskPriority.HIGH)
        )

        val dailyGoals = listOf(
            DailyGoal(id = 1, date = monday, targetMinutes = 90, actualMinutes = 90, isGoalMet = true),
            DailyGoal(id = 2, date = tuesday, targetMinutes = 90, actualMinutes = 60, isGoalMet = false)
        )

        val userSettings = UserSettings(
            userName = "Test Student",
            defaultDailyGoalMinutes = 90,
            currentStreak = 4
        )

        val summary = WeeklySummaryGenerator.generateWeeklySummary(
            weekOffset = 0,
            sessions = sessions,
            tasks = tasks,
            dailyGoals = dailyGoals,
            subjects = subjects,
            userSettings = userSettings
        )

        // Total time: 60 + 30 + 60 = 150 mins
        assertEquals(150, summary.totalStudyMinutes)
        assertEquals(3, summary.totalSessionsCount)

        // Daily average
        assertEquals(150 / 7, summary.dailyAverageMinutes)

        // Goal completion: 1 day met out of 7
        assertEquals(1, summary.daysGoalMetCount)
        assertEquals(7, summary.totalDaysInPeriod)
        assertEquals((1f / 7f) * 100f, summary.goalCompletionRatePercent, 0.1f)

        // Task completion: 2 of 3 completed
        assertEquals(3, summary.tasksScheduledCount)
        assertEquals(2, summary.tasksCompletedCount)
        assertEquals((2f / 3f) * 100f, summary.taskCompletionRatePercent, 0.1f)

        // Text summary contains key elements
        assertTrue(summary.summaryText.contains("Total Study Time"))
        assertTrue(summary.summaryText.contains("Goal Completion Rate"))
        assertTrue(summary.summaryText.contains("Math"))
        assertTrue(summary.summaryText.contains("Science"))
        assertTrue(summary.summaryText.contains("Test Student"))
    }
}
