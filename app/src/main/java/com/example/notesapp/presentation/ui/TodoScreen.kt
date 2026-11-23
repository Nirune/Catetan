package com.example.notesapp.presentation.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import com.example.notesapp.R
import com.example.notesapp.data.model.TodoEntity
import com.example.notesapp.presentation.viewmodel.TodoViewModel
import com.example.notesapp.reminder.ReminderScheduler
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
    val context = LocalContext.current
    var editingTodoId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editDraft by rememberSaveable { mutableStateOf("") }
    var reminderPickerTarget by rememberSaveable(stateSaver = ReminderPickerTargetSaver) {
        mutableStateOf<ReminderPickerTarget?>(null)
    }
    var newTodoText by rememberSaveable { mutableStateOf("") }
    var newTodoReminderAt by rememberSaveable { mutableStateOf<Long?>(null) }
    var suppressDialogAutofocus by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.todo_reminder_permission_denied),
                    actionLabel = context.getString(R.string.todo_reminder_notification_settings),
                    withDismissAction = true
                )
                if (result == SnackbarResult.ActionPerformed) {
                    val settingsIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(settingsIntent)
                }
            }
        }
    }

    val activeTodos = todos.filter { !it.isCompleted }
    val completedTodos = todos.filter { it.isCompleted }
    val lifecycleOwner = LocalLifecycleOwner.current
    val hasFutureReminder = activeTodos.any { it.reminderAt?.let { at -> at > System.currentTimeMillis() } == true }
    val tickerNow = remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isTodoScreenStarted by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) isTodoScreenStarted = true
            if (event == Lifecycle.Event.ON_STOP) isTodoScreenStarted = false
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(hasFutureReminder, isTodoScreenStarted) {
        if (hasFutureReminder && isTodoScreenStarted) {
            while (isActive) {
                val now = System.currentTimeMillis()
                tickerNow.longValue = now
                if (activeTodos.none { it.reminderAt?.let { at -> at > now } == true }) break
                delay(1000L - now % 1000L)
            }
        }
    }
    LaunchedEffect(showAddDialog) {
        if (showAddDialog) suppressDialogAutofocus = false
    }

    val editTodo = editingTodoId?.let { id -> todos.firstOrNull { it.id == id } }

    if (showAddDialog && reminderPickerTarget == null) {
        AddTodoDialog(
            text = newTodoText,
            onTextChange = { newTodoText = it },
            reminderAt = newTodoReminderAt,
            onReminderClick = {
                focusManager.clearFocus()
                keyboardController?.hide()
                suppressDialogAutofocus = true
                reminderPickerTarget = ReminderPickerTarget.New(newTodoReminderAt)
            },
            onDismiss = {
                newTodoText = ""
                newTodoReminderAt = null
                onAddDialogDismiss()
            },
            onAdd = { text, reminderAt ->
                if (reminderAt != null) {
                    viewModel.addTodoWithReminder(text, reminderAt)
                } else {
                    viewModel.addTodo(text)
                }
                scope.launch {
                    snackbarHostState.showSnackbar(message = "Todo ditambahkan", actionLabel = "OK")
                }
                newTodoText = ""
                newTodoReminderAt = null
                onAddDialogDismiss()
            },
            requestFocusOnShow = !suppressDialogAutofocus
        )
    }

    if (editTodo != null && reminderPickerTarget == null) {
        EditTodoDialog(
            text = editDraft,
            onTextChange = { editDraft = it },
            reminderAt = editTodo.reminderAt,
            onDismiss = {
                editingTodoId = null
                editDraft = ""
            },
            onSave = { newText ->
                if (newText.isNotEmpty()) {
                    viewModel.updateTodoText(editTodo.id, newText)
                    scope.launch {
                        snackbarHostState.showSnackbar(message = "Todo diperbarui", actionLabel = "OK")
                    }
                }
                editingTodoId = null
                editDraft = ""
            },
            onReminderClick = {
                focusManager.clearFocus()
                keyboardController?.hide()
                suppressDialogAutofocus = true
                reminderPickerTarget = ReminderPickerTarget.Existing(editTodo.id, editTodo.text, editTodo.reminderAt)
            },
            requestFocusOnShow = !suppressDialogAutofocus
        )
    }

    reminderPickerTarget?.let { target ->
        val text = when (target) {
            is ReminderPickerTarget.New -> newTodoText
            is ReminderPickerTarget.Existing -> target.text
        }
        val existingReminderAt = when (target) {
            is ReminderPickerTarget.New -> target.pendingReminderAt
            is ReminderPickerTarget.Existing -> target.reminderAt
        }
        ReminderPicker(
            targetText = text,
            existingReminderAt = existingReminderAt,
            onDismiss = {
                reminderPickerTarget = null
                suppressDialogAutofocus = true
            },
            onSet = { reminderAt ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                if (!ReminderScheduler.canScheduleExactAlarms(context)) {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = context.getString(R.string.todo_reminder_exact_alarm_denied),
                            actionLabel = context.getString(R.string.todo_reminder_exact_alarm_settings),
                            withDismissAction = true
                        ).let { result ->
                            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                ReminderScheduler.openExactAlarmSettings(context)
                            }
                        }
                    }
                }
                when (target) {
                    is ReminderPickerTarget.New -> {
                        newTodoReminderAt = reminderAt
                        reminderPickerTarget = null
                        suppressDialogAutofocus = true
                    }
                    is ReminderPickerTarget.Existing -> {
                        viewModel.setReminder(target.id, reminderAt)
                        reminderPickerTarget = null
                        suppressDialogAutofocus = true
                    }
                }
            },
            onClear = {
                when (target) {
                    is ReminderPickerTarget.New -> {
                        newTodoReminderAt = null
                        reminderPickerTarget = null
                        suppressDialogAutofocus = true
                    }
                    is ReminderPickerTarget.Existing -> {
                        viewModel.clearReminder(target.id)
                        reminderPickerTarget = null
                        suppressDialogAutofocus = true
                    }
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
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
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (activeTodos.isNotEmpty()) {
                    item(key = "header_aktif") {
                        SectionHeader(title = "Aktif", count = activeTodos.size)
                    }
                    items(activeTodos, key = { "aktif_${it.id}" }) { todo ->
                        TodoItem(
                            todo = todo,
                            onToggle = { viewModel.toggleComplete(todo) },
                            onEdit = {
                                suppressDialogAutofocus = false
                                editingTodoId = todo.id
                                editDraft = todo.text
                            },
                            onDelete = {
                                viewModel.deleteTodo(todo)
                                scope.launch {
                                    snackbarHostState.showSnackbar(message = "Todo dihapus", actionLabel = "OK")
                                }
                            },
                            onReminderClick = { reminderPickerTarget = ReminderPickerTarget.Existing(todo.id, todo.text, todo.reminderAt) },
                            tickerNow = tickerNow
                        )
                    }
                }
                if (completedTodos.isNotEmpty()) {
                    item(key = "header_selesai") {
                        Column {
                            if (activeTodos.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            SectionHeader(title = "Selesai", count = completedTodos.size)
                        }
                    }
                    items(completedTodos, key = { "selesai_${it.id}" }) { todo ->
                        TodoItem(
                            todo = todo,
                            onToggle = { viewModel.toggleComplete(todo) },
                            onEdit = {
                                suppressDialogAutofocus = false
                                editingTodoId = todo.id
                                editDraft = todo.text
                            },
                            onDelete = {
                                viewModel.deleteTodo(todo)
                                scope.launch {
                                    snackbarHostState.showSnackbar(message = "Todo dihapus", actionLabel = "OK")
                                }
                            },
                            onReminderClick = { reminderPickerTarget = ReminderPickerTarget.Existing(todo.id, todo.text, todo.reminderAt) },
                            tickerNow = tickerNow
                        )
                    }
                }
            }
        }
    }
}

