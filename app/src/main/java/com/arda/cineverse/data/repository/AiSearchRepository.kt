package com.arda.cineverse.data.repository

import com.arda.cineverse.data.model.Movie
import com.arda.cineverse.data.remote.GeminiApiService
import com.arda.cineverse.data.remote.GeminiNetworkModule
import com.arda.cineverse.data.remote.TmdbApiService
import com.arda.cineverse.data.remote.TmdbNetworkModule
import com.arda.cineverse.data.remote.dto.GeminiContent
import com.arda.cineverse.data.remote.dto.GeminiGenerationConfig
import com.arda.cineverse.data.remote.dto.GeminiPart
import com.arda.cineverse.data.remote.dto.GeminiRequest
import com.arda.cineverse.data.remote.dto.GeminiResponse
import com.arda.cineverse.data.remote.toUiMovie
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import retrofit2.HttpException

private data class GuessedMovie(val title: String, val year: Int? = null)

class AiSearchRepository(
    private val geminiApi: GeminiApiService = GeminiNetworkModule.api,
    private val tmdbApi: TmdbApiService = TmdbNetworkModule.api,
) {
    private val gson = Gson()

    /**
     * Kullanıcının doğal dille yaptığı film tarifini Gemini'ye gönderir,
     * dönen 3 tahmini TMDB'de arayıp gerçek film verisiyle eşleştirir.
     */
    suspend fun searchWithDescription(description: String): Result<List<Movie>> = runCatching {
        val request = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = buildPrompt(description))))),
            generationConfig = GeminiGenerationConfig(),
        )

        val response = generateContentWithRetry(request)
        val rawJson = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: error("Yapay zeka bir yanıt üretemedi")

        val type = object : TypeToken<List<GuessedMovie>>() {}.type
        val guesses: List<GuessedMovie> = gson.fromJson(rawJson, type)

        guesses.mapNotNull { guess ->
            tmdbApi.searchMovies(query = guess.title).results.firstOrNull()?.toUiMovie()
        }.take(3)
    }

    /**
     * Gemini bazen "503 model şu an yoğun" hatası döndürüyor — bu geçici bir durum.
     * Sadece 503 hatasında, artan bekleme süreleriyle en fazla 3 kez deniyoruz.
     * Başka bir hata (400, 403 vb.) gelirse hemen fırlatıyoruz, tekrar denemenin faydası yok.
     */
    private suspend fun generateContentWithRetry(
        request: GeminiRequest,
        maxAttempts: Int = 3,
    ): GeminiResponse {
        var lastError: Exception? = null
        repeat(maxAttempts) { attempt ->
            try {
                return geminiApi.generateContent(request)
            } catch (e: HttpException) {
                lastError = e
                val isLastAttempt = attempt == maxAttempts - 1
                if (e.code() != 503 || isLastAttempt) throw e
                delay(1500L * (attempt + 1))
            }
        }
        throw lastError ?: IllegalStateException("Gemini isteği başarısız oldu")
    }

    private fun buildPrompt(description: String): String = """
        Kullanıcı bir filmi doğal dille tarif ediyor. Bu tarife göre en olası 3 film tahminini ver.
        SADECE aşağıdaki JSON formatında yanıt ver, başka hiçbir açıklama veya metin ekleme:
        [{"title": "Film Adı", "year": 2014}, {"title": "Film Adı 2", "year": 2010}, {"title": "Film Adı 3", "year": null}]

        Kullanıcının tarifi: "$description"
    """.trimIndent()
}