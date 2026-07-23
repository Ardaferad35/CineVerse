package com.arda.cineverse.data.remote

import com.arda.cineverse.data.remote.dto.CreditsResponseDto
import com.arda.cineverse.data.remote.dto.GenreListResponseDto
import com.arda.cineverse.data.remote.dto.MovieDetailDto
import com.arda.cineverse.data.remote.dto.MoviesResponseDto
import com.arda.cineverse.data.remote.dto.MultiSearchResponseDto
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
}