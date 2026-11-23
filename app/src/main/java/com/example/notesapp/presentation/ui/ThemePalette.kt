package com.example.notesapp.presentation.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Definisi temaCatetan. Satu tempat untuk semua skema warna.
 * Urutan daftar [themePalettes] adalah urutan yang tampil di layar Tampilan.
 */
data class ThemePalette(
    val name: String,
    val light: ColorScheme,
    val dark: ColorScheme
)

private fun light(
    primary: Color, onPrimary: Color,
    primaryContainer: Color, onPrimaryContainer: Color,
    secondary: Color, onSecondary: Color,
    secondaryContainer: Color, onSecondaryContainer: Color,
    tertiary: Color, onTertiary: Color,
    background: Color, onBackground: Color,
    surface: Color, onSurface: Color,
    surfaceVariant: Color, onSurfaceVariant: Color,
    outline: Color, outlineVariant: Color,
    error: Color, onError: Color,
    errorContainer: Color, onErrorContainer: Color,
    inverseSurface: Color, inverseOnSurface: Color,
    inversePrimary: Color, surfaceTint: Color, scrim: Color
): ColorScheme = lightColorScheme(
    primary = primary, onPrimary = onPrimary,
    primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
    secondary = secondary, onSecondary = onSecondary,
    secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary, onTertiary = onTertiary,
    background = background, onBackground = onBackground,
    surface = surface, onSurface = onSurface,
    surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
    outline = outline, outlineVariant = outlineVariant,
    error = error, onError = onError,
    errorContainer = errorContainer, onErrorContainer = onErrorContainer,
    inverseSurface = inverseSurface, inverseOnSurface = inverseOnSurface,
    inversePrimary = inversePrimary, surfaceTint = surfaceTint, scrim = scrim
)

private fun dark(
    primary: Color, onPrimary: Color,
    primaryContainer: Color, onPrimaryContainer: Color,
    secondary: Color, onSecondary: Color,
    secondaryContainer: Color, onSecondaryContainer: Color,
    tertiary: Color, onTertiary: Color,
    background: Color, onBackground: Color,
    surface: Color, onSurface: Color,
    surfaceVariant: Color, onSurfaceVariant: Color,
    outline: Color, outlineVariant: Color,
    error: Color, onError: Color,
    errorContainer: Color, onErrorContainer: Color,
    inverseSurface: Color, inverseOnSurface: Color,
    inversePrimary: Color, surfaceTint: Color, scrim: Color
): ColorScheme = darkColorScheme(
    primary = primary, onPrimary = onPrimary,
    primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
    secondary = secondary, onSecondary = onSecondary,
    secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary, onTertiary = onTertiary,
    background = background, onBackground = onBackground,
    surface = surface, onSurface = onSurface,
    surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
    outline = outline, outlineVariant = outlineVariant,
    error = error, onError = onError,
    errorContainer = errorContainer, onErrorContainer = onErrorContainer,
    inverseSurface = inverseSurface, inverseOnSurface = inverseOnSurface,
    inversePrimary = inversePrimary, surfaceTint = surfaceTint, scrim = scrim
)

private val CatetanLight = light(
    primary = Color(0xFF6750A4), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF), onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DEF8), onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260), onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFBFE), onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE), onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC), onSurfaceVariant = Color(0xFF49454E),
    outline = Color(0xFF49454E), outlineVariant = Color(0xFFC4BEC8),
    error = Color(0xFFB3261E), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC), onErrorContainer = Color(0xFF410E0B),
    inverseSurface = Color(0xFF1C1B1F), inverseOnSurface = Color(0xFFFFFBFE),
    inversePrimary = Color(0xFFEADDFF), surfaceTint = Color(0xFF6750A4), scrim = Color(0xFF000000)
)

private val CatetanDark = dark(
    primary = Color(0xFFEADDFF), onPrimary = Color(0xFF4A3860),
    primaryContainer = Color(0xFF6750A4), onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC7F0), onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458), onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFFFB8C8), onTertiary = Color(0xFF492532),
    background = Color(0xFF141218), onBackground = Color(0xFFE6E1E6),
    surface = Color(0xFF141218), onSurface = Color(0xFFE6E1E6),
    surfaceVariant = Color(0xFF49454E), onSurfaceVariant = Color(0xFFCAC7D0),
    outline = Color(0xFFCAC7D0), outlineVariant = Color(0xFF646068),
    error = Color(0xFFF2B8B5), onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18), onErrorContainer = Color(0xFFF9DEDC),
    inverseSurface = Color(0xFFE6E1E6), inverseOnSurface = Color(0xFF141218),
    inversePrimary = Color(0xFF6750A4), surfaceTint = Color(0xFFEADDFF), scrim = Color(0xFF000000)
)

