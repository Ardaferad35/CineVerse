package com.arda.cineverse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arda.cineverse.data.common.GENERIC_WRITE_FAILURE_MESSAGE
import com.arda.cineverse.data.common.OfflineWriteException
import com.arda.cineverse.data.common.SyncResult
import com.arda.cineverse.data.connectivity.ConnectivityObserver
import com.arda.cineverse.data.local.datastore.UserPreferencesRepository
import com.arda.cineverse.data.model.Category
import com.arda.cineverse.data.model.FeaturedMovie
import com.arda.cineverse.data.model.FeaturedTvShow
import com.arda.cineverse.data.model.Movie
import com.arda.cineverse.data.model.SavedMovie
import com.arda.cineverse.data.model.SearchSuggestion
import com.arda.cineverse.data.model.UpcomingMovie
import com.arda.cineverse.data.remote.toHomeMovie
import com.arda.cineverse.data.repository.MovieRepository
import com.arda.cineverse.data.repository.RecommendationRepository
import com.arda.cineverse.data.repository.TvRepository
import com.arda.cineverse.data.repository.UserListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val isTvMode: Boolean = false,
    val featuredMovie: FeaturedMovie? = null,
    val featuredTvShow: FeaturedTvShow? = null,
    val popularMovies: List<Movie> = emptyList(),
    val popularTvShows: List<Movie> = emptyList(),
    val topRatedMovies: List<Movie> = emptyList(),
    val upcomingMovies: List<UpcomingMovie> = emptyList(),
    val onAirTvShows: List<Movie> = emptyList(),
    val recommendedMovies: List<Movie> = emptyList(),
    val categories: List<Category> = emptyList(),
    val errorMessage: String? = null,
    // Smart Offline Mode: Home her zaman Room'daki (belki bayat) veriyi
    // anında gösterir; bu üç alan sadece senkronizasyon durumunu UI'a
    // yansıtmak için var (ekstra bir loading/error state makinesi gerekmez).
    val isOffline: Boolean = false,
    val lastSyncedAt: Long? = null,
    val isSyncing: Boolean = false,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchSuggestions: List<SearchSuggestion> = emptyList(),
    // Favoriler/İzleme Listesi Room'dan gözlemlenir (offline-first); toggle'lar
    // önce bu set'leri iyimser günceller, yazma başarısız olursa geri alınır.
    val favoriteMovieIds: Set<Int> = emptySet(),
    val favoriteTvIds: Set<Int> = emptySet(),
    val watchlistMovieIds: Set<Int> = emptySet(),
    val watchlistTvIds: Set<Int> = emptySet(),
    // Room'da cache'lenmeyen tek Home bölümü: TV moduna girişte ağdan çekilir,
    // offline'ken boş kalabilir.
    val upcomingTvShows: List<UpcomingMovie> = emptyList(),
    val offlineActionMessage: String? = null,
    val reminderMovieIds: Set<Int> = emptySet(),
)

private data class MovieCache(
    val popular: List<Movie>,
    val topRated: List<Movie>,
    val upcoming: List<UpcomingMovie>,
    val featured: FeaturedMovie?,
    val recommended: List<Movie>,
    val categories: List<Category>,
)

private data class TvCache(
    val popular: List<Movie>,
    val onAir: List<Movie>,
    val featured: FeaturedTvShow?,
    val recommended: List<Movie>,
    val categories: List<Category>,
)

private sealed interface HomeCacheSnapshot {
    data class MovieSnapshot(val data: MovieCache) : HomeCacheSnapshot
    data class TvSnapshot(val data: TvCache) : HomeCacheSnapshot
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val tvRepository: TvRepository,
    private val recommendationRepository: RecommendationRepository,
    private val userListRepository: UserListRepository,
    private val connectivityObserver: ConnectivityObserver,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private var searchJob: Job? = null
    private var offlineMessageJob: Job? = null
    private val isTvModeFlow = MutableStateFlow(false)

    init {
        observeCache()
        observeLastSyncedAt()
        observeTvShowsForDice()
        observeSavedLists()
        // İlk açılışta (ve her yeniden bağlantı kurulduğunda) taze veri çek.
        // Offline'ken observeCache() zaten Room'daki son senkronize veriyi
        // anında gösteriyor, bu yüzden burada beklemeye gerek yok.
        viewModelScope.launch {
            connectivityObserver.isOnline.distinctUntilChanged().collect { online ->
                if (online) {
                    launch { userListRepository.syncFavoritesAndWatchlist() }
                    refreshCurrentMode()
                }
            }
        }
    }

