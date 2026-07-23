package com.arda.cineverse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arda.cineverse.data.model.Comment
import com.arda.cineverse.data.repository.CommentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.round

data class CommentUiState(
    val isLoading: Boolean = true,
    val comments: List<Comment> = emptyList(),
    val averageRating: Double = 0.0,
    val ratingCounts: Map<Int, Int> = emptyMap(),
    val errorMessage: String? = null,
)

class CommentViewModel(
    private val movieId: Int,
    private val repository: CommentRepository = CommentRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommentUiState())
    val uiState: StateFlow<CommentUiState> = _uiState

    val currentUserId: String?
        get() = repository.currentUserId()

    init {
        loadComments()
    }

    fun loadComments() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            repository.getComments(movieId).fold(
                onSuccess = { comments ->
                    val counts = (1..5).associateWith { star -> comments.count { it.rating == star } }
                    val average = if (comments.isNotEmpty()) comments.map { it.rating }.average() else 0.0
                    _uiState.value = CommentUiState(
                        isLoading = false,
                        comments = comments,
                        averageRating = round(average * 10) / 10.0,
                        ratingCounts = counts,
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Yorumlar yüklenemedi")
                },
            )
        }
    }

    fun submitComment(text: String, rating: Int, isSpoiler: Boolean) {
        viewModelScope.launch {
            repository.addComment(movieId, text, rating, isSpoiler)
            loadComments()
        }
    }

    fun editComment(commentId: String, text: String, rating: Int, isSpoiler: Boolean) {
        viewModelScope.launch {
            repository.updateComment(movieId, commentId, text, rating, isSpoiler)
            loadComments()
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            repository.deleteComment(movieId, commentId)
            loadComments()
        }
    }
}

class CommentViewModelFactory(private val movieId: Int) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CommentViewModel(movieId) as T
    }
}