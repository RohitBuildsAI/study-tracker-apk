package com.example.util

import com.example.data.model.DailyGoal
import com.example.data.model.StudySession
import com.example.data.model.StudyTask
import com.example.data.model.Subject
import com.example.data.model.TaskStatus
import com.example.data.preferences.UserSettings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DailySummaryItem(
    val dateIso: String,
    val dayName: String,
    val displayDate: String,
    val studiedMinutes: Int,
    val targetMinutes: Int,
    val isGoalMet: Boolean,
    val completedTasks: Int,
    val totalTasks: Int,
    val sessionCount: Int
)

data class SubjectSummaryItem(
    val subjectName: String,
    val colorHex: String,
    val studiedMinutes: Int,
    val percentageOfTotal: Float,
    val targetMinutes: Int,
    val sessionCount: Int,
    val isTargetMet: Boolean
)

data class WeeklyStudySummary(
    val weekStartDate: String,
    val weekEndDate: String,
    val weekDateRangeLabel: String,
    val totalStudySeconds: Int,
    val totalStudyMinutes: Int,
    val totalSessionsCount: Int,
    val dailyAverageMinutes: Int,
    val daysGoalMetCount: Int,
    val totalDaysInPeriod: Int,
    val goalCompletionRatePercent: Float,
    val totalTargetMinutes: Int,
    val targetMinutesAchievementPercent: Float,
    val tasksScheduledCount: Int,
    val tasksCompletedCount: Int,
    val taskCompletionRatePercent: Float,
    val bestDayName: String,
    val bestDayMinutes: Int,
    val currentStreak: Int,
    val dailyBreakdowns: List<DailySummaryItem>,
    val subjectBreakdowns: List<SubjectSummaryItem>,
    val summaryText: String
)

