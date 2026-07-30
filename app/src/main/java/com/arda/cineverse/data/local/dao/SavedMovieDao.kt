package com.arda.cineverse.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.arda.cineverse.data.local.entity.SavedMovieEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedMovieDao {

    @Query("SELECT * FROM saved_movies WHERE listType = :listType ORDER BY addedAt DESC")
    fun observeByListType(listType: String): Flow<List<SavedMovieEntity>>

    @Query("DELETE FROM saved_movies WHERE listType = :listType")
    suspend fun clear(listType: String)

    @Query("DELETE FROM saved_movies")
    suspend fun clearAll()

    @Upsert
    suspend fun upsertAll(items: List<SavedMovieEntity>)

    @Transaction
    suspend fun replaceAll(listType: String, items: List<SavedMovieEntity>) {
        clear(listType)
        upsertAll(items)
    }
}
