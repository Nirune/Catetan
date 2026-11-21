package com.example.notesapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.example.notesapp.data.model.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNoteById(id: Long): Flow<NoteEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity): Int

    @Delete
    suspend fun deleteNote(note: NoteEntity): Int

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long): Int

    @Query("DELETE FROM notes WHERE id IN (:ids)")
    suspend fun deleteNotesByIds(ids: List<Long>): Int

    @Query("UPDATE notes SET isPinned = NOT isPinned WHERE id = :id")
    suspend fun togglePin(id: Long): Int

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun getNotesCount(): Int

    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    suspend fun getAllNotesList(): List<NoteEntity>
}