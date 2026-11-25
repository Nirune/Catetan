package com.example.notesapp.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.notesapp.data.model.NoteEntity
import com.example.notesapp.presentation.viewmodel.NoteViewModel
import kotlin.math.ceil

private val retentionChoices = listOf(7, 14, 30, 60, 0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    viewModel: NoteViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val notes by viewModel.trashedNotes.collectAsState()
    var retentionDays by remember { mutableIntStateOf(ThemePreferences.trashRetentionDays) }
    var selectedNote by remember { mutableStateOf<NoteEntity?>(null) }
    var selectedNoteBeforePermanentDelete by remember { mutableStateOf<NoteEntity?>(null) }
    var permanentlyDeleteTargetId by remember { mutableStateOf<Long?>(null) }
    var showPermanentDelete by remember { mutableStateOf(false) }
    var showEmptyTrash by remember { mutableStateOf(false) }
    var showRetentionMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Sampah") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    Box {
                        TextButton(onClick = { showRetentionMenu = true }) {
                            Text("${retentionLabel(retentionDays)}")
                        }
                        DropdownMenu(
                            expanded = showRetentionMenu,
                            onDismissRequest = { showRetentionMenu = false }
                        ) {
                            retentionChoices.forEach { days ->
                                DropdownMenuItem(
                                    text = { Text(retentionLabel(days)) },
                                    onClick = {
                                        viewModel.setTrashRetentionDays(days)
                                        retentionDays = days
                                        showRetentionMenu = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = { showEmptyTrash = true },
                        enabled = notes.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Kosongkan Sampah")
                    }
                }
            )
        }
    ) { insets ->
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(insets),
                contentAlignment = Alignment.Center
            ) {
                Text("Sampah kosong", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(insets),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    val deletedAt = note.deletedAt ?: return@items
                    val preview = if (note.title.isNotBlank()) note.content else ""
                    val remainingDays = if (retentionDays == 0) null else {
                        ceil((deletedAt + retentionDays * 86_400_000.0 - System.currentTimeMillis()) / 86_400_000.0)
                            .toInt().coerceAtLeast(0)
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selectedNote = note },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(
                                note.title.ifBlank { note.content.lineSequence().firstOrNull().orEmpty().ifBlank { "Catatan tanpa judul" } },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (preview.isNotBlank()) {
                                Text(
                                    preview,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                "Dihapus ${formatNoteDate(deletedAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Text(
                                remainingDays?.let { "Terhapus permanen dalam $it hari" } ?: "Tidak pernah dihapus otomatis",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    selectedNote?.let { note ->
        AlertDialog(
            onDismissRequest = { selectedNote = null },
            title = { Text(note.title.ifBlank { "Catatan tanpa judul" }) },
            text = { Text("Pilih tindakan untuk catatan ini.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.restoreNote(note.id)
                    selectedNote = null
                }) { Text("Pulihkan") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPermanentDelete = true
                    selectedNoteBeforePermanentDelete = note
                    permanentlyDeleteTargetId = note.id
                    selectedNote = null
                }) {
                    Text("Hapus permanen", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    if (showPermanentDelete) {
        AlertDialog(
            onDismissRequest = { showPermanentDelete = false },
            title = { Text("Hapus permanen?") },
            text = { Text("Catatan ini tidak dapat dipulihkan setelah dihapus.") },
            confirmButton = {
                TextButton(onClick = {
                    permanentlyDeleteTargetId?.let { viewModel.permanentlyDeleteNote(it) }
                    showPermanentDelete = false
                    permanentlyDeleteTargetId = null
                }) { Text("Hapus permanen", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPermanentDelete = false
                    selectedNote = selectedNoteBeforePermanentDelete
                    permanentlyDeleteTargetId = null
                }) { Text("Batal") }
            }
        )
    }

    if (showEmptyTrash) {
        AlertDialog(
            onDismissRequest = { showEmptyTrash = false },
            title = { Text("Kosongkan Sampah?") },
            text = { Text("Semua ${notes.size} catatan di Sampah akan dihapus permanen.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.emptyTrash()
                    showEmptyTrash = false
                }) { Text("Kosongkan", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrash = false }) { Text("Batal") }
            }
        )
    }
}

private fun retentionLabel(days: Int): String = if (days == 0) "Tidak pernah" else "$days hari"
