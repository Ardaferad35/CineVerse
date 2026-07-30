package com.arda.cineverse.di

import com.arda.cineverse.data.connectivity.ConnectivityObserver
import com.arda.cineverse.data.connectivity.NetworkConnectivityObserver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ConnectivityModule {

    @Binds
    @Singleton
    abstract fun bindConnectivityObserver(impl: NetworkConnectivityObserver): ConnectivityObserver
}
