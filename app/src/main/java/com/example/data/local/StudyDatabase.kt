package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.DailyGoal
import com.example.data.model.StudySession
import com.example.data.model.StudyTask
import com.example.data.model.Subject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Subject::class,
        StudyTask::class,
        StudySession::class,
        DailyGoal::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class StudyDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun studyTaskDao(): StudyTaskDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun dailyGoalDao(): DailyGoalDao

    companion object {
        @Volatile
        private var INSTANCE: StudyDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): StudyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudyDatabase::class.java,
                    "study_track_database"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }

            private suspend fun populateInitialData(database: StudyDatabase) {
                val subjectDao = database.subjectDao()
                val initialSubjects = listOf(
                    Subject(name = "Mathematics", colorHex = "#3B82F6", iconName = "calculate", isCustom = false, targetHoursPerWeek = 7f),
                    Subject(name = "Science", colorHex = "#10B981", iconName = "science", isCustom = false, targetHoursPerWeek = 6f),
                    Subject(name = "English", colorHex = "#8B5CF6", iconName = "menu_book", isCustom = false, targetHoursPerWeek = 4f),
                    Subject(name = "Kannada", colorHex = "#F59E0B", iconName = "translate", isCustom = false, targetHoursPerWeek = 3f),
                    Subject(name = "Social Science", colorHex = "#EF4444", iconName = "public", isCustom = false, targetHoursPerWeek = 4f),
                    Subject(name = "Computer Science", colorHex = "#06B6D4", iconName = "code", isCustom = false, targetHoursPerWeek = 5f)
                )
                subjectDao.insertSubjects(initialSubjects)
            }
        }
    }
}
