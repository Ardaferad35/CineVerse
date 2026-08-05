package com.arda.cineverse.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arda.cineverse.data.common.GENERIC_WRITE_FAILURE_MESSAGE
import com.arda.cineverse.data.common.OfflineWriteException
import com.arda.cineverse.data.model.Movie
import com.arda.cineverse.data.model.MovieDetail
import com.arda.cineverse.data.model.SavedMovie
import com.arda.cineverse.data.repository.MovieRepository
import com.arda.cineverse.data.repository.RecommendationRepository
import com.arda.cineverse.data.repository.UserListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MovieDetailUiState(
    val isLoading: Boolean = true,
    val movie: MovieDetail? = null,
    val isFavorite: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    // true ise `movie` TMDB'den tam çekilemedi, Home cache'inden (Room)
    // kurtarılan kısmi bir görünüm — kadro/fragman/benzer filmler eksik.
    val isOfflineFallback: Boolean = false,
    // Offline'ken favori/izleme listesi yazma girişimi reddedildiğinde kısa
    // süreliğine gösterilecek mesaj (errorMessage'dan farklı: o tam ekran
    // yükleme hatası içindir, bu geçici bir bildirimdir).
    val offlineMessage: String? = null,
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

@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val movieRepository: MovieRepository,
    private val userListRepository: UserListRepository,
    private val recommendationRepository: RecommendationRepository,
) : ViewModel() {

    // "movieId", CineVerseNavGraph'taki MOVIE_DETAIL rotasının nav argument
    // adıyla birebir aynı olmalı — Hilt bu SavedStateHandle'ı o backstack
    // kaydının argümanlarından otomatik dolduruyor.
    private val movieId: Int = checkNotNull(savedStateHandle["movieId"]) { "movieId eksik" }

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
            val result = if (newValue) {
                userListRepository.addFavorite(movie.toSavedMovie())
            } else {
                userListRepository.removeFavorite(movie.id)
            }
            result.fold(
                onSuccess = {
                    if (newValue) {
                        recommendationRepository.recordFavorite(movie.id, movie.genreIds)
                    } else {
                        recommendationRepository.removeFavoriteSignal(movie.id)
                    }
                },
                onFailure = { error -> revertOnFailure(error) { _uiState.value = _uiState.value.copy(isFavorite = !newValue) } },
            )
        }
    }

    fun toggleWatchlist() {
        val movie = _uiState.value.movie ?: return
        val newValue = !_uiState.value.isSaved
        _uiState.value = _uiState.value.copy(isSaved = newValue)
        viewModelScope.launch {
            val result = if (newValue) {
                userListRepository.addToWatchlist(movie.toSavedMovie())
            } else {
                userListRepository.removeFromWatchlist(movie.id)
            }
            result.onFailure { error ->
                revertOnFailure(error) { _uiState.value = _uiState.value.copy(isSaved = !newValue) }
            }
        }
    }

    /**
     * Yazma hatasında iyimser UI değişikliğini geri alır ve kullanıcıya kısa
     * süreli bir mesaj gösterir. Offline hatasıyla sınırlı DEĞİL: Firestore
     * başka bir sebeple de reddedebilir (izin/zaman aşımı) — geri alınmazsa
     * ikon "kaydedildi" gösterip yazma gerçekleşmemiş olurdu.
     */
    private fun revertOnFailure(error: Throwable, revert: () -> Unit) {
        revert()
        _uiState.value = _uiState.value.copy(
            offlineMessage = if (error is OfflineWriteException) error.message else GENERIC_WRITE_FAILURE_MESSAGE,
        )
    }

    fun clearOfflineMessage() {
        _uiState.value = _uiState.value.copy(offlineMessage = null)
    }
}