package com.example.notesapp.presentation.ui

import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.LocalContentColor
import com.example.notesapp.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notesapp.data.model.NoteEntity
import com.example.notesapp.presentation.ui.ThemeMode
import com.example.notesapp.presentation.ui.ThemePreferences
import com.example.notesapp.presentation.ui.themeModeLabel
import com.example.notesapp.presentation.ui.themePalette
import com.example.notesapp.presentation.ui.ViewMode
import com.example.notesapp.presentation.ui.isEffectiveDarkTheme
import com.example.notesapp.presentation.viewmodel.ExportFormat
import com.example.notesapp.presentation.viewmodel.ExportResult
import com.example.notesapp.presentation.viewmodel.ImportResult
import com.example.notesapp.presentation.viewmodel.NoteViewModel
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Material "done_all" icon (official SVG path) built as a lightweight ImageVector,
// sama seperti PaletteIcon di AddEditNoteScreen. Tidak perlu material-icons-extended.
// Path resmi dari Material Icons (done_all, 24px): tiga centang bertumpuk.
// Sumber: https://raw.githubusercontent.com/google/material-design-icons/master/src/image/palette/materialiconsoutlined/24px.svg
private const val SELECT_ALL_PATH =
    "M18,7l-1.41,-1.41 -6.34,6.34 1.41,1.41L18,7z" +
        "M22.24,5.59L11.66,16.17 7.48,12l-1.41,1.41L11.66,19l12,-12 -1.42,-1.41z" +
        "M0.41,13.41L6,19l1.41,-1.41L1.83,12 0.41,13.41z"

val SelectAllIcon: ImageVector = ImageVector.Builder(
    name = "Outlined.SelectAll",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).addPath(
    pathData = addPathNodes(SELECT_ALL_PATH),
    fill = SolidColor(Color.Black)
).build()

// Custom Description (document) icon - Material Icons "description"
private const val DESCRIPTION_PATH =
    "M14,2H6A2,2 0 0,0 4,4V20A2,2 0 0,0 6,22H18A2,2 0 0,0 20,20V8L14,2M18,20H6V4H13V9H18V20Z"

val DescriptionIcon: ImageVector = ImageVector.Builder(
    name = "Filled.Description",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).addPath(
    pathData = addPathNodes(DESCRIPTION_PATH),
    fill = SolidColor(Color.Black)
).build()

// Custom StarOutline icon - Material Icons "star_border"
private const val STAR_OUTLINE_PATH =
    "M12,17.27L18.18,21L16.54,13.97L22,9.24L14.81,8.62L12,2L9.19,8.63L2,9.24L7.45,13.97L5.82,21L12,17.27Z"

val StarOutlineIcon: ImageVector = ImageVector.Builder(
    name = "Filled.StarOutline",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).addPath(
    pathData = addPathNodes(STAR_OUTLINE_PATH),
    fill = SolidColor(Color.Black)
).build()

