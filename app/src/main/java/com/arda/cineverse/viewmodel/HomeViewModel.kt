package com.arda.cineverse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private data class Quint<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val isTvMode: Boolean = false,
    val featuredMovie: FeaturedMovie? = null,
    val featuredTvShow: FeaturedTvShow? = null,
    val popularMovies: List<Movie> = emptyList(),
    val upcomingMovies: List<UpcomingMovie> = emptyList(),
    val onAirTvShows: List<Movie> = emptyList(),
    val recommendedMovies: List<Movie> = emptyList(),
    val categories: List<Category> = emptyList(),
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchSuggestions: List<SearchSuggestion> = emptyList(),
)

class HomeViewModel(
    private val repository: MovieRepository = MovieRepository(),
    private val tvRepository: TvRepository = TvRepository(),
    private val recommendationRepository: RecommendationRepository = RecommendationRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private var searchJob: Job? = null

    init {
        loadMovies()
    }

    fun loadMovies() {
        loadHome(isTvMode = false)
    }

    fun loadTvShows() {
        loadHome(isTvMode = true)
    }

    private fun loadHome(isTvMode: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, isTvMode = isTvMode, errorMessage = null)
            if (isTvMode) {
                loadTvHome()
            } else {
                loadMovieHome()
            }
        }
    }

    private suspend fun loadMovieHome() {
        val (popular, upcoming, featured, recommended, categories) = coroutineScope {
            val popularDeferred = async { repository.getPopularMovies() }
            val upcomingDeferred = async { repository.getUpcomingMovies() }
            val featuredDeferred = async { repository.getFeaturedMovie() }
            val recommendedDeferred = async { recommendationRepository.getRecommendations() }
            val categoriesDeferred = async { repository.getAllGenres() }

            Quint(
                popularDeferred.await().getOrNull(),
                upcomingDeferred.await().getOrNull(),
                featuredDeferred.await().getOrNull(),
                recommendedDeferred.await().getOrDefault(emptyList()),
                categoriesDeferred.await().getOrDefault(emptyList()),
            )
        }

        _uiState.value = if (popular != null && upcoming != null) {
            _uiState.value.copy(
                isLoading = false,
                isTvMode = false,
                featuredMovie = featured,
                featuredTvShow = null,
                popularMovies = popular,
                upcomingMovies = upcoming,
                onAirTvShows = emptyList(),
                recommendedMovies = recommended,
                categories = categories,
            )
        } else {
            _uiState.value.copy(
                isLoading = false,
                isTvMode = false,
                errorMessage = "Filmler y\u00FCklenemedi. \u0130nternet ba\u011Flant\u0131n\u0131z\u0131 kontrol edin.",
            )
        }
    }

    private suspend fun loadTvHome() {
        val (popular, onAir, featured, recommended, categories) = coroutineScope {
            val popularDeferred = async { tvRepository.getPopularTvShows() }
            val onAirDeferred = async { tvRepository.getOnTheAirTvShows() }
            val featuredDeferred = async { tvRepository.getFeaturedTvShow() }
            val recommendedDeferred = async { recommendationRepository.getTvRecommendations() }
            val categoriesDeferred = async { tvRepository.getAllTvGenres() }

            Quint(
                popularDeferred.await().getOrNull(),
                onAirDeferred.await().getOrNull(),
                featuredDeferred.await().getOrNull(),
                recommendedDeferred.await().getOrDefault(emptyList()),
                categoriesDeferred.await().getOrDefault(emptyList()),
            )
        }

        _uiState.value = if (popular != null && onAir != null) {
            _uiState.value.copy(
                isLoading = false,
                isTvMode = true,
                featuredMovie = null,
                featuredTvShow = featured,
                popularMovies = popular.map { it.toHomeMovie() },
                upcomingMovies = emptyList(),
                onAirTvShows = onAir.map { it.toHomeMovie() },
                recommendedMovies = recommended.map { it.toHomeMovie() },
                categories = categories,
            )
        } else {
            _uiState.value.copy(
                isLoading = false,
                isTvMode = true,
                errorMessage = "Diziler y\u00FCklenemedi. \u0130nternet ba\u011Flant\u0131n\u0131z\u0131 kontrol edin.",
            )
        }
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
