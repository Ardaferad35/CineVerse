package com.arda.cineverse.data.model

/**
 * Yapay zekanın kullanıcının ne istediğine dair kararı. Metnin tonunu bu
 * belirliyor: [IDENTIFY] "bu olabilir mi?", [RECOMMEND] "şunu önerebilirim".
 * [ASK] ise tarif tahmin yürütmek için fazla belirsiz demek — kart yerine
 * tek bir netleştirici soru geliyor.
 */
enum class AiIntent { IDENTIFY, RECOMMEND, ASK }

/** Bir film/dizi ve yapay zekanın onu neden verdiğine dair kendi cümlesi. */
data class AiMovieSuggestion(
    val movie: Movie,
    val note: String,
)

/** Yapay zekanın tek bir turda ürettiği, TMDB ile eşleştirilmiş yanıt. */
data class AiReply(
    val intent: AiIntent,
    val text: String,
    val suggestions: List<AiMovieSuggestion> = emptyList(),
)

sealed interface ChatMessage {
    val id: Long
}

data class UserChatMessage(
    override val id: Long,
    val text: String,
) : ChatMessage

data class AiChatMessage(
    override val id: Long,
    val text: String,
    val intent: AiIntent,
    val suggestions: List<AiMovieSuggestion> = emptyList(),
) : ChatMessage

data class ChatErrorMessage(
    override val id: Long,
    val text: String,
) : ChatMessage
