package com.example.notesapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val isCompleted: Boolean = false,
    val createdAt: Long = Instant.now().toEpochMilli(),
    val updatedAt: Long = Instant.now().toEpochMilli(),
    val sortOrder: Int = 0,
    val reminderAt: Long? = null
) {
    companion object {
        fun create(text: String, sortOrder: Int = 0): TodoEntity {
            val now = Instant.now().toEpochMilli()
            return TodoEntity(
                text = text,
                isCompleted = false,
                createdAt = now,
                updatedAt = now,
                sortOrder = sortOrder
            )
        }
    }
}
