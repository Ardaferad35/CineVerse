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
    // Yorum gönder/düzenle/sil/yanıtla işlemlerinden biri başarısız olduğunda
    // kısa süreliğine gösterilecek mesaj — bu durumlarda liste sessizce
    // yeniden yüklenmez, kullanıcı işlemin gerçekleşmediğini görür.
    val mutationErrorMessage: String? = null,
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
                    // Puan istatistikleri sadece ASIL yorumlardan hesaplanır —
                    // yanıtların kendi puanı olmadığı için (0) ortalamayı bozmasın.
                    val topLevelComments = comments.filter { it.replyToCommentId == null }
                    val counts = (1..5).associateWith { star -> topLevelComments.count { it.rating == star } }
                    val average = if (topLevelComments.isNotEmpty()) topLevelComments.map { it.rating }.average() else 0.0
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

    fun submitReply(parentCommentId: String, parentCommentUserId: String, text: String, movieTitle: String) {
        viewModelScope.launch {
            repository.addReply(movieId, parentCommentId, parentCommentUserId, text, movieTitle).fold(
                onSuccess = { loadComments() },
                onFailure = { error -> _uiState.value = _uiState.value.copy(mutationErrorMessage = mutationErrorText(error)) },
            )
        }
    }

    fun submitComment(
        text: String,
        rating: Int,
        isSpoiler: Boolean,
        movieTitle: String,
        moviePosterUrl: String?,
        movieYear: Int?,
        movieGenreIds: List<Int> = emptyList(),
    ) {
        viewModelScope.launch {
            repository.addComment(movieId, text, rating, isSpoiler, movieTitle, moviePosterUrl, movieYear, movieGenreIds).fold(
                onSuccess = { loadComments() },
                onFailure = { error -> _uiState.value = _uiState.value.copy(mutationErrorMessage = mutationErrorText(error)) },
            )
        }
    }

    fun editComment(
        commentId: String,
        text: String,
        rating: Int,
        isSpoiler: Boolean,
        movieTitle: String,
        moviePosterUrl: String?,
        movieYear: Int?,
        movieGenreIds: List<Int> = emptyList(),
    ) {
        viewModelScope.launch {
            repository.updateComment(movieId, commentId, text, rating, isSpoiler, movieTitle, moviePosterUrl, movieYear, movieGenreIds).fold(
                onSuccess = { loadComments() },
                onFailure = { error -> _uiState.value = _uiState.value.copy(mutationErrorMessage = mutationErrorText(error)) },
            )
        }
    }

    fun deleteComment(
        commentId: String,
        movieTitle: String,
        moviePosterUrl: String?,
        movieYear: Int?,
        movieGenreIds: List<Int> = emptyList(),
    ) {
        viewModelScope.launch {
            repository.deleteComment(movieId, commentId, movieTitle, moviePosterUrl, movieYear, movieGenreIds).fold(
                onSuccess = { loadComments() },
                onFailure = { error -> _uiState.value = _uiState.value.copy(mutationErrorMessage = mutationErrorText(error)) },
            )
        }
    }

    fun clearMutationError() {
        _uiState.value = _uiState.value.copy(mutationErrorMessage = null)
    }

    private fun mutationErrorText(error: Throwable): String =
        error.message?.takeIf { it.isNotBlank() } ?: "İşlem gerçekleştirilemedi. Lütfen tekrar deneyin."
}

class CommentViewModelFactory(
    private val movieId: Int,
    private val repository: CommentRepository = CommentRepository(),
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CommentViewModel(movieId, repository) as T
    }
}
