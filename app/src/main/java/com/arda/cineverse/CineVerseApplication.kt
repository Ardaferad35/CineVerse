package com.arda.cineverse

import android.app.Application
import com.arda.cineverse.di.AppGraph
import dagger.hilt.android.HiltAndroidApp

// Hilt Gradle plugin'i uygulanmadığı için (bkz. root build.gradle.kts),
// üretilen Hilt_CineVerseApplication sınıfı doğrudan extend ediliyor —
// bytecode transform yerine resmi "plugin'siz Hilt" yöntemi. Plugin
// olmadan @HiltAndroidApp'in taban sınıfı açıkça belirtilmesi gerekiyor.
@HiltAndroidApp(Application::class)
class CineVerseApplication : Hilt_CineVerseApplication() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
    }
}
