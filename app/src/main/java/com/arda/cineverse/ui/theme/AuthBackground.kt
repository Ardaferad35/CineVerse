package com.arda.cineverse.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Login, Register ve Forgot Password ekranlarında ortak kullanılan arka plan.
 * Görseldeki tasarıma göre: koyu zemin + üstten gelen soft mor parıltı.
 */
@Composable
fun CineVerseAuthBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            // Zemin rengi (#0B0B10)
            .background(Color(0xFF0B0B10))
            // Üst-orta mor parıltı (#7C5CFC), ekranın üst kısmından yumuşakça yayılıyor
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF7C5CFC).copy(alpha = 0.20f),
                        Color(0xFF5A3CF0).copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = Offset.Unspecified,
                    radius = 820f
                )
            )
    ) {
        content()
    }
}