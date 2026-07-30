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
import java.io.IOException

private data class GuessedItem(
    val title: String,
    val year: Int? = null,
    val type: String? = null,
)

class AiSearchRepository(
    private val geminiApi: GeminiApiService = GeminiNetworkModule.api,
    private val tmdbApi: TmdbApiService = TmdbNetworkModule.api,
) {
    private val gson = Gson()

    /**
     * Kullanıcının doğal dille yaptığı film veya dizi tarifini Gemini'ye gönderir,
     * dönen 3 tahmini TMDB'de arayıp gerçek film/dizi verisiyle eşleştirir.
     */
    suspend fun searchWithDescription(description: String): Result<List<Movie>> = runCatching {
        val request = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = buildPrompt(description))))),
            generationConfig = GeminiGenerationConfig(),
        )

        val response = generateContentWithRetry(request)
        val rawJson = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: error("Yapay zeka bir yanıt üretemedi")

        val type = object : TypeToken<List<GuessedItem>>() {}.type
        val guesses: List<GuessedItem> = gson.fromJson(rawJson, type)

        guesses.mapNotNull { guess ->
            val searchResults = tmdbApi.searchMulti(query = guess.title).results.mapNotNull { it.toUiMovie() }
            if (guess.type == "tv") {
                searchResults.firstOrNull { it.mediaType == "tv" } ?: searchResults.firstOrNull()
            } else if (guess.type == "movie") {
                searchResults.firstOrNull { it.mediaType == "movie" } ?: searchResults.firstOrNull()
            } else {
                searchResults.firstOrNull()
            }
        }.distinctBy { "${it.mediaType}_${it.id}" }.take(3)
    }

    /**
     * Gemini bazen "503 model şu an yoğun" hatası ya da geçici bir ağ zaman aşımı
     * (SocketTimeoutException) verebiliyor — ikisi de geçici durumlar. Her ikisinde
     * de artan bekleme süreleriyle en fazla 3 kez deniyoruz. Başka bir HTTP hatası
     * (400, 403 vb.) gelirse hemen fırlatıyoruz, tekrar denemenin faydası yok.
     */
    private suspend fun generateContentWithRetry(
        request: GeminiRequest,
        maxAttempts: Int = 3,
    ): GeminiResponse {
        var lastError: Exception? = null
        repeat(maxAttempts) { attempt ->
            val isLastAttempt = attempt == maxAttempts - 1
            try {
                return geminiApi.generateContent(request)
            } catch (e: HttpException) {
                lastError = e
                if (e.code() != 503 || isLastAttempt) throw e
                delay(1500L * (attempt + 1))
            } catch (e: IOException) {
                // Zaman aşımı, bağlantı kopması vb. — bunlar da geçici olabilir, tekrar deneriz
                lastError = e
                if (isLastAttempt) throw e
                delay(1000L * (attempt + 1))
            }
        }
        throw lastError ?: IllegalStateException("Gemini isteği başarısız oldu")
    }

    private fun buildPrompt(description: String): String = """
        Kullanıcı bir film veya diziyi doğal dille tarif ediyor. Bu tarife göre en olası 3 film veya dizi tahminini ver.
        SADECE aşağıdaki JSON formatında yanıt ver, başka hiçbir açıklama veya metin ekleme:
        [{"title": "Başlık 1", "year": 2014, "type": "movie"}, {"title": "Başlık 2", "year": 2010, "type": "tv"}, {"title": "Başlık 3", "year": null, "type": "movie"}]

        Kullanıcının tarifi: "$description"
    """.trimIndent()
}