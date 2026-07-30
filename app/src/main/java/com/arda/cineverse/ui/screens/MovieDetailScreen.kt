package com.arda.cineverse.ui.screens

import android.content.Intent
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.arda.cineverse.data.model.Comment
import com.arda.cineverse.ui.components.*
import com.arda.cineverse.ui.theme.*
import com.arda.cineverse.viewmodel.CommentViewModel
import com.arda.cineverse.viewmodel.CommentViewModelFactory
import com.arda.cineverse.viewmodel.MovieDetailViewModel
import com.arda.cineverse.viewmodel.MovieDetailViewModelFactory
import kotlinx.coroutines.launch

import androidx.compose.runtime.LaunchedEffect
import com.arda.cineverse.data.model.Movie
import com.arda.cineverse.data.model.SavedMovie
import com.arda.cineverse.data.repository.RecommendationRepository
import com.arda.cineverse.data.repository.UserListRepository

private val StarColorDetail = Color(0xFFFFC857)

// Backdrop üzerindeki ikon düğmeleri her zaman koyu bir karartmanın (Color.Black
// alpha 0.4) üzerinde durur — bu karartma temadan bağımsız hep koyu kalıyor, o
// yüzden ikon tint'i de temaya göre değişen OnSurface yerine SABİT açık renk
// olmalı (aksi halde Açık temada OnSurface neredeyse siyaha döner ve ikonlar
// koyu daire üzerinde görünmez olur). Aynı mantık FeaturedMovieBanner'da da var.
private val OverlayIconColor = Color(0xFFFFFFFF)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MovieDetailScreen(
    movieId: Int,
    onBack: () -> Unit = {},
    onGoHome: () -> Unit = {},
    onMovieClick: (Int) -> Unit = {},
    movieDetailViewModel: MovieDetailViewModel = viewModel(factory = MovieDetailViewModelFactory(movieId)),
    commentViewModel: CommentViewModel = viewModel(factory = CommentViewModelFactory(movieId)),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val detailState by movieDetailViewModel.uiState.collectAsState()
    val commentState by commentViewModel.uiState.collectAsState()

    val userListRepository = remember { UserListRepository() }
    val recommendationRepository = remember { RecommendationRepository() }

    var favoriteMovieIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    LaunchedEffect(Unit) {
        val favorites = userListRepository.getFavorites().getOrDefault(emptyList())
        favoriteMovieIds = favorites.map { it.id }.toSet()
    }

    fun toggleSimilarFavorite(similar: Movie) {
        val isFav = similar.id in favoriteMovieIds
        favoriteMovieIds = if (isFav) favoriteMovieIds - similar.id else favoriteMovieIds + similar.id
        scope.launch {
            if (isFav) {
                userListRepository.removeFavorite(similar.id, mediaType = "movie")
                recommendationRepository.removeFavoriteSignal(similar.id)
            } else {
                userListRepository.addFavorite(
                    SavedMovie(
                        id = similar.id,
                        title = similar.title,
                        posterUrl = similar.posterUrl,
                        rating = similar.rating,
                        year = similar.year,
                        genreIds = similar.genreIds,
                        mediaType = "movie",
                    ),
                )
                recommendationRepository.recordFavorite(similar.id, similar.genreIds)
            }
        }
    }

    var editingComment by remember { mutableStateOf<Comment?>(null) }
    var commentPendingDelete by remember { mutableStateOf<Comment?>(null) }
    var replyingToComment by remember { mutableStateOf<Comment?>(null) }
    var showTrailerPlayer by remember { mutableStateOf(false) }
    val commentBoxRequester = remember { BringIntoViewRequester() }

    fun playTrailer(key: String?) {
        if (key.isNullOrBlank()) return
        val uri = "https://www.youtube.com/watch?v=$key".toUri()
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    fun openInYouTubeApp(key: String) {
        val uri = "https://www.youtube.com/watch?v=$key".toUri()
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    fun shareMovie(title: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "CineVerse'de \"$title\" filmine göz at!")
        }
        context.startActivity(Intent.createChooser(sendIntent, "Paylaş"))
    }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        when {
            detailState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            detailState.movie == null -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(12.dp)
                            .align(Alignment.TopStart),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = OnSurface)
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            detailState.errorMessage ?: "Film bulunamadı",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(16.dp))
                        CVGradientButton(text = "Tekrar Dene", onClick = { movieDetailViewModel.load() })
                    }
                }
            }
            else -> {
                val movie = detailState.movie!!
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {

                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(420.dp)) {
                                if (movie.backdropUrl != null) {
                                    AsyncImage(
                                        model = movie.backdropUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    Box(Modifier.fillMaxSize().background(SurfaceVariant))
                                }
                                Box(
                                    modifier = Modifier.fillMaxSize().background(
                                        brush = Brush.verticalGradient(
                                            listOf(Color.Transparent, Background.copy(alpha = 0.5f), Background),
                                        ),
                                    ),
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.TopCenter)
                                        .statusBarsPadding()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    CircleIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, onClick = onBack)
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        CircleIconButton(
                                            icon = if (detailState.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                            tint = if (detailState.isFavorite) ErrorColor else OverlayIconColor,
                                            onClick = { movieDetailViewModel.toggleFavorite() },
                                        )
                                        CircleIconButton(
                                            icon = if (detailState.isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                            tint = if (detailState.isSaved) Primary else OverlayIconColor,
                                            onClick = { movieDetailViewModel.toggleWatchlist() },
                                        )
                                        CircleIconButton(icon = Icons.Filled.Share, onClick = { shareMovie(movie.title) })
                                    }
                                }

                                if (movie.trailerKey != null) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.5f))
                                            .clickable { playTrailer(movie.trailerKey) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = "Fragmanı oynat", tint = Color.White, modifier = Modifier.size(32.dp))
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(horizontal = 20.dp, vertical = 16.dp),
                                ) {
                                    if (detailState.isOfflineFallback) {
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(ErrorColor.copy(alpha = 0.16f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                Icons.Filled.CloudOff,
                                                contentDescription = null,
                                                tint = ErrorColor,
                                                modifier = Modifier.size(12.dp),
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                "Çevrimdışı • Sınırlı bilgi gösteriliyor",
                                                color = ErrorColor,
                                                style = MaterialTheme.typography.labelSmall,
                                            )
                                        }
                                        Spacer(Modifier.height(8.dp))
                                    }
                                    Text(movie.title, color = OnSurface, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Star, contentDescription = null, tint = StarColorDetail, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("${movie.tmdbRating}", color = OnSurface, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Spacer(Modifier.width(8.dp))
                                        Text("${movie.year ?: ""}", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                                        if (movie.durationLabel.isNotEmpty()) {
                                            Spacer(Modifier.width(8.dp))
                                            Text(movie.durationLabel, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                                        }
                                        if (movie.genres.isNotEmpty()) {
                                            Spacer(Modifier.width(8.dp))
                                            Text(movie.genres.take(3).joinToString(" • "), color = TextSecondary, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                                CVGradientButton(
                                    text = "Fragmanı İzle",
                                    onClick = { playTrailer(movie.trailerKey) },
                                    enabled = movie.trailerKey != null,
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(1.dp, Primary, RoundedCornerShape(14.dp))
                                        .clickable { movieDetailViewModel.toggleWatchlist() }
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        if (detailState.isSaved) "İzleme Listemde" else "İzleme Listeme Ekle",
                                        color = Primary,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }

                        item {
                            Column(Modifier.padding(horizontal = 20.dp)) {
                                Text("Özet", color = OnSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                ExpandableOverview(text = movie.overview.ifBlank { "Bu film için özet bulunamadı." })
                                movie.director?.let {
                                    Spacer(Modifier.height(10.dp))
                                    Text("Yönetmen: $it", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        if (movie.cast.isNotEmpty()) {
                            item {
                                Column(Modifier.padding(top = 20.dp)) {
                                    Text(
                                        "Oyuncular",
                                        color = OnSurface,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 20.dp),
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                        items(movie.cast, key = { it.id }) { cast -> CastMemberChip(cast) }
                                    }
                                }
                            }
                        }

                        item {
                            Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                                Text("Kullanıcı Puanı", color = OnSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(12.dp))
                                if (commentState.comments.isEmpty()) {
                                    Text("Henüz puan verilmedi, ilk puanı sen ver!", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(90.dp)) {
                                            Text("${commentState.averageRating}", color = OnSurface, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                                            Row {
                                                repeat(5) { i ->
                                                    Icon(
                                                        if (i < commentState.averageRating.toInt()) Icons.Filled.Star else Icons.Filled.StarBorder,
                                                        contentDescription = null,
                                                        tint = StarColorDetail,
                                                        modifier = Modifier.size(14.dp),
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Text("${commentState.comments.count { it.replyToCommentId == null }} oy", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                        }
                                        Spacer(Modifier.width(16.dp))
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            for (star in 5 downTo 1) {
                                                RatingDistributionBar(
                                                    star = star,
                                                    count = commentState.ratingCounts[star] ?: 0,
                                                    total = commentState.ratingCounts.values.sum(),
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "Puanını ver",
                                    color = Primary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable {
                                        scope.launch { commentBoxRequester.bringIntoView() }
                                    },
                                )
                            }
                        }

                        item {
                            Text(
                                "Yorumlar (${commentState.comments.count { it.replyToCommentId == null }})",
                                color = OnSurface,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 20.dp),
                            )
                        }

                        val topLevelComments = commentState.comments.filter { it.replyToCommentId == null }
                        val repliesByParent = commentState.comments
                            .filter { it.replyToCommentId != null }
                            .groupBy { it.replyToCommentId }

                        items(topLevelComments, key = { it.id }) { comment ->
                            val isOwner = comment.userId == commentViewModel.currentUserId
                            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                                if (editingComment?.id == comment.id) {
                                    CommentInputBox(
                                        initialText = comment.text,
                                        initialRating = comment.rating,
                                        initialSpoiler = comment.isSpoiler,
                                        submitLabel = "Güncelle",
                                        onSubmit = { text, rating, spoiler ->
                                            commentViewModel.editComment(comment.id, text, rating, spoiler, movie.title, movie.posterUrl, movie.year, movie.genreIds)
                                            editingComment = null
                                        },
                                        onCancel = { editingComment = null },
                                    )
                                } else {
                                    CommentItem(
                                        comment = comment,
                                        isOwner = isOwner,
                                        onEditClick = { editingComment = comment },
                                        onDeleteClick = { commentPendingDelete = comment },
                                        onReplyClick = { replyingToComment = comment },
                                    )
                                }

                                val replies = repliesByParent[comment.id].orEmpty()
                                if (replies.isNotEmpty()) {
                                    Column(modifier = Modifier.padding(start = 30.dp)) {
                                        replies.forEach { reply ->
                                            CommentItem(
                                                comment = reply,
                                                isOwner = reply.userId == commentViewModel.currentUserId,
                                                isReply = true,
                                                onEditClick = {},
                                                onDeleteClick = { commentPendingDelete = reply },
                                            )
                                        }
                                    }
                                }

                                if (replyingToComment?.id == comment.id) {
                                    ReplyInputBox(
                                        onSubmit = { text ->
                                            commentViewModel.submitReply(comment.id, comment.userId, text, movie.title)
                                            replyingToComment = null
                                        },
                                        onCancel = { replyingToComment = null },
                                        modifier = Modifier.padding(start = 30.dp, top = 6.dp),
                                    )
                                }
                            }
                        }

                        item {
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 20.dp, vertical = 12.dp)
                                    .bringIntoViewRequester(commentBoxRequester),
                            ) {
                                Text("Yorum Yaz", color = OnSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(10.dp))
                                CommentInputBox(
                                    onSubmit = { text, rating, spoiler ->
                                        commentViewModel.submitComment(text, rating, spoiler, movie.title, movie.posterUrl, movie.year, movie.genreIds)
                                    },
                                )
                            }
                        }

                        if (movie.similarMovies.isNotEmpty()) {
                            item {
                                Column(Modifier.padding(top = 12.dp)) {
                                    Text(
                                        "Benzer Filmler",
                                        color = OnSurface,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 20.dp),
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        items(movie.similarMovies, key = { it.id }) { similar ->
                                            PopularMovieCard(
                                                movie = similar.copy(isFavorite = similar.id in favoriteMovieIds),
                                                onClick = { onMovieClick(similar.id) },
                                                onFavoriteClick = { toggleSimilarFavorite(similar) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    MovieDetailStickyBar(
                        isFavorite = detailState.isFavorite,
                        isSaved = detailState.isSaved,
                        onHomeClick = onGoHome,
                        onFavoriteClick = { movieDetailViewModel.toggleFavorite() },
                        onSaveClick = { movieDetailViewModel.toggleWatchlist() },
                        onRateClick = { scope.launch { commentBoxRequester.bringIntoView() } },
                        modifier = Modifier.navigationBarsPadding(),
                    )
                }
            }
        }

        val currentMovie = detailState.movie
        if (showTrailerPlayer && currentMovie?.trailerKey != null) {
            TrailerPlayerOverlay(
                videoKey = currentMovie.trailerKey,
                onClose = { showTrailerPlayer = false },
                onOpenInYouTube = { openInYouTubeApp(currentMovie.trailerKey) },
            )
        }
    }

    commentPendingDelete?.let { comment ->
        val currentMovie = detailState.movie
        AlertDialog(
            onDismissRequest = { commentPendingDelete = null },
            title = { Text("Yorumu sil") },
            text = { Text("Bu yorumu silmek istediğinize emin misiniz?") },
            confirmButton = {
                TextButton(onClick = {
                    if (currentMovie != null) {
                        commentViewModel.deleteComment(comment.id, currentMovie.title, currentMovie.posterUrl, currentMovie.year, currentMovie.genreIds)
                    }
                    commentPendingDelete = null
                }) { Text("Sil", color = ErrorColor) }
            },
            dismissButton = {
                TextButton(onClick = { commentPendingDelete = null }) { Text("Vazgeç") }
            },
        )
    }
}

@Composable
private fun CircleIconButton(icon: ImageVector, tint: Color = OverlayIconColor, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun TrailerPlayerOverlay(
    videoKey: String,
    onClose: () -> Unit,
    onOpenInYouTube: () -> Unit,
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose { webViewRef?.destroy() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.97f))
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                CircleIconButton(icon = Icons.Filled.Close, onClick = onClose)
            }

            AndroidView(
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                factory = { ctx ->
                    WebView(ctx).apply {
                        setLayerType(WebView.LAYER_TYPE_SOFTWARE, null)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        webChromeClient = object : WebChromeClient() {
                            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                                Log.d(
                                    "CVTrailerWebView",
                                    "JS console [${message.messageLevel()}] ${message.message()} " +
                                            "(${message.sourceId()}:${message.lineNumber()})",
                                )
                                return true
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?,
                            ) {
                                super.onReceivedError(view, request, error)
                                Log.e(
                                    "CVTrailerWebView",
                                    "onReceivedError: code=${error?.errorCode} desc=${error?.description} url=${request?.url}",
                                )
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                Log.d("CVTrailerWebView", "onPageFinished: $url")
                            }
                        }
                        loadUrl("https://www.youtube.com/embed/$videoKey?autoplay=1&playsinline=1&modestbranding=1&rel=0")
                        webViewRef = this
                    }
                },
            )

            Spacer(Modifier.height(20.dp))
            Text(
                "YouTube uygulamasında aç",
                color = Primary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable { onOpenInYouTube() },
            )
        }
    }
}