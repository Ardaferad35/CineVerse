package com.arda.cineverse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arda.cineverse.data.model.SavedMovie
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

    fun removeFromCurrentList(movieId: Int) {
        viewModelScope.launch {
            if (_uiState.value.selectedTab == MyListTab.FAVORITES) {
                repository.removeFavorite(movieId)
            } else {
                repository.removeFromWatchlist(movieId)
            }
            loadAll()
        }
    }
}