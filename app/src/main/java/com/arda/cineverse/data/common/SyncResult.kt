package com.arda.cineverse.data.common

/**
 * Bir offline-first refresh çağrısının sonucu. UI'ın gördüğü veri her
 * zaman Room'daki (Flow ile gözlemlenen) cache'ten gelir — bu tip sadece
 * "senkronizasyon oldu mu" bilgisini taşır, ekstra bir loading/error
 * durum makinesi gerektirmez.
 */
sealed interface SyncResult {
    data object Success : SyncResult
    data object Offline : SyncResult
    data class Error(val cause: Throwable) : SyncResult
}
