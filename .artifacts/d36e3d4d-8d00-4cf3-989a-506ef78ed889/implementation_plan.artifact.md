# CineVerseNavGraph Hatalarını Giderme ve Proje Yapılandırması

Bu plan, `CineVerseNavGraph.kt` dosyasındaki hataları gidermek, eksik Navigation bağımlılıklarını eklemek ve projedeki paket adı uyumsuzluklarını düzeltmek için hazırlanmıştır.

## Kullanıcı İncelemesi Gerekenler
- **Paket Adı Değişikliği:** Projedeki klasör yapısı `com.arda.cineverse` olduğu için, tüm dosyalardaki paket bildirimlerini bu yapıya uygun şekilde güncelleyeceğim. Ayrıca `build.gradle.kts` dosyasındaki `namespace` değerini de `com.arda.cineverse` yapacağım.
- **Navigation Sürümü:** `androidx.navigation:navigation-compose` kütüphanesinin `2.9.8` sürümü kullanılacaktır.

## Önerilen Değişiklikler

### Bağımlılık Yönetimi

#### [MODIFY] [libs.versions.toml](file:///C:/Users/ardaf/AndroidStudioProjects/CineVerse/gradle/libs.versions.toml)
- `navigation` sürümü ve `androidx-navigation-compose` kütüphane tanımı eklenecek.

#### [MODIFY] [build.gradle.kts](file:///C:/Users/ardaf/AndroidStudioProjects/CineVerse/app/build.gradle.kts)
- `namespace` ve `applicationId` değerleri `com.arda.cineverse` olarak güncellenecek.
- `androidx-navigation-compose` bağımlılığı eklenecek.

### Paket ve Kod Düzeltmeleri

#### [MODIFY] Tüm Kotlin Dosyaları
- `package com.cineverse...` olan bildirimler, dosyanın bulunduğu klasöre göre `package com.arda.cineverse...` olarak düzeltilecek.
- Özellikle `ui/screens/auth` altındaki ekranların paketleri `com.arda.cineverse.ui.screens.auth` yapılacak.

#### [MODIFY] [CineVerseNavGraph.kt](file:///C:/Users/ardaf/AndroidStudioProjects/CineVerse/app/src/main/java/com/arda/cineverse/navigation/CineVerseNavGraph.kt)
- Paket adı düzeltilecek.
- Ekranların yeni paket konumlarına göre importlar güncellenecek.
- Navigation kütüphanesi eklendikten sonra `NavHost`, `composable` vb. hataları kendiliğinden düzelecek.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/ardaf/AndroidStudioProjects/CineVerse/app/src/main/java/com/arda/cineverse/MainActivity.kt)
- Paket adı ve importlar güncellenecek.

## Doğrulama Planı

### Otomatik Testler
- `./gradlew assembleDebug` komutu ile projenin hatasız derlendiği kontrol edilecek.

### Manuel Doğrulama
- IDE üzerindeki "Unresolved reference" hatalarının kaybolduğu teyit edilecek.
- Gradle Sync yapılarak bağımlılıkların başarıyla yüklendiği görülecek.
