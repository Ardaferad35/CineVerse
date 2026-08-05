// Hilt Gradle plugin: dagger/dagger#4944 ve #5099 (AGP 9.0 uyumsuzluğu ve
// hemen ardından 2.59'daki ComponentTreeDeps regresyonu) 2.60'ta düzeltildi
// — bu yüzden burada 2.60.1 kullanılıyor, "plugin'siz Hilt" workaround'u
// artık gerekmiyor (bkz. app/build.gradle.kts'teki hilt-android sürümü).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.gms.google-services") version "4.5.0" apply false
    id("com.google.devtools.ksp") version "2.2.10-2.0.2" apply false
    id("com.google.dagger.hilt.android") version "2.60.1" apply false
}