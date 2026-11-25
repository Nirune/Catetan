package com.example.notesapp.presentation.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.example.notesapp.data.database.NotesDatabase
import com.example.notesapp.data.model.FolderEntity
import com.example.notesapp.data.model.NoteEntity
import com.example.notesapp.domain.repository.NoteRepository
import com.example.notesapp.presentation.ui.ThemeMode
import com.example.notesapp.presentation.ui.NoteSortOrder
import com.example.notesapp.presentation.ui.ThemePreferences
import com.example.notesapp.presentation.ui.ViewMode

sealed interface ExportResult {
    data class Success(val fileName: String, val uri: String?) : ExportResult
    data class Error(val message: String) : ExportResult
}

sealed interface ImportResult {
    data class Success(val importedCount: Int, val createdFolderCount: Int = 0) : ImportResult
    data class Error(val message: String) : ImportResult
}

enum class ExportFormat {
    TXT, JSON, MD
}

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: NoteRepository

    val allNotes: StateFlow<List<NoteEntity>>
    val trashedNotes: StateFlow<List<NoteEntity>>
    val trashCount: StateFlow<Int>
    val folders: StateFlow<List<FolderEntity>>
    private val _activeFolderId = MutableStateFlow<Long?>(null)
    val activeFolderId: StateFlow<Long?> = _activeFolderId
    val notesInActiveFolder: StateFlow<List<NoteEntity>>
    val exportResult = MutableStateFlow<ExportResult?>(null)
    val importResult = MutableStateFlow<ImportResult?>(null)

    private val _themeMode = MutableStateFlow(ThemePreferences.themeMode)
    val themeMode: StateFlow<ThemeMode> = _themeMode

    private val _themeName = MutableStateFlow(ThemePreferences.themeName)
    val themeName: StateFlow<String> = _themeName

    private val _viewMode = MutableStateFlow(ThemePreferences.viewMode)
    val viewMode: StateFlow<ViewMode> = _viewMode
    private val _noteSortOrder = MutableStateFlow(ThemePreferences.noteSortOrder)
    val noteSortOrder: StateFlow<NoteSortOrder> = _noteSortOrder

    init {
        val noteDao = NotesDatabase.getDatabase(application).noteDao()
        repository = NoteRepository(noteDao, NotesDatabase.getDatabase(application).folderDao())
        allNotes = repository.allNotes.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        trashedNotes = repository.trashedNotes.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        trashCount = repository.trashCount.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
        folders = repository.folders.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        notesInActiveFolder = combine(allNotes, _activeFolderId) { notes, folderId ->
            if (folderId == null) notes else notes.filter { it.folderId == folderId }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        val retentionDays = ThemePreferences.trashRetentionDays
        if (retentionDays > 0) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.purgeTrashBefore(System.currentTimeMillis() - retentionDays * 24L * 60L * 60L * 1000L)
            }
        }
    }

    fun getRepository(): NoteRepository = repository

    fun setActiveFolder(folderId: Long?) { _activeFolderId.value = folderId }

    suspend fun createFolder(name: String): String? {
        return createFolderWithId(name).second
    }

    suspend fun createFolderWithId(name: String): Pair<Long?, String?> {
        val cleaned = name.trim()
        validateFolderName(cleaned)?.let { return null to it }
        if (repository.folderNameCount(cleaned) > 0) return null to "Nama folder sudah dipakai"
        val folderId = repository.createFolder(FolderEntity(name = cleaned, createdAt = System.currentTimeMillis()))
        _activeFolderId.value = folderId
        return folderId to null
    }

    suspend fun exportFolder(folderId: Long) {
        val folder = repository.folders.first().firstOrNull { it.id == folderId }
            ?: run {
                exportResult.value = ExportResult.Error("Folder tidak ditemukan")
                return
            }
        val folderNotes = repository.getNotesInFolder(folderId).first()
        if (folderNotes.isEmpty()) {
            exportResult.value = ExportResult.Error("Folder kosong, tidak ada yang diekspor")
            return
        }
        exportCtnNotes(folderNotes, listOf(folder))
    }

    suspend fun renameFolder(id: Long, name: String): String? {
        val cleaned = name.trim()
        validateFolderName(cleaned)?.let { return it }
        if (repository.folderNameCount(cleaned, id) > 0) return "Nama folder sudah dipakai"
        repository.renameFolder(id, cleaned)
        return null
    }

    suspend fun deleteFolder(id: Long) {
        if (_activeFolderId.value == id) _activeFolderId.value = null
        repository.deleteFolder(id)
    }

    suspend fun moveNotesToFolder(ids: List<Long>, targetFolderId: Long?) {
        if (ids.isNotEmpty()) repository.moveNotesToFolder(ids, targetFolderId)
    }

    private fun validateFolderName(name: String): String? = when {
        name.isBlank() -> "Nama folder tidak boleh kosong"
        name.length > 24 -> "Nama folder maksimal 24 karakter"
        else -> null
    }

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

    fun setNoteSortOrder(order: NoteSortOrder) {
        ThemePreferences.setNoteSortOrder(order)
        _noteSortOrder.value = order
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
                    val id = repository.insertNote(note.copy(folderId = _activeFolderId.value))
                    onResult(note.copy(id = id, folderId = _activeFolderId.value))
        }
    }

    suspend fun insertNoteAndReturn(title: String, content: String, color: Int): NoteEntity {
        val note = NoteEntity.create(title = title, content = content, color = color)
            .copy(folderId = _activeFolderId.value)
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
            repository.moveToTrash(note)
        }
    }

    fun deleteNotesByIds(ids: List<Long>) {
        viewModelScope.launch {
            repository.moveToTrashByIds(ids)
        }
    }

    fun restoreNote(id: Long) {
        viewModelScope.launch { repository.restoreNote(id) }
    }

    fun permanentlyDeleteNote(id: Long) {
        viewModelScope.launch { repository.permanentlyDeleteNote(id) }
    }

    fun emptyTrash() {
        viewModelScope.launch { repository.emptyTrash() }
    }

    fun setTrashRetentionDays(days: Int) {
        ThemePreferences.setTrashRetentionDays(days)
    }


    fun togglePin(id: Long) {
        viewModelScope.launch {
            repository.togglePin(id)
        }
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

    fun exportSingleNote(note: NoteEntity, format: ExportFormat) {
        viewModelScope.launch(Dispatchers.IO) {
            val activeNote = repository.getActiveNoteForExport(note.id)
            if (activeNote == null) {
                exportResult.value = ExportResult.Error("Catatan sudah berada di Sampah")
            } else {
        exportNotesInternal(listOf(activeNote), format, isSingle = true)
            }
        }
    }

    fun exportAllNotes(format: ExportFormat) {
        exportNotesInternal(emptyList(), format, isSingle = false)
    }

    fun exportAllCtn() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val notes = repository.getAllNotesForExport()
                if (notes.isEmpty()) {
                    exportResult.value = ExportResult.Error("Tidak ada catatan untuk diekspor")
                    return@launch
                }
                val allFolders = repository.folders.first()
                val content = exportToCtn(notes, allFolders)
                val fileName = buildCtnFileName(null)
                val destination = saveToCatetanFolder(content, fileName, "application/x-catetan")
                exportResult.value = ExportResult.Success(fileName, destination)
            } catch (e: Exception) {
                Log.e("Export", "CTN export failed", e)
                exportResult.value = ExportResult.Error("Gagal ekspor: ${e.message}")
            }
        }
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
                lastImportNewFolderCount = 0

                val importedCount = when (extension) {
                    "ctn" -> importCtn(content)
                    "json" -> importSingleJson(content)
                    "txt" -> importSingleTxt(content)
                    "md" -> importSingleMd(content)
                    else -> throw IllegalArgumentException("Format file tidak didukung: .$extension")
                }

                importResult.value = ImportResult.Success(importedCount, lastImportNewFolderCount)
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
        lastImportNewFolderCount = 0
        val json = Json { ignoreUnknownKeys = true }
        val ctnExport = json.decodeFromString<CtnExport>(content)
        val folderNameToId = mutableMapOf<String, Long>()
        var createdFolderCount = 0
        val fileFolderNames = (ctnExport.folders.orEmpty() + ctnExport.notes.mapNotNull { it.folder })
            .map { it.trim().take(24) }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(java.util.Locale.ROOT) }
        val existingFolders = repository.folders.first()
        fileFolderNames.forEach { name ->
            val existing = existingFolders.firstOrNull { it.name.equals(name, ignoreCase = true) }
            if (existing != null) {
                folderNameToId[name.lowercase(java.util.Locale.ROOT)] = existing.id
            } else {
                val id = repository.createFolder(FolderEntity(name = name, createdAt = System.currentTimeMillis()))
                folderNameToId[name.lowercase(java.util.Locale.ROOT)] = id
                createdFolderCount++
            }
        }
        var count = 0
        var perNoteFolderCount = 0
        ctnExport.notes.forEach { ctnNote ->
            val folderKey = ctnNote.folder?.trim()?.take(24)?.lowercase(java.util.Locale.ROOT)
            if (!folderKey.isNullOrBlank() && folderKey !in folderNameToId) {
                val existing = repository.folders.first().firstOrNull { it.name.equals(folderKey, ignoreCase = true) }
                if (existing != null) {
                    folderNameToId[folderKey] = existing.id
                } else {
                    val newId = repository.createFolder(FolderEntity(name = folderKey, createdAt = System.currentTimeMillis()))
                    folderNameToId[folderKey] = newId
                    perNoteFolderCount++
                }
            }
            val note = NoteEntity(
                id = 0, // biarkan Room generate id baru
                title = ctnNote.title,
                content = ctnNote.content,
                createdAt = ctnNote.createdAt ?: ctnNote.updatedAt,
                updatedAt = ctnNote.updatedAt,
                isPinned = ctnNote.isPinned,
                color = ctnNote.color,
                folderId = folderKey?.let(folderNameToId::get)
            )
            repository.insertNote(note)
            count++
        }
        lastImportNewFolderCount = createdFolderCount + perNoteFolderCount
        return count
    }

    private var lastImportNewFolderCount: Int = 0

    private suspend fun ensureImportedFolder(rawName: String?): Long? {
        val name = rawName?.trim()?.take(24)?.takeIf { it.isNotBlank() } ?: return null
        val existing = repository.folders.first().firstOrNull { it.name.equals(name, ignoreCase = true) }
        if (existing != null) return existing.id
        val id = repository.createFolder(FolderEntity(name = name, createdAt = System.currentTimeMillis()))
        lastImportNewFolderCount++
        return id
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
        val updatedAt = noteMap["updatedAt"]?.toString()?.toLong() ?: Instant.now().toEpochMilli()
        val createdAt = noteMap["createdAt"]?.toString()?.toLong() ?: updatedAt
        val folderId = ensureImportedFolder(noteMap["folder"]?.toString())

        val note = NoteEntity(
            id = 0,
            title = title,
            content = contentText,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isPinned = isPinned,
            color = color,
            folderId = folderId
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

    private fun exportNotesInternal(
        notes: List<NoteEntity>,
        format: ExportFormat,
        isSingle: Boolean
    ) {
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

                val fileName = buildExportFileName(notesToExport, format, isSingle)
                val destinationName = saveToCatetanFolder(content, fileName, format.mimeType)
                exportResult.value = ExportResult.Success(fileName, destinationName)
            } catch (e: Exception) {
                Log.e("Export", "Export failed format=$format single=$isSingle", e)
                exportResult.value = ExportResult.Error("Gagal ekspor: ${e.message}")
            }
        }
    }

    private fun exportCtnNotes(notes: List<NoteEntity>, folders: List<FolderEntity>? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            exportResult.value = null
            try {
                if (notes.isEmpty()) {
                    exportResult.value = ExportResult.Error("Folder kosong, tidak ada yang diekspor")
                    return@launch
                }
                val content = exportToCtn(notes, folders)
                val fileName = buildCtnFileName(folders?.singleOrNull()?.name)
                val destination = saveToCatetanFolder(content, fileName, "application/x-catetan")
                exportResult.value = ExportResult.Success(fileName, destination)
            } catch (e: Exception) {
                Log.e("Export", "CTN export failed", e)
                exportResult.value = ExportResult.Error("Gagal ekspor: ${e.message}")
            }
        }
    }

    private fun buildCtnFileName(folderName: String?): String {
        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS").format(LocalDateTime.now())
        val base = folderName?.let { "catetan_${fileSafeFolderName(it).take(40)}" } ?: "catetan_semua"
        return "${base}_$timestamp.ctn"
    }

    private fun fileSafeFolderName(folderName: String): String = folderName.replace(Regex("[\\\\/:*?\"<>|]"), "_")

    private fun buildExportFileName(notes: List<NoteEntity>, format: ExportFormat, isSingle: Boolean): String {
        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS").format(LocalDateTime.now())
        val baseName = if (isSingle) {
            sanitizeFileName(notes.first().title.ifBlank { "Catatan" }).ifBlank { "Catatan" }
        } else {
            "Catetan_semua"
        }
        return "${baseName}_$timestamp.${format.name.lowercase()}"
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

    private suspend fun exportToJson(notes: List<NoteEntity>): String {
        val json = Json { prettyPrint = true }
        val folderNames = repository.folders.first().associate { it.id to it.name }
        val exportData = notes.map { note ->
            mapOf(
                "id" to note.id,
                "title" to note.title,
                "content" to note.content,
                "createdAt" to note.createdAt,
                "updatedAt" to note.updatedAt,
                "isPinned" to note.isPinned,
                "color" to note.color,
                "folder" to note.folderId?.let(folderNames::get)
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

    private suspend fun exportToCtn(notes: List<NoteEntity>, includedFolders: List<FolderEntity>? = null): String {
        val folders = repository.folders.first()
        val folderNameById = folders.associate { it.id to it.name }
        val exportedFolders = includedFolders ?: folders
        val exportData = CtnExport(
            app = "Catetan",
            version = "1.1",
            exportedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
            notes = notes.map { note ->
                CtnNote(
                    id = note.id,
                    title = note.title,
                    content = note.content,
                    createdAt = note.createdAt,
                    updatedAt = note.updatedAt,
                    isPinned = note.isPinned,
                    color = note.color,
                    folder = note.folderId?.let(folderNameById::get)
                )
            },
            folders = exportedFolders.map { it.name }
        )
        return Json { prettyPrint = true }.encodeToString(exportData)
    }

    private suspend fun saveToCatetanFolder(content: String, fileName: String, mimeType: String): String = withContext(Dispatchers.IO) {
        val context = getApplication<Application>()
        val resolver: ContentResolver = context.contentResolver
        val savedUriString = ThemePreferences.exportPathUri
            ?: throw IllegalStateException("Folder ekspor belum dipilih. Pilih folder di Pengaturan > Path Ekspor.")
        val savedUri = Uri.parse(savedUriString)
        val hasPersistedPermission = resolver.persistedUriPermissions.any {
            it.uri == savedUri && it.isReadPermission && it.isWritePermission
        }
        if (!hasPersistedPermission) {
            throw SecurityException("Izin folder ekspor hilang. Pilih ulang folder di Pengaturan > Path Ekspor.")
        }
        val directory = DocumentFile.fromTreeUri(context, savedUri)
            ?: throw IllegalStateException("Folder ekspor tidak dapat dibuka. Pilih ulang folder.")
        if (!directory.canWrite()) {
            throw SecurityException("Folder ekspor tidak bisa ditulis. Pilih folder lain.")
        }
        val targetName = uniqueFileName(directory, fileName)
        val file = directory.createFile(mimeType, targetName)
            ?: throw IllegalStateException("Gagal membuat file di folder ekspor.")
        try {
            val output = resolver.openOutputStream(file.uri, "w")
                ?: throw IllegalStateException("Gagal membuka file untuk ditulis.")
            output.use { it.write(content.toByteArray(Charsets.UTF_8)) }
        } catch (e: Exception) {
            file.delete()
            throw e
        }
        val folderName = directory.name?.takeIf { it.isNotBlank() } ?: "Folder ekspor"
        "$folderName/$targetName"
    }

    private fun uniqueFileName(directory: DocumentFile, requestedName: String): String {
        if (directory.findFile(requestedName) == null) return requestedName
        val extension = requestedName.substringAfterLast('.', "")
        val base = requestedName.substringBeforeLast('.', requestedName)
        var index = 1
        while (true) {
            val candidate = "$base ($index).$extension"
            if (directory.findFile(candidate) == null) return candidate
            index++
        }
    }

    val ExportFormat.mimeType: String
        get() = when (this) {
            ExportFormat.TXT -> "text/plain"
            ExportFormat.JSON -> "application/json"
            ExportFormat.MD -> "text/markdown"
        }

}

@Serializable
data class CtnExport(
    @SerialName("app") val app: String,
    @SerialName("version") val version: String,
    @SerialName("exportedAt") val exportedAt: String,
    @SerialName("notes") val notes: List<CtnNote>,
    @SerialName("folders") val folders: List<String>? = null
)

@Serializable
data class CtnNote(
    @SerialName("id") val id: Long,
    @SerialName("title") val title: String,
    @SerialName("content") val content: String,
    @SerialName("createdAt") val createdAt: Long? = null,
    @SerialName("updatedAt") val updatedAt: Long,
    @SerialName("isPinned") val isPinned: Boolean,
    @SerialName("color") val color: Int,
    @SerialName("folder") val folder: String? = null
)
