package com.arda.cineverse.data.remote

import com.arda.cineverse.data.remote.dto.GeminiRequest
import com.arda.cineverse.data.remote.dto.GeminiResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface GeminiApiService {
    // Model adı yola parametre olarak geçiyor: "flash-lite" yoğun olduğunda
    // aynı istek daha büyük bir modele yönlendirilebiliyor (bkz. AiChatRepository).
    @POST("models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Body request: GeminiRequest,
    ): GeminiResponse
}
