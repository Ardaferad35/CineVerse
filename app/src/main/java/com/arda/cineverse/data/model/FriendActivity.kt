package com.arda.cineverse.data.model

enum class FriendActivityType {
    FAVORITE,      // Favorilere ekledi
    WATCHLIST,     // İzleme listesine ekledi
    COMMENT,       // Yorum / Puan verdi
    RECOMMENDATION // Arkadaşına film önerdi
}

data class FriendActivity(
    val id: String = "",
    val type: FriendActivityType,
    val mediaId: Int,
    val mediaTitle: String,
    val mediaType: String = "movie",
    val posterUrl: String? = null,
    val timestamp: Long = 0L,
    val rating: Double? = null,
    val commentText: String? = null,
    val note: String? = null,
)
