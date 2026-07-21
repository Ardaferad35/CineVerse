package com.arda.cineverse.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

@Composable
fun CineVerseLoginBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B10))
            // Üst-orta mor parıltı
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF7C5CFC).copy(alpha = 0.22f),
                        Color(0xFF7C5CFC).copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    center = Offset.Unspecified,
                    radius = 900f
                )
            )
    ) {
        // Sağ-alt teal parıltı için ikinci katman
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00D4C8).copy(alpha = 0.13f),
                            Color(0xFF00D4C8).copy(alpha = 0.04f),
                            Color.Transparent
                        ),
                        center = Offset.Infinite, // sağ-alt köşeye yakın konumlanır
                        radius = 800f
                    )
                )
        )
        content()
    }
}