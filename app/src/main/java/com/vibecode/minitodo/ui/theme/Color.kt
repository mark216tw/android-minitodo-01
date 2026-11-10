package com.vibecode.minitodo.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.vibecode.minitodo.domain.ThemePalette
import com.vibecode.minitodo.domain.TodoColor

val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4A5F8A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9E2FF),
    onPrimaryContainer = Color(0xFF001B3F),
    secondary = Color(0xFF5B5F71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0E1F9),
    onSecondaryContainer = Color(0xFF181B2B),
    tertiary = Color(0xFF76546A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8EC),
    onTertiaryContainer = Color(0xFF2C1225),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFBF8FD),
    onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFFBF8FD),
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFE2E1EC),
    onSurfaceVariant = Color(0xFF45464F),
    outline = Color(0xFF767680),
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFB3C5FF),
    onPrimary = Color(0xFF1A3059),
    primaryContainer = Color(0xFF324771),
    onPrimaryContainer = Color(0xFFD9E2FF),
    secondary = Color(0xFFC4C5DD),
    onSecondary = Color(0xFF2D2F42),
    secondaryContainer = Color(0xFF434659),
    onSecondaryContainer = Color(0xFFE0E1F9),
    tertiary = Color(0xFFE5BAD1),
    onTertiary = Color(0xFF442639),
    tertiaryContainer = Color(0xFF5D3C51),
    onTertiaryContainer = Color(0xFFFFD8EC),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1B1B1F),
    onBackground = Color(0xFFE4E1E6),
    surface = Color(0xFF1B1B1F),
    onSurface = Color(0xFFE4E1E6),
    surfaceVariant = Color(0xFF45464F),
    onSurfaceVariant = Color(0xFFC6C5D0),
    outline = Color(0xFF90909A),
)

private val GreenLightColorScheme = LightColorScheme.copy(
    primary = Color(0xFF386A20),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB7F397),
    onPrimaryContainer = Color(0xFF042100),
    secondary = Color(0xFF55624C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD9E7CB),
    onSecondaryContainer = Color(0xFF131F0D),
    tertiary = Color(0xFF386667),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBBEBEC),
    onTertiaryContainer = Color(0xFF002021),
    surfaceTint = Color(0xFF386A20),
    inversePrimary = Color(0xFF9CD67D),
)

private val GreenDarkColorScheme = DarkColorScheme.copy(
    primary = Color(0xFF9CD67D),
    onPrimary = Color(0xFF0A3900),
    primaryContainer = Color(0xFF1F5110),
    onPrimaryContainer = Color(0xFFB7F397),
    secondary = Color(0xFFBDCBB0),
    onSecondary = Color(0xFF283422),
    secondaryContainer = Color(0xFF3E4A36),
    onSecondaryContainer = Color(0xFFD9E7CB),
    tertiary = Color(0xFFA0CFD0),
    onTertiary = Color(0xFF003738),
    tertiaryContainer = Color(0xFF1E4E4F),
    onTertiaryContainer = Color(0xFFBBEBEC),
    surfaceTint = Color(0xFF9CD67D),
    inversePrimary = Color(0xFF386A20),
)

private val PurpleLightColorScheme = LightColorScheme.copy(
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    surfaceTint = Color(0xFF6750A4),
    inversePrimary = Color(0xFFD0BCFF),
)

private val PurpleDarkColorScheme = DarkColorScheme.copy(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    surfaceTint = Color(0xFFD0BCFF),
    inversePrimary = Color(0xFF6750A4),
)

private val OrangeLightColorScheme = LightColorScheme.copy(
    primary = Color(0xFF855300),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDDB3),
    onPrimaryContainer = Color(0xFF2B1700),
    secondary = Color(0xFF6F5A3F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFBDEBC),
    onSecondaryContainer = Color(0xFF271904),
    tertiary = Color(0xFF57643D),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDBE8BB),
    onTertiaryContainer = Color(0xFF151F04),
    surfaceTint = Color(0xFF855300),
    inversePrimary = Color(0xFFFFB95F),
)

private val OrangeDarkColorScheme = DarkColorScheme.copy(
    primary = Color(0xFFFFB95F),
    onPrimary = Color(0xFF472A00),
    primaryContainer = Color(0xFF664000),
    onPrimaryContainer = Color(0xFFFFDDB3),
    secondary = Color(0xFFDEC1A0),
    onSecondary = Color(0xFF3E2D16),
    secondaryContainer = Color(0xFF56432A),
    onSecondaryContainer = Color(0xFFFBDEBC),
    tertiary = Color(0xFFBFCEA1),
    onTertiary = Color(0xFF2A3517),
    tertiaryContainer = Color(0xFF404C2B),
    onTertiaryContainer = Color(0xFFDBE8BB),
    surfaceTint = Color(0xFFFFB95F),
    inversePrimary = Color(0xFF855300),
)

private val PinkLightColorScheme = LightColorScheme.copy(
    primary = Color(0xFFB0005A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9E2),
    onPrimaryContainer = Color(0xFF3E001D),
    secondary = Color(0xFF74565F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFD9E2),
    onSecondaryContainer = Color(0xFF2B151C),
    tertiary = Color(0xFF7C5635),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDCC1),
    onTertiaryContainer = Color(0xFF2E1500),
    surfaceTint = Color(0xFFB0005A),
    inversePrimary = Color(0xFFFFB1C8),
)

