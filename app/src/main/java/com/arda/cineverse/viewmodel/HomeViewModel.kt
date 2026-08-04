package com.arda.cineverse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arda.cineverse.data.common.SyncResult
import com.arda.cineverse.data.connectivity.ConnectivityObserver
import com.arda.cineverse.data.local.datastore.UserPreferencesRepository
import com.arda.cineverse.data.model.Category
import com.arda.cineverse.data.model.FeaturedMovie
import com.arda.cineverse.data.model.FeaturedTvShow
import com.arda.cineverse.data.model.Movie
import com.arda.cineverse.data.model.SearchSuggestion
import com.arda.cineverse.data.model.UpcomingMovie
import com.arda.cineverse.data.remote.toHomeMovie
import com.arda.cineverse.data.repository.MovieRepository
import com.arda.cineverse.data.repository.RecommendationRepository
import com.arda.cineverse.data.repository.TvRepository
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
    private val connectivityObserver: ConnectivityObserver,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private var searchJob: Job? = null
    private val isTvModeFlow = MutableStateFlow(false)

    init {
        observeCache()
        observeLastSyncedAt()
        observeTvShowsForDice()
        // İlk açılışta (ve her yeniden bağlantı kurulduğunda) taze veri çek.
        // Offline'ken observeCache() zaten Room'daki son senkronize veriyi
        // anında gösteriyor, bu yüzden burada beklemeye gerek yok.
        viewModelScope.launch {
            connectivityObserver.isOnline.distinctUntilChanged().collect { online ->
                if (online) refreshCurrentMode()
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
                        _uiState.value.copy(
                            isTvMode = false,
                            isLoading = _uiState.value.isLoading && !hasContent,
                            errorMessage = if (hasContent) null else _uiState.value.errorMessage,
                            featuredMovie = data.featured,
                            featuredTvShow = null,
                            popularMovies = data.popular,
                            topRatedMovies = data.topRated,
                            upcomingMovies = data.upcoming,
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
}

private data class MovieCoreCache(
    val popular: List<Movie>,
    val topRated: List<Movie>,
    val upcoming: List<UpcomingMovie>,
    val featured: FeaturedMovie?,
)
