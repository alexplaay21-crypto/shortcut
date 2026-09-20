package com.sunflower.shortcut.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Persisted in settings (data.models.AppSettings, generated later) — kept here
 * because it is fundamentally a theming concept and MainActivity reads it
 * before the data module exists in this incremental build.
 */
enum class AppTheme {
    SYSTEM, LIGHT, DARK, AMOLED
}

private val DarkScheme = darkColorScheme(
    primary = Amber,
    onPrimary = Color(0xFF1A1200),
    secondary = AmberDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnBackgroundDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    error = ErrorDark
)

private val AmoledScheme = DarkScheme.copy(
    background = BackgroundAmoled,
    surface = SurfaceAmoled,
    surfaceVariant = SurfaceVariantAmoled
)

private val LightScheme = lightColorScheme(
    primary = AmberDark,
    onPrimary = Color.White,
    secondary = Amber,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnBackgroundLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    error = ErrorLight
)

@Composable
fun ShortcutTheme(
    appTheme: AppTheme,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val colorScheme = when (appTheme) {
        AppTheme.SYSTEM -> if (systemDark) DarkScheme else LightScheme
        AppTheme.LIGHT -> LightScheme
        AppTheme.DARK -> DarkScheme
        AppTheme.AMOLED -> AmoledScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ShortcutTypography,
        shapes = ShortcutShapes,
        content = content
    )
}
