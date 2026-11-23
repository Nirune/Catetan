package com.example.notesapp.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.notesapp.MainActivity
import com.example.notesapp.R
import com.example.notesapp.BuildConfig

object NotificationHelper {
    private const val CHANNEL_ID = "todo_reminder"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.todo_reminder_notif_channel),
                    NotificationManager.IMPORTANCE_HIGH
                )
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun showTodoReminder(context: Context, todoId: Long, text: String) {
        ensureChannel(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val importance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.getNotificationChannel(CHANNEL_ID)?.importance ?: -1
        } else {
            NotificationManager.IMPORTANCE_DEFAULT
        }
        val notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(MainActivity.EXTRA_OPEN_TAB, "todo")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            todoId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.todo_reminder_notif_title))
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        val manager = NotificationManagerCompat.from(context)
        if (manager.areNotificationsEnabled()) {
            manager.notify(todoId.toInt(), notification)
            if (BuildConfig.DEBUG) Log.d("Reminder", "notify id=$todoId channel=$CHANNEL_ID importance=$importance notificationsEnabled=$notificationsEnabled result=posted")
        } else if (BuildConfig.DEBUG) {
            Log.d("Reminder", "notify id=$todoId channel=$CHANNEL_ID importance=$importance notificationsEnabled=$notificationsEnabled result=blocked")
        }
    }
}
