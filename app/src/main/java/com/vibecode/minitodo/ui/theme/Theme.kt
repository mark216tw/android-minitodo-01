package com.vibecode.minitodo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.vibecode.minitodo.domain.ThemeMode
import com.vibecode.minitodo.domain.ThemePalette

fun resolveDarkTheme(themeMode: ThemeMode, systemDark: Boolean): Boolean = when (themeMode) {
    ThemeMode.SYSTEM -> systemDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

fun ThemePalette.effectivePalette(): ThemePalette =
    if (this == ThemePalette.DYNAMIC && android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
        ThemePalette.BLUE
    } else {
        this
    }

@Composable
fun MiniTodoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: ThemePalette = ThemePalette.DYNAMIC,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val (lightScheme, darkScheme) = palette.colorSchemes(context)

    MaterialTheme(
        colorScheme = if (darkTheme) darkScheme else lightScheme,
        typography = Typography(),
        content = content,
    )
}
