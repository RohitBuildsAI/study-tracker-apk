package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateTimeUtils {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.getDefault())

    fun getTodayIsoString(): String {
        return isoFormat.format(Date())
    }

    fun formatDisplayDate(dateIso: String): String {
        return try {
            val date = isoFormat.parse(dateIso) ?: return dateIso
            val today = getTodayIsoString()
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterday = isoFormat.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 2)
            val tomorrow = isoFormat.format(cal.time)

            when (dateIso) {
                today -> "Today, " + SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
                yesterday -> "Yesterday, " + SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
                tomorrow -> "Tomorrow, " + SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
                else -> displayDateFormat.format(date)
            }
        } catch (e: Exception) {
            dateIso
        }
    }

    fun formatShortDate(dateIso: String): String {
        return try {
            val date = isoFormat.parse(dateIso) ?: return dateIso
            shortDateFormat.format(date)
        } catch (e: Exception) {
            dateIso
        }
    }

    fun getDayOfWeekShort(dateIso: String): String {
        return try {
            val date = isoFormat.parse(dateIso) ?: return ""
            dayOfWeekFormat.format(date)
        } catch (e: Exception) {
            ""
        }
    }

    fun getGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 4..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..21 -> "Good Evening"
            else -> "Good Night"
        }
    }

    fun formatDurationMinutes(minutes: Int): String {
        if (minutes < 60) {
            return "${minutes}m"
        }
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        return if (remainingMinutes == 0) {
            "${hours}h"
        } else {
            "${hours}h ${remainingMinutes}m"
        }
    }

    fun formatDurationSeconds(totalSeconds: Int): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    fun getPastNDays(n: Int): List<String> {
        val dates = mutableListOf<String>()
        val cal = Calendar.getInstance()
        for (i in (n - 1) downTo 0) {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -i)
            dates.add(isoFormat.format(c.time))
        }
        return dates
    }

    fun formatTime(hour: Int, minute: Int): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time)
    }

    fun parseTimeStringToHourMinute(timeStr: String): Pair<Int, Int>? {
        if (timeStr.isBlank()) return null
        val clean = timeStr.trim().uppercase()
        val formats = listOf(
            SimpleDateFormat("hh:mm a", Locale.getDefault()),
            SimpleDateFormat("h:mm a", Locale.getDefault()),
            SimpleDateFormat("HH:mm", Locale.getDefault()),
            SimpleDateFormat("H:mm", Locale.getDefault()),
            SimpleDateFormat("h:mma", Locale.getDefault()),
            SimpleDateFormat("hh:mma", Locale.getDefault())
        )
        for (format in formats) {
            try {
                val date = format.parse(clean)
                if (date != null) {
                    val cal = Calendar.getInstance()
                    cal.time = date
                    return Pair(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
                }
            } catch (_: Exception) {}
        }
        return null
    }

    fun calculateDurationBetweenTimes(startTimeStr: String, endTimeStr: String): Int? {
        val start = parseTimeStringToHourMinute(startTimeStr) ?: return null
        val end = parseTimeStringToHourMinute(endTimeStr) ?: return null
        val startMins = start.first * 60 + start.second
        var endMins = end.first * 60 + end.second
        if (endMins < startMins) {
            // End time next day or past midnight
            endMins += 24 * 60
        }
        val diff = endMins - startMins
        return if (diff > 0) diff else null
    }

    fun addMinutesToTime(startTimeStr: String, minutesToAdd: Int): String? {
        val start = parseTimeStringToHourMinute(startTimeStr) ?: return null
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, start.first)
        cal.set(Calendar.MINUTE, start.second)
        cal.add(Calendar.MINUTE, minutesToAdd)
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time)
    }

    fun getMonthDays(year: Int, month: Int): List<DateInfo> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, 1)

        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday, etc.

        val days = mutableListOf<DateInfo>()
        
        // Leading blank padding
        val offset = (firstDayOfWeek - 1) // Sunday start
        for (i in 0 until offset) {
            days.add(DateInfo(dayOfMonth = 0, isoString = "", isCurrentMonth = false))
        }

        for (day in 1..maxDay) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            days.add(
                DateInfo(
                    dayOfMonth = day,
                    isoString = isoFormat.format(cal.time),
                    isCurrentMonth = true
                )
            )
        }

        return days
    }
}

data class DateInfo(
    val dayOfMonth: Int,
    val isoString: String,
    val isCurrentMonth: Boolean
)
