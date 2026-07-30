package com.arda.cineverse.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.arda.cineverse.data.local.entity.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {

    @Query("SELECT * FROM watch_history WHERE mediaType = :mediaType ORDER BY position ASC")
    fun observeHistory(mediaType: String): Flow<List<WatchHistoryEntity>>

    @Query("DELETE FROM watch_history WHERE mediaType = :mediaType")
    suspend fun clear(mediaType: String)

    @Upsert
    suspend fun upsertAll(items: List<WatchHistoryEntity>)

    @Transaction
    suspend fun replaceAll(mediaType: String, items: List<WatchHistoryEntity>) {
        clear(mediaType)
        upsertAll(items)
    }
}
