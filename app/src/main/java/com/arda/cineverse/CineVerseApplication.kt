package com.arda.cineverse

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.arda.cineverse.di.AppGraph
import dagger.hilt.android.HiltAndroidApp

// Hilt Gradle plugin'i uygulanmadığı için (bkz. root build.gradle.kts),
// üretilen Hilt_CineVerseApplication sınıfı doğrudan extend ediliyor —
// bytecode transform yerine resmi "plugin'siz Hilt" yöntemi. Plugin
// olmadan @HiltAndroidApp'in taban sınıfı açıkça belirtilmesi gerekiyor.
@HiltAndroidApp(Application::class)
class CineVerseApplication : Hilt_CineVerseApplication(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100 * 1024 * 1024)
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .build()
    }
}
