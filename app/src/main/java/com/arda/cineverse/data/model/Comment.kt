package com.arda.cineverse.data.model

/**
 * Firestore'daki "movies/{movieId}/comments" koleksiyonunda tutulur.
 * Tüm alanlar varsayılan değerlere sahip olmalı (Firestore'un otomatik
 * nesneye çevirme işlemi için gerekli).
 */
data class Comment(
    val id: String = "",
    val movieId: Int = 0,
    val userId: String = "",
    val userName: String = "",
    val text: String = "",
    val rating: Int = 5,
    val isSpoiler: Boolean = false,
    val createdAt: Long = 0L,
    val editedAt: Long? = null,
)