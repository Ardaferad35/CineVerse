package com.arda.cineverse.data.remote

import com.arda.cineverse.data.remote.dto.CreditsResponseDto
import com.arda.cineverse.data.remote.dto.GenreListResponseDto
import com.arda.cineverse.data.remote.dto.MovieDetailDto
import com.arda.cineverse.data.remote.dto.MoviesResponseDto
import com.arda.cineverse.data.remote.dto.MultiSearchResponseDto
import com.arda.cineverse.data.remote.dto.TvShowDetailDto
import com.arda.cineverse.data.remote.dto.TvShowsResponseDto
import com.arda.cineverse.data.remote.dto.VideosResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApiService {

    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("language") language: String = "tr-TR",
        @Query("page") page: Int = 1,
    ): MoviesResponseDto

    @GET("movie/upcoming")
    suspend fun getUpcomingMovies(
        @Query("language") language: String = "tr-TR",
        @Query("page") page: Int = 1,
    ): MoviesResponseDto

    @GET("movie/top_rated")
    suspend fun getTopRatedMovies(
        @Query("language") language: String = "tr-TR",
        @Query("page") page: Int = 1,
    ): MoviesResponseDto

    @GET("movie/{movie_id}")
    suspend fun getMovieDetail(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = "tr-TR",
    ): MovieDetailDto

    @GET("movie/{movie_id}/credits")
    suspend fun getMovieCredits(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = "tr-TR",
    ): CreditsResponseDto

    @GET("movie/{movie_id}/videos")
    suspend fun getMovieVideos(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = "tr-TR",
    ): VideosResponseDto

    @GET("movie/{movie_id}/similar")
    suspend fun getSimilarMovies(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = "tr-TR",
        @Query("page") page: Int = 1,
    ): MoviesResponseDto

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("language") language: String = "tr-TR",
        @Query("page") page: Int = 1,
    ): MoviesResponseDto

    @GET("search/multi")
    suspend fun searchMulti(
        @Query("query") query: String,
        @Query("language") language: String = "tr-TR",
        @Query("page") page: Int = 1,
    ): MultiSearchResponseDto

    @GET("discover/movie")
    suspend fun discoverMovies(
        @Query("language") language: String = "tr-TR",
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("vote_average.gte") minVoteAverage: Double? = null,
        @Query("vote_count.gte") minVoteCount: Int? = null,
        @Query("with_genres") withGenres: String? = null,
        @Query("page") page: Int = 1,
    ): MoviesResponseDto

    @GET("genre/movie/list")
    suspend fun getMovieGenres(
        @Query("language") language: String = "tr-TR",
    ): GenreListResponseDto

    @GET("tv/popular")
    suspend fun getPopularTvShows(
        @Query("language") language: String = "tr-TR",
        @Query("page") page: Int = 1,
    ): TvShowsResponseDto

    @GET("tv/top_rated")
    suspend fun getTopRatedTvShows(
        @Query("language") language: String = "tr-TR",
        @Query("page") page: Int = 1,
    ): TvShowsResponseDto

    @GET("tv/on_the_air")
    suspend fun getOnTheAirTvShows(
        @Query("language") language: String = "tr-TR",
        @Query("page") page: Int = 1,
    ): TvShowsResponseDto

    @GET("tv/{tv_id}")
    suspend fun getTvShowDetail(
        @Path("tv_id") tvId: Int,
        @Query("language") language: String = "tr-TR",
    ): TvShowDetailDto

    @GET("tv/{tv_id}/credits")
    suspend fun getTvShowCredits(
        @Path("tv_id") tvId: Int,
        @Query("language") language: String = "tr-TR",
    ): CreditsResponseDto

    @GET("tv/{tv_id}/videos")
    suspend fun getTvShowVideos(
        @Path("tv_id") tvId: Int,
        @Query("language") language: String = "tr-TR",
    ): VideosResponseDto

    @GET("tv/{tv_id}/similar")
    suspend fun getSimilarTvShows(
        @Path("tv_id") tvId: Int,
        @Query("language") language: String = "tr-TR",
        @Query("page") page: Int = 1,
    ): TvShowsResponseDto

    @GET("genre/tv/list")
    suspend fun getTvShowGenres(
        @Query("language") language: String = "tr-TR",
    ): GenreListResponseDto

    @GET("discover/tv")
    suspend fun discoverTvShows(
        @Query("language") language: String = "tr-TR",
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("vote_average.gte") minVoteAverage: Double? = null,
        @Query("vote_count.gte") minVoteCount: Int? = null,
        @Query("with_genres") withGenres: String? = null,
        @Query("page") page: Int = 1,
    ): TvShowsResponseDto
}
