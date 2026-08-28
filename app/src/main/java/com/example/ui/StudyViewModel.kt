package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.StudyDatabase
import com.example.data.model.DailyGoal
import com.example.data.model.StudySession
import com.example.data.model.StudyTask
import com.example.data.model.Subject
import com.example.data.model.SubjectWeeklyProgress
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.preferences.UserPreferencesManager
import com.example.data.preferences.UserSettings
import com.example.data.repository.StudyRepository
import com.example.ui.timer.ActiveTimerState
import com.example.ui.timer.TimerMode
import com.example.util.DateTimeUtils
import com.example.util.NotificationHelper
import com.example.util.TaskReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudyViewModel(application: Application) : AndroidViewModel(application) {
    private val database = StudyDatabase.getDatabase(application, viewModelScope)
    private val preferencesManager = UserPreferencesManager(application)
    private val repository = StudyRepository(
        subjectDao = database.subjectDao(),
        taskDao = database.studyTaskDao(),
        sessionDao = database.studySessionDao(),
        dailyGoalDao = database.dailyGoalDao(),
        preferencesManager = preferencesManager
    )

    val userSettings: StateFlow<UserSettings> = preferencesManager.settingsFlow

    val allSubjects: StateFlow<List<Subject>> = repository.allSubjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentDate = MutableStateFlow(DateTimeUtils.getTodayIsoString())
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    val todayDateString: String
        get() = _currentDate.value

    private val _selectedDate = MutableStateFlow(DateTimeUtils.getTodayIsoString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val tasksForSelectedDate: StateFlow<List<StudyTask>> = _selectedDate
        .flatMapLatest { date -> repository.getTasksForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val tasksForToday: StateFlow<List<StudyTask>> = _currentDate
        .flatMapLatest { date -> repository.getTasksForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<StudyTask>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessions: StateFlow<List<StudySession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDailyGoals: StateFlow<List<DailyGoal>> = repository.allDailyGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val todayGoal: StateFlow<DailyGoal?> = _currentDate
        .flatMapLatest { date -> repository.getGoalForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val weeklySubjectProgress: StateFlow<List<SubjectWeeklyProgress>> = combine(
        allSubjects,
        allSessions
    ) { subjects, sessions ->
        val weekDates = DateTimeUtils.getCurrentWeekDates()
        val thisWeekSessions = sessions.filter { it.date in weekDates }

        subjects.map { subject ->
            val subSessions = thisWeekSessions.filter {
                it.subjectId == subject.id || it.subjectName.equals(subject.name, ignoreCase = true)
            }
            val totalStudiedMins = subSessions.sumOf { (it.durationSeconds + 59) / 60 }
            val targetMins = (subject.targetHoursPerWeek * 60).toInt().coerceAtLeast(1)
            val ratio = (totalStudiedMins.toFloat() / targetMins.toFloat()).coerceIn(0f, 1f)
            val isMet = totalStudiedMins >= targetMins && targetMins > 0

            SubjectWeeklyProgress(
                subject = subject,
                studiedMinutesThisWeek = totalStudiedMins,
                targetMinutesThisWeek = targetMins,
                progressRatio = ratio,
                isGoalMet = isMet,
                sessionCount = subSessions.size
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state navigation & dialogs
    val activeTab = MutableStateFlow(0) // 0: Home, 1: Schedule, 2: Records, 3: Analytics, 4: Settings
    val showAddTaskDialog = MutableStateFlow(false)
    val taskToEdit = MutableStateFlow<StudyTask?>(null)
    val showAddSubjectDialog = MutableStateFlow(false)
    val showTimerDialog = MutableStateFlow(false)
    val showWeeklySummaryDialog = MutableStateFlow(false)
    val isFocusModeRequested = MutableStateFlow(false)
    val searchQuery = MutableStateFlow("")
    val recordViewMode = MutableStateFlow(0) // 0: History, 1: Calendar

    // Active Timer state
    private val _activeTimerState = MutableStateFlow(ActiveTimerState())
    val activeTimerState: StateFlow<ActiveTimerState> = _activeTimerState.asStateFlow()

    private var timerJob: Job? = null
    private var sessionStartTimeEpoch: Long = 0L

    init {
        NotificationHelper.createNotificationChannel(application)
        checkAndRefreshNewDay()

        // Background monitor for date rollovers (e.g. crossing midnight while app is open)
        viewModelScope.launch {
            while (isActive) {
                delay(30_000) // Check every 30 seconds
                val actualToday = DateTimeUtils.getTodayIsoString()
                if (actualToday != _currentDate.value) {
                    checkAndRefreshNewDay()
                }
            }
        }
    }

    /**
     * Checks if a new day has arrived, refreshes current and selected dates,
     * validates streak expiry, and initializes today's daily goal.
     */
    fun checkAndRefreshNewDay(forceTodaySelected: Boolean = false) {
        val actualToday = DateTimeUtils.getTodayIsoString()
        val previousDate = _currentDate.value
        val isDayChanged = actualToday != previousDate

        if (isDayChanged) {
            _currentDate.value = actualToday
            if (_selectedDate.value == previousDate || forceTodaySelected) {
                _selectedDate.value = actualToday
            }
        } else if (forceTodaySelected) {
            _selectedDate.value = actualToday
        }

        preferencesManager.checkDailyStreak(actualToday)
        ensureTodayGoalInitialized(actualToday)
    }

    private fun ensureTodayGoalInitialized(targetDate: String = DateTimeUtils.getTodayIsoString()) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getGoalForDateDirect(targetDate)
            if (existing == null) {
                val defaultMinutes = userSettings.value.defaultDailyGoalMinutes
                repository.insertOrUpdateGoal(
                    DailyGoal(
                        date = targetDate,
                        targetMinutes = defaultMinutes,
                        completedMinutes = 0,
                        isGoalMet = false
                    )
                )
            }
        }
    }

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
    }

    fun setTab(index: Int) {
        activeTab.value = index
    }

    fun openAddTaskDialog(task: StudyTask? = null) {
        taskToEdit.value = task
        showAddTaskDialog.value = true
    }

    fun openWeeklySummary() {
        showWeeklySummaryDialog.value = true
    }

    fun closeWeeklySummary() {
        showWeeklySummaryDialog.value = false
    }

    fun closeAddTaskDialog() {
        showAddTaskDialog.value = false
        taskToEdit.value = null
    }

    fun saveTask(
        title: String,
        subject: Subject,
        description: String,
        date: String,
        startTime: String,
        endTime: String,
        targetDurationMinutes: Int,
        priority: TaskPriority,
        reminderEnabled: Boolean,
        reminderMinutesBefore: Int,
        isPomodoro: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentEdit = taskToEdit.value
            if (currentEdit != null) {
                val updated = currentEdit.copy(
                    title = title,
                    subjectId = subject.id,
                    subjectName = subject.name,
                    subjectColorHex = subject.colorHex,
                    description = description,
                    date = date,
                    startTime = startTime,
                    endTime = endTime,
                    targetDurationMinutes = targetDurationMinutes,
                    priority = priority,
                    reminderEnabled = reminderEnabled,
                    reminderMinutesBefore = reminderMinutesBefore,
                    isPomodoro = isPomodoro
                )
                repository.updateTask(updated)
                if (reminderEnabled && updated.status != TaskStatus.COMPLETED) {
                    TaskReminderScheduler.scheduleReminder(getApplication(), updated)
                } else {
                    TaskReminderScheduler.cancelReminder(getApplication(), updated.id)
                }
            } else {
                val newTask = StudyTask(
                    title = title,
                    subjectId = subject.id,
                    subjectName = subject.name,
                    subjectColorHex = subject.colorHex,
                    description = description,
                    date = date,
                    startTime = startTime,
                    endTime = endTime,
                    targetDurationMinutes = targetDurationMinutes,
                    priority = priority,
                    reminderEnabled = reminderEnabled,
                    reminderMinutesBefore = reminderMinutesBefore,
                    isPomodoro = isPomodoro,
                    status = TaskStatus.NOT_STARTED
                )
                val insertedId = repository.insertTask(newTask)
                if (reminderEnabled) {
                    val taskWithId = newTask.copy(id = insertedId)
                    TaskReminderScheduler.scheduleReminder(getApplication(), taskWithId)
                }
            }
            withContext(Dispatchers.Main) {
                closeAddTaskDialog()
            }
        }
    }

    fun deleteTask(task: StudyTask) {
        viewModelScope.launch(Dispatchers.IO) {
            TaskReminderScheduler.cancelReminder(getApplication(), task.id)
            repository.deleteTask(task)
        }
    }

    fun duplicateTask(task: StudyTask) {
        viewModelScope.launch(Dispatchers.IO) {
            val duplicate = task.copy(
                id = 0,
                title = "${task.title} (Copy)",
                status = TaskStatus.NOT_STARTED,
                completedDurationMinutes = 0,
                createdAt = System.currentTimeMillis()
            )
            val insertedId = repository.insertTask(duplicate)
            if (duplicate.reminderEnabled) {
                TaskReminderScheduler.scheduleReminder(getApplication(), duplicate.copy(id = insertedId))
            }
        }
    }

    fun toggleTaskReminder(task: StudyTask) {
        viewModelScope.launch(Dispatchers.IO) {
            val newEnabled = !task.reminderEnabled
            val mins = if (task.reminderMinutesBefore > 0) task.reminderMinutesBefore else 15
            val updated = task.copy(
                reminderEnabled = newEnabled,
                reminderMinutesBefore = mins
            )
            repository.updateTask(updated)
            if (newEnabled && updated.status != TaskStatus.COMPLETED) {
                TaskReminderScheduler.scheduleReminder(getApplication(), updated)
            } else {
                TaskReminderScheduler.cancelReminder(getApplication(), task.id)
            }
        }
    }

    fun toggleTaskStatus(task: StudyTask) {
        viewModelScope.launch(Dispatchers.IO) {
            val newStatus = if (task.status == TaskStatus.COMPLETED) {
                TaskStatus.NOT_STARTED
            } else {
                TaskStatus.COMPLETED
            }
            val completedMinutes = if (newStatus == TaskStatus.COMPLETED) {
                if (task.completedDurationMinutes > 0) task.completedDurationMinutes else task.targetDurationMinutes
            } else {
                0
            }
            repository.updateTaskStatus(task.id, newStatus, completedMinutes)
            if (newStatus == TaskStatus.COMPLETED) {
                TaskReminderScheduler.cancelReminder(getApplication(), task.id)
            } else if (task.reminderEnabled) {
                TaskReminderScheduler.scheduleReminder(getApplication(), task.copy(status = newStatus))
            }
            recalculateTodayProgress()
        }
    }

    // Timer Controls
    fun startStudyForTask(task: StudyTask) {
        sessionStartTimeEpoch = System.currentTimeMillis()
        val targetSeconds = task.targetDurationMinutes * 60
        // If task was already completed or completedDuration is full, start fresh (0s).
        // Otherwise, resume from the exact completed minutes!
        val initialElapsedSeconds = if (task.completedDurationMinutes < task.targetDurationMinutes) {
            task.completedDurationMinutes * 60
        } else {
            0
        }

        _activeTimerState.value = ActiveTimerState(
            isRunning = true,
            isPaused = false,
            task = task,
            subjectName = task.subjectName,
            subjectColorHex = task.subjectColorHex,
            mode = if (task.isPomodoro) TimerMode.POMODORO_WORK else TimerMode.TASK_COUNTDOWN,
            elapsedSeconds = initialElapsedSeconds,
            targetSeconds = if (task.isPomodoro) userSettings.value.pomodoroWorkMinutes * 60 else targetSeconds,
            isCompletedDialogShown = false,
            savedStudyTargetSeconds = if (task.isPomodoro) userSettings.value.pomodoroWorkMinutes * 60 else targetSeconds,
            savedStudyElapsedSeconds = initialElapsedSeconds,
            initialTaskCompletedSeconds = initialElapsedSeconds
        )
        showTimerDialog.value = true

        // Update task status to in progress
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTaskStatus(task.id, TaskStatus.IN_PROGRESS, task.completedDurationMinutes)
        }
        startTimerJob()
    }

    fun startQuickStudy(subject: Subject, durationMinutes: Int, isPomodoro: Boolean = false) {
        sessionStartTimeEpoch = System.currentTimeMillis()
        val targetSecs = if (isPomodoro) userSettings.value.pomodoroWorkMinutes * 60 else durationMinutes * 60
        _activeTimerState.value = ActiveTimerState(
            isRunning = true,
            isPaused = false,
            task = null,
            subjectName = subject.name,
            subjectColorHex = subject.colorHex,
            mode = if (isPomodoro) TimerMode.POMODORO_WORK else TimerMode.TASK_COUNTDOWN,
            elapsedSeconds = 0,
            targetSeconds = targetSecs,
            isCompletedDialogShown = false
        )
        showTimerDialog.value = true
        startTimerJob()
    }

    fun startStudyInFocusMode(task: StudyTask) {
        startStudyForTask(task)
        isFocusModeRequested.value = true
    }

    fun openFocusMode() {
        isFocusModeRequested.value = true
        showTimerDialog.value = true
    }

    fun closeFocusMode() {
        isFocusModeRequested.value = false
    }

    private fun startTimerJob() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val current = _activeTimerState.value
                if (current.isRunning && !current.isPaused) {
                    val nextElapsed = current.elapsedSeconds + 1
                    val reachedTarget = nextElapsed >= current.targetSeconds && current.targetSeconds > 0

                    _activeTimerState.value = current.copy(
                        elapsedSeconds = nextElapsed,
                        isCompletedDialogShown = if (reachedTarget && !current.isCompletedDialogShown) true else current.isCompletedDialogShown
                    )

                    if (reachedTarget && !current.isCompletedDialogShown) {
                        onTargetTimeReached()
                    }
                }
            }
        }
    }

    private fun onTargetTimeReached() {
        val app = getApplication<Application>()
        val isBreak = _activeTimerState.value.isBreak
        if (userSettings.value.vibrationEnabled) {
            NotificationHelper.triggerVibration(app)
        }
        if (userSettings.value.notificationsEnabled) {
            val title = _activeTimerState.value.task?.title ?: _activeTimerState.value.subjectName
            if (isBreak) {
                NotificationHelper.showNotification(
                    app,
                    1002,
                    "☕ Break Time is Over!",
                    "Hope you feel refreshed! Ready to start your study countdown?"
                )
            } else {
                NotificationHelper.showNotification(
                    app,
                    1001,
                    "Study Goal Reached! 🎉",
                    "Great job! You completed your target session for $title."
                )
            }
        }
    }

    fun pauseTimer() {
        _activeTimerState.value = _activeTimerState.value.copy(isPaused = true)
    }

    fun resumeTimer() {
        _activeTimerState.value = _activeTimerState.value.copy(isPaused = false)
    }

    fun addExtraMinutes(minutes: Int) {
        val current = _activeTimerState.value
        _activeTimerState.value = current.copy(
            targetSeconds = current.targetSeconds + (minutes * 60),
            isCompletedDialogShown = false
        )
    }

    fun startBreakTimer(breakMinutes: Int = userSettings.value.pomodoroBreakMinutes) {
        val current = _activeTimerState.value
        val isFromStudy = current.mode != TimerMode.POMODORO_BREAK

        // Check if the current study session or interval had reached its target
        val hasCompletedCurrentTarget = isFromStudy && current.targetSeconds > 0 && current.elapsedSeconds >= current.targetSeconds

        val newCumulativeStudy = if (isFromStudy && hasCompletedCurrentTarget) {
            current.cumulativeStudySeconds + current.elapsedSeconds
        } else {
            current.cumulativeStudySeconds
        }

        // If target was reached, next study session will start fresh from 0.
        // If break was taken mid-session, save the current elapsed progress so countdown continues from where it paused!
        val savedElapsed = if (isFromStudy) {
            if (hasCompletedCurrentTarget) 0 else current.elapsedSeconds
        } else {
            current.savedStudyElapsedSeconds
        }

        val savedTarget = if (isFromStudy && current.targetSeconds > 0) {
            current.targetSeconds
        } else if (current.savedStudyTargetSeconds > 0) {
            current.savedStudyTargetSeconds
        } else {
            userSettings.value.pomodoroWorkMinutes * 60
        }

        val savedMode = if (isFromStudy) current.mode else current.savedStudyMode

        _activeTimerState.value = current.copy(
            mode = TimerMode.POMODORO_BREAK,
            elapsedSeconds = 0,
            targetSeconds = breakMinutes * 60,
            isPaused = false,
            isCompletedDialogShown = false,
            savedStudyTargetSeconds = savedTarget,
            savedStudyElapsedSeconds = savedElapsed,
            savedStudyMode = savedMode,
            cumulativeStudySeconds = newCumulativeStudy,
            pomodoroCyclesCompleted = if (hasCompletedCurrentTarget) current.pomodoroCyclesCompleted + 1 else current.pomodoroCyclesCompleted
        )
        startTimerJob()
    }

    fun resumeStudyAfterBreak(workDurationMinutes: Int? = null) {
        val current = _activeTimerState.value
        val isExplicitDuration = workDurationMinutes != null && workDurationMinutes > 0

        val targetSecs = if (isExplicitDuration) {
            workDurationMinutes * 60
        } else if (current.savedStudyTargetSeconds > 0) {
            current.savedStudyTargetSeconds
        } else {
            userSettings.value.pomodoroWorkMinutes * 60
        }

        // If resuming with explicit new duration, start from 0.
        // Otherwise, restore the exact elapsed seconds the user had completed prior to the break!
        val restoredElapsed = if (isExplicitDuration) 0 else current.savedStudyElapsedSeconds

        _activeTimerState.value = current.copy(
            mode = current.savedStudyMode,
            elapsedSeconds = restoredElapsed,
            targetSeconds = targetSecs,
            isPaused = false,
            isCompletedDialogShown = false
        )
        startTimerJob()
    }

    fun finishAndSaveTimerSession(markCompleted: Boolean = true, notes: String = "") {
        val state = _activeTimerState.value
        // Calculate total actual study time across intervals (excluding break time)
        val studySecondsInCurrentInterval = if (state.mode != TimerMode.POMODORO_BREAK) {
            state.elapsedSeconds
        } else {
            state.savedStudyElapsedSeconds
        }
        val totalStudySec = state.cumulativeStudySeconds + studySecondsInCurrentInterval
        
        // Newly studied seconds in this session run (excluding previously saved progress)
        val newSessionStudySec = maxOf(0, totalStudySec - state.initialTaskCompletedSeconds)
        val newSessionMins = if (newSessionStudySec > 0) maxOf(1, (newSessionStudySec + 59) / 60) else 0

        // Total task completion minutes
        val totalTaskCompletedMins = (totalStudySec + 59) / 60

        timerJob?.cancel()
        timerJob = null

        viewModelScope.launch(Dispatchers.IO) {
            val task = state.task
            val subjectId = task?.subjectId ?: 1L
            val subjectName = state.subjectName.ifEmpty { task?.subjectName ?: "Study Session" }
            val subjectColor = state.subjectColorHex.ifEmpty { task?.subjectColorHex ?: "#3B82F6" }

            val now = System.currentTimeMillis()
            val effectiveDurationSec = if (newSessionStudySec > 0) newSessionStudySec else totalStudySec
            val start = if (sessionStartTimeEpoch > 0) sessionStartTimeEpoch else now - (effectiveDurationSec * 1000L)

            val isGoalReached = state.targetSeconds > 0 && totalStudySec >= state.targetSeconds
            val isFinalCompleted = markCompleted || isGoalReached

            // 1. Record session for newly completed study time
            if (effectiveDurationSec > 0) {
                val session = StudySession(
                    taskId = task?.id,
                    taskTitle = task?.title ?: "Quick Study: $subjectName",
                    subjectId = subjectId,
                    subjectName = subjectName,
                    subjectColorHex = subjectColor,
                    date = todayDateString,
                    startTimeEpoch = start,
                    endTimeEpoch = now,
                    durationSeconds = effectiveDurationSec,
                    isCompleted = isFinalCompleted,
                    notes = notes
                )
                repository.insertSession(session)
            }

            // 2. Update task if linked
            if (task != null) {
                val newStatus = if (isFinalCompleted) {
                    TaskStatus.COMPLETED
                } else if (totalTaskCompletedMins > 0) {
                    TaskStatus.IN_PROGRESS
                } else {
                    TaskStatus.NOT_STARTED
                }

                val finalCompletedMins = if (isFinalCompleted && markCompleted && totalTaskCompletedMins < task.targetDurationMinutes) {
                    task.targetDurationMinutes
                } else {
                    totalTaskCompletedMins
                }

                repository.updateTaskStatus(task.id, newStatus, finalCompletedMins)
                if (newStatus == TaskStatus.COMPLETED) {
                    TaskReminderScheduler.cancelReminder(getApplication(), task.id)
                }
            }

            // 3. Recalculate daily goal and streak
            recalculateTodayProgress()

            withContext(Dispatchers.Main) {
                _activeTimerState.value = ActiveTimerState()
                showTimerDialog.value = false
            }
        }
    }

    fun dismissTimerWithoutSaving() {
        timerJob?.cancel()
        timerJob = null
        _activeTimerState.value = ActiveTimerState()
        showTimerDialog.value = false
    }

    private suspend fun recalculateTodayProgress() {
        val todaySessions = repository.getSessionsForDate(todayDateString).firstOrNull() ?: emptyList()
        val totalSessionMins = todaySessions.sumOf { (it.durationSeconds + 59) / 60 }

        val todayTasks = repository.getTasksForDate(todayDateString).firstOrNull() ?: emptyList()
        val completedTasksMins = todayTasks.filter { it.status == TaskStatus.COMPLETED }.sumOf {
            if (it.completedDurationMinutes > 0) it.completedDurationMinutes else it.targetDurationMinutes
        }

        val effectiveCompletedMins = maxOf(totalSessionMins, completedTasksMins)

        val currentGoal = repository.getGoalForDateDirect(todayDateString)
        val targetMins = currentGoal?.targetMinutes ?: userSettings.value.defaultDailyGoalMinutes
        val isMet = effectiveCompletedMins >= targetMins

        repository.insertOrUpdateGoal(
            DailyGoal(
                date = todayDateString,
                targetMinutes = targetMins,
                completedMinutes = effectiveCompletedMins,
                isGoalMet = isMet
            )
        )

        if (effectiveCompletedMins > 0) {
            preferencesManager.recordStudyDate(todayDateString, isMet)
        }
    }

    // Daily Goal updating
    fun updateTodayGoal(targetMinutes: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentGoal = repository.getGoalForDateDirect(todayDateString)
            val completed = currentGoal?.completedMinutes ?: 0
            val isMet = completed >= targetMinutes
            repository.insertOrUpdateGoal(
                DailyGoal(
                    date = todayDateString,
                    targetMinutes = targetMinutes,
                    completedMinutes = completed,
                    isGoalMet = isMet
                )
            )
            preferencesManager.updateDefaultDailyGoal(targetMinutes)
        }
    }

    // Subject Management
    fun createSubject(name: String, colorHex: String, iconName: String, targetHoursPerWeek: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            val subject = Subject(
                name = name,
                colorHex = colorHex,
                iconName = iconName,
                isCustom = true,
                targetHoursPerWeek = targetHoursPerWeek
            )
            repository.insertSubject(subject)
            withContext(Dispatchers.Main) {
                showAddSubjectDialog.value = false
            }
        }
    }

    fun updateSubject(subject: Subject) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateSubject(subject)
        }
    }

    fun updateSubjectTargetHours(subjectId: Long, targetHours: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSubjectById(subjectId)
            if (current != null) {
                repository.updateSubject(current.copy(targetHoursPerWeek = targetHours))
            }
        }
    }

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSubject(subject)
        }
    }

    // Settings actions
    fun setPomodoroSettings(workMinutes: Int, breakMinutes: Int) {
        preferencesManager.updatePomodoroSettings(workMinutes, breakMinutes)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        preferencesManager.updateNotificationsEnabled(enabled)
    }

    fun setSoundEnabled(enabled: Boolean) {
        preferencesManager.updateSoundEnabled(enabled)
    }

    fun setVibrationEnabled(enabled: Boolean) {
        preferencesManager.updateVibrationEnabled(enabled)
    }

    fun setDarkMode(mode: String) {
        preferencesManager.updateDarkMode(mode)
    }

    fun setTimerCountdownStyle(styleName: String) {
        preferencesManager.updateTimerCountdownStyle(styleName)
    }

    fun setUserName(name: String) {
        preferencesManager.updateUserName(name)
    }
}
