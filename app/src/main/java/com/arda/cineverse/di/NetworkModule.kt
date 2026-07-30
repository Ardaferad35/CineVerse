package com.arda.cineverse.di

import com.arda.cineverse.data.remote.TmdbApiService
import com.arda.cineverse.data.remote.TmdbNetworkModule
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /** Mevcut TmdbNetworkModule singleton'ını (Retrofit/OkHttp kurulumu) yeniden kullanır. */
    @Provides
    @Singleton
    fun provideTmdbApiService(): TmdbApiService = TmdbNetworkModule.api
}