private val PinkDarkColorScheme = DarkColorScheme.copy(
    primary = Color(0xFFFFB1C8),
    onPrimary = Color(0xFF65002A),
    primaryContainer = Color(0xFF8E0045),
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = Color(0xFFE3BDC6),
    onSecondary = Color(0xFF422931),
    secondaryContainer = Color(0xFF5A3F47),
    onSecondaryContainer = Color(0xFFFFD9E2),
    tertiary = Color(0xFFEFBD94),
    onTertiary = Color(0xFF47290B),
    tertiaryContainer = Color(0xFF613F20),
    onTertiaryContainer = Color(0xFFFFDCC1),
    surfaceTint = Color(0xFFFFB1C8),
    inversePrimary = Color(0xFFB0005A),
)

private val TealLightColorScheme = LightColorScheme.copy(
    primary = Color(0xFF006A60),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF74F8E5),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFF4A635F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE8E2),
    onSecondaryContainer = Color(0xFF051F1B),
    tertiary = Color(0xFF456179),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCBE6FF),
    onTertiaryContainer = Color(0xFF001E31),
    surfaceTint = Color(0xFF006A60),
    inversePrimary = Color(0xFF53DBC9),
)

private val TealDarkColorScheme = DarkColorScheme.copy(
    primary = Color(0xFF53DBC9),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF005048),
    onPrimaryContainer = Color(0xFF74F8E5),
    secondary = Color(0xFFB0CCC6),
    onSecondary = Color(0xFF1B3531),
    secondaryContainer = Color(0xFF324B47),
    onSecondaryContainer = Color(0xFFCCE8E2),
    tertiary = Color(0xFFADCAE5),
    onTertiary = Color(0xFF163349),
    tertiaryContainer = Color(0xFF2D4960),
    onTertiaryContainer = Color(0xFFCBE6FF),
    surfaceTint = Color(0xFF53DBC9),
    inversePrimary = Color(0xFF006A60),
)

private val paletteSchemes: Map<ThemePalette, Pair<ColorScheme, ColorScheme>> = mapOf(
    ThemePalette.BLUE to (LightColorScheme to DarkColorScheme),
    ThemePalette.GREEN to (GreenLightColorScheme to GreenDarkColorScheme),
    ThemePalette.PURPLE to (PurpleLightColorScheme to PurpleDarkColorScheme),
    ThemePalette.ORANGE to (OrangeLightColorScheme to OrangeDarkColorScheme),
    ThemePalette.PINK to (PinkLightColorScheme to PinkDarkColorScheme),
    ThemePalette.TEAL to (TealLightColorScheme to TealDarkColorScheme),
)

fun ThemePalette.colorScheme(darkTheme: Boolean): ColorScheme {
    val schemes = paletteSchemes[this] ?: (LightColorScheme to DarkColorScheme)
    return if (darkTheme) schemes.second else schemes.first
}

fun ThemePalette.colorSchemes(context: Context): Pair<ColorScheme, ColorScheme> {
    if (this == ThemePalette.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        return dynamicLightColorScheme(context) to dynamicDarkColorScheme(context)
    }
    return colorScheme(darkTheme = false) to colorScheme(darkTheme = true)
}

val ThemePalette.previewColor: Color
    get() = when (this) {
        ThemePalette.DYNAMIC -> Color(0xFF6750A4)
        ThemePalette.BLUE -> LightColorScheme.primary
        ThemePalette.GREEN -> GreenLightColorScheme.primary
        ThemePalette.PURPLE -> PurpleLightColorScheme.primary
        ThemePalette.ORANGE -> OrangeLightColorScheme.primary
        ThemePalette.PINK -> PinkLightColorScheme.primary
        ThemePalette.TEAL -> TealLightColorScheme.primary
    }

fun ThemePalette.onThemePaletteColor(): Color =
    if (this == ThemePalette.DYNAMIC) {
        Color(0xFFFFFFFF)
    } else if (previewColor.luminance() > 0.5f) {
        Color(0xFF1B1B1F)
    } else {
        Color(0xFFFFFFFF)
    }

private val NoneColor = Color(0xFF8E9099)
private val RedColor = Color(0xFFE53935)
private val OrangeColor = Color(0xFFFB8C00)
private val YellowColor = Color(0xFFF9A825)
private val LimeColor = Color(0xFF7CB342)
private val GreenColor = Color(0xFF43A047)
private val TealColor = Color(0xFF00897B)
private val CyanColor = Color(0xFF00ACC1)
private val BlueColor = Color(0xFF1E88E5)
private val IndigoColor = Color(0xFF3949AB)
private val PurpleColor = Color(0xFF8E24AA)
private val DeepPurpleColor = Color(0xFF5E35B1)
private val PinkColor = Color(0xFFD81B60)
private val BrownColor = Color(0xFF6D4C41)
private val BlueGreyColor = Color(0xFF546E7A)
private val DeepOrangeColor = Color(0xFFF4511E)

fun todoColor(color: TodoColor): Color = when (color) {
    TodoColor.NONE -> NoneColor
    TodoColor.RED -> RedColor
    TodoColor.ORANGE -> OrangeColor
    TodoColor.YELLOW -> YellowColor
    TodoColor.LIME -> LimeColor
    TodoColor.GREEN -> GreenColor
    TodoColor.TEAL -> TealColor
    TodoColor.CYAN -> CyanColor
    TodoColor.BLUE -> BlueColor
    TodoColor.INDIGO -> IndigoColor
    TodoColor.PURPLE -> PurpleColor
    TodoColor.DEEP_PURPLE -> DeepPurpleColor
    TodoColor.PINK -> PinkColor
    TodoColor.BROWN -> BrownColor
    TodoColor.BLUE_GREY -> BlueGreyColor
    TodoColor.DEEP_ORANGE -> DeepOrangeColor
}

fun onTodoColor(color: TodoColor): Color = when (color) {
    TodoColor.YELLOW,
    TodoColor.ORANGE,
    TodoColor.LIME,
    TodoColor.CYAN,
    -> Color(0xFF1B1B1F)

    else -> Color(0xFFFFFFFF)
}
