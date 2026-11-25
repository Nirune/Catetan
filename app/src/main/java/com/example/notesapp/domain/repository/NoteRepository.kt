package com.example.notesapp.domain.repository

import com.example.notesapp.data.dao.NoteDao
import com.example.notesapp.data.dao.FolderDao
import com.example.notesapp.data.model.FolderEntity
import com.example.notesapp.data.model.NoteEntity
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao, private val folderDao: FolderDao? = null) {
    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()
    val trashedNotes: Flow<List<NoteEntity>> = noteDao.getTrashedNotes()
    val trashCount: Flow<Int> = noteDao.getTrashCount()

    fun getNotesInFolder(folderId: Long?): Flow<List<NoteEntity>> = noteDao.getNotesInFolder(folderId)
    val folders: Flow<List<FolderEntity>> get() = requireNotNull(folderDao).getAllFolders()
    suspend fun createFolder(folder: FolderEntity): Long = requireNotNull(folderDao).insertFolder(folder)
    suspend fun renameFolder(id: Long, name: String): Int = requireNotNull(folderDao).renameFolder(id, name)
    suspend fun folderNameCount(name: String): Int = requireNotNull(folderDao).countByName(name)
    suspend fun folderNameCount(name: String, exceptId: Long): Int = requireNotNull(folderDao).countByName(name, exceptId)
    suspend fun deleteFolder(id: Long): Int {
        requireNotNull(folderDao).detachNotes(id)
        return requireNotNull(folderDao).deleteFolder(id)
    }
    suspend fun moveNotesToFolder(ids: List<Long>, targetFolderId: Long?): Int =
        noteDao.moveNotesToFolder(ids, targetFolderId)

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

    suspend fun updateNoteWithFolder(note: NoteEntity) = noteDao.updateActiveNoteWithFolder(
        id = note.id,
        title = note.title,
        content = note.content,
        color = note.color,
        updatedAt = note.updatedAt,
        folderId = note.folderId
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
