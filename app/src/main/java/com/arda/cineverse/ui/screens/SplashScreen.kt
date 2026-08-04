package com.arda.cineverse.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arda.cineverse.R
import com.arda.cineverse.ui.components.CineVerseLogo
import com.arda.cineverse.ui.theme.ThemeState
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val isDark = ThemeState.isDarkTheme

    // Colors from the design
    val backgroundColor = if (isDark) Color(0xFF161B22) else Color(0xFFF7F8FC)
    val textColor = if (isDark) Color.White else Color(0xFF1A1D23)
    val accentColor = Color(0xFF6C5CE7)
    val subtitleColor = if (isDark) Color(0xFFA78BFA) else Color(0xFF6C5CE7).copy(alpha = 0.7f)

    // Animation States
    var startAnimation by remember { mutableStateOf(false) }
    
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "alpha"
    )
    
    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.8f,
        animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        // Render hatalarını önlemek için animasyonun başlamasından önce çok kısa bir bekleme
        delay(100) 
        startAnimation = true
        delay(2200) 
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Decorative background elements kaldırıldı - Bazı emülatörlerde render sorununa yol açabiliyor.

        // Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // New Programmatic Logo
            CineVerseLogo(
                modifier = Modifier
                    .scale(scaleAnim)
                    .alpha(alphaAnim),
                size = 180.dp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Title
            Text(
                text = "CineVerse",
                color = textColor,
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.alpha(alphaAnim)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Decorative star/divider element
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(alphaAnim * 0.7f)
            ) {
                Box(modifier = Modifier.width(40.dp).height(1.dp).background(accentColor.copy(alpha = 0.5f)))
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(12.dp).padding(horizontal = 4.dp)
                )
                Box(modifier = Modifier.width(40.dp).height(1.dp).background(accentColor.copy(alpha = 0.5f)))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Subtitles
            Text(
                text = "Discover Movies",
                color = textColor.copy(alpha = 0.8f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.alpha(alphaAnim)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Beyond the Screen",
                color = subtitleColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.alpha(alphaAnim)
            )
        }
    }
}
