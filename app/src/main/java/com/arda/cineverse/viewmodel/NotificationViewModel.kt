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

    // NOT: init'te SADECE refreshUnreadCount() çağrılıyor — loadNotifications()
    // ÇAĞRILMIYOR. Bu ViewModel hem NotificationsScreen'de (liste + "hepsini
    // okundu say") hem de HomeScreen'de (sadece rozet sayısı için, AYRI bir
    // örnek olarak) kullanılıyor. Önceden ikisi de init'te birlikte
    // tetikleniyordu; bu hem iki coroutine'in aynı _unreadCount'a sırasız
    // yazmasına (rozet yanlışlıkla sıfırdan farklı görünmesi) HEM DE daha
    // ciddisi: sadece Ana Sayfa'yı açmanın bile TÜM bildirimleri sessizce
    // okundu işaretlemesine yol açıyordu. loadNotifications() artık sadece
    // NotificationsScreen gerçekten açıldığında (bkz. o ekrandaki
    // LaunchedEffect) çağrılıyor.
    init {
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