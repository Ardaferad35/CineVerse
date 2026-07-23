package com.arda.cineverse.data.remote

import com.arda.cineverse.data.model.CastMember
import com.arda.cineverse.data.model.FeaturedMovie
import com.arda.cineverse.data.model.Movie
import com.arda.cineverse.data.model.MovieDetail
import com.arda.cineverse.data.model.UpcomingMovie
import com.arda.cineverse.data.remote.dto.CreditsResponseDto
import com.arda.cineverse.data.remote.dto.MovieDetailDto
import com.arda.cineverse.data.remote.dto.MovieDto
import com.arda.cineverse.data.remote.dto.VideosResponseDto
import kotlin.math.round

private val genreIdToTurkishName = mapOf(
    28 to "Aksiyon",
    12 to "Macera",
    16 to "Animasyon",
    35 to "Komedi",
    80 to "Suç",
    99 to "Belgesel",
    18 to "Dram",
    10751 to "Aile",
    14 to "Fantastik",
    36 to "Tarih",
    27 to "Korku",
    10402 to "Müzik",
    9648 to "Gizem",
    10749 to "Romantik",
    878 to "Bilim Kurgu",
    10770 to "TV Filmi",
    53 to "Gerilim",
    10752 to "Savaş",
    37 to "Vahşi Batı",
)

private val turkishMonths = listOf(
    "OCA", "ŞUB", "MAR", "NİS", "MAY", "HAZ", "TEM", "AĞU", "EYL", "EKİ", "KAS", "ARA"
)

private fun String?.extractYear(): Int? =
    this?.takeIf { it.length >= 4 }?.substring(0, 4)?.toIntOrNull()

private fun formatRuntime(minutes: Int?): String {
    if (minutes == null || minutes <= 0) return ""
    val h = minutes / 60
    val m = minutes % 60
    return "${h}sa ${m}dk"
}

fun MovieDto.toUiMovie(): Movie {
    val genreName = genre_ids.firstOrNull()?.let { genreIdToTurkishName[it] } ?: "Film"
    return Movie(
        id = id,
        title = title,
        year = release_date.extractYear(),
        genre = genreName,
        rating = round(vote_average * 10) / 10.0,
        posterUrl = TmdbNetworkModule.posterUrl(poster_path),
    )
}

fun MovieDto.toUpcomingMovie(): UpcomingMovie {
    val label = release_date?.takeIf { it.length >= 10 }?.let { date ->
        val month = date.substring(5, 7).toIntOrNull()
        val day = date.substring(8, 10)
        if (month != null && month in 1..12) "$day ${turkishMonths[month - 1]}" else ""
    } ?: ""
    return UpcomingMovie(
        id = id,
        title = title,
        releaseDateLabel = label,
        year = release_date.extractYear(),
        posterUrl = TmdbNetworkModule.posterUrl(poster_path),
    )
}

fun MovieDetailDto.toFeaturedMovie(): FeaturedMovie {
    val genreName = genres.firstOrNull()?.name ?: "Film"
    return FeaturedMovie(
        id = id,
        title = title.uppercase(),
        rating = round(vote_average * 10) / 10.0,
        year = release_date.extractYear() ?: 0,
        durationLabel = formatRuntime(runtime),
        genre = genreName,
        description = overview,
        posterUrl = TmdbNetworkModule.posterUrl(poster_path),
        backdropUrl = TmdbNetworkModule.posterUrl(backdrop_path, size = "w780"),
    )
}

/**
 * Film detayı + oyuncu kadrosu + fragmanlar + benzer filmler verisini
 * tek bir ekran modelinde birleştirir.
 */
fun buildMovieDetail(
    detail: MovieDetailDto,
    credits: CreditsResponseDto,
    videos: VideosResponseDto,
    similar: List<MovieDto>,
): MovieDetail {
    val director = credits.crew.firstOrNull { it.job == "Director" }?.name

    val cast = credits.cast.take(10).map {
        CastMember(
            id = it.id,
            name = it.name,
            character = it.character,
            profileUrl = TmdbNetworkModule.posterUrl(it.profile_path, size = "w185"),
        )
    }

    val trailer = videos.results.firstOrNull {
        it.site.equals("YouTube", ignoreCase = true) && it.type.equals("Trailer", ignoreCase = true) && it.official
    } ?: videos.results.firstOrNull {
        it.site.equals("YouTube", ignoreCase = true) && it.type.equals("Trailer", ignoreCase = true)
    } ?: videos.results.firstOrNull {
        it.site.equals("YouTube", ignoreCase = true)
    }

    return MovieDetail(
        id = detail.id,
        title = detail.title,
        backdropUrl = TmdbNetworkModule.posterUrl(detail.backdrop_path, size = "w780"),
        posterUrl = TmdbNetworkModule.posterUrl(detail.poster_path),
        tmdbRating = round(detail.vote_average * 10) / 10.0,
        year = detail.release_date.extractYear(),
        durationLabel = formatRuntime(detail.runtime),
        genres = detail.genres.map { it.name },
        overview = detail.overview,
        director = director,
        cast = cast,
        trailerKey = trailer?.key,
        similarMovies = similar.map { it.toUiMovie() },
    )
}