private sealed class ReminderPickerTarget {
    data class New(var pendingReminderAt: Long?) : ReminderPickerTarget()
    data class Existing(val id: Long, val text: String, val reminderAt: Long?) : ReminderPickerTarget()
}

private val ReminderPickerTargetSaver = androidx.compose.runtime.saveable.Saver<ReminderPickerTarget?, List<Any>>(
    save = { target ->
        when (target) {
            null -> listOf(0L)
            is ReminderPickerTarget.New -> listOf(1L, target.pendingReminderAt ?: -1L)
            is ReminderPickerTarget.Existing -> listOf(2L, target.id, target.text, target.reminderAt ?: -1L)
        }
    },
    restore = { saved ->
        when (saved.firstOrNull()) {
            0L -> null
            1L -> ReminderPickerTarget.New((saved.getOrNull(1) as? Long)?.takeIf { it >= 0L })
            2L -> ReminderPickerTarget.Existing(
                id = saved.getOrNull(1) as? Long ?: -1L,
                text = saved.getOrNull(2) as? String ?: "",
                reminderAt = (saved.getOrNull(3) as? Long)?.takeIf { it >= 0L }
            )
            else -> null
        }
    }
)

@Composable
private fun SectionHeader(title: String, count: Int) {
    Text(
        text = "$title ($count)",
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )
}

private val zoneId = ZoneId.systemDefault()
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun formatReminderLabel(millis: Long): String {
    val now = Instant.now()
    val at = Instant.ofEpochMilli(millis)
    val today = now.atZone(zoneId).toLocalDate()
    val targetDate = at.atZone(zoneId).toLocalDate()
    val time = timeFormatter.format(at.atZone(zoneId))
    return when (targetDate) {
        today -> "Hari ini $time"
        today.plusDays(1) -> "Besok $time"
        else -> "${targetDate.dayOfMonth} ${targetDate.monthValue} $time"
    }
}

