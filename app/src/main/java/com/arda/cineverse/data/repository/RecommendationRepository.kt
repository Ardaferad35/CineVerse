package com.arda.cineverse.data.repository

import com.arda.cineverse.data.model.Movie
import com.arda.cineverse.data.model.TvShow
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

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
 */
class RecommendationRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val movieRepository: MovieRepository = MovieRepository(),
    private val tvRepository: TvRepository = TvRepository(),
) {
    // "movieId" alan adı tarihi nedenlerle böyle kaldı ama film ve dizi
    // pencereleri AYRI Firestore alanlarında tutulduğu için (recentlyViewed/
    // recentFavorites vs. recentlyViewedTv/recentFavoritesTv) bu sınıf
    // dizi id'leri için de sorunsuzca yeniden kullanılabiliyor.
    private data class MovieSignal(val movieId: Int, val genreIds: List<Int>) {
        fun toMap(): Map<String, Any> = mapOf("movieId" to movieId, "genreIds" to genreIds)
    }

    private fun userDoc(uid: String) = firestore.collection("users").document(uid)

    /** Bir film detayı açıldığında çağrılır. */
    suspend fun recordView(movieId: Int, genreIds: List<Int>) {
        val uid = auth.currentUser?.uid ?: return
        runCatching {
            addToWindow(uid, "recentlyViewed", movieId, genreIds)
        }.onFailure { error ->
            android.util.Log.e("CVRecommendations", "recordView başarısız oldu", error)
        }
    }

    /** Bir film favorilere eklendiğinde çağrılır. */
    suspend fun recordFavorite(movieId: Int, genreIds: List<Int>) {
        val uid = auth.currentUser?.uid ?: return
        runCatching {
            addToWindow(uid, "recentFavorites", movieId, genreIds)
        }.onFailure { error ->
            android.util.Log.e("CVRecommendations", "recordFavorite başarısız oldu", error)
        }
    }

    /**
     * Bir film favorilerden çıkarıldığında çağrılır. Sinyali pencereden
     * tamamen kaldırır — favorile/çıkar döngüsüyle puan şişirmeyi engeller.
     */
    suspend fun removeFavoriteSignal(movieId: Int) {
        val uid = auth.currentUser?.uid ?: return
        runCatching {
            val docRef = userDoc(uid)
            val snapshot = docRef.get().await()
            val current = readWindow(snapshot, "recentFavorites")
            val updated = current.filterNot { it.movieId == movieId }
            docRef.update("recentFavorites", updated.map { it.toMap() }).await()
        }.onFailure { error ->
            android.util.Log.e("CVRecommendations", "removeFavoriteSignal başarısız oldu", error)
        }
    }

    /** Bir dizi detayı açıldığında çağrılır. */
    suspend fun recordTvView(tvId: Int, genreIds: List<Int>) {
        val uid = auth.currentUser?.uid ?: return
        runCatching {
            addToWindow(uid, "recentlyViewedTv", tvId, genreIds)
        }.onFailure { error ->
            android.util.Log.e("CVRecommendations", "recordTvView başarısız oldu", error)
        }
    }

    /** Bir dizi favorilere eklendiğinde çağrılır. */
    suspend fun recordTvFavorite(tvId: Int, genreIds: List<Int>) {
        val uid = auth.currentUser?.uid ?: return
        runCatching {
            addToWindow(uid, "recentFavoritesTv", tvId, genreIds)
        }.onFailure { error ->
            android.util.Log.e("CVRecommendations", "recordTvFavorite başarısız oldu", error)
        }
    }

    /**
     * Bir dizi favorilerden çıkarıldığında çağrılır. Sinyali pencereden
     * tamamen kaldırır — favorile/çıkar döngüsüyle puan şişirmeyi engeller.
     */
    suspend fun removeTvFavoriteSignal(tvId: Int) {
        val uid = auth.currentUser?.uid ?: return
        runCatching {
            val docRef = userDoc(uid)
            val snapshot = docRef.get().await()
            val current = readWindow(snapshot, "recentFavoritesTv")
            val updated = current.filterNot { it.movieId == tvId }
            docRef.update("recentFavoritesTv", updated.map { it.toMap() }).await()
        }.onFailure { error ->
            android.util.Log.e("CVRecommendations", "removeTvFavoriteSignal başarısız oldu", error)
        }
    }

    private suspend fun addToWindow(uid: String, field: String, movieId: Int, genreIds: List<Int>) {
        val docRef = userDoc(uid)
        val snapshot = docRef.get().await()
        val current = readWindow(snapshot, field)
        // Aynı film zaten pencerede varsa önce çıkarılır, sonra en başa
        // (en taze konuma) tekrar eklenir — yinelenmeden, sadece güncellenir.
        val updated = (listOf(MovieSignal(movieId, genreIds)) + current.filterNot { it.movieId == movieId }).take(20)
        docRef.update(field, updated.map { it.toMap() }).await()
        android.util.Log.d("CVRecommendations", "$field güncellendi: movieId=$movieId genreIds=$genreIds yeni boyut=${updated.size}")
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
        android.util.Log.d(
            "CVRecommendations",
            "recentlyViewed=${recentlyViewed.size} film, recentFavorites=${recentFavorites.size} film",
        )

        if (recentlyViewed.isEmpty() && recentFavorites.isEmpty()) {
            android.util.Log.d("CVRecommendations", "İki pencere de boş, öneri üretilmeyecek")
            return@runCatching emptyList()
        }

        // Tür puanları HER SEFERİNDE bu iki pencereden anlık hesaplanıyor —
        // kalıcı bir sayaç yok. Favorileme 3 kat, izleme 1 kat ağırlıklı.
        val genreScores = mutableMapOf<Int, Int>()
        recentlyViewed.forEach { signal ->
            signal.genreIds.forEach { genreId -> genreScores[genreId] = (genreScores[genreId] ?: 0) + 1 }
        }
        recentFavorites.forEach { signal ->
            signal.genreIds.forEach { genreId -> genreScores[genreId] = (genreScores[genreId] ?: 0) + 3 }
        }
        android.util.Log.d("CVRecommendations", "Tür puanları: $genreScores")

        val topGenreIds = genreScores.entries.sortedByDescending { it.value }.take(2).map { it.key }
        android.util.Log.d("CVRecommendations", "En yüksek türler: $topGenreIds")
        if (topGenreIds.isEmpty()) return@runCatching emptyList()

        val excludeIds = (recentlyViewed.map { it.movieId } + recentFavorites.map { it.movieId }).toSet()

        val candidates = topGenreIds.flatMap { genreId ->
            movieRepository.getMoviesByGenre(genreId, page = 1).getOrDefault(emptyList())
        }
        android.util.Log.d("CVRecommendations", "Aday film sayısı: ${candidates.size}")

        val result = candidates.distinctBy { it.id }.filterNot { it.id in excludeIds }.take(15)
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
        android.util.Log.d(
            "CVRecommendations",
            "recentlyViewedTv=${recentlyViewed.size} dizi, recentFavoritesTv=${recentFavorites.size} dizi",
        )

        if (recentlyViewed.isEmpty() && recentFavorites.isEmpty()) {
            android.util.Log.d("CVRecommendations", "İki dizi penceresi de boş, öneri üretilmeyecek")
            return@runCatching emptyList()
        }

        val genreScores = mutableMapOf<Int, Int>()
        recentlyViewed.forEach { signal ->
            signal.genreIds.forEach { genreId -> genreScores[genreId] = (genreScores[genreId] ?: 0) + 1 }
        }
        recentFavorites.forEach { signal ->
            signal.genreIds.forEach { genreId -> genreScores[genreId] = (genreScores[genreId] ?: 0) + 3 }
        }
        android.util.Log.d("CVRecommendations", "Dizi tür puanları: $genreScores")

        val topGenreIds = genreScores.entries.sortedByDescending { it.value }.take(2).map { it.key }
        android.util.Log.d("CVRecommendations", "En yüksek dizi türleri: $topGenreIds")
        if (topGenreIds.isEmpty()) return@runCatching emptyList()

        val excludeIds = (recentlyViewed.map { it.movieId } + recentFavorites.map { it.movieId }).toSet()

        val candidates = topGenreIds.flatMap { genreId ->
            tvRepository.getTvShowsByGenre(genreId, page = 1).getOrDefault(emptyList())
        }
        android.util.Log.d("CVRecommendations", "Aday dizi sayısı: ${candidates.size}")

        val result = candidates.distinctBy { it.id }.filterNot { it.id in excludeIds }.take(15)
        android.util.Log.d("CVRecommendations", "Sonuç: ${result.size} dizi önerildi")
        result
    }.onFailure { error ->
        android.util.Log.e("CVRecommendations", "getTvRecommendations başarısız oldu", error)
    }
}