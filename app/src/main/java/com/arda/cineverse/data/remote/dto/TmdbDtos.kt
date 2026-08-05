package com.arda.cineverse.data.remote.dto

data class MoviesResponseDto(
    val page: Int,
    val results: List<MovieDto>,
    val total_pages: Int,
    val total_results: Int,
)

data class TvShowsResponseDto(
    val page: Int,
    val results: List<TvShowDto>,
    val total_pages: Int,
    val total_results: Int,
)

data class MovieDto(
    val id: Int,
    val title: String,
    val overview: String,
    val poster_path: String?,
    val backdrop_path: String?,
    val release_date: String?,
    val vote_average: Double,
    val genre_ids: List<Int> = emptyList(),
)

data class TvShowDto(
    val id: Int,
    val name: String,
    val overview: String,
    val poster_path: String?,
    val backdrop_path: String?,
    val first_air_date: String?,
    val vote_average: Double,
    val genre_ids: List<Int> = emptyList(),
)

data class MovieCollectionDto(
    val id: Int,
    val name: String,
    val poster_path: String? = null,
    val backdrop_path: String? = null,
)

data class MovieCollectionResponseDto(
    val id: Int,
    val name: String,
    val parts: List<MovieDto> = emptyList(),
)

data class MovieDetailDto(
    val id: Int,
    val title: String,
    val overview: String,
    val poster_path: String?,
    val backdrop_path: String?,
    val release_date: String?,
    val vote_average: Double,
    val runtime: Int?,
    val genres: List<GenreDto> = emptyList(),
    val belongs_to_collection: MovieCollectionDto? = null,
)

data class TvShowDetailDto(
    val id: Int,
    val name: String,
    val overview: String,
    val poster_path: String?,
    val backdrop_path: String?,
    val first_air_date: String?,
    val vote_average: Double,
    val episode_run_time: List<Int> = emptyList(),
    val number_of_seasons: Int? = null,
    val number_of_episodes: Int? = null,
    val created_by: List<CreatedByDto> = emptyList(),
    val genres: List<GenreDto> = emptyList(),
    val seasons: List<TvSeasonSummaryDto> = emptyList(),
)

data class TvSeasonSummaryDto(
    val id: Int,
    val season_number: Int,
    val name: String? = null,
    val episode_count: Int = 0,
    val air_date: String? = null,
    val poster_path: String? = null,
    val overview: String? = null,
)

data class TvSeasonDetailDto(
    val id: Int? = null,
    val season_number: Int,
    val name: String? = null,
    val overview: String? = null,
    val poster_path: String? = null,
    val episodes: List<TvEpisodeDto> = emptyList(),
)

data class TvEpisodeDto(
    val id: Int,
    val episode_number: Int,
    val season_number: Int,
    val name: String,
    val overview: String? = null,
    val air_date: String? = null,
    val still_path: String? = null,
    val vote_average: Double? = null,
    val runtime: Int? = null,
)

data class CreatedByDto(
    val id: Int,
    val name: String,
    val profile_path: String? = null,
)

data class GenreDto(
    val id: Int,
    val name: String,
)

data class GenreListResponseDto(
    val genres: List<GenreDto> = emptyList(),
)


data class CreditsResponseDto(
    val cast: List<CastMemberDto>,
    val crew: List<CrewMemberDto> = emptyList(),
)

data class CastMemberDto(
    val id: Int,
    val name: String,
    val character: String,
    val profile_path: String?,
)

data class CrewMemberDto(
    val id: Int,
    val name: String,
    val job: String,
    val department: String,
)

data class VideosResponseDto(
    val results: List<VideoDto>,
)

data class VideoDto(
    val id: String,
    val key: String,
    val site: String,
    val type: String,
    val official: Boolean = false,
    val name: String = "",
)

data class MultiSearchResponseDto(
    val results: List<MultiSearchResultDto> = emptyList(),
)

data class MultiSearchResultDto(
    val id: Int,
    val media_type: String? = null,
    val title: String? = null,
    val name: String? = null,
    val poster_path: String? = null,
    val profile_path: String? = null,
    val release_date: String? = null,
    val first_air_date: String? = null,
    val vote_average: Double? = null,
    val genre_ids: List<Int> = emptyList(),
)
