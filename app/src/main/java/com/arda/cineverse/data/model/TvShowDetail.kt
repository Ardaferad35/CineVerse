package com.arda.cineverse.data.model

data class TvShowDetail(
    val id: Int,
    val name: String,
    val backdropUrl: String?,
    val posterUrl: String?,
    val tmdbRating: Double,
    val year: Int?,
    val seasonsLabel: String,
    val episodesLabel: String = "",
    val genres: List<String>,
    val genreIds: List<Int> = emptyList(),
    val overview: String,
    val createdBy: String?,
    val cast: List<CastMember>,
    val trailerKey: String?,
    val seasons: List<TvSeasonSummary> = emptyList(),
    val similarShows: List<TvShow>,
)
