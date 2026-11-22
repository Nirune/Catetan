package com.example.notesapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.notesapp.data.model.NoteEntity
import com.example.notesapp.domain.repository.NoteRepository
import com.example.notesapp.presentation.ui.AddEditNoteScreen
import com.example.notesapp.presentation.ui.AppTheme
import com.example.notesapp.presentation.ui.NoteListScreen
import com.example.notesapp.presentation.ui.SearchScreen
import com.example.notesapp.presentation.ui.TodoScreen
import com.example.notesapp.presentation.ui.ThemeMode
import com.example.notesapp.presentation.ui.ThemePreferences
import com.example.notesapp.presentation.ui.SelectAllIcon
import com.example.notesapp.presentation.ui.isEffectiveDarkTheme
import com.example.notesapp.presentation.viewmodel.NoteViewModel
import com.example.notesapp.presentation.viewmodel.SearchViewModel
import com.example.notesapp.presentation.viewmodel.TodoViewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


class MainActivity : ComponentActivity() {
    private val noteViewModel: NoteViewModel by viewModels()
    private val todoViewModel: TodoViewModel by viewModels()
    private val searchViewModel: SearchViewModel by lazy {
        val noteRepository = noteViewModel.getRepository()
        SearchViewModel(application, noteRepository)
    }

