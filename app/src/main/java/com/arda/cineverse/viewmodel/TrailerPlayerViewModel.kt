package com.arda.cineverse.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class TrailerPlayerUiState(
    val videoKey: String? = null,
    val title: String? = null,
    val isMinimized: Boolean = false,
)

@Singleton
class TrailerPlayerManager @Inject constructor() {
    private val _uiState = MutableStateFlow(TrailerPlayerUiState())
    val uiState: StateFlow<TrailerPlayerUiState> = _uiState.asStateFlow()

    fun playTrailer(videoKey: String, title: String? = null) {
        if (videoKey.isBlank()) return
        _uiState.update {
            it.copy(
                videoKey = videoKey,
                title = title ?: "",
                isMinimized = false,
            )
        }
    }

    fun minimize() {
        _uiState.update { it.copy(isMinimized = true) }
    }

    fun expand() {
        _uiState.update { it.copy(isMinimized = false) }
    }

    fun close() {
        _uiState.update {
            it.copy(
                videoKey = null,
                title = null,
                isMinimized = false,
            )
        }
    }
}

@HiltViewModel
class TrailerPlayerViewModel @Inject constructor(
    private val manager: TrailerPlayerManager
) : ViewModel() {

    val uiState: StateFlow<TrailerPlayerUiState> = manager.uiState

    fun playTrailer(videoKey: String, title: String? = null) {
        manager.playTrailer(videoKey, title)
    }

    fun minimize() = manager.minimize()

    fun expand() = manager.expand()

    fun close() = manager.close()
}
