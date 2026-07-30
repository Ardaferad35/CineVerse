package com.arda.cineverse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arda.cineverse.data.common.OfflineWriteException
import com.arda.cineverse.data.model.SavedMovie
import com.arda.cineverse.data.repository.RecommendationRepository
import com.arda.cineverse.data.repository.UserListRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.round

enum class MyListTab { FAVORITES, WATCHLIST }

data class MyListUiState(
    val isLoading: Boolean = true,
    val selectedTab: MyListTab = MyListTab.FAVORITES,
    val favorites: List<SavedMovie> = emptyList(),
    val watchlist: List<SavedMovie> = emptyList(),
    val errorMessage: String? = null,
    // Offline'ken kaldırma girişimi reddedildiğinde kısa süreliğine gösterilecek mesaj.
    val offlineMessage: String? = null,
) {
    val currentList: List<SavedMovie>
        get() = if (selectedTab == MyListTab.FAVORITES) favorites else watchlist

    val averageRating: Double
        get() {
            val list = currentList
            return if (list.isEmpty()) 0.0 else round(list.map { it.rating }.average() * 10) / 10.0
        }
}

class MyListViewModel(
    private val repository: UserListRepository = UserListRepository(),
    private val recommendationRepository: RecommendationRepository = RecommendationRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyListUiState())
    val uiState: StateFlow<MyListUiState> = _uiState

    init {
        loadAll()
    }

    fun loadAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val favResult = repository.getFavorites()
            val watchResult = repository.getWatchlist()
            val favorites = favResult.getOrNull()
            val watchlist = watchResult.getOrNull()

            _uiState.value = if (favorites != null && watchlist != null) {
                _uiState.value.copy(isLoading = false, favorites = favorites, watchlist = watchlist)
            } else {
                _uiState.value.copy(isLoading = false, errorMessage = "Liste yüklenemedi. İnternet bağlantınızı kontrol edin.")
            }
        }
    }

    fun selectTab(tab: MyListTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun removeFromCurrentList(mediaId: Int, mediaType: String = "movie") {
        val isFavTab = _uiState.value.selectedTab == MyListTab.FAVORITES
        val removedItem = _uiState.value.currentList.find { it.id == mediaId && it.mediaType == mediaType } ?: return

        // 1. İyimser Güncelleme: Kart ekrandan tak diye anında kaybolur, yükleme ekranı ÇIKMAZ!
        if (isFavTab) {
            val updatedFavs = _uiState.value.favorites.filterNot { it.id == mediaId && it.mediaType == mediaType }
            _uiState.value = _uiState.value.copy(favorites = updatedFavs)
        } else {
            val updatedWatch = _uiState.value.watchlist.filterNot { it.id == mediaId && it.mediaType == mediaType }
            _uiState.value = _uiState.value.copy(watchlist = updatedWatch)
        }

        // 2. Arka planda sessizce silme ve Öneri Sinyallerini derhal temizleme
        viewModelScope.launch {
            val result = if (isFavTab) repository.removeFavorite(mediaId, mediaType) else repository.removeFromWatchlist(mediaId, mediaType)
            result.fold(
                onSuccess = {
                    if (isFavTab) {
                        if (mediaType == "tv") {
                            recommendationRepository.removeTvFavoriteSignal(mediaId)
                            recommendationRepository.refreshTvRecommendations()
                        } else {
                            recommendationRepository.removeFavoriteSignal(mediaId)
                            recommendationRepository.refreshRecommendations()
                        }
                    }
                },
                onFailure = { error ->
                    // Offline'ken silme reddedildi — kartı geri getir, kullanıcı asla
                    // sessizce kaybolan bir öğeyle baş başa kalmasın.
                    if (error is OfflineWriteException) {
                        if (isFavTab) {
                            _uiState.value = _uiState.value.copy(favorites = _uiState.value.favorites + removedItem, offlineMessage = error.message)
                        } else {
                            _uiState.value = _uiState.value.copy(watchlist = _uiState.value.watchlist + removedItem, offlineMessage = error.message)
                        }
                    }
                },
            )
        }
    }

    fun clearOfflineMessage() {
        _uiState.value = _uiState.value.copy(offlineMessage = null)
    }
}