// Catppuccin light
private val CatppuccinLight = light(
    primary = Color(0xFF8839EF),
    onPrimary = Color(0xFFEFF1F5),
    primaryContainer = Color(0xFFEDE1FD),
    onPrimaryContainer = Color(0xFF4C4F69),
    secondary = Color(0xFF7287FD),
    onSecondary = Color(0xFFEFF1F5),
    secondaryContainer = Color(0xFFEAEDFF),
    onSecondaryContainer = Color(0xFF4C4F69),
    tertiary = Color(0xFF7287FD),
    onTertiary = Color(0xFFEFF1F5),
    background = Color(0xFFEFF1F5),
    onBackground = Color(0xFF4C4F69),
    surface = Color(0xFFEFF1F5),
    onSurface = Color(0xFF4C4F69),
    surfaceVariant = Color(0xFFCCD0DA),
    onSurfaceVariant = Color(0xFF565966),
    outline = Color(0xFF565966),
    outlineVariant = Color(0xFFADB0B9),
    error = Color(0xFFD20F39),
    onError = Color(0xFFEFF1F5),
    errorContainer = Color(0xFFF8DBE1),
    onErrorContainer = Color(0xFF4C4F69),
    inverseSurface = Color(0xFF4C4F69),
    inverseOnSurface = Color(0xFFEFF1F5),
    inversePrimary = Color(0xFFEDE1FD),
    surfaceTint = Color(0xFF8839EF),
    scrim = Color(0xFF000000)
)

// Catppuccin dark
private val CatppuccinDark = dark(
    primary = Color(0xFFCBA6F7),
    onPrimary = Color(0xFF1E1E2E),
    primaryContainer = Color(0xFFCDCCF5),
    onPrimaryContainer = Color(0xFF1E1E2E),
    secondary = Color(0xFFB4BEFE),
    onSecondary = Color(0xFF1E1E2E),
    secondaryContainer = Color(0xFFC8D1F6),
    onSecondaryContainer = Color(0xFF1E1E2E),
    tertiary = Color(0xFFB4BEFE),
    onTertiary = Color(0xFF1E1E2E),
    background = Color(0xFF1E1E2E),
    onBackground = Color(0xFFCDD6F4),
    surface = Color(0xFF1E1E2E),
    onSurface = Color(0xFFCDD6F4),
    surfaceVariant = Color(0xFF313244),
    onSurfaceVariant = Color(0xFFA6ADC8),
    outline = Color(0xFFA6ADC8),
    outlineVariant = Color(0xFF4F5060),
    error = Color(0xFFF38BA8),
    onError = Color(0xFF1E1E2E),
    errorContainer = Color(0xFFD5C7E5),
    onErrorContainer = Color(0xFF1E1E2E),
    inverseSurface = Color(0xFFCDD6F4),
    inverseOnSurface = Color(0xFF1E1E2E),
    inversePrimary = Color(0xFFCDCCF5),
    surfaceTint = Color(0xFFCBA6F7),
    scrim = Color(0xFF000000)
)

// Tokyo Night light
private val TokyoNightLight = light(
    primary = Color(0xFF2463B8),
    onPrimary = Color(0xFFE1E2E7),
    primaryContainer = Color(0xFFDEE8F4),
    onPrimaryContainer = Color(0xFF3760BF),
    secondary = Color(0xFF9854F1),
    onSecondary = Color(0xFFE1E2E7),
    secondaryContainer = Color(0xFFF0E5FD),
    onSecondaryContainer = Color(0xFF3760BF),
    tertiary = Color(0xFF9854F1),
    onTertiary = Color(0xFFE1E2E7),
    background = Color(0xFFE1E2E7),
    onBackground = Color(0xFF3760BF),
    surface = Color(0xFFE1E2E7),
    onSurface = Color(0xFF3359B2),
    surfaceVariant = Color(0xFFC4C8DA),
    onSurfaceVariant = Color(0xFF2E4E8F),
    outline = Color(0xFF2E4E8F),
    outlineVariant = Color(0xFFA6AAB9),
    error = Color(0xFFF52A65),
    onError = Color(0xFFE1E2E7),
    errorContainer = Color(0xFFFEDFE8),
    onErrorContainer = Color(0xFF3760BF),
    inverseSurface = Color(0xFF3760BF),
    inverseOnSurface = Color(0xFFE1E2E7),
    inversePrimary = Color(0xFFDEE8F4),
    surfaceTint = Color(0xFF2463B8),
    scrim = Color(0xFF000000)
)

