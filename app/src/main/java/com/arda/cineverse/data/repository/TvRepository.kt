package com.arda.cineverse.data.repository

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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate

class TvRepository(
    private val api: TmdbApiService = TmdbNetworkModule.api,
) {
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
            val similarDeferred = async { api.getSimilarTvShows(tvId) }

            buildTvShowDetail(
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

    private companion object {
        const val MAX_ON_THE_AIR_PAGES = 3
    }
}
