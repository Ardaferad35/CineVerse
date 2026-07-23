package com.arda.cineverse.data.remote

import com.arda.cineverse.data.remote.dto.GeminiRequest
import com.arda.cineverse.data.remote.dto.GeminiResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface GeminiApiService {
    @POST("models/gemini-3.5-flash-lite:generateContent")
    suspend fun generateContent(@Body request: GeminiRequest): GeminiResponse
}