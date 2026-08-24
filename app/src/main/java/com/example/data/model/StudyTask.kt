package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_tasks")
data class StudyTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val subjectId: Long,
    val subjectName: String,
    val subjectColorHex: String,
    val description: String = "",
    val date: String, // Format: "YYYY-MM-DD"
    val startTime: String = "", // e.g. "06:00 PM"
    val endTime: String = "", // e.g. "07:00 PM"
    val targetDurationMinutes: Int = 60,
    val completedDurationMinutes: Int = 0,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val status: TaskStatus = TaskStatus.NOT_STARTED,
    val reminderEnabled: Boolean = false,
    val reminderMinutesBefore: Int = 15,
    val isPomodoro: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
