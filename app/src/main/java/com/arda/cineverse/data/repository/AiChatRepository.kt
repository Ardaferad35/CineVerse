package com.arda.cineverse.data.repository

import com.arda.cineverse.data.model.AiChatMessage
import com.arda.cineverse.data.model.AiIntent
import com.arda.cineverse.data.model.AiMovieSuggestion
import com.arda.cineverse.data.model.AiReply
import com.arda.cineverse.data.model.ChatMessage
import com.arda.cineverse.data.model.Movie
import com.arda.cineverse.data.model.UserChatMessage
import com.arda.cineverse.data.remote.GeminiApiService
import com.arda.cineverse.data.remote.GeminiNetworkModule
import com.arda.cineverse.data.remote.TmdbApiService
import com.arda.cineverse.data.remote.TmdbNetworkModule
import com.arda.cineverse.data.remote.dto.GeminiContent
import com.arda.cineverse.data.remote.dto.GeminiGenerationConfig
import com.arda.cineverse.data.remote.dto.GeminiPart
import com.arda.cineverse.data.remote.dto.GeminiRequest
import com.arda.cineverse.data.remote.dto.GeminiResponse
import com.arda.cineverse.data.remote.dto.GeminiSchema
import com.arda.cineverse.data.remote.dto.GeminiThinkingConfig
import com.arda.cineverse.data.remote.toUiMovie
import com.google.gson.Gson
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException

/**
 * Sebebi bilinen, mesajı doğrudan kullanıcıya gösterilebilecek asistan hatası.
 * "Model yoğun" ile "internet yok" kullanıcı için çok farklı iki durum; ikisine
 * de aynı genel metni göstermek ne yapması gerektiğini gizliyordu.
 */
class AiChatException(override val message: String) : Exception(message)

/** Gemini'nin döndürdüğü ham JSON'un karşılığı. */
private data class AiReplyDto(
    val intent: String? = null,
    val reply: String? = null,
    val items: List<AiItemDto> = emptyList(),
)

private data class AiItemDto(
    val title: String? = null,
    val year: Int? = null,
    val type: String? = null,
    val note: String? = null,
)

