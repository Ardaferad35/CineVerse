package com.arda.cineverse.data.model

data class ReelItem(
    val id: Int,
    val title: String,
    val overview: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val rating: Double,
    val year: Int?,
    val genres: List<String>,
    val genreIds: List<Int> = emptyList(),
    val trailerKey: String,
    val mediaType: String, // "movie" or "tv"
    val videoUrl: String? = null,
)
