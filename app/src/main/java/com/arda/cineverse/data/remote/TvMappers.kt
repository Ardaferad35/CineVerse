package com.arda.cineverse.data.remote

import com.arda.cineverse.data.model.CastMember
import com.arda.cineverse.data.model.FeaturedMovie
import com.arda.cineverse.data.model.FeaturedTvShow
import com.arda.cineverse.data.model.Movie
import com.arda.cineverse.data.model.TvShow
import com.arda.cineverse.data.model.TvShowDetail
import com.arda.cineverse.data.remote.dto.CreditsResponseDto
import com.arda.cineverse.data.remote.dto.TvShowDetailDto
import com.arda.cineverse.data.remote.dto.TvShowDto
import com.arda.cineverse.data.remote.dto.VideosResponseDto
import kotlin.math.round

private val tvGenreIdToTurkishName = mapOf(
    10759 to "Aksiyon & Macera",
    16 to "Animasyon",
    35 to "Komedi",
    80 to "Su\u00E7",
    99 to "Belgesel",
    18 to "Dram",
    10751 to "Aile",
    10762 to "\u00C7ocuk",
    9648 to "Gizem",
    10763 to "Haber",
    10764 to "Reality",
    10765 to "Bilim Kurgu & Fantastik",
    10766 to "Pembe Dizi",
    10767 to "Talk Show",
    10768 to "Sava\u015F & Politik",
    37 to "Vah\u015Fi Bat\u0131",
)

private fun String?.extractYear(): Int? =
    this?.takeIf { it.length >= 4 }?.substring(0, 4)?.toIntOrNull()

private val compoundTvGenreSeparators = listOf(" & ", " ve ")

/**
 * TMDB'nin "Aksiyon & Macera", "Bilim Kurgu & Fantastik" gibi bileşik dizi
 * tür adları, film türleri için tasarlanmış kısa chip UI'ında ortadan kesiliyor.
 * Gösterim amaçlı, ayraçtan önceki kısmı kullanıyoruz: "Aksiyon & Macera" -> "Aksiyon".
 */
fun String.simplifyTvGenreName(): String {
    val separator = compoundTvGenreSeparators.firstOrNull { contains(it) } ?: return this
    return substringBefore(separator).trim()
}

private fun formatSeasons(seasons: Int?): String {
    if (seasons == null || seasons <= 0) return ""
    return "$seasons sezon"
}

private fun formatEpisodes(episodes: Int?): String {
    if (episodes == null || episodes <= 0) return ""
    return "$episodes bölüm"
}

fun TvShowDto.toUiTvShow(): TvShow {
    val genreNames = genre_ids.mapNotNull { tvGenreIdToTurkishName[it]?.simplifyTvGenreName() }
    return TvShow(
        id = id,
        name = name,
        year = first_air_date.extractYear(),
        genre = genreNames.firstOrNull() ?: "Dizi",
        genres = genreNames,
        genreIds = genre_ids,
        overview = overview,
        rating = round(vote_average * 10) / 10.0,
        posterUrl = TmdbNetworkModule.posterUrl(poster_path),
    )
}

fun buildTvShowDetail(
    detail: TvShowDetailDto,
    credits: CreditsResponseDto,
    videos: VideosResponseDto,
    similar: List<TvShowDto>,
): TvShowDetail {
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

    return TvShowDetail(
        id = detail.id,
        name = detail.name,
        backdropUrl = TmdbNetworkModule.posterUrl(detail.backdrop_path, size = "w780"),
        posterUrl = TmdbNetworkModule.posterUrl(detail.poster_path),
        tmdbRating = round(detail.vote_average * 10) / 10.0,
        year = detail.first_air_date.extractYear(),
        seasonsLabel = formatSeasons(detail.number_of_seasons),
        episodesLabel = formatEpisodes(detail.number_of_episodes),
        genres = detail.genres.map { it.name.simplifyTvGenreName() },
        genreIds = detail.genres.map { it.id },
        overview = detail.overview,
        createdBy = detail.created_by.firstOrNull()?.name,
        cast = cast,
        trailerKey = trailer?.key,
        similarShows = similar.map { it.toUiTvShow() },
    )
}

fun TvShowDetailDto.toFeaturedTvShow(): FeaturedTvShow {
    val genreName = genres.firstOrNull()?.name?.simplifyTvGenreName() ?: "Dizi"
    return FeaturedTvShow(
        id = id,
        title = name.uppercase(),
        rating = round(vote_average * 10) / 10.0,
        year = first_air_date.extractYear() ?: 0,
        durationLabel = formatSeasons(number_of_seasons),
        genre = genreName,
        description = overview,
        posterUrl = TmdbNetworkModule.posterUrl(poster_path),
        backdropUrl = TmdbNetworkModule.posterUrl(backdrop_path, size = "w780"),
    )
}

/** Yalnızca gösterim amaçlı: FeaturedMovieBanner'ı Günün Dizisi için de kullanabilmek adına. */
fun FeaturedTvShow.toFeaturedMovie(): FeaturedMovie = FeaturedMovie(
    id = id,
    title = title,
    rating = rating,
    year = year,
    durationLabel = durationLabel,
    genre = genre,
    description = description,
    posterUrl = posterUrl,
    backdropUrl = backdropUrl,
)

fun TvShow.toHomeMovie(): Movie = Movie(
    id = id,
    title = name,
    year = year,
    genre = genre,
    genres = genres,
    genreIds = genreIds,
    rating = rating,
    posterUrl = posterUrl,
    overview = overview,
)
