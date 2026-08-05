package com.arda.cineverse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arda.cineverse.data.model.ReelItem
import com.arda.cineverse.data.model.SavedMovie
import com.arda.cineverse.data.repository.MovieReelsRepository
import com.arda.cineverse.data.repository.RecommendationRepository
import com.arda.cineverse.data.repository.UserListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MovieReelsUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val reels: List<ReelItem> = emptyList(),
    val currentPage: Int = 1,
    val favoriteKeys: Set<String> = emptySet(),
    val watchlistKeys: Set<String> = emptySet(),
    val errorMessage: String? = null,
)

@HiltViewModel
class MovieReelsViewModel @Inject constructor(
    private val reelsRepository: MovieReelsRepository,
    private val userListRepository: UserListRepository,
    private val recommendationRepository: RecommendationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MovieReelsUiState())
    val uiState: StateFlow<MovieReelsUiState> = _uiState

    init {
        loadReels()
        observeSavedStatuses()
    }

    fun loadReels() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, currentPage = 1)
            reelsRepository.getMovieReels(page = 1).fold(
                onSuccess = { items ->
                    _uiState.value = _uiState.value.copy(isLoading = false, reels = items)
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Fragmanlar yüklenemedi. İnternet bağlantınızı kontrol edin.",
                    )
                },
            )
        }
    }

    fun loadMoreReels() {
        if (_uiState.value.isLoadingMore) return
        val nextPage = _uiState.value.currentPage + 1
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            reelsRepository.getMovieReels(page = nextPage).fold(
                onSuccess = { newItems ->
                    val existingIds = _uiState.value.reels.map { "${it.mediaType}_${it.id}" }.toSet()
                    val filteredNewItems = newItems.filter { "${it.mediaType}_${it.id}" !in existingIds }
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        currentPage = nextPage,
                        reels = _uiState.value.reels + filteredNewItems,
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isLoadingMore = false)
                },
            )
        }
    }

    private fun observeSavedStatuses() {
        viewModelScope.launch {
            userListRepository.observeFavorites().collect { favorites ->
                val favKeys = favorites.map { "${it.mediaType}_${it.id}" }.toSet()
                _uiState.value = _uiState.value.copy(favoriteKeys = favKeys)
            }
        }
        viewModelScope.launch {
            userListRepository.observeWatchlist().collect { watchlist ->
                val watchKeys = watchlist.map { "${it.mediaType}_${it.id}" }.toSet()
                _uiState.value = _uiState.value.copy(watchlistKeys = watchKeys)
            }
        }
    }

    fun toggleFavorite(reel: ReelItem) {
        val key = "${reel.mediaType}_${reel.id}"
        val isFav = key in _uiState.value.favoriteKeys
        viewModelScope.launch {
            val savedMovie = SavedMovie(
                id = reel.id,
                title = reel.title,
                posterUrl = reel.posterUrl,
                rating = reel.rating,
                year = reel.year,
                genreIds = reel.genreIds,
                mediaType = reel.mediaType,
            )
            if (isFav) {
                userListRepository.removeFavorite(reel.id, mediaType = reel.mediaType)
                if (reel.mediaType == "tv") {
                    recommendationRepository.removeTvFavoriteSignal(reel.id)
                } else {
                    recommendationRepository.removeFavoriteSignal(reel.id)
                }
            } else {
                userListRepository.addFavorite(savedMovie)
                if (reel.mediaType == "tv") {
                    recommendationRepository.recordTvFavorite(reel.id, reel.genreIds)
                } else {
                    recommendationRepository.recordFavorite(reel.id, reel.genreIds)
                }
            }
        }
    }

    fun toggleWatchlist(reel: ReelItem) {
        val key = "${reel.mediaType}_${reel.id}"
        val isSaved = key in _uiState.value.watchlistKeys
        viewModelScope.launch {
            val savedMovie = SavedMovie(
                id = reel.id,
                title = reel.title,
                posterUrl = reel.posterUrl,
                rating = reel.rating,
                year = reel.year,
                genreIds = reel.genreIds,
                mediaType = reel.mediaType,
            )
            if (isSaved) {
                userListRepository.removeFromWatchlist(reel.id, mediaType = reel.mediaType)
            } else {
                userListRepository.addToWatchlist(savedMovie)
            }
        }
    }

    fun addBothToFavoritesAndWatchlist(reel: ReelItem) {
        val key = "${reel.mediaType}_${reel.id}"
        val isFav = key in _uiState.value.favoriteKeys
        val isWatch = key in _uiState.value.watchlistKeys

        viewModelScope.launch {
            val savedMovie = SavedMovie(
                id = reel.id,
                title = reel.title,
                posterUrl = reel.posterUrl,
                rating = reel.rating,
                year = reel.year,
                genreIds = reel.genreIds,
                mediaType = reel.mediaType,
            )
            if (!isFav) {
                userListRepository.addFavorite(savedMovie)
                if (reel.mediaType == "tv") {
                    recommendationRepository.recordTvFavorite(reel.id, reel.genreIds)
                } else {
                    recommendationRepository.recordFavorite(reel.id, reel.genreIds)
                }
            }
            if (!isWatch) {
                userListRepository.addToWatchlist(savedMovie)
            }
        }
    }
}
