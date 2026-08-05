package com.arda.cineverse.di

import android.content.Context
import com.arda.cineverse.data.connectivity.ConnectivityObserver
import com.arda.cineverse.data.local.dao.CategoryDao
import com.arda.cineverse.data.local.dao.FeaturedDao
import com.arda.cineverse.data.local.dao.FriendDao
import com.arda.cineverse.data.local.dao.MovieDao
import com.arda.cineverse.data.local.dao.PendingActionDao
import com.arda.cineverse.data.local.dao.SavedMovieDao
import com.arda.cineverse.data.local.dao.TvEpisodeProgressDao
import com.arda.cineverse.data.local.dao.TvShowDao
import com.arda.cineverse.data.local.dao.WatchHistoryDao
import com.arda.cineverse.data.local.datastore.UserPreferencesRepository
import com.arda.cineverse.data.local.entity.SectionType
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Repository'lerin çoğu (MovieRepository, TvRepository, RecommendationRepository,
 * UserListRepository) proje genelindeki mevcut "default parametre ile kendi
 * bağımlılığını sağla" konvansiyonunu koruyor (bkz. TmdbNetworkModule.api).
 * Room/DataStore/Connectivity gibi Context gerektiren Hilt-yönetimli
 * singleton'lara bu konvansiyonu bozmadan erişebilmek için resmi Hilt
 * "manuel entry point" mekanizması kullanılıyor: Application zaten Hilt'in
 * SingletonComponent'ini barındırıyor, [EntryPoints.get] ile buradan
 * singleton'lar çekilebiliyor.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppEntryPoint {
    fun movieDao(): MovieDao
    fun tvShowDao(): TvShowDao
    fun categoryDao(): CategoryDao
    fun featuredDao(): FeaturedDao
    fun savedMovieDao(): SavedMovieDao
    fun watchHistoryDao(): WatchHistoryDao
    fun friendDao(): FriendDao
    fun tvEpisodeProgressDao(): TvEpisodeProgressDao
    fun pendingActionDao(): PendingActionDao
    fun connectivityObserver(): ConnectivityObserver
    fun userPreferencesRepository(): UserPreferencesRepository
}

object AppGraph {
    private lateinit var entryPoint: AppEntryPoint

    fun init(context: Context) {
        entryPoint = EntryPoints.get(context.applicationContext, AppEntryPoint::class.java)
    }

    val movieDao: MovieDao get() = entryPoint.movieDao()
    val tvShowDao: TvShowDao get() = entryPoint.tvShowDao()
    val categoryDao: CategoryDao get() = entryPoint.categoryDao()
    val featuredDao: FeaturedDao get() = entryPoint.featuredDao()
    val savedMovieDao: SavedMovieDao get() = entryPoint.savedMovieDao()
    val watchHistoryDao: WatchHistoryDao get() = entryPoint.watchHistoryDao()
    val friendDao: FriendDao get() = entryPoint.friendDao()
    val tvEpisodeProgressDao: TvEpisodeProgressDao get() = entryPoint.tvEpisodeProgressDao()
    val pendingActionDao: PendingActionDao get() = entryPoint.pendingActionDao()
    val connectivityObserver: ConnectivityObserver get() = entryPoint.connectivityObserver()
    val userPreferencesRepository: UserPreferencesRepository get() = entryPoint.userPreferencesRepository()

    /**
     * Çıkış yapıldığında çağrılır: Room'daki hesaba özel verileri (favoriler,
     * izleme listesi, izleme geçmişi, kişiselleştirilmiş "Size Özel" önerileri)
     * temizler. Aksi halde aynı cihazda farklı bir hesapla giriş yapan
     * kullanıcı, senkron tamamlanana kadar önceki hesabın offline cache'ini
     * görebilir (hesaplar arası veri sızıntısı).
     */
    suspend fun clearUserScopedCache() {
        savedMovieDao.clearAll()
        watchHistoryDao.clearAll()
        friendDao.clearAll()
        tvEpisodeProgressDao.clearAll()
        pendingActionDao.clearAll()
        movieDao.clearSection(SectionType.FOR_YOU)
        tvShowDao.clearSection(SectionType.TV_FOR_YOU)
    }
}
