package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_sessions")
data class StudySession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long? = null,
    val taskTitle: String = "",
    val subjectId: Long,
    val subjectName: String,
    val subjectColorHex: String,
    val date: String, // "YYYY-MM-DD"
    val startTimeEpoch: Long,
    val endTimeEpoch: Long,
    val durationSeconds: Int,
    val isCompleted: Boolean = true,
    val notes: String = ""
)