data class NoteListTopBarCallbacks(
    val onExport: () -> Unit,
    val onSettings: () -> Unit,
    val onImport: () -> Unit,
    val onSelectAll: () -> Unit,
    val onExitSelection: () -> Unit,
    val onDeleteSelected: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    viewModel: NoteViewModel,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean,
    selectedNoteIds: List<Long>,
    onSelectionModeChange: (Boolean) -> Unit,
    onSelectionChange: (List<Long>) -> Unit,
    onNoteClick: (NoteEntity) -> Unit = {},
    onAddClick: () -> Unit = {},
    onTopBarCallbacks: ((NoteListTopBarCallbacks) -> Unit)? = null,
    onThemeModeChanged: (ThemeMode) -> Unit = {},
    onAppearanceClick: () -> Unit,
    themeName: String,
    onPickFile: () -> Unit = {},
    onPickFolder: () -> Unit = {}
) {
    val notes by viewModel.allNotes.collectAsState()
    val themeName by viewModel.themeName.collectAsState()
    var showSingleExportDialog by remember { mutableStateOf(false) }
    var noteToExport by remember { mutableStateOf<NoteEntity?>(null) }
    var noteToDeleteId by rememberSaveable { mutableStateOf(-1L) }
    val noteToDelete = notes.firstOrNull { it.id == noteToDeleteId }
    var notesToDeleteIds by rememberSaveable { mutableStateOf(emptyList<Long>()) }
    val notesToDelete = notes.filter { notesToDeleteIds.contains(it.id) }
    val exportResult by viewModel.exportResult.collectAsState()
    val importResult by viewModel.importResult.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showThemeDialog by remember { mutableStateOf(false) }

    var showFilenameDialog by remember { mutableStateOf(false) }
    var exportFilename by remember { mutableStateOf("") }
    var pendingNoteForExport by remember { mutableStateOf<NoteEntity?>(null) }
    var pendingFormatForExport by remember { mutableStateOf<ExportFormat?>(null) }
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }

    fun isNoteSelected(note: NoteEntity): Boolean = selectedNoteIds.contains(note.id)

    fun enterSelectionMode(noteId: Long) {
        onSelectionModeChange(true)
        if (!selectedNoteIds.contains(noteId)) {
            onSelectionChange(selectedNoteIds + listOf(noteId))
        }
    }

    fun toggleSelection(noteId: Long) {
        if (selectedNoteIds.contains(noteId)) {
            val newSelection = selectedNoteIds - listOf(noteId)
            onSelectionChange(newSelection)
            if (newSelection.isEmpty()) {
                onSelectionModeChange(false)
            }
        } else {
            onSelectionChange(selectedNoteIds + listOf(noteId))
        }
    }

    fun exitSelectionMode() {
        onSelectionModeChange(false)
        onSelectionChange(emptyList())
        notesToDeleteIds = emptyList()
        showDeleteConfirmation = false
    }

    fun toggleSelectAll() {
        if (selectedNoteIds.size == notes.size && notes.isNotEmpty()) {
            onSelectionChange(emptyList())
            // Tetap di mode seleksi, counter 0 / N
        } else {
            onSelectionChange(notes.map { it.id })
            onSelectionModeChange(true)
        }
    }

    // Provide top bar callbacks to parent (MainActivity) for NotesTopBar.
    // Didefinisikan setelah helper seleksi supaya bisa manggil fungsi lokal itu.
    fun provideTopBarCallbacks() {
        onTopBarCallbacks?.let {
            it(
                NoteListTopBarCallbacks(
                    onExport = { showFilenameDialog = true },
                    onSettings = { showThemeDialog = true },
                    onImport = onPickFile,
                    onSelectAll = { toggleSelectAll() },
                    onExitSelection = { exitSelectionMode() },
                    onDeleteSelected = {
                        notesToDeleteIds = notes
                            .filter { selectedNoteIds.contains(it.id) }
                            .map { it.id }
                        showDeleteConfirmation = true
                    }
                )
            )
        }
    }
    provideTopBarCallbacks()

    // Bersihkan id seleksi yang sudah tidak ada di daftar (contoh: setelah hapus)
    LaunchedEffect(notes) {
        val validIds = notes.map { it.id }.toSet()
        val newSelection = selectedNoteIds.filter { validIds.contains(it) }
        if (newSelection != selectedNoteIds) {
            onSelectionChange(newSelection)
        }
        if (newSelection.isEmpty() && isSelectionMode) {
            exitSelectionMode()
        }
        notesToDeleteIds = notesToDeleteIds.filter { validIds.contains(it) }
        if (notesToDeleteIds.isEmpty()) {
            showDeleteConfirmation = false
        }
    }

    val themeMode by viewModel.themeMode.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val isDark = isEffectiveDarkTheme(themeMode)
    val currentExportPath = ThemePreferences.exportPathUri

    fun showSingleNoteExportDialog(note: NoteEntity) {
        pendingNoteForExport = note
        pendingFormatForExport = null
        val baseName = viewModel.sanitizeFileName(note.title.takeIf { it.isNotEmpty() } ?: note.content.lines().firstOrNull()?.takeIf { it.isNotEmpty() } ?: "catetan")
        val timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm").format(LocalDateTime.now())
        exportFilename = "${baseName}_$timestamp"
        showFilenameDialog = true
    }

    // Handle export result
    LaunchedEffect(exportResult) {
        exportResult?.let { result ->
            when (result) {
                is ExportResult.Success -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Berhasil ekspor: ${result.fileName}",
                            actionLabel = "OK"
                        )
                    }
                }
                is ExportResult.Error -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = result.message,
                            actionLabel = "OK"
                        )
                    }
                }
            }
            viewModel.exportResult.value = null
        }
    }

    // Handle import result
    LaunchedEffect(importResult) {
        importResult?.let { result ->
            when (result) {
                is ImportResult.Success -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Berhasil impor: ${result.importedCount} catatan",
                            actionLabel = "OK"
                        )
                    }
                }
                is ImportResult.Error -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = result.message,
                            actionLabel = "OK"
                        )
                    }
                }
            }
            viewModel.importResult.value = null
        }
    }

    // Settings dialog - full version with Export Path, View Mode, and Theme
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Path export section
                    Text(
                        text = stringResource(R.string.export_path_title),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = {
                                showThemeDialog = false
                                onPickFolder()
                            })
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.export_path_pick),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Text(
                            text = stringResource(
                                R.string.export_path_current,
                                currentExportPath ?: stringResource(R.string.export_path_default)
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Divider(
                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp)
                    )

                    // Tampilan section
                    Text(
                        text = stringResource(R.string.view_mode),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clickable(onClick = {
                                viewModel.setViewMode(
                                    if (viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
                                )
                            })
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.view_mode_grid),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = viewMode == ViewMode.GRID,
                            onCheckedChange = { _ ->
                                viewModel.setViewMode(
                                    if (viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
                                )
                            }
                        )
                    }

                    Divider(
                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp)
                    )

                    // Baris Tema & Mode membuka layar Tampilan
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = {
                                onAppearanceClick()
                                showThemeDialog = false
                            })
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tema & Mode",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "›",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        }
                        Text(
                            text = "${themePalette(themeName).name} · ${themeModeLabel(themeMode)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Divider(
                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text(stringResource(R.string.close)) }
            }
        )
    }

    // Filename dialog
    if (showFilenameDialog) {
        AlertDialog(
            onDismissRequest = {
                showFilenameDialog = false
                pendingNoteForExport = null
                pendingFormatForExport = null
            },
            title = { Text(stringResource(R.string.filename_title), fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = exportFilename,
                    onValueChange = { exportFilename = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.filename_hint)) },
                    singleLine = true,
                    maxLines = 1,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showFilenameDialog = false
                    if (pendingNoteForExport != null) {
                        noteToExport = pendingNoteForExport
                        showSingleExportDialog = true
                        pendingNoteForExport = null
                    } else {
                        viewModel.exportCtn(exportFilename)
                    }
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showFilenameDialog = false
                    pendingNoteForExport = null
                    pendingFormatForExport = null
                }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    // Single Note Export Dialog (format picker)
    if (showSingleExportDialog && noteToExport != null) {
        SingleNoteExportDialog(
            note = noteToExport!!,
            onDismiss = {
                showSingleExportDialog = false
                noteToExport = null
                pendingFormatForExport = null
            },
            onExport = { format ->
                viewModel.exportSingleNote(noteToExport!!, format, exportFilename)
                showSingleExportDialog = false
                noteToExport = null
                pendingFormatForExport = null
            }
        )
    }

    // Delete confirmation dialog
    noteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDeleteId = -1L },
            title = { Text("Hapus Catatan", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Catatan \"${note.title.ifBlank { "Catatan tanpa judul" }}\" " +
                        "akan dihapus permanen.\n\nLanjutkan?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteNote(note)
                        noteToDeleteId = -1L
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDeleteId = -1L }) {
                    Text("Batal")
                }
            }
        )
    }

    // Batch delete confirmation dialog (mode seleksi)
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Hapus Catatan", fontWeight = FontWeight.Bold) },
            text = {
                val pinnedCount = notesToDelete.count { it.isPinned }
                val base = "Catatan ${notesToDelete.size} akan dihapus permanen.\n\nLanjutkan?"
                val warning = if (pinnedCount > 0) {
                    "\n\n${pinnedCount} di antaranya berbintang."
                } else ""
                Text(base + warning)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ids = notesToDeleteIds
                        viewModel.deleteNotesByIds(ids)
                        notesToDeleteIds = emptyList()
                        showDeleteConfirmation = false
                        exitSelectionMode()
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Main content - no Scaffold, just the list/grid
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        BackHandler(enabled = isSelectionMode) {
            exitSelectionMode()
        }

        if (notes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = DescriptionIcon,
                        contentDescription = "Catatan kosong",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Belum ada catatan",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap tombol '+' di bawah buat bikin!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            when (viewMode) {
                ViewMode.LIST -> NoteListContent(
                    notes = notes,
                    isSelectionMode = isSelectionMode,
                    selectedNoteIds = selectedNoteIds,
                    onNoteClick = onNoteClick,
                    onLongClick = { noteId -> enterSelectionMode(noteId) },
                    onToggleSelection = { noteId -> toggleSelection(noteId) },
                    onDelete = { note -> noteToDeleteId = note.id },
                    onPinToggle = { noteId -> viewModel.togglePin(noteId) },
                    onExport = { note ->
                        pendingNoteForExport = note
                        pendingFormatForExport = null
                        val baseName = viewModel.sanitizeFileName(note.title.takeIf { it.isNotEmpty() } ?: note.content.lines().firstOrNull()?.takeIf { it.isNotEmpty() } ?: "catetan")
                        val timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm").format(LocalDateTime.now())
                        exportFilename = "${baseName}_$timestamp"
                        showFilenameDialog = true
                    },
                    accentColorProvider = { note -> NoteColorMapper.getNoteAccentColorNonComposable(note.color, isDark) }
                )
                ViewMode.GRID -> NoteGridContent(
                    notes = notes,
                    isSelectionMode = isSelectionMode,
                    selectedNoteIds = selectedNoteIds,
                    onNoteClick = onNoteClick,
                    onLongClick = { noteId -> enterSelectionMode(noteId) },
                    onToggleSelection = { noteId -> toggleSelection(noteId) },
                    onDelete = { note -> noteToDeleteId = note.id },
                    onPinToggle = { noteId -> viewModel.togglePin(noteId) },
                    onExport = { note ->
                        pendingNoteForExport = note
                        pendingFormatForExport = null
                        val baseName = viewModel.sanitizeFileName(note.title.takeIf { it.isNotEmpty() } ?: note.content.lines().firstOrNull()?.takeIf { it.isNotEmpty() } ?: "catetan")
                        val timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm").format(LocalDateTime.now())
                        exportFilename = "${baseName}_$timestamp"
                        showFilenameDialog = true
                    },
                    accentColorProvider = { note -> NoteColorMapper.getNoteAccentColorNonComposable(note.color, isDark) }
                )
            }
        }
    }
}

