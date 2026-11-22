package com.example.notesapp.presentation.ui

import android.app.Activity
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notesapp.R
import com.example.notesapp.data.model.TodoEntity
import com.example.notesapp.presentation.viewmodel.TodoViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("EXPERIMENTAL_API_USAGE")
@Composable
fun TodoScreen(
    viewModel: TodoViewModel,
    modifier: Modifier = Modifier,
    showAddDialog: Boolean,
    onAddDialogDismiss: () -> Unit
) {
    val todos by viewModel.allTodos.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var editingTodo by remember { mutableStateOf<TodoEntity?>(null) }
    var editText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    // Separate active and completed todos
    val activeTodos = todos.filter { !it.isCompleted }
    val completedTodos = todos.filter { it.isCompleted }

    // Add todo dialog
    if (showAddDialog) {
        AddTodoDialog(
            onDismiss = onAddDialogDismiss,
            onAdd = { text ->
                viewModel.addTodo(text)
                scope.launch {
                    snackbarHostState.showSnackbar(message = "Todo ditambahkan", actionLabel = "OK")
                }
                onAddDialogDismiss()
            },
            focusRequester = focusRequester
        )
    }

    // Edit todo dialog
    editingTodo?.let { todo ->
        EditTodoDialog(
            todo = todo,
            initialText = todo.text,
            onDismiss = {
                editingTodo = null
                editText = ""
            },
            onSave = { newText ->
                if (newText.trim().isNotEmpty()) {
                    viewModel.updateTodoText(todo.id, newText.trim())
                    scope.launch {
                        snackbarHostState.showSnackbar(message = "Todo diperbarui", actionLabel = "OK")
                    }
                }
                editingTodo = null
                editText = ""
            },
            focusRequester = focusRequester
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        // Active todos section
        if (activeTodos.isNotEmpty()) {
            SectionHeader(title = "Aktif", count = activeTodos.size)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(activeTodos) { _, todo ->
                    TodoItem(
                        todo = todo,
                        onToggle = { viewModel.toggleComplete(todo) },
                        onEdit = {
                            editingTodo = todo
                            editText = todo.text
                        },
                        onDelete = {
                            viewModel.deleteTodo(todo)
                            scope.launch {
                                snackbarHostState.showSnackbar(message = "Todo dihapus", actionLabel = "OK")
                            }
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Completed todos section
        if (completedTodos.isNotEmpty()) {
            SectionHeader(title = "Selesai", count = completedTodos.size)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(completedTodos) { _, todo ->
                    TodoItem(
                        todo = todo,
                        onToggle = { viewModel.toggleComplete(todo) },
                        onEdit = {
                            editingTodo = todo
                            editText = todo.text
                        },
                        onDelete = {
                            viewModel.deleteTodo(todo)
                            scope.launch {
                                snackbarHostState.showSnackbar(message = "Todo dihapus", actionLabel = "OK")
                            }
                        }
                    )
                }
            }
        }

        // Empty state
        if (todos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckBoxOutlineBlank,
                        contentDescription = "Todo kosong",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Belum ada todo",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tambah todo pertama dengan tombol + di bawah",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Text(
        text = "$title ($count)",
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun TodoItem(
    todo: TodoEntity,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isCompleted = todo.isCompleted
    val textColor = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
    val containerColor = if (isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = textColor
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isCompleted,
                onCheckedChange = { _ -> onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = todo.text,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                overflow = TextOverflow.Ellipsis,
                maxLines = 3,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onEdit)
                        .padding(vertical = 8.dp)
                    ),
                textDecoration = if (isCompleted) TextDecoration.LineThrough else null
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Hapus",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AddTodoDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    focusRequester: FocusRequester
) {
    var text by remember { mutableStateOf("") }
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Todo Baru", fontWeight = FontWeight.Bold) },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = { Text("Apa yang perlu dilakukan?") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
                keyboardActions = KeyboardActions(onDone = { onAdd(text.trim()); onDismiss() }),
                singleLine = true,
                maxLines = 1
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (text.trim().isNotEmpty()) {
                    onAdd(text.trim())
                } else {
                    onDismiss()
                }
            }) {
                Text("Tambah")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
    LaunchedEffect(Unit) {
        keyboardController?.show()
        focusRequester.requestFocus()
    }
}

@Composable
private fun EditTodoDialog(
    todo: TodoEntity,
    initialText: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    focusRequester: FocusRequester
) {
    var text by remember { mutableStateOf(initialText) }
    val keyboardController = LocalSoftwareKeyboardController.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Todo", fontWeight = FontWeight.Bold) },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = { Text("Apa yang perlu dilakukan?") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
                keyboardActions = KeyboardActions(onDone = { onSave(text.trim()); onDismiss() }),
                singleLine = true,
                maxLines = 1
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (text.trim().isNotEmpty()) {
                    onSave(text.trim())
                } else {
                    onDismiss()
                }
            }) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
    LaunchedEffect(Unit) {
        keyboardController?.show()
        focusRequester.requestFocus()
    }
}