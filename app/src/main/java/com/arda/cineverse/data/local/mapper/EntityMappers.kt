package com.arda.cineverse.data.local.mapper

import com.arda.cineverse.data.local.entity.CategoryEntity
import com.arda.cineverse.data.local.entity.FeaturedMovieEntity
import com.arda.cineverse.data.local.entity.FeaturedTvEntity
import com.arda.cineverse.data.local.entity.MovieEntity
import com.arda.cineverse.data.local.entity.SavedMovieEntity
import com.arda.cineverse.data.local.entity.TvShowEntity
import com.arda.cineverse.data.model.Category
import com.arda.cineverse.data.model.FeaturedMovie
import com.arda.cineverse.data.model.FeaturedTvShow
import com.arda.cineverse.data.model.Movie
import com.arda.cineverse.data.model.SavedMovie
import com.arda.cineverse.data.model.TvShow
import com.arda.cineverse.data.model.UpcomingMovie

fun Movie.toEntity(cachedAt: Long, releaseDateLabel: String? = null): MovieEntity = MovieEntity(
    id = id,
    title = title,
    year = year,
    genre = genre,
    genres = genres,
    genreIds = genreIds,
    rating = rating,
    posterUrl = posterUrl,
    overview = overview,
    releaseDateLabel = releaseDateLabel,
    cachedAt = cachedAt,
)

fun MovieEntity.toDomain(): Movie = Movie(
    id = id,
    title = title,
    year = year,
    genre = genre,
    genres = genres,
    genreIds = genreIds,
    rating = rating,
    posterUrl = posterUrl,
    overview = overview,
)

fun UpcomingMovie.toEntity(cachedAt: Long): MovieEntity = MovieEntity(
    id = id,
    title = title,
    year = year,
    genre = "",
    genres = emptyList(),
    genreIds = emptyList(),
    rating = 0.0,
    posterUrl = posterUrl,
    overview = "",
    releaseDateLabel = releaseDateLabel,
    releaseDateStr = releaseDateStr,
    cachedAt = cachedAt,
)

fun MovieEntity.toUpcomingMovie(): UpcomingMovie = UpcomingMovie(
    id = id,
    title = title,
    releaseDateLabel = releaseDateLabel ?: "",
    releaseDateStr = releaseDateStr ?: "",
    year = year,
    posterUrl = posterUrl,
)

fun TvShow.toEntity(cachedAt: Long): TvShowEntity = TvShowEntity(
    id = id,
    name = name,
    year = year,
    genre = genre,
    genres = genres,
    genreIds = genreIds,
    rating = rating,
    posterUrl = posterUrl,
    overview = overview,
    cachedAt = cachedAt,
)

fun TvShowEntity.toDomain(): TvShow = TvShow(
    id = id,
    name = name,
    year = year,
    genre = genre,
    genres = genres,
    genreIds = genreIds,
    rating = rating,
    posterUrl = posterUrl,
    overview = overview,
)

fun Category.toEntity(isTv: Boolean, position: Int): CategoryEntity = CategoryEntity(
    id = id,
    label = label,
    genreId = genreId,
    isTv = isTv,
    position = position,
)

fun CategoryEntity.toDomain(): Category = Category(id = id, label = label, genreId = genreId)

fun FeaturedMovie.toEntity(cachedAt: Long): FeaturedMovieEntity = FeaturedMovieEntity(
    id = id,
    title = title,
    rating = rating,
    year = year,
    durationLabel = durationLabel,
    genre = genre,
    description = description,
    posterUrl = posterUrl,
    backdropUrl = backdropUrl,
    cachedAt = cachedAt,
)

fun FeaturedMovieEntity.toDomain(): FeaturedMovie = FeaturedMovie(
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

fun FeaturedTvShow.toEntity(cachedAt: Long): FeaturedTvEntity = FeaturedTvEntity(
    id = id,
    title = title,
    rating = rating,
    year = year,
    durationLabel = durationLabel,
    genre = genre,
    description = description,
    posterUrl = posterUrl,
    backdropUrl = backdropUrl,
    cachedAt = cachedAt,
)

fun FeaturedTvEntity.toDomain(): FeaturedTvShow = FeaturedTvShow(
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

/**
 * Offline detay fallback'inin üçüncü basamağı: film/dizi ne Home cache'inde
 * (movies/tv_shows) ne de favorilerde/izleme listesinde (saved_movies) ama
 * "Günün Filmi/Dizisi" ise, o slotta zaten özet dahil zengin bir kopyası var
 * — bkz. MovieRepository.getCachedMovie/TvRepository.getCachedTvShow. Çağıran
 * taraf, dönen satırın GERÇEKTEN aranan id'ye ait olduğunu (slot her zaman
 * "current" olduğu için farklı bir film/dizi olabilir) kontrol etmeli.
 */
fun FeaturedMovieEntity.toPartialMovie(): Movie = Movie(
    id = id,
    title = title,
    year = year,
    genre = genre,
    rating = rating,
    posterUrl = posterUrl,
    overview = description,
)

fun FeaturedTvEntity.toPartialTvShow(): TvShow = TvShow(
    id = id,
    name = title,
    year = year,
    genre = genre,
    rating = rating,
    posterUrl = posterUrl,
    overview = description,
)

fun SavedMovie.toEntity(listType: String): SavedMovieEntity = SavedMovieEntity(
    mediaId = id,
    mediaType = mediaType,
    listType = listType,
    title = title,
    posterUrl = posterUrl,
    rating = rating,
    year = year,
    addedAt = addedAt,
    genreIds = genreIds,
)

fun SavedMovieEntity.toDomain(): SavedMovie = SavedMovie(
    id = mediaId,
    title = title,
    posterUrl = posterUrl,
    rating = rating,
    year = year,
    addedAt = addedAt,
    genreIds = genreIds,
    mediaType = mediaType,
)

/**
 * Offline detay fallback'inin ikinci basamağı: film/dizi Home cache'inde
 * (movies/tv_shows) hiç yoksa ama kullanıcı onu favorilere/izleme listesine
 * eklediyse, o kayıttaki minimum bilgiden (özet/tür ismi olmadan) kısmi bir
 * görünüm oluşturur — bkz. MovieRepository.getCachedMovie/TvRepository.getCachedTvShow.
 */
fun SavedMovieEntity.toPartialMovie(): Movie = Movie(
    id = mediaId,
    title = title,
    year = year,
    genreIds = genreIds,
    rating = rating,
    posterUrl = posterUrl,
    mediaType = mediaType,
)

fun SavedMovieEntity.toPartialTvShow(): TvShow = TvShow(
    id = mediaId,
    name = title,
    year = year,
    genre = "",
    genreIds = genreIds,
    rating = rating,
    posterUrl = posterUrl,
)
