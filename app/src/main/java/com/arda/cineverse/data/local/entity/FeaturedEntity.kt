package com.arda.cineverse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Günün filmi/dizisi — tek satırlık slotlar, her yenilemede REPLACE edilir. */
@Entity(tableName = "featured_movie")
data class FeaturedMovieEntity(
    @PrimaryKey val slot: String = "current",
    val id: Int,
    val title: String,
    val rating: Double,
    val year: Int,
    val durationLabel: String,
    val genre: String,
    val description: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val cachedAt: Long,
)

@Entity(tableName = "featured_tv")
data class FeaturedTvEntity(
    @PrimaryKey val slot: String = "current",
    val id: Int,
    val title: String,
    val rating: Double,
    val year: Int,
    val durationLabel: String,
    val genre: String,
    val description: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val cachedAt: Long,
)
