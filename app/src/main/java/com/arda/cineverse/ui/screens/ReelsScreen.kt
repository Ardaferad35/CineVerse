package com.arda.cineverse.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.arda.cineverse.R
import com.arda.cineverse.data.model.ReelItem
import com.arda.cineverse.ui.components.CVGradientButton
import com.arda.cineverse.ui.components.RecommendShareSheet
import com.arda.cineverse.ui.theme.*
import com.arda.cineverse.viewmodel.MovieReelsViewModel
import com.arda.cineverse.viewmodel.RecommendShareViewModel
import kotlinx.coroutines.delay

private val YouTubeRedGradient = listOf(Color(0xFFFF0000), Color(0xFFC40000))
private val GoldStarColor = Color(0xFFFFC857)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReelsScreen(
    onBack: () -> Unit,
    onMovieClick: (Int) -> Unit,
    onTvShowClick: (Int) -> Unit,
    viewModel: MovieReelsViewModel = hiltViewModel(),
    recommendShareViewModel: RecommendShareViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val shareState by recommendShareViewModel.uiState.collectAsState()
    val context = LocalContext.current

    var shareMediaItem by remember { mutableStateOf<ReelItem?>(null) }
    var showShareSheet by remember { mutableStateOf(false) }

    LaunchedEffect(shareState.sentCount) {
        val count = shareState.sentCount ?: return@LaunchedEffect
        showShareSheet = false
        val message = if (count == 1) {
            context.getString(R.string.share_sent_one)
        } else {
            context.getString(R.string.share_sent_many, count)
        }
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Primary, strokeWidth = 3.dp, modifier = Modifier.size(44.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Benzersiz Keşif Akışı Hazırlanıyor...",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
            uiState.errorMessage != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(uiState.errorMessage!!, color = OnSurface, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    CVGradientButton(text = stringResource(R.string.common_retry), onClick = { viewModel.loadReels() })
                }
            }
            uiState.reels.isNotEmpty() -> {
                val pagerState = rememberPagerState(pageCount = { uiState.reels.size })

                // Sonsuz kaydırma (Infinite Scroll) & Görsel Ön-Yükleme (Image Preloading)
                LaunchedEffect(pagerState.currentPage, uiState.reels) {
                    if (pagerState.currentPage >= uiState.reels.size - 3 && !uiState.isLoadingMore) {
                        viewModel.loadMoreReels()
                    }
                    // Sonraki 3 kartın afişini Coil disk/bellek önbelleğine önden yükle
                    val maxIndex = uiState.reels.size - 1
                    val nextStart = pagerState.currentPage + 1
                    val nextEnd = minOf(pagerState.currentPage + 3, maxIndex)
                    if (nextStart <= nextEnd) {
                        for (nextIndex in nextStart..nextEnd) {
                            val nextReel = uiState.reels.getOrNull(nextIndex)
                            val posterUrl = nextReel?.posterUrl
                            if (!posterUrl.isNullOrBlank()) {
                                val request = ImageRequest.Builder(context)
                                    .data(posterUrl)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .build()
                                context.imageLoader.enqueue(request)
                            }
                        }
                    }
                }

                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    val reel = uiState.reels[page]
                    val key = "${reel.mediaType}_${reel.id}"
                    val isFavorite = key in uiState.favoriteKeys
                    val isInWatchlist = key in uiState.watchlistKeys

                    HeroDiscoveryCardItem(
                        reel = reel,
                        isFavorite = isFavorite,
                        isInWatchlist = isInWatchlist,
                        onOpenDetail = {
                            if (reel.mediaType == "tv") onTvShowClick(reel.id) else onMovieClick(reel.id)
                        },
                        onToggleFavorite = { viewModel.toggleFavorite(reel) },
                        onToggleWatchlist = { viewModel.toggleWatchlist(reel) },
                        onSwipeRightBothSaved = { viewModel.addBothToFavoritesAndWatchlist(reel) },
                        onShare = {
                            recommendShareViewModel.reset()
                            shareMediaItem = reel
                            showShareSheet = true
                        },
                    )
                }
            }
        }

        // Üst Sabit Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = Color.White)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Primary.copy(alpha = 0.85f), Color(0xFFE040FB).copy(alpha = 0.85f))
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Keşif Akışı",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Akışı Yenile / Karıştır Butonu
            IconButton(
                onClick = { viewModel.loadReels() },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(1.dp, Primary.copy(alpha = 0.4f), CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Akışı Yenile",
                    tint = Primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // Arkadaşa Öneri Gönderme Sheet'i
        if (showShareSheet && shareMediaItem != null) {
            val item = shareMediaItem!!
            RecommendShareSheet(
                state = shareState,
                mediaTitle = item.title,
                posterUrl = item.posterUrl,
                isTvShow = item.mediaType == "tv",
                onToggleFriend = recommendShareViewModel::toggleFriend,
                onNoteChange = recommendShareViewModel::onNoteChange,
                onSend = {
                    recommendShareViewModel.send(
                        mediaId = item.id,
                        mediaType = item.mediaType,
                        mediaTitle = item.title,
                        posterUrl = item.posterUrl,
                    )
                },
                onDismiss = { showShareSheet = false },
            )
        }
    }
}

