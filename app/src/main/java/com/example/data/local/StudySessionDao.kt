package com.example.data.local

import androidx.room.*
import com.example.data.model.StudySession
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions ORDER BY startTimeEpoch DESC")
    fun getAllSessions(): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions WHERE date = :date ORDER BY startTimeEpoch DESC")
    fun getSessionsForDate(date: String): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions WHERE date BETWEEN :startDate AND :endDate ORDER BY startTimeEpoch ASC")
    fun getSessionsBetweenDates(startDate: String, endDate: String): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions WHERE subjectId = :subjectId ORDER BY startTimeEpoch DESC")
    fun getSessionsForSubject(subjectId: Long): Flow<List<StudySession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySession): Long

    @Delete
    suspend fun deleteSession(session: StudySession)

    @Query("DELETE FROM study_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)
}
