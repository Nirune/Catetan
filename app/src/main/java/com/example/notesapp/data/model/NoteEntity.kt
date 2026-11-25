package com.example.notesapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val createdAt: Long = Instant.now().toEpochMilli(),
    val updatedAt: Long = Instant.now().toEpochMilli(),
    val isPinned: Boolean = false,
    val color: Int = 0xFFFFFFFF.toInt(), // White default
    val deletedAt: Long? = null
) {
    companion object {
        fun create(title: String, content: String, color: Int = 0xFFFFFFFF.toInt()): NoteEntity {
            val now = Instant.now().toEpochMilli()
            return NoteEntity(
                title = title,
                content = content,
                createdAt = now,
                updatedAt = now,
                color = color
            )
        }
    }
}