// Tokyo Night dark
private val TokyoNightDark = dark(
    primary = Color(0xFF7AA2F7),
    onPrimary = Color(0xFF1A1B26),
    primaryContainer = Color(0xFFB2C2F5),
    onPrimaryContainer = Color(0xFF1A1B26),
    secondary = Color(0xFFBB9AF7),
    onSecondary = Color(0xFF1A1B26),
    secondaryContainer = Color(0xFFBFC0F5),
    onSecondaryContainer = Color(0xFF1A1B26),
    tertiary = Color(0xFFBB9AF7),
    onTertiary = Color(0xFF1A1B26),
    background = Color(0xFF1A1B26),
    onBackground = Color(0xFFC0CAF5),
    surface = Color(0xFF1A1B26),
    onSurface = Color(0xFFC0CAF5),
    surfaceVariant = Color(0xFF292E42),
    onSurfaceVariant = Color(0xFF9AA2C4),
    outline = Color(0xFF9AA2C4),
    outlineVariant = Color(0xFF494D5E),
    error = Color(0xFFF7768E),
    onError = Color(0xFF1A1B26),
    errorContainer = Color(0xFFCBB9E0),
    onErrorContainer = Color(0xFF1A1B26),
    inverseSurface = Color(0xFFC0CAF5),
    inverseOnSurface = Color(0xFF1A1B26),
    inversePrimary = Color(0xFFB2C2F5),
    surfaceTint = Color(0xFF7AA2F7),
    scrim = Color(0xFF000000)
)

// Rosé Pine light
private val RoséPineLight = light(
    primary = Color(0xFF7A6890),
    onPrimary = Color(0xFFFAF4ED),
    primaryContainer = Color(0xFFEBE8EE),
    onPrimaryContainer = Color(0xFF575279),
    secondary = Color(0xFFD7827E),
    onSecondary = Color(0xFFFAF4ED),
    secondaryContainer = Color(0xFFF9ECEC),
    onSecondaryContainer = Color(0xFF575279),
    tertiary = Color(0xFFD7827E),
    onTertiary = Color(0xFFFAF4ED),
    background = Color(0xFFFAF4ED),
    onBackground = Color(0xFF575279),
    surface = Color(0xFFFAF4ED),
    onSurface = Color(0xFF575279),
    surfaceVariant = Color(0xFFF2E9E1),
    onSurfaceVariant = Color(0xFF6A6586),
    outline = Color(0xFF6A6586),
    outlineVariant = Color(0xFFCDC6BF),
    error = Color(0xFFB4637A),
    onError = Color(0xFFFAF4ED),
    errorContainer = Color(0xFFF4E8EB),
    onErrorContainer = Color(0xFF575279),
    inverseSurface = Color(0xFF575279),
    inverseOnSurface = Color(0xFFFAF4ED),
    inversePrimary = Color(0xFFEBE8EE),
    surfaceTint = Color(0xFF7A6890),
    scrim = Color(0xFF000000)
)

// Rosé Pine dark
private val RoséPineDark = dark(
    primary = Color(0xFFC4A7E7),
    onPrimary = Color(0xFF191724),
    primaryContainer = Color(0xFFDAD3F1),
    onPrimaryContainer = Color(0xFF191724),
    secondary = Color(0xFFEBBCBA),
    onSecondary = Color(0xFF191724),
    secondaryContainer = Color(0xFFE2D7E8),
    onSecondaryContainer = Color(0xFF191724),
    tertiary = Color(0xFFEBBCBA),
    onTertiary = Color(0xFF191724),
    background = Color(0xFF191724),
    onBackground = Color(0xFFE0DEF4),
    surface = Color(0xFF191724),
    onSurface = Color(0xFFE0DEF4),
    surfaceVariant = Color(0xFF26233A),
    onSurfaceVariant = Color(0xFF908CAA),
    outline = Color(0xFF908CAA),
    outlineVariant = Color(0xFF464457),
    error = Color(0xFFEB6F92),
    onError = Color(0xFF191724),
    errorContainer = Color(0xFFE2C8E0),
    onErrorContainer = Color(0xFF191724),
    inverseSurface = Color(0xFFE0DEF4),
    inverseOnSurface = Color(0xFF191724),
    inversePrimary = Color(0xFFDAD3F1),
    surfaceTint = Color(0xFFC4A7E7),
    scrim = Color(0xFF000000)
)

