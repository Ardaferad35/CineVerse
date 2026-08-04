package com.arda.cineverse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arda.cineverse.data.model.AiChatMessage
import com.arda.cineverse.data.model.ChatErrorMessage
import com.arda.cineverse.data.model.ChatMessage
import com.arda.cineverse.data.model.Movie
import com.arda.cineverse.data.model.UserChatMessage
import com.arda.cineverse.data.repository.AiChatException
import com.arda.cineverse.data.repository.AiChatRepository
import com.arda.cineverse.data.repository.RecommendationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AiChatUiState(
    /** Ana sayfadaki Filmler/Diziler seçimiyle geliyor; asistanın kimliğini bu belirliyor. */
    val isTvMode: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    /** Yanıt beklenirken hem "yazıyor" balonu hem de gönder butonunun kilidi bunu kullanıyor. */
    val isThinking: Boolean = false,
)

class AiChatViewModel(
    private val chatRepository: AiChatRepository = AiChatRepository(),
    private val recommendationRepository: RecommendationRepository = RecommendationRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState

    private var nextId = 0L

    /**
     * Film ve dizi asistanı ayrı sohbetler yürütüyor. Aynı thread'i paylaşsalardı
     * dizi asistanı bir önceki film konuşmasının üstüne konuşurdu; mod değişince
     * geçmişi silmek de kullanıcının sohbetini kaybettirirdi.
     */
    private val parkedThreads = mutableMapOf<Boolean, List<ChatMessage>>()

    /** Hata sonrası "tekrar dene" için elde tutulan son kullanıcı mesajı (mod başına). */
    private val lastUserMessages = mutableMapOf<Boolean, String>()

    fun setTvMode(isTvMode: Boolean) {
        val current = _uiState.value
        if (current.isTvMode == isTvMode) return
        parkedThreads[current.isTvMode] = current.messages
        _uiState.value = current.copy(
            isTvMode = isTvMode,
            messages = parkedThreads[isTvMode].orEmpty(),
            input = "",
        )
    }

    fun onInputChange(value: String) {
        _uiState.update { it.copy(input = value) }
    }

    fun send(text: String = _uiState.value.input) {
        val message = text.trim()
        if (message.isEmpty() || _uiState.value.isThinking) return

        // Geçmiş, kullanıcının yeni mesajı listeye eklenmeden ÖNCE alınıyor:
        // repository yeni mesajı zaten ayrı parametre olarak gönderiyor.
        val history = _uiState.value.messages
        val isTvMode = _uiState.value.isTvMode
        lastUserMessages[isTvMode] = message

        appendMessage(isTvMode, UserChatMessage(id = nextId++, text = message))
        _uiState.update { it.copy(input = "", isThinking = true) }

        viewModelScope.launch {
            chatRepository.reply(history = history, message = message, isTvMode = isTvMode).fold(
                onSuccess = { reply ->
                    appendMessage(
                        isTvMode,
                        AiChatMessage(
                            id = nextId++,
                            text = reply.text,
                            intent = reply.intent,
                            suggestions = reply.suggestions,
                        ),
                    )
                    recordTasteSignal(reply.suggestions.map { suggestion -> suggestion.movie })
                },
                onFailure = { error ->
                    appendMessage(
                        isTvMode,
                        ChatErrorMessage(
                            id = nextId++,
                            // Sebebi bilinen hatalar (model yoğun, bağlantı yok) kendi
                            // metnini taşıyor; geri kalanı için genel mesaj.
                            text = (error as? AiChatException)?.message ?: "Asistan şu an yanıt veremedi.",
                        ),
                    )
                },
            )
            _uiState.update { it.copy(isThinking = false) }
        }
    }

    /** Hata balonundaki "Tekrar dene" — son kullanıcı mesajını yeniden gönderir. */
    fun retry() {
        val message = lastUserMessages[_uiState.value.isTvMode] ?: return
        // Başarısız denemenin hata balonu listede kalmasın, sohbet temiz görünsün.
        _uiState.update { state ->
            state.copy(
                messages = state.messages.dropLastWhile { it is ChatErrorMessage || it is UserChatMessage },
            )
        }
        send(message)
    }

    fun clear() {
        lastUserMessages.remove(_uiState.value.isTvMode)
        _uiState.update { it.copy(messages = emptyList(), input = "") }
    }

    /**
     * Yanıt beklenirken kullanıcı diğer asistana geçmiş olabilir; o durumda mesaj
     * ekrandaki sohbete değil, ait olduğu park edilmiş thread'e yazılıyor.
     */
    private fun appendMessage(isTvMode: Boolean, message: ChatMessage) {
        if (_uiState.value.isTvMode == isTvMode) {
            _uiState.update { it.copy(messages = it.messages + message) }
        } else {
            parkedThreads[isTvMode] = parkedThreads[isTvMode].orEmpty() + message
        }
    }

    /**
     * Asistanın verdiği yapımların türleri, ana sayfadaki "Size Özel" bölümünü
     * besleyen sinyale yazılıyor — klasik aramada da aynısı yapılıyordu.
     */
    private fun recordTasteSignal(movies: List<Movie>) {
        val genreIds = movies.flatMap { it.genreIds }.distinct()
        if (genreIds.isEmpty()) return
        viewModelScope.launch {
            val isTv = movies.any { it.mediaType == "tv" }
            recommendationRepository.recordSearchSignal(genreIds, if (isTv) "tv" else "movie")
        }
    }
}
