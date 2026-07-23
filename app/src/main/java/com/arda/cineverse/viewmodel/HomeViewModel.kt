package com.arda.cineverse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arda.cineverse.data.model.FeaturedMovie
import com.arda.cineverse.data.model.Movie
import com.arda.cineverse.data.model.UpcomingMovie
import com.arda.cineverse.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val featuredMovie: FeaturedMovie? = null,
    val popularMovies: List<Movie> = emptyList(),
    val upcomingMovies: List<UpcomingMovie> = emptyList(),
    val errorMessage: String? = null,
)

class HomeViewModel(
    private val repository: MovieRepository = MovieRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

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
                HomeUiState(
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
}