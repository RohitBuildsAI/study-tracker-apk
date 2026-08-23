package com.example.data.local

import androidx.room.*
import com.example.data.model.DailyGoal
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyGoalDao {
    @Query("SELECT * FROM daily_goals WHERE date = :date")
    fun getGoalForDate(date: String): Flow<DailyGoal?>

    @Query("SELECT * FROM daily_goals WHERE date = :date")
    suspend fun getGoalForDateDirect(date: String): DailyGoal?

    @Query("SELECT * FROM daily_goals ORDER BY date DESC")
    fun getAllDailyGoals(): Flow<List<DailyGoal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateGoal(goal: DailyGoal)

    @Query("UPDATE daily_goals SET completedMinutes = :completedMinutes, isGoalMet = :isGoalMet WHERE date = :date")
    suspend fun updateGoalProgress(date: String, completedMinutes: Int, isGoalMet: Boolean)
}