@Composable
private fun ReminderCountdownText(
    reminderAt: Long,
    tickerNow: androidx.compose.runtime.State<Long>,
    style: androidx.compose.ui.text.TextStyle,
    color: androidx.compose.ui.graphics.Color,
    maxLines: Int
) {
    val now = tickerNow.value
    val remainingSeconds = ((reminderAt - now).coerceAtLeast(0L) + 999L) / 1000L
    val hours = remainingSeconds / 3600L
    val minutes = (remainingSeconds % 3600L) / 60L
    val seconds = remainingSeconds % 60L
    val countdown = "%02d:%02d:%02d".format(hours, minutes, seconds)
    val absoluteTime = formatReminderLabel(reminderAt).removePrefix("Hari ini ")
    Text(
        text = androidx.compose.ui.text.buildAnnotatedString {
            pushStyle(androidx.compose.ui.text.SpanStyle(fontFeatureSettings = "tnum"))
            append(countdown)
            pop()
            pushStyle(androidx.compose.ui.text.SpanStyle(color = color.copy(alpha = 0.78f)))
            append(" · $absoluteTime")
            pop()
        },
        style = style,
        color = color,
        maxLines = maxLines,
        softWrap = false,
        overflow = TextOverflow.Clip
    )
}

@Composable
private fun TodoItem(
    todo: TodoEntity,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReminderClick: () -> Unit,
    tickerNow: androidx.compose.runtime.State<Long>
) {
    val isCompleted = todo.isCompleted
    val textColor = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
    val containerColor = if (isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
    val reminderAt = todo.reminderAt?.takeIf { it > System.currentTimeMillis() }

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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = todo.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onEdit)
                            .padding(vertical = 8.dp),
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else null
                    )
                    Text(
                        text = "Dibuat ${formatNoteDate(todo.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (isCompleted) 0.5f else 1f
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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

            if (reminderAt != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.padding(start = 28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ReminderChip(reminderAt, tickerNow, onReminderClick)
                }
            }
        }
    }
}

@Composable
private fun ReminderChip(
    reminderAt: Long,
    tickerNow: androidx.compose.runtime.State<Long>,
    onClick: () -> Unit
) {
    val now = tickerNow.value
    if (reminderAt <= now) return
    val remainingMinutes = ((reminderAt - now + 59_999L) / 60_000L).coerceAtLeast(1L)
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = "Pengingat dalam $remainingMinutes menit, pukul ${timeFormatter.format(Instant.ofEpochMilli(reminderAt).atZone(zoneId))}"
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            ReminderCountdownText(
                reminderAt = reminderAt,
                tickerNow = tickerNow,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ReminderRow(
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.NotificationsActive,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(id = R.string.todo_reminder_label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun checkAndRequestNotificationPermission(
    context: Context,
    launcher: androidx.activity.result.ActivityResultLauncher<String>
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun AddTodoDialog(
    text: String,
    onTextChange: (String) -> Unit,
    reminderAt: Long?,
    onReminderClick: () -> Unit,
    onDismiss: () -> Unit,
    onAdd: (String, Long?) -> Unit,
    requestFocusOnShow: Boolean
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val reminderLabel = reminderAt?.let { formatReminderLabel(it) }
        ?: stringResource(id = R.string.todo_reminder_none)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Todo Baru", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                TextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Apa yang perlu dilakukan?") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
                    keyboardActions = KeyboardActions(onDone = {
                        if (text.isNotBlank()) onAdd(text.trim(), reminderAt) else onDismiss()
                    }),
                    singleLine = true,
                    maxLines = 1
                )
                ReminderRow(
                    label = reminderLabel,
                    onClick = onReminderClick
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (text.isNotBlank()) {
                    onAdd(text.trim(), reminderAt)
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
    LaunchedEffect(requestFocusOnShow) {
        if (requestFocusOnShow) {
            keyboardController?.show()
        }
    }
}

@Composable
private fun EditTodoDialog(
    text: String,
    onTextChange: (String) -> Unit,
    reminderAt: Long?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onReminderClick: () -> Unit,
    requestFocusOnShow: Boolean
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val reminderLabel = reminderAt?.takeIf { it > System.currentTimeMillis() }?.let {
        formatReminderLabel(it)
    } ?: stringResource(id = R.string.todo_reminder_none)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Todo", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                TextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Apa yang perlu dilakukan?") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
                    keyboardActions = KeyboardActions(onDone = {
                        if (text.isNotBlank()) onSave(text.trim()) else onDismiss()
                    }),
                    singleLine = true,
                    maxLines = 1
                )
                ReminderRow(
                    label = reminderLabel,
                    onClick = onReminderClick
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (text.isNotBlank()) {
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
    LaunchedEffect(requestFocusOnShow) {
        if (requestFocusOnShow) {
            keyboardController?.show()
        }
    }
}
