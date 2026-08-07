package com.arda.cineverse.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Kanonik film verisi — bir film, birden fazla section'da görünse bile tek satır olarak tutulur. */
@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val year: Int?,
    val genre: String,
    val genres: List<String>,
    val genreIds: List<Int>,
    val rating: Double,
    val posterUrl: String?,
    val overview: String,
    val releaseDateLabel: String? = null,
    val cachedAt: Long,
)

@Entity(
    tableName = "movie_section_cross_ref",
    primaryKeys = ["section", "movieId"],
    indices = [
        Index(value = ["section"]),
        Index(value = ["movieId"]),
    ],
)
data class MovieSectionCrossRefEntity(
    val section: SectionType,
    val movieId: Int,
    val position: Int,
    val syncedAt: Long,
)
