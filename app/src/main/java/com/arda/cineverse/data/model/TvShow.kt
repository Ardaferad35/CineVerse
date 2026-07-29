package com.arda.cineverse.data.model

data class TvShow(
    val id: Int,
    val name: String,
    val year: Int?,
    val genre: String,
    val genres: List<String> = emptyList(),
    val genreIds: List<Int> = emptyList(),
    val rating: Double,
    val posterUrl: String? = null,
    val overview: String = "",
)

data class FeaturedTvShow(
    val id: Int,
    val title: String,
    val rating: Double,
    val year: Int,
    val durationLabel: String,
    val genre: String,
    val description: String,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
)
