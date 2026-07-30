package com.arda.cineverse.di

import android.content.Context
import androidx.room.Room
import com.arda.cineverse.data.local.dao.CategoryDao
import com.arda.cineverse.data.local.dao.FeaturedDao
import com.arda.cineverse.data.local.dao.MovieDao
import com.arda.cineverse.data.local.dao.SavedMovieDao
import com.arda.cineverse.data.local.dao.TvShowDao
import com.arda.cineverse.data.local.dao.WatchHistoryDao
import com.arda.cineverse.data.local.db.CineVerseDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CineVerseDatabase =
        Room.databaseBuilder(context, CineVerseDatabase::class.java, CineVerseDatabase.DATABASE_NAME).build()

    @Provides
    fun provideMovieDao(database: CineVerseDatabase): MovieDao = database.movieDao()

    @Provides
    fun provideTvShowDao(database: CineVerseDatabase): TvShowDao = database.tvShowDao()

    @Provides
    fun provideCategoryDao(database: CineVerseDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideFeaturedDao(database: CineVerseDatabase): FeaturedDao = database.featuredDao()

    @Provides
    fun provideSavedMovieDao(database: CineVerseDatabase): SavedMovieDao = database.savedMovieDao()

    @Provides
    fun provideWatchHistoryDao(database: CineVerseDatabase): WatchHistoryDao = database.watchHistoryDao()
}
