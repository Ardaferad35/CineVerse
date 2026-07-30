package com.arda.cineverse.data.connectivity

import kotlinx.coroutines.flow.Flow

interface ConnectivityObserver {
    /** Bağlantı durumu değiştikçe (doğrulanmış internet erişimi olup olmadığı) yayınlar. */
    val isOnline: Flow<Boolean>

    /** Tek seferlik anlık kontrol — refresh çağrılarından önce kullanılır. */
    suspend fun isCurrentlyOnline(): Boolean
}
