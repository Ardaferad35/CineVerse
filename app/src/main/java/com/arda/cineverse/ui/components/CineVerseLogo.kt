package com.arda.cineverse.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * CineVerse markasının yüksek kaliteli, vektörel logosu.
 * Jetpack Compose Canvas kullanılarak programatik olarak çizilir.
 * Bu sayede her çözünürlükte keskin kalır.
 */
@Composable
fun CineVerseLogo(
    modifier: Modifier = Modifier,
    size: Dp = 160.dp
) {
    val logoPurple = Color(0xFF6C5CE7)
    val logoBlue = Color(0xFFA78BFA)
    val logoGold = Color(0xFFF5C542)

    Canvas(modifier = modifier.size(size)) {
        val canvasWidth = size.toPx()
        val canvasHeight = size.toPx()
        val center = Offset(canvasWidth / 2, canvasHeight / 2)
        val radius = canvasWidth * 0.4f
        val strokeWidth = canvasWidth * 0.12f

        // 1. "C" Harfi / Film Şeridi Yayını Çiz
        val gradient = Brush.sweepGradient(
            colors = listOf(logoPurple, logoBlue, logoPurple),
            center = center
        )

        drawArc(
            brush = gradient,
            startAngle = 45f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // 2. Film Şeridi Deliklerini (Punches) Çiz
        val holeCount = 12
        val holeAngleStep = 270f / (holeCount - 1)
        val holeSize = strokeWidth * 0.4f
        
        for (i in 0 until holeCount) {
            val angle = 45f + (i * holeAngleStep)
            val angleRad = Math.toRadians(angle.toDouble())
            
            // Delikleri yayın tam ortasına hizala
            val holeX = center.x + radius * cos(angleRad).toFloat()
            val holeY = center.y + radius * sin(angleRad).toFloat()
            
            rotate(degrees = angle + 90f, pivot = Offset(holeX, holeY)) {
                drawRect(
                    color = Color.White.copy(alpha = 0.3f),
                    topLeft = Offset(holeX - holeSize / 2, holeY - holeSize / 2),
                    size = Size(holeSize, holeSize * 0.6f),
                    style = Fill
                )
            }
        }

        // 3. Merkezdeki Play İkonunu Çiz (Altın Üçgen)
        val triangleSize = radius * 0.5f
        val path = Path().apply {
            moveTo(center.x + triangleSize * 0.6f, center.y) // Sağ uç
            lineTo(center.x - triangleSize * 0.3f, center.y - triangleSize * 0.5f) // Üst sol
            lineTo(center.x - triangleSize * 0.3f, center.y + triangleSize * 0.5f) // Alt sol
            close()
        }
        
        drawPath(
            path = path,
            color = logoGold,
            style = Fill
        )
        
        // Play ikonuna hafif bir gölge/parıltı efekti
        drawPath(
            path = path,
            color = Color.Black.copy(alpha = 0.1f),
            style = Stroke(width = 2f)
        )
    }
}
