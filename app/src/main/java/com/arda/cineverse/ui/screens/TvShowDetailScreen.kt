package com.arda.cineverse.ui.screens

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.compose.runtime.LaunchedEffect
import com.arda.cineverse.R
import com.arda.cineverse.data.model.Comment
import com.arda.cineverse.data.model.Movie
import com.arda.cineverse.data.model.SavedMovie
import com.arda.cineverse.data.model.TvShow
import com.arda.cineverse.data.repository.CommentRepository
import com.arda.cineverse.data.repository.RecommendationRepository
import com.arda.cineverse.data.repository.UserListRepository
import com.arda.cineverse.ui.components.CastMemberChip
import com.arda.cineverse.ui.components.CastSpotlightDialog
import com.arda.cineverse.ui.components.ClearOfflineMessageAfterDelay
import com.arda.cineverse.ui.components.CommentInputBox
import com.arda.cineverse.ui.components.CommentItem
import com.arda.cineverse.ui.components.CVGradientButton
import com.arda.cineverse.ui.components.ExpandableOverview
import com.arda.cineverse.ui.components.MovieDetailStickyBar
import com.arda.cineverse.ui.components.OfflineActionSnackbar
import com.arda.cineverse.ui.components.PopularMovieCard
import com.arda.cineverse.ui.components.RatingDistributionBar
import com.arda.cineverse.ui.components.RecommendShareBannerCard
import com.arda.cineverse.ui.components.RecommendShareSheet
import com.arda.cineverse.ui.components.TvSeasonsAndEpisodesSection
import com.arda.cineverse.ui.components.rememberOfflineWriteMessageState
import com.arda.cineverse.ui.components.ReplyInputBox
import com.arda.cineverse.ui.theme.*
import com.arda.cineverse.viewmodel.CommentViewModel
import com.arda.cineverse.viewmodel.CommentViewModelFactory
import com.arda.cineverse.viewmodel.RecommendShareViewModel
import com.arda.cineverse.viewmodel.TvShowDetailViewModel
import kotlinx.coroutines.launch

private val StarColorTvDetail = Color(0xFFFFC857)

