// NOT: Hilt Gradle plugin'i (com.google.dagger.hilt.android) kasıtlı olarak
// UYGULANMIYOR — bu projenin AGP 9.3.1 sürümüyle "Android BaseExtension not
// found" hatası veriyor (bilinen bir Hilt/AGP 9 uyumsuzluğu, dagger/dagger#4944).
// Bunun yerine resmi "Gradle plugin'siz Hilt" yöntemi kullanılıyor:
// hilt-android + hilt-android-compiler (KSP) bağımlılıkları yeterli, sadece
// @HiltAndroidApp/@AndroidEntryPoint ile işaretlenen sınıflar üretilen
// Hilt_* sınıflarını (bytecode transform yerine) doğrudan extend ediyor
// (bkz. CineVerseApplication, MainActivity).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.gms.google-services") version "4.5.0" apply false
    id("com.google.devtools.ksp") version "2.2.10-2.0.2" apply false
}