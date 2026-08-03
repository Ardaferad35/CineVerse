package com.arda.cineverse.data.model

/** "users/{uid}" belgesinden kullanıcı adına göre bulunan herkese açık profil özeti. */
data class FriendSearchResult(
    val uid: String,
    val username: String,
    val fullName: String,
    val avatarId: String,
)
