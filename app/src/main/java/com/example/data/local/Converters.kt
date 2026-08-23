package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus

class Converters {
    @TypeConverter
    fun fromPriority(priority: TaskPriority): String = priority.name

    @TypeConverter
    fun toPriority(value: String): TaskPriority = try {
        TaskPriority.valueOf(value)
    } catch (e: Exception) {
        TaskPriority.MEDIUM
    }

    @TypeConverter
    fun fromStatus(status: TaskStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): TaskStatus = try {
        TaskStatus.valueOf(value)
    } catch (e: Exception) {
        TaskStatus.NOT_STARTED
    }
}
