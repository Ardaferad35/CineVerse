package com.arda.cineverse.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.arda.cineverse.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories WHERE isTv = :isTv ORDER BY position ASC")
    fun observeAll(isTv: Boolean): Flow<List<CategoryEntity>>

    @Query("DELETE FROM categories WHERE isTv = :isTv")
    suspend fun clear(isTv: Boolean)

    @Upsert
    suspend fun upsertAll(categories: List<CategoryEntity>)

    @Transaction
    suspend fun replaceAll(isTv: Boolean, categories: List<CategoryEntity>) {
        clear(isTv)
        upsertAll(categories)
    }
}
