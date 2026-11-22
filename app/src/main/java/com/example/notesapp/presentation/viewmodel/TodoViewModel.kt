package com.example.notesapp.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.notesapp.data.database.NotesDatabase
import com.example.notesapp.data.model.TodoEntity
import com.example.notesapp.domain.repository.TodoRepository
import com.example.notesapp.reminder.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodoViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TodoRepository

    val allTodos: StateFlow<List<TodoEntity>>

    init {
        val todoDao = NotesDatabase.getDatabase(application).todoDao()
        repository = TodoRepository(todoDao)
        allTodos = repository.allTodos.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun addTodo(text: String) {
        viewModelScope.launch {
            val sortOrder = repository.getNextSortOrder(false)
            val todo = TodoEntity.create(text = text, sortOrder = sortOrder)
            repository.insertTodo(todo)
        }
    }

    fun addTodoWithReminder(text: String, reminderAt: Long) {
        viewModelScope.launch {
            val sortOrder = repository.getNextSortOrder(false)
            val todo = TodoEntity.create(text = text, sortOrder = sortOrder)
            val id = repository.insertTodo(todo)
            if (id != -1L) {
                repository.setReminderAt(id, reminderAt)
                ReminderScheduler.schedule(getApplication(), id, reminderAt)
            }
        }
    }

    fun toggleComplete(todo: TodoEntity) {
        viewModelScope.launch {
            val updated = todo.copy(
                isCompleted = !todo.isCompleted,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateTodo(updated)
            if (updated.isCompleted) {
                ReminderScheduler.cancel(getApplication(), todo.id)
                repository.clearReminderAt(todo.id)
            }
        }
    }

    fun updateTodoText(id: Long, text: String) {
        viewModelScope.launch {
            repository.updateTodoText(id, text)
        }
    }

    fun deleteTodo(todo: TodoEntity) {
        viewModelScope.launch {
            ReminderScheduler.cancel(getApplication(), todo.id)
            repository.deleteTodo(todo)
        }
    }

    fun deleteTodoById(id: Long) {
        viewModelScope.launch {
            ReminderScheduler.cancel(getApplication(), id)
            repository.deleteTodoById(id)
        }
    }

    fun setReminder(id: Long, reminderAt: Long?) {
        viewModelScope.launch {
            repository.setReminderAt(id, reminderAt)
            if (reminderAt != null) {
                ReminderScheduler.schedule(getApplication(), id, reminderAt)
            } else {
                ReminderScheduler.cancel(getApplication(), id)
            }
        }
    }

    fun clearReminder(id: Long) {
        viewModelScope.launch {
            repository.clearReminderAt(id)
            ReminderScheduler.cancel(getApplication(), id)
        }
    }
}
