# Firestore Hatalarını Giderme Planı

`AuthViewModel.kt` dosyasındaki Firestore ile ilgili "Unresolved reference" hataları, Firestore kütüphanesinin projeye tam olarak entegre edilmemesinden ve senkronizasyon eksikliğinden kaynaklanmaktadır.

## Hataların Kaynakları

1.  **Bağımlılık Yönetimi Uyumsuzluğu:** `app/build.gradle.kts` dosyasına Firestore bağımlılığı manuel olarak eklenmiş, ancak projenin merkezi sürüm kataloğu (`libs.versions.toml`) yapısına dahil edilmemiştir.
2.  **Senkronizasyon:** Bağımlılık eklendikten sonra Gradle senkronizasyonu yapılmadığı için kod içerisinde Firestore sınıfları (FirebaseFirestore, FieldValue vb.) tanınmamaktadır.

## Önerilen Değişiklikler

### [Bağımlılık Yönetimi]

#### [MODIFY] [libs.versions.toml](file:///C:/Users/ardaf/AndroidStudioProjects/CineVerse/gradle/libs.versions.toml)
- Firestore kütüphanesini (`firebase-firestore`) sürüm kataloğuna ekleyeceğiz.

#### [MODIFY] [app/build.gradle.kts](file:///C:/Users/ardaf/AndroidStudioProjects/CineVerse/app/build.gradle.kts)
- Manuel eklenen `implementation("com.google.firebase:firebase-firestore-ktx")` satırını, katalog üzerinden `implementation(libs.firebase.firestore)` olarak güncelleyeceğiz.

### [Proje Yapılandırması]

- **Gradle Sync:** Değişiklikler sonrası projeyi senkronize ederek kütüphanelerin indirilmesini ve sınıfların tanınmasını sağlayacağız.

## Doğrulama Planı

### Otomatik Testler
- `./gradlew :app:compileDebugKotlin` komutu ile projenin hatasız derlendiğini kontrol edeceğiz.

### Manuel Doğrulama
- `AuthViewModel.kt` dosyasındaki Firestore referanslarının (FirebaseFirestore, collection, document, set, FieldValue) hatalarının giderildiğini doğrulayacağız.
