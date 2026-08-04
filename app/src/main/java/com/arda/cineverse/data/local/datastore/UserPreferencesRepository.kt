package com.arda.cineverse.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "cineverse_preferences")

/**
 * Tema ve son senkronizasyon zamanı gibi kullanıcı tercihlerini saklar.
 * Tek DataStore dosyasını (`cineverse_preferences`) hem Hilt üzerinden
 * enjekte edilen yerlerde hem de [com.arda.cineverse.ui.theme.ThemeState]
 * gibi Hilt-dışı çağıranlarda aynı örnekle (Context.applicationContext'e
 * bağlı delegate) paylaşır — aynı dosya için iki ayrı DataStore örneği
 * oluşmasını önler.
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val HOME_LAST_SYNCED_AT = longPreferencesKey("home_last_synced_at")
    }

    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            isDarkTheme = prefs[Keys.IS_DARK_THEME] ?: true,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            homeLastSyncedAt = prefs[Keys.HOME_LAST_SYNCED_AT],
        )
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.dataStore.edit { it[Keys.IS_DARK_THEME] = isDark }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setHomeLastSyncedAt(timestamp: Long) {
        context.dataStore.edit { it[Keys.HOME_LAST_SYNCED_AT] = timestamp }
    }
}
