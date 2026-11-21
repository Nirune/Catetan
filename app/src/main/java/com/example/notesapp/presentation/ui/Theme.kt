package com.example.notesapp.presentation.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val BrandPurple = Color(0xFF6750A4)
private val BrandPurpleLight = Color(0xFFEADDFF)
private val BrandPurpleDark = Color(0xFF4A3860)

private val LightColorScheme = lightColorScheme(
    primary = BrandPurple,
    onPrimary = Color.White,
    primaryContainer = BrandPurpleLight,
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454E),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC7D0)
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandPurpleLight,
    onPrimary = BrandPurpleDark,
    primaryContainer = BrandPurple,
    onPrimaryContainer = BrandPurpleLight,
    secondary = Color(0xFFCCC7F0),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFFFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E1E6),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E1E6),
    surfaceVariant = Color(0xFF49454E),
    onSurfaceVariant = Color(0xFFCAC7D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454E)
)

enum class ThemeMode {
    LIGHT, DARK, AUTO
}

enum class ViewMode {
    LIST, GRID
}

object ThemePreferences {
    private const val PREFS_NAME = "catetan_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_EXPORT_PATH_URI = "export_path_uri"
    private const val KEY_VIEW_MODE = "view_mode"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    val themeMode: ThemeMode
        get() {
            val prefs = prefs ?: return ThemeMode.AUTO
            return when (prefs.getInt(KEY_THEME_MODE, 2)) {
                0 -> ThemeMode.LIGHT
                1 -> ThemeMode.DARK
                else -> ThemeMode.AUTO
            }
        }

    fun setThemeMode(mode: ThemeMode) {
        val value = when (mode) {
            ThemeMode.LIGHT -> 0
            ThemeMode.DARK -> 1
            ThemeMode.AUTO -> 2
        }
        prefs?.edit()?.putInt(KEY_THEME_MODE, value)?.apply()
    }

    val exportPathUri: String?
        get() = prefs?.getString(KEY_EXPORT_PATH_URI, null)

    fun setExportPathUri(uri: String?) {
        prefs?.edit()?.putString(KEY_EXPORT_PATH_URI, uri)?.apply()
    }

    val viewMode: ViewMode
        get() {
            val prefs = prefs ?: return ViewMode.LIST
            return when (prefs.getInt(KEY_VIEW_MODE, 0)) {
                1 -> ViewMode.GRID
                else -> ViewMode.LIST
            }
        }

    fun setViewMode(mode: ViewMode) {
        val value = when (mode) {
            ViewMode.LIST -> 0
            ViewMode.GRID -> 1
        }
        prefs?.edit()?.putInt(KEY_VIEW_MODE, value)?.apply()
    }
}

/**
 * Mengembalikan apakah tema gelap efektif, berdasarkan [themeMode]:
 * - LIGHT: selalu terang
 * - DARK: selalu gelap
 * - AUTO: ikuti sistem
 */
@Composable
fun isEffectiveDarkTheme(themeMode: ThemeMode): Boolean = when (themeMode) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.AUTO -> isSystemInDarkTheme()
}

@Composable
fun AppTheme(
    themeMode: ThemeMode = ThemeMode.AUTO,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    ThemePreferences.init(context)

    val effectiveDarkTheme = isEffectiveDarkTheme(themeMode)
    val colorScheme = if (effectiveDarkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