// Gruvbox light
private val GruvboxLight = light(
    primary = Color(0xFFAF3A03),
    onPrimary = Color(0xFFFBF1C7),
    primaryContainer = Color(0xFFF3E1D9),
    onPrimaryContainer = Color(0xFF3C3836),
    secondary = Color(0xFF076678),
    onSecondary = Color(0xFFFBF1C7),
    secondaryContainer = Color(0xFFDAE8EB),
    onSecondaryContainer = Color(0xFF3C3836),
    tertiary = Color(0xFF076678),
    onTertiary = Color(0xFFFBF1C7),
    background = Color(0xFFFBF1C7),
    onBackground = Color(0xFF3C3836),
    surface = Color(0xFFFBF1C7),
    onSurface = Color(0xFF3C3836),
    surfaceVariant = Color(0xFFEBDBB2),
    onSurfaceVariant = Color(0xFF504B45),
    outline = Color(0xFF504B45),
    outlineVariant = Color(0xFFC7BA97),
    error = Color(0xFF9D0006),
    onError = Color(0xFFFBF1C7),
    errorContainer = Color(0xFFF0D9DA),
    onErrorContainer = Color(0xFF3C3836),
    inverseSurface = Color(0xFF3C3836),
    inverseOnSurface = Color(0xFFFBF1C7),
    inversePrimary = Color(0xFFF3E1D9),
    surfaceTint = Color(0xFFAF3A03),
    scrim = Color(0xFF000000)
)

// Gruvbox dark
private val GruvboxDark = dark(
    primary = Color(0xFFFE8019),
    onPrimary = Color(0xFF282828),
    primaryContainer = Color(0xFFEFC993),
    onPrimaryContainer = Color(0xFF282828),
    secondary = Color(0xFF83A598),
    onSecondary = Color(0xFF282828),
    secondaryContainer = Color(0xFFD6D0AD),
    onSecondaryContainer = Color(0xFF282828),
    tertiary = Color(0xFF83A598),
    onTertiary = Color(0xFF282828),
    background = Color(0xFF282828),
    onBackground = Color(0xFFEBDBB2),
    surface = Color(0xFF282828),
    onSurface = Color(0xFFEBDBB2),
    surfaceVariant = Color(0xFF3C3836),
    onSurfaceVariant = Color(0xFFB8A794),
    outline = Color(0xFFB8A794),
    outlineVariant = Color(0xFF595554),
    error = Color(0xFFFB4934),
    onError = Color(0xFF282828),
    errorContainer = Color(0xFFEEBE99),
    onErrorContainer = Color(0xFF282828),
    inverseSurface = Color(0xFFEBDBB2),
    inverseOnSurface = Color(0xFF282828),
    inversePrimary = Color(0xFFEFC993),
    surfaceTint = Color(0xFFFE8019),
    scrim = Color(0xFF000000)
)

// Kanagawa light
private val KanagawaLight = light(
    primary = Color(0xFF4D699B),
    onPrimary = Color(0xFFF2ECBC),
    primaryContainer = Color(0xFFE4E8F0),
    onPrimaryContainer = Color(0xFF545464),
    secondary = Color(0xFF624C83),
    onSecondary = Color(0xFFF2ECBC),
    secondaryContainer = Color(0xFFE7E4EC),
    onSecondaryContainer = Color(0xFF545464),
    tertiary = Color(0xFF624C83),
    onTertiary = Color(0xFFF2ECBC),
    background = Color(0xFFF2ECBC),
    onBackground = Color(0xFF545464),
    surface = Color(0xFFF2ECBC),
    onSurface = Color(0xFF545464),
    surfaceVariant = Color(0xFFE5DDB0),
    onSurfaceVariant = Color(0xFF5E5E6D),
    outline = Color(0xFF5E5E6D),
    outlineVariant = Color(0xFFC2BB95),
    error = Color(0xFFD7474B),
    onError = Color(0xFFF2ECBC),
    errorContainer = Color(0xFFF9E3E4),
    onErrorContainer = Color(0xFF545464),
    inverseSurface = Color(0xFF545464),
    inverseOnSurface = Color(0xFFF2ECBC),
    inversePrimary = Color(0xFFE4E8F0),
    surfaceTint = Color(0xFF4D699B),
    scrim = Color(0xFF000000)
)

