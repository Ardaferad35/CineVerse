package com.arda.cineverse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arda.cineverse.data.model.Movie
import com.arda.cineverse.data.repository.AiSearchRepository
import com.arda.cineverse.data.repository.MovieRepository
import com.arda.cineverse.data.repository.RecommendationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class SearchMode { NORMAL, AI }

data class SearchUiState(
    val query: String = "",
    val mode: SearchMode = SearchMode.NORMAL,
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
    val results: List<Movie> = emptyList(),
    val errorMessage: String? = null,
)

class SearchViewModel(
    private val movieRepository: MovieRepository = MovieRepository(),
    private val aiSearchRepository: AiSearchRepository = AiSearchRepository(),
    private val recommendationRepository: RecommendationRepository = RecommendationRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
    }

    fun setMode(mode: SearchMode) {
        _uiState.value = _uiState.value.copy(mode = mode)
    }

    fun search() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) return
        val mode = _uiState.value.mode

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, hasSearched = true)

        viewModelScope.launch {
            val result = if (mode == SearchMode.AI) {
                aiSearchRepository.searchWithDescription(query)
            } else {
                movieRepository.searchMoviesAndTv(query)
            }
            result.fold(
                onSuccess = { movies ->
                    _uiState.value = _uiState.value.copy(isLoading = false, results = movies)
                    val allGenreIds = movies.flatMap { it.genreIds }.distinct()
                    if (allGenreIds.isNotEmpty()) {
                        val isTv = movies.any { it.mediaType == "tv" }
                        recommendationRepository.recordSearchSignal(allGenreIds, if (isTv) "tv" else "movie")
                    }
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = if (mode == SearchMode.AI) {
                            "Yapay zeka şu an yanıt veremedi, tekrar deneyin."
                        } else {
                            "Arama başarısız oldu, tekrar deneyin."
                        },
                    )
                },
            )
        }
    }
}