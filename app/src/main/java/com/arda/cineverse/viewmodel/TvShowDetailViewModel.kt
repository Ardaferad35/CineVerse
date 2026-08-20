package com.arda.cineverse.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arda.cineverse.data.common.GENERIC_WRITE_FAILURE_MESSAGE
import com.arda.cineverse.data.common.OfflineWriteException
import com.arda.cineverse.data.model.SavedMovie
import com.arda.cineverse.data.model.TvShow
import com.arda.cineverse.data.model.TvShowDetail
import com.arda.cineverse.data.repository.RecommendationRepository
import com.arda.cineverse.data.repository.TvRepository
import com.arda.cineverse.data.repository.UserListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.arda.cineverse.data.local.entity.TvEpisodeProgressEntity
import com.arda.cineverse.data.model.TvEpisode
import com.arda.cineverse.data.model.TvEpisodeProgress
import com.arda.cineverse.data.model.TvSeasonDetail
import com.arda.cineverse.data.model.TvSeasonSummary

data class TvShowDetailUiState(
    val isLoading: Boolean = true,
    val tvShow: TvShowDetail? = null,
    val isFavorite: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val offlineMessage: String? = null,
    val isOfflineFallback: Boolean = false,
    val selectedSeasonNumber: Int = 1,
    val selectedSeasonDetail: TvSeasonDetail? = null,
    val isLoadingSeasonDetail: Boolean = false,
    val watchedEpisodesMap: Map<String, Boolean> = emptyMap(),
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

@HiltViewModel
class TvShowDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tvRepository: TvRepository,
    private val userListRepository: UserListRepository,
    private val recommendationRepository: RecommendationRepository,
) : ViewModel() {

    // "tvId", CineVerseNavGraph'taki TV_DETAIL rotasının nav argument adıyla
    // birebir aynı olmalı — Hilt bu SavedStateHandle'ı o backstack kaydının
    // argümanlarından otomatik dolduruyor.
    private val tvId: Int = checkNotNull(savedStateHandle["tvId"]) { "tvId eksik" }

    private val _uiState = MutableStateFlow(TvShowDetailUiState())
    val uiState: StateFlow<TvShowDetailUiState> = _uiState

    init {
        load()
        loadUserListStatus()
        observeEpisodeProgress()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, isOfflineFallback = false)
            tvRepository.getTvShowDetailFull(tvId).fold(
                onSuccess = { tvShow ->
                    _uiState.value = _uiState.value.copy(isLoading = false, tvShow = tvShow, isOfflineFallback = false)
                    recommendationRepository.recordTvView(tvShow.id, tvShow.genreIds)
                    
                    val progressList = tvRepository.getEpisodeProgress(tvId)
                    val targetSeason = determineTargetSeason(tvShow.seasons, progressList)
                    selectSeason(targetSeason)
                },
                onFailure = {
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
                            errorMessage = "Dizi bilgileri yüklenemedi. İnternet bağlantınızı kontrol edin.",
                        )
                    }
                },
            )
        }
    }

    private fun determineTargetSeason(
        seasons: List<TvSeasonSummary>,
        progressList: List<TvEpisodeProgressEntity>,
    ): Int {
        val mainSeasons = seasons.filter { it.seasonNumber > 0 }
        if (mainSeasons.isEmpty()) return seasons.firstOrNull()?.seasonNumber ?: 1
        if (progressList.isEmpty()) return mainSeasons.first().seasonNumber

        val watchedBySeason = progressList.filter { it.isWatched }.groupBy { it.seasonNumber }
        if (watchedBySeason.isEmpty()) return mainSeasons.first().seasonNumber

        val maxWatchedSeasonNumber = watchedBySeason.keys.maxOrNull() ?: mainSeasons.first().seasonNumber
        val currentSeasonSummary = mainSeasons.find { it.seasonNumber == maxWatchedSeasonNumber }

        if (currentSeasonSummary != null) {
            val watchedCountInSeason = watchedBySeason[maxWatchedSeasonNumber]?.size ?: 0
            // Eğer en son izlenen sezon tam olarak tamamlanmışsa ve bir sonraki sezon varsa, doğrudan sonraki sezona geç
            if (currentSeasonSummary.episodeCount > 0 && watchedCountInSeason >= currentSeasonSummary.episodeCount) {
                val nextSeason = mainSeasons.find { it.seasonNumber > maxWatchedSeasonNumber }
                if (nextSeason != null) {
                    return nextSeason.seasonNumber
                }
            }
        }

        return maxWatchedSeasonNumber
    }

    private fun observeEpisodeProgress() {
        viewModelScope.launch {
            tvRepository.observeEpisodeProgress(tvId).collect { list ->
                val map = list.associate { "s${it.seasonNumber}_e${it.episodeNumber}" to it.isWatched }
                _uiState.value = _uiState.value.copy(watchedEpisodesMap = map)
            }
        }
    }

    fun selectSeason(seasonNumber: Int) {
        _uiState.value = _uiState.value.copy(selectedSeasonNumber = seasonNumber, isLoadingSeasonDetail = true)
        viewModelScope.launch {
            tvRepository.getTvSeasonDetail(tvId, seasonNumber).fold(
                onSuccess = { detail ->
                    _uiState.value = _uiState.value.copy(selectedSeasonDetail = detail, isLoadingSeasonDetail = false)
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isLoadingSeasonDetail = false)
                },
            )
        }
    }

    fun toggleEpisodeWatched(seasonNumber: Int, episodeNumber: Int) {
        val key = "s${seasonNumber}_e${episodeNumber}"
        val isCurrentlyWatched = _uiState.value.watchedEpisodesMap[key] == true
        val targetWatched = !isCurrentlyWatched
        viewModelScope.launch {
            tvRepository.toggleEpisodeWatched(tvId, seasonNumber, episodeNumber, targetWatched)
            if (targetWatched && !_uiState.value.isSaved) {
                toggleWatchlist()
            }
        }
    }

    fun toggleSeasonWatched(seasonNumber: Int, episodes: List<TvEpisode>) {
        val allWatched = episodes.all { ep ->
            _uiState.value.watchedEpisodesMap["s${seasonNumber}_e${ep.episodeNumber}"] == true
        }
        val targetWatched = !allWatched
        viewModelScope.launch {
            tvRepository.toggleSeasonWatched(tvId, seasonNumber, episodes, targetWatched)
            if (targetWatched && !_uiState.value.isSaved) {
                toggleWatchlist()
            }
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
