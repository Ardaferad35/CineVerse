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

    suspend fun isFavorite(movieId: Int): Boolean = runCatching {
        favoritesCollection().document(movieId.toString()).get().await().exists()
    }.getOrDefault(false)

    suspend fun isInWatchlist(movieId: Int): Boolean = runCatching {
        watchlistCollection().document(movieId.toString()).get().await().exists()
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
        favoritesCollection().document(movie.id.toString())
            .set(movie.copy(addedAt = System.currentTimeMillis())).await()
        Unit
    }

    suspend fun removeFavorite(movieId: Int): Result<Unit> = runCatching {
        favoritesCollection().document(movieId.toString()).delete().await()
        Unit
    }

    suspend fun addToWatchlist(movie: SavedMovie): Result<Unit> = runCatching {
        watchlistCollection().document(movie.id.toString())
            .set(movie.copy(addedAt = System.currentTimeMillis())).await()
        Unit
    }

    suspend fun removeFromWatchlist(movieId: Int): Result<Unit> = runCatching {
        watchlistCollection().document(movieId.toString()).delete().await()
        Unit
    }
}