package com.example.notesapp.presentation.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.LocalContentColor
import com.example.notesapp.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.platform.LocalViewConfiguration
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
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs
import com.example.notesapp.data.model.NoteEntity
import com.example.notesapp.presentation.ui.ThemeMode
import com.example.notesapp.presentation.ui.NoteSortOrder
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
    val onDeleteSelected: () -> Unit,
    val onMoveSelected: () -> Unit
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
    onVisibleSelectionStats: (selected: Int, total: Int) -> Unit,
    onNoteClick: (NoteEntity) -> Unit = {},
    onAddClick: () -> Unit = {},
    onTopBarCallbacks: ((NoteListTopBarCallbacks) -> Unit)? = null,
    onThemeModeChanged: (ThemeMode) -> Unit = {},
    onAppearanceClick: () -> Unit,
    themeName: String,
    onPickFile: () -> Unit = {},
    onPickFolder: () -> Unit = {},
    onTrashClick: () -> Unit = {}
) {
    val notes by viewModel.allNotes.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val activeFolderId by viewModel.activeFolderId.collectAsState()
    val folderNotes by viewModel.notesInActiveFolder.collectAsState()
    val sortOrder by viewModel.noteSortOrder.collectAsState()
    val sortedNotes = remember(folderNotes, sortOrder) { sortNotes(folderNotes, sortOrder) }
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
    val trashCount by viewModel.trashCount.collectAsState()
    var showSortDialog by remember { mutableStateOf(false) }
    var folderDialogMode by remember { mutableStateOf<String?>(null) }
    var folderTarget by remember { mutableStateOf<com.example.notesapp.data.model.FolderEntity?>(null) }
    var folderNameDraft by remember { mutableStateOf("") }
    var folderError by remember { mutableStateOf<String?>(null) }
    var showDeleteFolderConfirmation by remember { mutableStateOf(false) }
    val folderChipState = rememberLazyListState()
    var folderSwipeDirection by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val viewConfiguration = LocalViewConfiguration.current

    var showAllExportDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    var showMoveFolderDialog by remember { mutableStateOf(false) }
    var showNewFolderForMoveDialog by remember { mutableStateOf(false) }
    var newFolderForMoveName by remember { mutableStateOf("") }
    var moveFolderError by remember { mutableStateOf<String?>(null) }

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

    fun selectFolderAt(index: Int) {
        val activeIndex = activeFolderId?.let { id -> folders.indexOfFirst { it.id == id } } ?: -1
        val currentIndex = if (activeIndex < 0) 0 else activeIndex + 1
        if (index != currentIndex) folderSwipeDirection = if (index > currentIndex) 1 else -1
        when (index) {
            0 -> viewModel.setActiveFolder(null)
            in 1..folders.size -> folders.getOrNull(index - 1)?.let { viewModel.setActiveFolder(it.id) }
        }
    }

    fun moveFolderBy(direction: Int) {
        if (isSelectionMode || direction == 0) return
        val activeIndex = activeFolderId?.let { id -> folders.indexOfFirst { it.id == id } } ?: -1
        val currentIndex = if (activeIndex < 0) 0 else activeIndex + 1
        val targetIndex = (currentIndex + direction).coerceIn(0, folders.size)
        if (targetIndex != currentIndex) {
            selectFolderAt(targetIndex)
        }
    }

    LaunchedEffect(activeFolderId, folders) {
        val folderIndex = activeFolderId?.let { id -> folders.indexOfFirst { it.id == id } } ?: -1
        val activeIndex = if (folderIndex < 0) 0 else folderIndex + 1
        folderChipState.animateScrollToItem(activeIndex.coerceIn(0, folders.size))
    }

    fun toggleSelectAll() {
        val visibleIds = sortedNotes.map { it.id }
        if (visibleIds.isNotEmpty() && visibleIds.all { it in selectedNoteIds }) {
            onSelectionChange(emptyList())
            // Tetap di mode seleksi, counter 0 / N
        } else {
            onSelectionChange(visibleIds)
            onSelectionModeChange(visibleIds.isNotEmpty())
        }
    }

    // Provide top bar callbacks to parent (MainActivity) for NotesTopBar.
    // Didefinisikan setelah helper seleksi supaya bisa manggil fungsi lokal itu.
    fun provideTopBarCallbacks() {
        onTopBarCallbacks?.let {
            it(
                NoteListTopBarCallbacks(
                    onExport = { showAllExportDialog = true },
                    onSettings = { showThemeDialog = true },
                    onImport = onPickFile,
                    onSelectAll = { toggleSelectAll() },
                    onExitSelection = { exitSelectionMode() },
                    onDeleteSelected = {
                        val visibleIds = sortedNotes.map { it.id }.toSet()
                        notesToDeleteIds = selectedNoteIds.filter { it in visibleIds }
                        showDeleteConfirmation = notesToDeleteIds.isNotEmpty()
                    },
                    onMoveSelected = { showMoveFolderDialog = true }
                )
            )
        }
    }

    provideTopBarCallbacks()

    // Changing folder clears selection; selections are scoped to the visible folder.
    LaunchedEffect(activeFolderId) {
        if (isSelectionMode || selectedNoteIds.isNotEmpty()) exitSelectionMode()
    }
    LaunchedEffect(sortedNotes) {
        val validIds = sortedNotes.map { it.id }.toSet()
        val newSelection = selectedNoteIds.filter { it in validIds }
        if (newSelection != selectedNoteIds) onSelectionChange(newSelection)
        if (newSelection.isEmpty() && isSelectionMode) onSelectionModeChange(false)
        notesToDeleteIds = notesToDeleteIds.filter { it in validIds }
        if (notesToDeleteIds.isEmpty()) {
            showDeleteConfirmation = false
        }
    }

    val currentVisibleSelectionCount = sortedNotes.count { it.id in selectedNoteIds }
    val currentVisibleNoteCount = sortedNotes.size
    SideEffect {
        onVisibleSelectionStats(currentVisibleSelectionCount, currentVisibleNoteCount)
    }

    val themeMode by viewModel.themeMode.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val isDark = isEffectiveDarkTheme(themeMode)
    val currentExportPath = ThemePreferences.exportPathUri
    // Handle export result
    LaunchedEffect(exportResult) {
        exportResult?.let { result ->
            when (result) {
                is ExportResult.Success -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Diekspor ke ${result.uri ?: result.fileName}",
                            actionLabel = "OK"
                        )
                    }
                }
                is ExportResult.Error -> {
                    scope.launch {
                        val action = if (result.message.contains("folder ekspor", ignoreCase = true)) "Pilih folder" else "OK"
                        val snackbar = snackbarHostState.showSnackbar(
                            message = result.message,
                            actionLabel = action
                        )
                        if (snackbar == SnackbarResult.ActionPerformed && action == "Pilih folder") {
                            onPickFolder()
                        }
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
                            message = "${result.importedCount} catatan diimpor, ${result.createdFolderCount} folder baru dibuat",
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
                    modifier = Modifier
                        .padding(24.dp)
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
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

                    Text(
                        text = "Data",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = {
                                showThemeDialog = false
                                onTrashClick()
                            })
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sampah",
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
                            text = if (trashCount == 0) "Sampah kosong" else "$trashCount catatan",
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

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = { showSortDialog = true })
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Urutkan", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.weight(1f))
                            Text("›", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = sortOrderLabel(sortOrder),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text(stringResource(R.string.close)) }
            }
        )
    }

    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = { Text("Urutkan catatan") },
            text = {
                Column {
                    NoteSortOrder.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable {
                                    viewModel.setNoteSortOrder(option)
                                    showSortDialog = false
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = sortOrder == option,
                                onClick = {
                                    viewModel.setNoteSortOrder(option)
                                    showSortDialog = false
                                }
                            )
                            Text(sortOrderLabel(option), color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSortDialog = false }) { Text("Tutup") }
            }
        )
    }

    val exportTargetNote = noteToExport
    if (showSingleExportDialog && exportTargetNote != null) {
        SingleNoteExportDialog(
            note = exportTargetNote,
            onDismiss = {
                showSingleExportDialog = false
                noteToExport = null
            },
            onExport = { format ->
                viewModel.exportSingleNote(exportTargetNote, format)
                showSingleExportDialog = false
                noteToExport = null
            }
        )
    }

    if (showAllExportDialog) {
        AllNotesExportDialog(
            noteCount = notes.size,
            onDismiss = { showAllExportDialog = false },
            onExport = { format ->
                viewModel.exportAllNotes(format)
                showAllExportDialog = false
            }
        )
    }

    val selectedNotesForMove = notes.filter { selectedNoteIds.contains(it.id) && it.deletedAt == null }
    val commonFolderId = selectedNotesForMove.map { it.folderId }.distinct().singleOrNull()
    val commonFolderSelection = selectedNotesForMove.isNotEmpty() && selectedNotesForMove.map { it.folderId }.distinct().size == 1
    val moveTargetFolderSnapshot = selectedNotesForMove

    fun closeMoveDialog() {
        showMoveFolderDialog = false
    }

    fun commitMove(targetFolderId: Long?, label: String) {
        val ids = selectedNotesForMove.map { it.id }
        scope.launch {
            if (ids.isNotEmpty() && moveTargetFolderSnapshot.all { it.folderId != targetFolderId }) {
                viewModel.moveNotesToFolder(ids, targetFolderId)
            snackbarHostState.showSnackbar("${ids.size} catatan dipindahkan ke $label")
            }
            closeMoveDialog()
            onSelectionChange(emptyList())
            onSelectionModeChange(false)
        }
    }

    if (showMoveFolderDialog) {
        AlertDialog(
            onDismissRequest = { closeMoveDialog() },
            title = { Text("Pindahkan ke folder") },
            text = {
                Column {
                    MoveFolderOption("Tanpa folder", commonFolderSelection && commonFolderId == null) {
                        commitMove(null, "Tanpa folder")
                    }
                    folders.forEach { folder ->
                        MoveFolderOption(folder.name, commonFolderSelection && commonFolderId == folder.id) {
                            commitMove(folder.id, folder.name)
                        }
                    }
                    MoveFolderOption("+ Folder baru", false) {
                        closeMoveDialog()
                        newFolderForMoveName = ""
                        moveFolderError = null
                        showNewFolderForMoveDialog = true
                    }
                }
            },
            confirmButton = { TextButton(onClick = { closeMoveDialog() }) { Text("Tutup") } }
        )
    }

    if (showNewFolderForMoveDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderForMoveDialog = false; moveFolderError = null },
            title = { Text("Folder baru") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newFolderForMoveName,
                        onValueChange = { newFolderForMoveName = it; moveFolderError = null },
                        singleLine = true,
                        label = { Text("Nama folder") },
                        supportingText = { Text("Maksimal 24 karakter") }
                    )
                    moveFolderError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val (id, error) = viewModel.createFolderWithId(newFolderForMoveName)
                        if (error != null) {
                            moveFolderError = error
                        } else if (id != null) {
                            showNewFolderForMoveDialog = false
                            commitMove(id, newFolderForMoveName.trim())
                        }
                    }
                }) { Text("Buat dan pindahkan") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderForMoveDialog = false; moveFolderError = null }) { Text("Batal") }
            }
        )
    }

    // Delete confirmation dialog
        noteToDelete?.let { note ->
            AlertDialog(
                onDismissRequest = { noteToDeleteId = -1L },
                title = { Text("Pindahkan ke Sampah", fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Catatan \"${note.title.ifBlank { note.content.lineSequence().firstOrNull().orEmpty().ifBlank { "Catatan tanpa judul" } }}\" akan dipindahkan ke Sampah. " +
                            if (note.isPinned) "Bintangnya akan dilepas." else ""
                    )
                },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteNote(note)
                        noteToDeleteId = -1L
                    }
                ) {
                    Text("Pindahkan", color = MaterialTheme.colorScheme.error)
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
                title = { Text("Pindahkan ke Sampah", fontWeight = FontWeight.Bold) },
            text = {
                val pinnedCount = notesToDelete.count { it.isPinned }
                    val base = "Catatan ${notesToDelete.size} akan dipindahkan ke Sampah."
                val warning = if (pinnedCount > 0) {
                    "\n\n${pinnedCount} di antaranya berbintang."
                } else ""
                Text(base + warning)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val visibleIds = sortedNotes.map { it.id }.toSet()
                        val ids = notesToDeleteIds.filter { it in visibleIds }
                        viewModel.deleteNotesByIds(ids)
                        notesToDeleteIds = emptyList()
                        showDeleteConfirmation = false
                        exitSelectionMode()
                    }
                ) {
                    Text("Pindahkan", color = MaterialTheme.colorScheme.error)
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

        Column(modifier = Modifier.fillMaxSize()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                state = folderChipState,
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item(key = "folder_chip_all") {
                    FolderFilterChip(
                        label = "Semua",
                        selected = activeFolderId == null,
                        onClick = { selectFolderAt(0) },
                        bringIntoViewRequester = remember { BringIntoViewRequester() },
                        activeKey = activeFolderId == null
                    )
                }
                itemsIndexed(folders, key = { _, folder -> "folder_chip_${folder.id}" }) { index, folder ->
                    FolderFilterChip(
                        label = folder.name,
                        selected = activeFolderId == folder.id,
                        onClick = { selectFolderAt(index + 1) },
                        bringIntoViewRequester = remember(folder.id) { BringIntoViewRequester() },
                        activeKey = activeFolderId == folder.id,
                        onLongClick = {
                            folderTarget = folder
                            folderNameDraft = folder.name
                            folderDialogMode = "menu"
                        }
                    )
                }
                item(key = "folder_chip_add") {
                    FolderFilterChip(label = "+", selected = false, onClick = {
                        folderTarget = null
                        folderNameDraft = ""
                        folderError = null
                        folderDialogMode = "create"
                    })
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .folderSwipeGesture(
                        enabled = !isSelectionMode,
                        edgeExclusionFraction = 0.13f,
                        touchSlop = viewConfiguration.touchSlop,
                        thresholdPx = with(density) { 64.dp.toPx() },
                        onSwipe = { direction -> moveFolderBy(direction) }
                    )
            ) {
                AnimatedContent(
                    targetState = activeFolderId,
                    transitionSpec = {
                        val enteringFrom = if (folderSwipeDirection > 0) 1 else -1
                        slideInHorizontally { fullWidth -> enteringFrom * fullWidth } togetherWith
                            slideOutHorizontally { fullWidth -> -enteringFrom * fullWidth }
                    },
                    label = "folderContentSlide"
                ) { displayedFolderId ->
                    val displayedNotes = remember(notes, displayedFolderId, sortOrder) {
                        val inFolder = if (displayedFolderId == null) notes else notes.filter { it.folderId == displayedFolderId }
                        sortNotes(inFolder, sortOrder)
                    }
                    if (displayedNotes.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (displayedFolderId != null) "Folder kosong" else "Belum ada catatan",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        when (viewMode) {
                            ViewMode.LIST -> NoteListContent(
                                notes = displayedNotes,
                                isSelectionMode = isSelectionMode,
                                selectedNoteIds = selectedNoteIds,
                                onNoteClick = onNoteClick,
                                onLongClick = { noteId -> enterSelectionMode(noteId) },
                                onToggleSelection = { noteId -> toggleSelection(noteId) },
                                onDelete = { note -> noteToDeleteId = note.id },
                                onPinToggle = { noteId -> viewModel.togglePin(noteId) },
                                onExport = { note ->
                                    noteToExport = note
                                    showSingleExportDialog = true
                                },
                                accentColorProvider = { note -> NoteColorMapper.getNoteAccentColorNonComposable(note.color, isDark) }
                            )
                            ViewMode.GRID -> NoteGridContent(
                                notes = displayedNotes,
                                isSelectionMode = isSelectionMode,
                                selectedNoteIds = selectedNoteIds,
                                onNoteClick = onNoteClick,
                                onLongClick = { noteId -> enterSelectionMode(noteId) },
                                onToggleSelection = { noteId -> toggleSelection(noteId) },
                                onDelete = { note -> noteToDeleteId = note.id },
                                onPinToggle = { noteId -> viewModel.togglePin(noteId) },
                                onExport = { note ->
                                    noteToExport = note
                                    showSingleExportDialog = true
                                },
                                accentColorProvider = { note -> NoteColorMapper.getNoteAccentColorNonComposable(note.color, isDark) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (folderDialogMode == "create" || folderDialogMode == "rename") {
        val isRename = folderDialogMode == "rename"
        AlertDialog(
            onDismissRequest = { folderDialogMode = null; folderError = null },
            title = { Text(if (isRename) "Ganti nama folder" else "Folder baru") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = folderNameDraft,
                        onValueChange = { folderNameDraft = it; folderError = null },
                        singleLine = true,
                        label = { Text("Nama folder") },
                        supportingText = { Text("Maksimal 24 karakter") }
                    )
                    folderError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val renameTarget = folderTarget
                    scope.launch {
                        folderError = if (isRename) {
                            renameTarget?.let { viewModel.renameFolder(it.id, folderNameDraft) }
                        } else viewModel.createFolder(folderNameDraft)
                        if (folderError == null) folderDialogMode = null
                    }
                }) { Text(if (isRename) "Simpan" else "Buat") }
            },
            dismissButton = {
                TextButton(onClick = { folderDialogMode = null; folderError = null }) { Text("Batal") }
            }
        )
    }

    val menuFolderTarget = folderTarget
    if (folderDialogMode == "menu" && menuFolderTarget != null) {
        AlertDialog(
            onDismissRequest = { folderDialogMode = null },
            title = { Text(menuFolderTarget.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Pilih tindakan untuk folder ini.")
                    TextButton(onClick = {
                        folderNameDraft = menuFolderTarget.name
                        folderError = null
                        folderDialogMode = "rename"
                    }) { Text("Ganti nama") }
                    TextButton(onClick = {
                        folderDialogMode = null
                        scope.launch { viewModel.exportFolder(menuFolderTarget.id) }
                    }) { Text("Ekspor folder") }
                    TextButton(onClick = {
                        showDeleteFolderConfirmation = true
                        folderDialogMode = null
                    }) { Text("Hapus folder", color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = { folderDialogMode = null }) { Text("Tutup") }
            }
        )
    }

    val deleteFolderTarget = folderTarget
    if (showDeleteFolderConfirmation && deleteFolderTarget != null) {
        AlertDialog(
            onDismissRequest = { showDeleteFolderConfirmation = false; folderTarget = null },
            title = { Text("Hapus folder?") },
            text = {
                Text("Hapus folder ${deleteFolderTarget.name}? Catatan di dalamnya tidak ikut terhapus dan akan menjadi tanpa folder.")
            },
            confirmButton = {
                TextButton(onClick = {
                    val folderId = deleteFolderTarget.id
                    showDeleteFolderConfirmation = false
                    folderTarget = null
                    scope.launch { viewModel.deleteFolder(folderId) }
                }) { Text("Hapus folder", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteFolderConfirmation = false; folderTarget = null }) { Text("Batal") }
            }
        )
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
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
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
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(notes) { note ->
            val isSelected = selectedNoteIds.contains(note.id)
            NoteCardSurface(
                modifier = Modifier.height(gridCardHeight()),
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
    val context = LocalContext.current
    val displayTitle = remember(note.title, note.content) {
        if (note.title.isNotBlank()) note.title
        else note.content.take(50).substringBefore('\n').trim()
            .ifBlank { "Catatan tanpa judul" }
    }

    val previewText = remember(note.title, note.content) {
        if (note.title.isNotBlank()) note.content.replace(Regex("\\n{2,}"), "\n").trim().take(240)
        else ""
    }

    val createdDate = remember(note.createdAt) {
        formatNoteDate(note.createdAt)
    }
    val showUpdatedDate = note.updatedAt - note.createdAt > 60_000L
    val updatedDate = remember(note.updatedAt) { formatNoteDate(note.updatedAt) }

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
                        var shareMenuExpanded by remember(note.id) { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { shareMenuExpanded = true }) {
                                Icon(Icons.Filled.Share, contentDescription = "Bagikan", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            DropdownMenu(
                                expanded = shareMenuExpanded,
                                onDismissRequest = { shareMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Bagikan teks") },
                                    onClick = {
                                        shareMenuExpanded = false
                                        shareNoteText(context, note)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Simpan sebagai file…") },
                                    onClick = {
                                        shareMenuExpanded = false
                                        onExport?.invoke()
                                    }
                                )
                            }
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
    val context = LocalContext.current
    val iconButtonSize = 36.dp
    val displayTitle = remember(note.title, note.content) {
        if (note.title.isNotBlank()) note.title
        else note.content.take(50).substringBefore('\n').trim()
            .ifBlank { "Catatan tanpa judul" }
    }

    val previewText = remember(note.title, note.content) {
        if (note.title.isNotBlank()) note.content.replace(Regex("\\n{2,}"), "\n").trim().take(240)
        else ""
    }

    val createdDate = remember(note.createdAt) {
        formatNoteDate(note.createdAt)
    }
    val showUpdatedDate = note.updatedAt - note.createdAt > 60_000L
    val updatedDate = remember(note.updatedAt) { formatNoteDate(note.updatedAt) }

    val accentColor = NoteColorMapper.getNoteAccentColor(note.color)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Bagian atas: bar warna + judul + pratinjau (mengisi ruang atas)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
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

        // Dates and actions are stacked to preserve space in the narrow grid cell.
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPinToggle,
                        modifier = Modifier.size(iconButtonSize)
                    ) {
                        Icon(
                            imageVector = if (note.isPinned) Icons.Filled.Star else StarOutlineIcon,
                            contentDescription = if (note.isPinned) "Batal pin" else "Pin",
                            tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    var shareMenuExpanded by remember(note.id) { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { shareMenuExpanded = true },
                            modifier = Modifier.size(iconButtonSize)
                        ) {
                            Icon(
                                Icons.Filled.Share,
                                contentDescription = "Bagikan",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = shareMenuExpanded,
                            onDismissRequest = { shareMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Bagikan teks") },
                                onClick = {
                                    shareMenuExpanded = false
                                    shareNoteText(context, note)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Simpan sebagai file…") },
                                onClick = {
                                    shareMenuExpanded = false
                                    onExport?.invoke()
                                }
                            )
                        }
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(iconButtonSize)
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Hapus",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun shareNoteText(context: android.content.Context, note: NoteEntity) {
    val shareText = if (note.title.isBlank()) note.content else "${note.title}\n\n${note.content}"
    if (shareText.length > 100_000) {
        Toast.makeText(
            context,
            "Catatan terlalu panjang untuk dibagikan sebagai teks, gunakan Simpan sebagai file",
            Toast.LENGTH_LONG
        ).show()
        return
    }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, note.title)
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    try {
        context.startActivity(Intent.createChooser(sendIntent, "Bagikan catatan"))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Tidak ada aplikasi untuk membagikan teks", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun gridCardHeight(): androidx.compose.ui.unit.Dp {
    val density = LocalDensity.current
    val fontScale = density.fontScale.coerceAtMost(1.3f)
    val titleHeight = MaterialTheme.typography.bodyLarge.lineHeight.value.dp.coerceAtLeast(20.dp) * 2
    val previewHeight = 13.dp * 1.25f * 3
    val dateHeight = MaterialTheme.typography.labelSmall.lineHeight.value.dp.coerceAtLeast(14.dp) * 2 + 2.dp
    val topContent = 6.dp + 4.dp + titleHeight + 4.dp + previewHeight
    val bottomContent = dateHeight + 2.dp + 36.dp
    val contentPadding = 24.dp
    return ((topContent + bottomContent + contentPadding) * fontScale).coerceIn(216.dp, 300.dp)
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

private fun sortOrderLabel(order: NoteSortOrder): String = when (order) {
    NoteSortOrder.NEWEST_CREATED -> "Terbaru dibuat"
    NoteSortOrder.OLDEST_CREATED -> "Terlama dibuat"
    NoteSortOrder.LAST_MODIFIED -> "Terakhir diubah"
    NoteSortOrder.TITLE_ASC -> "Abjad (A-Z)"
    NoteSortOrder.COLOR -> "Warna"
}

private fun Modifier.folderSwipeGesture(
    enabled: Boolean,
    edgeExclusionFraction: Float,
    touchSlop: Float,
    thresholdPx: Float,
    onSwipe: (Int) -> Unit
): Modifier = pointerInput(enabled, edgeExclusionFraction, touchSlop, thresholdPx) {
    if (!enabled) return@pointerInput
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val width = size.width.toFloat()
        val safeLeft = width * edgeExclusionFraction
        val safeRight = width * (1f - edgeExclusionFraction)
        if (down.position.x !in safeLeft..safeRight) return@awaitEachGesture

        val tracker = VelocityTracker()
        tracker.addPosition(down.uptimeMillis, down.position)
        var totalX = 0f
        var axisLocked: Boolean? = null
        var pointerReleased = false
        while (!pointerReleased) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            tracker.addPosition(change.uptimeMillis, change.position)
            val dx = change.position.x - down.position.x
            val dy = change.position.y - down.position.y
            totalX = dx
            if (axisLocked == null && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                axisLocked = abs(dx) > 2f * abs(dy)
            }
            if (axisLocked == true) change.consume()
            pointerReleased = change.changedToUpIgnoreConsumed()
        }
        if (axisLocked == true) {
            val velocity = tracker.calculateVelocity().x
            if (abs(totalX) > thresholdPx || abs(velocity) > 900f) {
                val swipeX = if (abs(totalX) > thresholdPx) totalX else velocity
                onSwipe(if (swipeX < 0f) 1 else -1)
            }
        }
    }
}

@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun FolderFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    bringIntoViewRequester: BringIntoViewRequester? = null,
    activeKey: Boolean = false
) {
    LaunchedEffect(activeKey) {
        if (activeKey) bringIntoViewRequester?.bringIntoView()
    }
    Surface(
        modifier = (bringIntoViewRequester?.let { Modifier.bringIntoViewRequester(it) } ?: Modifier)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun sortNotes(notes: List<NoteEntity>, order: NoteSortOrder): List<NoteEntity> {
    val pinned = notes.filter { it.isPinned }
    val unpinned = notes.filterNot { it.isPinned }
    val comparator = when (order) {
        NoteSortOrder.NEWEST_CREATED -> compareByDescending<NoteEntity> { it.createdAt }
            .thenByDescending { it.updatedAt }
        NoteSortOrder.OLDEST_CREATED -> compareBy<NoteEntity> { it.createdAt }
            .thenByDescending { it.updatedAt }
        NoteSortOrder.LAST_MODIFIED -> compareByDescending<NoteEntity> { it.updatedAt }
        NoteSortOrder.TITLE_ASC -> compareBy<NoteEntity> { it.title.isBlank() }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            .thenByDescending { it.updatedAt }
        NoteSortOrder.COLOR -> {
            val colorOrder = NoteColorMapper.allNoteColors.withIndex().associate { it.value to it.index }
            compareBy<NoteEntity> { colorOrder[it.color] ?: Int.MAX_VALUE }
                .thenByDescending { it.updatedAt }
        }
    }
    return pinned.sortedWith(comparator) + unpinned.sortedWith(comparator)
}

@Composable
private fun MoveFolderOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SingleNoteExportDialog(
    note: NoteEntity,
    onDismiss: () -> Unit,
    onExport: (ExportFormat) -> Unit
) {
    ExportFormatDialog(
        subtitle = note.title.ifBlank { note.content.lineSequence().firstOrNull().orEmpty() }
            .ifBlank { "Catatan tanpa judul" },
        onDismiss = onDismiss,
        onExport = onExport
    )
}

@Composable
private fun AllNotesExportDialog(
    noteCount: Int,
    onDismiss: () -> Unit,
    onExport: (ExportFormat) -> Unit
) {
    ExportFormatDialog(
        subtitle = "Semua catatan ($noteCount)",
        onDismiss = onDismiss,
        onExport = onExport
    )
}

@Composable
private fun ExportFormatDialog(
    subtitle: String,
    onDismiss: () -> Unit,
    onExport: (ExportFormat) -> Unit
) {
    val formats = listOf(
        ExportMenuItem(ExportFormat.TXT, "Text (.txt)", "Ekspor catatan sebagai teks biasa yang mudah dibaca."),
        ExportMenuItem(ExportFormat.JSON, "JSON (.json)", "Ekspor data catatan beserta metadata dalam format JSON."),
        ExportMenuItem(ExportFormat.MD, "Markdown (.md)", "Ekspor catatan dengan judul dan isi dalam format Markdown.")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ekspor Catatan", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.heightIn(max = 440.dp)) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    formats.forEachIndexed { index, item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .clickable(onClick = { onExport(item.format) })
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = item.desc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (index < formats.lastIndex) {
                            Divider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

private data class ExportMenuItem(
    val format: ExportFormat,
    val label: String,
    val desc: String
)
