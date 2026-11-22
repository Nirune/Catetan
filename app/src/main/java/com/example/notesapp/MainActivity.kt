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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.notesapp.data.model.NoteEntity
import com.example.notesapp.domain.repository.NoteRepository
import com.example.notesapp.presentation.ui.AddEditNoteScreen
import com.example.notesapp.presentation.ui.AppearanceScreen
import com.example.notesapp.presentation.ui.AppTheme
import com.example.notesapp.presentation.ui.NoteListScreen
import com.example.notesapp.presentation.ui.NoteListTopBarCallbacks
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_OPEN_TAB = "extra_open_tab"
    }

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
        val openTab = intent.getStringExtra(EXTRA_OPEN_TAB)
        setContent {
            val themeMode by noteViewModel.themeMode.collectAsState()
            val themeName by noteViewModel.themeName.collectAsState()
            AppTheme(themeMode = themeMode, themeName = themeName) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    NotesAppNavigation(
                        noteViewModel = noteViewModel,
                        todoViewModel = todoViewModel,
                        searchViewModel = searchViewModel,
                        openTab = openTab,
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
    openTab: String?,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onPickFile: () -> Unit,
    onPickFolder: () -> Unit
) {
    val navController = rememberNavController()
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var isSelectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedNoteIds by rememberSaveable { mutableStateOf(emptyList<Long>()) }
    var showTodoAddDialog by remember { mutableStateOf(false) }
    var exportClick by remember { mutableStateOf<(() -> Unit)?>(null) }
    var settingsClick by remember { mutableStateOf<(() -> Unit)?>(null) }
    var importClick by remember { mutableStateOf<(() -> Unit)?>(null) }
    var selectAllClick by remember { mutableStateOf<(() -> Unit)?>(null) }
    var exitSelectionClick by remember { mutableStateOf<(() -> Unit)?>(null) }
    var deleteSelectedClick by remember { mutableStateOf<(() -> Unit)?>(null) }

    val themeName by noteViewModel.themeName.collectAsState()

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
                    onExitSelection = { exitSelectionClick?.invoke() },
                    onSelectAll = { selectAllClick?.invoke() },
                    onDeleteSelected = { deleteSelectedClick?.invoke() },
                    onExportClick = { exportClick?.invoke() },
                    onImportClick = { importClick?.invoke() },
                    onSettingsClick = { settingsClick?.invoke() },
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
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    val items = listOf(
                        NavigationBarItem(
                            label = "Catatan",
                            route = "notes",
                            iconSelected = { Icon(Icons.Filled.Description, contentDescription = "Catatan") },
                            iconUnselected = { Icon(Icons.Outlined.Description, contentDescription = "Catatan") }
                        ),
                        NavigationBarItem(
                            label = "Todo",
                            route = "todo",
                            iconSelected = { Icon(Icons.Filled.Checklist, contentDescription = "Todo") },
                            iconUnselected = { Icon(Icons.Outlined.Checklist, contentDescription = "Todo") }
                        ),
                        NavigationBarItem(
                            label = "Cari",
                            route = "search",
                            iconSelected = { Icon(Icons.Filled.Search, contentDescription = "Cari") },
                            iconUnselected = { Icon(Icons.Outlined.Search, contentDescription = "Cari") }
                        )
                    )
                    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route ?: "notes"
                    val clampedDensity = with(LocalDensity.current) {
                        Density(density, fontScale.coerceAtMost(1.3f))
                    }
                    items.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    // Save/restore state when switching tabs
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = if (selected) item.iconSelected else item.iconUnselected,
                            label = {
                                CompositionLocalProvider(LocalDensity provides clampedDensity) {
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = openTab ?: "notes",
            enterTransition = { fadeIn(animationSpec = tween(150)) },
            exitTransition = { fadeOut(animationSpec = tween(100)) },
            popEnterTransition = { fadeIn(animationSpec = tween(150)) },
            popExitTransition = { fadeOut(animationSpec = tween(100)) }
        ) {
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
                    onTopBarCallbacks = { callbacks: NoteListTopBarCallbacks ->
                        exportClick = callbacks.onExport
                        settingsClick = callbacks.onSettings
                        importClick = callbacks.onImport
                        selectAllClick = callbacks.onSelectAll
                        exitSelectionClick = callbacks.onExitSelection
                        deleteSelectedClick = callbacks.onDeleteSelected
                    },
                    onThemeModeChanged = onThemeModeChanged,
                    onAppearanceClick = { navController.navigate("appearance") },
                    themeName = themeName,
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
            composable("appearance") {
                AppearanceScreen(
                    viewModel = noteViewModel,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

private data class NavigationBarItem(
    val label: String,
    val route: String,
    val iconSelected: @Composable () -> Unit,
    val iconUnselected: @Composable () -> Unit
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
    // Search punya field sendiri; top bar cuma nyisihin inset status bar biar field nggak nempel jam.
    Box(modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
    )
}
