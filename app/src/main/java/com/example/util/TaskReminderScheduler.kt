package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.model.StudyTask
import com.example.data.model.TaskStatus

object TaskReminderScheduler {

    /**
     * Schedules an alarm reminder for a specific StudyTask.
     * Fires [reminderMinutesBefore] minutes (default 15) prior to the task's scheduled startTime.
     */
    fun scheduleReminder(context: Context, task: StudyTask) {
        if (!task.reminderEnabled || task.status == TaskStatus.COMPLETED || task.startTime.isBlank() || task.date.isBlank()) {
            cancelReminder(context, task.id)
            return
        }

        val triggerMillis = DateTimeUtils.calculateReminderTriggerMillis(
            dateIso = task.date,
            startTimeStr = task.startTime,
            reminderMinutesBefore = task.reminderMinutesBefore
        ) ?: return

        val now = System.currentTimeMillis()
        if (triggerMillis <= now) {
            // Target reminder time has already passed
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            putExtra(TaskReminderReceiver.EXTRA_TASK_ID, task.id)
            putExtra(TaskReminderReceiver.EXTRA_TASK_TITLE, task.title)
            putExtra(TaskReminderReceiver.EXTRA_SUBJECT_NAME, task.subjectName)
            putExtra(TaskReminderReceiver.EXTRA_START_TIME, task.startTime)
            putExtra(TaskReminderReceiver.EXTRA_REMINDER_MINS, task.reminderMinutesBefore)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        } catch (se: SecurityException) {
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } catch (_: Exception) {}
        } catch (_: Exception) {}
    }

    /**
     * Cancels any active alarm reminder for the given taskId.
     */
    fun cancelReminder(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TaskReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Reschedules reminders for all active tasks with reminderEnabled = true.
     */
    fun rescheduleAll(context: Context, tasks: List<StudyTask>) {
        tasks.filter { it.reminderEnabled && it.status != TaskStatus.COMPLETED }.forEach { task ->
            scheduleReminder(context, task)
        }
    }
}
