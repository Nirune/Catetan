package com.example.notesapp.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import com.example.notesapp.data.database.NotesDatabase
import com.example.notesapp.data.model.TodoEntity
import com.example.notesapp.domain.repository.TodoRepository

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

    fun addTodoAndReturn(text: String, onResult: (TodoEntity) -> Unit) {
        viewModelScope.launch {
            val sortOrder = repository.getNextSortOrder(false)
            val todo = TodoEntity.create(text = text, sortOrder = sortOrder)
            val id = repository.insertTodo(todo)
            onResult(todo.copy(id = id))
        }
    }

    fun toggleComplete(todo: TodoEntity) {
        viewModelScope.launch {
            val updated = todo.copy(
                isCompleted = !todo.isCompleted,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateTodo(updated)
        }
    }

    fun updateTodoText(id: Long, text: String) {
        viewModelScope.launch {
            repository.updateTodoText(id, text)
        }
    }

    fun deleteTodo(todo: TodoEntity) {
        viewModelScope.launch {
            repository.deleteTodo(todo)
        }
    }

    fun deleteTodoById(id: Long) {
        viewModelScope.launch {
            repository.deleteTodoById(id)
        }
    }
}