    // File picker launcher untuk impor catatan
    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { noteViewModel.importFromUri(it) }
    }

    // Folder picker launcher untuk ganti path export
    private val pickFolderLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { ThemePreferences.setExportPathUri(it.toString()) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemePreferences.init(this)
        enableEdgeToEdge()
        setContent {
            val themeMode by noteViewModel.themeMode.collectAsState()
            AppTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    NotesAppNavigation(
                        noteViewModel = noteViewModel,
                        todoViewModel = todoViewModel,
                        searchViewModel = searchViewModel,
                        onThemeModeChanged = { newMode ->
                            noteViewModel.setThemeMode(newMode)
                        },
                        onPickFile = {
                            pickFileLauncher.launch(arrayOf<String>("*/*"))
                        },
                        onPickFolder = {
                            pickFolderLauncher.launch(null)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesAppNavigation(
    noteViewModel: NoteViewModel,
    todoViewModel: TodoViewModel,
    searchViewModel: SearchViewModel,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onPickFile: () -> Unit,
    onPickFolder: () -> Unit
) {
    val navController = rememberNavController()
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedNoteIds by rememberSaveable { mutableStateOf(emptyList<Long>()) }
    var showTodoAddDialog by remember { mutableStateOf(false) }
    var exportClick by remember { mutableStateOf<() -> Unit>({}) }
    var settingsClick by remember { mutableStateOf<() -> Unit>({}) }
    var importClick by remember { mutableStateOf<() -> Unit>({}) }

    // Bottom bar visibility - only show on main 3 tabs
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val showBottomBar = remember(currentBackStackEntry) {
        derivedStateOf {
            val currentRoute = currentBackStackEntry?.destination?.route
            currentRoute == "notes" || currentRoute == "todo" || currentRoute == "search"
        }
    }.value

    Scaffold(
        topBar = {
            when (navController.currentBackStackEntryAsState().value?.destination?.route) {
                "notes" -> NotesTopBar(
                    isSelectionMode = isSelectionMode,
                    selectedCount = selectedNoteIds.size,
                    totalCount = noteViewModel.allNotes.value.size,
                    onExitSelection = { isSelectionMode = false; selectedNoteIds = emptyList() },
                    onSelectAll = {
                        val notes = noteViewModel.allNotes.value
                        if (selectedNoteIds.size == notes.size && notes.isNotEmpty()) {
                            selectedNoteIds = emptyList()
                        } else {
                            selectedNoteIds = notes.map { it.id }
                            isSelectionMode = true
                        }
                    },
                    onDeleteSelected = { /* NoteListScreen handles batch delete via callback */ },
                    onExportClick = exportClick,
                    onImportClick = importClick,
                    onSettingsClick = settingsClick,
                    noteViewModel = noteViewModel
                )
                "todo" -> TodoTopBar()
                "search" -> SearchTopBar()
                else -> null
            }
        },
        floatingActionButton = {
            when (navController.currentBackStackEntryAsState().value?.destination?.route) {
                "notes" -> if (!isSelectionMode) {
                    FloatingActionButton(
                        onClick = {
                            editingNote = null
                            navController.navigate("add_edit")
                        },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah Catatan")
                    }
                }
                "todo" -> FloatingActionButton(
                    onClick = { showTodoAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Todo")
                }
                else -> null
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    val items = listOf(
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Note, contentDescription = "Catatan") },
                            label = "Catatan",
                            route = "notes"
                        ),
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.CheckBoxOutlineBlank, contentDescription = "Todo") },
                            label = "Todo",
                            route = "todo"
                        ),
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Search, contentDescription = "Cari") },
                            label = "Cari",
                            route = "search"
                        )
                    )
                    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route ?: "notes"
                    items.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    // Save/restore state when switching tabs
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = item.icon,
                            label = { Text(item.label) },
                            alwaysShowLabel = false
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = "notes") {
            composable("notes") {
                NoteListScreen(
                    viewModel = noteViewModel,
                    modifier = Modifier.padding(innerPadding),
                    isSelectionMode = isSelectionMode,
                    selectedNoteIds = selectedNoteIds,
                    onSelectionModeChange = { isSelectionMode = it },
                    onSelectionChange = { selectedNoteIds = it },
                    onNoteClick = { note ->
                        editingNote = note
                        navController.navigate("add_edit")
                    },
                    onAddClick = {
                        editingNote = null
                        navController.navigate("add_edit")
                    },
                    onTopBarCallbacks = { export, settings, import_ ->
                        exportClick = export
                        settingsClick = settings
                        importClick = import_
                    },
                    onThemeModeChanged = onThemeModeChanged,
                    onPickFile = onPickFile,
                    onPickFolder = onPickFolder
                )
            }
            composable("todo") {
                TodoScreen(
                    viewModel = todoViewModel,
                    modifier = Modifier.padding(innerPadding),
                    showAddDialog = showTodoAddDialog,
                    onAddDialogDismiss = { showTodoAddDialog = false }
                )
            }
            composable("search") {
                SearchScreen(
                    searchViewModel = searchViewModel,
                    noteViewModel = noteViewModel,
                    modifier = Modifier.padding(innerPadding),
                    onNoteClick = { note ->
                        editingNote = note
                        navController.navigate("add_edit")
                    }
                )
            }
            composable("add_edit") {
                AddEditNoteScreen(
                    viewModel = noteViewModel,
                    noteToEdit = editingNote,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

private data class NavigationBarItem(
    val icon: @Composable () -> Unit,
    val label: String,
    val route: String
)

@Composable
fun NotesTopBar(
    isSelectionMode: Boolean,
    selectedCount: Int,
    totalCount: Int,
    onExitSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onSettingsClick: () -> Unit,
    noteViewModel: NoteViewModel
) {
    val themeMode by noteViewModel.themeMode.collectAsState()
    val effectiveDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.AUTO -> isEffectiveDarkTheme(themeMode)
    }

    if (isSelectionMode) {
        TopAppBar(
            title = { Text("$selectedCount / $totalCount") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            navigationIcon = {
                IconButton(onClick = onExitSelection) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Keluar seleksi",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            },
            actions = {
                val allSelected = selectedCount == totalCount && totalCount > 0
                IconButton(onClick = onSelectAll) {
                    Icon(
                        imageVector = SelectAllIcon,
                        contentDescription = if (allSelected) "Batalkan semua" else "Pilih semua",
                        tint = if (allSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                IconButton(onClick = onDeleteSelected) {
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
                containerColor = if (effectiveDark) MaterialTheme.colorScheme.surface
                else MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = if (effectiveDark) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onPrimaryContainer
            ),
            actions = {
                IconButton(onClick = onExportClick) {
                    Icon(Icons.Default.Share, contentDescription = "Export")
                }
                IconButton(onClick = onImportClick) {
                    Icon(Icons.Default.Add, contentDescription = "Impor")
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "Pengaturan")
                }
            }
        )
    }
}

@Composable
fun TodoTopBar() {
    TopAppBar(
        title = { Text(stringResource(id = R.string.app_name), fontWeight = FontWeight.Bold) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun SearchTopBar() {
    // Search screen has its own search field, so no top bar needed
    Box(modifier = Modifier
        .fillMaxWidth()
        .height(0.dp)
    )
}