class AiChatRepository(
    private val geminiApi: GeminiApiService = GeminiNetworkModule.api,
    private val tmdbApi: TmdbApiService = TmdbNetworkModule.api,
) {
    private val gson = Gson()

    /**
     * Kullanıcının yeni mesajını, sohbetin o ana kadarki geçmişiyle birlikte
     * Gemini'ye gönderir; dönen başlıkları TMDB'de arayıp gerçek film/dizi
     * verisine bağlar. Geçmiş gönderiliyor çünkü "peki daha komik bir şey?"
     * ya da "hayır, o değildi" gibi devam mesajları ancak önceki turlar
     * bilinirse anlamlı — asistanın kendini tekrar etmemesi de buna bağlı.
     */
    suspend fun reply(
        history: List<ChatMessage>,
        message: String,
        isTvMode: Boolean = false,
    ): Result<AiReply> = runCatching {
        val request = GeminiRequest(
            contents = history.toGeminiContents() + GeminiContent(
                role = "user",
                parts = listOf(GeminiPart(text = message)),
            ),
            generationConfig = GeminiGenerationConfig(
                responseSchema = REPLY_SCHEMA,
                thinkingConfig = GeminiThinkingConfig(thinkingBudget = 0),
            ),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemInstruction(isTvMode)))),
        )

        val response = generateContentWithRetry(request)
        val rawJson = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: error("Yapay zeka bir yanıt üretemedi")

        val dto = gson.fromJson(rawJson, AiReplyDto::class.java)
            ?: error("Yapay zekanın yanıtı okunamadı")

        val intent = when (dto.intent?.uppercase()) {
            "IDENTIFY" -> AiIntent.IDENTIFY
            "ASK" -> AiIntent.ASK
            else -> AiIntent.RECOMMEND
        }

        // ASK'te kart beklenmiyor; boşuna TMDB isteği atmıyoruz.
        val suggestions = if (intent == AiIntent.ASK) {
            emptyList()
        } else {
            coroutineScope {
                dto.items.take(MAX_ITEMS)
                    .map { item ->
                        async {
                            resolveOnTmdb(item, expectedType = if (isTvMode) "tv" else "movie")
                                ?.let { AiMovieSuggestion(it, item.note.orEmpty()) }
                        }
                    }
                    .awaitAll()
                    .filterNotNull()
                    .distinctBy { "${it.movie.mediaType}_${it.movie.id}" }
            }
        }

        AiReply(
            intent = intent,
            text = dto.reply?.trim().orEmpty().ifEmpty { FALLBACK_REPLY },
            suggestions = suggestions,
        )
    }

    /**
     * Sohbet geçmişini Gemini'nin beklediği role'lü formata çevirir. Asistanın
     * kendi turları, o turda ürettiği ham JSON yerine "cümle + önerdiği
     * başlıklar" şeklinde geri veriliyor: model için okuması aynı derecede
     * açık ama çok daha az token, ve daha önce ne önerdiğini net görüyor.
     */
    private fun List<ChatMessage>.toGeminiContents(): List<GeminiContent> =
        takeLast(HISTORY_TURNS).mapNotNull { message ->
            when (message) {
                is UserChatMessage -> GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = message.text)),
                )
                is AiChatMessage -> {
                    val titles = message.suggestions.joinToString { it.movie.title }
                    val text = if (titles.isEmpty()) message.text else "${message.text} (önerdiklerim: $titles)"
                    GeminiContent(role = "model", parts = listOf(GeminiPart(text = text)))
                }
                // Hata balonları sohbetin bir parçası değil, sadece kullanıcıya
                // gösterilen bir uyarı — modele geçmiş olarak verilmiyor.
                else -> null
            }
        }

    /**
     * [expectedType] aktif asistanın türü ("tv" ya da "movie"). Model kuralı
     * çiğneyip dizi asistanında bir film adı verirse bile, TMDB'de aynı isimli
     * bir dizi varsa onu seçiyoruz.
     */
    private suspend fun resolveOnTmdb(item: AiItemDto, expectedType: String): Movie? {
        val title = item.title?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val results = runCatching { tmdbApi.searchMulti(query = title).results.mapNotNull { it.toUiMovie() } }
            .getOrDefault(emptyList())
        if (results.isEmpty()) return null

        // İstenen türde sonuç yoksa elimizdeki en iyi eşleşmeye düşüyoruz.
        val byType = results.filter { it.mediaType == expectedType }.ifEmpty { results }

        // Aynı isimde birden fazla yapım olabilir (remake'ler); yıl verildiyse
        // en yakın yıla sahip olanı seçiyoruz.
        return item.year?.let { year -> byType.minByOrNull { movie -> movie.year?.let { kotlin.math.abs(it - year) } ?: 99 } }
            ?: byType.first()
    }

    /**
     * Gemini bazen "503 model şu an yoğun" hatası ya da geçici bir ağ zaman aşımı
     * (SocketTimeoutException) verebiliyor — ikisi de geçici durumlar, bir kez daha
     * deniyoruz. Başka bir HTTP hatası (400, 403 vb.) gelirse hemen fırlatıyoruz,
     * tekrar denemenin faydası yok.
     *
     * Deneme sayısı ve bekleme süresi kasıtlı olarak kısa: sohbette 20 saniye bekleyip
     * yanıt alamamak, "şu an yoğun, tekrar dene" mesajını hemen görmekten daha kötü.
     */
    private suspend fun generateContentWithRetry(request: GeminiRequest): GeminiResponse {
        repeat(MAX_ATTEMPTS) { attempt ->
            val isLastAttempt = attempt == MAX_ATTEMPTS - 1
            try {
                return geminiApi.generateContent(model = MODEL, request = request)
            } catch (e: HttpException) {
                if (e.code() != 503) throw e
                if (isLastAttempt) throw AiChatException(BUSY_MESSAGE)
                delay(800L)
            } catch (e: IOException) {
                // Zaman aşımı, bağlantı kopması vb. — bunlar da geçici olabilir
                if (isLastAttempt) throw AiChatException(OFFLINE_MESSAGE)
                delay(800L)
            }
        }
        throw AiChatException(BUSY_MESSAGE)
    }

    private companion object {
        // flash-lite adı üstünde daha hafif ama pratikte sürekli 503 dönüyor ve
        // yanıtı yarıda kesiyordu; düşünme kapalı flash hem stabil hem de hızlı.
        const val MODEL = "gemini-3.5-flash"

        const val MAX_ATTEMPTS = 2

        const val MAX_ITEMS = 3

        /** Modele geri beslenen tur sayısı — bağlamı korumakla token maliyeti arasındaki denge. */
        const val HISTORY_TURNS = 8

        const val FALLBACK_REPLY = "Şunlara bakabilirsin:"

        const val BUSY_MESSAGE = "Asistan şu an çok yoğun. Birkaç saniye sonra tekrar dene."
        const val OFFLINE_MESSAGE = "Bağlantı kurulamadı. İnternetini kontrol edip tekrar dene."

        val REPLY_SCHEMA = GeminiSchema(
            type = "OBJECT",
            required = listOf("intent", "reply", "items"),
            properties = mapOf(
                "intent" to GeminiSchema(type = "STRING", enum = listOf("IDENTIFY", "RECOMMEND", "ASK")),
                "reply" to GeminiSchema(type = "STRING"),
                "items" to GeminiSchema(
                    type = "ARRAY",
                    items = GeminiSchema(
                        type = "OBJECT",
                        required = listOf("title", "type", "note"),
                        properties = mapOf(
                            "title" to GeminiSchema(type = "STRING"),
                            "year" to GeminiSchema(type = "INTEGER", nullable = true),
                            "type" to GeminiSchema(type = "STRING", enum = listOf("movie", "tv")),
                            "note" to GeminiSchema(type = "STRING"),
                        ),
                    ),
                ),
            ),
        )

        /**
         * Film ve dizi asistanı aynı kuralları paylaşıyor; ayrıldıkları tek yer
         * hangi tür yapımı önerdikleri. Ana sayfada "Diziler" seçiliyken asistanın
         * film önermesi kullanıcı için kopukluk yaratıyordu.
         */
        fun systemInstruction(isTvMode: Boolean): String {
            val medium = if (isTvMode) "dizi" else "film"
            val itemType = if (isTvMode) "tv" else "movie"
            val scopeRule = if (isTvMode) {
                """
                - SADECE dizi öner. "type" alanı her zaman "tv" olmalı; film, kısa film ya da
                  belgesel filmi verme. Mini diziler ve animasyon diziler serbest.
                - Kullanıcı ısrarla film isterse tek cümleyle burasının dizi asistanı olduğunu
                  söyle ve dizi öner.
                """
            } else {
                """
                - SADECE film öner. "type" alanı her zaman "movie" olmalı; dizi verme.
                - Kullanıcı ısrarla dizi isterse tek cümleyle burasının film asistanı olduğunu
                  söyle ve film öner.
                """
            }.trimIndent()

            return """
                Sen CineVerse uygulamasının $medium asistanısın. Kullanıcıyla Türkçe, samimi ve kısa konuşursun.

                Kullanıcının isteğini şu üç niyetten birine ayır:
                - IDENTIFY: Adını hatırlamadığı bir yapımı tarif ediyor (sahne, konu, oyuncu, afiş vb.).
                  "reply" temkinli olsun, ör: "Tarifine en çok bunlar uyuyor, bu olabilir mi?"
                - RECOMMEND: Ruh hâlini anlatıyor, canının ne istediğini söylüyor ya da beğendiği bir yapıma
                  benzer bir şey istiyor. "reply" doğal ve sıcak olsun, ör: "Bu havadayken şunu önerebilirim:"
                - ASK: İstek tahmin yürütmek için fazla belirsiz ya da konu film/dizi dışında.
                  "items" boş kalsın, "reply" alanına TEK bir kısa netleştirici soru yaz.

                Kurallar:
                $scopeRule
                - "items" en fazla 3 öğe içerir. IDENTIFY'da en olası adaydan başlayarak sırala.
                - Yeni bir istekte 3 seçenek sun. Sadece kullanıcı "bir tane daha" gibi tek bir şey
                  istediyse 1 öğe döndür.
                - "title" TMDB'de aranacağı için yapımın ORİJİNAL adı olmalı (Türkçe çevirisi değil).
                - "note" o yapımı neden verdiğini anlatan, kullanıcının kendi cümlesine gönderme yapan
                  tek kısa cümledir. Konuyu özetleme, sonunu asla açıklama.
                - Bu sohbette daha önce önerdiğin yapımları tekrar etme.
                - Emin olmadığın yapımları uydurma; gerçekten var olanları ver.
                - Film/dizi dışındaki konulara girme, kibarca konuya dön (intent: ASK).

                SADECE aşağıdaki JSON'u döndür, öncesine veya sonrasına hiçbir metin ekleme:
                {"intent":"RECOMMEND","reply":"...","items":[{"title":"...","year":2014,"type":"$itemType","note":"..."}]}
            """.trimIndent()
        }
    }
}
