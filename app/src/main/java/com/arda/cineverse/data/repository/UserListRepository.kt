package com.arda.cineverse.data.repository

import com.arda.cineverse.data.common.OfflineWriteException
import com.arda.cineverse.data.common.SyncResult
import com.arda.cineverse.data.connectivity.ConnectivityObserver
import com.arda.cineverse.data.local.dao.PendingActionDao
import com.arda.cineverse.data.local.dao.SavedMovieDao
import com.arda.cineverse.data.local.entity.PendingActionEntity
import com.arda.cineverse.data.local.mapper.toDomain
import com.arda.cineverse.data.local.mapper.toEntity
import com.arda.cineverse.data.model.SavedMovie
import com.arda.cineverse.di.AppGraph
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

import com.google.gson.Gson

private const val LIST_FAVORITE = "FAVORITE"
private const val LIST_WATCHLIST = "WATCHLIST"

/**
 * Favoriler/İzleme Listesi Firestore'da tutuluyor (hesaba bağlı, çok cihazlı senkronizasyon).
 * Room ("saved_movies") çevrimdışı önbellektir: kullanıcı internet olmadan da ekleme/çıkarma
 * yapabilir, değişiklikler Room'a yansır ve [PendingActionEntity] ile çevrimdışı işlem
 * kuyruğuna yazılır. İnternet bağlantısı geldiğinde [processPendingActions] tüm alanlarıyla
 * (başlık, afiş, puan, türler vb.) verileri Firestore'a aktarır.
 */
class UserListRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val savedMovieDao: SavedMovieDao,
    private val pendingActionDao: PendingActionDao,
    private val connectivityObserver: ConnectivityObserver,
) {
    private val gson = Gson()

    // bkz. MovieRepository.Companion.default() — aynı gerekçe.
    companion object {
        fun default(): UserListRepository = UserListRepository(
            firestore = FirebaseFirestore.getInstance(),
            auth = FirebaseAuth.getInstance(),
            savedMovieDao = AppGraph.savedMovieDao,
            pendingActionDao = AppGraph.pendingActionDao,
            connectivityObserver = AppGraph.connectivityObserver,
        )
    }
    private fun requireUid(): String = auth.currentUser?.uid ?: error("Giriş yapmalısınız")

    private fun favoritesCollection() =
        firestore.collection("users").document(requireUid()).collection("favorites")

    private fun watchlistCollection() =
        firestore.collection("users").document(requireUid()).collection("watchlist")

    private fun documentId(mediaId: Int, mediaType: String = "movie"): String =
        if (mediaType == "movie") mediaId.toString() else "${mediaType}_$mediaId"

    // --- Offline okuma: her zaman Room'dan ---

    fun observeFavorites(): Flow<List<SavedMovie>> =
        savedMovieDao.observeByListType(LIST_FAVORITE).map { list -> list.map { it.toDomain() } }

    fun observeWatchlist(): Flow<List<SavedMovie>> =
        savedMovieDao.observeByListType(LIST_WATCHLIST).map { list -> list.map { it.toDomain() } }

    /** Çevrimiçiyken çağrılır: Firestore'dan çekip Room'u tamamen değiştirir. */
    suspend fun syncFavoritesAndWatchlist(): SyncResult {
        if (!connectivityObserver.isCurrentlyOnline()) return SyncResult.Offline
        return runCatching {
            val favorites = getFavorites().getOrThrow()
            val watchlist = getWatchlist().getOrThrow()
            savedMovieDao.replaceAll(LIST_FAVORITE, favorites.map { it.toEntity(LIST_FAVORITE) })
            savedMovieDao.replaceAll(LIST_WATCHLIST, watchlist.map { it.toEntity(LIST_WATCHLIST) })
        }.fold(onSuccess = { SyncResult.Success }, onFailure = { SyncResult.Error(it) })
    }

    // Firestore yerine Room'a bakıyor — bkz. SavedMovieDao.exists(). Firestore
    // .get() offline'ken güvenilir şekilde cevap vermeyebileceğinden (SDK'nın
    // kendi cache'i o belgeyi hiç görmemiş olabilir), kalp/kaydet ikonunun
    // durumu eskiden offline'da hep "false" gösteriyordu — artık her zaman
    // Room'daki (senkron olduğu sürece güncel) gerçek durumu yansıtıyor.

    suspend fun isFavorite(movieId: Int): Boolean = isFavorite(movieId, mediaType = "movie")

    suspend fun isFavorite(mediaId: Int, mediaType: String): Boolean =
        savedMovieDao.exists(mediaId, mediaType, LIST_FAVORITE)

    suspend fun isInWatchlist(movieId: Int): Boolean = isInWatchlist(movieId, mediaType = "movie")

    suspend fun isInWatchlist(mediaId: Int, mediaType: String): Boolean =
        savedMovieDao.exists(mediaId, mediaType, LIST_WATCHLIST)

    suspend fun getFavorites(): Result<List<SavedMovie>> = runCatching {
        favoritesCollection()
            .orderBy("addedAt", Query.Direction.DESCENDING)
            .get().await()
            .documents.mapNotNull { it.toObject(SavedMovie::class.java) }
    }

    suspend fun getWatchlist(): Result<List<SavedMovie>> = runCatching {
        watchlistCollection()
            .orderBy("addedAt", Query.Direction.DESCENDING)
            .get().await()
            .documents.mapNotNull { it.toObject(SavedMovie::class.java) }
    }

    private fun activitiesCollection() =
        firestore.collection("users").document(requireUid()).collection("activities")

    suspend fun addFavorite(movie: SavedMovie): Result<Unit> {
        val now = System.currentTimeMillis()
        val movieWithTime = movie.copy(addedAt = now)
        savedMovieDao.upsert(movieWithTime.toEntity(LIST_FAVORITE))

        if (!connectivityObserver.isCurrentlyOnline()) {
            pendingActionDao.insert(
                PendingActionEntity(
                    actionType = "ADD_FAVORITE",
                    mediaId = movie.id,
                    mediaType = movie.mediaType,
                    payloadJson = gson.toJson(movieWithTime),
                ),
            )
            return Result.success(Unit)
        }

        return runCatching {
            favoritesCollection().document(documentId(movie.id, movie.mediaType))
                .set(movieWithTime).await()

            runCatching {
                val actId = "fav_${movie.mediaType}_${movie.id}"
                activitiesCollection().document(actId).set(
                    hashMapOf(
                        "id" to actId,
                        "type" to "FAVORITE",
                        "mediaId" to movie.id,
                        "mediaTitle" to movie.title,
                        "mediaType" to movie.mediaType,
                        "posterUrl" to movie.posterUrl,
                        "timestamp" to now,
                        "rating" to movie.rating,
                    ),
                ).await()
            }
            Unit
        }.onSuccess { syncFavoritesAndWatchlist() }
    }

    suspend fun removeFavorite(movieId: Int): Result<Unit> = removeFavorite(movieId, mediaType = "movie")

    suspend fun removeFavorite(mediaId: Int, mediaType: String): Result<Unit> {
        savedMovieDao.delete(mediaId, mediaType, LIST_FAVORITE)

        if (!connectivityObserver.isCurrentlyOnline()) {
            pendingActionDao.insert(
                PendingActionEntity(
                    actionType = "REMOVE_FAVORITE",
                    mediaId = mediaId,
                    mediaType = mediaType,
                ),
            )
            return Result.success(Unit)
        }

        return runCatching {
            favoritesCollection().document(documentId(mediaId, mediaType)).delete().await()
            runCatching { activitiesCollection().document("fav_${mediaType}_${mediaId}").delete().await() }
            Unit
        }.onSuccess { syncFavoritesAndWatchlist() }
    }

    suspend fun addToWatchlist(movie: SavedMovie): Result<Unit> {
        val now = System.currentTimeMillis()
        val movieWithTime = movie.copy(addedAt = now)
        savedMovieDao.upsert(movieWithTime.toEntity(LIST_WATCHLIST))

        if (!connectivityObserver.isCurrentlyOnline()) {
            pendingActionDao.insert(
                PendingActionEntity(
                    actionType = "ADD_WATCHLIST",
                    mediaId = movie.id,
                    mediaType = movie.mediaType,
                    payloadJson = gson.toJson(movieWithTime),
                ),
            )
            return Result.success(Unit)
        }

        return runCatching {
            watchlistCollection().document(documentId(movie.id, movie.mediaType))
                .set(movieWithTime).await()

            runCatching {
                val actId = "watch_${movie.mediaType}_${movie.id}"
                activitiesCollection().document(actId).set(
                    hashMapOf(
                        "id" to actId,
                        "type" to "WATCHLIST",
                        "mediaId" to movie.id,
                        "mediaTitle" to movie.title,
                        "mediaType" to movie.mediaType,
                        "posterUrl" to movie.posterUrl,
                        "timestamp" to now,
                        "rating" to movie.rating,
                    ),
                ).await()
            }
            Unit
        }.onSuccess { syncFavoritesAndWatchlist() }
    }

    suspend fun removeFromWatchlist(movieId: Int): Result<Unit> = removeFromWatchlist(movieId, mediaType = "movie")

    suspend fun removeFromWatchlist(mediaId: Int, mediaType: String): Result<Unit> {
        savedMovieDao.delete(mediaId, mediaType, LIST_WATCHLIST)

        if (!connectivityObserver.isCurrentlyOnline()) {
            pendingActionDao.insert(
                PendingActionEntity(
                    actionType = "REMOVE_WATCHLIST",
                    mediaId = mediaId,
                    mediaType = mediaType,
                ),
            )
            return Result.success(Unit)
        }

        return runCatching {
            watchlistCollection().document(documentId(mediaId, mediaType)).delete().await()
            runCatching { activitiesCollection().document("watch_${mediaType}_${mediaId}").delete().await() }
            Unit
        }.onSuccess { syncFavoritesAndWatchlist() }
    }

    suspend fun processPendingActions(): Boolean {
        if (!connectivityObserver.isCurrentlyOnline()) return false
        val pending = pendingActionDao.getAllPendingActions()
        if (pending.isEmpty()) return true

        for (action in pending) {
            val res = runCatching {
                when (action.actionType) {
                    "ADD_FAVORITE" -> {
                        val savedMovie = action.payloadJson?.let {
                            runCatching { gson.fromJson(it, SavedMovie::class.java) }.getOrNull()
                        } ?: savedMovieDao.getById(action.mediaId, action.mediaType)?.toDomain()

                        if (savedMovie != null) {
                            favoritesCollection().document(documentId(savedMovie.id, savedMovie.mediaType))
                                .set(savedMovie).await()
                            runCatching {
                                val actId = "fav_${savedMovie.mediaType}_${savedMovie.id}"
                                activitiesCollection().document(actId).set(
                                    hashMapOf(
                                        "id" to actId,
                                        "type" to "FAVORITE",
                                        "mediaId" to savedMovie.id,
                                        "mediaTitle" to savedMovie.title,
                                        "mediaType" to savedMovie.mediaType,
                                        "posterUrl" to savedMovie.posterUrl,
                                        "timestamp" to savedMovie.addedAt,
                                        "rating" to savedMovie.rating,
                                    ),
                                ).await()
                            }
                        } else {
                            val now = System.currentTimeMillis()
                            favoritesCollection().document(documentId(action.mediaId, action.mediaType))
                                .set(mapOf("id" to action.mediaId, "mediaType" to action.mediaType, "addedAt" to now)).await()
                        }
                    }
                    "REMOVE_FAVORITE" -> {
                        favoritesCollection().document(documentId(action.mediaId, action.mediaType)).delete().await()
                        runCatching { activitiesCollection().document("fav_${action.mediaType}_${action.mediaId}").delete().await() }
                    }
                    "ADD_WATCHLIST" -> {
                        val savedMovie = action.payloadJson?.let {
                            runCatching { gson.fromJson(it, SavedMovie::class.java) }.getOrNull()
                        } ?: savedMovieDao.getById(action.mediaId, action.mediaType)?.toDomain()

                        if (savedMovie != null) {
                            watchlistCollection().document(documentId(savedMovie.id, savedMovie.mediaType))
                                .set(savedMovie).await()
                            runCatching {
                                val actId = "watch_${savedMovie.mediaType}_${savedMovie.id}"
                                activitiesCollection().document(actId).set(
                                    hashMapOf(
                                        "id" to actId,
                                        "type" to "WATCHLIST",
                                        "mediaId" to savedMovie.id,
                                        "mediaTitle" to savedMovie.title,
                                        "mediaType" to savedMovie.mediaType,
                                        "posterUrl" to savedMovie.posterUrl,
                                        "timestamp" to savedMovie.addedAt,
                                        "rating" to savedMovie.rating,
                                    ),
                                ).await()
                            }
                        } else {
                            val now = System.currentTimeMillis()
                            watchlistCollection().document(documentId(action.mediaId, action.mediaType))
                                .set(mapOf("id" to action.mediaId, "mediaType" to action.mediaType, "addedAt" to now)).await()
                        }
                    }
                    "REMOVE_WATCHLIST" -> {
                        watchlistCollection().document(documentId(action.mediaId, action.mediaType)).delete().await()
                        runCatching { activitiesCollection().document("watch_${action.mediaType}_${action.mediaId}").delete().await() }
                    }
                }
            }
            if (res.isSuccess) {
                pendingActionDao.deleteById(action.id)
            }
        }
        syncFavoritesAndWatchlist()
        return true
    }
}
