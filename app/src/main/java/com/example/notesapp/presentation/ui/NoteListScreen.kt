package com.example.notesapp.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.notesapp.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notesapp.data.model.NoteEntity
import com.example.notesapp.presentation.ui.ThemeMode
import com.example.notesapp.presentation.ui.ThemePreferences
import com.example.notesapp.presentation.ui.isEffectiveDarkTheme
import com.example.notesapp.presentation.viewmodel.ExportFormat
import com.example.notesapp.presentation.viewmodel.ExportResult
import com.example.notesapp.presentation.viewmodel.NoteViewModel
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    viewModel: NoteViewModel,
    onNoteClick: (NoteEntity) -> Unit = {},
    onAddClick: () -> Unit = {},
    onThemeModeChanged: (ThemeMode) -> Unit = {}
) {
    val notes by viewModel.allNotes.collectAsState()
    var showSingleExportDialog by remember { mutableStateOf(false) }
    var noteToExport by remember { mutableStateOf<NoteEntity?>(null) }
    var noteToDelete by remember { mutableStateOf<NoteEntity?>(null) }
    val exportResult by viewModel.exportResult.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showThemeDialog by remember { mutableStateOf(false) }

    val themeMode = ThemePreferences.themeMode
    val isDark = isEffectiveDarkTheme(themeMode)

    fun showSingleNoteExportDialog(note: NoteEntity) {
        noteToExport = note
        showSingleExportDialog = true
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.app_name), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = if (isDark) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { viewModel.exportCtn() }) {
                        Icon(Icons.Default.Share, contentDescription = "Export")
                    }
                    IconButton(onClick = { showThemeDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Pengaturan")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(notes, key = { it.id }) { note ->
                        NoteItem(
                            note = note,
                            onClick = { onNoteClick(note) },
                            onDelete = { noteToDelete = note },
                            onPinToggle = { viewModel.togglePin(note.id) },
                            onExport = { showSingleNoteExportDialog(note) }
                        )
                    }
                }
            }

            // Single Note Export Dialog
            if (showSingleExportDialog && noteToExport != null) {
                SingleNoteExportDialog(
                    note = noteToExport!!,
                    onDismiss = { showSingleExportDialog = false },
                    onExport = { format ->
                        viewModel.exportSingleNote(noteToExport!!, format)
                        showSingleExportDialog = false
                        noteToExport = null
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
                                                    ThemePreferences.setThemeMode(mode)
                                                    showThemeDialog = false
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

@Composable
fun NoteItem(
    note: NoteEntity,
    onClick: () -> Unit,
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

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .background(MaterialTheme.colorScheme.surface)
                .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
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

            // Right side: date + actions
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

        // Divider tipis antar item (inset dari kiri, mulai dari teks)
        Divider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            modifier = Modifier.padding(start = 30.dp)
        )
    }
}

private fun formatRelativeDate(timestamp: Long): String {
    val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
    val now = LocalDateTime.now()

    // Always show date + time. Add the year only when it differs from the current year.
    val pattern = if (date.year == now.year) "d MMM, HH:mm" else "d MMM yyyy, HH:mm"
    return date.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
}