@Composable
private fun HeroDiscoveryCardItem(
    reel: ReelItem,
    isFavorite: Boolean,
    isInWatchlist: Boolean,
    onOpenDetail: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleWatchlist: () -> Unit,
    onSwipeRightBothSaved: () -> Unit,
    onShare: () -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var isExpandedOverview by remember { mutableStateOf(false) }
    var showSwipeFeedback by remember { mutableStateOf(false) }
    var totalDragX by remember { mutableFloatStateOf(0f) }

    fun playYouTubeTrailer() {
        if (reel.trailerKey.isBlank() || reel.trailerKey == "trailer_demo") return
        val uri = Uri.parse("https://www.youtube.com/watch?v=${reel.trailerKey}")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    LaunchedEffect(showSwipeFeedback) {
        if (showSwipeFeedback) {
            delay(1800)
            showSwipeFeedback = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(reel.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (totalDragX > 60f) {
                            onSwipeRightBothSaved()
                            showSwipeFeedback = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        totalDragX = 0f
                    },
                    onDragCancel = { totalDragX = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        totalDragX += dragAmount
                    },
                )
            },
    ) {
        // Tam Ekran Arka Plan Posteri/Backdrop
        if (reel.backdropUrl != null || reel.posterUrl != null) {
            AsyncImage(
                model = reel.backdropUrl ?: reel.posterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(Modifier.fillMaxSize().background(SurfaceVariant))
        }

        // Sinematik Karartma Gradienti
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.95f),
                        ),
                    ),
                ),
        )

        // Sağa Kaydırma (Swipe Right) İyileştirilmiş Bildirimi
        AnimatedVisibility(
            visible = showSwipeFeedback,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.9f))
                    .border(1.5.dp, Primary, RoundedCornerShape(18.dp))
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Favorite, contentDescription = null, tint = ErrorColor, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Filled.Bookmark, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Favorilere & İzleme Listesine Eklendi!",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        // ÖNDE DURAN EFSANEVİ FİLM/DİZİ KARTI (Center / Bottom Glassmorphic Card)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.92f)
                .padding(bottom = 36.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.Black.copy(alpha = 0.78f))
                .border(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Primary.copy(alpha = 0.8f),
                            Color(0xFFE040FB).copy(alpha = 0.4f),
                            Color.White.copy(alpha = 0.1f),
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .shadow(16.dp, RoundedCornerShape(28.dp))
                .padding(18.dp),
        ) {
            Column {
                // Üst Başlık ve Afiş Satırı
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (reel.posterUrl != null) {
                        Box(
                            modifier = Modifier
                                .size(width = 64.dp, height = 94.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(
                                    width = 1.5.dp,
                                    brush = Brush.linearGradient(listOf(Primary, Color(0xFFE040FB))),
                                    shape = RoundedCornerShape(14.dp),
                                )
                                .shadow(12.dp, RoundedCornerShape(14.dp)),
                        ) {
                            AsyncImage(
                                model = reel.posterUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = reel.title,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 26.sp,
                        )

                        Spacer(Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // TMDB Puan Rozeti
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GoldStarColor.copy(alpha = 0.25f))
                                    .border(0.5.dp, GoldStarColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Star, contentDescription = null, tint = GoldStarColor, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "${reel.rating}",
                                        color = GoldStarColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }

                            Spacer(Modifier.width(10.dp))

                            // Yıl & Tür Etiketi
                            reel.year?.let {
                                Text(text = "$it • ", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Primary.copy(alpha = 0.25f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = if (reel.mediaType == "tv") "Dizi" else "Film",
                                    color = Primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }

                // Tür Rozetleri Satırı
                if (reel.genres.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        reel.genres.take(3).forEach { genre ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 11.dp, vertical = 5.dp),
                            ) {
                                Text(genre, color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                // Açıklama Metni
                if (reel.overview.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = reel.overview,
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        maxLines = if (isExpandedOverview) 8 else 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { isExpandedOverview = !isExpandedOverview },
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Alt Aksiyon Satırı (YouTube Fragman Butonu + Hızlı Aksiyon İkonları)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    // YouTube Fragmanı İzle Butonu
                    if (reel.trailerKey.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brush.horizontalGradient(YouTubeRedGradient))
                                .clickable { playYouTubeTrailer() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Fragmanı İzle",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(15.dp),
                                )
                            }
                        }

                        Spacer(Modifier.width(10.dp))
                    }

                    // Hızlı Aksiyon İkon Dock'u
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Favori
                        CircleActionIconButton(
                            icon = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            tint = if (isFavorite) ErrorColor else Color.White,
                            onClick = onToggleFavorite,
                        )

                        // Kaydet
                        CircleActionIconButton(
                            icon = if (isInWatchlist) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            tint = if (isInWatchlist) Primary else Color.White,
                            onClick = onToggleWatchlist,
                        )

                        // Paylaş
                        CircleActionIconButton(
                            icon = Icons.AutoMirrored.Filled.Send,
                            tint = Color.White,
                            onClick = onShare,
                        )

                        // Detay
                        CircleActionIconButton(
                            icon = Icons.Filled.Info,
                            tint = Color.White,
                            onClick = onOpenDetail,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CircleActionIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.15f))
            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}
