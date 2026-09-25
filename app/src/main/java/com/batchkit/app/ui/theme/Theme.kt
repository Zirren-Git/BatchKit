package com.batchkit.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.batchkit.app.core.model.ThemeMode

private val DarkColors = darkColorScheme(
    primary = Blue80,
    secondary = Teal80,
    background = Slate10,
    surface = Slate20,
)

private val LightColors = lightColorScheme(
    primary = Blue40,
    secondary = Teal40,
    background = Slate95,
    surface = Slate90,
)

/** Material 3 theme that follows the user choice, with dynamic colour on Android 12+. */
@Composable
fun BatchKitTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BatchKitTypography,
        content = content,
    )
}
