package com.arda.cineverse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arda.cineverse.data.model.FeaturedMovie
import com.arda.cineverse.data.model.Movie
import com.arda.cineverse.data.model.SearchSuggestion
import com.arda.cineverse.data.model.UpcomingMovie
import com.arda.cineverse.data.repository.MovieRepository
import com.arda.cineverse.data.repository.RecommendationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

data class HomeUiState(
    val isLoading: Boolean = true,
    val featuredMovie: FeaturedMovie? = null,
    val popularMovies: List<Movie> = emptyList(),
    val upcomingMovies: List<UpcomingMovie> = emptyList(),
    val recommendedMovies: List<Movie> = emptyList(),
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchSuggestions: List<SearchSuggestion> = emptyList(),
)

class HomeViewModel(
    private val repository: MovieRepository = MovieRepository(),
    private val recommendationRepository: RecommendationRepository = RecommendationRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private var searchJob: Job? = null

    init {
        loadMovies()
    }

    fun loadMovies() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val (popular, upcoming, featured, recommended) = coroutineScope {
                val popularDeferred = async { repository.getPopularMovies() }
                val upcomingDeferred = async { repository.getUpcomingMovies() }
                val featuredDeferred = async { repository.getFeaturedMovie() }
                val recommendedDeferred = async { recommendationRepository.getRecommendations() }

                Quad(
                    popularDeferred.await().getOrNull(),
                    upcomingDeferred.await().getOrNull(),
                    featuredDeferred.await().getOrNull(),
                    recommendedDeferred.await().getOrDefault(emptyList()),
                )
            }

            _uiState.value = if (popular != null && upcoming != null) {
                _uiState.value.copy(
                    isLoading = false,
                    featuredMovie = featured,
                    popularMovies = popular,
                    upcomingMovies = upcoming,
                    recommendedMovies = recommended,
                )
            } else {
                _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Filmler yüklenemedi. İnternet bağlantınızı kontrol edin.",
                )
            }
        }
    }

    /**
     * Kullanıcı yazmayı bitirdikten ~400ms sonra aramayı tetikler (debounce).
     * Her yeni harf girişinde önceki bekleyen arama iptal edilir, gereksiz
     * TMDB isteği atılmaz.
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