// Backdrop üzerindeki ikon düğmeleri her zaman koyu bir karartmanın (Color.Black
// alpha 0.4) üzerinde durur — bu karartma temadan bağımsız hep koyu kalıyor, o
// yüzden ikon tint'i de temaya göre değişen OnSurface yerine SABİT açık renk
// olmalı (aksi halde Açık temada OnSurface neredeyse siyaha döner ve ikonlar
// koyu daire üzerinde görünmez olur). Aynı mantık FeaturedMovieBanner'da da var.
private val OverlayIconColor = Color(0xFFFFFFFF)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TvShowDetailScreen(
    tvId: Int,
    onBack: () -> Unit = {},
    onGoHome: () -> Unit = {},
    onTvShowClick: (Int) -> Unit = {},
    tvShowDetailViewModel: TvShowDetailViewModel = hiltViewModel(),
    commentViewModel: CommentViewModel = viewModel(
        factory = CommentViewModelFactory(
            movieId = tvId,
            repository = CommentRepository(
                commentsRootCollection = "tv_shows",
                ratingsRootCollection = "tv_show_ratings",
            ),
        ),
    ),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val detailState by tvShowDetailViewModel.uiState.collectAsState()
    val commentState by commentViewModel.uiState.collectAsState()

    val userListRepository = remember { UserListRepository.default() }
    val recommendationRepository = remember { RecommendationRepository.default() }

    var favoriteTvIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    val offlineMessageState = rememberOfflineWriteMessageState()

    LaunchedEffect(Unit) {
        val favorites = userListRepository.getFavorites().getOrDefault(emptyList())
        favoriteTvIds = favorites.filter { it.mediaType == "tv" }.map { it.id }.toSet()
    }

    fun toggleSimilarTvFavorite(similar: TvShow) {
        val isFav = similar.id in favoriteTvIds
        favoriteTvIds = if (isFav) favoriteTvIds - similar.id else favoriteTvIds + similar.id
        fun revert() {
            favoriteTvIds = if (isFav) favoriteTvIds + similar.id else favoriteTvIds - similar.id
        }
        scope.launch {
            val result = if (isFav) {
                userListRepository.removeFavorite(similar.id, mediaType = "tv")
            } else {
                userListRepository.addFavorite(
                    SavedMovie(
                        id = similar.id,
                        title = similar.name,
                        posterUrl = similar.posterUrl,
                        rating = similar.rating,
                        year = similar.year,
                        genreIds = similar.genreIds,
                        mediaType = "tv",
                    ),
                )
            }
            result.fold(
                onSuccess = {
                    if (isFav) recommendationRepository.removeTvFavoriteSignal(similar.id) else recommendationRepository.recordTvFavorite(similar.id, similar.genreIds)
                },
                onFailure = { error -> offlineMessageState.handle(error) { revert() } },
            )
        }
    }

    var editingComment by remember { mutableStateOf<Comment?>(null) }
    var commentPendingDelete by remember { mutableStateOf<Comment?>(null) }
    var replyingToComment by remember { mutableStateOf<Comment?>(null) }
    var selectedCast by remember { mutableStateOf<com.arda.cineverse.data.model.CastMember?>(null) }
    val commentBoxRequester = remember { BringIntoViewRequester() }

    val recommendShareViewModel: RecommendShareViewModel = hiltViewModel()
    val shareState by recommendShareViewModel.uiState.collectAsState()
    var showShareSheet by remember { mutableStateOf(false) }
    var shareSentMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(shareState.sentCount) {
        val count = shareState.sentCount ?: return@LaunchedEffect
        showShareSheet = false
        // LaunchedEffect'in gövdesi @Composable değil (suspend lambda), bu
        // yüzden stringResource() yerine context.getString() kullanılıyor.
        shareSentMessage = if (count == 1) {
            context.getString(R.string.share_sent_one)
        } else {
            context.getString(R.string.share_sent_many, count)
        }
    }

    fun playTrailer(key: String?) {
        if (key.isNullOrBlank()) return
        val uri = "https://www.youtube.com/watch?v=$key".toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("TvShowDetailScreen", "Could not launch trailer intent", e)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        when {
            detailState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            detailState.tvShow == null -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(12.dp)
                            .align(Alignment.TopStart),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = OnSurface)
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            detailState.errorMessage ?: stringResource(R.string.tv_detail_not_found),
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(16.dp))
                        CVGradientButton(text = stringResource(R.string.common_retry), onClick = { tvShowDetailViewModel.load() })
                    }
                }
            }
            else -> {
                val tvShow = detailState.tvShow!!
                val seasonsEpisodesLabel = listOf(tvShow.seasonsLabel, tvShow.episodesLabel)
                    .filter { it.isNotBlank() }
                    .joinToString(" / ")

                Column(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(420.dp)) {
                                if (tvShow.backdropUrl != null) {
                                    AsyncImage(
                                        model = tvShow.backdropUrl,
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
                                            onClick = { tvShowDetailViewModel.toggleFavorite() },
                                        )
                                        CircleIconButton(
                                            icon = if (detailState.isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                            tint = if (detailState.isSaved) Primary else OverlayIconColor,
                                            onClick = { tvShowDetailViewModel.toggleWatchlist() },
                                        )
                                        CircleIconButton(
                                            icon = Icons.Filled.Share,
                                            onClick = {
                                                recommendShareViewModel.reset()
                                                showShareSheet = true
                                            },
                                        )
                                    }
                                }

                                if (tvShow.trailerKey != null) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.5f))
                                            .clickable { playTrailer(tvShow.trailerKey) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.movie_detail_play_trailer_cd), tint = Color.White, modifier = Modifier.size(32.dp))
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
                                                stringResource(R.string.movie_detail_offline_limited),
                                                color = ErrorColor,
                                                style = MaterialTheme.typography.labelSmall,
                                            )
                                        }
                                        Spacer(Modifier.height(8.dp))
                                    }
                                    Text(tvShow.name, color = OnSurface, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Star, contentDescription = null, tint = StarColorTvDetail, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("${tvShow.tmdbRating}", color = OnSurface, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Spacer(Modifier.width(8.dp))
                                        Text("${tvShow.year ?: ""}", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                                        if (seasonsEpisodesLabel.isNotEmpty()) {
                                            Spacer(Modifier.width(8.dp))
                                            Text(seasonsEpisodesLabel, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                    if (tvShow.genres.isNotEmpty()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(tvShow.genres.take(3).joinToString(" / "), color = TextSecondary, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                    }
                                }
                            }
                        }

                        item {
                            Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                                CVGradientButton(
                                    text = stringResource(R.string.movie_detail_watch_trailer),
                                    icon = Icons.Filled.PlayArrow,
                                    onClick = { playTrailer(tvShow.trailerKey) },
                                    enabled = tvShow.trailerKey != null,
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(1.dp, Primary, RoundedCornerShape(14.dp))
                                        .clickable { tvShowDetailViewModel.toggleWatchlist() }
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = if (detailState.isSaved) Icons.Filled.Check else Icons.Filled.Add,
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        if (detailState.isSaved) stringResource(R.string.movie_detail_in_watchlist) else stringResource(R.string.movie_detail_add_to_watchlist),
                                        color = Primary,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }

                        item {
                            Column(Modifier.padding(horizontal = 20.dp)) {
                                Text(stringResource(R.string.movie_detail_overview_title), color = OnSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                ExpandableOverview(text = tvShow.overview.ifBlank { stringResource(R.string.tv_detail_no_overview) })
                                tvShow.createdBy?.let {
                                    Spacer(Modifier.height(10.dp))
                                    Text(stringResource(R.string.tv_detail_created_by, it), color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        item {
                            Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                                RecommendShareBannerCard(
                                    onRecommendClick = {
                                        recommendShareViewModel.reset()
                                        showShareSheet = true
                                    },
                                )
                            }
                        }

                        if (tvShow.seasons.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(12.dp))
                                TvSeasonsAndEpisodesSection(
                                    seasons = tvShow.seasons,
                                    selectedSeasonNumber = detailState.selectedSeasonNumber,
                                    seasonDetail = detailState.selectedSeasonDetail,
                                    isLoadingSeason = detailState.isLoadingSeasonDetail,
                                    watchedEpisodesMap = detailState.watchedEpisodesMap,
                                    onSeasonSelect = { seasonNum -> tvShowDetailViewModel.selectSeason(seasonNum) },
                                    onEpisodeToggle = { seasonNum, epNum -> tvShowDetailViewModel.toggleEpisodeWatched(seasonNum, epNum) },
                                    onSeasonToggleAll = { seasonNum, episodes -> tvShowDetailViewModel.toggleSeasonWatched(seasonNum, episodes) },
                                )
                            }
                        }

                        if (tvShow.cast.isNotEmpty()) {
                            item {
                                Column(Modifier.padding(top = 20.dp)) {
                                    Text(
                                        stringResource(R.string.movie_detail_cast_title),
                                        color = OnSurface,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 20.dp),
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                        items(tvShow.cast, key = { it.id }) { cast ->
                                            CastMemberChip(cast = cast, onClick = { selectedCast = cast })
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                                Text(stringResource(R.string.movie_detail_user_rating_title), color = OnSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(12.dp))
                                if (commentState.comments.isEmpty()) {
                                    Text(stringResource(R.string.movie_detail_no_ratings_yet), color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(90.dp)) {
                                            Text("${commentState.averageRating}", color = OnSurface, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                                            Row {
                                                repeat(5) { i ->
                                                    Icon(
                                                        if (i < commentState.averageRating.toInt()) Icons.Filled.Star else Icons.Filled.StarBorder,
                                                        contentDescription = null,
                                                        tint = StarColorTvDetail,
                                                        modifier = Modifier.size(14.dp),
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Text(stringResource(R.string.detail_vote_count, commentState.comments.count { it.replyToCommentId == null }), color = TextSecondary, style = MaterialTheme.typography.labelSmall)
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
                                    stringResource(R.string.detail_rate_now),
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
                                stringResource(R.string.detail_comments_title, commentState.comments.count { it.replyToCommentId == null }),
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
                                        submitLabel = stringResource(R.string.common_update),
                                        onSubmit = { text, rating, spoiler ->
                                            commentViewModel.editComment(comment.id, text, rating, spoiler, tvShow.name, tvShow.posterUrl, tvShow.year, tvShow.genreIds)
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
                                            commentViewModel.submitReply(comment.id, comment.userId, text, tvShow.name)
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
                                Text(stringResource(R.string.detail_write_comment_title), color = OnSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(10.dp))
                                CommentInputBox(
                                    onSubmit = { text, rating, spoiler ->
                                        commentViewModel.submitComment(text, rating, spoiler, tvShow.name, tvShow.posterUrl, tvShow.year, tvShow.genreIds)
                                    },
                                )
                            }
                        }

                        if (tvShow.similarShows.isNotEmpty()) {
                            item {
                                Column(Modifier.padding(top = 12.dp)) {
                                    Text(
                                        stringResource(R.string.tv_detail_similar_title),
                                        color = OnSurface,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 20.dp),
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        items(tvShow.similarShows, key = { it.id }) { similar ->
                                            PopularMovieCard(
                                                movie = Movie(
                                                    id = similar.id,
                                                    title = similar.name,
                                                    posterUrl = similar.posterUrl,
                                                    rating = similar.rating,
                                                    year = similar.year,
                                                    genreIds = similar.genreIds,
                                                    mediaType = "tv",
                                                    isFavorite = similar.id in favoriteTvIds,
                                                ),
                                                onClick = { onTvShowClick(similar.id) },
                                                onFavoriteClick = { toggleSimilarTvFavorite(similar) },
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
                        onFavoriteClick = { tvShowDetailViewModel.toggleFavorite() },
                        onSaveClick = { tvShowDetailViewModel.toggleWatchlist() },
                        onRateClick = { scope.launch { commentBoxRequester.bringIntoView() } },
                        modifier = Modifier.navigationBarsPadding(),
                    )
                }
            }
        }

        ClearOfflineMessageAfterDelay(detailState.offlineMessage) { tvShowDetailViewModel.clearOfflineMessage() }
        ClearOfflineMessageAfterDelay(commentState.mutationErrorMessage) { commentViewModel.clearMutationError() }
        ClearOfflineMessageAfterDelay(shareSentMessage) { shareSentMessage = null }
        (detailState.offlineMessage ?: offlineMessageState.message ?: commentState.mutationErrorMessage ?: shareSentMessage)?.let {
            OfflineActionSnackbar(
                message = it,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 88.dp)
                    .fillMaxWidth(),
            )
        }
    }

    if (showShareSheet) {
        detailState.tvShow?.let { tvShow ->
            RecommendShareSheet(
                state = shareState,
                mediaTitle = tvShow.name,
                posterUrl = tvShow.posterUrl,
                isTvShow = true,
                onToggleFriend = recommendShareViewModel::toggleFriend,
                onNoteChange = recommendShareViewModel::onNoteChange,
                onSend = {
                    recommendShareViewModel.send(
                        mediaId = tvId,
                        mediaType = "tv",
                        mediaTitle = tvShow.name,
                        posterUrl = tvShow.posterUrl,
                    )
                },
                onDismiss = { showShareSheet = false },
            )
        }
    }

    selectedCast?.let { cast ->
        CastSpotlightDialog(
            cast = cast,
            onDismiss = { selectedCast = null },
        )
    }

    commentPendingDelete?.let { comment ->
        val currentTvShow = detailState.tvShow
        AlertDialog(
            onDismissRequest = { commentPendingDelete = null },
            title = { Text(stringResource(R.string.detail_delete_comment_title)) },
            text = { Text(stringResource(R.string.detail_delete_comment_body)) },
            confirmButton = {
                TextButton(onClick = {
                    if (currentTvShow != null) {
                        commentViewModel.deleteComment(comment.id, currentTvShow.name, currentTvShow.posterUrl, currentTvShow.year, currentTvShow.genreIds)
                    }
                    commentPendingDelete = null
                }) { Text(stringResource(R.string.comment_delete), color = ErrorColor) }
            },
            dismissButton = {
                TextButton(onClick = { commentPendingDelete = null }) { Text(stringResource(R.string.common_cancel)) }
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
private fun SimilarTvShowCard(tvShow: TvShow, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(128.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Surface)
            .clickable { onClick() }
            .padding(bottom = 8.dp),
    ) {
        AsyncImage(
            model = tvShow.posterUrl,
            contentDescription = tvShow.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .background(SurfaceVariant),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            tvShow.name,
            color = OnSurface,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = StarColorTvDetail, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text("${tvShow.rating}", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
        }
    }
}
