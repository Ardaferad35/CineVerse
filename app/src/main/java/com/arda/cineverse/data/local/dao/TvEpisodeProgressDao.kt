package com.arda.cineverse.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arda.cineverse.data.local.entity.TvEpisodeProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TvEpisodeProgressDao {

    @Query("SELECT * FROM tv_episode_progress WHERE tvId = :tvId")
    fun observeProgressForTv(tvId: Int): Flow<List<TvEpisodeProgressEntity>>

    @Query("SELECT * FROM tv_episode_progress WHERE tvId = :tvId")
    suspend fun getProgressForTv(tvId: Int): List<TvEpisodeProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(progress: TvEpisodeProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<TvEpisodeProgressEntity>)

    @Query("DELETE FROM tv_episode_progress WHERE tvId = :tvId AND seasonNumber = :seasonNumber AND episodeNumber = :episodeNumber")
    suspend fun delete(tvId: Int, seasonNumber: Int, episodeNumber: Int)

    @Query("DELETE FROM tv_episode_progress WHERE tvId = :tvId AND seasonNumber = :seasonNumber")
    suspend fun deleteSeason(tvId: Int, seasonNumber: Int)

    @Query("DELETE FROM tv_episode_progress WHERE tvId = :tvId")
    suspend fun clearForTv(tvId: Int)

    @Query("DELETE FROM tv_episode_progress")
    suspend fun clearAll()
}
