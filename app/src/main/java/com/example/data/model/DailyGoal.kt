package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_goals")
data class DailyGoal(
    @PrimaryKey
    val date: String, // "YYYY-MM-DD"
    val targetMinutes: Int = 180, // Default 3 hours
    val completedMinutes: Int = 0,
    val isGoalMet: Boolean = false
)
