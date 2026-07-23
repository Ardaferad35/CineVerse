package com.arda.cineverse.data.repository

import com.arda.cineverse.data.model.Comment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class CommentRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    private fun commentsCollection(movieId: Int) =
        firestore.collection("movies").document(movieId.toString()).collection("comments")

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
    ): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("Yorum yapmak için giriş yapmalısınız")

        // Kullanıcının kayıt sırasında Firestore'a yazdığımız gerçek adını kullanıyoruz
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
        Unit
    }

    suspend fun updateComment(
        movieId: Int,
        commentId: String,
        text: String,
        rating: Int,
        isSpoiler: Boolean,
    ): Result<Unit> = runCatching {
        val updates = mapOf(
            "text" to text,
            "rating" to rating,
            "isSpoiler" to isSpoiler,
            "editedAt" to System.currentTimeMillis(),
        )
        commentsCollection(movieId).document(commentId).update(updates).await()
        Unit
    }

    suspend fun deleteComment(movieId: Int, commentId: String): Result<Unit> = runCatching {
        commentsCollection(movieId).document(commentId).delete().await()
        Unit
    }
}