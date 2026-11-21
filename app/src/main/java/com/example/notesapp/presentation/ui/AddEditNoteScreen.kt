package com.example.notesapp.presentation.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.notesapp.R
import com.example.notesapp.data.model.NoteEntity
import com.example.notesapp.presentation.viewmodel.NoteViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Material "outlined palette" icon (official SVG path) built as a lightweight ImageVector,
// so we don't have to pull the 30MB+ material-icons-extended artifact.
// Sumber: https://raw.githubusercontent.com/google/material-design-icons/master/src/image/palette/materialiconsoutlined/24px.svg
private const val PALETTE_PATH =
    "M12,22C6.49,22,2,17.51,2,12S6.49,2,12,2s10,4.04,10,9c0,3.31-2.69,6-6,6h-1.77" +
        "c-0.28,0-0.5,0.22-0.5,0.5c0,0.12,0.05,0.23,0.13,0.33c0.41,0.47,0.64,1.06,0.64,1.67" +
        "C14.5,20.88,13.38,22,12,22z" +
        "M12,4c-4.41,0-8,3.59-8,8s3.59,8,8,8c0.28,0,0.5-0.22,0.5-0.5c0-0.16-0.08-0.28-0.14-0.35" +
        "c-0.41-0.46-0.63-1.05-0.63-1.65c0-1.38,1.12-2.5,2.5-2.5H16c2.21,0,4-1.79,4-4" +
        "C20,7.14,16.41,4,12,4z" +
        "M5,11.5a1.5,1.5 0 1,0 3,0a1.5,1.5 0 1,0 -3,0z" +
        "M8,7.5a1.5,1.5 0 1,0 3,0a1.5,1.5 0 1,0 -3,0z" +
        "M13,7.5a1.5,1.5 0 1,0 3,0a1.5,1.5 0 1,0 -3,0z" +
        "M16,11.5a1.5,1.5 0 1,0 3,0a1.5,1.5 0 1,0 -3,0z"

private val PaletteIcon: ImageVector = ImageVector.Builder(
    name = "Outlined.Palette",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).addPath(
    pathData = addPathNodes(PALETTE_PATH),
    fill = SolidColor(Color.Black)
).build()

// Fixed line height for the ruled-paper body (text layout). Nilai yang sama sebagai Dp dipakai untuk padding/layout.
private val NoteLineHeight = 28.sp
private val NoteLineHeightDp = 28.dp

private data class UrlRange(val start: Int, val end: Int, val url: String)

private data class LinkifiedText(val text: AnnotatedString, val urls: List<UrlRange>)

// http(s):// and www. URLs. Trailing punctuation is trimmed after matching.
private val UrlRegex = Regex("""(?i)\b(?:https?://|www\.)[^\s<>"]+""")
private val UrlTrailingPunctuation = charArrayOf('.', ',', ';', ':', '!', '?', ')', ']', '}', '\'', '"')

/**
 * Builds the read-mode AnnotatedString: URLs get a blue underlined style, and their
 * character ranges are kept so a single tap can be resolved back to a specific URL.
 */
private fun linkifyContent(raw: String, linkColor: Color): LinkifiedText {
    val builder = AnnotatedString.Builder(raw)
    val urls = mutableListOf<UrlRange>()
    var match = UrlRegex.find(raw)
    while (match != null) {
        val url = match.value.trimEnd(*UrlTrailingPunctuation)
        val minimumLength = when {
            url.startsWith("https://", ignoreCase = true) -> 8
            url.startsWith("http://", ignoreCase = true) -> 7
            else -> 4 // www.
        }
        if (url.length > minimumLength) {
            val start = match.range.first
            val end = start + url.length
            builder.addStyle(
                SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                start,
                end
            )
            urls += UrlRange(start, end, url)
        }
        match = UrlRegex.find(raw, match.range.last + 1)
    }
    return LinkifiedText(builder.toAnnotatedString(), urls)
}

/**
 * Resolves a tap position on the read-mode text to a URL range, taking the text's
 * left padding into account. Returns null for taps on plain text or empty paper.
 */
