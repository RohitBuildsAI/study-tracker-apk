package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.analytics.AddSubjectDialog
import com.example.ui.analytics.AnalyticsScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.navigation.Screen
import com.example.ui.records.RecordsScreen
import com.example.ui.schedule.AddEditTaskDialog
import com.example.ui.schedule.ScheduleScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.StudyTrackTheme
import com.example.ui.timer.ActiveStudyTimerDialog
import com.example.ui.timer.QuickStudyDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyTrackApp(viewModel: StudyViewModel) {
    val navController = rememberNavController()

    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val allSubjects by viewModel.allSubjects.collectAsStateWithLifecycle()
    val tasksForToday by viewModel.tasksForToday.collectAsStateWithLifecycle()
    val tasksForSelectedDate by viewModel.tasksForSelectedDate.collectAsStateWithLifecycle()
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val allDailyGoals by viewModel.allDailyGoals.collectAsStateWithLifecycle()
    val todayGoal by viewModel.todayGoal.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()

    val showAddTaskDialog by viewModel.showAddTaskDialog.collectAsStateWithLifecycle()
    val taskToEdit by viewModel.taskToEdit.collectAsStateWithLifecycle()
    val showAddSubjectDialog by viewModel.showAddSubjectDialog.collectAsStateWithLifecycle()
    val showTimerDialog by viewModel.showTimerDialog.collectAsStateWithLifecycle()
    val activeTimerState by viewModel.activeTimerState.collectAsStateWithLifecycle()

    var showQuickStudyDialog by remember { mutableStateOf(false) }

    // Theme dark mode handling
    val isDark = when (userSettings.darkModeSetting) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    StudyTrackTheme(darkTheme = isDark) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                NavigationBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("main_bottom_nav"),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 0.dp
                ) {
                    Screen.bottomNavScreens.forEach { screen ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            ),
                            modifier = Modifier.testTag(screen.testTag)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Dashboard.route,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(Screen.Dashboard.route) {
                        DashboardScreen(
                            tasks = tasksForToday,
                            dailyGoal = todayGoal,
                            userSettings = userSettings,
                            onAddTaskClick = { viewModel.openAddTaskDialog() },
                            onQuickStudyClick = { showQuickStudyDialog = true },
                            onStartStudy = { task -> viewModel.startStudyForTask(task) },
                            onToggleComplete = { task -> viewModel.toggleTaskStatus(task) },
                            onEditTask = { task -> viewModel.openAddTaskDialog(task) },
                            onDuplicateTask = { task -> viewModel.duplicateTask(task) },
                            onDeleteTask = { task -> viewModel.deleteTask(task) },
                            onNavigateToSchedule = {
                                navController.navigate(Screen.Schedule.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                    composable(Screen.Schedule.route) {
                        ScheduleScreen(
                            tasks = tasksForSelectedDate,
                            selectedDate = selectedDate,
                            onSelectDate = { date -> viewModel.setSelectedDate(date) },
                            onAddTask = { viewModel.openAddTaskDialog() },
                            onStartStudy = { task -> viewModel.startStudyForTask(task) },
                            onToggleComplete = { task -> viewModel.toggleTaskStatus(task) },
                            onEditTask = { task -> viewModel.openAddTaskDialog(task) },
                            onDuplicateTask = { task -> viewModel.duplicateTask(task) },
                            onDeleteTask = { task -> viewModel.deleteTask(task) }
                        )
                    }
                    composable(Screen.Records.route) {
                        RecordsScreen(
                            sessions = allSessions,
                            tasks = allTasks,
                            dailyGoals = allDailyGoals,
                            userSettings = userSettings,
                            selectedCalendarDate = selectedDate,
                            onSelectCalendarDate = { date -> viewModel.setSelectedDate(date) }
                        )
                    }
                    composable(Screen.Analytics.route) {
                        AnalyticsScreen(
                            subjects = allSubjects,
                            sessions = allSessions,
                            tasks = allTasks,
                            dailyGoals = allDailyGoals,
                            userSettings = userSettings,
                            onAddSubjectClick = { viewModel.showAddSubjectDialog.value = true },
                            onDeleteSubject = { subject -> viewModel.deleteSubject(subject) }
                        )
                    }
                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            userSettings = userSettings,
                            onUpdateDailyGoal = { mins -> viewModel.updateTodayGoal(mins) },
                            onUpdatePomodoro = { work, brk -> viewModel.setPomodoroSettings(work, brk) },
                            onToggleNotifications = { enabled -> viewModel.setNotificationsEnabled(enabled) },
                            onToggleSound = { enabled -> viewModel.setSoundEnabled(enabled) },
                            onToggleVibration = { enabled -> viewModel.setVibrationEnabled(enabled) },
                            onSelectDarkMode = { mode -> viewModel.setDarkMode(mode) }
                        )
                    }
                }

                // Add / Edit Task Dialog (Can be opened from Dashboard or Schedule / Task Creation)
                if (showAddTaskDialog) {
                    AddEditTaskDialog(
                        taskToEdit = taskToEdit,
                        subjects = allSubjects,
                        initialDate = selectedDate,
                        onDismiss = { viewModel.closeAddTaskDialog() },
                        onSave = { title, subject, desc, date, start, end, duration, priority, reminder, remMins, isPomodoro ->
                            viewModel.saveTask(
                                title, subject, desc, date, start, end, duration, priority, reminder, remMins, isPomodoro
                            )
                        },
                        onOpenAddSubject = {
                            viewModel.showAddSubjectDialog.value = true
                        }
                    )
                }

                // Quick Study Dialog
                if (showQuickStudyDialog) {
                    QuickStudyDialog(
                        subjects = allSubjects,
                        onDismiss = { showQuickStudyDialog = false },
                        onStart = { subject, duration, isPomodoro ->
                            showQuickStudyDialog = false
                            viewModel.startQuickStudy(subject, duration, isPomodoro)
                        }
                    )
                }

                // Add Subject Dialog
                if (showAddSubjectDialog) {
                    AddSubjectDialog(
                        onDismiss = { viewModel.showAddSubjectDialog.value = false },
                        onSave = { name, colorHex, iconName, targetHours ->
                            viewModel.createSubject(name, colorHex, iconName, targetHours)
                        }
                    )
                }

                // Active Timer Overlay / Fullscreen Modal
                if (showTimerDialog && activeTimerState.isRunning) {
                    ActiveStudyTimerDialog(
                        timerState = activeTimerState,
                        onPause = { viewModel.pauseTimer() },
                        onResume = { viewModel.resumeTimer() },
                        onAddExtraMinutes = { mins -> viewModel.addExtraMinutes(mins) },
                        onStartBreak = { mins -> viewModel.startBreakTimer(mins) },
                        onFinishAndSave = { markCompleted, notes ->
                            viewModel.finishAndSaveTimerSession(markCompleted, notes)
                        },
                        onDismiss = {
                            // Don't kill the session, just allow student to minimize back to view dashboard while timer runs
                            viewModel.showTimerDialog.value = false
                        }
                    )
                }
            }
        }
    }
}
