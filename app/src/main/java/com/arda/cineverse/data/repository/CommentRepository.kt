package com.arda.cineverse.data.repository

import com.arda.cineverse.data.model.Comment
import com.arda.cineverse.data.model.Movie
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlin.math.round

class CommentRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    private fun commentsCollection(movieId: Int) =
        firestore.collection("movies").document(movieId.toString()).collection("comments")

    private fun ratingsCollection() = firestore.collection("movie_ratings")

    fun currentUserId(): String? = auth.currentUser?.uid

    suspend fun getComments(movieId: Int): Result<List<Comment>> = runCatching {
        commentsCollection(movieId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()
            .documents
            .mapNotNull { doc -> doc.toObject(Comment::class.java)?.copy(id = doc.id) }
    }

    suspend fun addComment(
        movieId: Int,
        text: String,
        rating: Int,
        isSpoiler: Boolean,
        movieTitle: String,
        moviePosterUrl: String?,
        movieYear: Int?,
        movieGenreIds: List<Int> = emptyList(),
    ): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("Yorum yapmak için giriş yapmalısınız")

        val userDoc = firestore.collection("users").document(user.uid).get().await()
        val displayName = userDoc.getString("fullName")
            ?: user.email?.substringBefore("@")
            ?: "Kullanıcı"

        val comment = hashMapOf(
            "movieId" to movieId,
            "userId" to user.uid,
            "userName" to displayName,
            "text" to text,
            "rating" to rating,
            "isSpoiler" to isSpoiler,
            "createdAt" to System.currentTimeMillis(),
            "editedAt" to null,
        )
        commentsCollection(movieId).add(comment).await()
        recalculateAggregate(movieId, movieTitle, moviePosterUrl, movieYear, movieGenreIds)
    }

    suspend fun updateComment(
        movieId: Int,
        commentId: String,
        text: String,
        rating: Int,
        isSpoiler: Boolean,
        movieTitle: String,
        moviePosterUrl: String?,
        movieYear: Int?,
        movieGenreIds: List<Int> = emptyList(),
    ): Result<Unit> = runCatching {
        val updates = mapOf(
            "text" to text,
            "rating" to rating,
            "isSpoiler" to isSpoiler,
            "editedAt" to System.currentTimeMillis(),
        )
        commentsCollection(movieId).document(commentId).update(updates).await()
        recalculateAggregate(movieId, movieTitle, moviePosterUrl, movieYear, movieGenreIds)
    }

    suspend fun deleteComment(
        movieId: Int,
        commentId: String,
        movieTitle: String,
        moviePosterUrl: String?,
        movieYear: Int?,
        movieGenreIds: List<Int> = emptyList(),
    ): Result<Unit> = runCatching {
        commentsCollection(movieId).document(commentId).delete().await()
        recalculateAggregate(movieId, movieTitle, moviePosterUrl, movieYear, movieGenreIds)
    }

    /**
     * Bir filmin tüm yorumlarını yeniden okuyup ortalama puanı hesaplar ve
     * "movie_ratings/{movieId}" belgesine (tür ID'leri dahil) yazar. Hiç yorum
     * kalmadıysa belgeyi tamamen siler.
     */
    private suspend fun recalculateAggregate(
        movieId: Int,
        movieTitle: String,
        moviePosterUrl: String?,
        movieYear: Int?,
        movieGenreIds: List<Int>,
    ) {
        val comments = commentsCollection(movieId).get().await()
            .documents.mapNotNull { it.toObject(Comment::class.java) }

        val ratingDoc = ratingsCollection().document(movieId.toString())
        if (comments.isEmpty()) {
            ratingDoc.delete().await()
        } else {
            val average = round(comments.map { it.rating }.average() * 10) / 10.0
            val data = hashMapOf(
                "movieId" to movieId,
                "title" to movieTitle,
                "posterUrl" to moviePosterUrl,
                "year" to movieYear,
                "averageRating" to average,
                "ratingCount" to comments.size,
                "genreIds" to movieGenreIds,
            )
            ratingDoc.set(data).await()
        }
    }

    /**
     * "Uygulama İçi Puan" sıralaması (genel Popüler Filmler ekranı için):
     * kullanıcılarımızın puanladığı tüm filmleri ortalama puana göre getirir.
     */
    suspend fun getTopRatedByAppUsers(page: Int = 1): Result<List<Movie>> = runCatching {
        if (page > 1) return@runCatching emptyList()

        ratingsCollection()
            .orderBy("averageRating", Query.Direction.DESCENDING)
            .limit(100)
            .get()
            .await()
            .documents
            .mapNotNull { doc -> docToMovie(doc) }
    }

    /**
     * "Uygulama İçi Puan" sıralaması (belirli bir tür/kategori için):
     * yalnızca o türe ait ve kullanıcılarımızca puanlanmış filmleri getirir.
     *
     * Not: Bu sorgu Firestore'da "composite index" gerektirebilir — ilk
     * çalıştırmada hata alırsanız, hata mesajındaki linke tıklayıp Firebase
     * Console'da index'i tek tıkla oluşturabilirsiniz, birkaç dakika sürer.
     */
    suspend fun getTopRatedByAppUsersForGenre(genreId: Int, page: Int = 1): Result<List<Movie>> = runCatching {
        if (page > 1) return@runCatching emptyList()

        ratingsCollection()
            .whereArrayContains("genreIds", genreId)
            .orderBy("averageRating", Query.Direction.DESCENDING)
            .limit(100)
            .get()
            .await()
            .documents
            .mapNotNull { doc -> docToMovie(doc) }
    }

    private fun docToMovie(doc: com.google.firebase.firestore.DocumentSnapshot): Movie? {
        val movieId = doc.getLong("movieId")?.toInt() ?: return null
        return Movie(
            id = movieId,
            title = doc.getString("title") ?: "",
            year = doc.getLong("year")?.toInt(),
            genre = "",
            rating = doc.getDouble("averageRating") ?: 0.0,
            posterUrl = doc.getString("posterUrl"),
        )
    }
}