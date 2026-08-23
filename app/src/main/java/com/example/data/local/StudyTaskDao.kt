package com.example.data.local

import androidx.room.*
import com.example.data.model.StudyTask
import com.example.data.model.TaskStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyTaskDao {
    @Query("SELECT * FROM study_tasks WHERE date = :date ORDER BY priority DESC, id ASC")
    fun getTasksForDate(date: String): Flow<List<StudyTask>>

    @Query("SELECT * FROM study_tasks ORDER BY date DESC, id DESC")
    fun getAllTasks(): Flow<List<StudyTask>>

    @Query("SELECT * FROM study_tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): StudyTask?

    @Query("SELECT * FROM study_tasks WHERE status = :status")
    fun getTasksByStatus(status: TaskStatus): Flow<List<StudyTask>>

    @Query("SELECT * FROM study_tasks WHERE subjectId = :subjectId")
    fun getTasksForSubject(subjectId: Long): Flow<List<StudyTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: StudyTask): Long

    @Update
    suspend fun updateTask(task: StudyTask)

    @Delete
    suspend fun deleteTask(task: StudyTask)

    @Query("DELETE FROM study_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("UPDATE study_tasks SET status = :status, completedDurationMinutes = :completedMinutes WHERE id = :id")
    suspend fun updateTaskStatus(id: Long, status: TaskStatus, completedMinutes: Int)
}
