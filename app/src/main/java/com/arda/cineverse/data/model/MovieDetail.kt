package com.arda.cineverse.data.model

data class CastMember(
    val id: Int,
    val name: String,
    val character: String,
    val profileUrl: String?,
)

data class MovieDetail(
    val id: Int,
    val title: String,
    val backdropUrl: String?,
    val posterUrl: String?,
    val tmdbRating: Double,
    val year: Int?,
    val durationLabel: String,
    val genres: List<String>,
    val overview: String,
    val director: String?,
    val cast: List<CastMember>,
    val trailerKey: String?,
    val similarMovies: List<Movie>,
)