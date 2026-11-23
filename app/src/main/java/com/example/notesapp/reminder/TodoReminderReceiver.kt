package com.example.notesapp.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.notesapp.BuildConfig
import com.example.notesapp.data.database.NotesDatabase
import com.example.notesapp.domain.repository.TodoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TodoReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val resultPendingIntent = goAsync()
        val todoId = intent.getLongExtra(EXTRA_TODO_ID, -1L)
        if (BuildConfig.DEBUG) Log.d("Reminder", "receive id=$todoId actualAt=${System.currentTimeMillis()}")
        if (todoId == -1L) {
            resultPendingIntent.finish()
            return
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            val repository = TodoRepository(NotesDatabase.getDatabase(context).todoDao())
            val todo = repository.getTodoByIdImmediate(todoId)
            if (BuildConfig.DEBUG) Log.d("Reminder", "database lookup id=$todoId found=${todo != null} completed=${todo?.isCompleted}")
            if (todo != null && !todo.isCompleted) {
                NotificationHelper.showTodoReminder(context, todoId, todo.text)
            }
            repository.clearReminderAt(todoId)
            resultPendingIntent.finish()
        }
    }

    companion object {
        const val EXTRA_TODO_ID = "extra_todo_id"
    }
}
