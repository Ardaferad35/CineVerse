package com.arda.cineverse.data.repository

import com.arda.cineverse.data.model.SavedMovie
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class UserListRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    private fun requireUid(): String = auth.currentUser?.uid ?: error("Giriş yapmalısınız")

    private fun favoritesCollection() =
        firestore.collection("users").document(requireUid()).collection("favorites")

    private fun watchlistCollection() =
        firestore.collection("users").document(requireUid()).collection("watchlist")

    private fun documentId(mediaId: Int, mediaType: String = "movie"): String =
        if (mediaType == "movie") mediaId.toString() else "${mediaType}_$mediaId"

    suspend fun isFavorite(movieId: Int): Boolean = isFavorite(movieId, mediaType = "movie")

    suspend fun isFavorite(mediaId: Int, mediaType: String): Boolean = runCatching {
        favoritesCollection().document(documentId(mediaId, mediaType)).get().await().exists()
    }.getOrDefault(false)

    suspend fun isInWatchlist(movieId: Int): Boolean = isInWatchlist(movieId, mediaType = "movie")

    suspend fun isInWatchlist(mediaId: Int, mediaType: String): Boolean = runCatching {
        watchlistCollection().document(documentId(mediaId, mediaType)).get().await().exists()
    }.getOrDefault(false)

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

    suspend fun addFavorite(movie: SavedMovie): Result<Unit> = runCatching {
        favoritesCollection().document(documentId(movie.id, movie.mediaType))
            .set(movie.copy(addedAt = System.currentTimeMillis())).await()
        Unit
    }

    suspend fun removeFavorite(movieId: Int): Result<Unit> = removeFavorite(movieId, mediaType = "movie")

    suspend fun removeFavorite(mediaId: Int, mediaType: String): Result<Unit> = runCatching {
        favoritesCollection().document(documentId(mediaId, mediaType)).delete().await()
        Unit
    }

    suspend fun addToWatchlist(movie: SavedMovie): Result<Unit> = runCatching {
        watchlistCollection().document(documentId(movie.id, movie.mediaType))
            .set(movie.copy(addedAt = System.currentTimeMillis())).await()
        Unit
    }

    suspend fun removeFromWatchlist(movieId: Int): Result<Unit> = removeFromWatchlist(movieId, mediaType = "movie")

    suspend fun removeFromWatchlist(mediaId: Int, mediaType: String): Result<Unit> = runCatching {
        watchlistCollection().document(documentId(mediaId, mediaType)).delete().await()
        Unit
    }
}
