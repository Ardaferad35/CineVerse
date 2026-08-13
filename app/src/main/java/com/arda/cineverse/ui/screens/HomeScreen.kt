package com.arda.cineverse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Recommend
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arda.cineverse.R
import com.arda.cineverse.ui.components.*
import com.arda.cineverse.data.model.Category
import com.arda.cineverse.data.remote.toFeaturedMovie
import com.arda.cineverse.ui.components.CVBottomNavBar
import com.arda.cineverse.ui.components.CVGradientButton
import com.arda.cineverse.ui.components.CategoryChip
import com.arda.cineverse.ui.components.FeaturedMovieBanner
import com.arda.cineverse.ui.components.HomeMode
import com.arda.cineverse.ui.components.HomeModeSelector
import com.arda.cineverse.ui.components.HomeSearchBar
import com.arda.cineverse.ui.components.HomeSectionHeader
import com.arda.cineverse.ui.components.HomeTopBar
import com.arda.cineverse.ui.components.OfflineStatusBanner
import com.arda.cineverse.ui.components.PopularMovieCard
import com.arda.cineverse.ui.components.SearchSuggestionsList
import com.arda.cineverse.ui.components.UpcomingMovieCard
import com.arda.cineverse.ui.components.categoryColor
import com.arda.cineverse.ui.components.categoryIcon
import com.arda.cineverse.ui.theme.Accent
import com.arda.cineverse.ui.theme.Background
import com.arda.cineverse.ui.theme.Primary
import com.arda.cineverse.ui.theme.Surface
import com.arda.cineverse.ui.theme.TextSecondary
import com.arda.cineverse.viewmodel.HomeViewModel
import com.arda.cineverse.viewmodel.NotificationViewModel

