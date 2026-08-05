package com.arda.cineverse.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "tv_episode_progress",
    primaryKeys = ["tvId", "seasonNumber", "episodeNumber"],
)
data class TvEpisodeProgressEntity(
    val tvId: Int,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val isWatched: Boolean,
    val watchedAt: Long = System.currentTimeMillis(),
)
