package com.example.notesapp.presentation.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Single source of truth for note accent colors per theme.
 * Database stores pastel colors (0xFFFFF9C4, etc.), but UI uses these accent colors.
 * Same accent used for: list strip, edit top bar, color picker.
 */
object NoteColorMapper {

    // Note colors stored in database (pastel) -> Accent colors per theme
    private val accentColors = mapOf(
        // Yellow
        0xFFFFF9C4.toInt() to ColorPair(
            light = Color(0xFFC0A000), // Amber 700
            dark = Color(0xFFFFD600)   // Amber A400
        ),
        // Green
        0xFFC8E6C9.toInt() to ColorPair(
            light = Color(0xFF2E7D32), // Green 800
            dark = Color(0xFF81C784)   // Green 300
        ),
        // Blue
        0xFFB3E5FC.toInt() to ColorPair(
            light = Color(0xFF0277BD), // Blue 800
            dark = Color(0xFF4FC3F7)   // Blue 300
        ),
        // Peach / Orange
        0xFFFFCCBC.toInt() to ColorPair(
            light = Color(0xFFD84315), // Deep Orange 700
            dark = Color(0xFFFF7043)   // Deep Orange 300
        ),
        // Purple
        0xFFE1BEE7.toInt() to ColorPair(
            light = Color(0xFF6A1B9A), // Deep Purple 700
            dark = Color(0xFFCE93D8)   // Purple 300
        ),
        // White / Neutral (Gray)
        0xFFFFFFFF.toInt() to ColorPair(
            light = Color(0xFF616161), // Gray 700
            dark = Color(0xFFBDBDBD)   // Gray 300
        )
    )

    data class ColorPair(val light: Color, val dark: Color)

    // Default note color (Yellow)
    const val DEFAULT_NOTE_COLOR = 0xFFFFF9C4.toInt()

    // All available note colors (database values)
    val allNoteColors = listOf(
        0xFFFFF9C4.toInt(), // Yellow
        0xFFC8E6C9.toInt(), // Green
        0xFFB3E5FC.toInt(), // Blue
        0xFFFFCCBC.toInt(), // Peach
        0xFFE1BEE7.toInt(), // Purple
        0xFFFFFFFF.toInt()  // White / Neutral
    )

    /** Get the accent color for a note color, matching current theme. */
    @Composable
    fun getNoteAccentColor(noteColor: Int): Color {
        val pair = accentColors[noteColor] ?: accentColors[DEFAULT_NOTE_COLOR]!!
        return if (isSystemInDarkTheme()) pair.dark else pair.light
    }

    /** Get accent color for specific theme (for dialog/preview). Non-composable version. */
    fun getNoteAccentColor(noteColor: Int, isDark: Boolean): Color {
        val pair = accentColors[noteColor] ?: accentColors[DEFAULT_NOTE_COLOR]!!
        return if (isDark) pair.dark else pair.light
    }

    /** Non-composable version for use in non-composable contexts. */
    fun getNoteAccentColorNonComposable(noteColor: Int, isDark: Boolean): Color {
        val pair = accentColors[noteColor] ?: accentColors[DEFAULT_NOTE_COLOR]!!
        return if (isDark) pair.dark else pair.light
    }

    /** Compute contrasting text color (black or white) for a given background color. */
    fun getContrastColor(background: Color): Color {
        // Relative luminance (sRGB)
        val luminance = 0.2126 * background.red + 0.7152 * background.green + 0.0722 * background.blue
        return if (luminance > 0.5f) Color.Black else Color.White
    }

    /** Get all color pairs for the color picker dialog. */
    @Composable
    fun getAllColorPairs(): List<ColorPickerItem> {
        val isDark = isSystemInDarkTheme()
        return allNoteColors.map { colorInt ->
            val accent = getNoteAccentColor(colorInt, isDark)
            ColorPickerItem(
                dbColor = colorInt,
                accentColor = accent,
                contrastColor = getContrastColor(accent)
            )
        }
    }
}

data class ColorPickerItem(
    val dbColor: Int,
    val accentColor: Color,
    val contrastColor: Color
)