package com.arda.cineverse.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Tüm renk paletinin tek yerde toplandığı veri sınıfı. "DarkColors" ve
 * "LightColors" olmak üzere iki farklı örneği var; hangisinin aktif
 * olduğu CineVerseTheme tarafından belirlenip LocalCineVerseColors ile
 * dağıtılıyor.
 */
data class CineVerseColors(
    val primary: Color,
    val primaryDark: Color,
    val accent: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onPrimary: Color,
    val onSurface: Color,
    val textSecondary: Color,
    val dividerColor: Color,
    val warning: Color,
    val errorColor: Color,
) {
    val primaryGradient: List<Color> get() = listOf(primary, accent)
}

val DarkColors = CineVerseColors(
    primary = Color(0xFF7C5CFC),
    primaryDark = Color(0xFF5A3CF0),
    accent = Color(0xFF00D4C8),
    background = Color(0xFF0B0B10),
    surface = Color(0xFF171821),
    surfaceVariant = Color(0xFF222330),
    onPrimary = Color(0xFFFFFFFF),
    onSurface = Color(0xFFE6E6F0),
    textSecondary = Color(0xFFA6A8B5),
    dividerColor = Color(0xFF2F3140),
    warning = Color(0xFFFFC857),
    errorColor = Color(0xFFFF4D6D),
)

// CineVerse Light — kullanıcının sağladığı görsel palete göre
val LightColors = CineVerseColors(
    primary = Color(0xFF7B61FF),
    primaryDark = Color(0xFF5A45E0),
    accent = Color(0xFFFF5C8A),
    background = Color(0xFFF7F7FA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F1F5),
    onPrimary = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1C1E),
    textSecondary = Color(0xFF6B7280),
    dividerColor = Color(0xFFE5E7EB),
    warning = Color(0xFFFFC857),
    errorColor = Color(0xFFDC2626),
)

val LocalCineVerseColors = staticCompositionLocalOf { DarkColors }

// Aşağıdaki isimler eskiden sabit değerlerdi, artık aktif temaya göre
// anlık değişiyor. Projenin geri kalanında "Primary", "Background" vb.
// kullanan hiçbir dosyanın değişmesine gerek YOK — hepsi otomatik olarak
// doğru temanın rengini alıyor.
val Primary: Color @Composable get() = LocalCineVerseColors.current.primary
val PrimaryDark: Color @Composable get() = LocalCineVerseColors.current.primaryDark
val Accent: Color @Composable get() = LocalCineVerseColors.current.accent
val Background: Color @Composable get() = LocalCineVerseColors.current.background
val Surface: Color @Composable get() = LocalCineVerseColors.current.surface
val SurfaceVariant: Color @Composable get() = LocalCineVerseColors.current.surfaceVariant
val OnPrimary: Color @Composable get() = LocalCineVerseColors.current.onPrimary
val OnSurface: Color @Composable get() = LocalCineVerseColors.current.onSurface
val TextSecondary: Color @Composable get() = LocalCineVerseColors.current.textSecondary
val DividerColor: Color @Composable get() = LocalCineVerseColors.current.dividerColor
val Warning: Color @Composable get() = LocalCineVerseColors.current.warning
val ErrorColor: Color @Composable get() = LocalCineVerseColors.current.errorColor
val PrimaryGradient: List<Color> @Composable get() = LocalCineVerseColors.current.primaryGradient