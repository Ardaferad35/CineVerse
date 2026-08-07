# 🎬 CineVerse - Sinema & Dizi Keşif ve Sosyal Platformu

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="128" height="128" alt="CineVerse Logo" />
</p>

<p align="center">
  <b>Modern Android Mimarisi, Yapay Zeka Desteği ve Çevrimdışı Senkronizasyon ile Donatılmış Sinema & Dizi Ekosistemi</b>
</p>

<p align="center">
  <a href="#-teknoloji-yığını--mimari"><img src="https://img.shields.io/badge/Kotlin-1.9+-7F52FF?style=flat-square&logo=kotlin&logoColor=white" /></a>
  <a href="#-teknoloji-yığını--mimari"><img src="https://img.shields.io/badge/Jetpack_Compose-Material3-4285F4?style=flat-square&logo=android" /></a>
  <a href="#-teknoloji-yığını--mimari"><img src="https://img.shields.io/badge/Architecture-MVVM%20%2B%20Clean-FF6F00?style=flat-square" /></a>
  <a href="#-teknoloji-yığını--mimari"><img src="https://img.shields.io/badge/Firebase-Auth%20%7C%20Firestore%20%7C%20FCM-FFCA28?style=flat-square&logo=firebase&logoColor=black" /></a>
  <a href="#-teknoloji-yığını--mimari"><img src="https://img.shields.io/badge/Gemini_AI-Google_REST_API-8E44AD?style=flat-square&logo=google" /></a>
</p>

---

