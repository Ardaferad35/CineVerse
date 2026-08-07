package com.arda.cineverse.data.remote

import com.arda.cineverse.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object GeminiNetworkModule {

    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/"

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val authorized = original.newBuilder()
            .addHeader("x-goog-api-key", BuildConfig.GEMINI_API_KEY)
            .addHeader("Content-Type", "application/json")
            .build()
        chain.proceed(authorized)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        // Yanıt tek seferde (stream'siz) geldiği için modelin tüm metni üretmesini
        // bekliyoruz; düşünen modellerde bu 30 saniyeyi aşabiliyor ve istek boşuna
        // zaman aşımına düşüyordu.
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: GeminiApiService = retrofit.create(GeminiApiService::class.java)
}