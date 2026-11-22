package com.example.notesapp.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.notesapp.R
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val ITEM_HEIGHT_DP = 48
private const val VISIBLE_ITEMS = 5
private const val WHEEL_CYCLES = 400

private const val HOURS = 24
private const val MINUTES = 60
private const val SECONDS = 60

private data class WheelValue(val hour: Int, val minute: Int, val second: Int)

private val PRESETS = listOf(
    WheelValue(0, 15, 0),
    WheelValue(0, 30, 0),
    WheelValue(1, 0, 0)
)

private fun positiveMod(value: Int, modulus: Int): Int = ((value % modulus) + modulus) % modulus

private fun shortestDelta(target: Int, current: Int, modulus: Int): Int {
    val raw = positiveMod(target - current, modulus)
    return if (raw > modulus / 2) raw - modulus else raw
}

private fun initialWheelValue(existingReminderAt: Long?, now: Long): WheelValue {
    if (existingReminderAt == null) return WheelValue(0, 15, 0)
    val remainingSeconds = (existingReminderAt - now) / 1000L
    val clamped = remainingSeconds.coerceIn(1L, 23L * 3600L + 59L * 60L + 59L)
    val hour = (clamped / 3600L).toInt()
    val minute = ((clamped % 3600L) / 60L).toInt()
    val second = (clamped % 60L).toInt()
    return WheelValue(hour, minute, second)
}

private fun centerItemIndex(listState: LazyListState): Int {
    val layoutInfo = listState.layoutInfo
    val centerPx = layoutInfo.viewportStartOffset + layoutInfo.viewportSize.height / 2
    var bestIndex = listState.firstVisibleItemIndex
    var bestDistance = Int.MAX_VALUE
    for (item in layoutInfo.visibleItemsInfo) {
        val itemCenter = item.offset + item.size / 2
        val distance = abs(itemCenter - centerPx)
        if (distance < bestDistance) {
            bestDistance = distance
            bestIndex = item.index
        }
    }
    return bestIndex
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderPicker(
    targetText: String,
    existingReminderAt: Long?,
    onDismiss: () -> Unit,
    onSet: (Long) -> Unit,
    onClear: () -> Unit
) {
    val initialValue = remember(existingReminderAt) {
        initialWheelValue(existingReminderAt, System.currentTimeMillis())
    }

    var hour by rememberSaveable { mutableIntStateOf(initialValue.hour) }
    var minute by rememberSaveable { mutableIntStateOf(initialValue.minute) }
    var second by rememberSaveable { mutableIntStateOf(initialValue.second) }

    var presetEvent by remember { mutableIntStateOf(0) }
    var presetValue by remember { mutableStateOf(initialValue) }

    val durationMillis = hour * 3600_000L + minute * 60_000L + second * 1000L
    val canSet = durationMillis > 0L
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val compactHeight = LocalConfiguration.current.screenHeightDp < 600
    val sectionGap = if (compactHeight) 8.dp else 20.dp
    val wheelRows = if (compactHeight) 3 else VISIBLE_ITEMS

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = if (compactHeight) 8.dp else 16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.todo_reminder_pick_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = targetText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(sectionGap))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WheelColumn(
                    label = "Jam",
                    initialValue = initialValue.hour,
                    count = HOURS,
                    visibleRows = wheelRows,
                    presetEvent = presetEvent,
                    presetValue = presetValue.hour,
                    onValueSettled = { hour = it }
                )
                Text(
                    text = ":",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                WheelColumn(
                    label = "Menit",
                    initialValue = initialValue.minute,
                    count = MINUTES,
                    visibleRows = wheelRows,
                    presetEvent = presetEvent,
                    presetValue = presetValue.minute,
                    onValueSettled = { minute = it }
                )
                Text(
                    text = ":",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                WheelColumn(
                    label = "Detik",
                    initialValue = initialValue.second,
                    count = SECONDS,
                    visibleRows = wheelRows,
                    presetEvent = presetEvent,
                    presetValue = presetValue.second,
                    onValueSettled = { second = it }
                )
            }

            Spacer(modifier = Modifier.height(sectionGap))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PRESETS.forEach { preset ->
                    ReminderPresetChip(
                        label = formatPreset(preset),
                        active = hour == preset.hour && minute == preset.minute && second == preset.second,
                        onClick = {
                            presetValue = preset
                            presetEvent += 1
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(sectionGap))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(id = R.string.todo_reminder_btn_cancel),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (existingReminderAt != null) {
                    TextButton(onClick = onClear) {
                        Text(
                            text = stringResource(id = R.string.todo_reminder_btn_remove),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                FilledTonalButton(
                    onClick = { onSet(System.currentTimeMillis() + durationMillis) },
                    enabled = canSet
                ) {
                    Text(stringResource(id = R.string.todo_reminder_btn_set))
                }
            }

            Spacer(modifier = Modifier.height(if (compactHeight) 4.dp else 16.dp))
        }
    }
}

private fun formatPreset(preset: WheelValue): String =
    "%02d:%02d:%02d".format(preset.hour, preset.minute, preset.second)

@Composable
private fun WheelColumn(
    label: String,
    initialValue: Int,
    count: Int,
    visibleRows: Int,
    presetEvent: Int,
    presetValue: Int,
    onValueSettled: (Int) -> Unit
) {
    val initialIndex = remember {
        count * (WHEEL_CYCLES / 2) + positiveMod(initialValue, count)
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(listState)
    val totalItems = count * WHEEL_CYCLES

    val centerIndex by remember(listState) {
        derivedStateOf { centerItemIndex(listState) }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { scrolling ->
                if (!scrolling) {
                    kotlinx.coroutines.delay(80)
                    if (!listState.isScrollInProgress) {
                        onValueSettled(positiveMod(centerItemIndex(listState), count))
                    }
                }
            }
    }

        LaunchedEffect(presetEvent) {
        if (presetEvent == 0) return@LaunchedEffect
        if (listState.isScrollInProgress) {
            return@LaunchedEffect
        }
        val current = listState.firstVisibleItemIndex
        val delta = shortestDelta(presetValue, positiveMod(current, count), count)
        val target = (current + delta).coerceIn(0, totalItems - 1)
        listState.animateScrollToItem(target)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .height((visibleRows * ITEM_HEIGHT_DP).dp)
                .width(72.dp)
        ) {
            LazyColumn(
                state = listState,
                flingBehavior = flingBehavior,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = (ITEM_HEIGHT_DP * 2).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(totalItems) { index ->
                    WheelItem(
                        value = positiveMod(index, count),
                        isSelected = positiveMod(index, count) == positiveMod(centerIndex, count)
                    )
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun WheelItem(
    value: Int,
    isSelected: Boolean
) {
    Box(
        modifier = Modifier
            .height(ITEM_HEIGHT_DP.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "%02d".format(value),
            style = if (isSelected) {
                MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            } else {
                MaterialTheme.typography.titleMedium
            },
            color = if (isSelected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun ReminderPresetChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = if (active) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .background(
                color = if (active) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}