// Content for LIST view
@Composable
private fun NoteListContent(
    notes: List<NoteEntity>,
    isSelectionMode: Boolean,
    selectedNoteIds: List<Long>,
    onNoteClick: (NoteEntity) -> Unit,
    onLongClick: (Long) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onDelete: (NoteEntity) -> Unit,
    onPinToggle: (Long) -> Unit,
    onExport: (NoteEntity) -> Unit,
    accentColorProvider: (NoteEntity) -> Color
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(notes) { note ->
            val isSelected = selectedNoteIds.contains(note.id)
            NoteCardSurface(
                modifier = Modifier.fillMaxWidth(),
                isSelected = isSelected,
                isSelectionMode = isSelectionMode,
                onClick = { onNoteClick(note) },
                onLongClick = { onLongClick(note.id) },
                onToggleSelection = { onToggleSelection(note.id) },
                accentColor = accentColorProvider(note)
            ) {
                NoteItem(
                    note = note,
                    isSelected = isSelected,
                    isSelectionMode = isSelectionMode,
                    onClick = { onNoteClick(note) },
                    onLongClick = { onLongClick(note.id) },
                    onToggleSelection = { onToggleSelection(note.id) },
                    onDelete = { onDelete(note) },
                    onPinToggle = { onPinToggle(note.id) },
                    onExport = { onExport(note) }
                )
            }
        }
    }
}

