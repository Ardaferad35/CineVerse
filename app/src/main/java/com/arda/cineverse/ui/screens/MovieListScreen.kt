package com.arda.cineverse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arda.cineverse.data.model.Movie
import com.arda.cineverse.data.model.SavedMovie
import com.arda.cineverse.data.model.TvShow
import com.arda.cineverse.data.model.UpcomingMovie
import com.arda.cineverse.data.remote.toHomeMovie
import com.arda.cineverse.data.repository.CommentRepository
import com.arda.cineverse.data.repository.MovieRepository
import com.arda.cineverse.data.repository.RecommendationRepository
import com.arda.cineverse.data.repository.TvRepository
import com.arda.cineverse.data.repository.UserListRepository
import com.arda.cineverse.ui.components.CVGradientButton
import com.arda.cineverse.ui.components.MovieListItemCard
import com.arda.cineverse.ui.components.OfflineActionSnackbar
import com.arda.cineverse.ui.components.OfflineWriteMessageState
import com.arda.cineverse.ui.components.rememberOfflineWriteMessageState
import com.arda.cineverse.ui.components.SortChip
import com.arda.cineverse.ui.components.UpcomingMovieCard
import com.arda.cineverse.ui.theme.Background
import com.arda.cineverse.ui.theme.OnSurface
import com.arda.cineverse.ui.theme.Primary
import com.arda.cineverse.ui.theme.TextSecondary
import com.arda.cineverse.viewmodel.PaginatedMovieListViewModel
import com.arda.cineverse.viewmodel.PaginatedMovieListViewModelFactory
import kotlinx.coroutines.launch

sealed class MovieListSource {
    data object Popular : MovieListSource()
    data object Upcoming : MovieListSource()
    data object TvPopular : MovieListSource()
    data object TvOnAir : MovieListSource()
    data class Genre(val genreId: Int, val label: String) : MovieListSource()
    data class TvGenre(val genreId: Int, val label: String) : MovieListSource()
}

private enum class SortMode(val label: String) {
    POPULAR("Popüler"),
    TOP_RATED("En Yüksek Puan"),
    APP_RATING("Uygulama İçi Puan"),
}

private fun Movie.toSavedMovie() = SavedMovie(id = id, title = title, posterUrl = posterUrl, rating = rating, year = year, genreIds = genreIds, mediaType = mediaType)
private fun TvShow.toSavedMovie() = SavedMovie(id = id, title = name, posterUrl = posterUrl, rating = rating, year = year, genreIds = genreIds, mediaType = "tv")

/**
 * Favori toggle'ının offline-aware yazma + öneri sinyali güncelleme mantığı —
 * bu dosyadaki 4 farklı listede (film türü/popüler dizi/yayındaki dizi/dizi
 * türü) aynı şekilde tekrarlanmasın diye ortaklandı.
 */
private suspend fun writeFavoriteToggle(
    userListRepository: UserListRepository,
    recommendationRepository: RecommendationRepository,
    isFav: Boolean,
    savedMovie: SavedMovie,
): Result<Unit> {
    val result = if (isFav) {
        userListRepository.removeFavorite(savedMovie.id, mediaType = savedMovie.mediaType)
    } else {
        userListRepository.addFavorite(savedMovie)
    }
    result.onSuccess {
        val isTv = savedMovie.mediaType == "tv"
        if (isFav) {
            if (isTv) recommendationRepository.removeTvFavoriteSignal(savedMovie.id) else recommendationRepository.removeFavoriteSignal(savedMovie.id)
        } else {
            if (isTv) recommendationRepository.recordTvFavorite(savedMovie.id, savedMovie.genreIds) else recommendationRepository.recordFavorite(savedMovie.id, savedMovie.genreIds)
        }
    }
    return result
}

private suspend fun writeWatchlistToggle(userListRepository: UserListRepository, isSaved: Boolean, savedMovie: SavedMovie): Result<Unit> =
    if (isSaved) {
        userListRepository.removeFromWatchlist(savedMovie.id, mediaType = savedMovie.mediaType)
    } else {
        userListRepository.addToWatchlist(savedMovie)
    }

