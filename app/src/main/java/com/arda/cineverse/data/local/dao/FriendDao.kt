package com.arda.cineverse.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.arda.cineverse.data.local.entity.FriendEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {

    @Query("SELECT * FROM friends ORDER BY fullName ASC")
    fun observeAll(): Flow<List<FriendEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM friends WHERE friendUid = :friendUid)")
    suspend fun exists(friendUid: String): Boolean

    @Query("DELETE FROM friends WHERE friendUid = :friendUid")
    suspend fun deleteById(friendUid: String)

    @Query("DELETE FROM friends")
    suspend fun clearAll()

    @Upsert
    suspend fun upsertAll(items: List<FriendEntity>)

    @Transaction
    suspend fun replaceAll(items: List<FriendEntity>) {
        clearAll()
        upsertAll(items)
    }
}