// Kanagawa dark
private val KanagawaDark = dark(
    primary = Color(0xFF7E9CD8),
    onPrimary = Color(0xFF1F1F28),
    primaryContainer = Color(0xFFC9CBC0),
    onPrimaryContainer = Color(0xFF1F1F28),
    secondary = Color(0xFF957FB8),
    onSecondary = Color(0xFF1F1F28),
    secondaryContainer = Color(0xFFCEC5BA),
    onSecondaryContainer = Color(0xFF1F1F28),
    tertiary = Color(0xFF957FB8),
    onTertiary = Color(0xFF1F1F28),
    background = Color(0xFF1F1F28),
    onBackground = Color(0xFFDCD7BA),
    surface = Color(0xFF1F1F28),
    onSurface = Color(0xFFDCD7BA),
    surfaceVariant = Color(0xFF2A2A37),
    onSurfaceVariant = Color(0xFF9A9888),
    outline = Color(0xFF9A9888),
    outlineVariant = Color(0xFF494955),
    error = Color(0xFFE46876),
    onError = Color(0xFF1F1F28),
    errorContainer = Color(0xFFDEC1AC),
    onErrorContainer = Color(0xFF1F1F28),
    inverseSurface = Color(0xFFDCD7BA),
    inverseOnSurface = Color(0xFF1F1F28),
    inversePrimary = Color(0xFFC9CBC0),
    surfaceTint = Color(0xFF7E9CD8),
    scrim = Color(0xFF000000)
)

// One Dark light
private val OneDarkLight = light(
    primary = Color(0xFF3A6DDC),
    onPrimary = Color(0xFFFAFAFA),
    primaryContainer = Color(0xFFE1E9FA),
    onPrimaryContainer = Color(0xFF383A42),
    secondary = Color(0xFFA626A4),
    onSecondary = Color(0xFFFAFAFA),
    secondaryContainer = Color(0xFFF2DEF1),
    onSecondaryContainer = Color(0xFF383A42),
    tertiary = Color(0xFFA626A4),
    onTertiary = Color(0xFFFAFAFA),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF383A42),
    surface = Color(0xFFFAFAFA),
    onSurface = Color(0xFF383A42),
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF62646B),
    outline = Color(0xFF62646B),
    outlineVariant = Color(0xFFCCCCCC),
    error = Color(0xFFE45649),
    onError = Color(0xFFFAFAFA),
    errorContainer = Color(0xFFFBE6E4),
    onErrorContainer = Color(0xFF383A42),
    inverseSurface = Color(0xFF383A42),
    inverseOnSurface = Color(0xFFFAFAFA),
    inversePrimary = Color(0xFFE1E9FA),
    surfaceTint = Color(0xFF3A6DDC),
    scrim = Color(0xFF000000)
)

// One Dark dark
private val OneDarkDark = dark(
    primary = Color(0xFF61AFEF),
    onPrimary = Color(0xFF282C34),
    primaryContainer = Color(0xFF9CB1C9),
    onPrimaryContainer = Color(0xFF282C34),
    secondary = Color(0xFFC678DD),
    onSecondary = Color(0xFF282C34),
    secondaryContainer = Color(0xFFB0A6C5),
    onSecondaryContainer = Color(0xFF282C34),
    tertiary = Color(0xFFC678DD),
    onTertiary = Color(0xFF282C34),
    background = Color(0xFF282C34),
    onBackground = Color(0xFFABB2BF),
    surface = Color(0xFF282C34),
    onSurface = Color(0xFFABB2BF),
    surfaceVariant = Color(0xFF2C313C),
    onSurfaceVariant = Color(0xFF97A0AE),
    outline = Color(0xFF959DAB),
    outlineVariant = Color(0xFF4B4F59),
    error = Color(0xFFE06C75),
    onError = Color(0xFF282C34),
    errorContainer = Color(0xFFB6A4B0),
    onErrorContainer = Color(0xFF282C34),
    inverseSurface = Color(0xFFABB2BF),
    inverseOnSurface = Color(0xFF282C34),
    inversePrimary = Color(0xFF9CB1C9),
    surfaceTint = Color(0xFF61AFEF),
    scrim = Color(0xFF000000)
)