object WeeklySummaryGenerator {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayRangeFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    private val fullDateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())

    /**
     * Returns week dates (Monday to Sunday) for an offset from current week (0 = this week, -1 = last week, etc.)
     */
    fun getWeekDates(weekOffset: Int = 0): List<String> {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.add(Calendar.WEEK_OF_YEAR, weekOffset)
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        val dates = mutableListOf<String>()
        for (i in 0..6) {
            dates.add(isoFormat.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return dates
    }

    fun generateWeeklySummary(
        weekOffset: Int = 0,
        sessions: List<StudySession>,
        tasks: List<StudyTask>,
        dailyGoals: List<DailyGoal>,
        subjects: List<Subject>,
        userSettings: UserSettings
    ): WeeklyStudySummary {
        val weekDates = getWeekDates(weekOffset)
        val startDateIso = weekDates.first()
        val endDateIso = weekDates.last()

        val startDateObj = isoFormat.parse(startDateIso) ?: Date()
        val endDateObj = isoFormat.parse(endDateIso) ?: Date()

        val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
        val weekRangeLabel = "${displayRangeFormat.format(startDateObj)} – ${displayRangeFormat.format(endDateObj)}, ${yearFormat.format(endDateObj)}"

        // Filter data for the specific week
        val weekSessions = sessions.filter { it.date in weekDates }
        val weekTasks = tasks.filter { it.date in weekDates }

        val totalStudySeconds = weekSessions.sumOf { it.durationSeconds }
        val totalStudyMinutes = (totalStudySeconds + 59) / 60
        val totalSessionsCount = weekSessions.size

        // Active days or total 7 days
        val dailyAverageMinutes = totalStudyMinutes / 7

        // Build daily breakdowns
        val dailyBreakdowns = weekDates.map { dateIso ->
            val daySessions = weekSessions.filter { it.date == dateIso }
            val dayStudiedMins = daySessions.sumOf { (it.durationSeconds + 59) / 60 }

            val dayTasks = weekTasks.filter { it.date == dateIso }
            val dayCompletedTasks = dayTasks.count { it.status == TaskStatus.COMPLETED }

            val goal = dailyGoals.find { it.date == dateIso }
            val targetMins = goal?.targetMinutes ?: userSettings.defaultDailyGoalMinutes
            val isMet = goal?.isGoalMet == true || (targetMins > 0 && dayStudiedMins >= targetMins)

            val parsedDate = isoFormat.parse(dateIso) ?: Date()

            DailySummaryItem(
                dateIso = dateIso,
                dayName = SimpleDateFormat("EEEE", Locale.getDefault()).format(parsedDate),
                displayDate = fullDateFormat.format(parsedDate),
                studiedMinutes = dayStudiedMins,
                targetMinutes = targetMins,
                isGoalMet = isMet,
                completedTasks = dayCompletedTasks,
                totalTasks = dayTasks.size,
                sessionCount = daySessions.size
            )
        }

        val daysGoalMetCount = dailyBreakdowns.count { it.isGoalMet }
        val goalCompletionRatePercent = if (dailyBreakdowns.isNotEmpty()) {
            (daysGoalMetCount.toFloat() / dailyBreakdowns.size.toFloat()) * 100f
        } else 0f

        val totalTargetMinutes = dailyBreakdowns.sumOf { it.targetMinutes }
        val targetMinutesAchievementPercent = if (totalTargetMinutes > 0) {
            (totalStudyMinutes.toFloat() / totalTargetMinutes.toFloat()) * 100f
        } else 0f

        val tasksScheduledCount = weekTasks.size
        val tasksCompletedCount = weekTasks.count { it.status == TaskStatus.COMPLETED }
        val taskCompletionRatePercent = if (tasksScheduledCount > 0) {
            (tasksCompletedCount.toFloat() / tasksScheduledCount.toFloat()) * 100f
        } else 0f

        // Best Day
        val bestDayItem = dailyBreakdowns.maxByOrNull { it.studiedMinutes }
        val bestDayName = if (bestDayItem != null && bestDayItem.studiedMinutes > 0) {
            "${bestDayItem.dayName} (${DateTimeUtils.formatDurationMinutes(bestDayItem.studiedMinutes)})"
        } else "None yet"
        val bestDayMinutes = bestDayItem?.studiedMinutes ?: 0

        // Subject breakdowns
        val subjectBreakdowns = subjects.mapNotNull { sub ->
            val subSessions = weekSessions.filter { it.subjectId == sub.id || it.subjectName.equals(sub.name, ignoreCase = true) }
            val subMins = subSessions.sumOf { (it.durationSeconds + 59) / 60 }
            val targetMins = (sub.targetHoursPerWeek * 60).toInt()

            if (subMins > 0 || targetMins > 0) {
                val percent = if (totalStudyMinutes > 0) (subMins.toFloat() / totalStudyMinutes.toFloat()) * 100f else 0f
                val isTargetMet = targetMins > 0 && subMins >= targetMins
                SubjectSummaryItem(
                    subjectName = sub.name,
                    colorHex = sub.colorHex,
                    studiedMinutes = subMins,
                    percentageOfTotal = percent,
                    targetMinutes = targetMins,
                    sessionCount = subSessions.size,
                    isTargetMet = isTargetMet
                )
            } else null
        }.sortedByDescending { it.studiedMinutes }

        // Generate clean text-based summary string
        val summaryText = buildSummaryText(
            userName = userSettings.userName,
            weekLabel = weekRangeLabel,
            weekOffset = weekOffset,
            totalStudyMinutes = totalStudyMinutes,
            totalSessionsCount = totalSessionsCount,
            dailyAverageMinutes = dailyAverageMinutes,
            daysGoalMetCount = daysGoalMetCount,
            totalDays = dailyBreakdowns.size,
            goalCompletionRatePercent = goalCompletionRatePercent,
            totalTargetMinutes = totalTargetMinutes,
            targetMinutesAchievementPercent = targetMinutesAchievementPercent,
            tasksCompleted = tasksCompletedCount,
            tasksTotal = tasksScheduledCount,
            taskCompletionRatePercent = taskCompletionRatePercent,
            currentStreak = userSettings.currentStreak,
            bestDayName = bestDayName,
            dailyBreakdowns = dailyBreakdowns,
            subjectBreakdowns = subjectBreakdowns
        )

        return WeeklyStudySummary(
            weekStartDate = startDateIso,
            weekEndDate = endDateIso,
            weekDateRangeLabel = weekRangeLabel,
            totalStudySeconds = totalStudySeconds,
            totalStudyMinutes = totalStudyMinutes,
            totalSessionsCount = totalSessionsCount,
            dailyAverageMinutes = dailyAverageMinutes,
            daysGoalMetCount = daysGoalMetCount,
            totalDaysInPeriod = dailyBreakdowns.size,
            goalCompletionRatePercent = goalCompletionRatePercent,
            totalTargetMinutes = totalTargetMinutes,
            targetMinutesAchievementPercent = targetMinutesAchievementPercent,
            tasksScheduledCount = tasksScheduledCount,
            tasksCompletedCount = tasksCompletedCount,
            taskCompletionRatePercent = taskCompletionRatePercent,
            bestDayName = bestDayName,
            bestDayMinutes = bestDayMinutes,
            currentStreak = userSettings.currentStreak,
            dailyBreakdowns = dailyBreakdowns,
            subjectBreakdowns = subjectBreakdowns,
            summaryText = summaryText
        )
    }

    private fun buildSummaryText(
        userName: String,
        weekLabel: String,
        weekOffset: Int,
        totalStudyMinutes: Int,
        totalSessionsCount: Int,
        dailyAverageMinutes: Int,
        daysGoalMetCount: Int,
        totalDays: Int,
        goalCompletionRatePercent: Float,
        totalTargetMinutes: Int,
        targetMinutesAchievementPercent: Float,
        tasksCompleted: Int,
        tasksTotal: Int,
        taskCompletionRatePercent: Float,
        currentStreak: Int,
        bestDayName: String,
        dailyBreakdowns: List<DailySummaryItem>,
        subjectBreakdowns: List<SubjectSummaryItem>
    ): String {
        val weekTitle = when (weekOffset) {
            0 -> "This Week's Study Report ($weekLabel)"
            -1 -> "Last Week's Study Report ($weekLabel)"
            else -> "Weekly Study Report ($weekLabel)"
        }

        val totalHours = totalStudyMinutes / 60
        val remMins = totalStudyMinutes % 60
        val targetHours = totalTargetMinutes / 60
        val targetRemMins = totalTargetMinutes % 60

        val formattedTotalTime = if (totalHours > 0) "${totalHours}h ${remMins}m" else "${remMins}m"
        val formattedTargetTime = if (targetHours > 0) "${targetHours}h ${targetRemMins}m" else "${targetRemMins}m"
        val formattedAvgTime = DateTimeUtils.formatDurationMinutes(dailyAverageMinutes)

        // Day-by-day table lines
        val dailyLines = dailyBreakdowns.joinToString("\n") { day ->
            val statusIcon = if (day.isGoalMet) "✅ Goal Met" else if (day.studiedMinutes > 0) "⏳ In Progress" else "⚪ No Activity"
            val timeStr = DateTimeUtils.formatDurationMinutes(day.studiedMinutes).padEnd(6)
            val targetStr = "(Goal: ${DateTimeUtils.formatDurationMinutes(day.targetMinutes)})".padEnd(14)
            val taskStr = if (day.totalTasks > 0) "• Tasks: ${day.completedTasks}/${day.totalTasks}" else ""
            "  • ${day.dayName.take(3)}, ${DateTimeUtils.formatShortDate(day.dateIso).padEnd(6)} : $timeStr $targetStr $statusIcon $taskStr"
        }

        // Subject breakdown lines
        val subjectLines = if (subjectBreakdowns.isNotEmpty()) {
            subjectBreakdowns.joinToString("\n") { sub ->
                val timeStr = DateTimeUtils.formatDurationMinutes(sub.studiedMinutes)
                val targetInfo = if (sub.targetMinutes > 0) {
                    val targetStr = DateTimeUtils.formatDurationMinutes(sub.targetMinutes)
                    val status = if (sub.isTargetMet) "🎯 Target Achieved" else "${(sub.studiedMinutes * 100 / sub.targetMinutes)}%"
                    "/ $targetStr ($status)"
                } else ""
                val percentStr = String.format(Locale.getDefault(), "%.1f%%", sub.percentageOfTotal)
                "  • ${sub.subjectName}: $timeStr ($percentStr of total) $targetInfo • ${sub.sessionCount} sessions"
            }
        } else {
            "  • No subject sessions recorded for this period"
        }

        // Motivational evaluation
        val ratingMessage = when {
            goalCompletionRatePercent >= 85f || targetMinutesAchievementPercent >= 100f ->
                "🎉 Outstanding achievement! You exceeded your weekly study goals and built tremendous momentum."
            goalCompletionRatePercent >= 60f || targetMinutesAchievementPercent >= 75f ->
                "👏 Solid effort this week! You maintained great consistency across your target subjects."
            totalStudyMinutes > 0 ->
                "💪 Good start! Keep reviewing your schedule to hit your daily targets consistently next week."
            else ->
                "🚀 Ready to begin! Plan your upcoming week and start tracking to build a strong study habit."
        }

        return """
╔══════════════════════════════════════════════════════════════╗
   📚 StudyTrack Weekly Statistics Summary
   $weekTitle
   Student: $userName | Streak: $currentStreak Days 🔥
╚══════════════════════════════════════════════════════════════╝

📊 KEY WEEKLY METRICS:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
• Total Study Time        : $formattedTotalTime ($totalStudyMinutes mins across $totalSessionsCount sessions)
• Weekly Target Time      : $formattedTargetTime (${String.format(Locale.getDefault(), "%.1f", targetMinutesAchievementPercent)}% achieved)
• Goal Completion Rate    : $daysGoalMetCount of $totalDays days (${String.format(Locale.getDefault(), "%.1f", goalCompletionRatePercent)}%)
• Task Completion Rate    : $tasksCompleted of $tasksTotal tasks (${String.format(Locale.getDefault(), "%.1f", taskCompletionRatePercent)}%)
• Daily Study Average     : $formattedAvgTime / day
• Most Productive Day     : $bestDayName

📅 DAILY BREAKDOWN:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
$dailyLines

📖 SUBJECT PROGRESS & DISTRIBUTION:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
$subjectLines

💡 WEEKLY INSIGHT:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
$ratingMessage

----------------------------------------------------------------
Generated by StudyTrack • Stay Focused, Achieve More
""".trimIndent()
    }
}
