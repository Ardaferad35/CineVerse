package com.arda.cineverse.data.remote.dto

data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig,
)

data class GeminiContent(
    val parts: List<GeminiPart>,
)

data class GeminiPart(
    val text: String,
)

data class GeminiGenerationConfig(
    val responseMimeType: String = "application/json",
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
)

data class GeminiCandidate(
    val content: GeminiContent? = null,
)