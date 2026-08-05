package com.arda.cineverse.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.arda.cineverse.data.local.dao.CategoryDao
import com.arda.cineverse.data.local.dao.FeaturedDao
import com.arda.cineverse.data.local.dao.FriendDao
import com.arda.cineverse.data.local.dao.MovieDao
import com.arda.cineverse.data.local.dao.SavedMovieDao
import com.arda.cineverse.data.local.dao.TvShowDao
import com.arda.cineverse.data.local.dao.PendingActionDao
import com.arda.cineverse.data.local.dao.TvEpisodeProgressDao
import com.arda.cineverse.data.local.dao.WatchHistoryDao
import com.arda.cineverse.data.local.entity.CategoryEntity
import com.arda.cineverse.data.local.entity.FeaturedMovieEntity
import com.arda.cineverse.data.local.entity.FeaturedTvEntity
import com.arda.cineverse.data.local.entity.FriendEntity
import com.arda.cineverse.data.local.entity.MovieEntity
import com.arda.cineverse.data.local.entity.MovieSectionCrossRefEntity
import com.arda.cineverse.data.local.entity.PendingActionEntity
import com.arda.cineverse.data.local.entity.SavedMovieEntity
import com.arda.cineverse.data.local.entity.TvEpisodeProgressEntity
import com.arda.cineverse.data.local.entity.TvSectionCrossRefEntity
import com.arda.cineverse.data.local.entity.TvShowEntity
import com.arda.cineverse.data.local.entity.WatchHistoryEntity

@Database(
    entities = [
        MovieEntity::class,
        MovieSectionCrossRefEntity::class,
        TvShowEntity::class,
        TvSectionCrossRefEntity::class,
        CategoryEntity::class,
        FeaturedMovieEntity::class,
        FeaturedTvEntity::class,
        SavedMovieEntity::class,
        WatchHistoryEntity::class,
        FriendEntity::class,
        TvEpisodeProgressEntity::class,
        PendingActionEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class CineVerseDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao
    abstract fun tvShowDao(): TvShowDao
    abstract fun categoryDao(): CategoryDao
    abstract fun featuredDao(): FeaturedDao
    abstract fun savedMovieDao(): SavedMovieDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun friendDao(): FriendDao
    abstract fun tvEpisodeProgressDao(): TvEpisodeProgressDao
    abstract fun pendingActionDao(): PendingActionDao

    companion object {
        const val DATABASE_NAME = "cineverse.db"
    }
}
