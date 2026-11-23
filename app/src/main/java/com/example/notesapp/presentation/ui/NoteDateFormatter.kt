package com.example.notesapp.presentation.ui

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatNoteDate(timestamp: Long): String {
    val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
    val now = LocalDateTime.now()
    val pattern = if (date.year == now.year) "d MMM, HH:mm" else "d MMM yyyy, HH:mm"
    return date.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
}