## 📌 İçindekiler
- [✨ Öne Çıkan Özellikler](#-öne-çıkan-özellikler)
- [🏗️ Mimari ve Tasarım Desenleri](#️-mimari-ve-tasarım-desenleri)
- [🛠️ Teknoloji Yığını](#️-teknoloji-yığını)
- [📱 Ekranlar ve Kullanıcı Deneyimi](#-ekranlar-ve-kullanıcı-deneyimi)
- [⚡ Çevrimdışı (Offline-First) Senkronizasyon](#-çevrimdışı-offline-first-senkronizasyon)
- [🚀 Kurulum ve Yapılandırma](#-kurulum-ve-yapılandırma)
- [🧪 Test ve Kalite Güvencesi](#-test-ve-kalite-güvencesi)

---

## ✨ Öne Çıkan Özellikler

### 🎥 Film & Dizi Keşfi
* **Trendler ve Kategoriler:** TMDB REST API entegrasyonu ile en popüler, en çok oylanan ve vizyondaki içerikler.
* **Akıllı Öneri Motoru:** Kullanıcının geçmiş aramaları ve etkileşimlerine dayalı dinamik kayan pencere (*Sliding Window*) öneri sistemi.
* **Detay Sayfaları:** Oyuncu kadrosu, benzer yapımlar, YouTube fragmanları, bölüm takibi (diziler için) ve ortalama topluluk puanları.

### 🤖 Yapay Zeka (AI) Asistanı
* **Google Gemini AI Entegrasyonu:** Kullanıcının ne izlemek istediğini anlayan, ruh haline göre spesifik film/dizi tavsiye eden akıllı sohbet asistanı.
* **Çift Modlu Hafıza:** Film ve dizi modları arasında bağımsız sohbet bağlamı koruması.

### 📱 Reels Keşif Akışı
* **Dikey Akış:** TikTok / Shorts tarzında hızlı film ve dizi keşif kartları.
* **Fragman & Hızlı İşlem:** Doğrudan kart üzerinden fragman açma ve favorilere/izleme listesine ekleme.

### 💬 Topluluk & Sosyal Etkileşim
* **Yorum ve Derecelendirme:** Yıldızlı puanlama, detaylı eleştiri yazma ve spoiler korumalı gizlenebilir yorum alanı.
* **Yorum Yanıtları:** Yorumların altına yanıt verebilme ve tartışma başlatabilme.
* **Arkadaşlık Sistemi:** Kullanıcı adı arama, arkadaş ekleme, arkadaşların izleme geçmişini ve aktivitelerini canlı takip etme.

### 🔔 Akıllı Bildirimler
* **Push Notifications:** Firebase Cloud Messaging (FCM) ve Supabase Edge Functions (`friend-push`) altyapısı ile arkadaşlık istekleri ve bildirim iletimi.

---

## 🏗️ Mimari ve Tasarım Desenleri

CineVerse, **Google** tarafından önerilen en güncel Android mimari standartlarına tam uyumlu olarak geliştirilmiştir:

```
com.arda.cineverse/
├── data/
│   ├── common/             # SyncResult, Exception sınıfları
│   ├── connectivity/       # Canlı ağ durumu takip mekanizması (ConnectivityObserver)
│   ├── local/              # Room Veritabanı, DAO'lar, Entity'ler ve DataStore Önbelleği
│   ├── model/              # Domain modelleri (Movie, SavedMovie, Comment, etc.)
│   ├── remote/             # Retrofit servisleri (TMDB API, Gemini API, Supabase Push Client)
│   └── repository/         # İş mantığı ve veri kaynağı yönetimi (Single Source of Truth)
├── di/                     # Dependency Injection (Hilt modülleri)
├── notifications/          # Firebase Messaging Service & WorkManager Workers
├── ui/
│   ├── components/         # Reusable Jetpack Compose bileşenleri (Buttons, Cards, Dialogs)
│   ├── screens/            # Ekran Compose kodları (Home, Detail, Reels, MyList, Profile, etc.)
│   └── theme/              # Renk paleti, Tipografi, Dinamik Tema yönetimi
└── viewmodel/              # StateFlow destekli MVVM ViewModel katmanı
```

* **Clean Architecture:** Data, Domain ve UI katmanları net bir şekilde ayrıştırılmıştır.
* **MVVM + Unidirectional Data Flow (UDF):** Tüm ekranlar durumlara (*StateFlow*) bağlı çalışır. UI yalnızca durumları gözlemler ve kullanıcı aksiyonlarını Event olarak fırlatır.
* **Dependency Injection:** Hilt (Dagger) kullanılarak nesne bağımlılıkları yönetilir.

---

## 🛠️ Teknoloji Yığını

* **Dil:** 100% [Kotlin](https://kotlinlang.org/)
* **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material3)
* **Asenkron İşlemler:** Kotlin Coroutines & Reactive [Flow](https://kotlinlang.org/docs/flow.html)
* **Bağımlılık Enjeksiyonu:** [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
* **Yerel Veritabanı:** [Room Database](https://developer.android.com/training/data-storage/room) & [Preferences DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
* **Ağ & Ağ İstemcisi:** [Retrofit 2](https://square.github.io/retrofit/) + [OkHttp 4](https://square.github.io/okhttp/) + [Gson](https://github.com/google/gson)
* **Görsel Yükleme:** [Coil Compose](https://coil-kt.github.io/coil/compose/)
* **Bulut & Arka Plan Servisleri:** 
  * Firebase Auth (E-posta / Şifre)
  * Firebase Firestore (Cloud NoSQL Database)
  * Firebase Cloud Messaging (FCM)
  * Supabase Edge Functions (Serverless TypeScript push dispatcher)
* **AI Servisi:** Google Generative AI REST API (Gemini)
* **Birim Testleri:** JUnit4, Coroutines Test Kit, Mockk

---

## ⚡ Çevrimdışı (Offline-First) Senkronizasyon

CineVerse, kesintisiz bir kullanıcı deneyimi sunmak amacıyla **Offline-First** felsefesine göre tasarlanmıştır:

1. **Room Önbelleği:** Kullanıcının favorileri, izleme listesi, profil bilgileri ve bildirimleri yerel SQLite veritabanında saklanır. Cihaz çevrimdışıyken uygulama tüm ekranları anında açar.
2. **Pending Actions Kuyruğu:** İnternet yokken yapılan ekleme/çıkarma işlemleri `PendingActionEntity` olarak saklanır. Filmin tüm öznitelikleri (başlık, afiş, puan, türler) JSON biçiminde muhafaza edilir.
3. **Otomatik Senkronizasyon:** Cihaz yeniden internete bağlandığında `UserListRepository.processPendingActions()` tetiklenerek çevrimdışı kuyruktaki işlemler Firestore'a tam haliyle aktarılır.

---

## 🚀 Kurulum ve Yapılandırma

### Gereksinimler
* Android Studio Ladybug / Jellyfish (veya üzeri)
* JDK 17+
* Android SDK (Min SDK: 24, Target SDK: 35)

### Adım Adım Kurulum

1. **Projeyi Klonlayın:**
   ```bash
   git clone https://github.com/Ardaferad35/CineVerse.git
   cd CineVerse
   ```

2. **API Anahtarlarını Tanımlayın:**
   Proje kök dizinindeki `local.properties` dosyasına aşağıdaki servis anahtarlarınızı ekleyin:
   ```properties
   TMDB_API_KEY=senin_tmdb_api_anahtarin
   GEMINI_API_KEY=senin_gemini_api_anahtarin
   SUPABASE_URL=https://senin-projen.supabase.co
   SUPABASE_ANON_KEY=senin_supabase_anon_anahtarin
   ```

3. **Firebase Yapılandırması:**
   * Firebase Console üzerinde bir proje oluşturun ve Android uygulamanızı (`com.arda.cineverse`) ekleyin.
   * İndirdiğiniz `google-services.json` dosyasını `app/` dizinine kopyalayın.

4. **Projeyi Derleyin ve Çalıştırın:**
   ```bash
   ./gradlew.bat assembleDebug
   ```

---

## 🧪 Test ve Kalite Güvencesi

Projede ViewModel ve Repository katmanları için birim testleri mevcuttur. Testleri çalıştırmak için:

```bash
./gradlew.bat test --continue
```

Başlıca test sınıfları:
* `AuthViewModelTest`
* `FriendsViewModelTest`
* `MovieDetailViewModelTest`
* `NotificationViewModelTest`
* `RecommendShareViewModelTest`

---

<p align="center">
  Developed with ❤️ for Movie & TV Show Lovers
</p>
