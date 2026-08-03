package com.arda.cineverse.data.model

/**
 * Firestore'daki "users/{uid}/friendRequests" koleksiyonunda tutulur. Belge
 * ID'si her zaman gönderenin (fromUid) uid'idir.
 */
data class FriendRequest(
    val fromUid: String = "",
    val fromUsername: String = "",
    val fromFullName: String = "",
    val fromAvatarId: String = "",
    val status: String = "pending", // "pending" or "accepted"
    val createdAt: Long = 0L,
)
