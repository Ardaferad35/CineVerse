package com.arda.cineverse.data.model

data class Movie(
    val id: Int,
    val title: String,
    val year: Int?,
    val genre: String,
    val rating: Double,
    val posterUrl: String? = null,
    val isFavorite: Boolean = false,
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
    Category("action", "Aksiyon"),
    Category("scifi", "Bilim Kurgu"),
    Category("drama", "Dram"),
    Category("comedy", "Komedi"),
    Category("horror", "Korku"),
    Category("animation", "Animasyon"),
)