// Content for GRID view
@Composable
private fun NoteGridContent(
    notes: List<NoteEntity>,
    isSelectionMode: Boolean,
    selectedNoteIds: List<Long>,
    onNoteClick: (NoteEntity) -> Unit,
    onLongClick: (Long) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onDelete: (NoteEntity) -> Unit,
    onPinToggle: (Long) -> Unit,
    onExport: (NoteEntity) -> Unit,
    accentColorProvider: (NoteEntity) -> Color
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(notes) { note ->
            val isSelected = selectedNoteIds.contains(note.id)
            NoteCardSurface(
                modifier = Modifier.heightIn(min = 200.dp),
                isSelected = isSelected,
                isSelectionMode = isSelectionMode,
                onClick = { onNoteClick(note) },
                onLongClick = { onLongClick(note.id) },
                onToggleSelection = { onToggleSelection(note.id) },
                accentColor = accentColorProvider(note)
            ) {
                NoteCardGrid(
                    note = note,
                    isSelected = isSelected,
                    isSelectionMode = isSelectionMode,
                    onClick = { onNoteClick(note) },
                    onLongClick = { onLongClick(note.id) },
                    onToggleSelection = { onToggleSelection(note.id) },
                    onDelete = { onDelete(note) },
                    onPinToggle = { onPinToggle(note.id) },
                    onExport = { onExport(note) }
                )
            }
        }
    }
}

