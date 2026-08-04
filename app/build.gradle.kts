import java.util.Properties
import java.io.FileInputStream

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(FileInputStream(localPropertiesFile))
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("com.google.devtools.ksp")
    // Hilt Gradle plugin BİLEREK yok — bkz. root build.gradle.kts'teki not.
}

android {
    namespace = "com.arda.cineverse"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.arda.cineverse"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "TMDB_API_KEY",
            "\"${localProperties.getProperty("TMDB_API_KEY", "")}\""
        )
        buildConfigField(
            "String",
            "TMDB_READ_ACCESS_TOKEN",
            "\"${localProperties.getProperty("TMDB_READ_ACCESS_TOKEN", "")}\""
        )
        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"${localProperties.getProperty("GEMINI_API_KEY", "")}\""
        )
        // Arkadaşlık isteği/kabulü push bildirimleri için Supabase Edge
        // Function'ı (bkz. supabase/functions/friend-push). SUPABASE_ANON_KEY
        // client'a gömülmesi güvenli, herkese açık bir değerdir (Supabase'in
        // kendi platform kapısını geçmek için kullanılır — asıl yetkilendirme
        // fonksiyon içinde Firebase ID token doğrulamasıyla yapılıyor).
        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"${localProperties.getProperty("SUPABASE_URL", "")}\""
        )
        buildConfigField(
            "String",
            "SUPABASE_ANON_KEY",
            "\"${localProperties.getProperty("SUPABASE_ANON_KEY", "")}\""
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            // R8 açık: kod küçültme + obfuscation. Kapalıyken release APK'sı
            // gereksiz büyüktü ve BuildConfig'teki API anahtarları düz metin
            // olarak çok daha kolay çıkarılabiliyordu. Reflection kullanan
            // kütüphanelerin (Gson, Firestore) ihtiyaç duyduğu keep kuralları
            // proguard-rules.pro'da.
            optimization {
                enable = true
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.runtime)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.play.services)

    // TMDB için ağ katmanı
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Film posterleri için görsel yükleme
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Zamanlanmış arka plan görevleri (gece yarısı Günün Filmi kontrolü,
    // periyodik Yakında Vizyona Girecekler kontrolü) için
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // Smart Offline Mode: yerel önbellek (Room), ayar deposu (DataStore) ve
    // bunları bağlayan dependency injection (Hilt)
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    implementation("com.google.dagger:hilt-android:2.57.1")
    ksp("com.google.dagger:hilt-android-compiler:2.57.1")
    // 1.4.0, compileSdk 37 gerektiriyor (proje 36'da) ve dagger:hilt-android'i
    // 2.59'a yükseltip yukarıdaki 2.57.1 compiler ile sürüm uyuşmazlığı
    // yaratıyordu ("getSavedStateHandleHolder" symbol not found) — bu yüzden
    // compileSdk 36 ile uyumlu 1.2.0'da sabitlendi.
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("androidx.datastore:datastore-preferences:1.2.1")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}