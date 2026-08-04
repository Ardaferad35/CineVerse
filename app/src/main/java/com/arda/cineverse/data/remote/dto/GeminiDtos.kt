package com.arda.cineverse.data.remote.dto

data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig,
    // Sohbet boyunca sabit kalan kurallar (kimliği, yanıt formatı). Her tur
    // yeniden gönderilen "contents" içine karıştırmak yerine buraya konuyor —
    // model bunu bir kullanıcı mesajı gibi değil, talimat olarak işliyor.
    val systemInstruction: GeminiContent? = null,
)

data class GeminiContent(
    val parts: List<GeminiPart>,
    // Çok turlu sohbette kimin konuştuğu: "user" veya "model". Tek seferlik
    // isteklerde ve systemInstruction'da gönderilmiyor.
    val role: String? = null,
)

data class GeminiPart(
    val text: String,
)

data class GeminiGenerationConfig(
    val responseMimeType: String = "application/json",
    // Sadece "JSON döndür" demek yetmiyordu: model yanıtı bazen cümlenin
    // ortasında kesip yarım JSON gönderiyordu. Şema verildiğinde çıktı
    // biçimi modele zorlatılıyor, alan adları da garanti altına alınıyor.
    val responseSchema: GeminiSchema? = null,
    val thinkingConfig: GeminiThinkingConfig? = null,
)

/**
 * Modelin yanıt öncesi "düşünme" bütçesi. Sıfır verildiğinde düşünme adımı
 * tamamen kapanıyor: sohbet asistanı için yanıt saniyeler kadar erken geliyor,
 * kaliteden gözle görülür bir kayıp olmuyor.
 */
data class GeminiThinkingConfig(
    val thinkingBudget: Int,
)

/**
 * Gemini'nin beklediği OpenAPI alt kümesi. Null alanlar Gson tarafından
 * atlandığı için tek bir sınıf hem nesne, hem dizi, hem de ilkel tipleri
 * tarif edebiliyor.
 */
data class GeminiSchema(
    val type: String,
    val properties: Map<String, GeminiSchema>? = null,
    val items: GeminiSchema? = null,
    val required: List<String>? = null,
    val enum: List<String>? = null,
    val nullable: Boolean? = null,
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
)

data class GeminiCandidate(
    val content: GeminiContent? = null,
)
