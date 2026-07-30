package com.arda.cineverse.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arda.cineverse.ui.theme.Background
import com.arda.cineverse.ui.theme.SurfaceVariant

/**
 * Modern parıldayan (Shimmer Effect) animasyonu uygulayan Modifier uzantısı.
 */
fun Modifier.shimmerEffect(
    baseColor: Color = Color(0xFF262538),
    highlightColor: Color = Color(0xFF3F3D56),
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerTranslate",
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            baseColor,
            highlightColor,
            baseColor,
        ),
        start = Offset(translateAnim - 300f, translateAnim - 300f),
        end = Offset(translateAnim, translateAnim),
    )

    this.background(brush)
}

/**
 * Ana Sayfa Banner (Öne Çıkan İçerik) Yükleme İskeleti
 */
@Composable
fun HomeBannerShimmer(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(420.dp)
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .shimmerEffect(),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .shimmerEffect(Color(0xFF33314A), Color(0xFF4C4A6B)),
            )
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .width(220.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .shimmerEffect(Color(0xFF33314A), Color(0xFF4C4A6B)),
            )
            Spacer(Modifier.height(12.dp))
            Row {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .shimmerEffect(Color(0xFF33314A), Color(0xFF4C4A6B)),
                )
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .shimmerEffect(Color(0xFF33314A), Color(0xFF4C4A6B)),
                )
            }
        }
    }
}

/**
 * Film / Dizi Kartı Yükleme İskeleti
 */
@Composable
fun MovieCardShimmer(
    width: Dp = 140.dp,
    height: Dp = 210.dp,
) {
    Column(
        modifier = Modifier.width(width),
    ) {
        Box(
            modifier = Modifier
                .width(width)
                .height(height)
                .clip(RoundedCornerShape(14.dp))
                .shimmerEffect(),
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .width(width * 0.8f)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect(Color(0xFF33314A), Color(0xFF4C4A6B)),
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(width * 0.5f)
                .height(10.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect(Color(0xFF33314A), Color(0xFF4C4A6B)),
        )
    }
}

/**
 * Yatay Film / Dizi Satırı Yükleme İskeleti
 */
@Composable
fun HomeSectionShimmer(
    titleWidth: Dp = 150.dp,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .width(titleWidth)
                .height(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .shimmerEffect(Color(0xFF33314A), Color(0xFF4C4A6B)),
        )
        Spacer(Modifier.height(14.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            userScrollEnabled = false,
        ) {
            items(5) {
                MovieCardShimmer()
            }
        }
    }
}

/**
 * Izgara (Grid) Yükleme İskeleti (Arama ve Listem ekranları için)
 */
@Composable
fun GridShimmer(
    modifier: Modifier = Modifier,
    columns: Int = 3,
    itemCount: Int = 9,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        userScrollEnabled = false,
    ) {
        items(itemCount) {
            MovieCardShimmer(width = 110.dp, height = 160.dp)
        }
    }
}

/**
 * Tüm Ana Sayfa Yükleme İskeleti (Full Home Shimmer Screen)
 */
@Composable
fun HomeScreenShimmer() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        HomeBannerShimmer()
        Spacer(Modifier.height(16.dp))
        HomeSectionShimmer(titleWidth = 180.dp)
        HomeSectionShimmer(titleWidth = 140.dp)
    }
}
