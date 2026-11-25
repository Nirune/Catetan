package com.example.notesapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.notesapp.data.model.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE deletedAt IS NULL ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id AND deletedAt IS NULL")
    fun getNoteById(id: Long): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :id AND deletedAt IS NULL")
    suspend fun getActiveNoteForExport(id: Long): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity): Int

    @Query("UPDATE notes SET deletedAt = :deletedAt, isPinned = 0 WHERE id = :id AND deletedAt IS NULL")
    suspend fun moveToTrash(id: Long, deletedAt: Long): Int

    @Query("UPDATE notes SET deletedAt = :deletedAt, isPinned = 0 WHERE id = :id AND deletedAt IS NULL")
    suspend fun moveToTrashById(id: Long, deletedAt: Long): Int

    @Query("UPDATE notes SET deletedAt = :deletedAt, isPinned = 0 WHERE id IN (:ids) AND deletedAt IS NULL")
    suspend fun moveNotesToTrash(ids: List<Long>, deletedAt: Long): Int

    @Query("SELECT * FROM notes WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun getTrashedNotes(): Flow<List<NoteEntity>>

    @Query("UPDATE notes SET deletedAt = NULL WHERE id = :id AND deletedAt IS NOT NULL")
    suspend fun restoreNote(id: Long): Int

    @Query("DELETE FROM notes WHERE id = :id AND deletedAt IS NOT NULL")
    suspend fun permanentlyDeleteNote(id: Long): Int

    @Query("DELETE FROM notes WHERE deletedAt IS NOT NULL")
    suspend fun emptyTrash(): Int

    @Query("DELETE FROM notes WHERE deletedAt IS NOT NULL AND deletedAt < :cutoff")
    suspend fun purgeTrashBefore(cutoff: Long): Int

    @Query("UPDATE notes SET title = :title, content = :content, color = :color, updatedAt = :updatedAt WHERE id = :id AND deletedAt IS NULL")
    suspend fun updateActiveNote(id: Long, title: String, content: String, color: Int, updatedAt: Long): Int

    @Query("UPDATE notes SET isPinned = NOT isPinned WHERE id = :id AND deletedAt IS NULL")
    suspend fun togglePin(id: Long): Int

    @Query("SELECT COUNT(*) FROM notes WHERE deletedAt IS NULL")
    suspend fun getNotesCount(): Int

    @Query("SELECT * FROM notes WHERE deletedAt IS NULL ORDER BY isPinned DESC, updatedAt DESC")
    suspend fun getAllNotesList(): List<NoteEntity>

    @Query("SELECT COUNT(*) FROM notes WHERE deletedAt IS NOT NULL")
    fun getTrashCount(): Flow<Int>
}
