package com.arda.cineverse.data.model

/**
 * Firestore'daki "users/{uid}/notifications" koleksiyonunda tutulur.
 */
data class AppNotification(
    val id: String = "",
    val type: String = "", // "comment_reply", "upcoming_movie", "film_of_the_day"
    val title: String = "",
    val body: String = "",
    val movieId: Int? = null,
    val isRead: Boolean = false,
    val createdAt: Long = 0L,
)