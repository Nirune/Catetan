package com.example.notesapp.presentation.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.OutputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.example.notesapp.data.database.NotesDatabase
import com.example.notesapp.data.model.NoteEntity
import com.example.notesapp.domain.repository.NoteRepository
import com.example.notesapp.presentation.ui.ThemeMode
import com.example.notesapp.presentation.ui.ThemePreferences
import com.example.notesapp.presentation.ui.ViewMode

sealed interface ExportResult {
    data class Success(val fileName: String, val uri: Uri?) : ExportResult
    data class Error(val message: String) : ExportResult
}

sealed interface ImportResult {
    data class Success(val importedCount: Int) : ImportResult
    data class Error(val message: String) : ImportResult
}

enum class ExportFormat {
    TXT, JSON, MD
}

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: NoteRepository

    val allNotes: StateFlow<List<NoteEntity>>
    val exportResult = MutableStateFlow<ExportResult?>(null)
    val importResult = MutableStateFlow<ImportResult?>(null)

    private val _themeMode = MutableStateFlow(ThemePreferences.themeMode)
    val themeMode: StateFlow<ThemeMode> = _themeMode

    private val _themeName = MutableStateFlow(ThemePreferences.themeName)
    val themeName: StateFlow<String> = _themeName

    private val _viewMode = MutableStateFlow(ThemePreferences.viewMode)
    val viewMode: StateFlow<ViewMode> = _viewMode

    init {
        val noteDao = NotesDatabase.getDatabase(application).noteDao()
        repository = NoteRepository(noteDao)
        allNotes = repository.allNotes.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun getRepository(): NoteRepository = repository

    fun setThemeMode(mode: ThemeMode) {
        ThemePreferences.setThemeMode(mode)
        _themeMode.value = mode
    }

    fun setThemeName(name: String) {
        ThemePreferences.setThemeName(name)
        _themeName.value = ThemePreferences.themeName
    }

    fun setViewMode(mode: ViewMode) {
        ThemePreferences.setViewMode(mode)
        _viewMode.value = mode
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

    suspend fun insertNoteAndReturn(title: String, content: String, color: Int): NoteEntity {
        val note = NoteEntity.create(title = title, content = content, color = color)
        val id = repository.insertNote(note)
        return note.copy(id = id)
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

    fun deleteNotesByIds(ids: List<Long>) {
        viewModelScope.launch {
            repository.deleteNotesByIds(ids)
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

    /**
     * Membersihkan nama file: hapus karakter terlarang \ / : * ? " < > |,
     * ganti spasi/tab ganda jadi satu spasi, trim, hapus titik di akhir,
     * batasi maksimal 50 karakter.
     */
    fun sanitizeFileName(name: String): String {
        return name
            .replace("[\\\\/:*?\"<>|]".toRegex(), "")
            .replace("[\\s]+".toRegex(), " ")
            .trim()
            .trimEnd('.')
            .trim()
            .take(50)
    }

    fun exportCtn(customFilename: String? = null) {
        exportCtnInternal(customFilename)
    }

    fun exportSingleNote(note: NoteEntity, format: ExportFormat, customFilename: String? = null) {
        val resolvedFilename = customFilename?.takeIf { it.isNotEmpty() } ?: sanitizeFileName(
            note.title.takeIf { it.isNotEmpty() }
                ?: note.content.lines().firstOrNull()?.takeIf { it.isNotEmpty() }
                ?: "catetan"
        )
        exportNotesInternal(listOf(note), format, isSingle = true, customFilename = resolvedFilename)
    }

    /**
     * Impor catatan dari file yang dipilih via Storage Access Framework.
     * Mendukung .ctn (semua catatan), .txt/.json/.md (satu catatan).
     */
    fun importFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            importResult.value = null
            try {
                val resolver: ContentResolver = getApplication<Application>().contentResolver
                val inputStream = resolver.openInputStream(uri) ?: throw IllegalStateException("Tidak dapat membuka file")
                val content = inputStream.bufferedReader().use { it.readText() }

                val fileName = getFileNameFromUri(resolver, uri) ?: "unknown"
                val extension = fileName.substringAfterLast('.').lowercase()

                val importedCount = when (extension) {
                    "ctn" -> importCtn(content)
                    "json" -> importSingleJson(content)
                    "txt" -> importSingleTxt(content)
                    "md" -> importSingleMd(content)
                    else -> throw IllegalArgumentException("Format file tidak didukung: .$extension")
                }

                importResult.value = ImportResult.Success(importedCount)
            } catch (e: Exception) {
                importResult.value = ImportResult.Error("Gagal impor: ${e.message}")
            }
        }
    }

    private fun getFileNameFromUri(resolver: ContentResolver, uri: Uri): String? {
        return resolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                cursor.getString(columnIndex)
            } else null
        }
    }

    /**
     * Impor semua catatan dari format .ctn (JSON Catetan).
     */
    private suspend fun importCtn(content: String): Int {
        val json = Json { ignoreUnknownKeys = true }
        val ctnExport = json.decodeFromString<CtnExport>(content)
        var count = 0
        ctnExport.notes.forEach { ctnNote ->
            val note = NoteEntity(
                id = 0, // biarkan Room generate id baru
                title = ctnNote.title,
                content = ctnNote.content,
                createdAt = ctnNote.createdAt,
                updatedAt = ctnNote.updatedAt,
                isPinned = ctnNote.isPinned,
                color = ctnNote.color
            )
            repository.insertNote(note)
            count++
        }
        return count
    }

    /**
     * Impor satu catatan dari JSON.
     */
    private suspend fun importSingleJson(content: String): Int {
        val json = Json { ignoreUnknownKeys = true }
        val noteMap = json.decodeFromString<Map<String, Any>>(content)
        val title = noteMap["title"]?.toString() ?: "Tanpa Judul"
        val contentText = noteMap["content"]?.toString() ?: ""
        val isPinned = noteMap["isPinned"] as? Boolean ?: false
        val color = noteMap["color"]?.toString()?.toIntOrNull() ?: 0xFFFFFFFF.toInt()
        val createdAt = noteMap["createdAt"]?.toString()?.toLong() ?: Instant.now().toEpochMilli()
        val updatedAt = noteMap["updatedAt"]?.toString()?.toLong() ?: Instant.now().toEpochMilli()

        val note = NoteEntity(
            id = 0,
            title = title,
            content = contentText,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isPinned = isPinned,
            color = color
        )
        repository.insertNote(note)
        return 1
    }

    /**
     * Impor satu catatan dari TXT (baris pertama = judul, sisanya = isi).
     */
    private suspend fun importSingleTxt(content: String): Int {
        val lines = content.lines()
        val title = lines.firstOrNull()?.takeIf { it.isNotEmpty() } ?: "Tanpa Judul"
        val contentText = lines.drop(1).joinToString("\n").trimEnd()
        val note = NoteEntity(
            id = 0,
            title = title,
            content = contentText,
            createdAt = Instant.now().toEpochMilli(),
            updatedAt = Instant.now().toEpochMilli(),
            isPinned = false,
            color = 0xFFFFFFFF.toInt()
        )
        repository.insertNote(note)
        return 1
    }

    /**
     * Impor satu catatan dari Markdown (baris pertama dengan # = judul, sisanya = isi).
     */
    private suspend fun importSingleMd(content: String): Int {
        val lines = content.lines()
        var title = "Tanpa Judul"
        var contentStartIndex = 0
        for ((index, line) in lines.withIndex()) {
            if (line.startsWith("# ")) {
                title = line.removePrefix("# ").trim()
                contentStartIndex = index + 1
                break
            } else if (line.isNotEmpty()) {
                title = line.trim()
                contentStartIndex = index + 1
                break
            }
        }
        val contentText = lines.drop(contentStartIndex).joinToString("\n").trimEnd()
        val note = NoteEntity(
            id = 0,
            title = title,
            content = contentText,
            createdAt = Instant.now().toEpochMilli(),
            updatedAt = Instant.now().toEpochMilli(),
            isPinned = false,
            color = 0xFFFFFFFF.toInt()
        )
        repository.insertNote(note)
        return 1
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

                val timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm").format(LocalDateTime.now())
                val defaultBaseName = if (isSingle && notes.isNotEmpty()) {
                    sanitizeFileName(notes.first().title.takeIf { it.isNotEmpty() } ?: notes.first().content.lines().firstOrNull()?.takeIf { it.isNotEmpty() } ?: "catetan")
                } else {
                    "catetan"
                }
                val baseName = customFilename?.takeIf { it.isNotEmpty() } ?: "${defaultBaseName}_$timestamp"
                val fileName = "$baseName.${format.name.lowercase()}"
                val uri = saveToCatetanFolder(content, fileName, format.mimeType)
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
                val timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm").format(LocalDateTime.now())
                val baseName = customFilename?.takeIf { it.isNotEmpty() } ?: "catetan_$timestamp"
                val fileName = "$baseName.ctn"
                val uri = saveToCatetanFolder(content, fileName, ctnMimeType)
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

    private fun saveToCatetanFolder(content: String, fileName: String, mimeType: String): Uri {
        val context = getApplication<Application>()
        val resolver: ContentResolver = context.contentResolver

        // Cek apakah ada path export custom yang disimpan
        val savedUriString = ThemePreferences.exportPathUri
        if (savedUriString != null) {
            val savedUri = Uri.parse(savedUriString)
            val dirFile = DocumentFile.fromTreeUri(context, savedUri)
            if (dirFile != null) {
                val newFile = dirFile.createFile(mimeType, fileName)
                if (newFile != null) {
                    resolver.openOutputStream(newFile.uri)?.use { outputStream ->
                        outputStream.write(content.toByteArray())
                    }
                    return newFile.uri
                }
            }
        }

        // Fallback ke Documents via MediaStore
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents")
        }

        val uri: Uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IllegalStateException("Gagal membuat file")

        resolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(content.toByteArray())
        } ?: throw IllegalStateException("Gagal menulis file")

        // Hapus flag pending agar file terlihat oleh sistem
        contentValues.clear()
        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, contentValues, null, null)

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
