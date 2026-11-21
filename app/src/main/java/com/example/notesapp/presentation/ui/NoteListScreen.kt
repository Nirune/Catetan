package com.example.notesapp.presentation.ui

import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notesapp.data.model.NoteEntity
import com.example.notesapp.presentation.ui.ThemeMode
import com.example.notesapp.presentation.ui.ThemePreferences
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

private val SelectAllIcon: ImageVector = ImageVector.Builder(
    name = "Outlined.SelectAll",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).addPath(
    pathData = addPathNodes(SELECT_ALL_PATH),
    fill = SolidColor(Color.Black)
).build()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    viewModel: NoteViewModel,
    onNoteClick: (NoteEntity) -> Unit = {},
    onAddClick: () -> Unit = {},
    onThemeModeChanged: (ThemeMode) -> Unit = {},
    onPickFile: () -> Unit = {},
    onPickFolder: () -> Unit = {}
) {
    val notes by viewModel.allNotes.collectAsState()
    var showSingleExportDialog by remember { mutableStateOf(false) }
    var noteToExport by remember { mutableStateOf<NoteEntity?>(null) }
    var noteToDelete by remember { mutableStateOf<NoteEntity?>(null) }
    var notesToDelete by remember { mutableStateOf<List<NoteEntity>>(emptyList()) }
    val exportResult by viewModel.exportResult.collectAsState()
    val importResult by viewModel.importResult.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showThemeDialog by remember { mutableStateOf(false) }

    var showFilenameDialog by remember { mutableStateOf(false) }
    var exportFilename by remember { mutableStateOf("") }
    var pendingNoteForExport by remember { mutableStateOf<NoteEntity?>(null) }
    var pendingFormatForExport by remember { mutableStateOf<ExportFormat?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Mode seleksi multi catatan
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedNoteIds by rememberSaveable { mutableStateOf(emptyList<Long>()) }

    fun isNoteSelected(note: NoteEntity): Boolean = selectedNoteIds.contains(note.id)

    fun enterSelectionMode(noteId: Long) {
        isSelectionMode = true
        if (!selectedNoteIds.contains(noteId)) {
            selectedNoteIds = selectedNoteIds + listOf(noteId)
        }
    }

    fun toggleSelection(noteId: Long) {
        if (selectedNoteIds.contains(noteId)) {
            selectedNoteIds = selectedNoteIds - listOf(noteId)
            if (selectedNoteIds.isEmpty()) {
                isSelectionMode = false
            }
        } else {
            selectedNoteIds = selectedNoteIds + listOf(noteId)
        }
    }

    fun exitSelectionMode() {
        isSelectionMode = false
        selectedNoteIds = emptyList()
        notesToDelete = emptyList()
    }

    fun toggleSelectAll() {
        if (selectedNoteIds.size == notes.size && notes.isNotEmpty()) {
            selectedNoteIds = emptyList()
            // Tetap di mode seleksi, counter 0 / N
        } else {
            selectedNoteIds = notes.map { it.id }
            isSelectionMode = true
        }
    }

    // Bersihkan id seleksi yang sudah tidak ada di daftar (contoh: setelah hapus)
    LaunchedEffect(notes) {
        val validIds = notes.map { it.id }.toSet()
        selectedNoteIds = selectedNoteIds.filter { validIds.contains(it) }
        if (selectedNoteIds.isEmpty() && isSelectionMode) {
            exitSelectionMode()
        }
        notesToDelete = notesToDelete.filter { validIds.contains(it.id) }
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

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedNoteIds.size} / ${notes.size}") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    navigationIcon = {
                        IconButton(onClick = { exitSelectionMode() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Keluar seleksi",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    },
                    actions = {
                        val allSelected = selectedNoteIds.size == notes.size && notes.isNotEmpty()
                        IconButton(onClick = { toggleSelectAll() }) {
                            Icon(
                                imageVector = SelectAllIcon,
                                contentDescription = if (allSelected) "Batalkan semua" else "Pilih semua",
                                tint = if (allSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        IconButton(onClick = {
                            notesToDelete = notes.filter { selectedNoteIds.contains(it.id) }
                            showDeleteConfirmation = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus terpilih",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text(stringResource(id = R.string.app_name), fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.surface
                        else MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = if (isDark) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    actions = {
                        IconButton(onClick = {
                            val timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm").format(LocalDateTime.now())
                            exportFilename = "catetan_$timestamp"
                            pendingNoteForExport = null
                            pendingFormatForExport = null
                            showFilenameDialog = true
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Export")
                        }
                        IconButton(onClick = { onPickFile() }) {
                            Icon(Icons.Default.Add, contentDescription = "Impor")
                        }
                        IconButton(onClick = { showThemeDialog = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Pengaturan")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = onAddClick,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Note")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        BackHandler(enabled = isSelectionMode) {
            exitSelectionMode()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (notes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Belum ada catatan.\nTap tombol '+' di bawah buat bikin!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp
                    )
                }
            } else {
                if (viewMode == ViewMode.GRID) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(notes, key = { it.id }) { note ->
                            val selected = isNoteSelected(note)
                            NoteCardGrid(
                                note = note,
                                isSelected = selected,
                                isSelectionMode = isSelectionMode,
                                onClick = { onNoteClick(note) },
                                onLongClick = { enterSelectionMode(note.id) },
                                onToggleSelection = { toggleSelection(note.id) },
                                onDelete = { noteToDelete = note },
                                onPinToggle = { viewModel.togglePin(note.id) },
                                onExport = { showSingleNoteExportDialog(note) }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(notes, key = { it.id }) { note ->
                            val selected = isNoteSelected(note)
                            NoteItem(
                                note = note,
                                isSelected = selected,
                                isSelectionMode = isSelectionMode,
                                onClick = { onNoteClick(note) },
                                onLongClick = { enterSelectionMode(note.id) },
                                onToggleSelection = { toggleSelection(note.id) },
                                onDelete = { noteToDelete = note },
                                onPinToggle = { viewModel.togglePin(note.id) },
                                onExport = { showSingleNoteExportDialog(note) }
                            )
                        }
                    }
                }
            }

            // Filename input dialog (shown before export)
            if (showFilenameDialog) {
                AlertDialog(
                    onDismissRequest = { showFilenameDialog = false },
                    title = { Text(stringResource(R.string.filename_title), fontWeight = FontWeight.Bold) },
                    text = {
                        var dialogText by remember { mutableStateOf(exportFilename) }
                        var dialogSelection by remember { mutableStateOf(TextRange(0, exportFilename.length)) }

                        LaunchedEffect(exportFilename) {
                            dialogText = exportFilename
                            dialogSelection = TextRange(0, exportFilename.length)
                        }

                        TextField(
                            value = TextFieldValue(text = dialogText, selection = dialogSelection),
                            onValueChange = {
                                dialogText = it.text
                                dialogSelection = TextRange(it.text.length, it.text.length)
                                exportFilename = it.text
                            },
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
                                // Per-note export: show format dialog next
                                noteToExport = pendingNoteForExport
                                showSingleExportDialog = true
                                pendingNoteForExport = null
                            } else {
                                // Global export (.ctn)
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
                    onDismissRequest = { noteToDelete = null },
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
                                noteToDelete = null
                            }
                        ) {
                            Text("Hapus", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { noteToDelete = null }) {
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
                                val ids = notesToDelete.map { it.id }
                                viewModel.deleteNotesByIds(ids)
                                notesToDelete = emptyList()
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

            // Theme settings dialog
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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 56.dp)
                                    .clickable(onClick = {
                                        showThemeDialog = false
                                        onPickFolder()
                                    })
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.export_path_pick),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.weight(1f))
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

                            // Tema section
                            Text(
                                text = stringResource(R.string.theme),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Column(
                                modifier = Modifier.selectableGroup()
                            ) {
                                val options = listOf(
                                    Pair(stringResource(R.string.theme_auto), ThemeMode.AUTO),
                                    Pair(stringResource(R.string.theme_light), ThemeMode.LIGHT),
                                    Pair(stringResource(R.string.theme_dark), ThemeMode.DARK)
                                )
                                options.forEachIndexed { _, (label, mode) ->
                                    val isSelected = themeMode == mode
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                            .selectable(
                                                selected = isSelected,
                                                enabled = true,
                                                onClick = {
                                                    onThemeModeChanged(mode)
                                                }
                                            ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = null,
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = MaterialTheme.colorScheme.primary,
                                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showThemeDialog = false }) {
                            Text(stringResource(R.string.close))
                        }
                    }
                )
            }
        }
    }
}

// Single note export dialog
@Composable
fun SingleNoteExportDialog(
    note: NoteEntity,
    onDismiss: () -> Unit,
    onExport: (ExportFormat) -> Unit
) {
    val formats = listOf(
        ExportMenuItem(ExportFormat.TXT, "Text (.txt)", "Plain text sederhana"),
        ExportMenuItem(ExportFormat.JSON, "JSON (.json)", "Format terstruktur untuk backup"),
        ExportMenuItem(ExportFormat.MD, "Markdown (.md)", "Format markdown untuk docs")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Ekspor \"${note.title}\"",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Column(
                    modifier = Modifier.selectableGroup()
                ) {
                    formats.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clickable { onExport(item.format) }
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

// Bintang aktif (note di-pin): kuning terisi, konsisten di tema terang maupun gelap
private val PinActiveYellow = Color(0xFFFFD600)

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
        else note.content.takeWhile { it != '\n' }.take(50).trim()
            .ifBlank { "Catatan tanpa judul" }
    }

    val previewText = remember(note.title, note.content) {
        if (note.title.isNotBlank()) note.content
        else ""
    }.trim()

    val relativeDate = remember(note.updatedAt) {
        formatRelativeDate(note.updatedAt)
    }

    val accentColor = NoteColorMapper.getNoteAccentColor(note.color)
    val selectionTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isSelected) selectionTint else MaterialTheme.colorScheme.surface)
                .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)
                .noteCardGesturesModifier(
                    isSelectionMode = isSelectionMode,
                    onClick = onClick,
                    onLongClick = onLongClick,
                    onToggleSelection = onToggleSelection
                ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vertical color strip (6dp pill shape, margin kiri 0 karena Row punya padding)
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(accentColor)
            )

            // Content area (title + preview)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title
                Text(
                    text = displayTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Preview (only show if there's content beyond title)
                if (previewText.isNotBlank()) {
                    Text(
                        text = previewText,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Right side: date + actions (disembunyikan selama mode seleksi)
            if (!isSelectionMode) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = relativeDate,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPinToggle,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = if (note.isPinned) "Unpin" else "Pin",
                                modifier = Modifier.size(20.dp),
                                tint = if (note.isPinned) PinActiveYellow
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        onExport?.let {
                            IconButton(
                                onClick = it,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Export",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Divider tipis antar item (inset dari kiri, mulai dari teks)
        Divider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            modifier = Modifier.padding(start = 30.dp)
        )
    }
}

/**
 * Kartu note untuk tampilan grid. Card surfaceVariant, sudut 12dp.
 * Layout sederhana: Column padding 12dp, urutan:
 * 1. Bar warna note 6dp (ikut terpotong sudut)
 * 2. Judul max 2 baris
 * 3. Pratinjau max 3 baris
 * 4. Spacer(weight=1f) mendorong bagian bawah ke tepi bawah
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
        else note.content.takeWhile { it != '\n' }.take(50).trim()
            .ifBlank { "Catatan tanpa judul" }
    }

    val previewText = remember(note.title, note.content) {
        if (note.title.isNotBlank()) note.content
        else ""
    }.trim()

    val relativeDate = remember(note.updatedAt) {
        formatRelativeDate(note.updatedAt)
    }

    val accentColor = NoteColorMapper.getNoteAccentColor(note.color)
    val selectionTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val cardBackground = MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .noteCardGesturesModifier(
                isSelectionMode = isSelectionMode,
                onClick = onClick,
                onLongClick = onLongClick,
                onToggleSelection = onToggleSelection
            )
            .then(
                if (isSelected) Modifier.background(selectionTint, RoundedCornerShape(12.dp))
                else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = cardBackground,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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

            // 3. Pratinjau max 3 baris
            if (previewText.isNotBlank()) {
                Text(
                    text = previewText,
                    fontSize = 13.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 4. Spacer mendorong bagian bawah ke tepi bawah kartu
            Spacer(modifier = Modifier.weight(1f))

            // 5. Bawah: tanggal kiri + ikon kanan (selalu tampil, tidak terdorong)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = relativeDate,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!isSelectionMode) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPinToggle,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = if (note.isPinned) "Unpin" else "Pin",
                                modifier = Modifier.size(20.dp),
                                tint = if (note.isPinned) PinActiveYellow
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        onExport?.let {
                            IconButton(
                                onClick = it,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Export",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatRelativeDate(timestamp: Long): String {
    val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
    val now = LocalDateTime.now()

    // Always show date + time. Add the year only when it differs from the current year.
    val pattern = if (date.year == now.year) "d MMM, HH:mm" else "d MMM yyyy, HH:mm"
    return date.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
}