@Composable
fun HomeScreen(
    onMovieClick: (movieId: Int) -> Unit = {},
    onTvShowClick: (tvId: Int) -> Unit = {},
    onSeeAllClick: (section: String) -> Unit = {},
    /** Asistan sekmesi de buradan açılıyor; aktif Filmler/Diziler seçimi birlikte taşınıyor. */
    onAiSearchClick: (isTvMode: Boolean) -> Unit = {},
    onNavigateTab: (Int) -> Unit = {},
    onCategoryClick: (Category) -> Unit = {},
    onTvCategoryClick: (Category) -> Unit = {},
    onProfileClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onReelsClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel(),
) {
    var selectedCategoryId by remember { mutableStateOf("") }

    val uiState by homeViewModel.uiState.collectAsState()
    // Bir tek gerçek kaynak: seçili mod her zaman ViewModel'in isTvMode'undan
    // türetilir. Aksi halde (ör. TV detayına gidip Home'a geri dönünce) bu
    // composable yeniden oluşturulup remember state'i sıfırlanırken,
    // ViewModel (NavBackStackEntry'ye bağlı olduğu için) eski durumunu
    // koruyor ve seçici ile gösterilen içerik birbirinden kopuyordu.
    val selectedHomeMode = if (uiState.isTvMode) HomeMode.TV_SHOWS else HomeMode.MOVIES
    val unreadCount by notificationViewModel.unreadCount.collectAsState()

    // Arkadaşlar artık ayrı bir sayfaya gitmek yerine ana sayfanın üstünde
    // sağdan açılan bir panelde (bkz. FriendsPanel). Tam ekran "friends"
    // rotası duruyor — push bildirimi ve Profil girişi oraya gidiyor.
    var showFriendsPanel by rememberSaveable { mutableStateOf(false) }

    var quickPreviewData by remember { mutableStateOf<QuickPreviewData?>(null) }
    var showDiceDialog by remember { mutableStateOf(false) }
    var shareMediaId by remember { mutableStateOf<Int?>(null) }
    var shareMediaType by remember { mutableStateOf<String>("movie") }
    var shareMediaTitle by remember { mutableStateOf<String>("") }
    var shareMediaPosterUrl by remember { mutableStateOf<String?>(null) }
    var showShareSheet by remember { mutableStateOf(false) }

    val recommendShareViewModel: com.arda.cineverse.viewmodel.RecommendShareViewModel = hiltViewModel()
    val shareState by recommendShareViewModel.uiState.collectAsState()
    val context = LocalContext.current

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

    // Favoriler/İzleme Listesi/çevrimdışı mesajı ve TV "Yakında" bölümü artık
    // tamamen HomeViewModel'de yönetiliyor (Room gözlemi + iyimser toggle +
    // Firestore senkronu); ekran sadece state okuyup event yolluyor.
    val favoriteMovieIds = uiState.favoriteMovieIds
    val favoriteTvIds = uiState.favoriteTvIds
    val watchlistMovieIds = uiState.watchlistMovieIds
    val watchlistTvIds = uiState.watchlistTvIds
    val upcomingTvShows = uiState.upcomingTvShows
    val offlineActionMessage = uiState.offlineActionMessage

    LaunchedEffect(uiState.categories, selectedHomeMode) {
        if (uiState.categories.isNotEmpty() && uiState.categories.none { it.id == selectedCategoryId }) {
            selectedCategoryId = uiState.categories.first().id
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Spacer(Modifier.height(12.dp))
            HomeTopBar(
                onProfileClick = onProfileClick,
                onNotificationsClick = onNotificationsClick,
                onFriendsClick = { showFriendsPanel = true },
                onReelsClick = onReelsClick,
                unreadNotificationCount = unreadCount,
            )
            Spacer(Modifier.height(12.dp))
            HomeModeSelector(
                selected = selectedHomeMode,
                onSelectedChange = { mode ->
                    if (selectedHomeMode != mode) {
                        selectedCategoryId = ""
                        when (mode) {
                            HomeMode.MOVIES -> homeViewModel.loadMovies()
                            HomeMode.TV_SHOWS -> homeViewModel.loadTvShows()
                        }
                    }
                },
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(12.dp))
            HomeSearchBar(
                value = uiState.searchQuery,
                onValueChange = { homeViewModel.onSearchQueryChange(it) },
                onAiClick = { onAiSearchClick(uiState.isTvMode) },
                onClear = { homeViewModel.clearSearch() },
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(12.dp))
            OfflineStatusBanner(
                isOffline = uiState.isOffline,
                lastSyncedAt = uiState.lastSyncedAt,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(16.dp))

            when {
                uiState.isLoading -> {
                    HomeScreenShimmer()
                }
                uiState.errorMessage != null -> {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(uiState.errorMessage!!, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(16.dp))
                        CVGradientButton(
                            text = stringResource(R.string.common_retry),
                            onClick = {
                                if (selectedHomeMode == HomeMode.TV_SHOWS) {
                                    homeViewModel.loadTvShows()
                                } else {
                                    homeViewModel.loadMovies()
                                }
                            },
                        )
                    }
                }
                uiState.searchQuery.isNotBlank() -> {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
                    ) {
                        when {
                            uiState.isSearching -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Surface)
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    CircularProgressIndicator(color = Primary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(10.dp))
                                    Text(stringResource(R.string.home_searching), color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            uiState.searchSuggestions.isNotEmpty() -> {
                                SearchSuggestionsList(
                                    suggestions = uiState.searchSuggestions,
                                    onMovieClick = onMovieClick,
                                    onTvShowClick = onTvShowClick,
                                )
                            }
                            else -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Surface)
                                        .padding(16.dp),
                                ) {
                                    Text(stringResource(R.string.home_no_results), color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 110.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        if (uiState.isTvMode) {
                            val featuredTv = uiState.featuredTvShow
                            if (featuredTv != null) {
                                item {
                                    FeaturedMovieBanner(
                                        movie = featuredTv.toFeaturedMovie(),
                                        label = stringResource(R.string.home_tv_of_the_day),
                                        onDetailsClick = { onTvShowClick(featuredTv.id) },
                                        onAddToListClick = { homeViewModel.toggleFeaturedTvWatchlist(featuredTv) },
                                        onLongClick = {
                                            quickPreviewData = QuickPreviewData(
                                                id = featuredTv.id,
                                                title = featuredTv.title,
                                                overview = featuredTv.description,
                                                posterUrl = featuredTv.posterUrl,
                                                backdropUrl = featuredTv.backdropUrl,
                                                rating = featuredTv.rating,
                                                year = featuredTv.year,
                                                genre = featuredTv.genre,
                                                mediaType = "tv",
                                            )
                                        },
                                        isInWatchlist = featuredTv.id in watchlistTvIds,
                                        modifier = Modifier.padding(horizontal = 20.dp),
                                    )
                                }
                            }
                        } else {
                            val featured = uiState.featuredMovie
                            if (featured != null) {
                                item {
                                    FeaturedMovieBanner(
                                        movie = featured,
                                        onDetailsClick = { onMovieClick(featured.id) },
                                        onAddToListClick = { homeViewModel.toggleFeaturedWatchlist(featured) },
                                        onLongClick = {
                                            quickPreviewData = QuickPreviewData(
                                                id = featured.id,
                                                title = featured.title,
                                                overview = featured.description,
                                                posterUrl = featured.posterUrl,
                                                backdropUrl = featured.backdropUrl,
                                                rating = featured.rating,
                                                year = featured.year,
                                                genre = featured.genre,
                                                mediaType = "movie",
                                            )
                                        },
                                        isInWatchlist = featured.id in watchlistMovieIds,
                                        modifier = Modifier.padding(horizontal = 20.dp),
                                    )
                                }
                            }
                        }

                        if (uiState.recommendedMovies.isNotEmpty()) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    HomeSectionHeader(
                                        icon = Icons.Filled.Recommend,
                                        iconTint = Primary,
                                        title = stringResource(R.string.home_section_recommended),
                                        showSeeAll = false,
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        items(uiState.recommendedMovies, key = { it.id }) { movie ->
                                            PopularMovieCard(
                                                movie = movie.copy(
                                                    isFavorite = movie.id in if (uiState.isTvMode) favoriteTvIds else favoriteMovieIds,
                                                ),
                                                onClick = {
                                                    if (uiState.isTvMode) onTvShowClick(movie.id) else onMovieClick(movie.id)
                                                },
                                                onLongClick = {
                                                    quickPreviewData = QuickPreviewData(
                                                        id = movie.id,
                                                        title = movie.title,
                                                        overview = movie.overview,
                                                        posterUrl = movie.posterUrl,
                                                        rating = movie.rating,
                                                        year = movie.year,
                                                        genre = movie.genre,
                                                        mediaType = if (uiState.isTvMode) "tv" else "movie",
                                                        isFavorite = movie.id in if (uiState.isTvMode) favoriteTvIds else favoriteMovieIds,
                                                    )
                                                },
                                                onFavoriteClick = {
                                                    homeViewModel.toggleFavorite(movie, mediaType = if (uiState.isTvMode) "tv" else "movie")
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                HomeSectionHeader(
                                    icon = Icons.Filled.LocalFireDepartment,
                                    iconTint = Color(0xFFFF7A45),
                                    title = if (uiState.isTvMode) stringResource(R.string.movie_list_source_tv_popular) else stringResource(R.string.movie_list_source_popular),
                                    onSeeAllClick = { onSeeAllClick(if (uiState.isTvMode) "popular_tv" else "popular") },
                                    showSeeAll = true,
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    items(uiState.popularMovies, key = { it.id }) { movie ->
                                        PopularMovieCard(
                                            movie = movie.copy(
                                                isFavorite = movie.id in if (uiState.isTvMode) favoriteTvIds else favoriteMovieIds,
                                            ),
                                            onClick = {
                                                if (uiState.isTvMode) onTvShowClick(movie.id) else onMovieClick(movie.id)
                                            },
                                            onLongClick = {
                                                quickPreviewData = QuickPreviewData(
                                                    id = movie.id,
                                                    title = movie.title,
                                                    overview = movie.overview,
                                                    posterUrl = movie.posterUrl,
                                                    rating = movie.rating,
                                                    year = movie.year,
                                                    genre = movie.genre,
                                                    mediaType = if (uiState.isTvMode) "tv" else "movie",
                                                    isFavorite = movie.id in if (uiState.isTvMode) favoriteTvIds else favoriteMovieIds,
                                                )
                                            },
                                            onFavoriteClick = {
                                                homeViewModel.toggleFavorite(movie, mediaType = if (uiState.isTvMode) "tv" else "movie")
                                            },
                                        )
                                    }
                                }
                            }
                        }

                        if (!uiState.isTvMode && uiState.topRatedMovies.isNotEmpty()) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    HomeSectionHeader(
                                        icon = Icons.Filled.Star,
                                        iconTint = Color(0xFFFFC857),
                                        title = stringResource(R.string.home_section_top_rated),
                                        showSeeAll = false,
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        items(uiState.topRatedMovies, key = { it.id }) { movie ->
                                            PopularMovieCard(
                                                movie = movie.copy(isFavorite = movie.id in favoriteMovieIds),
                                                onClick = { onMovieClick(movie.id) },
                                                onLongClick = {
                                                    quickPreviewData = QuickPreviewData(
                                                        id = movie.id,
                                                        title = movie.title,
                                                        overview = movie.overview,
                                                        posterUrl = movie.posterUrl,
                                                        rating = movie.rating,
                                                        year = movie.year,
                                                        genre = movie.genre,
                                                        mediaType = "movie",
                                                        isFavorite = movie.id in favoriteMovieIds,
                                                    )
                                                },
                                                onFavoriteClick = { homeViewModel.toggleFavorite(movie, mediaType = "movie") },
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (uiState.isTvMode) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    HomeSectionHeader(
                                        icon = Icons.Filled.CalendarMonth,
                                        iconTint = Accent,
                                        title = stringResource(R.string.movie_list_source_tv_on_air),
                                        onSeeAllClick = { onSeeAllClick("on_air_tv") },
                                        showSeeAll = true,
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        items(uiState.onAirTvShows, key = { it.id }) { tvShow ->
                                            PopularMovieCard(
                                                movie = tvShow.copy(isFavorite = tvShow.id in favoriteTvIds),
                                                onClick = { onTvShowClick(tvShow.id) },
                                                onLongClick = {
                                                    quickPreviewData = QuickPreviewData(
                                                        id = tvShow.id,
                                                        title = tvShow.title,
                                                        overview = tvShow.overview,
                                                        posterUrl = tvShow.posterUrl,
                                                        rating = tvShow.rating,
                                                        year = tvShow.year,
                                                        genre = tvShow.genre,
                                                        mediaType = "tv",
                                                        isFavorite = tvShow.id in favoriteTvIds,
                                                    )
                                                },
                                                onFavoriteClick = { homeViewModel.toggleFavorite(tvShow, mediaType = "tv") },
                                            )
                                        }
                                    }
                                }
                            }
                            if (upcomingTvShows.isNotEmpty()) {
                                item {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        HomeSectionHeader(
                                            icon = Icons.Filled.CalendarMonth,
                                            iconTint = Accent,
                                            title = stringResource(R.string.movie_list_source_tv_upcoming),
                                            onSeeAllClick = { onSeeAllClick("upcoming_tv") },
                                        )
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 20.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            items(upcomingTvShows, key = { it.id }) { tvShow ->
                                                UpcomingMovieCard(
                                                    movie = tvShow,
                                                    onClick = { onTvShowClick(tvShow.id) },
                                                    onLongClick = {
                                                        quickPreviewData = QuickPreviewData(
                                                            id = tvShow.id,
                                                            title = tvShow.title,
                                                            posterUrl = tvShow.posterUrl,
                                                            year = tvShow.year,
                                                            mediaType = "tv",
                                                        )
                                                    },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    HomeSectionHeader(
                                        icon = Icons.Filled.CalendarMonth,
                                        iconTint = Accent,
                                        title = stringResource(R.string.movie_list_source_upcoming),
                                        onSeeAllClick = { onSeeAllClick("upcoming") },
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        items(uiState.upcomingMovies, key = { it.id }) { movie ->
                                            UpcomingMovieCard(
                                                movie = movie,
                                                onClick = { onMovieClick(movie.id) },
                                                onLongClick = {
                                                    quickPreviewData = QuickPreviewData(
                                                        id = movie.id,
                                                        title = movie.title,
                                                        posterUrl = movie.posterUrl,
                                                        year = movie.year,
                                                        mediaType = "movie",
                                                    )
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                HomeSectionHeader(
                                    icon = Icons.Filled.GridView,
                                    iconTint = Primary,
                                    title = stringResource(R.string.home_section_categories),
                                    showSeeAll = false,
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    items(uiState.categories, key = { it.id }) { category ->
                                        CategoryChip(
                                            category = category,
                                            icon = categoryIcon(category.genreId),
                                            iconTint = categoryColor(category.genreId),
                                            selected = category.id == selectedCategoryId,
                                            onClick = {
                                                selectedCategoryId = category.id
                                                if (uiState.isTvMode) {
                                                    onTvCategoryClick(category)
                                                } else {
                                                    onCategoryClick(category)
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!uiState.isLoading && uiState.errorMessage == null) {
            // Alt Bara Doğrudan Bitişik Efsanevi Keşif Akışı Butonu ve Alt Navigasyon Barı
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                // Alt Bara Üstten Bitişik Seviye Butonu (Daha Büyük & Belirgin)
                Box(
                    modifier = Modifier
                        .offset(y = 6.dp)
                        .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Primary, Color(0xFFE040FB)),
                            ),
                        )
                        .border(
                            width = 1.5.dp,
                            brush = Brush.horizontalGradient(
                                listOf(Color.White.copy(alpha = 0.7f), Color.White.copy(alpha = 0.25f)),
                            ),
                            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
                        )
                        .clickable { onReelsClick() }
                        .padding(horizontal = 26.dp, vertical = 11.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Sonsuz Keşif Akışı",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                CVBottomNavBar(
                    selectedIndex = 0,
                    onItemSelected = { index ->
                        if (index == 1) onAiSearchClick(uiState.isTvMode) else onNavigateTab(index)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        offlineActionMessage?.let { message ->
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 88.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(message, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }

        FloatingDiceButton(
            onClick = { showDiceDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 80.dp),
        )

        if (showDiceDialog) {
            DicePickerDialog(
                popularMovies = uiState.popularMovies,
                popularTvShows = uiState.popularTvShows,
                topRatedMovies = uiState.topRatedMovies,
                onMovieClick = onMovieClick,
                onTvShowClick = onTvShowClick,
                onDismiss = { showDiceDialog = false },
            )
        }

        quickPreviewData?.let { data ->
            QuickPreviewSheet(
                data = data,
                onNavigateDetail = {
                    val targetId = data.id
                    val isTv = data.mediaType == "tv"
                    quickPreviewData = null
                    if (isTv) onTvShowClick(targetId) else onMovieClick(targetId)
                },
                onShareWithFriends = {
                    recommendShareViewModel.reset()
                    shareMediaId = data.id
                    shareMediaType = data.mediaType
                    shareMediaTitle = data.title
                    shareMediaPosterUrl = data.posterUrl
                    showShareSheet = true
                    quickPreviewData = null
                },
                onDismiss = { quickPreviewData = null },
            )
        }

        if (showShareSheet && shareMediaId != null) {
            RecommendShareSheet(
                state = shareState,
                mediaTitle = shareMediaTitle,
                posterUrl = shareMediaPosterUrl,
                isTvShow = shareMediaType == "tv",
                onToggleFriend = recommendShareViewModel::toggleFriend,
                onNoteChange = recommendShareViewModel::onNoteChange,
                onSend = {
                    recommendShareViewModel.send(
                        mediaId = shareMediaId!!,
                        mediaType = shareMediaType,
                        mediaTitle = shareMediaTitle,
                        posterUrl = shareMediaPosterUrl,
                    )
                },
                onDismiss = { showShareSheet = false },
            )
        }

        // En üstte: alt navigasyon çubuğunu ve mesajları da örtmeli.
        FriendsPanel(
            visible = showFriendsPanel,
            onDismiss = { showFriendsPanel = false },
            onMovieClick = onMovieClick,
            onTvShowClick = onTvShowClick,
        )
    }
}