/**
 * Permukaan kartu seragam untuk grid & list: Card surfaceVariant, sudut 12dp,
 * elevation 0, padding 12dp. Tint seleksi di atas Card.
 * Dipakai oleh NoteCardGrid dan NoteItem agar gaya konsisten.
 */
@Composable
fun NoteCardSurface(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleSelection: () -> Unit,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                containerColor,
                RoundedCornerShape(12.dp)
            )
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                else Modifier
            )
            .noteCardGesturesModifier(
                isSelectionMode = isSelectionMode,
                onClick = onClick,
                onLongClick = onLongClick,
                onToggleSelection = onToggleSelection
            ),
        contentAlignment = Alignment.TopStart
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

/**
 * Item catatan untuk tampilan LIST (horizontal).
 * Isi: bar warna kiri (4dp, radius 2dp, inset 12dp atas/bawah, gap 12dp ke teks),
 * judul max 2 baris, pratinjau max 3 baris, tanggal + ikon pin/share/delete.
 */
@Composable
fun NoteItem(
    note: NoteEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleSelection: () -> Unit,
    onDelete: () -> Unit,
    onPinToggle: () -> Unit,
    onExport: (() -> Unit)? = null
) {
    val displayTitle = remember(note.title, note.content) {
        if (note.title.isNotBlank()) note.title
        else note.content.take(50).substringBefore('\n').trim()
            .ifBlank { "Catatan tanpa judul" }
    }

    val previewText = remember(note.title, note.content) {
        if (note.title.isNotBlank()) note.content.take(240).trim()
        else ""
    }

    val createdDate = remember(note.createdAt) {
        formatRelativeDate(note.createdAt)
    }
    val showUpdatedDate = note.updatedAt - note.createdAt > 60_000L
    val updatedDate = remember(note.updatedAt) { formatRelativeDate(note.updatedAt) }

    val accentColor = NoteColorMapper.getNoteAccentColor(note.color)

    // Content Row dengan drawBehind untuk bar warna vertikal kiri.
    // drawBehind SEBELUM padding supaya koordinat relatif ke tepi kartu penuh.
    // Padding konten: start=28dp (12 inset + 4 bar + 12 gap), vertikal 12dp.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                // Koordinat relatif tepi kartu penuh (sebelum padding konten)
                val insetPx = 12.dp.toPx()
                val barWidthPx = 4.dp.toPx()
                val height = size.height
                val barHeight = (height - 2 * insetPx).coerceAtLeast(0f)
                drawRoundRect(
                    color = accentColor,
                    topLeft = Offset(insetPx, insetPx),
                    size = Size(barWidthPx, barHeight),
                    cornerRadius = CornerRadius(barWidthPx / 2f)
                )
            }
            .padding(start = 28.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .heightIn(min = 0.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Judul max 2 baris
            Text(
                text = displayTitle,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Pratinjau max 3 baris
            if (previewText.isNotBlank()) {
                Text(
                    text = previewText,
                    fontSize = 13.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Timestamps + icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Dibuat $createdDate",
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (showUpdatedDate) {
                        Text(
                            text = "Diubah $updatedDate",
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!isSelectionMode) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onPinToggle) {
                            Icon(
                                imageVector = if (note.isPinned) Icons.Filled.Star else StarOutlineIcon,
                                contentDescription = if (note.isPinned) "Batal pin" else "Pin",
                                tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onExport ?: {}) {
                            Icon(Icons.Filled.Share, contentDescription = "Ekspor", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Filled.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Kartu catatan untuk tampilan GRID (vertikal).
 * 1. Bar warna atas (6dp, ikut terpotong sudut Card)
 * 2. Judul max 2 baris
 * 3. Pratinjau max 3 baris (weight 1f, fill=false)
 * 4. Spacer mendorong bagian bawah ke tepi bawah
 * 5. Tanggal + baris ikon (tinggi tetap, selalu tampil)
 * Tint seleksi di atas Card. Semua teks pakai onSurface/onSurfaceVariant kontras.
 */
@Composable
fun NoteCardGrid(
    note: NoteEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleSelection: () -> Unit,
    onDelete: () -> Unit,
    onPinToggle: () -> Unit,
    onExport: (() -> Unit)? = null
) {
    val displayTitle = remember(note.title, note.content) {
        if (note.title.isNotBlank()) note.title
        else note.content.take(50).substringBefore('\n').trim()
            .ifBlank { "Catatan tanpa judul" }
    }

    val previewText = remember(note.title, note.content) {
        if (note.title.isNotBlank()) note.content.take(240).trim()
        else ""
    }

    val createdDate = remember(note.createdAt) {
        formatRelativeDate(note.createdAt)
    }
    val showUpdatedDate = note.updatedAt - note.createdAt > 60_000L
    val updatedDate = remember(note.updatedAt) { formatRelativeDate(note.updatedAt) }

    val accentColor = NoteColorMapper.getNoteAccentColor(note.color)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Bagian atas: bar warna + judul + pratinjau (mengisi ruang atas)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 1. Bar warna atas (6dp, ikut terpotong sudut Card)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(accentColor)
            )

            // 2. Judul max 2 baris
            Text(
                text = displayTitle,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 3. Pratinjau max 3 baris (weight 1f agar tidak mengisi ruang bawah)
            if (previewText.isNotBlank()) {
                Text(
                    text = previewText,
                    fontSize = 13.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Bottom timestamps and actions stay aligned along the card edge.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Dibuat $createdDate",
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (showUpdatedDate) {
                    Text(
                        text = "Diubah $updatedDate",
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!isSelectionMode) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPinToggle) {
                        Icon(
                            imageVector = if (note.isPinned) Icons.Filled.Star else StarOutlineIcon,
                            contentDescription = if (note.isPinned) "Batal pin" else "Pin",
                            tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onExport ?: {}) {
                        Icon(Icons.Filled.Share, contentDescription = "Ekspor", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

/**
 * Modifier gestur yang dipakai bersama oleh kartu daftar dan kartu grid.
 * Membaca state seleksi TERBARU melalui rememberUpdatedState agar keputusan
 * tap-vs-toggle tidak pakai nilai basi saat mode seleksi bertoggle.
 */
@Composable
fun Modifier.noteCardGesturesModifier(
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleSelection: () -> Unit
): Modifier {
    val updatedIsSelectionMode by rememberUpdatedState(isSelectionMode)
    val updatedOnClick by rememberUpdatedState(onClick)
    val updatedOnLongClick by rememberUpdatedState(onLongClick)
    val updatedOnToggleSelection by rememberUpdatedState(onToggleSelection)

    return this.then(pointerInput(Unit) {
        detectTapGestures(
            onLongPress = {
                if (!updatedIsSelectionMode) updatedOnLongClick()
            },
            onTap = {
                if (!updatedIsSelectionMode) {
                    updatedOnClick()
                } else {
                    updatedOnToggleSelection()
                }
            }
        )
    })
}

private fun formatRelativeDate(timestamp: Long): String {
    val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
    val now = LocalDateTime.now()

    // Always show date + time. Add the year only when it differs from the current year.
    val pattern = if (date.year == now.year) "d MMM, HH:mm" else "d MMM yyyy, HH:mm"
    return date.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
}

@Composable
private fun SingleNoteExportDialog(
    note: NoteEntity,
    onDismiss: () -> Unit,
    onExport: (ExportFormat) -> Unit
) {
    val items = listOf(
        ExportMenuItem(ExportFormat.TXT, "Text (.txt)", "Catatan sebagai teks biasa"),
        ExportMenuItem(ExportFormat.JSON, "JSON (.json)", "Data lengkap termasuk metadata"),
        ExportMenuItem(ExportFormat.MD, "Markdown (.md)", "Format Markdown dengan header")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ekspor Catatan", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.selectableGroup()
            ) {
                items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clickable { onExport(item.format); onDismiss() }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = item.desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

data class ExportMenuItem(
    val format: ExportFormat,
    val label: String,
    val desc: String
)