    fun loadMovies() = switchMode(isTv = false)

    fun loadTvShows() = switchMode(isTv = true)

    private fun switchMode(isTv: Boolean) {
        if (isTvModeFlow.value == isTv) return
        isTvModeFlow.value = isTv
        _uiState.value = _uiState.value.copy(isTvMode = isTv, errorMessage = null)
        viewModelScope.launch { refreshCurrentMode() }
    }

    private fun observeCache() {
        isTvModeFlow
            .flatMapLatest { isTv -> if (isTv) tvCacheFlow() else movieCacheFlow() }
            .onEach { snapshot ->
                _uiState.value = when (snapshot) {
                    is HomeCacheSnapshot.MovieSnapshot -> {
                        val data = snapshot.data
                        val hasContent = data.popular.isNotEmpty()
                        val todayStr = java.time.LocalDate.now().toString()
                        _uiState.value.copy(
                            isTvMode = false,
                            isLoading = _uiState.value.isLoading && !hasContent,
                            errorMessage = if (hasContent) null else _uiState.value.errorMessage,
                            featuredMovie = data.featured,
                            featuredTvShow = null,
                            popularMovies = data.popular,
                            topRatedMovies = data.topRated,
                            upcomingMovies = data.upcoming
                                .filter { it.releaseDateStr.isEmpty() || it.releaseDateStr >= todayStr }
                                .map { it.copy(isReminderSet = it.id in _uiState.value.reminderMovieIds) },
                            onAirTvShows = emptyList(),
                            recommendedMovies = data.recommended,
                            categories = data.categories,
                        )
                    }
                    is HomeCacheSnapshot.TvSnapshot -> {
                        val data = snapshot.data
                        val hasContent = data.popular.isNotEmpty() || data.onAir.isNotEmpty()
                        _uiState.value.copy(
                            isTvMode = true,
                            isLoading = _uiState.value.isLoading && !hasContent,
                            errorMessage = if (hasContent) null else _uiState.value.errorMessage,
                            featuredMovie = null,
                            featuredTvShow = data.featured,
                            popularMovies = data.popular,
                            topRatedMovies = emptyList(),
                            upcomingMovies = emptyList(),
                            onAirTvShows = data.onAir,
                            recommendedMovies = data.recommended,
                            categories = data.categories,
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeLastSyncedAt() {
        combine(connectivityObserver.isOnline, userPreferencesRepository.userPreferences) { online, prefs ->
            online to prefs.homeLastSyncedAt
        }.onEach { (online, lastSyncedAt) ->
            _uiState.value = _uiState.value.copy(isOffline = !online, lastSyncedAt = lastSyncedAt)
        }.launchIn(viewModelScope)
    }

    private fun observeTvShowsForDice() {
        combine(tvRepository.observePopularTvShows(), tvRepository.observeOnAirTvShows()) { popular, onAir ->
            (popular + onAir).map { it.toHomeMovie() }.distinctBy { it.id }
        }.onEach { tvList ->
            _uiState.value = _uiState.value.copy(popularTvShows = tvList)
        }.launchIn(viewModelScope)
    }

    private fun observeSavedLists() {
        userListRepository.observeFavorites().onEach { favorites ->
            _uiState.value = _uiState.value.copy(
                favoriteMovieIds = favorites.filter { it.mediaType == "movie" }.map { it.id }.toSet(),
                favoriteTvIds = favorites.filter { it.mediaType == "tv" }.map { it.id }.toSet(),
            )
        }.launchIn(viewModelScope)
        userListRepository.observeWatchlist().onEach { watchlist ->
            _uiState.value = _uiState.value.copy(
                watchlistMovieIds = watchlist.filter { it.mediaType == "movie" }.map { it.id }.toSet(),
                watchlistTvIds = watchlist.filter { it.mediaType == "tv" }.map { it.id }.toSet(),
            )
        }.launchIn(viewModelScope)
    }

    fun toggleFavorite(movie: Movie, mediaType: String = "movie") {
        val isTv = mediaType == "tv"
        val currentIds = if (isTv) _uiState.value.favoriteTvIds else _uiState.value.favoriteMovieIds
        val isFav = movie.id in currentIds
        updateFavoriteIds(isTv) { ids -> if (isFav) ids - movie.id else ids + movie.id }
        viewModelScope.launch {
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
                updateFavoriteIds(isTv) { ids -> if (isFav) ids + movie.id else ids - movie.id }
                showWriteFailure(error)
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
        val isSaved = featured.id in _uiState.value.watchlistMovieIds
        updateWatchlistIds(isTv = false) { ids -> if (isSaved) ids - featured.id else ids + featured.id }
        viewModelScope.launch {
            val result = if (isSaved) {
                userListRepository.removeFromWatchlist(featured.id)
            } else {
                userListRepository.addToWatchlist(
                    SavedMovie(id = featured.id, title = featured.title, posterUrl = featured.posterUrl, rating = featured.rating, year = featured.year),
                )
            }
            result.onFailure { error ->
                updateWatchlistIds(isTv = false) { ids -> if (isSaved) ids + featured.id else ids - featured.id }
                showWriteFailure(error)
            }
        }
    }

    fun toggleFeaturedTvWatchlist(featured: FeaturedTvShow) {
        val isSaved = featured.id in _uiState.value.watchlistTvIds
        updateWatchlistIds(isTv = true) { ids -> if (isSaved) ids - featured.id else ids + featured.id }
        viewModelScope.launch {
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
                updateWatchlistIds(isTv = true) { ids -> if (isSaved) ids + featured.id else ids - featured.id }
                showWriteFailure(error)
            }
        }
    }

    private fun updateFavoriteIds(isTv: Boolean, transform: (Set<Int>) -> Set<Int>) {
        _uiState.value = if (isTv) {
            _uiState.value.copy(favoriteTvIds = transform(_uiState.value.favoriteTvIds))
        } else {
            _uiState.value.copy(favoriteMovieIds = transform(_uiState.value.favoriteMovieIds))
        }
    }

    private fun updateWatchlistIds(isTv: Boolean, transform: (Set<Int>) -> Set<Int>) {
        _uiState.value = if (isTv) {
            _uiState.value.copy(watchlistTvIds = transform(_uiState.value.watchlistTvIds))
        } else {
            _uiState.value.copy(watchlistMovieIds = transform(_uiState.value.watchlistMovieIds))
        }
    }

    /**
     * Yazma hatasında iyimser UI değişikliği geri alınır ve kullanıcıya bilgi
     * verilir. Offline hatasıyla sınırlı DEĞİL: Firestore başka bir sebeple de
     * reddedebilir — geri alınmazsa ikon "kaydedildi" gösterip yazma
     * gerçekleşmemiş olurdu. Mesaj 2.5 sn sonra otomatik temizlenir.
     */
    private fun showWriteFailure(error: Throwable) {
        _uiState.value = _uiState.value.copy(
            offlineActionMessage = if (error is OfflineWriteException) error.message else GENERIC_WRITE_FAILURE_MESSAGE,
        )
        offlineMessageJob?.cancel()
        offlineMessageJob = viewModelScope.launch {
            delay(2500)
            _uiState.value = _uiState.value.copy(offlineActionMessage = null)
        }
    }

    private fun movieCacheFlow(): kotlinx.coroutines.flow.Flow<HomeCacheSnapshot> {
        val core = combine(
            repository.observePopularMovies(),
            repository.observeTopRatedMovies(),
            repository.observeUpcomingMovies(),
            repository.observeFeaturedMovie(),
        ) { popular, topRated, upcoming, featured ->
            MovieCoreCache(popular, topRated, upcoming, featured)
        }
        val extra = combine(
            recommendationRepository.observeRecommendedMovies(),
            repository.observeCategories(),
        ) { recommended, categories -> recommended to categories }

        return combine(core, extra) { core2, (recommended, categories) ->
            HomeCacheSnapshot.MovieSnapshot(
                MovieCache(
                    popular = core2.popular,
                    topRated = core2.topRated,
                    upcoming = core2.upcoming,
                    featured = core2.featured,
                    recommended = recommended,
                    categories = categories,
                ),
            )
        }
    }

    private fun tvCacheFlow(): kotlinx.coroutines.flow.Flow<HomeCacheSnapshot> = combine(
        tvRepository.observePopularTvShows(),
        tvRepository.observeOnAirTvShows(),
        tvRepository.observeFeaturedTvShow(),
        recommendationRepository.observeRecommendedTvShows(),
        tvRepository.observeTvCategories(),
    ) { popular, onAir, featured, recommended, categories ->
        HomeCacheSnapshot.TvSnapshot(
            TvCache(
                popular = popular.map { it.toHomeMovie() },
                onAir = onAir.map { it.toHomeMovie() },
                featured = featured,
                recommended = recommended.map { it.toHomeMovie() },
                categories = categories,
            ),
        )
    }

    private suspend fun refreshCurrentMode() {
        _uiState.value = _uiState.value.copy(isSyncing = true)
        val results = if (isTvModeFlow.value) {
            coroutineScope {
                launch { repository.refreshPopularMovies() }
                // "Yakında Yayınlanacak Diziler" Room'da cache'lenmiyor; TV modu
                // her yenilendiğinde (mod değişimi + yeniden bağlanma) ağdan çekilir.
                launch {
                    _uiState.value = _uiState.value.copy(
                        upcomingTvShows = tvRepository.getUpcomingTvShows().getOrDefault(emptyList()),
                    )
                }
                listOf(
                    async { tvRepository.refreshPopularTvShows() },
                    async { tvRepository.refreshOnAirTvShows() },
                    async { tvRepository.refreshFeaturedTvShow() },
                    async { tvRepository.refreshTvCategories() },
                    async { recommendationRepository.refreshTvRecommendations() },
                ).awaitAll()
            }
        } else {
            coroutineScope {
                launch { tvRepository.refreshPopularTvShows() }
                launch { tvRepository.refreshOnAirTvShows() }
                listOf(
                    async { repository.refreshPopularMovies() },
                    async { repository.refreshTopRatedMovies() },
                    async { repository.refreshUpcomingMovies() },
                    async { repository.refreshFeaturedMovie() },
                    async { repository.refreshCategories() },
                    async { recommendationRepository.refreshRecommendations() },
                ).awaitAll()
            }
        }

        val anySuccess = results.any { it is SyncResult.Success }
        if (anySuccess) {
            userPreferencesRepository.setHomeLastSyncedAt(System.currentTimeMillis())
        }

        val hasCachedContent = _uiState.value.popularMovies.isNotEmpty() ||
            _uiState.value.onAirTvShows.isNotEmpty()

        _uiState.value = _uiState.value.copy(
            isSyncing = false,
            isLoading = false,
            errorMessage = if (!anySuccess && !hasCachedContent) {
                if (_uiState.value.isTvMode) {
                    "Diziler yüklenemedi. İnternet bağlantınızı kontrol edin."
                } else {
                    "Filmler yüklenemedi. İnternet bağlantınızı kontrol edin."
                }
            } else {
                null
            },
        )
    }

    /**
     * Kullanici yazmayi bitirdikten ~400ms sonra aramayi tetikler (debounce).
     * Her yeni harf girisinde onceki bekleyen arama iptal edilir, gereksiz
     * TMDB istegi atilmaz.
     */
    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchJob?.cancel()

        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(searchSuggestions = emptyList(), isSearching = false)
            return
        }

        searchJob = viewModelScope.launch {
            delay(400)
            _uiState.value = _uiState.value.copy(isSearching = true)
            val result = repository.searchMulti(query)
            _uiState.value = _uiState.value.copy(
                isSearching = false,
                searchSuggestions = result.getOrDefault(emptyList()).take(4),
            )
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.value = _uiState.value.copy(searchQuery = "", searchSuggestions = emptyList(), isSearching = false)
    }

    fun loadReminderState(context: android.content.Context) {
        val reminderIds = com.arda.cineverse.notifications.UpcomingReminderManager.getReminderMovieIds(context)
        _uiState.value = _uiState.value.copy(
            reminderMovieIds = reminderIds,
            upcomingMovies = _uiState.value.upcomingMovies.map { it.copy(isReminderSet = it.id in reminderIds) },
            upcomingTvShows = _uiState.value.upcomingTvShows.map { it.copy(isReminderSet = it.id in reminderIds) },
        )
    }

    fun toggleUpcomingReminder(context: android.content.Context, movie: UpcomingMovie): Boolean {
        val isSet = com.arda.cineverse.notifications.UpcomingReminderManager.toggleReminder(
            context,
            movie.id,
            movie.title,
            movie.releaseDateStr,
        )
        val currentReminders = _uiState.value.reminderMovieIds.toMutableSet()
        if (isSet) currentReminders.add(movie.id) else currentReminders.remove(movie.id)

        _uiState.value = _uiState.value.copy(
            reminderMovieIds = currentReminders,
            upcomingMovies = _uiState.value.upcomingMovies.map {
                if (it.id == movie.id) it.copy(isReminderSet = isSet) else it
            },
            upcomingTvShows = _uiState.value.upcomingTvShows.map {
                if (it.id == movie.id) it.copy(isReminderSet = isSet) else it
            },
        )
        return isSet
    }
}

private data class MovieCoreCache(
    val popular: List<Movie>,
    val topRated: List<Movie>,
    val upcoming: List<UpcomingMovie>,
    val featured: FeaturedMovie?,
)
