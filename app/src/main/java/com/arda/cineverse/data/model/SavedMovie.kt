package com.arda.cineverse.data.model

/**
 * Firestore'daki "users/{uid}/favorites" veya "users/{uid}/watchlist"
 * koleksiyonlarında tutulur. Tekrar TMDB'ye sormamak için gösterim
 * için gereken minimum bilgiyi kendi içinde taşır.
 */
data class SavedMovie(
    val id: Int = 0,
    val title: String = "",
    val posterUrl: String? = null,
    val rating: Double = 0.0,
    val year: Int? = null,
    val addedAt: Long = 0L,
    val genreIds: List<Int> = emptyList(),
    val mediaType: String = "movie",
)
