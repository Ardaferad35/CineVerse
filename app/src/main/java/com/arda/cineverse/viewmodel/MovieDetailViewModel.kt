package com.arda.cineverse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arda.cineverse.data.model.Movie
import com.arda.cineverse.data.model.MovieDetail
import com.arda.cineverse.data.model.SavedMovie
import com.arda.cineverse.data.repository.MovieRepository
import com.arda.cineverse.data.repository.RecommendationRepository
import com.arda.cineverse.data.repository.UserListRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class MovieDetailUiState(
    val isLoading: Boolean = true,
    val movie: MovieDetail? = null,
    val isFavorite: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    // true ise `movie` TMDB'den tam çekilemedi, Home cache'inden (Room)
    // kurtarılan kısmi bir görünüm — kadro/fragman/benzer filmler eksik.
    val isOfflineFallback: Boolean = false,
)

/** Room'da cache'lenmiş temel film verisinden (kadro/fragman/benzer filmler olmadan) kısmi bir MovieDetail oluşturur. */
private fun Movie.toPartialMovieDetail(): MovieDetail = MovieDetail(
    id = id,
    title = title,
    backdropUrl = null,
    posterUrl = posterUrl,
    tmdbRating = rating,
    year = year,
    durationLabel = "",
    genres = genres,
    genreIds = genreIds,
    overview = overview,
    director = null,
    cast = emptyList(),
    trailerKey = null,
    similarMovies = emptyList(),
)

private fun MovieDetail.toSavedMovie() = SavedMovie(
    id = id,
    title = title,
    posterUrl = posterUrl,
    rating = tmdbRating,
    year = year,
    genreIds = genreIds,
)

class MovieDetailViewModel(
    private val movieId: Int,
    private val movieRepository: MovieRepository = MovieRepository(),
    private val userListRepository: UserListRepository = UserListRepository(),
    private val recommendationRepository: RecommendationRepository = RecommendationRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(MovieDetailUiState())
    val uiState: StateFlow<MovieDetailUiState> = _uiState

    init {
        load()
        loadUserListStatus()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, isOfflineFallback = false)
            movieRepository.getMovieDetailFull(movieId).fold(
                onSuccess = { movie ->
                    _uiState.value = _uiState.value.copy(isLoading = false, movie = movie, isOfflineFallback = false)
                    // Öneri sistemi için: bu filmi "görüntülendi" penceresine ekle.
                    recommendationRepository.recordView(movie.id, movie.genreIds)
                },
                onFailure = {
                    // Tam detay çekilemedi (muhtemelen çevrimdışı) — Home'da
                    // daha önce cache'lenmiş temel film verisiyle kısmi bir
                    // görünüm göstermeyi dene. Kadro/fragman/benzer filmler
                    // bölümleri bu veri boş geldiğinde UI'da zaten gizleniyor.
                    val cachedMovie = movieRepository.getCachedMovie(movieId)
                    if (cachedMovie != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            movie = cachedMovie.toPartialMovieDetail(),
                            isOfflineFallback = true,
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Film bilgileri yüklenemedi. İnternet bağlantınızı kontrol edin.",
                        )
                    }
                },
            )
        }
    }

    private fun loadUserListStatus() {
        viewModelScope.launch {
            val favorite = userListRepository.isFavorite(movieId)
            val saved = userListRepository.isInWatchlist(movieId)
            _uiState.value = _uiState.value.copy(isFavorite = favorite, isSaved = saved)
        }
    }

    fun toggleFavorite() {
        val movie = _uiState.value.movie ?: return
        val newValue = !_uiState.value.isFavorite
        _uiState.value = _uiState.value.copy(isFavorite = newValue)
        viewModelScope.launch {
            if (newValue) {
                userListRepository.addFavorite(movie.toSavedMovie())
                recommendationRepository.recordFavorite(movie.id, movie.genreIds)
            } else {
                userListRepository.removeFavorite(movie.id)
                recommendationRepository.removeFavoriteSignal(movie.id)
            }
        }
    }

    fun toggleWatchlist() {
        val movie = _uiState.value.movie ?: return
        val newValue = !_uiState.value.isSaved
        _uiState.value = _uiState.value.copy(isSaved = newValue)
        viewModelScope.launch {
            if (newValue) {
                userListRepository.addToWatchlist(movie.toSavedMovie())
            } else {
                userListRepository.removeFromWatchlist(movie.id)
            }
        }
    }
}

class MovieDetailViewModelFactory(private val movieId: Int) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MovieDetailViewModel(movieId) as T
    }
}