@Composable
fun MovieListScreen(
    source: MovieListSource,
    onBack: () -> Unit = {},
    onMovieClick: (Int) -> Unit = {},
    onTvShowClick: (Int) -> Unit = {},
) {
    // Bu ekrandaki 4 farklı liste türünün (film türü/popüler dizi/yayındaki
    // dizi/dizi türü) hepsi aynı favori/izleme listesi toggle mantığını
    // paylaşıyor — offline mesaj state'i burada tek yerden tutulup ilgili
    // listeye geçiriliyor, aynı anda sadece biri gösterildiği için çakışma olmaz.
    val offlineMessageState = rememberOfflineWriteMessageState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .statusBarsPadding(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = OnSurface)
                }
                Text(
                    text = when (source) {
                        MovieListSource.Popular -> "Popüler Filmler"
                        MovieListSource.Upcoming -> "Yakında Vizyona Girecekler"
                        MovieListSource.TvPopular -> "Popüler Diziler"
                        MovieListSource.TvOnAir -> "Şu An Yayında"
                        is MovieListSource.Genre -> source.label
                        is MovieListSource.TvGenre -> source.label
                    },
                    color = OnSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = { /* Detaylı filtreleme sonraki adımda eklenecek */ }) {
                    Icon(Icons.Filled.FilterList, contentDescription = "Filtrele", tint = OnSurface)
                }
            }

            when (source) {
                MovieListSource.Upcoming -> UpcomingMoviesGrid(onMovieClick = onMovieClick)
                is MovieListSource.TvGenre -> TvGenreList(source = source, onTvShowClick = onTvShowClick, offlineMessageState = offlineMessageState)
                MovieListSource.TvPopular -> TvPopularList(onTvShowClick = onTvShowClick, offlineMessageState = offlineMessageState)
                MovieListSource.TvOnAir -> TvOnAirList(onTvShowClick = onTvShowClick, offlineMessageState = offlineMessageState)
                else -> RichMovieList(source = source, onMovieClick = onMovieClick, offlineMessageState = offlineMessageState)
            }
        }

        offlineMessageState.message?.let {
            OfflineActionSnackbar(
                message = it,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun RichMovieList(source: MovieListSource, onMovieClick: (Int) -> Unit, offlineMessageState: OfflineWriteMessageState) {
    var sortMode by remember { mutableStateOf(SortMode.POPULAR) }
    val repository = remember { MovieRepository() }
    val userListRepository = remember { UserListRepository() }
    val commentRepository = remember { CommentRepository() }
    val recommendationRepository = remember { RecommendationRepository() }
    val scope = rememberCoroutineScope()

    val fetchPage: suspend (Int) -> Result<List<Movie>> = remember(source, sortMode) {
        when (source) {
            MovieListSource.Popular -> { page ->
                when (sortMode) {
                    SortMode.POPULAR -> repository.getPopularMovies(page)
                    SortMode.TOP_RATED -> repository.getTopRatedMovies(page)
                    SortMode.APP_RATING -> commentRepository.getTopRatedByAppUsers(page)
                }
            }
            is MovieListSource.Genre -> { page ->
                when (sortMode) {
                    SortMode.POPULAR -> repository.getMoviesByGenre(source.genreId, page)
                    SortMode.TOP_RATED -> repository.getMoviesByGenreTopRated(source.genreId, page)
                    SortMode.APP_RATING -> commentRepository.getTopRatedByAppUsersForGenre(source.genreId, page)
                }
            }
            MovieListSource.Upcoming -> { _ -> Result.success(emptyList()) }
            is MovieListSource.TvGenre -> { _ -> Result.success(emptyList()) }
            MovieListSource.TvPopular -> { _ -> Result.success(emptyList()) }
            MovieListSource.TvOnAir -> { _ -> Result.success(emptyList()) }
        }
    }

    val viewModel: PaginatedMovieListViewModel<Movie> = viewModel(
        key = "movieList_${source}_$sortMode",
        factory = PaginatedMovieListViewModelFactory(fetchPage = fetchPage, idSelector = { it.id }),
    )
    val uiState by viewModel.uiState.collectAsState()

    var favoriteIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var watchlistIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    LaunchedEffect(Unit) {
        favoriteIds = userListRepository.getFavorites().getOrDefault(emptyList()).map { it.id }.toSet()
        watchlistIds = userListRepository.getWatchlist().getOrDefault(emptyList()).map { it.id }.toSet()
    }

    fun toggleFavorite(movie: Movie) {
        val isFav = movie.id in favoriteIds
        favoriteIds = if (isFav) favoriteIds - movie.id else favoriteIds + movie.id
        scope.launch {
            writeFavoriteToggle(userListRepository, recommendationRepository, isFav, movie.toSavedMovie()).onFailure { error ->
                offlineMessageState.handle(error) { favoriteIds = if (isFav) favoriteIds + movie.id else favoriteIds - movie.id }
            }
        }
    }

    fun toggleWatchlist(movie: Movie) {
        val isSaved = movie.id in watchlistIds
        watchlistIds = if (isSaved) watchlistIds - movie.id else watchlistIds + movie.id
        scope.launch {
            writeWatchlistToggle(userListRepository, isSaved, movie.toSavedMovie()).onFailure { error ->
                offlineMessageState.handle(error) { watchlistIds = if (isSaved) watchlistIds + movie.id else watchlistIds - movie.id }
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SortChip(label = SortMode.POPULAR.label, selected = sortMode == SortMode.POPULAR, onClick = { sortMode = SortMode.POPULAR })
        SortChip(label = SortMode.TOP_RATED.label, selected = sortMode == SortMode.TOP_RATED, onClick = { sortMode = SortMode.TOP_RATED })
        SortChip(label = SortMode.APP_RATING.label, selected = sortMode == SortMode.APP_RATING, onClick = { sortMode = SortMode.APP_RATING })
    }

    when {
        uiState.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        }
        uiState.errorMessage != null && uiState.items.isEmpty() -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(uiState.errorMessage!!, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                CVGradientButton(text = "Tekrar Dene", onClick = viewModel::loadFirstPage)
            }
        }
        else -> {
            val listState = rememberLazyListState()

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(uiState.items, key = { _, movie -> movie.id }) { _, movie ->
                    MovieListItemCard(
                        movie = movie,
                        isFavorite = movie.id in favoriteIds,
                        onDetailsClick = { onMovieClick(movie.id) },
                        onAddToListClick = { toggleWatchlist(movie) },
                        onFavoriteClick = { toggleFavorite(movie) },
                    )
                }
                if (uiState.isLoadingMore) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Primary, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        }
                    }
                }
            }

            LaunchedEffect(listState, uiState.items.size) {
                snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                    .collect { lastVisibleIndex ->
                        if (lastVisibleIndex != null && lastVisibleIndex >= uiState.items.size - 6) {
                            viewModel.loadNextPage()
                        }
                    }
            }
        }
    }
}

