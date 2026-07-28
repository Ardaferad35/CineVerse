package com.arda.cineverse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arda.cineverse.data.model.AppNotification
import com.arda.cineverse.data.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val isLoading: Boolean = true,
    val notifications: List<AppNotification> = emptyList(),
)

class NotificationViewModel(
    private val repository: NotificationRepository = NotificationRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount

    init {
        loadNotifications()
        refreshUnreadCount()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val notifications = repository.getNotifications().getOrDefault(emptyList())
            _uiState.value = NotificationsUiState(isLoading = false, notifications = notifications)
            // Ekran açıldığında hepsini okundu say
            repository.markAllAsRead()
            _unreadCount.value = 0
        }
    }

    fun refreshUnreadCount() {
        viewModelScope.launch {
            _unreadCount.value = repository.getUnreadCount().getOrDefault(0)
        }
    }
}