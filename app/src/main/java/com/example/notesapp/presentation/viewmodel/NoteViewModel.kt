package com.example.notesapp.presentation.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.notesapp.data.database.NotesDatabase
import com.example.notesapp.data.model.NoteEntity
import com.example.notesapp.domain.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.OutputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

sealed interface ExportResult {
    data class Success(val fileName: String, val uri: Uri?) : ExportResult
    data class Error(val message: String) : ExportResult
}

enum class ExportFormat {
    TXT, JSON, MD
}

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: NoteRepository

    val allNotes: StateFlow<List<NoteEntity>>
    val exportResult = MutableStateFlow<ExportResult?>(null)

    init {
        val noteDao = NotesDatabase.getDatabase(application).noteDao()
        repository = NoteRepository(noteDao)
        allNotes = repository.allNotes.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun addNote(title: String, content: String, color: Int = 0xFFFFF9C4.toInt()) {
        viewModelScope.launch {
            val note = NoteEntity.create(title = title, content = content, color = color)
            repository.insertNote(note)
        }
    }

    fun addNoteAndReturn(title: String, content: String, color: Int, onResult: (NoteEntity) -> Unit) {
        viewModelScope.launch {
            val note = NoteEntity.create(title = title, content = content, color = color)
            val id = repository.insertNote(note)
            onResult(note.copy(id = id))
        }
    }

    fun updateNote(note: NoteEntity) {
        viewModelScope.launch {
            val updated = note.copy(updatedAt = System.currentTimeMillis())
            repository.updateNote(updated)
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun togglePin(id: Long) {
        viewModelScope.launch {
            repository.togglePin(id)
        }
    }

    fun exportNotes(format: ExportFormat) {
        exportNotesInternal(listOf(), format, isSingle = false)
    }

    fun exportCtn(customFilename: String? = null) {
        exportCtnInternal(customFilename)
    }

    fun exportSingleNote(note: NoteEntity, format: ExportFormat, customFilename: String? = null) {
        exportNotesInternal(listOf(note), format, isSingle = true, customFilename = customFilename)
    }

    private fun exportNotesInternal(notes: List<NoteEntity>, format: ExportFormat, isSingle: Boolean, customFilename: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            exportResult.value = null
            try {
                val notesToExport = if (isSingle) notes else repository.getAllNotesForExport()
                if (notesToExport.isEmpty()) {
                    exportResult.value = ExportResult.Error("Tidak ada catatan untuk diekspor")
                    return@launch
                }

                val content = when (format) {
                    ExportFormat.TXT -> exportToTxt(notesToExport)
                    ExportFormat.JSON -> exportToJson(notesToExport)
                    ExportFormat.MD -> exportToMarkdown(notesToExport)
                }

                val prefix = "catetan"
                val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now())
                val baseName = customFilename?.takeIf { it.isNotEmpty() } ?: "${prefix}_export_$timestamp"
                val fileName = "$baseName.${format.name.lowercase()}"
                val uri = saveToDownloads(content, fileName, format.mimeType)
                exportResult.value = ExportResult.Success(fileName, uri)
            } catch (e: Exception) {
                exportResult.value = ExportResult.Error("Gagal ekspor: ${e.message}")
            }
        }
    }

    private fun exportCtnInternal(customFilename: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            exportResult.value = null
            try {
                val notesToExport = repository.getAllNotesForExport()
                if (notesToExport.isEmpty()) {
                    exportResult.value = ExportResult.Error("Tidak ada catatan untuk diekspor")
                    return@launch
                }

                val content = exportToCtn(notesToExport)
                val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now())
                val baseName = customFilename?.takeIf { it.isNotEmpty() } ?: "catetan-export-$timestamp"
                val fileName = "$baseName.ctn"
                val uri = saveToDownloads(content, fileName, ctnMimeType)
                exportResult.value = ExportResult.Success(fileName, uri)
            } catch (e: Exception) {
                exportResult.value = ExportResult.Error("Gagal ekspor: ${e.message}")
            }
        }
    }

    private fun exportToTxt(notes: List<NoteEntity>): String {
        val sb = StringBuilder()
        sb.appendLine("NOTES EXPORT - ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))}")
        sb.appendLine("Total: ${notes.size} catatan")
        sb.appendLine("=".repeat(50))
        sb.appendLine()

        notes.forEachIndexed { index, note ->
            val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(note.updatedAt), ZoneId.systemDefault())
            sb.appendLine("${index + 1}. ${note.title}")
            if (note.isPinned) sb.appendLine("   📌 Pinned")
            sb.appendLine("   $date")
            sb.appendLine("   ${"-".repeat(40)}")
            sb.appendLine("   ${note.content}")
            sb.appendLine()
        }
        return sb.toString()
    }

    private fun exportToJson(notes: List<NoteEntity>): String {
        val json = Json { prettyPrint = true }
        val exportData = notes.map { note ->
            mapOf(
                "id" to note.id,
                "title" to note.title,
                "content" to note.content,
                "createdAt" to note.createdAt,
                "updatedAt" to note.updatedAt,
                "isPinned" to note.isPinned,
                "color" to note.color
            )
        }
        return json.encodeToString(exportData)
    }

    private fun exportToMarkdown(notes: List<NoteEntity>): String {
        val sb = StringBuilder()
        sb.appendLine("# Notes Export")
        sb.appendLine("**Tanggal:** ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))}")
        sb.appendLine("**Total:** ${notes.size} catatan")
        sb.appendLine()

        notes.forEachIndexed { index, note ->
            val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(note.updatedAt), ZoneId.systemDefault())
            sb.appendLine("## ${index + 1}. ${note.title}")
            if (note.isPinned) sb.appendLine("> 📌 **Pinned**")
            sb.appendLine("> *$date*")
            sb.appendLine()
            sb.appendLine("${note.content}")
            sb.appendLine()
            sb.appendLine("---")
            sb.appendLine()
        }
        return sb.toString()
    }

    private fun exportToCtn(notes: List<NoteEntity>): String {
        val json = Json { prettyPrint = true }
        val exportData = CtnExport(
            app = "Catetan",
            version = "1.0.0",
            exportedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
            notes = notes.map { note ->
                CtnNote(
                    id = note.id,
                    title = note.title,
                    content = note.content,
                    createdAt = note.createdAt,
                    updatedAt = note.updatedAt,
                    isPinned = note.isPinned,
                    color = note.color
                )
            }
        )
        return json.encodeToString(exportData)
    }

    private fun saveToDownloads(content: String, fileName: String, mimeType: String): Uri {
        val resolver: ContentResolver = getApplication<Application>().contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
        }

        val uri: Uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IllegalStateException("Failed to create file")

        resolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(content.toByteArray())
        } ?: throw IllegalStateException("Failed to write file")

        return uri
    }

    val ExportFormat.mimeType: String
        get() = when (this) {
            ExportFormat.TXT -> "text/plain"
            ExportFormat.JSON -> "application/json"
            ExportFormat.MD -> "text/markdown"
        }

    val ctnMimeType: String = "application/x-catetan"
}

@Serializable
data class CtnExport(
    @SerialName("app") val app: String,
    @SerialName("version") val version: String,
    @SerialName("exportedAt") val exportedAt: String,
    @SerialName("notes") val notes: List<CtnNote>
)

@Serializable
data class CtnNote(
    @SerialName("id") val id: Long,
    @SerialName("title") val title: String,
    @SerialName("content") val content: String,
    @SerialName("createdAt") val createdAt: Long,
    @SerialName("updatedAt") val updatedAt: Long,
    @SerialName("isPinned") val isPinned: Boolean,
    @SerialName("color") val color: Int
)