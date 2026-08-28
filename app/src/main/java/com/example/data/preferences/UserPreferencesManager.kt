package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserSettings(
    val userName: String = "Alex",
    val defaultDailyGoalMinutes: Int = 180,
    val pomodoroWorkMinutes: Int = 25,
    val pomodoroBreakMinutes: Int = 5,
    val notificationsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val currentStreak: Int = 1,
    val longestStreak: Int = 1,
    val lastStudyDate: String = "",
    val darkModeSetting: String = "SYSTEM", // SYSTEM, LIGHT, DARK
    val timerCountdownStyle: String = "CleanDigital", // FlipClock, RetroSplit, VintageTick, ChronosAnalog, GhostOutline, CleanDigital
    val offlineModeAlwaysActive: Boolean = true
)

class UserPreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("studytrack_prefs", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<UserSettings> = _settingsFlow.asStateFlow()

    private fun loadSettings(): UserSettings {
        return UserSettings(
            userName = prefs.getString("user_name", "Alex") ?: "Alex",
            defaultDailyGoalMinutes = prefs.getInt("default_daily_goal", 180),
            pomodoroWorkMinutes = prefs.getInt("pomodoro_work", 25),
            pomodoroBreakMinutes = prefs.getInt("pomodoro_break", 5),
            notificationsEnabled = prefs.getBoolean("notifications_enabled", true),
            soundEnabled = prefs.getBoolean("sound_enabled", true),
            vibrationEnabled = prefs.getBoolean("vibration_enabled", true),
            currentStreak = prefs.getInt("current_streak", 1),
            longestStreak = prefs.getInt("longest_streak", 1),
            lastStudyDate = prefs.getString("last_study_date", "") ?: "",
            darkModeSetting = prefs.getString("dark_mode_setting", "SYSTEM") ?: "SYSTEM",
            timerCountdownStyle = prefs.getString("timer_countdown_style", "CleanDigital") ?: "CleanDigital",
            offlineModeAlwaysActive = true
        )
    }

    fun updateUserName(name: String) {
        val trimmed = name.trim().ifEmpty { "Alex" }
        prefs.edit().putString("user_name", trimmed).apply()
        _settingsFlow.value = _settingsFlow.value.copy(userName = trimmed)
    }

    fun updateDefaultDailyGoal(minutes: Int) {
        prefs.edit().putInt("default_daily_goal", minutes).apply()
        _settingsFlow.value = _settingsFlow.value.copy(defaultDailyGoalMinutes = minutes)
    }

    fun updatePomodoroSettings(workMinutes: Int, breakMinutes: Int) {
        prefs.edit()
            .putInt("pomodoro_work", workMinutes)
            .putInt("pomodoro_break", breakMinutes)
            .apply()
        _settingsFlow.value = _settingsFlow.value.copy(
            pomodoroWorkMinutes = workMinutes,
            pomodoroBreakMinutes = breakMinutes
        )
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(notificationsEnabled = enabled)
    }

    fun updateSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("sound_enabled", enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(soundEnabled = enabled)
    }

    fun updateVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("vibration_enabled", enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(vibrationEnabled = enabled)
    }

    fun updateDarkMode(mode: String) {
        prefs.edit().putString("dark_mode_setting", mode).apply()
        _settingsFlow.value = _settingsFlow.value.copy(darkModeSetting = mode)
    }

    fun updateTimerCountdownStyle(styleName: String) {
        prefs.edit().putString("timer_countdown_style", styleName).apply()
        _settingsFlow.value = _settingsFlow.value.copy(timerCountdownStyle = styleName)
    }

    fun checkDailyStreak(todayDate: String) {
        val currentSettings = _settingsFlow.value
        val lastDate = currentSettings.lastStudyDate
        if (lastDate.isEmpty() || lastDate == todayDate) {
            return
        }

        val isConsecutive = isYesterday(lastDate, todayDate)
        if (!isConsecutive) {
            // More than 1 day has passed without studying -> reset current streak to 0
            prefs.edit().putInt("current_streak", 0).apply()
            _settingsFlow.value = currentSettings.copy(currentStreak = 0)
        }
    }

    fun recordStudyDate(todayDate: String, goalMet: Boolean) {
        val currentSettings = _settingsFlow.value
        val lastDate = currentSettings.lastStudyDate

        if (lastDate == todayDate) {
            // Already recorded today
            return
        }

        var newStreak = currentSettings.currentStreak
        // Check if yesterday
        val isConsecutive = isYesterday(lastDate, todayDate)
        if (isConsecutive) {
            newStreak += 1
        } else {
            // Streak started or reset
            newStreak = 1
        }

        val newLongest = maxOf(newStreak, currentSettings.longestStreak)

        prefs.edit()
            .putInt("current_streak", newStreak)
            .putInt("longest_streak", newLongest)
            .putString("last_study_date", todayDate)
            .apply()

        _settingsFlow.value = currentSettings.copy(
            currentStreak = newStreak,
            longestStreak = newLongest,
            lastStudyDate = todayDate
        )
    }

    private fun isYesterday(lastDate: String, todayDate: String): Boolean {
        if (lastDate.isEmpty() || todayDate.isEmpty()) return false
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val lastParsed = sdf.parse(lastDate) ?: return false
            val todayParsed = sdf.parse(todayDate) ?: return false

            val calLast = java.util.Calendar.getInstance().apply {
                time = lastParsed
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
                add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
            val calToday = java.util.Calendar.getInstance().apply {
                time = todayParsed
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }

            calLast.get(java.util.Calendar.YEAR) == calToday.get(java.util.Calendar.YEAR) &&
            calLast.get(java.util.Calendar.DAY_OF_YEAR) == calToday.get(java.util.Calendar.DAY_OF_YEAR)
        } catch (e: Exception) {
            false
        }
    }
}
