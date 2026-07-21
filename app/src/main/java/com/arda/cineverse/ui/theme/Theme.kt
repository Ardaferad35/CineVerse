package com.arda.cineverse.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CineVerseColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    secondary = Accent,
    onSecondary = OnPrimary,
    background = Background,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DividerColor,
    error = ErrorColor,
    onError = OnPrimary,
)

@Composable
fun CineVerseTheme(content: @Composable () -> Unit) {
    // Design system is dark-only; ignore system light/dark setting.
    MaterialTheme(
        colorScheme = CineVerseColorScheme,
        typography = CineVerseTypography,
        content = content,
    )
}
