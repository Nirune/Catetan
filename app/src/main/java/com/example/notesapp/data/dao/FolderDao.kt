package com.example.notesapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.notesapp.data.model.FolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY createdAt ASC, id ASC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Insert
    suspend fun insertFolder(folder: FolderEntity): Long

    @Query("UPDATE folders SET name = :name WHERE id = :id")
    suspend fun renameFolder(id: Long, name: String): Int

    @Query("UPDATE notes SET folderId = NULL WHERE folderId = :id")
    suspend fun detachNotes(id: Long): Int

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteFolder(id: Long): Int

    @Query("SELECT COUNT(*) FROM folders WHERE id != :exceptId AND name = :name COLLATE NOCASE")
    suspend fun countByName(name: String, exceptId: Long): Int

    @Query("SELECT COUNT(*) FROM folders WHERE name = :name COLLATE NOCASE")
    suspend fun countByName(name: String): Int
}
