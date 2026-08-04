package com.arda.cineverse.data.local.datastore

/** DataStore'da saklanan kullanıcı tercihleri. */
data class UserPreferences(
    val isDarkTheme: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val homeLastSyncedAt: Long? = null,
)
