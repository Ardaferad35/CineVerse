package com.arda.cineverse.data.model

data class Movie(
    val id: Int,
    val title: String,
    val year: Int?,
    val genre: String,
    val genres: List<String> = emptyList(),
    val genreIds: List<Int> = emptyList(),
    val rating: Double,
    val posterUrl: String? = null,
    val isFavorite: Boolean = false,
    val overview: String = "",
    val mediaType: String = "movie",
)

data class UpcomingMovie(
    val id: Int,
    val title: String,
    val releaseDateLabel: String,
    val year: Int?,
    val posterUrl: String? = null,
)

data class Category(
    val id: String,
    val label: String,
    val genreId: Int,
)

data class FeaturedMovie(
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

val mockCategories = listOf(
    Category("action", "Aksiyon", 28),
    Category("scifi", "Bilim Kurgu", 878),
    Category("drama", "Dram", 18),
    Category("comedy", "Komedi", 35),
    Category("horror", "Korku", 27),
    Category("animation", "Animasyon", 16),
)