package com.arda.cineverse.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Recommend
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arda.cineverse.ui.components.*
import com.arda.cineverse.data.model.Category
import com.arda.cineverse.data.model.FeaturedMovie
import com.arda.cineverse.data.model.FeaturedTvShow
import com.arda.cineverse.data.model.Movie
import com.arda.cineverse.data.model.SavedMovie
import com.arda.cineverse.data.model.UpcomingMovie
import com.arda.cineverse.data.remote.toFeaturedMovie
import com.arda.cineverse.data.repository.RecommendationRepository
import com.arda.cineverse.data.repository.TvRepository
import com.arda.cineverse.data.repository.UserListRepository
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
import com.arda.cineverse.data.common.OfflineWriteException
import com.arda.cineverse.viewmodel.NotificationViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = viewModel(),
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

    val userListRepository = remember { UserListRepository() }
    val recommendationRepository = remember { RecommendationRepository() }
    val tvRepository = remember { TvRepository() }
    val scope = rememberCoroutineScope()

    var favoriteMovieIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var favoriteTvIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var watchlistMovieIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var watchlistTvIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var offlineActionMessage by remember { mutableStateOf<String?>(null) }
    var upcomingTvShows by remember { mutableStateOf<List<UpcomingMovie>>(emptyList()) }

    LaunchedEffect(offlineActionMessage) {
        if (offlineActionMessage != null) {
            delay(2500)
            offlineActionMessage = null
        }
    }

    // "Yakında Yayınlanacak Diziler": Popüler Diziler/Şu An Yayında'nın aksine
    // Room'da offline-cache'lenmiş kalıcı bir bölüm değil — TV moduna her
    // girildiğinde tek seferlik ağ çağrısıyla dolduruluyor, bu yüzden offline'ken
    // boş kalabilir.
    LaunchedEffect(uiState.isTvMode) {
        if (uiState.isTvMode) {
            upcomingTvShows = tvRepository.getUpcomingTvShows().getOrDefault(emptyList())
        }
    }

    // Favoriler/İzleme Listesi Room'dan (offline-first) gözlemleniyor; ayrıca
    // her Home açılışında çevrimiçiyse Firestore'dan taze veriyle senkronize
    // edilir (bkz. UserListRepository.syncFavoritesAndWatchlist).
    LaunchedEffect(Unit) {
        scope.launch { userListRepository.syncFavoritesAndWatchlist() }
    }
    LaunchedEffect(Unit) {
        userListRepository.observeFavorites().collect { favorites ->
            favoriteMovieIds = favorites.filter { it.mediaType == "movie" }.map { it.id }.toSet()
            favoriteTvIds = favorites.filter { it.mediaType == "tv" }.map { it.id }.toSet()
        }
    }
    LaunchedEffect(Unit) {
        userListRepository.observeWatchlist().collect { watchlist ->
            watchlistMovieIds = watchlist.filter { it.mediaType == "movie" }.map { it.id }.toSet()
            watchlistTvIds = watchlist.filter { it.mediaType == "tv" }.map { it.id }.toSet()
        }
    }

    LaunchedEffect(uiState.categories, selectedHomeMode) {
        if (uiState.categories.isNotEmpty() && uiState.categories.none { it.id == selectedCategoryId }) {
            selectedCategoryId = uiState.categories.first().id
        }
    }

    /** Offline'ken yazma işlemleri reddedilir; optimistic UI değişikliği geri alınır ve kullanıcıya bilgi verilir. */
    fun handleOfflineWriteFailure(error: Throwable, revert: () -> Unit): Boolean {
        if (error is OfflineWriteException) {
            revert()
            offlineActionMessage = error.message
            return true
        }
        return false
    }

    fun toggleFavorite(movie: Movie, mediaType: String = "movie") {
        val isTv = mediaType == "tv"
        val currentIds = if (isTv) favoriteTvIds else favoriteMovieIds
        val isFav = movie.id in currentIds
        if (isTv) {
            favoriteTvIds = if (isFav) favoriteTvIds - movie.id else favoriteTvIds + movie.id
        } else {
            favoriteMovieIds = if (isFav) favoriteMovieIds - movie.id else favoriteMovieIds + movie.id
        }
        fun revert() {
            if (isTv) {
                favoriteTvIds = if (isFav) favoriteTvIds + movie.id else favoriteTvIds - movie.id
            } else {
                favoriteMovieIds = if (isFav) favoriteMovieIds + movie.id else favoriteMovieIds - movie.id
            }
        }
        scope.launch {
            val result = if (isFav) {
                userListRepository.removeFavorite(movie.id, mediaType = mediaType)
            } else {
                userListRepository.addFavorite(
                    SavedMovie(
                        id = movie.id,
                        title = movie.title,
                        posterUrl = movie.posterUrl,
                        rating = movie.rating,
                        year = movie.year,
                        genreIds = movie.genreIds,
                        mediaType = mediaType,
                    ),
                )
            }
            result.onFailure { error ->
                if (!handleOfflineWriteFailure(error) { revert() }) return@onFailure
            }
            if (result.isSuccess) {
                if (isFav) {
                    if (isTv) recommendationRepository.removeTvFavoriteSignal(movie.id) else recommendationRepository.removeFavoriteSignal(movie.id)
                } else {
                    if (isTv) recommendationRepository.recordTvFavorite(movie.id, movie.genreIds) else recommendationRepository.recordFavorite(movie.id, movie.genreIds)
                }
            }
        }
    }

    fun toggleFeaturedWatchlist(featured: FeaturedMovie) {
        val isSaved = featured.id in watchlistMovieIds
        watchlistMovieIds = if (isSaved) watchlistMovieIds - featured.id else watchlistMovieIds + featured.id
        scope.launch {
            val result = if (isSaved) {
                userListRepository.removeFromWatchlist(featured.id)
            } else {
                userListRepository.addToWatchlist(
                    SavedMovie(id = featured.id, title = featured.title, posterUrl = featured.posterUrl, rating = featured.rating, year = featured.year),
                )
            }
            result.onFailure { error ->
                handleOfflineWriteFailure(error) {
                    watchlistMovieIds = if (isSaved) watchlistMovieIds + featured.id else watchlistMovieIds - featured.id
                }
            }
        }
    }

    fun toggleFeaturedTvWatchlist(featured: FeaturedTvShow) {
        val isSaved = featured.id in watchlistTvIds
        watchlistTvIds = if (isSaved) watchlistTvIds - featured.id else watchlistTvIds + featured.id
        scope.launch {
            val result = if (isSaved) {
                userListRepository.removeFromWatchlist(featured.id, mediaType = "tv")
            } else {
                userListRepository.addToWatchlist(
                    SavedMovie(
                        id = featured.id,
                        title = featured.title,
                        posterUrl = featured.posterUrl,
                        rating = featured.rating,
                        year = featured.year,
                        mediaType = "tv",
                    ),
                )
            }
            result.onFailure { error ->
                handleOfflineWriteFailure(error) {
                    watchlistTvIds = if (isSaved) watchlistTvIds + featured.id else watchlistTvIds - featured.id
                }
            }
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
                            text = "Tekrar Dene",
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
                                    Text("Aran\u0131yor...", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
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
                                    Text("Sonu\u00E7 bulunamad\u0131", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
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
                                        label = "GÜNÜN DİZİSİ",
                                        onDetailsClick = { onTvShowClick(featuredTv.id) },
                                        onAddToListClick = { toggleFeaturedTvWatchlist(featuredTv) },
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
                                        onAddToListClick = { toggleFeaturedWatchlist(featured) },
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
                                        title = "Sizin \u0130\u00E7in \u00D6nerilenler",
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
                                                onFavoriteClick = {
                                                    toggleFavorite(movie, mediaType = if (uiState.isTvMode) "tv" else "movie")
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
                                    title = if (uiState.isTvMode) "Pop\u00FCler Diziler" else "Pop\u00FCler Filmler",
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
                                            onFavoriteClick = {
                                                toggleFavorite(movie, mediaType = if (uiState.isTvMode) "tv" else "movie")
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
                                        title = "En \u00C7ok Be\u011Fenilenler",
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
                                                onFavoriteClick = { toggleFavorite(movie, mediaType = "movie") },
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
                                        title = "\u015Eu An Yay\u0131nda",
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
                                                onFavoriteClick = { toggleFavorite(tvShow, mediaType = "tv") },
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
                                            title = "Yak\u0131nda Yay\u0131nlanacak Diziler",
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
                                        title = "Yak\u0131nda Vizyona Girecekler",
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
                                    title = "Kategoriler",
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
            CVBottomNavBar(
                selectedIndex = 0,
                onItemSelected = { index ->
                    if (index == 1) onAiSearchClick(uiState.isTvMode) else onNavigateTab(index)
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            )
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

        // En üstte: alt navigasyon çubuğunu ve mesajları da örtmeli.
        FriendsPanel(
            visible = showFriendsPanel,
            onDismiss = { showFriendsPanel = false },
        )
    }
}
