package com.arda.cineverse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tv_shows")
data class TvShowEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val year: Int?,
    val genre: String,
    val genres: List<String>,
    val genreIds: List<Int>,
    val rating: Double,
    val posterUrl: String?,
    val overview: String,
    val cachedAt: Long,
)

@Entity(tableName = "tv_section_cross_ref", primaryKeys = ["section", "tvId"])
data class TvSectionCrossRefEntity(
    val section: SectionType,
    val tvId: Int,
    val position: Int,
    val syncedAt: Long,
)