@Composable
private fun UpcomingMoviesGrid(onMovieClick: (Int) -> Unit) {
    val viewModel: PaginatedMovieListViewModel<UpcomingMovie> = viewModel(
        factory = PaginatedMovieListViewModelFactory(
            fetchPage = { page -> MovieRepository().getUpcomingMovies(page) },
            idSelector = { it.id },
        ),
    )
    val uiState by viewModel.uiState.collectAsState()

    when {
        uiState.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        }
        uiState.errorMessage != null && uiState.items.isEmpty() -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(uiState.errorMessage!!, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                CVGradientButton(text = "Tekrar Dene", onClick = viewModel::loadFirstPage)
            }
        }
        else -> {
            val gridState = rememberLazyGridState()

            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(uiState.items, key = { it.id }) { movie ->
                    UpcomingMovieCard(movie = movie, onClick = { onMovieClick(movie.id) })
                }
                if (uiState.isLoadingMore) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Primary, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        }
                    }
                }
            }

            LaunchedEffect(gridState, uiState.items.size) {
                snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                    .collect { lastVisibleIndex ->
                        if (lastVisibleIndex != null && lastVisibleIndex >= uiState.items.size - 6) {
                            viewModel.loadNextPage()
                        }
                    }
            }
        }
    }
}

