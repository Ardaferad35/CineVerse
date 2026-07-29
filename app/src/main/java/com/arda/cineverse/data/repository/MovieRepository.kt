package com.arda.cineverse.data.repository

import com.arda.cineverse.data.model.Category
import com.arda.cineverse.data.model.FeaturedMovie
import com.arda.cineverse.data.model.Movie
import com.arda.cineverse.data.model.MovieDetail
import com.arda.cineverse.data.model.SearchSuggestion
import com.arda.cineverse.data.model.UpcomingMovie
import com.arda.cineverse.data.remote.TmdbApiService
import com.arda.cineverse.data.remote.TmdbNetworkModule
import com.arda.cineverse.data.remote.buildMovieDetail
import com.arda.cineverse.data.remote.toFeaturedMovie
import com.arda.cineverse.data.remote.toSearchSuggestion
import com.arda.cineverse.data.remote.toUiMovie
import com.arda.cineverse.data.remote.toUpcomingMovie
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate

class MovieRepository(
    private val api: TmdbApiService = TmdbNetworkModule.api,
) {
    suspend fun getPopularMovies(page: Int = 1): Result<List<Movie>> = runCatching {
        api.getPopularMovies(page = page).results.map { it.toUiMovie() }
    }

    suspend fun getUpcomingMovies(page: Int = 1): Result<List<UpcomingMovie>> = runCatching {
        val currentYear = LocalDate.now().year
        api.getUpcomingMovies(page = page).results
            .map { it.toUpcomingMovie() }
            .filter { movie -> movie.year != null && movie.year >= currentYear }
    }

    suspend fun searchMovies(query: String): Result<List<Movie>> = runCatching {
        api.searchMovies(query = query).results.map { it.toUiMovie() }
    }

    suspend fun searchMulti(query: String): Result<List<SearchSuggestion>> = runCatching {
        api.searchMulti(query = query).results.mapNotNull { it.toSearchSuggestion() }
    }

    suspend fun getFeaturedMovie(): Result<FeaturedMovie> = runCatching {
        val seed = todaySeed()
        val page = (seed % 5) + 1
        val pool = api.discoverMovies(page = page, minVoteAverage = 7.0, minVoteCount = 500).results
        check(pool.isNotEmpty()) { "Uygun film bulunamadı" }
        val chosen = pool[seed % pool.size]
        api.getMovieDetail(chosen.id).toFeaturedMovie()
    }

    suspend fun getMoviesByGenre(genreId: Int, page: Int = 1): Result<List<Movie>> = runCatching {
        api.discoverMovies(page = page, withGenres = genreId.toString(), sortBy = "popularity.desc")
            .results.map { it.toUiMovie() }
    }

    suspend fun getMoviesByGenreTopRated(genreId: Int, page: Int = 1): Result<List<Movie>> = runCatching {
        api.discoverMovies(page = page, withGenres = genreId.toString(), sortBy = "vote_average.desc", minVoteCount = 300)
            .results.map { it.toUiMovie() }
    }

    suspend fun getTopRatedMovies(page: Int = 1): Result<List<Movie>> = runCatching {
        api.discoverMovies(page = page, sortBy = "vote_average.desc", minVoteCount = 300)
            .results.map { it.toUiMovie() }
    }

    suspend fun getAllGenres(): Result<List<Category>> = runCatching {
        api.getMovieGenres().genres.map { genre ->
            Category(id = genre.id.toString(), label = genre.name, genreId = genre.id)
        }
    }

    /**
     * Film detayı ekranı için gereken her şeyi (detay, kadro, fragman, benzer filmler)
     * paralel olarak çekip tek bir modelde birleştirir.
     */
    suspend fun getMovieDetailFull(movieId: Int): Result<MovieDetail> = runCatching {
        coroutineScope {
            val detailDeferred = async { api.getMovieDetail(movieId) }
            val creditsDeferred = async { api.getMovieCredits(movieId) }
            val videosDeferred = async { api.getMovieVideos(movieId) }
            val similarDeferred = async { api.getSimilarMovies(movieId) }

            buildMovieDetail(
                detail = detailDeferred.await(),
                credits = creditsDeferred.await(),
                videos = videosDeferred.await(),
                similar = similarDeferred.await().results.take(10),
            )
        }
    }

    private fun todaySeed(): Int {
        val today = LocalDate.now()
        return today.year * 1000 + today.dayOfYear
    }
}