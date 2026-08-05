package com.arda.cineverse.data.repository

import com.arda.cineverse.data.common.SyncResult
import com.arda.cineverse.data.connectivity.ConnectivityObserver
import com.arda.cineverse.data.local.dao.MovieDao
import com.arda.cineverse.data.local.dao.TvShowDao
import com.arda.cineverse.data.local.dao.WatchHistoryDao
import com.arda.cineverse.data.local.entity.SectionType
import com.arda.cineverse.data.local.entity.WatchHistoryEntity
import com.arda.cineverse.data.local.mapper.toDomain
import com.arda.cineverse.data.local.mapper.toEntity
import com.arda.cineverse.data.model.Movie
import com.arda.cineverse.data.model.TvShow
import com.arda.cineverse.di.AppGraph
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * "Sizin İçin Önerilenler" (Ana Sayfa) için içerik-tabanlı basit bir öneri
 * sistemi.
 *
 * ÖNEMLİ TASARIM KARARI: Kalıcı, hiç azalmayan bir "tür puanı" sayacı
 * KULLANMIYORUZ. Bunun yerine kullanıcının en son etkileşimde bulunduğu
 * EN FAZLA 20 İZLENEN ve EN FAZLA 20 FAVORİLENEN filmi (kayan pencere,
 * "sliding window") saklıyoruz. Tür ilgisi, öneri her üretileceğinde bu
 * güncel pencereden AN ÜZERİNDE hesaplanıyor. Bu şu iki sorunu çözüyor:
 *
 * 1. Eski sinyaller otomatik "solar": Örneğin uzun zaman önce favorilenen
 *    bir film, yeni favoriler eklendikçe pencereden düşer ve önerileri
 *    etkilemeye devam etmez.
 * 2. Favorile/favoriden-çıkar döngüsüyle puan şişirilemez: Favoriden
 *    çıkarma, o filmi pencereden TAMAMEN SİLER (puanı azaltmaz, çünkü
 *    zaten kalıcı bir puan yok) — tekrar favorilemek sadece filmi pencereye
 *    geri koyar, birikimli bir avantaj sağlamaz.
 *
 * OFFLINE MOD: getRecommendations()/getTvRecommendations() hesaplaması hâlâ
 * online iken (Firestore + TMDB discover) yapılıyor — bu mantık DEĞİŞMEDİ.
 * refreshRecommendations()/refreshTvRecommendations() sonucu Room'a
 * (FOR_YOU/TV_FOR_YOU section) yazarak offline erişilebilir kılıyor; ayrıca
 * "son izlenen 20" pencerelerini watch_history tablosuna aynalıyor.
 */
class RecommendationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val movieRepository: MovieRepository,
    private val tvRepository: TvRepository,
    private val userListRepository: UserListRepository,
    private val movieDao: MovieDao,
    private val tvShowDao: TvShowDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val connectivityObserver: ConnectivityObserver,
) {
    // bkz. MovieRepository.Companion.default() — aynı gerekçe.
    companion object {
        fun default(): RecommendationRepository = RecommendationRepository(
            firestore = FirebaseFirestore.getInstance(),
            auth = FirebaseAuth.getInstance(),
            movieRepository = MovieRepository.default(),
            tvRepository = TvRepository.default(),
            userListRepository = UserListRepository.default(),
            movieDao = AppGraph.movieDao,
            tvShowDao = AppGraph.tvShowDao,
            watchHistoryDao = AppGraph.watchHistoryDao,
            connectivityObserver = AppGraph.connectivityObserver,
        )
    }
    // "movieId" alan adı tarihi nedenlerle böyle kaldı ama film ve dizi
    // pencereleri AYRI Firestore alanlarında tutulduğu için (recentlyViewed/
    // recentFavorites vs. recentlyViewedTv/recentFavoritesTv) bu sınıf
    // dizi id'leri için de sorunsuzca yeniden kullanılabiliyor.
    private data class MovieSignal(val movieId: Int, val genreIds: List<Int>) {
        fun toMap(): Map<String, Any> = mapOf("movieId" to movieId, "genreIds" to genreIds)
    }

    private data class SearchSignal(val genreIds: List<Int>, val timestamp: Long)

    private fun userDoc(uid: String) = firestore.collection("users").document(uid)

    /**
     * Sinyal pencerelerini (recentlyViewed/recentFavorites/vb.) okuyup
     * değiştirirken read-modify-write yerine Firestore transaction'ı kullanır.
     * Aksi halde art arda hızlı gelen iki çağrı (ör. hızlı favori aç/kapa,
     * ya da art arda birkaç film açma) aynı eski snapshot üzerinden okuyup
     * birbirinin yazdığını sessizce ezebilir (lost update) — bkz. sınıf
     * dokümantasyonundaki "kayan pencere" tasarımı, kaybolan bir sinyal
     * kullanıcıya hiç yansımaz.
     */
    private suspend fun <T> runUserDocTransaction(
        uid: String,
        block: (DocumentSnapshot) -> Pair<Map<String, Any>, T>,
    ): T {
        val docRef = userDoc(uid)
        return firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val (updates, result) = block(snapshot)
            transaction.update(docRef, updates)
            result
        }.await()
    }

    // --- Offline-first: Room'dan gözlem ---

    fun observeRecommendedMovies(): Flow<List<Movie>> =
        movieDao.observeSection(SectionType.FOR_YOU).map { list -> list.map { it.toDomain() } }

    fun observeRecommendedTvShows(): Flow<List<TvShow>> =
        tvShowDao.observeSection(SectionType.TV_FOR_YOU).map { list -> list.map { it.toDomain() } }

    suspend fun refreshRecommendations(): SyncResult {
        if (!connectivityObserver.isCurrentlyOnline()) return SyncResult.Offline
        return getRecommendations().fold(
            onSuccess = { movies ->
                val syncedAt = System.currentTimeMillis()
                movieDao.replaceSection(SectionType.FOR_YOU, movies.map { it.toEntity(syncedAt) }, syncedAt)
                syncWatchHistoryFromFirestore()
                SyncResult.Success
            },
            onFailure = { SyncResult.Error(it) },
        )
    }

    suspend fun refreshTvRecommendations(): SyncResult {
        if (!connectivityObserver.isCurrentlyOnline()) return SyncResult.Offline
        return getTvRecommendations().fold(
            onSuccess = { shows ->
                val syncedAt = System.currentTimeMillis()
                tvShowDao.replaceSection(SectionType.TV_FOR_YOU, shows.map { it.toEntity(syncedAt) }, syncedAt)
                syncWatchHistoryFromFirestore()
                SyncResult.Success
            },
            onFailure = { SyncResult.Error(it) },
        )
    }

    /** Firestore'daki recentlyViewed/recentlyViewedTv pencerelerini watch_history tablosuna aynalar. */
    private suspend fun syncWatchHistoryFromFirestore() {
        val uid = auth.currentUser?.uid ?: return
        runCatching {
            val snapshot = userDoc(uid).get().await()
            val viewedMovies = readWindow(snapshot, "recentlyViewed")
            watchHistoryDao.replaceAll(
                mediaType = "movie",
                items = viewedMovies.mapIndexed { index, signal ->
                    WatchHistoryEntity(signal.movieId, "movie", signal.genreIds, System.currentTimeMillis(), index)
                },
            )
            val viewedTv = readWindow(snapshot, "recentlyViewedTv")
            watchHistoryDao.replaceAll(
                mediaType = "tv",
                items = viewedTv.mapIndexed { index, signal ->
                    WatchHistoryEntity(signal.movieId, "tv", signal.genreIds, System.currentTimeMillis(), index)
                },
            )
        }.onFailure { error ->
            android.util.Log.e("CVRecommendations", "syncWatchHistoryFromFirestore başarısız oldu", error)
        }
    }

    /** Bir film detayı açıldığında çağrılır (+3 Puan sinyali). */
    suspend fun recordView(movieId: Int, genreIds: List<Int>) {
        val uid = auth.currentUser?.uid ?: return
        runCatching {
            addToWindow(uid, "recentlyViewed", movieId, genreIds)
        }.onFailure { error ->
            android.util.Log.e("CVRecommendations", "recordView başarısız oldu", error)
        }
    }

    /** Bir film favorilere eklendiğinde çağrılır (+10 Puan sinyali). */
    suspend fun recordFavorite(movieId: Int, genreIds: List<Int>) {
        val uid = auth.currentUser?.uid ?: return
        runCatching {
            addToWindow(uid, "recentFavorites", movieId, genreIds)
        }.onFailure { error ->
            android.util.Log.e("CVRecommendations", "recordFavorite başarısız oldu", error)
        }
    }

    /**
     * Bir film favorilerden çıkarıldığında çağrılır.
     * Hem favori sinyalini hem de tıklama/izleme sinyallerini TAMAMEN TEMİZLER!
     */
    suspend fun removeFavoriteSignal(movieId: Int) {
        val uid = auth.currentUser?.uid ?: return
        runCatching {
            runUserDocTransaction(uid) { snapshot ->
                val updatedFavs = readWindow(snapshot, "recentFavorites").filterNot { it.movieId == movieId }
                val updatedViews = readWindow(snapshot, "recentlyViewed").filterNot { it.movieId == movieId }
                mapOf(
                    "recentFavorites" to updatedFavs.map { it.toMap() },
                    "recentlyViewed" to updatedViews.map { it.toMap() },
                ) to Unit
            }
        }.onFailure { error ->
            android.util.Log.e("CVRecommendations", "removeFavoriteSignal başarısız oldu", error)
        }
    }

    /** Bir dizi detayı açıldığında çağrılır (+3 Puan sinyali). */
    suspend fun recordTvView(tvId: Int, genreIds: List<Int>) {
        val uid = auth.currentUser?.uid ?: return
        runCatching {
            addToWindow(uid, "recentlyViewedTv", tvId, genreIds)
        }.onFailure { error ->
            android.util.Log.e("CVRecommendations", "recordTvView başarısız oldu", error)
        }
    }

    /** Bir dizi favorilere eklendiğinde çağrılır (+10 Puan sinyali). */
    suspend fun recordTvFavorite(tvId: Int, genreIds: List<Int>) {
        val uid = auth.currentUser?.uid ?: return
        runCatching {
            addToWindow(uid, "recentFavoritesTv", tvId, genreIds)
        }.onFailure { error ->
            android.util.Log.e("CVRecommendations", "recordTvFavorite başarısız oldu", error)
        }
    }

    /**
     * Bir dizi favorilerden çıkarıldığında çağrılır.
     * Hem favori sinyalini hem de tıklama/izleme sinyallerini TAMAMEN TEMİZLER!
     */
    suspend fun removeTvFavoriteSignal(tvId: Int) {
        val uid = auth.currentUser?.uid ?: return
        runCatching {
            runUserDocTransaction(uid) { snapshot ->
                val updatedFavs = readWindow(snapshot, "recentFavoritesTv").filterNot { it.movieId == tvId }
                val updatedViews = readWindow(snapshot, "recentlyViewedTv").filterNot { it.movieId == tvId }
                mapOf(
                    "recentFavoritesTv" to updatedFavs.map { it.toMap() },
                    "recentlyViewedTv" to updatedViews.map { it.toMap() },
                ) to Unit
            }
        }.onFailure { error ->
            android.util.Log.e("CVRecommendations", "removeTvFavoriteSignal başarısız oldu", error)
        }
    }

    /** Arama yapıldığında son 20 arama sinyaline kaydeder (+5 Puan). */
    suspend fun recordSearchSignal(genreIds: List<Int>, mediaType: String = "movie") {
        if (genreIds.isEmpty()) return
        val uid = auth.currentUser?.uid ?: return
        runCatching {
            val field = if (mediaType == "tv") "recentSearchesTv" else "recentSearches"
            runUserDocTransaction(uid) { snapshot ->
                val current = readSearchWindow(snapshot, field)
                val updated = (listOf(SearchSignal(genreIds, System.currentTimeMillis())) + current).take(20)
                mapOf(field to updated.map { mapOf("genreIds" to it.genreIds, "timestamp" to it.timestamp) }) to Unit
            }
        }.onFailure { error ->
            android.util.Log.e("CVRecommendations", "recordSearchSignal başarısız oldu", error)
        }
    }

    private suspend fun addToWindow(uid: String, field: String, movieId: Int, genreIds: List<Int>) {
        val newSize = runUserDocTransaction(uid) { snapshot ->
            val current = readWindow(snapshot, field)
            // Aynı film zaten pencerede varsa önce çıkarılır, sonra en başa
            // (en taze konuma) tekrar eklenir — yinelenmeden, sadece güncellenir.
            val updated = (listOf(MovieSignal(movieId, genreIds)) + current.filterNot { it.movieId == movieId }).take(20)
            mapOf(field to updated.map { it.toMap() }) to updated.size
        }
        android.util.Log.d("CVRecommendations", "$field güncellendi: movieId=$movieId genreIds=$genreIds yeni boyut=$newSize")
    }

    @Suppress("UNCHECKED_CAST")
    private fun readWindow(snapshot: DocumentSnapshot, field: String): List<MovieSignal> {
        val raw = snapshot.get(field) as? List<*> ?: return emptyList()
        return raw.mapNotNull { entry ->
            // Eski sistemden kalma düz sayı girişleri (örn. sadece "597") varsa
            // bunlar Map değildir, bu filmi atlayıp devam ediyoruz — ilk yeni
            // yazma işleminde alan zaten tamamen yeni formatla değiştirilecek.
            val map = entry as? Map<*, *> ?: return@mapNotNull null
            val movieId = (map["movieId"] as? Number)?.toInt() ?: return@mapNotNull null
            val genreIds = (map["genreIds"] as? List<*>)?.mapNotNull { (it as? Number)?.toInt() } ?: emptyList()
            MovieSignal(movieId, genreIds)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun readSearchWindow(snapshot: DocumentSnapshot, field: String): List<SearchSignal> {
        val raw = snapshot.get(field) as? List<*> ?: return emptyList()
        return raw.mapNotNull { entry ->
            val map = entry as? Map<*, *> ?: return@mapNotNull null
            val genreIds = (map["genreIds"] as? List<*>)?.mapNotNull { (it as? Number)?.toInt() } ?: emptyList()
            val timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
            SearchSignal(genreIds, timestamp)
        }
    }

    /**
     * En yüksek ilgi gösterilen 1-2 türden, kullanıcının son zamanlarda
     * zaten izlediği/favorilediği filmler hariç, popüler filmleri getirir.
     * Hiçbir sinyal yoksa (yeni kullanıcı) boş liste döner — Ana Sayfa bu
     * durumda bölümü hiç göstermiyor.
     */
    suspend fun getRecommendations(): Result<List<Movie>> = runCatching {
        val uid = auth.currentUser?.uid ?: return@runCatching emptyList()
        val snapshot = userDoc(uid).get().await()

        val recentlyViewed = readWindow(snapshot, "recentlyViewed")
        val recentFavorites = readWindow(snapshot, "recentFavorites")
        val recentSearches = readSearchWindow(snapshot, "recentSearches")
        val activeFavorites = userListRepository.getFavorites().getOrDefault(emptyList()).filter { it.mediaType == "movie" }

        val genreScores = mutableMapOf<Int, Int>()

        // 1. Aktif Favoriler (En Yüksek Öncelik: +10 Puan)
        activeFavorites.forEach { fav ->
            fav.genreIds.forEach { genreId -> genreScores[genreId] = (genreScores[genreId] ?: 0) + 10 }
        }

        // 2. Son 20 Arama Sinyali (+5 Puan)
        recentSearches.forEach { search ->
            search.genreIds.forEach { genreId -> genreScores[genreId] = (genreScores[genreId] ?: 0) + 5 }
        }

        // 3. Son 20 Tıklama / İzleme Sinyali (+3 Puan)
        recentlyViewed.forEach { signal ->
            signal.genreIds.forEach { genreId -> genreScores[genreId] = (genreScores[genreId] ?: 0) + 3 }
        }

        // 4. Firestore Geçmiş Favori Sinyalleri (+2 Puan)
        recentFavorites.forEach { signal ->
            signal.genreIds.forEach { genreId -> genreScores[genreId] = (genreScores[genreId] ?: 0) + 2 }
        }
        android.util.Log.d("CVRecommendations", "Film çoklu sinyal tür puanları: $genreScores")

        val topGenreIds = genreScores.entries.sortedByDescending { it.value }.take(2).map { it.key }
        android.util.Log.d("CVRecommendations", "En yüksek film türleri: $topGenreIds")
        if (topGenreIds.isEmpty()) return@runCatching emptyList()

        val excludeIds = (recentlyViewed.map { it.movieId } + recentFavorites.map { it.movieId } + activeFavorites.map { it.id }).toSet()

        // 2 tür x 3 sayfa = 6 TMDB çağrısı paralel atılıyor — sırayla
        // beklemek öneri yenilemesini (her Home senkronunda çalışıyor)
        // gereksiz yere 6 gidiş-dönüş süresine uzatıyordu.
        val candidateList = coroutineScope {
            topGenreIds.flatMap { genreId ->
                (1..3).map { page ->
                    async { movieRepository.getMoviesByGenre(genreId, page = page).getOrDefault(emptyList()) }
                }
            }.awaitAll().flatten()
        }

        // 1. Kullanıcının Pozitif İlgi Gösterdiği Türler Kümesi
        val preferredGenreSet = topGenreIds.toSet()

        // 2. Dinamik Genel Filtreleme ve Puanlama (Tüm Türler İçin Evrensel Mantık)
        val result = candidateList
            .distinctBy { it.id }
            .filterNot { it.id in excludeIds }
            .filter { movie ->
                val preferredCount = movie.genreIds.count { it in preferredGenreSet }
                val totalGenreCount = movie.genreIds.size.coerceAtLeast(1)
                val preferredRatio = preferredCount.toDouble() / totalGenreCount
                // İçerikte kullanıcının tercih ettiği tür ağırlıkta olmalı (en az %40 uyum)
                preferredCount > 0 && (preferredRatio >= 0.40 || preferredCount >= 2)
            }
            .map { movie ->
                val matchingGenreCount = movie.genreIds.count { it in preferredGenreSet }
                val totalGenreCount = movie.genreIds.size.coerceAtLeast(1)
                val genreAffinityRatio = matchingGenreCount.toDouble() / totalGenreCount
                val score = (matchingGenreCount * 10.0) + (genreAffinityRatio * 5.0) + (movie.rating * 1.5)
                movie to score
            }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(15)

        android.util.Log.d("CVRecommendations", "Sonuç: ${result.size} film önerildi")
        result
    }.onFailure { error ->
        android.util.Log.e("CVRecommendations", "getRecommendations başarısız oldu", error)
    }

    /**
     * getRecommendations() ile birebir aynı mantık — sadece dizi pencerelerinden
     * (recentlyViewedTv/recentFavoritesTv) okuyup tür adaylarını
     * TvRepository.getTvShowsByGenre() ile getiriyor.
     */
    suspend fun getTvRecommendations(): Result<List<TvShow>> = runCatching {
        val uid = auth.currentUser?.uid ?: return@runCatching emptyList()
        val snapshot = userDoc(uid).get().await()

        val recentlyViewed = readWindow(snapshot, "recentlyViewedTv")
        val recentFavorites = readWindow(snapshot, "recentFavoritesTv")
        val recentSearches = readSearchWindow(snapshot, "recentSearchesTv")
        val activeFavorites = userListRepository.getFavorites().getOrDefault(emptyList()).filter { it.mediaType == "tv" }

        val genreScores = mutableMapOf<Int, Int>()

        // 1. Aktif Favoriler (En Yüksek Öncelik: +10 Puan)
        activeFavorites.forEach { fav ->
            fav.genreIds.forEach { genreId -> genreScores[genreId] = (genreScores[genreId] ?: 0) + 10 }
        }

        // 2. Son 20 Arama Sinyali (+5 Puan)
        recentSearches.forEach { search ->
            search.genreIds.forEach { genreId -> genreScores[genreId] = (genreScores[genreId] ?: 0) + 5 }
        }

        // 3. Son 20 Tıklama / İzleme Sinyali (+3 Puan)
        recentlyViewed.forEach { signal ->
            signal.genreIds.forEach { genreId -> genreScores[genreId] = (genreScores[genreId] ?: 0) + 3 }
        }

        // 4. Firestore Geçmiş Favori Sinyalleri (+2 Puan)
        recentFavorites.forEach { signal ->
            signal.genreIds.forEach { genreId -> genreScores[genreId] = (genreScores[genreId] ?: 0) + 2 }
        }
        android.util.Log.d("CVRecommendations", "Dizi çoklu sinyal tür puanları: $genreScores")

        val topGenreIds = genreScores.entries.sortedByDescending { it.value }.take(2).map { it.key }
        android.util.Log.d("CVRecommendations", "En yüksek dizi türleri: $topGenreIds")
        if (topGenreIds.isEmpty()) return@runCatching emptyList()

        val excludeIds = (recentlyViewed.map { it.movieId } + recentFavorites.map { it.movieId } + activeFavorites.map { it.id }).toSet()

        // Film tarafıyla aynı gerekçe: 6 TMDB çağrısı paralel.
        val candidateList = coroutineScope {
            topGenreIds.flatMap { genreId ->
                (1..3).map { page ->
                    async { tvRepository.getTvShowsByGenre(genreId, page = page).getOrDefault(emptyList()) }
                }
            }.awaitAll().flatten()
        }

        val preferredGenreSet = topGenreIds.toSet()

        val result = candidateList
            .distinctBy { it.id }
            .filterNot { it.id in excludeIds }
            .filter { show ->
                val preferredCount = show.genreIds.count { it in preferredGenreSet }
                val totalGenreCount = show.genreIds.size.coerceAtLeast(1)
                val preferredRatio = preferredCount.toDouble() / totalGenreCount
                // İçerikte kullanıcının tercih ettiği tür ağırlıkta olmalı (en az %40 uyum)
                preferredCount > 0 && (preferredRatio >= 0.40 || preferredCount >= 2)
            }
            .map { show ->
                val matchingGenreCount = show.genreIds.count { it in preferredGenreSet }
                val totalGenreCount = show.genreIds.size.coerceAtLeast(1)
                val genreAffinityRatio = matchingGenreCount.toDouble() / totalGenreCount
                val score = (matchingGenreCount * 10.0) + (genreAffinityRatio * 5.0) + (show.rating * 1.5)
                show to score
            }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(15)

        android.util.Log.d("CVRecommendations", "Sonuç: ${result.size} dizi önerildi")
        result
    }.onFailure { error ->
        android.util.Log.e("CVRecommendations", "getTvRecommendations başarısız oldu", error)
    }
}