@Composable
private fun TvGenreList(source: MovieListSource.TvGenre, onTvShowClick: (Int) -> Unit, offlineMessageState: OfflineWriteMessageState) {
    val repository = remember { TvRepository() }
    val userListRepository = remember { UserListRepository() }
    val recommendationRepository = remember { RecommendationRepository() }
    val scope = rememberCoroutineScope()

    val viewModel: PaginatedMovieListViewModel<TvShow> = viewModel(
        key = "tvGenre_${source.genreId}",
        factory = PaginatedMovieListViewModelFactory(
            fetchPage = { page -> repository.getTvShowsByGenre(source.genreId, page) },
            idSelector = { it.id },
        ),
    )
    val uiState by viewModel.uiState.collectAsState()

    var favoriteIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var watchlistIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    LaunchedEffect(Unit) {
        val favorites = userListRepository.getFavorites().getOrDefault(emptyList())
        favoriteIds = favorites.filter { it.mediaType == "tv" }.map { it.id }.toSet()
        val watchlist = userListRepository.getWatchlist().getOrDefault(emptyList())
        watchlistIds = watchlist.filter { it.mediaType == "tv" }.map { it.id }.toSet()
    }

    fun toggleFavorite(show: TvShow) {
        val isFav = show.id in favoriteIds
        favoriteIds = if (isFav) favoriteIds - show.id else favoriteIds + show.id
        scope.launch {
            writeFavoriteToggle(userListRepository, recommendationRepository, isFav, show.toSavedMovie()).onFailure { error ->
                offlineMessageState.handle(error) { favoriteIds = if (isFav) favoriteIds + show.id else favoriteIds - show.id }
            }
        }
    }

    fun toggleWatchlist(show: TvShow) {
        val isSaved = show.id in watchlistIds
        watchlistIds = if (isSaved) watchlistIds - show.id else watchlistIds + show.id
        scope.launch {
            writeWatchlistToggle(userListRepository, isSaved, show.toSavedMovie()).onFailure { error ->
                offlineMessageState.handle(error) { watchlistIds = if (isSaved) watchlistIds + show.id else watchlistIds - show.id }
            }
        }
    }

    when {
        uiState.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        }
        uiState.errorMessage != null && uiState.items.isEmpty() -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(uiState.errorMessage!!, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                CVGradientButton(text = "Tekrar Dene", onClick = viewModel::loadFirstPage)
            }
        }
        else -> {
            val listState = rememberLazyListState()

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(uiState.items, key = { _, show -> show.id }) { _, show ->
                    MovieListItemCard(
                        movie = show.toHomeMovie(),
                        isFavorite = show.id in favoriteIds,
                        onDetailsClick = { onTvShowClick(show.id) },
                        onAddToListClick = { toggleWatchlist(show) },
                        onFavoriteClick = { toggleFavorite(show) },
                    )
                }
                if (uiState.isLoadingMore) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Primary, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        }
                    }
                }
            }

            LaunchedEffect(listState, uiState.items.size) {
                snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                    .collect { lastVisibleIndex ->
                        if (lastVisibleIndex != null && lastVisibleIndex >= uiState.items.size - 6) {
                            viewModel.loadNextPage()
                        }
                    }
            }
        }
    }
}
@Composable
private fun TvPopularList(onTvShowClick: (Int) -> Unit, offlineMessageState: OfflineWriteMessageState) {
    val repository = remember { TvRepository() }
    val userListRepository = remember { UserListRepository() }
    val recommendationRepository = remember { RecommendationRepository() }
    val scope = rememberCoroutineScope()

    val viewModel: PaginatedMovieListViewModel<TvShow> = viewModel(
        key = "tvPopularList",
        factory = PaginatedMovieListViewModelFactory(
            fetchPage = { page -> repository.getPopularTvShows(page) },
            idSelector = { it.id },
        ),
    )
    val uiState by viewModel.uiState.collectAsState()

    var favoriteIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var watchlistIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    LaunchedEffect(Unit) {
        val favorites = userListRepository.getFavorites().getOrDefault(emptyList())
        favoriteIds = favorites.filter { it.mediaType == "tv" }.map { it.id }.toSet()
        val watchlist = userListRepository.getWatchlist().getOrDefault(emptyList())
        watchlistIds = watchlist.filter { it.mediaType == "tv" }.map { it.id }.toSet()
    }

    fun toggleFavorite(show: TvShow) {
        val isFav = show.id in favoriteIds
        favoriteIds = if (isFav) favoriteIds - show.id else favoriteIds + show.id
        scope.launch {
            writeFavoriteToggle(userListRepository, recommendationRepository, isFav, show.toSavedMovie()).onFailure { error ->
                offlineMessageState.handle(error) { favoriteIds = if (isFav) favoriteIds + show.id else favoriteIds - show.id }
            }
        }
    }

    fun toggleWatchlist(show: TvShow) {
        val isSaved = show.id in watchlistIds
        watchlistIds = if (isSaved) watchlistIds - show.id else watchlistIds + show.id
        scope.launch {
            writeWatchlistToggle(userListRepository, isSaved, show.toSavedMovie()).onFailure { error ->
                offlineMessageState.handle(error) { watchlistIds = if (isSaved) watchlistIds + show.id else watchlistIds - show.id }
            }
        }
    }

    when {
        uiState.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        }
        uiState.errorMessage != null && uiState.items.isEmpty() -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(uiState.errorMessage!!, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                CVGradientButton(text = "Tekrar Dene", onClick = viewModel::loadFirstPage)
            }
        }
        else -> {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(uiState.items, key = { _, show -> show.id }) { _, show ->
                    MovieListItemCard(
                        movie = show.toHomeMovie(),
                        isFavorite = show.id in favoriteIds,
                        onDetailsClick = { onTvShowClick(show.id) },
                        onAddToListClick = { toggleWatchlist(show) },
                        onFavoriteClick = { toggleFavorite(show) },
                    )
                }
                if (uiState.isLoadingMore) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Primary, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        }
                    }
                }
            }
            LaunchedEffect(listState, uiState.items.size) {
                snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                    .collect { lastVisibleIndex ->
                        if (lastVisibleIndex != null && lastVisibleIndex >= uiState.items.size - 6) {
                            viewModel.loadNextPage()
                        }
                    }
            }
        }
    }
}