private fun linkUrlAt(
    position: Offset,
    layout: TextLayoutResult,
    urls: List<UrlRange>,
    leftPaddingPx: Float
): String? {
    val local = Offset(position.x - leftPaddingPx, position.y)
    if (local.x < 0f || local.y < 0f) return null
    if (local.x > layout.size.width || local.y > layout.size.height) return null
    val offset = layout.getOffsetForPosition(local)
    return urls.firstOrNull { offset >= it.start && offset < it.end }?.url
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun AddEditNoteScreen(
    viewModel: NoteViewModel,
    noteToEdit: NoteEntity? = null,
    onBack: () -> Unit
) {
    // currentNote melacak catatan yang sedang dilihat/diedit. Untuk catatan baru, di-set setelah
    // insert pertama (mengambil id dari database) supaya simpan selanjutnya update, bukan insert baru.
    var currentNote by remember { mutableStateOf(noteToEdit) }
    var title by remember { mutableStateOf(noteToEdit?.title ?: "") }
    var contentValue by remember { mutableStateOf(TextFieldValue(noteToEdit?.content ?: "")) }
    var selectedColor by remember { mutableStateOf(noteToEdit?.color ?: NoteColorMapper.DEFAULT_NOTE_COLOR) }
    var showColorPicker by remember { mutableStateOf(false) }
    // Existing notes open in read mode; brand-new notes open directly in edit mode.
    var isEditing by remember { mutableStateOf(noteToEdit == null) }
    var isSaving by remember { mutableStateOf(false) }

    // Measured window coordinates so the popup can open exactly 4dp below the top bar,
    // right edge aligned with the color button.
    var topBarBottomPx by remember { mutableStateOf(0f) }
    var colorButtonTopPx by remember { mutableStateOf(0f) }

    val activity = LocalContext.current as ComponentActivity
    val isDarkTheme = isSystemInDarkTheme()
    val density = LocalDensity.current
    val focusRequester = remember { FocusRequester() }
    val titleFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Undo/redo history for the note body (content text only, not the title).
    val undoStack = remember { mutableStateListOf<TextFieldValue>() }
    val redoStack = remember { mutableStateListOf<TextFieldValue>() }
    var lastSnapshotAt by remember { mutableStateOf(0L) }

    fun pushUndo(value: TextFieldValue) {
        undoStack.add(value)
        if (undoStack.size > 100) undoStack.removeAt(0)
    }

    fun pushRedo(value: TextFieldValue) {
        redoStack.add(value)
        if (redoStack.size > 100) redoStack.removeAt(0)
    }

    fun clearHistory() {
        undoStack.clear()
        redoStack.clear()
        lastSnapshotAt = 0L
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        pushRedo(contentValue)
        contentValue = undoStack.removeAt(undoStack.lastIndex)
        lastSnapshotAt = System.currentTimeMillis()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        pushUndo(contentValue)
        contentValue = redoStack.removeAt(redoStack.lastIndex)
        lastSnapshotAt = System.currentTimeMillis()
    }

    fun enterEditMode() {
        contentValue = contentValue.copy(selection = TextRange(contentValue.text.length))
        isEditing = true
    }

    fun exitEditMode() {
        isEditing = false
        keyboardController?.hide()
        focusManager.clearFocus()
        clearHistory()
    }

    fun saveNote() {
        if (title.isBlank() && contentValue.text.isBlank()) return
        if (isSaving) return
        val note = currentNote
        if (note == null) {
            // Catatan baru: insert, dapatkan id yang dikembalikan, lalu beralih ke mode baca
            // catatan yang sama (hindari duplikat di saat simpan berikutnya).
            isSaving = true
            isEditing = false
            viewModel.addNoteAndReturn(title, contentValue.text, selectedColor) { saved ->
                currentNote = saved
                isSaving = false
            }
        } else {
            viewModel.updateNote(
                note.copy(
                    title = title,
                    content = contentValue.text,
                    color = selectedColor
                )
            )
            exitEditMode()
        }
    }

    // Entering edit mode: put the cursor at the end and bring up the keyboard.
    LaunchedEffect(isEditing) {
        if (isEditing) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    // Read-mode link styling: one AnnotatedString with blue underlined URLs.
    val linkColor = if (isDarkTheme) Color(0xFF64B5F6) else Color(0xFF1565C0)
    val linkifiedContent = remember(contentValue.text, linkColor) {
        linkifyContent(contentValue.text, linkColor)
    }

    fun openUrl(url: String) {
        val normalized = if (url.startsWith("www.", ignoreCase = true)) "https://$url" else url
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalized)))
        } catch (e: ActivityNotFoundException) {
            scope.launch {
                snackbarHostState.showSnackbar("Tidak ada aplikasi untuk membuka link")
            }
        }
    }

    // Current accent color (animates on change)
    val currentAccent by animateColorAsState(
        targetValue = NoteColorMapper.getNoteAccentColor(selectedColor),
        label = "noteAccent"
    )

    // Contrasting text/icon color for top bar
    val topBarContentColor = NoteColorMapper.getContrastColor(currentAccent)

    // Set status bar icon color to match top bar (API 23+)
    val window = activity.window
    DisposableEffect(currentAccent) {
        val decorView = window.decorView
        val isLightIcons = topBarContentColor == Color.Black
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var flags = decorView.systemUiVisibility
            if (isLightIcons) {
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
            decorView.systemUiVisibility = flags
        }
        // Also set status bar color to match top bar for edge-to-edge
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = currentAccent.value.toInt()
        }
        onDispose {
            // Reset to theme default when leaving
            var flags = decorView.systemUiVisibility
            if (isDarkTheme) {
                flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            } else {
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
            decorView.systemUiVisibility = flags
        }
    }

    val bodyTextStyle = MaterialTheme.typography.bodyLarge.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 16.sp,
        lineHeight = NoteLineHeight
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (isEditing) {
                        NoteTitleField(
                            value = title,
                            onValueChange = { title = it },
                            enabled = true,
                            placeholder = "Judul",
                            textColor = MaterialTheme.colorScheme.onSurface,
                            surfaceColor = MaterialTheme.colorScheme.surface,
                            focusRequester = titleFocusRequester,
                        )
                    } else {
                        Text(
                            text = title.ifBlank { "(tanpa judul)" },
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = topBarContentColor
                        )
                    }
                },
                actions = {
                    if (isEditing) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clickable { showColorPicker = true }
                                .onGloballyPositioned {
                                    colorButtonTopPx = it.boundsInWindow().top
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(currentAccent)
                                    .border(1.5.dp, topBarContentColor, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = PaletteIcon,
                                    contentDescription = "Pilih warna",
                                    modifier = Modifier.size(18.dp),
                                    tint = topBarContentColor
                                )
                            }
                        }

                        if (showColorPicker) {
                            val offsetY = with(density) {
                                (topBarBottomPx - colorButtonTopPx + 4.dp.toPx()).roundToInt()
                            }
                            Popup(
                                alignment = Alignment.TopEnd,
                                offset = IntOffset(0, offsetY),
                                onDismissRequest = { showColorPicker = false },
                                properties = PopupProperties(focusable = true)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 3.dp,
                                    shadowElevation = 6.dp
                                ) {
                                    NoteColorPopup(
                                        currentColor = selectedColor,
                                        onColorSelected = { color ->
                                            selectedColor = color
                                            showColorPicker = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clickable { saveNote() },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Simpan",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    } else {
                        IconButton(onClick = { enterEditMode() }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_edit),
                                contentDescription = "Edit catatan",
                                tint = topBarContentColor
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = currentAccent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = topBarContentColor,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(paddingValues)
                .imePadding()
                .navigationBarsPadding()
        ) {
            // Ruled-paper body: content scrolls together with the lines.
            PaperBody(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                gesturesEnabled = !isEditing,
                onTap = { position, layout ->
                    linkUrlAt(
                        position = position,
                        layout = layout,
                        urls = linkifiedContent.urls,
                        leftPaddingPx = with(density) { 16.dp.toPx() }
                    )?.let { url -> openUrl(url) }
                },
                onDoubleTap = { enterEditMode() }
            ) { onTextLayout ->
                if (isEditing) {
                    BasicTextField(
                        value = contentValue,
                        onValueChange = { newValue ->
                            val oldValue = contentValue
                            if (newValue.text != oldValue.text) {
                                val now = System.currentTimeMillis()
                                val commonPrefix =
                                    oldValue.text.commonPrefixWith(newValue.text).length
                                val insertedChar = if (newValue.text.length > oldValue.text.length) {
                                    newValue.text.getOrNull(commonPrefix)
                                } else {
                                    null
                                }
                                // Snapshot before a new burst: after a ~500ms pause or on a
                                // space/newline. Continuous typing stays one history entry.
                                if (now - lastSnapshotAt > 500L ||
                                    insertedChar == ' ' ||
                                    insertedChar == '\n'
                                ) {
                                    pushUndo(oldValue)
                                    lastSnapshotAt = now
                                }
                                // Typing after an undo discards the redo history.
                                redoStack.clear()
                            }
                            contentValue = newValue
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(PaddingValues(top = NoteLineHeightDp + 13.dp, start = 16.dp, end = 16.dp, bottom = 0.dp))
                            .focusRequester(focusRequester),
                        textStyle = bodyTextStyle,
                        cursorBrush = SolidColor(currentAccent),
                        onTextLayout = onTextLayout,
                        decorationBox = { innerTextField ->
                            Box {
                                if (contentValue.text.isEmpty()) {
                                    Text(
                                        text = "Tulis catatanmu di sini...",
                                        style = bodyTextStyle.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                } else {
                    // Read mode: plain text, double-tap anywhere on the paper enters edit mode.
                    Text(
                        text = linkifiedContent.text,
                        style = bodyTextStyle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(PaddingValues(top = NoteLineHeightDp, start = 16.dp, end = 16.dp, bottom = 0.dp)),
                        onTextLayout = onTextLayout
                    )
                }
            }

            if (isEditing) {
                UndoRedoBar(
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = redoStack.isNotEmpty(),
                    onUndo = { undo() },
                    onRedo = { redo() }
                )
            }
        }
    }
}

/**
 * Title field shown in the top bar (shared between read & edit mode so position never jumps).
 * Single line, 40dp tall, 8dp rounded corners, surface background, onSurface text.
 */
@Composable
private fun NoteTitleField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    placeholder: String,
    textColor: Color,
    surfaceColor: Color,
    focusRequester: FocusRequester,
) {
    val titleTextStyle = MaterialTheme.typography.bodyLarge.copy(
        color = textColor,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
    )

    BasicTextField(
        value = value,
        onValueChange = { onValueChange(it) },
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(surfaceColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp)
            .focusRequester(focusRequester)
            .clickable(enabled = enabled) { focusRequester.requestFocus() },
        textStyle = titleTextStyle,
        singleLine = true,
        maxLines = 1,
        enabled = enabled,
        cursorBrush = SolidColor(textColor),
        decorationBox = { innerTextField ->
            Box(
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = titleTextStyle.copy(
                            color = textColor.copy(alpha = 0.5f)
                        )
                    )
                }
                innerTextField()
            }
        }
    )
}

/**
 * Bottom action bar with Undo and Redo buttons, pinned right above keyboard or nav bar.
 */
@Composable
private fun UndoRedoBar(
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    val activeTint = MaterialTheme.colorScheme.onSurface
    val inactiveTint = activeTint.copy(alpha = 0.38f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Divider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_undo),
                    contentDescription = "Undo",
                    modifier = Modifier.size(28.dp),
                    tint = if (canUndo) activeTint else inactiveTint
                )
            }
            Spacer(Modifier.width(16.dp))
            IconButton(
                onClick = onRedo,
                enabled = canRedo,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_redo),
                    contentDescription = "Redo",
                    modifier = Modifier.size(28.dp),
                    tint = if (canRedo) activeTint else inactiveTint
                )
            }
        }
    }
}

