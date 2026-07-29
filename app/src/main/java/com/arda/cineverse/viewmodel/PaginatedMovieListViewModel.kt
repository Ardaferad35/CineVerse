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
            _uiState.value = when (val outcome = fetchNonEmptyPage(fromPage = 1)) {
                is PageFetchOutcome.Found -> {
                    val deduped = outcome.items.distinctBy(idSelector)
                    PaginatedListUiState(items = deduped, isLoading = false, currentPage = outcome.page, endReached = false)
                }
                is PageFetchOutcome.End -> {
                    PaginatedListUiState(isLoading = false, currentPage = outcome.lastPage, endReached = true)
                }
                PageFetchOutcome.Error -> {
                    PaginatedListUiState(isLoading = false, errorMessage = "Filmler yüklenemedi. İnternet bağlantınızı kontrol edin.")
                }
            }
        }
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.isLoadingMore || state.endReached || state.isLoading) return

        viewModelScope.launch {
            _uiState.value = state.copy(isLoadingMore = true)
            _uiState.value = when (val outcome = fetchNonEmptyPage(fromPage = state.currentPage + 1)) {
                is PageFetchOutcome.Found -> {
                    val existingIds = state.items.map(idSelector).toSet()
                    val deduped = outcome.items.filterNot { idSelector(it) in existingIds }
                    state.copy(items = state.items + deduped, isLoadingMore = false, currentPage = outcome.page, endReached = false)
                }
                is PageFetchOutcome.End -> {
                    state.copy(isLoadingMore = false, currentPage = outcome.lastPage, endReached = true)
                }
                PageFetchOutcome.Error -> {
                    // Sessizce başarısız oluyor — kullanıcı tekrar aşağı kaydırınca yeniden dener
                    state.copy(isLoadingMore = false)
                }
            }
        }
    }

    /**
     * Bazı listelerde (ör. Yakında Vizyona Girecekler) repository katmanı sonuçları
     * filtreleyebiliyor, bu yüzden tek bir TMDB sayfası boş dönse bile listenin
     * gerçekten bittiği anlamına gelmez. Gerçek bir sonuç bulana ya da deneme
     * sınırına ulaşana kadar sırayla sonraki sayfaları dener.
     */
    private suspend fun fetchNonEmptyPage(fromPage: Int): PageFetchOutcome<T> {
        var page = fromPage
        repeat(MAX_EMPTY_PAGE_RETRIES) {
            val result = fetchPage(page)
            if (result.isFailure) return PageFetchOutcome.Error
            val items = result.getOrNull().orEmpty()
            if (items.isNotEmpty()) return PageFetchOutcome.Found(items, page)
            page += 1
        }
        return PageFetchOutcome.End(page - 1)
    }

    private sealed class PageFetchOutcome<out T> {
        data class Found<T>(val items: List<T>, val page: Int) : PageFetchOutcome<T>()
        data class End(val lastPage: Int) : PageFetchOutcome<Nothing>()
        data object Error : PageFetchOutcome<Nothing>()
    }

    private companion object {
        const val MAX_EMPTY_PAGE_RETRIES = 5
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