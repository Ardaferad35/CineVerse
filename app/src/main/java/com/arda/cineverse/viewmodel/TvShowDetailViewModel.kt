package com.arda.cineverse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arda.cineverse.data.common.GENERIC_WRITE_FAILURE_MESSAGE
import com.arda.cineverse.data.common.OfflineWriteException
import com.arda.cineverse.data.model.SavedMovie
import com.arda.cineverse.data.model.TvShow
import com.arda.cineverse.data.model.TvShowDetail
import com.arda.cineverse.data.repository.RecommendationRepository
import com.arda.cineverse.data.repository.TvRepository
import com.arda.cineverse.data.repository.UserListRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class TvShowDetailUiState(
    val isLoading: Boolean = true,
    val tvShow: TvShowDetail? = null,
    val isFavorite: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    // Offline'ken favori/izleme listesi yazma girişimi reddedildiğinde kısa
    // süreliğine gösterilecek mesaj.
    val offlineMessage: String? = null,
    // true ise `tvShow` TMDB'den tam çekilemedi, Home cache'inden (Room)
    // kurtarılan kısmi bir görünüm — kadro/fragman/benzer diziler eksik.
    val isOfflineFallback: Boolean = false,
)

/** Room'da cache'lenmiş temel dizi verisinden (kadro/fragman/benzer diziler olmadan) kısmi bir TvShowDetail oluşturur. */
private fun TvShow.toPartialTvShowDetail(): TvShowDetail = TvShowDetail(
    id = id,
    name = name,
    backdropUrl = null,
    posterUrl = posterUrl,
    tmdbRating = rating,
    year = year,
    seasonsLabel = "",
    episodesLabel = "",
    genres = genres,
    genreIds = genreIds,
    overview = overview,
    createdBy = null,
    cast = emptyList(),
    trailerKey = null,
    similarShows = emptyList(),
)

private fun TvShowDetail.toSavedMovie() = SavedMovie(
    id = id,
    title = name,
    posterUrl = posterUrl,
    rating = tmdbRating,
    year = year,
    genreIds = genreIds,
    mediaType = "tv",
)

class TvShowDetailViewModel(
    private val tvId: Int,
    private val tvRepository: TvRepository = TvRepository.default(),
    private val userListRepository: UserListRepository = UserListRepository.default(),
    private val recommendationRepository: RecommendationRepository = RecommendationRepository.default(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(TvShowDetailUiState())
    val uiState: StateFlow<TvShowDetailUiState> = _uiState

    init {
        load()
        loadUserListStatus()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, isOfflineFallback = false)
            tvRepository.getTvShowDetailFull(tvId).fold(
                onSuccess = { tvShow ->
                    _uiState.value = _uiState.value.copy(isLoading = false, tvShow = tvShow, isOfflineFallback = false)
                    // Öneri sistemi için: bu diziyi "görüntülendi" penceresine ekle.
                    recommendationRepository.recordTvView(tvShow.id, tvShow.genreIds)
                },
                onFailure = {
                    // Tam detay \u00E7ekilemedi (muhtemelen \u00E7evrimd\u0131\u015F\u0131) \u2014 Home'da
                    // daha \u00F6nce cache'lenmi\u015F temel dizi verisiyle k\u0131smi bir
                    // g\u00F6r\u00FCn\u00FCm g\u00F6stermeyi dene (bkz. MovieDetailViewModel.load).
                    val cachedTvShow = tvRepository.getCachedTvShow(tvId)
                    if (cachedTvShow != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            tvShow = cachedTvShow.toPartialTvShowDetail(),
                            isOfflineFallback = true,
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Dizi bilgileri y\u00FCklenemedi. \u0130nternet ba\u011Flant\u0131n\u0131z\u0131 kontrol edin.",
                        )
                    }
                },
            )
        }
    }

    private fun loadUserListStatus() {
        viewModelScope.launch {
            val favorite = userListRepository.isFavorite(tvId, mediaType = "tv")
            val saved = userListRepository.isInWatchlist(tvId, mediaType = "tv")
            _uiState.value = _uiState.value.copy(isFavorite = favorite, isSaved = saved)
        }
    }

    fun toggleFavorite() {
        val tvShow = _uiState.value.tvShow ?: return
        val newValue = !_uiState.value.isFavorite
        _uiState.value = _uiState.value.copy(isFavorite = newValue)
        viewModelScope.launch {
            val result = if (newValue) {
                userListRepository.addFavorite(tvShow.toSavedMovie())
            } else {
                userListRepository.removeFavorite(tvShow.id, mediaType = "tv")
            }
            result.fold(
                onSuccess = {
                    if (newValue) {
                        recommendationRepository.recordTvFavorite(tvShow.id, tvShow.genreIds)
                    } else {
                        recommendationRepository.removeTvFavoriteSignal(tvShow.id)
                    }
                },
                onFailure = { error -> revertOnFailure(error) { _uiState.value = _uiState.value.copy(isFavorite = !newValue) } },
            )
        }
    }

    fun toggleWatchlist() {
        val tvShow = _uiState.value.tvShow ?: return
        val newValue = !_uiState.value.isSaved
        _uiState.value = _uiState.value.copy(isSaved = newValue)
        viewModelScope.launch {
            val result = if (newValue) {
                userListRepository.addToWatchlist(tvShow.toSavedMovie())
            } else {
                userListRepository.removeFromWatchlist(tvShow.id, mediaType = "tv")
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

class TvShowDetailViewModelFactory(private val tvId: Int) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TvShowDetailViewModel(tvId) as T
    }
}