/**
 * Scrollable ruled-paper surface.
 *
 * The horizontal lines are drawn with drawBehind inside the scrolled content, so they
 * scroll with the text and always fill at least the whole viewport height. Lines start
 * from the top of the paper area and are spaced by [NoteLineHeight], matching the text
 * line height so baselines stay on the ruled lines.
 */
@Composable
private fun PaperBody(
    modifier: Modifier = Modifier,
    gesturesEnabled: Boolean = false,
    onTap: ((Offset, TextLayoutResult) -> Unit)? = null,
    onDoubleTap: (() -> Unit)? = null,
    content: @Composable (onTextLayout: (TextLayoutResult) -> Unit) -> Unit
) {
    val density = LocalDensity.current
    val lineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val spacingPx = with(density) { NoteLineHeight.toPx() }
    val strokePx = with(density) { 1.dp.toPx() }
    val textLayoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTap)

    // One gesture detector for the whole body: taps are resolved against the text layout,
    // while a double-tap anywhere (text or empty paper) switches to edit mode.
    val gestureModifier = if (gesturesEnabled) {
        Modifier.pointerInput(Unit) {
            detectTapGestures(
                onTap = { position ->
                    textLayoutResult.value?.let { layout ->
                        currentOnTap?.invoke(position, layout)
                    }
                },
                onDoubleTap = { currentOnDoubleTap?.invoke() }
            )
        }
    } else {
        Modifier
    }

    BoxWithConstraints(modifier) {
        val viewportHeight = maxHeight
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = viewportHeight)
                    .drawBehind {
                        var y = spacingPx
                        while (y <= size.height) {
                            drawLine(
                                color = lineColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = strokePx
                            )
                            y += spacingPx
                        }
                    }
                    .then(gestureModifier)
            ) {
                content { result -> textLayoutResult.value = result }
            }
        }
    }
}

/**
 * Compact color picker content. 6 swatches in a single row (3x2 on narrow screens).
 * Padding: 8dp vertical, 12dp horizontal -> popup height = swatch row + 16dp.
 */
@Composable
private fun NoteColorPopup(
    currentColor: Int,
    onColorSelected: (Int) -> Unit
) {
    val colorItems = NoteColorMapper.getAllColorPairs()
    val isNarrow = LocalConfiguration.current.screenWidthDp < 340
    val rows = if (isNarrow) colorItems.chunked(3) else listOf(colorItems)

    Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                rowItems.forEach { item ->
                    val isSelected = currentColor == item.dbColor
                    // 48dp touch target, 40dp visible box -> 8dp visual gap between swatches
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { onColorSelected(item.dbColor) },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(item.accentColor)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Warna terpilih",
                                    modifier = Modifier.size(18.dp),
                                    tint = item.contrastColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
