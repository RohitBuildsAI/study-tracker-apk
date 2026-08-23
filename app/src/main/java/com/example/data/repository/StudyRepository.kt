package com.example.data.repository

import com.example.data.local.DailyGoalDao
import com.example.data.local.StudySessionDao
import com.example.data.local.StudyTaskDao
import com.example.data.local.SubjectDao
import com.example.data.model.DailyGoal
import com.example.data.model.StudySession
import com.example.data.model.StudyTask
import com.example.data.model.Subject
import com.example.data.model.TaskStatus
import com.example.data.preferences.UserPreferencesManager
import kotlinx.coroutines.flow.Flow

class StudyRepository(
    private val subjectDao: SubjectDao,
    private val taskDao: StudyTaskDao,
    private val sessionDao: StudySessionDao,
    private val dailyGoalDao: DailyGoalDao,
    val preferencesManager: UserPreferencesManager
) {
    // Subjects
    val allSubjects: Flow<List<Subject>> = subjectDao.getAllSubjects()
    suspend fun insertSubject(subject: Subject): Long = subjectDao.insertSubject(subject)
    suspend fun updateSubject(subject: Subject) = subjectDao.updateSubject(subject)
    suspend fun deleteSubject(subject: Subject) = subjectDao.deleteSubject(subject)
    suspend fun getSubjectById(id: Long): Subject? = subjectDao.getSubjectById(id)

    // Tasks
    fun getTasksForDate(date: String): Flow<List<StudyTask>> = taskDao.getTasksForDate(date)
    val allTasks: Flow<List<StudyTask>> = taskDao.getAllTasks()
    suspend fun insertTask(task: StudyTask): Long = taskDao.insertTask(task)
    suspend fun updateTask(task: StudyTask) = taskDao.updateTask(task)
    suspend fun deleteTask(task: StudyTask) = taskDao.deleteTask(task)
    suspend fun deleteTaskById(id: Long) = taskDao.deleteTaskById(id)
    suspend fun getTaskById(id: Long): StudyTask? = taskDao.getTaskById(id)
    suspend fun updateTaskStatus(id: Long, status: TaskStatus, completedMinutes: Int) =
        taskDao.updateTaskStatus(id, status, completedMinutes)

    // Sessions
    val allSessions: Flow<List<StudySession>> = sessionDao.getAllSessions()
    fun getSessionsForDate(date: String): Flow<List<StudySession>> = sessionDao.getSessionsForDate(date)
    fun getSessionsBetweenDates(startDate: String, endDate: String): Flow<List<StudySession>> =
        sessionDao.getSessionsBetweenDates(startDate, endDate)
    fun getSessionsForSubject(subjectId: Long): Flow<List<StudySession>> =
        sessionDao.getSessionsForSubject(subjectId)
    suspend fun insertSession(session: StudySession): Long = sessionDao.insertSession(session)
    suspend fun deleteSession(session: StudySession) = sessionDao.deleteSession(session)

    // Daily Goals
    fun getGoalForDate(date: String): Flow<DailyGoal?> = dailyGoalDao.getGoalForDate(date)
    suspend fun getGoalForDateDirect(date: String): DailyGoal? = dailyGoalDao.getGoalForDateDirect(date)
    val allDailyGoals: Flow<List<DailyGoal>> = dailyGoalDao.getAllDailyGoals()
    suspend fun insertOrUpdateGoal(goal: DailyGoal) = dailyGoalDao.insertOrUpdateGoal(goal)
    suspend fun updateGoalProgress(date: String, completedMinutes: Int, isGoalMet: Boolean) =
        dailyGoalDao.updateGoalProgress(date, completedMinutes, isGoalMet)
}
