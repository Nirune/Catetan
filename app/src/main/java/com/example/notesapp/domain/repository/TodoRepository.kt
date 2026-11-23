package com.example.notesapp.domain.repository

import com.example.notesapp.data.dao.TodoDao
import com.example.notesapp.data.model.TodoEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class TodoRepository(private val todoDao: TodoDao) {
    val allTodos: Flow<List<TodoEntity>> = todoDao.getAllTodos()

    fun getTodoById(id: Long): Flow<TodoEntity?> = todoDao.getTodoById(id)

    suspend fun getPendingReminders(now: Long): List<TodoEntity> = todoDao.getPendingReminders(now)

    suspend fun getTodoByIdImmediate(id: Long): TodoEntity? {
        return todoDao.getTodoById(id).firstOrNull()
    }

    suspend fun insertTodo(todo: TodoEntity): Long = todoDao.insertTodo(todo)

    suspend fun updateTodo(todo: TodoEntity) = todoDao.updateTodo(todo)

    suspend fun deleteTodo(todo: TodoEntity) = todoDao.deleteTodo(todo)

    suspend fun deleteTodoById(id: Long) = todoDao.deleteTodoById(id)

    suspend fun toggleComplete(id: Long, completed: Boolean) {
        val updatedAt = System.currentTimeMillis()
        todoDao.toggleComplete(id, completed, updatedAt)
    }

    suspend fun updateTodoText(id: Long, text: String) {
        val updatedAt = System.currentTimeMillis()
        todoDao.updateTodoText(id, text, updatedAt)
    }

    suspend fun updateSortOrder(id: Long, sortOrder: Int) = todoDao.updateSortOrder(id, sortOrder)

    suspend fun setReminderAt(id: Long, reminderAt: Long?) {
        val updatedAt = System.currentTimeMillis()
        todoDao.setReminderAt(id, reminderAt, updatedAt)
    }

    suspend fun clearReminderAt(id: Long) {
        val updatedAt = System.currentTimeMillis()
        todoDao.setReminderAt(id, null, updatedAt)
    }

    suspend fun getNextSortOrder(isCompleted: Boolean): Int {
        val maxOrder = if (isCompleted) {
            todoDao.getMaxSortOrderCompleted()
        } else {
            todoDao.getMaxSortOrderActive()
        }
        return (maxOrder ?: -1) + 1
    }
}
