package com.arda.cineverse.data.model

data class TvSeasonSummary(
    val id: Int,
    val seasonNumber: Int,
    val name: String,
    val episodeCount: Int,
    val airDate: String?,
    val posterUrl: String?,
    val overview: String?,
)

data class TvSeasonDetail(
    val id: Int?,
    val seasonNumber: Int,
    val name: String?,
    val overview: String?,
    val posterUrl: String?,
    val episodes: List<TvEpisode>,
)

data class TvEpisode(
    val id: Int,
    val episodeNumber: Int,
    val seasonNumber: Int,
    val name: String,
    val overview: String?,
    val airDate: String?,
    val stillUrl: String?,
    val voteAverage: Double?,
    val runtimeMinutes: Int?,
)

data class TvEpisodeProgress(
    val tvId: Int,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val isWatched: Boolean,
    val watchedAt: Long,
)
