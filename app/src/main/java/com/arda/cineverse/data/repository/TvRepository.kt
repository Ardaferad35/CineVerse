package com.arda.cineverse.data.repository

import com.arda.cineverse.data.common.SyncResult
import com.arda.cineverse.data.connectivity.ConnectivityObserver
import com.arda.cineverse.data.local.dao.CategoryDao
import com.arda.cineverse.data.local.dao.FeaturedDao
import com.arda.cineverse.data.local.dao.TvShowDao
import com.arda.cineverse.data.local.entity.SectionType
import com.arda.cineverse.data.local.mapper.toDomain
import com.arda.cineverse.data.local.mapper.toEntity
import com.arda.cineverse.data.model.Category
import com.arda.cineverse.data.model.FeaturedTvShow
import com.arda.cineverse.data.model.TvShow
import com.arda.cineverse.data.model.TvShowDetail
import com.arda.cineverse.data.remote.TmdbApiService
import com.arda.cineverse.data.remote.TmdbNetworkModule
import com.arda.cineverse.data.remote.buildTvShowDetail
import com.arda.cineverse.data.remote.simplifyTvGenreName
import com.arda.cineverse.data.remote.toFeaturedTvShow
import com.arda.cineverse.data.remote.toUiTvShow
import com.arda.cineverse.di.AppGraph
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class TvRepository @Inject constructor(
    private val api: TmdbApiService = TmdbNetworkModule.api,
    private val tvShowDao: TvShowDao = AppGraph.tvShowDao,
    private val categoryDao: CategoryDao = AppGraph.categoryDao,
    private val featuredDao: FeaturedDao = AppGraph.featuredDao,
    private val connectivityObserver: ConnectivityObserver = AppGraph.connectivityObserver,
) {
    // --- Offline-first: sürekli gözlemlenen bölümler ---

    fun observePopularTvShows(): Flow<List<TvShow>> =
        tvShowDao.observeSection(SectionType.TV_POPULAR).map { list -> list.map { it.toDomain() } }

    fun observeOnAirTvShows(): Flow<List<TvShow>> =
        tvShowDao.observeSection(SectionType.TV_ON_AIR).map { list -> list.map { it.toDomain() } }

    fun observeFeaturedTvShow(): Flow<FeaturedTvShow?> = featuredDao.observeTv().map { it?.toDomain() }

    fun observeTvCategories(): Flow<List<Category>> = categoryDao.observeAll(isTv = true).map { list -> list.map { it.toDomain() } }

    fun observeSectionLastSyncedAt(section: SectionType): Flow<Long?> = tvShowDao.observeLastSyncedAt(section)

    suspend fun refreshPopularTvShows(page: Int = 1): SyncResult = refreshSection(SectionType.TV_POPULAR) {
        api.getPopularTvShows(page = page).results.map { it.toUiTvShow() }
    }

    suspend fun refreshOnAirTvShows(): SyncResult = refreshSection(SectionType.TV_ON_AIR) {
        getOnTheAirTvShows().getOrDefault(emptyList())
    }

    suspend fun refreshFeaturedTvShow(): SyncResult {
        if (!connectivityObserver.isCurrentlyOnline()) return SyncResult.Offline
        return getFeaturedTvShow().fold(
            onSuccess = { featured ->
                featuredDao.upsertTv(featured.toEntity(System.currentTimeMillis()))
                SyncResult.Success
            },
            onFailure = { SyncResult.Error(it) },
        )
    }

    suspend fun refreshTvCategories(): SyncResult {
        if (!connectivityObserver.isCurrentlyOnline()) return SyncResult.Offline
        return getAllTvGenres().fold(
            onSuccess = { categories ->
                categoryDao.replaceAll(isTv = true, categories.mapIndexed { index, category -> category.toEntity(isTv = true, position = index) })
                SyncResult.Success
            },
            onFailure = { SyncResult.Error(it) },
        )
    }

    private suspend fun refreshSection(section: SectionType, fetch: suspend () -> List<TvShow>): SyncResult {
        if (!connectivityObserver.isCurrentlyOnline()) return SyncResult.Offline
        return runCatching {
            val shows = fetch()
            val syncedAt = System.currentTimeMillis()
            tvShowDao.replaceSection(section, shows.map { it.toEntity(syncedAt) }, syncedAt)
        }.fold(onSuccess = { SyncResult.Success }, onFailure = { SyncResult.Error(it) })
    }

    // --- Tek seferlik / detay çağrıları — DEĞİŞMEDEN kalıyor ---

    suspend fun getPopularTvShows(page: Int = 1): Result<List<TvShow>> = runCatching {
        api.getPopularTvShows(page = page).results.map { it.toUiTvShow() }
    }

    suspend fun getTopRatedTvShows(page: Int = 1): Result<List<TvShow>> = runCatching {
        api.getTopRatedTvShows(page = page).results.map { it.toUiTvShow() }
    }

    /**
     * "Şu An Yayında": TMDB'nin tv/on_the_air uç noktası zaten yalnızca önümüzdeki
     * günlerde yeni bölüm yayınlanacak dizileri döndürüyor — bu yüzden filtre
     * GEREKMİYOR. Önceki sürüm first_air_date yılını (dizinin İLK yayın tarihi)
     * bugünün yılıyla karşılaştırıyordu; bu, film tarafındaki Yakında Vizyona
     * Girecekler filtresiyle aynı mantığı taklit etmeye çalışıyordu ama diziler
     * için yanlış sinyaldi — yıllardır devam eden ama hâlâ yayında olan bir dizi
     * (ör. 2015'te başlamış ama bu hafta yeni bölümü olan) first_air_date'i
     * bugünden eski olduğu için haksız yere eleniyor, listeyi 2-3 sonuca
     * düşürüyordu. TMDB'nin kendi "on the air" tanımına güveniyoruz.
     *
     * Bunun yerine, Popüler Diziler ile karşılaştırılabilir uzunlukta olması
     * için tek sayfa yetmezse (TMDB "on the air" havuzu bazen küçük oluyor)
     * hedefe ulaşana ya da TMDB gerçekten tükenene kadar birkaç sayfa daha
     * çekiyoruz — film tarafındaki "boş sayfa ≠ listenin sonu" düzeltmesiyle
     * aynı fikir.
     */
    suspend fun getOnTheAirTvShows(targetCount: Int = 20): Result<List<TvShow>> = runCatching {
        val results = mutableListOf<TvShow>()
        val seenIds = mutableSetOf<Int>()
        var page = 1
        while (results.size < targetCount && page <= MAX_ON_THE_AIR_PAGES) {
            val pageResults = api.getOnTheAirTvShows(page = page).results
            if (pageResults.isEmpty()) break
            pageResults.forEach { dto ->
                if (seenIds.add(dto.id)) results += dto.toUiTvShow()
            }
            page++
        }
        results
    }

    suspend fun getTvShowsByGenre(genreId: Int, page: Int = 1): Result<List<TvShow>> = runCatching {
        api.discoverTvShows(page = page, withGenres = genreId.toString(), sortBy = "popularity.desc")
            .results.map { it.toUiTvShow() }
    }

    suspend fun getAllTvGenres(): Result<List<Category>> = runCatching {
        api.getTvShowGenres().genres.map { genre ->
            Category(id = genre.id.toString(), label = genre.name.simplifyTvGenreName(), genreId = genre.id)
        }
    }

    suspend fun getFeaturedTvShow(): Result<FeaturedTvShow> = runCatching {
        val seed = todaySeed()
        val page = (seed % 5) + 1
        val pool = api.discoverTvShows(page = page, minVoteAverage = 7.0, minVoteCount = 500).results
        check(pool.isNotEmpty()) { "Uygun dizi bulunamadı" }
        val chosen = pool[seed % pool.size]
        api.getTvShowDetail(chosen.id).toFeaturedTvShow()
    }

    suspend fun getTvShowDetailFull(tvId: Int): Result<TvShowDetail> = runCatching {
        coroutineScope {
            val detailDeferred = async { api.getTvShowDetail(tvId) }
            val creditsDeferred = async { api.getTvShowCredits(tvId) }
            val videosDeferred = async { api.getTvShowVideos(tvId) }
            val rawSimilarDeferred = async { runCatching { api.getSimilarTvShows(tvId).results }.getOrDefault(emptyList()) }

            val detail = detailDeferred.await()
            val targetGenreIds = detail.genres.map { it.id }
            val genreQueryStr = targetGenreIds.take(2).joinToString(",")

            val discoverDeferred = async {
                if (genreQueryStr.isNotBlank()) {
                    runCatching {
                        api.discoverTvShows(withGenres = genreQueryStr, sortBy = "popularity.desc", minVoteCount = 100).results
                    }.getOrDefault(emptyList())
                } else emptyList()
            }

            val rawSimilar = rawSimilarDeferred.await()
            val discoverShows = discoverDeferred.await()

            val allCandidates = (rawSimilar + discoverShows)
                .distinctBy { it.id }
                .filter { it.id != tvId }

            val targetTokens = tokenizeOverview(detail.overview)

            val rankedSimilar = allCandidates
                .map { candidate ->
                    val candidateTokens = tokenizeOverview(candidate.overview)
                    val score = computeSimilarityScore(
                        targetGenreIds = targetGenreIds,
                        targetOverviewTokens = targetTokens,
                        candidateGenreIds = candidate.genre_ids,
                        candidateOverviewTokens = candidateTokens,
                        candidateVoteAverage = candidate.vote_average,
                    )
                    candidate to score
                }
                .filter { it.second > 0.0 }
                .sortedByDescending { it.second }
                .map { it.first }
                .take(10)

            buildTvShowDetail(
                detail = detail,
                credits = creditsDeferred.await(),
                videos = videosDeferred.await(),
                similar = rankedSimilar.ifEmpty { rawSimilar.take(10) },
            )
        }
    }

    private fun tokenizeOverview(text: String?): Set<String> {
        if (text.isNullOrBlank()) return emptySet()
        val stopWords = setOf("bir", "ve", "de", "da", "bu", "ile", "için", "ama", "gibi", "kendi", "daha", "en", "her", "o", "kadar", "sonra", "ki")
        return text.lowercase(java.util.Locale.forLanguageTag("tr"))
            .replace(Regex("[^a-zçğıöşü0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 2 && it !in stopWords }
            .toSet()
    }

    private fun computeSimilarityScore(
        targetGenreIds: List<Int>,
        targetOverviewTokens: Set<String>,
        candidateGenreIds: List<Int>,
        candidateOverviewTokens: Set<String>,
        candidateVoteAverage: Double,
    ): Double {
        val sharedGenres = candidateGenreIds.intersect(targetGenreIds.toSet()).size
        if (sharedGenres == 0) return 0.0
        val genreScore = sharedGenres.toDouble() / targetGenreIds.size.coerceAtLeast(1)

        val sharedTokens = candidateOverviewTokens.intersect(targetOverviewTokens).size
        val unionSize = (targetOverviewTokens.size + candidateOverviewTokens.size - sharedTokens).coerceAtLeast(1)
        val textScore = if (targetOverviewTokens.isEmpty()) 0.0 else sharedTokens.toDouble() / unionSize

        val ratingScore = (candidateVoteAverage / 10.0).coerceIn(0.0, 1.0)

        return (genreScore * 0.45) + (textScore * 0.35) + (ratingScore * 0.20)
    }

    private fun todaySeed(): Int {
        val today = LocalDate.now()
        return today.year * 1000 + today.dayOfYear
    }

    private companion object {
        const val MAX_ON_THE_AIR_PAGES = 3
    }
}
