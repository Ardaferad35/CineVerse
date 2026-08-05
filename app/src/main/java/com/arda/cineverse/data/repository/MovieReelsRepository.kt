package com.arda.cineverse.data.repository

import com.arda.cineverse.data.model.ReelItem
import com.arda.cineverse.data.remote.TmdbApiService
import com.arda.cineverse.data.remote.TmdbNetworkModule
import com.arda.cineverse.data.remote.extractYear
import com.arda.cineverse.data.remote.genreIdToTurkishName
import com.arda.cineverse.data.remote.tvGenreIdToTurkishName
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import kotlin.math.round
import kotlin.random.Random

class MovieReelsRepository @Inject constructor(
    private val api: TmdbApiService,
) {

    companion object {
        fun default(): MovieReelsRepository = MovieReelsRepository(
            api = TmdbNetworkModule.api,
        )
    }

    /**
     * Rastgeleleştirilmiş, zenginleştirilmiş Keşif Akışı Algoritması:
     * Popüler Filmler, En Yüksek Puanlılar, Yakında Gelecekler ve Dizi kaynaklarından
     * rastgele sayfalar çekerek her açılışta ve kaydırmada kullanıcıya benzersiz,
     * tekrar etmeyen zengin içerik yelpazesi sunar.
     */
    suspend fun getMovieReels(page: Int = 1): Result<List<ReelItem>> = runCatching {
        coroutineScope {
            val randomPageOffset = (page - 1) * 2 + Random.nextInt(1, 4)
            val randomTvPageOffset = (page - 1) * 2 + Random.nextInt(1, 4)

            val popMoviesDeferred = async { runCatching { api.getPopularMovies(page = randomPageOffset).results }.getOrDefault(emptyList()) }
            val topMoviesDeferred = async { runCatching { api.getTopRatedMovies(page = randomPageOffset).results }.getOrDefault(emptyList()) }
            val popTvDeferred = async { runCatching { api.getPopularTvShows(page = randomTvPageOffset).results }.getOrDefault(emptyList()) }
            val topTvDeferred = async { runCatching { api.getTopRatedTvShows(page = randomTvPageOffset).results }.getOrDefault(emptyList()) }

            val rawMovies = (popMoviesDeferred.await() + topMoviesDeferred.await()).distinctBy { it.id }.shuffled().take(15)
            val rawTvShows = (popTvDeferred.await() + topTvDeferred.await()).distinctBy { it.id }.shuffled().take(15)

            val movieReelsDeferred = rawMovies.map { dto ->
                async {
                    val videos = runCatching { api.getMovieVideos(dto.id, language = null).results }.getOrDefault(emptyList())

                    val rawKey = videos.firstOrNull {
                        it.site.equals("YouTube", ignoreCase = true) && it.type.equals("Trailer", ignoreCase = true) && !it.key.isNullOrBlank()
                    }?.key ?: videos.firstOrNull { it.site.equals("YouTube", ignoreCase = true) && !it.key.isNullOrBlank() }?.key

                    val cleanKey = extractCleanYouTubeId(rawKey) ?: ""

                    ReelItem(
                        id = dto.id,
                        title = dto.title,
                        overview = dto.overview,
                        posterUrl = TmdbNetworkModule.posterUrl(dto.poster_path),
                        backdropUrl = TmdbNetworkModule.posterUrl(dto.backdrop_path, size = "w780"),
                        rating = round(dto.vote_average * 10) / 10.0,
                        year = dto.release_date.extractYear(),
                        genres = dto.genre_ids.mapNotNull { genreIdToTurkishName[it] },
                        genreIds = dto.genre_ids,
                        trailerKey = cleanKey,
                        mediaType = "movie",
                    )
                }
            }

            val tvReelsDeferred = rawTvShows.map { dto ->
                async {
                    val videos = runCatching { api.getTvShowVideos(dto.id, language = null).results }.getOrDefault(emptyList())

                    val rawKey = videos.firstOrNull {
                        it.site.equals("YouTube", ignoreCase = true) && it.type.equals("Trailer", ignoreCase = true) && !it.key.isNullOrBlank()
                    }?.key ?: videos.firstOrNull { it.site.equals("YouTube", ignoreCase = true) && !it.key.isNullOrBlank() }?.key

                    val cleanKey = extractCleanYouTubeId(rawKey) ?: ""

                    ReelItem(
                        id = dto.id,
                        title = dto.name,
                        overview = dto.overview,
                        posterUrl = TmdbNetworkModule.posterUrl(dto.poster_path),
                        backdropUrl = TmdbNetworkModule.posterUrl(dto.backdrop_path, size = "w780"),
                        rating = round(dto.vote_average * 10) / 10.0,
                        year = dto.first_air_date.extractYear(),
                        genres = dto.genre_ids.mapNotNull { tvGenreIdToTurkishName[it] },
                        genreIds = dto.genre_ids,
                        trailerKey = cleanKey,
                        mediaType = "tv",
                    )
                }
            }

            val allReels = (movieReelsDeferred.awaitAll() + tvReelsDeferred.awaitAll())
                .filter { it.overview.isNotBlank() }
                .distinctBy { "${it.mediaType}_${it.id}" }
                .shuffled()

            allReels
        }
    }

    private fun extractCleanYouTubeId(rawKey: String?): String? {
        if (rawKey.isNullOrBlank()) return null
        var cleaned = rawKey.trim()
        if (cleaned.contains("v=")) {
            cleaned = cleaned.substringAfter("v=").substringBefore("&")
        } else if (cleaned.contains("youtu.be/")) {
            cleaned = cleaned.substringAfter("youtu.be/").substringBefore("?")
        } else if (cleaned.contains("embed/")) {
            cleaned = cleaned.substringAfter("embed/").substringBefore("?")
        }
        val id = cleaned.takeWhile { it.isLetterOrDigit() || it == '_' || it == '-' }
        return id.takeIf { it.length >= 8 }
    }
}
