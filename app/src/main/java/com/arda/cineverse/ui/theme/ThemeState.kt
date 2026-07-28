package com.arda.cineverse.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Uygulama genelinde aktif tema tercihini tutar. Bir kez init() ile
 * SharedPreferences'tan okunur, sonrasında Profil ekranından
 * setDarkTheme() ile değiştirilebilir — değişiklik anında tüm ekranlara
 * yansır (CineVerseTheme bunu okuyor) ve kalıcı olarak saklanır.
 */
object ThemeState {
    private const val PREFS_NAME = "cineverse_theme_prefs"
    private const val KEY_IS_DARK = "is_dark_theme"

    var isDarkTheme by mutableStateOf(true)
        private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isDarkTheme = prefs.getBoolean(KEY_IS_DARK, true)
    }

    fun setDarkTheme(context: Context, isDark: Boolean) {
        isDarkTheme = isDark
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_IS_DARK, isDark)
            .apply()
    }
}