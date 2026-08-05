package com.arda.cineverse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arda.cineverse.data.model.Friend
import com.arda.cineverse.data.repository.FriendRepository
import com.arda.cineverse.data.repository.RecommendationQuotaException
import com.arda.cineverse.data.repository.RecommendationShareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecommendShareUiState(
    val friends: List<Friend> = emptyList(),
    val selectedUids: Set<String> = emptySet(),
    val note: String = "",
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    /** Bugün kalan gönderim hakkı; sunucudan okunana kadar tam kabul ediliyor. */
    val remainingQuota: Int = RecommendationShareRepository.DAILY_LIMIT,
    val errorMessage: String? = null,
    /** Gönderim başarıyla tamamlandı — sayfa bunu görünce sheet'i kapatıyor. */
    val sentCount: Int? = null,
) {
    val isQuotaExhausted: Boolean get() = remainingQuota <= 0

    /** Seçim kalan hakkı aşarsa gönderim engelleniyor, sunucu zaten reddederdi. */
    val isOverQuota: Boolean get() = selectedUids.size > remainingQuota

    val canSend: Boolean
        get() = selectedUids.isNotEmpty() && !isOverQuota && !isSending && !isQuotaExhausted
}

@HiltViewModel
class RecommendShareViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    private val shareRepository: RecommendationShareRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecommendShareUiState())
    val uiState: StateFlow<RecommendShareUiState> = _uiState

    init {
        viewModelScope.launch {
            friendRepository.observeFriends().collect { friends ->
                _uiState.update { it.copy(friends = friends, isLoading = false) }
            }
        }
        viewModelScope.launch {
            friendRepository.syncFriends()
            refreshQuota()
        }
    }

    private suspend fun refreshQuota() {
        shareRepository.getRemainingQuota().onSuccess { remaining ->
            _uiState.update { it.copy(remainingQuota = remaining) }
        }
    }

    fun toggleFriend(uid: String) {
        _uiState.update { state ->
            val selected = if (uid in state.selectedUids) state.selectedUids - uid else state.selectedUids + uid
            state.copy(selectedUids = selected, errorMessage = null)
        }
    }

    fun onNoteChange(note: String) {
        _uiState.update { it.copy(note = note.take(MAX_NOTE_LENGTH)) }
    }

    fun send(mediaId: Int, mediaType: String, mediaTitle: String, posterUrl: String? = null) {
        val state = _uiState.value
        if (!state.canSend) return
        _uiState.update { it.copy(isSending = true, errorMessage = null) }

        viewModelScope.launch {
            shareRepository.send(
                targetUids = state.selectedUids.toList(),
                mediaId = mediaId,
                mediaType = mediaType,
                mediaTitle = mediaTitle,
                posterUrl = posterUrl,
                note = state.note.trim(),
            ).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSending = false, sentCount = state.selectedUids.size) }
                },
                onFailure = { error ->
                    // Kota sunucuda dolmuşsa istemcideki sayaç eskimiş demektir;
                    // yeniden okuyup butonu doğru duruma getiriyoruz.
                    if (error is RecommendationQuotaException) refreshQuota()
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            errorMessage = when (error) {
                                is RecommendationQuotaException -> "Günlük öneri hakkın doldu, yarın tekrar dene."
                                else -> "Gönderilemedi, tekrar dene."
                            },
                        )
                    }
                },
            )
        }
    }

    /** Sheet her açıldığında çağrılıyor: seçim ve not sıfırlanır, kota tazelenir. */
    fun reset() {
        _uiState.update {
            it.copy(selectedUids = emptySet(), note = "", errorMessage = null, sentCount = null)
        }
        viewModelScope.launch { refreshQuota() }
    }

    companion object {
        const val MAX_NOTE_LENGTH = 120
    }
}
