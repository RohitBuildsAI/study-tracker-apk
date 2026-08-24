package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.StudyDatabase
import com.example.data.model.TaskStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class TaskReminderReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_TASK_ID = "EXTRA_TASK_ID"
        const val EXTRA_TASK_TITLE = "EXTRA_TASK_TITLE"
        const val EXTRA_SUBJECT_NAME = "EXTRA_SUBJECT_NAME"
        const val EXTRA_START_TIME = "EXTRA_START_TIME"
        const val EXTRA_REMINDER_MINS = "EXTRA_REMINDER_MINS"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            // Reschedule active reminders after device reboot
            val scope = CoroutineScope(Dispatchers.IO)
            val db = StudyDatabase.getDatabase(context, scope)
            scope.launch {
                val tasks = db.studyTaskDao().getAllTasks().firstOrNull() ?: emptyList()
                val activeTasks = tasks.filter { it.reminderEnabled && it.status != TaskStatus.COMPLETED }
                TaskReminderScheduler.rescheduleAll(context, activeTasks)
            }
            return
        }

        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Study Session"
        val subjectName = intent.getStringExtra(EXTRA_SUBJECT_NAME) ?: "Subject"
        val startTime = intent.getStringExtra(EXTRA_START_TIME) ?: ""
        val reminderMinutes = intent.getIntExtra(EXTRA_REMINDER_MINS, 15)

        if (taskId != -1L) {
            NotificationHelper.showTaskReminderNotification(
                context = context,
                taskId = taskId,
                taskTitle = taskTitle,
                subjectName = subjectName,
                startTime = startTime,
                reminderMinutesBefore = reminderMinutes
            )
        }
    }
}
