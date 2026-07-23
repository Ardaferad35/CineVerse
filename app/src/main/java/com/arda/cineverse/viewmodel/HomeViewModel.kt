package com.arda.cineverse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arda.cineverse.data.model.FeaturedMovie
import com.arda.cineverse.data.model.Movie
import com.arda.cineverse.data.model.SearchSuggestion
import com.arda.cineverse.data.model.UpcomingMovie
import com.arda.cineverse.data.repository.MovieRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val featuredMovie: FeaturedMovie? = null,
    val popularMovies: List<Movie> = emptyList(),
    val upcomingMovies: List<UpcomingMovie> = emptyList(),
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchSuggestions: List<SearchSuggestion> = emptyList(),
)

class HomeViewModel(
    private val repository: MovieRepository = MovieRepository(),
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

            val popularResult = repository.getPopularMovies()
            val upcomingResult = repository.getUpcomingMovies()
            val featuredResult = repository.getFeaturedMovie()

            val popular = popularResult.getOrNull()
            val upcoming = upcomingResult.getOrNull()
            val featured = featuredResult.getOrNull()

            _uiState.value = if (popular != null && upcoming != null) {
                _uiState.value.copy(
                    isLoading = false,
                    featuredMovie = featured,
                    popularMovies = popular,
                    upcomingMovies = upcoming,
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