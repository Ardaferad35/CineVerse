package com.arda.cineverse.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun CineVerseTheme(
    isDarkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = if (isDarkTheme) DarkColors else LightColors

    val materialColorScheme = if (isDarkTheme) {
        darkColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            secondary = colors.accent,
            onSecondary = colors.onPrimary,
            background = colors.background,
            onBackground = colors.onSurface,
            surface = colors.surface,
            onSurface = colors.onSurface,
            surfaceVariant = colors.surfaceVariant,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.dividerColor,
            error = colors.errorColor,
            onError = colors.onPrimary,
        )
    } else {
        lightColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            secondary = colors.accent,
            onSecondary = colors.onPrimary,
            background = colors.background,
            onBackground = colors.onSurface,
            surface = colors.surface,
            onSurface = colors.onSurface,
            surfaceVariant = colors.surfaceVariant,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.dividerColor,
            error = colors.errorColor,
            onError = colors.onPrimary,
        )
    }

    CompositionLocalProvider(LocalCineVerseColors provides colors) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = CineVerseTypography,
            content = content,
        )
    }
}