// Ayu light
private val AyuLight = light(
    primary = Color(0xFFA56710),
    onPrimary = Color(0xFFFCFCFC),
    primaryContainer = Color(0xFFF2E3D5),
    onPrimaryContainer = Color(0xFF5C6166),
    secondary = Color(0xFF22A4E6),
    onSecondary = Color(0xFFFCFCFC),
    secondaryContainer = Color(0xFFDAEBFA),
    onSecondaryContainer = Color(0xFF5C6166),
    tertiary = Color(0xFF22A4E6),
    onTertiary = Color(0xFFFCFCFC),
    background = Color(0xFFFCFCFC),
    onBackground = Color(0xFF5C6166),
    surface = Color(0xFFFCFCFC),
    onSurface = Color(0xFF5C6166),
    surfaceVariant = Color(0xFFF3F4F5),
    onSurfaceVariant = Color(0xFF666B70),
    outline = Color(0xFF666B70),
    outlineVariant = Color(0xFFCECFD0),
    error = Color(0xFFF07171),
    onError = Color(0xFFFCFCFC),
    errorContainer = Color(0xFFFDEAEA),
    onErrorContainer = Color(0xFF5C6166),
    inverseSurface = Color(0xFF5C6166),
    inverseOnSurface = Color(0xFFFCFCFC),
    inversePrimary = Color(0xFFF2E3D5),
    surfaceTint = Color(0xFFA56710),
    scrim = Color(0xFF000000)
)

// Ayu dark
private val AyuDark = dark(
    primary = Color(0xFFFFCC66),
    onPrimary = Color(0xFF1F2430),
    primaryContainer = Color(0xFFD6CAB0),
    onPrimaryContainer = Color(0xFF1F2430),
    secondary = Color(0xFF73D0FF),
    onSecondary = Color(0xFF1F2430),
    secondaryContainer = Color(0xFFBACBCE),
    onSecondaryContainer = Color(0xFF1F2430),
    tertiary = Color(0xFF73D0FF),
    onTertiary = Color(0xFF1F2430),
    background = Color(0xFF1F2430),
    onBackground = Color(0xFFCCCAC2),
    surface = Color(0xFF1F2430),
    onSurface = Color(0xFFCCCAC2),
    surfaceVariant = Color(0xFF242936),
    onSurfaceVariant = Color(0xFF9AA5B7),
    outline = Color(0xFF9AA5B7),
    outlineVariant = Color(0xFF444954),
    error = Color(0xFFF28779),
    onError = Color(0xFF1F2430),
    errorContainer = Color(0xFFD4BDB3),
    onErrorContainer = Color(0xFF1F2430),
    inverseSurface = Color(0xFFCCCAC2),
    inverseOnSurface = Color(0xFF1F2430),
    inversePrimary = Color(0xFFD6CAB0),
    surfaceTint = Color(0xFFFFCC66),
    scrim = Color(0xFF000000)
)


val themePalettes: List<ThemePalette> = listOf(
    ThemePalette("Catetan", CatetanLight, CatetanDark),
    ThemePalette("Catppuccin", CatppuccinLight, CatppuccinDark),
    ThemePalette("Tokyo Night", TokyoNightLight, TokyoNightDark),
    ThemePalette("Rosé Pine", RoséPineLight, RoséPineDark),
    ThemePalette("Gruvbox", GruvboxLight, GruvboxDark),
    ThemePalette("Kanagawa", KanagawaLight, KanagawaDark),
    ThemePalette("One Dark", OneDarkLight, OneDarkDark),
    ThemePalette("Ayu", AyuLight, AyuDark),
)

const val DEFAULT_THEME_NAME = "Catetan"

fun themePalette(name: String): ThemePalette =
    themePalettes.firstOrNull { it.name == name } ?: themePalettes.first()

fun colorSchemeFor(name: String, dark: Boolean): ColorScheme {
    val p = themePalette(name)
    return if (dark) p.dark else p.light
}
