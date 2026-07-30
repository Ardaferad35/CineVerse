package com.arda.cineverse.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.arda.cineverse.data.local.entity.FeaturedMovieEntity
import com.arda.cineverse.data.local.entity.FeaturedTvEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeaturedDao {
    @Query("SELECT * FROM featured_movie WHERE slot = 'current' LIMIT 1")
    fun observeMovie(): Flow<FeaturedMovieEntity?>

    @Upsert
    suspend fun upsertMovie(entity: FeaturedMovieEntity)

    @Query("SELECT * FROM featured_tv WHERE slot = 'current' LIMIT 1")
    fun observeTv(): Flow<FeaturedTvEntity?>

    @Upsert
    suspend fun upsertTv(entity: FeaturedTvEntity)
}
