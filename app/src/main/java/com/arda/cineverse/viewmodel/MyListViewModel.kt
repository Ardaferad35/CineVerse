package com.arda.cineverse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arda.cineverse.data.common.GENERIC_WRITE_FAILURE_MESSAGE
import com.arda.cineverse.data.common.OfflineWriteException
import com.arda.cineverse.data.model.SavedMovie
import com.arda.cineverse.data.repository.CommentRepository
import com.arda.cineverse.data.repository.RecommendationRepository
import com.arda.cineverse.data.repository.UserListRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.round

enum class MyListTab { FAVORITES, WATCHLIST }

enum class MyListSortCriterion(val label: String) {
    IMDB_RATING("IMDb Puanı"),
    APP_RATING("Uygulama İçi Puan"),
    YEAR("Yayın Yılı"),
    TITLE("İsme Göre (A-Z)"),
}

enum class MyListSortOrder(val label: String) {
    DESCENDING("Azalan (Yüksek / En Yeni)"),
    ASCENDING("Artan (Düşük / En Eski)"),
}

data class MyListUiState(
    val isLoading: Boolean = true,
    val selectedTab: MyListTab = MyListTab.FAVORITES,
    val favorites: List<SavedMovie> = emptyList(),
    val watchlist: List<SavedMovie> = emptyList(),
    val appRatings: Map<String, Double> = emptyMap(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val sortCriterion: MyListSortCriterion = MyListSortCriterion.IMDB_RATING,
    val sortOrder: MyListSortOrder = MyListSortOrder.DESCENDING,
    val isFilterSheetOpen: Boolean = false,
    val errorMessage: String? = null,
    val offlineMessage: String? = null,
) {
    val rawList: List<SavedMovie>
        get() = if (selectedTab == MyListTab.FAVORITES) favorites else watchlist

    val currentList: List<SavedMovie>
        get() {
            var list = rawList

            // 1. Arama Filtresi
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                list = list.filter { it.title.lowercase().contains(q) }
            }

            // 2. Sıralama
            val isAsc = sortOrder == MyListSortOrder.ASCENDING
            return when (sortCriterion) {
                MyListSortCriterion.IMDB_RATING -> {
                    if (isAsc) list.sortedBy { it.rating } else list.sortedByDescending { it.rating }
                }
                MyListSortCriterion.APP_RATING -> {
                    if (isAsc) {
                        list.sortedBy { appRatings["${it.mediaType}_${it.id}"] ?: it.rating }
                    } else {
                        list.sortedByDescending { appRatings["${it.mediaType}_${it.id}"] ?: it.rating }
                    }
                }
                MyListSortCriterion.YEAR -> {
                    if (isAsc) list.sortedBy { it.year ?: 0 } else list.sortedByDescending { it.year ?: 0 }
                }
                MyListSortCriterion.TITLE -> {
                    if (isAsc) list.sortedBy { it.title.lowercase() } else list.sortedByDescending { it.title.lowercase() }
                }
            }
        }

    val hasActiveFilters: Boolean
        get() = sortCriterion != MyListSortCriterion.IMDB_RATING ||
                sortOrder != MyListSortOrder.DESCENDING ||
                searchQuery.isNotBlank()

    val activeFilterCount: Int
        get() {
            var count = 0
            if (sortCriterion != MyListSortCriterion.IMDB_RATING || sortOrder != MyListSortOrder.DESCENDING) count++
            if (searchQuery.isNotBlank()) count++
            return count
        }

    val averageRating: Double
        get() {
            val list = currentList
            return if (list.isEmpty()) 0.0 else round(list.map { it.rating }.average() * 10) / 10.0
        }
}

class MyListViewModel(
    private val repository: UserListRepository = UserListRepository(),
    private val recommendationRepository: RecommendationRepository = RecommendationRepository(),
    private val commentRepository: CommentRepository = CommentRepository(),
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

            if (favorites != null && watchlist != null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    favorites = favorites,
                    watchlist = watchlist,
                )
                fetchAppRatings(favorites + watchlist)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Liste yüklenemedi. İnternet bağlantınızı kontrol edin.",
                )
            }
        }
    }

    private fun fetchAppRatings(items: List<SavedMovie>) {
        viewModelScope.launch {
            val ratings = commentRepository.getAppRatingsForMediaList(items)
            _uiState.value = _uiState.value.copy(appRatings = ratings)
        }
    }

    fun selectTab(tab: MyListTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun toggleSearch() {
        val active = !_uiState.value.isSearchActive
        _uiState.value = _uiState.value.copy(
            isSearchActive = active,
            searchQuery = if (!active) "" else _uiState.value.searchQuery,
        )
    }

    fun setFilterSheetOpen(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isFilterSheetOpen = isOpen)
    }

    fun setSortCriterion(criterion: MyListSortCriterion) {
        _uiState.value = _uiState.value.copy(sortCriterion = criterion)
    }

    fun setSortOrder(order: MyListSortOrder) {
        _uiState.value = _uiState.value.copy(sortOrder = order)
    }

    fun resetFilters() {
        _uiState.value = _uiState.value.copy(
            sortCriterion = MyListSortCriterion.IMDB_RATING,
            sortOrder = MyListSortOrder.DESCENDING,
            searchQuery = "",
            isSearchActive = false,
        )
    }

    fun removeFromCurrentList(mediaId: Int, mediaType: String = "movie") {
        val isFavTab = _uiState.value.selectedTab == MyListTab.FAVORITES
        val removedItem = _uiState.value.currentList.find { it.id == mediaId && it.mediaType == mediaType } ?: return

        if (isFavTab) {
            val updatedFavs = _uiState.value.favorites.filterNot { it.id == mediaId && it.mediaType == mediaType }
            _uiState.value = _uiState.value.copy(favorites = updatedFavs)
        } else {
            val updatedWatch = _uiState.value.watchlist.filterNot { it.id == mediaId && it.mediaType == mediaType }
            _uiState.value = _uiState.value.copy(watchlist = updatedWatch)
        }

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
                    // Offline hatasıyla sınırlı değil: Firestore başka bir sebeple de
                    // reddedebilir — geri alınmazsa öğe listeden kaybolur ama silinmemiş olur.
                    val message = if (error is OfflineWriteException) error.message else GENERIC_WRITE_FAILURE_MESSAGE
                    if (isFavTab) {
                        _uiState.value = _uiState.value.copy(favorites = _uiState.value.favorites + removedItem, offlineMessage = message)
                    } else {
                        _uiState.value = _uiState.value.copy(watchlist = _uiState.value.watchlist + removedItem, offlineMessage = message)
                    }
                },
            )
        }
    }

    fun clearOfflineMessage() {
        _uiState.value = _uiState.value.copy(offlineMessage = null)
    }
}