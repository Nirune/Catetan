package com.example.notesapp.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.notesapp.data.database.NotesDatabase
import com.example.notesapp.domain.repository.TodoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }
        val resultPendingIntent = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            val repository = TodoRepository(NotesDatabase.getDatabase(context).todoDao())
            val now = System.currentTimeMillis()
            val pending = repository.getPendingReminders(now)
            val missed = pending.filter { it.reminderAt != null && it.reminderAt <= now }
            val upcoming = pending.filter { it.reminderAt != null && it.reminderAt > now }
            missed.take(MAX_MISSED_NOTIFICATIONS).forEach { todo ->
                NotificationHelper.showTodoReminder(context, todo.id, todo.text)
                repository.clearReminderAt(todo.id)
            }
            missed.drop(MAX_MISSED_NOTIFICATIONS).forEach { todo ->
                repository.clearReminderAt(todo.id)
            }
            upcoming.forEach { todo ->
                todo.reminderAt?.let { ReminderScheduler.schedule(context, todo.id, it) }
            }
            resultPendingIntent.finish()
        }
    }

    companion object {
        private const val MAX_MISSED_NOTIFICATIONS = 3
    }
}
