package com.arda.cineverse.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arda.cineverse.data.local.datastore.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Uygulama genelinde aktif tema tercihini tutar. Bir kez init() ile
 * DataStore'dan okunur, sonrasında Profil ekranından setDarkTheme() ile
 * değiştirilebilir — değişiklik anında tüm ekranlara yansır (CineVerseTheme
 * bunu okuyor) ve kalıcı olarak saklanır.
 *
 * init() SENKRON çalışmak zorunda: MainActivity, installSplashScreen()'den
 * ÖNCE setTheme() ile native splash temasını belirlemek için bu değeri
 * hemen okumalı (bkz. MainActivity.onCreate). DataStore async olduğundan
 * burada tek seferlik runBlocking kullanılıyor — tıpkı önceki
 * SharedPreferences sürümünün de onCreate()'i bloklaması gibi.
 */
object ThemeState {
    var isDarkTheme by mutableStateOf(true)
        private set

    private var repository: UserPreferencesRepository? = null
    private val backgroundScope = CoroutineScope(Dispatchers.IO)

    fun init(context: Context) {
        val repo = repository ?: UserPreferencesRepository(context.applicationContext).also { repository = it }
        isDarkTheme = runBlocking(Dispatchers.IO) { repo.userPreferences.first().isDarkTheme }
    }

    fun setDarkTheme(context: Context, isDark: Boolean) {
        isDarkTheme = isDark
        val repo = repository ?: UserPreferencesRepository(context.applicationContext).also { repository = it }
        backgroundScope.launch { repo.setDarkTheme(isDark) }
    }
}
