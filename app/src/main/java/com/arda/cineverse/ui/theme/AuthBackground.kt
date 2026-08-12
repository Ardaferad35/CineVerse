package com.arda.cineverse.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Login, Register ve Forgot Password ekranlarında ortak kullanılan arka plan.
 * Aktif temanın zemin rengi üzerine iki soft parıltı çizer: üst-ortadan
 * yayılan primary (mor) ve sağ-alt köşeden yayılan accent parıltısı.
 * Açık temada parıltılar daha soluk tutulur ki yazılar okunur kalsın.
 */
@Composable
fun CineVerseAuthBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val backgroundColor = Background
    val primaryGlow = Primary
    val accentGlow = Accent
    val isDark = ThemeState.isDarkTheme
    val primaryAlpha = if (isDark) 0.20f else 0.10f
    val accentAlpha = if (isDark) 0.13f else 0.06f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .drawBehind {
                if (size.width <= 0f || size.height <= 0f) return@drawBehind
                // Üst-orta parıltı
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(
                            primaryGlow.copy(alpha = primaryAlpha),
                            primaryGlow.copy(alpha = primaryAlpha * 0.35f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width / 2f, 0f),
                        radius = size.width * 1.1f,
                    )
                )
                // Sağ-alt parıltı
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(
                            accentGlow.copy(alpha = accentAlpha),
                            accentGlow.copy(alpha = accentAlpha * 0.3f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width, size.height),
                        radius = size.width * 0.95f,
                    )
                )
            }
    ) {
        content()
    }
}
