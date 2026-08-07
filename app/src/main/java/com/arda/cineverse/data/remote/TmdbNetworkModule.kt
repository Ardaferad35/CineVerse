package com.arda.cineverse.data.remote

import com.arda.cineverse.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object TmdbNetworkModule {

    private const val BASE_URL = "https://api.themoviedb.org/3/"
    private const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val authorized = original.newBuilder()
            .addHeader("Authorization", "Bearer ${BuildConfig.TMDB_READ_ACCESS_TOKEN}")
            .addHeader("Accept", "application/json")
            .build()
        chain.proceed(authorized)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BASIC
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: TmdbApiService = retrofit.create(TmdbApiService::class.java)

    /**
     * TMDB'den gelen "poster_path" gibi kısmi yolları tam URL'e çevirir.
     * size: "w200", "w500", "original" vb. (yüksek çözünürlük için "original" kullanılabilir)
     */
    fun posterUrl(path: String?, size: String = "w500"): String? =
        path?.let { "$IMAGE_BASE_URL$size$it" }
}