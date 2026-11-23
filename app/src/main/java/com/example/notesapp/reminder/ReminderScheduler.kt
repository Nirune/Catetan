package com.example.notesapp.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.notesapp.BuildConfig
import com.example.notesapp.data.database.NotesDatabase

object ReminderScheduler {
    private const val EXTRA_TODO_ID = "extra_todo_id"

    fun schedule(context: Context, todoId: Long, triggerAtEpochMillis: Long) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val now = System.currentTimeMillis()
            val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
            val notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            val pendingIntent = createPendingIntent(context, todoId)
            val triggerAtElapsed = triggerAtEpochMillis - System.currentTimeMillis() +
                android.os.SystemClock.elapsedRealtime()
            val api = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canExact) "inexact" else "exact"
            if (BuildConfig.DEBUG) {
                Log.d("Reminder", "schedule id=$todoId triggerAt=$triggerAtEpochMillis deltaMs=${triggerAtEpochMillis - now} api=$api canScheduleExactAlarms=$canExact notificationsEnabled=$notificationsEnabled")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (canExact) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAtElapsed,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAtElapsed,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAtElapsed,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            Log.e("Reminder", "schedule failed id=$todoId", e)
        }
    }

    fun cancel(context: Context, todoId: Long) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pendingIntent = findPendingIntent(context, todoId) ?: return
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        } catch (e: Exception) {
            Log.e("Reminder", "cancel failed id=$todoId", e)
        }
    }

    suspend fun rescheduleAll(context: Context) {
        val database = NotesDatabase.getDatabase(context)
        val pending = database.todoDao().getPendingReminders(System.currentTimeMillis())
        pending.forEach { todo ->
            todo.reminderAt?.let { schedule(context, todo.id, it) }
        }
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        }
    }

    private fun reminderIntent(context: Context, todoId: Long): Intent =
        Intent(context, TodoReminderReceiver::class.java).apply {
            putExtra(EXTRA_TODO_ID, todoId)
        }

    // Creates or updates the PendingIntent used for scheduling.
    private fun createPendingIntent(context: Context, todoId: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            todoId.toInt(),
            reminderIntent(context, todoId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    // Looks up an existing PendingIntent without creating one. Returns null when the
    // todo never had a reminder or the alarm was already cleared.
    private fun findPendingIntent(context: Context, todoId: Long): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            todoId.toInt(),
            reminderIntent(context, todoId),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
}
