package com.arda.cineverse

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.arda.cineverse.navigation.CineVerseNavGraph
import com.arda.cineverse.notifications.NotificationScheduler
import com.arda.cineverse.ui.screens.SplashScreen
import com.arda.cineverse.ui.theme.CineVerseTheme
import com.arda.cineverse.ui.theme.ThemeState
import dagger.hilt.android.AndroidEntryPoint

// Hilt Gradle plugin'i uygulanmadığı için (bkz. root build.gradle.kts),
// üretilen Hilt_MainActivity sınıfı doğrudan extend ediliyor. Plugin
// olmadan @AndroidEntryPoint'in taban sınıfı açıkça belirtilmesi gerekiyor.
@AndroidEntryPoint(ComponentActivity::class)
class MainActivity : Hilt_MainActivity() {

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Kullanıcı ne seçerse seçsin (izin ver/verme), uygulama normal çalışmaya devam eder */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        // ÖNEMLİ SIRALAMA: Kayıtlı tema tercihi, installSplashScreen()'den ÖNCE
        // okunup setTheme() ile uygulanmalı — aksi halde native splash (API 28-30
        // "backport" yolunda) her zaman manifest'teki varsayılan (gündüz) temayı
        // gösterir, kullanıcının gerçek Koyu/Açık tercihini yansıtmaz.
        ThemeState.init(applicationContext)
        setTheme(
            if (ThemeState.isDarkTheme) {
                R.style.Theme_CineVerse_Splash_Dark
            } else {
                R.style.Theme_CineVerse_Splash_Light
            },
        )

        // Android 12+'ta sistemin gösterdiği ÇOK KISA (yüz milisaniyelik) açılış
        // splash'ını devreye sokar — yukarıda setTheme() ile seçilen, temaya göre
        // doğru arka plan/ikonu kullanır.
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
                // Native splash (yukarıda) kapandıktan hemen sonra, referans
                // tasarımdaki zengin görseli (logo + yazı + süsleme) ~1.5sn
                // gösteren özel Compose ekranı devreye giriyor.
                var showSplash by remember { mutableStateOf(true) }
                if (showSplash) {
                    SplashScreen(onFinished = { showSplash = false })
                } else {
                    CineVerseNavGraph()
                }
            }
        }
    }
}