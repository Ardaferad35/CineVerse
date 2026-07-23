package com.arda.cineverse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PaginatedListUiState<T>(
    val items: List<T> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val currentPage: Int = 1,
    val endReached: Boolean = false,
)

/**
 * "Tümünü Gör" ekranları gibi, aşağı kaydırdıkça daha fazla öğe yükleyen
 * (infinite scroll) her liste için kullanılabilecek genel amaçlı ViewModel.
 *
 * idSelector: TMDB bazen ardışık sayfalarda aynı filmi tekrar döndürebiliyor
 * (liste sürekli güncellendiği için). Bu yüzden yeni gelen sayfadaki
 * ID'si zaten listede olan öğeleri eleyip Compose'un "aynı key iki kez
 * kullanılamaz" hatasını önlüyoruz.
 */
class PaginatedMovieListViewModel<T>(
    private val fetchPage: suspend (page: Int) -> Result<List<T>>,
    private val idSelector: (T) -> Any,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaginatedListUiState<T>())
    val uiState: StateFlow<PaginatedListUiState<T>> = _uiState

    init {
        loadFirstPage()
    }

    fun loadFirstPage() {
        viewModelScope.launch {
            _uiState.value = PaginatedListUiState(isLoading = true)
            val result = fetchPage(1)
            _uiState.value = result.fold(
                onSuccess = { items ->
                    val deduped = items.distinctBy(idSelector)
                    PaginatedListUiState(items = deduped, isLoading = false, currentPage = 1, endReached = deduped.isEmpty())
                },
                onFailure = {
                    PaginatedListUiState(isLoading = false, errorMessage = "Filmler yüklenemedi. İnternet bağlantınızı kontrol edin.")
                },
            )
        }
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.isLoadingMore || state.endReached || state.isLoading) return

        viewModelScope.launch {
            _uiState.value = state.copy(isLoadingMore = true)
            val nextPage = state.currentPage + 1
            val result = fetchPage(nextPage)
            _uiState.value = result.fold(
                onSuccess = { newItems ->
                    val existingIds = state.items.map(idSelector).toSet()
                    val deduped = newItems.filterNot { idSelector(it) in existingIds }
                    state.copy(
                        items = state.items + deduped,
                        isLoadingMore = false,
                        currentPage = nextPage,
                        // TMDB'nin gerçekten sonuncu sayfasına ulaştığımızı, orijinal
                        // (filtrelenmemiş) sonucun boş olup olmamasına göre anlıyoruz.
                        endReached = newItems.isEmpty(),
                    )
                },
                onFailure = {
                    // Sessizce başarısız oluyor — kullanıcı tekrar aşağı kaydırınca yeniden dener
                    state.copy(isLoadingMore = false)
                },
            )
        }
    }
}

class PaginatedMovieListViewModelFactory<T>(
    private val fetchPage: suspend (Int) -> Result<List<T>>,
    private val idSelector: (T) -> Any,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>): VM {
        return PaginatedMovieListViewModel(fetchPage, idSelector) as VM
    }
}