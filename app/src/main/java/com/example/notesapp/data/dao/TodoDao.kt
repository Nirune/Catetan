package com.example.notesapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.example.notesapp.data.model.TodoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY isCompleted ASC, sortOrder ASC, createdAt DESC")
    fun getAllTodos(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE id = :id")
    fun getTodoById(id: Long): Flow<TodoEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTodo(todo: TodoEntity): Long

    @Update
    suspend fun updateTodo(todo: TodoEntity): Int

    @Delete
    suspend fun deleteTodo(todo: TodoEntity): Int

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteTodoById(id: Long): Int

    @Query("UPDATE todos SET isCompleted = :completed, updatedAt = :updatedAt WHERE id = :id")
    suspend fun toggleComplete(id: Long, completed: Boolean, updatedAt: Long): Int

    @Query("UPDATE todos SET text = :text, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTodoText(id: Long, text: String, updatedAt: Long): Int

    @Query("UPDATE todos SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int): Int

    @Query("SELECT MAX(sortOrder) FROM todos WHERE isCompleted = 0")
    suspend fun getMaxSortOrderActive(): Int?

    @Query("SELECT MAX(sortOrder) FROM todos WHERE isCompleted = 1")
    suspend fun getMaxSortOrderCompleted(): Int?
}