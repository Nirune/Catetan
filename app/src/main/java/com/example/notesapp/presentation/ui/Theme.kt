package com.example.notesapp.presentation.ui

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat

enum class ThemeMode {
    LIGHT, DARK, AUTO
}

enum class ViewMode {
    LIST, GRID
}

object ThemePreferences {
    private const val PREFS_NAME = "catetan_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_THEME_NAME = "theme_name"
    private const val KEY_THEME_LEGACY = "theme"
    private const val KEY_EXPORT_PATH_URI = "export_path_uri"
    private const val KEY_VIEW_MODE = "view_mode"
    private const val KEY_TRASH_RETENTION_DAYS = "trash_retention_days"

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

    /**
     * Nama tema. Nilai lama "light"/"dark" dipetakan ke mode dengan tema [DEFAULT_THEME_NAME],
     * supaya pengguna lama nggak kehilangan pilihan. Nilai tidak dikenal jatuh ke default.
     */
    val themeName: String
        get() {
            val prefs = prefs ?: return DEFAULT_THEME_NAME
            val stored = prefs.getString(KEY_THEME_NAME, null) ?: run {
                when (prefs.getString(KEY_THEME_LEGACY, null)?.lowercase()) {
                    "light" -> {
                        setThemeMode(ThemeMode.LIGHT)
                        DEFAULT_THEME_NAME
                    }
                    "dark" -> {
                        setThemeMode(ThemeMode.DARK)
                        DEFAULT_THEME_NAME
                    }
                    else -> DEFAULT_THEME_NAME
                }
            }
            return themePalette(stored).name
        }

    fun setThemeName(name: String) {
        val resolved = themePalette(name).name
        prefs?.edit()?.putString(KEY_THEME_NAME, resolved)?.apply()
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

    val trashRetentionDays: Int
        get() = prefs?.getInt(KEY_TRASH_RETENTION_DAYS, 30) ?: 30

    fun setTrashRetentionDays(days: Int) {
        require(days in setOf(7, 14, 30, 60, 0))
        prefs?.edit()?.putInt(KEY_TRASH_RETENTION_DAYS, days)?.apply()
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
    themeName: String = DEFAULT_THEME_NAME,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    ThemePreferences.init(context)

    val effectiveDarkTheme = isEffectiveDarkTheme(themeMode)
    val colorScheme = colorSchemeFor(themeName, effectiveDarkTheme)

    // Warna ikon status bar & navigation bar mengikuti mode efektif aplikasi, bukan mode sistem.
    SideEffect {
        val window = (context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !effectiveDarkTheme
            isAppearanceLightNavigationBars = !effectiveDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

/**
 * Mengembalikan label mode (Sistem / Terang / Gelap) yang digunakan secara konsisten di layar
 * Tampilan dan subjudul baris "Tema & Mode" di dialog Pengaturan.
 */
fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.AUTO -> "Sistem"
    ThemeMode.LIGHT -> "Terang"
    ThemeMode.DARK -> "Gelap"
}
