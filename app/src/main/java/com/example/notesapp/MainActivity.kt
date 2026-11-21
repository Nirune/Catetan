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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.notesapp.data.model.NoteEntity
import com.example.notesapp.presentation.ui.AddEditNoteScreen
import com.example.notesapp.presentation.ui.AppTheme
import com.example.notesapp.presentation.ui.NoteListScreen
import com.example.notesapp.presentation.ui.ThemeMode
import com.example.notesapp.presentation.ui.ThemePreferences
import com.example.notesapp.presentation.viewmodel.NoteViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: NoteViewModel by viewModels()

    // File picker launcher untuk impor catatan
    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importFromUri(it) }
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
            val themeMode by viewModel.themeMode.collectAsState()
            AppTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    NotesAppNavigation(
                        viewModel = viewModel,
                        onThemeModeChanged = { newMode ->
                            viewModel.setThemeMode(newMode)
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

@Composable
fun NotesAppNavigation(
    viewModel: NoteViewModel,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onPickFile: () -> Unit,
    onPickFolder: () -> Unit
) {
    val navController = rememberNavController()
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }

    NavHost(navController = navController, startDestination = "list") {
        composable("list") {
            NoteListScreen(
                viewModel = viewModel,
                onNoteClick = { note ->
                    editingNote = note
                    navController.navigate("add_edit")
                },
                onAddClick = {
                    editingNote = null
                    navController.navigate("add_edit")
                },
                onThemeModeChanged = onThemeModeChanged,
                onPickFile = onPickFile,
                onPickFolder = onPickFolder
            )
        }
        composable("add_edit") {
            AddEditNoteScreen(
                viewModel = viewModel,
                noteToEdit = editingNote,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
