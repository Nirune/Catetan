package com.example.notesapp.domain.repository

import com.example.notesapp.data.dao.NoteDao
import com.example.notesapp.data.model.NoteEntity
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {
    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()
    val trashedNotes: Flow<List<NoteEntity>> = noteDao.getTrashedNotes()
    val trashCount: Flow<Int> = noteDao.getTrashCount()

    fun getNoteById(id: Long): Flow<NoteEntity?> = noteDao.getNoteById(id)
    suspend fun getActiveNoteForExport(id: Long): NoteEntity? = noteDao.getActiveNoteForExport(id)

    suspend fun insertNote(note: NoteEntity): Long = noteDao.insertNote(note)

    suspend fun updateNote(note: NoteEntity) = noteDao.updateActiveNote(
        id = note.id,
        title = note.title,
        content = note.content,
        color = note.color,
        updatedAt = note.updatedAt
    )

    suspend fun moveToTrash(note: NoteEntity, deletedAt: Long = System.currentTimeMillis()) =
        noteDao.moveToTrash(note.id, deletedAt)

    suspend fun moveToTrashByIds(ids: List<Long>, deletedAt: Long = System.currentTimeMillis()) =
        noteDao.moveNotesToTrash(ids, deletedAt)

    suspend fun restoreNote(id: Long) = noteDao.restoreNote(id)
    suspend fun permanentlyDeleteNote(id: Long) = noteDao.permanentlyDeleteNote(id)
    suspend fun emptyTrash() = noteDao.emptyTrash()
    suspend fun purgeTrashBefore(cutoff: Long) = noteDao.purgeTrashBefore(cutoff)

    suspend fun togglePin(id: Long) = noteDao.togglePin(id)

    suspend fun getAllNotesForExport(): List<NoteEntity> = noteDao.getAllNotesList()
}
