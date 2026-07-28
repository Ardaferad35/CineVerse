package com.arda.cineverse

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.arda.cineverse.navigation.CineVerseNavGraph
import com.arda.cineverse.notifications.NotificationScheduler
import com.arda.cineverse.ui.theme.CineVerseTheme
import com.arda.cineverse.ui.theme.ThemeState

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Kullanıcı ne seçerse seçsin (izin ver/verme), uygulama normal çalışmaya devam eder */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Kayıtlı tema tercihini (Koyu/Açık) yükle
        ThemeState.init(applicationContext)

        // Android 13+ (API 33) sistem bildirimleri için çalışma zamanı izni gerektiriyor.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Gece yarısı Günün Filmi kontrolü ve periyodik vizyon/bildirim
        // kontrolü — WorkManager zaten planlanmışsa tekrar planlamıyor,
        // bu yüzden her açılışta güvenle çağrılabilir.
        NotificationScheduler.scheduleAll(applicationContext)

        setContent {
            CineVerseTheme(isDarkTheme = ThemeState.isDarkTheme) {
                CineVerseNavGraph()
            }
        }
    }
}