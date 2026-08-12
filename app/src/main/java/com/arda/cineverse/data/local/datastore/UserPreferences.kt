package com.arda.cineverse.data.local.datastore

/** DataStore'da saklanan kullanıcı tercihleri. */
data class UserPreferences(
    val isDarkTheme: Boolean = true,
    val rememberMe: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val homeLastSyncedAt: Long? = null,
    val cachedUsername: String = "",
    val cachedAvatarId: String = "default",
    val cachedRatingSum: Long = 0L,
    val cachedRatingCount: Int = 0,
)