@Composable
private fun TvOnAirList(onTvShowClick: (Int) -> Unit, offlineMessageState: OfflineWriteMessageState) {
    val repository = remember { TvRepository() }
    val userListRepository = remember { UserListRepository() }
    val recommendationRepository = remember { RecommendationRepository() }
    val scope = rememberCoroutineScope()

    // Su An Yayinda: TMDB on_the_air endpoint tek seferlik buyuk bir havuz dondurur;
    // ilk sayfada hedef sayiya kadar cekip gosteriyoruz, ikinci sayfa bos donuyor.
    val viewModel: PaginatedMovieListViewModel<TvShow> = viewModel(
        key = "tvOnAirList",
        factory = PaginatedMovieListViewModelFactory(
            fetchPage = { page -> if (page == 1) repository.getOnTheAirTvShows(targetCount = 60) else Result.success(emptyList()) },
            idSelector = { it.id },
        ),
    )
    val uiState by viewModel.uiState.collectAsState()

    var favoriteIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var watchlistIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    LaunchedEffect(Unit) {
        val favorites = userListRepository.getFavorites().getOrDefault(emptyList())
        favoriteIds = favorites.filter { it.mediaType == "tv" }.map { it.id }.toSet()
        val watchlist = userListRepository.getWatchlist().getOrDefault(emptyList())
        watchlistIds = watchlist.filter { it.mediaType == "tv" }.map { it.id }.toSet()
    }

    fun toggleFavorite(show: TvShow) {
        val isFav = show.id in favoriteIds
        favoriteIds = if (isFav) favoriteIds - show.id else favoriteIds + show.id
        scope.launch {
            writeFavoriteToggle(userListRepository, recommendationRepository, isFav, show.toSavedMovie()).onFailure { error ->
                offlineMessageState.handle(error) { favoriteIds = if (isFav) favoriteIds + show.id else favoriteIds - show.id }
            }
        }
    }

    fun toggleWatchlist(show: TvShow) {
        val isSaved = show.id in watchlistIds
        watchlistIds = if (isSaved) watchlistIds - show.id else watchlistIds + show.id
        scope.launch {
            writeWatchlistToggle(userListRepository, isSaved, show.toSavedMovie()).onFailure { error ->
                offlineMessageState.handle(error) { watchlistIds = if (isSaved) watchlistIds + show.id else watchlistIds - show.id }
            }
        }
    }

    when {
        uiState.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        }
        uiState.errorMessage != null && uiState.items.isEmpty() -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(uiState.errorMessage!!, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                CVGradientButton(text = "Tekrar Dene", onClick = viewModel::loadFirstPage)
            }
        }
        else -> {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(uiState.items, key = { _, show -> show.id }) { _, show ->
                    MovieListItemCard(
                        movie = show.toHomeMovie(),
                        isFavorite = show.id in favoriteIds,
                        onDetailsClick = { onTvShowClick(show.id) },
                        onAddToListClick = { toggleWatchlist(show) },
                        onFavoriteClick = { toggleFavorite(show) },
                    )
                }
            }
        }
    }
}
