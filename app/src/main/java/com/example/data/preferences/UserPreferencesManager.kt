package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserSettings(
    val defaultDailyGoalMinutes: Int = 180,
    val pomodoroWorkMinutes: Int = 25,
    val pomodoroBreakMinutes: Int = 5,
    val notificationsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val currentStreak: Int = 1,
    val longestStreak: Int = 1,
    val lastStudyDate: String = "",
    val darkModeSetting: String = "SYSTEM" // SYSTEM, LIGHT, DARK
)

class UserPreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("studytrack_prefs", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<UserSettings> = _settingsFlow.asStateFlow()

    private fun loadSettings(): UserSettings {
        return UserSettings(
            defaultDailyGoalMinutes = prefs.getInt("default_daily_goal", 180),
            pomodoroWorkMinutes = prefs.getInt("pomodoro_work", 25),
            pomodoroBreakMinutes = prefs.getInt("pomodoro_break", 5),
            notificationsEnabled = prefs.getBoolean("notifications_enabled", true),
            soundEnabled = prefs.getBoolean("sound_enabled", true),
            vibrationEnabled = prefs.getBoolean("vibration_enabled", true),
            currentStreak = prefs.getInt("current_streak", 1),
            longestStreak = prefs.getInt("longest_streak", 1),
            lastStudyDate = prefs.getString("last_study_date", "") ?: "",
            darkModeSetting = prefs.getString("dark_mode_setting", "SYSTEM") ?: "SYSTEM"
        )
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
        } else if (lastDate.isEmpty()) {
            newStreak = 1
        } else {
            // Graceful streak reset or keep at 1
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
        if (lastDate.isEmpty()) return false
        try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val last = sdf.parse(lastDate) ?: return false
            val today = sdf.parse(todayDate) ?: return false
            val diffMs = today.time - last.time
            val diffDays = diffMs / (1000 * 60 * 60 * 24)
            return diffDays == 1L
        } catch (e: Exception) {
            return false
        }
    }
}
