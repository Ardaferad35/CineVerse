package com.arda.cineverse.data.repository

import com.arda.cineverse.data.common.SyncResult
import com.arda.cineverse.data.connectivity.ConnectivityObserver
import com.arda.cineverse.data.local.dao.CategoryDao
import com.arda.cineverse.data.local.dao.FeaturedDao
import com.arda.cineverse.data.local.dao.MovieDao
import com.arda.cineverse.data.local.entity.SectionType
import com.arda.cineverse.data.local.mapper.toDomain
import com.arda.cineverse.data.local.mapper.toEntity
import com.arda.cineverse.data.local.mapper.toUpcomingMovie
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
import com.arda.cineverse.di.AppGraph
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class MovieRepository @Inject constructor(
    private val api: TmdbApiService = TmdbNetworkModule.api,
    private val movieDao: MovieDao = AppGraph.movieDao,
    private val categoryDao: CategoryDao = AppGraph.categoryDao,
    private val featuredDao: FeaturedDao = AppGraph.featuredDao,
    private val connectivityObserver: ConnectivityObserver = AppGraph.connectivityObserver,
) {
    // ---------------------------------------------------------------
    // Offline-first: Home'un sürekli gözlemlenen bölümleri. UI her zaman
    // Room'daki (belki bayat) veriyi anında görür; refresh*() online iken
    // TMDB'den taze veri çekip Room'u günceller, bu da Flow'u re-emit ettirir.
    // ---------------------------------------------------------------

    fun observePopularMovies(): Flow<List<Movie>> =
        movieDao.observeSection(SectionType.POPULAR).map { list -> list.map { it.toDomain() } }

    fun observeTopRatedMovies(): Flow<List<Movie>> =
        movieDao.observeSection(SectionType.TOP_RATED).map { list -> list.map { it.toDomain() } }

    fun observeUpcomingMovies(): Flow<List<UpcomingMovie>> =
        movieDao.observeSection(SectionType.UPCOMING).map { list -> list.map { it.toUpcomingMovie() } }

    fun observeFeaturedMovie(): Flow<FeaturedMovie?> = featuredDao.observeMovie().map { it?.toDomain() }

    fun observeCategories(): Flow<List<Category>> = categoryDao.observeAll(isTv = false).map { list -> list.map { it.toDomain() } }

    fun observeSectionLastSyncedAt(section: SectionType): Flow<Long?> = movieDao.observeLastSyncedAt(section)

    /**
     * Offline'ken film detayı açılırken kullanılır: TMDB'den tam detay
     * (kadro/fragman/benzer filmler) çekilemezse, Home'da daha önce
     * cache'lenmiş temel film bilgisiyle (başlık, poster, puan, yıl, tür,
     * özet) kısmi bir görünüm oluşturmaya yarar.
     */
    suspend fun getCachedMovie(movieId: Int): Movie? = movieDao.getMovieById(movieId)?.toDomain()

    suspend fun refreshPopularMovies(page: Int = 1): SyncResult = refreshSection(SectionType.POPULAR) {
        api.getPopularMovies(page = page).results.map { it.toUiMovie() }
    }

    suspend fun refreshTopRatedMovies(page: Int = 1): SyncResult = refreshSection(SectionType.TOP_RATED) {
        api.getTopRatedMovies(page = page).results.map { it.toUiMovie() }
    }

    suspend fun refreshUpcomingMovies(page: Int = 1): SyncResult {
        if (!connectivityObserver.isCurrentlyOnline()) return SyncResult.Offline
        return runCatching {
            val currentYear = LocalDate.now().year
            val upcoming = api.getUpcomingMovies(page = page).results
                .map { it.toUpcomingMovie() }
                .filter { movie -> movie.year != null && movie.year >= currentYear }
            val syncedAt = System.currentTimeMillis()
            movieDao.replaceSection(SectionType.UPCOMING, upcoming.map { it.toEntity(syncedAt) }, syncedAt)
        }.fold(onSuccess = { SyncResult.Success }, onFailure = { SyncResult.Error(it) })
    }

    suspend fun refreshFeaturedMovie(): SyncResult {
        if (!connectivityObserver.isCurrentlyOnline()) return SyncResult.Offline
        return getFeaturedMovie().fold(
            onSuccess = { featured ->
                featuredDao.upsertMovie(featured.toEntity(System.currentTimeMillis()))
                SyncResult.Success
            },
            onFailure = { SyncResult.Error(it) },
        )
    }

    suspend fun refreshCategories(): SyncResult {
        if (!connectivityObserver.isCurrentlyOnline()) return SyncResult.Offline
        return getAllGenres().fold(
            onSuccess = { categories ->
                categoryDao.replaceAll(isTv = false, categories.mapIndexed { index, category -> category.toEntity(isTv = false, position = index) })
                SyncResult.Success
            },
            onFailure = { SyncResult.Error(it) },
        )
    }

    private suspend fun refreshSection(section: SectionType, fetch: suspend () -> List<Movie>): SyncResult {
        if (!connectivityObserver.isCurrentlyOnline()) return SyncResult.Offline
        return runCatching {
            val movies = fetch()
            val syncedAt = System.currentTimeMillis()
            movieDao.replaceSection(section, movies.map { it.toEntity(syncedAt) }, syncedAt)
        }.fold(onSuccess = { SyncResult.Success }, onFailure = { SyncResult.Error(it) })
    }

    // ---------------------------------------------------------------
    // Tek seferlik / detay çağrıları — DEĞİŞMEDEN kalıyor (Result<T> tabanlı,
    // cache gerektirmiyor: arama, kategori bazlı listeler, film detayı).
    // ---------------------------------------------------------------

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

    suspend fun searchMoviesAndTv(query: String): Result<List<Movie>> = runCatching {
        api.searchMulti(query = query).results.mapNotNull { it.toUiMovie() }
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

    /** TMDB'nin gerçek "movie/top_rated" uç noktası — önceden discover ile taklit ediliyordu. */
    suspend fun getTopRatedMovies(page: Int = 1): Result<List<Movie>> = runCatching {
        api.getTopRatedMovies(page = page).results.map { it.toUiMovie() }
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
            val rawSimilarDeferred = async { runCatching { api.getSimilarMovies(movieId).results }.getOrDefault(emptyList()) }

            val detail = detailDeferred.await()
            val targetGenreIds = detail.genres.map { it.id }
            val genreQueryStr = targetGenreIds.take(2).joinToString(",")

            val collectionId = detail.belongs_to_collection?.id
            val collectionDeferred = async {
                if (collectionId != null) {
                    runCatching { api.getCollectionDetail(collectionId).parts }.getOrDefault(emptyList())
                } else emptyList()
            }

            val discoverDeferred = async {
                if (genreQueryStr.isNotBlank()) {
                    runCatching {
                        api.discoverMovies(withGenres = genreQueryStr, sortBy = "popularity.desc", minVoteCount = 100).results
                    }.getOrDefault(emptyList())
                } else emptyList()
            }

            val collectionMovies = collectionDeferred.await()
            val rawSimilar = rawSimilarDeferred.await()
            val discoverMovies = discoverDeferred.await()

            val collectionMovieIds = collectionMovies.map { it.id }.toSet()

            val allCandidates = (collectionMovies + rawSimilar + discoverMovies)
                .distinctBy { it.id }
                .filter { it.id != movieId }

            val targetTokens = tokenizeOverview(detail.overview)

            val rankedSimilar = allCandidates
                .map { candidate ->
                    val candidateTokens = tokenizeOverview(candidate.overview)
                    val isCollectionMember = candidate.id in collectionMovieIds
                    val score = computeAdvancedSimilarityScore(
                        targetGenreIds = targetGenreIds,
                        targetOverviewTokens = targetTokens,
                        candidate = candidate,
                        candidateOverviewTokens = candidateTokens,
                        isCollectionMember = isCollectionMember,
                    )
                    candidate to score
                }
                .filter { it.second > 0.0 }
                .sortedByDescending { it.second }
                .map { it.first }
                .take(10)

            buildMovieDetail(
                detail = detail,
                credits = creditsDeferred.await(),
                videos = videosDeferred.await(),
                similar = rankedSimilar.ifEmpty { rawSimilar.take(10) },
            )
        }
    }

    private fun tokenizeOverview(text: String?): Set<String> {
        if (text.isNullOrBlank()) return emptySet()
        val stopWords = setOf("bir", "ve", "de", "da", "bu", "ile", "için", "ama", "gibi", "kendi", "daha", "en", "her", "o", "kadar", "sonra", "ki", "tarafından")
        return text.lowercase(java.util.Locale.forLanguageTag("tr"))
            .replace(Regex("[^a-zçğıöşü0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 2 && it !in stopWords }
            .toSet()
    }

    private fun computeAdvancedSimilarityScore(
        targetGenreIds: List<Int>,
        targetOverviewTokens: Set<String>,
        candidate: com.arda.cineverse.data.remote.dto.MovieDto,
        candidateOverviewTokens: Set<String>,
        isCollectionMember: Boolean,
    ): Double {
        var score = 0.0

        if (isCollectionMember) {
            score += 100.0
        }

        val sharedGenres = candidate.genre_ids.intersect(targetGenreIds.toSet()).size
        if (sharedGenres == 0 && !isCollectionMember) return 0.0

        val genreScore = sharedGenres.toDouble() / targetGenreIds.size.coerceAtLeast(1)
        score += genreScore * 40.0

        val sharedTokens = candidateOverviewTokens.intersect(targetOverviewTokens).size
        if (targetOverviewTokens.isNotEmpty()) {
            val unionSize = (targetOverviewTokens.size + candidateOverviewTokens.size - sharedTokens).coerceAtLeast(1)
            val textJaccard = sharedTokens.toDouble() / unionSize
            score += textJaccard * 30.0
        }

        val ratingRatio = (candidate.vote_average / 10.0).coerceIn(0.0, 1.0)
        score += ratingRatio * 20.0

        return score
    }

    private fun todaySeed(): Int {
        val today = LocalDate.now()
        return today.year * 1000 + today.dayOfYear
    }
}
