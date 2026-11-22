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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ReminderScheduler {
    private const val EXTRA_TODO_ID = "extra_todo_id"

    fun schedule(context: Context, todoId: Long, triggerAtEpochMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        val notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        val intent = Intent(context, TodoReminderReceiver::class.java).apply {
            putExtra(EXTRA_TODO_ID, todoId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            todoId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
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
    }

    fun cancel(context: Context, todoId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = pendingIntentFor(context, todoId)
        alarmManager.cancel(pendingIntent)
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

    private fun pendingIntentFor(context: Context, todoId: Long): PendingIntent {
        val intent = Intent(context, TodoReminderReceiver::class.java).apply {
            putExtra(EXTRA_TODO_ID, todoId)
        }
        return PendingIntent.getBroadcast(
            context,
            todoId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
