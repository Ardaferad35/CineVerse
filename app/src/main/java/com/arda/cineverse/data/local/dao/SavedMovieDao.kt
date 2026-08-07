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

    /**
     * Offline detay fallback'i için: bir film/dizi Home'un hiçbir bölümünde
     * (popüler/en çok oylanan/vb.) hiç görünmemiş olsa bile, kullanıcı onu
     * favorilere/izleme listesine eklediyse burada denk gelinebilir.
     */
    @Query("SELECT * FROM saved_movies WHERE mediaId = :mediaId AND mediaType = :mediaType LIMIT 1")
    suspend fun getById(mediaId: Int, mediaType: String): SavedMovieEntity?

    /**
     * "Favori mi/izleme listemde mi" göstergesi (kalp/kaydet ikonu) için.
     * Firestore'a gitmek yerine Room'a bakar — bu sayede offline'ken de
     * doğru durumu gösterir (Firestore .get() offline'ken güvenilir şekilde
     * cevap vermeyebilir, Room ise senkron olduğu sürece her zaman doğru).
     */
    @Query("SELECT EXISTS(SELECT 1 FROM saved_movies WHERE mediaId = :mediaId AND mediaType = :mediaType AND listType = :listType)")
    suspend fun exists(mediaId: Int, mediaType: String, listType: String): Boolean

    @Query("SELECT COUNT(*) FROM saved_movies WHERE listType = :listType")
    suspend fun countByListType(listType: String): Int

    @Query("DELETE FROM saved_movies WHERE listType = :listType")
    suspend fun clear(listType: String)

    @Query("DELETE FROM saved_movies")
    suspend fun clearAll()

    @Query("DELETE FROM saved_movies WHERE mediaId = :mediaId AND mediaType = :mediaType AND listType = :listType")
    suspend fun delete(mediaId: Int, mediaType: String, listType: String)

    @Upsert
    suspend fun upsert(item: SavedMovieEntity)

    @Upsert
    suspend fun upsertAll(items: List<SavedMovieEntity>)

    @Transaction
    suspend fun replaceAll(listType: String, items: List<SavedMovieEntity>) {
        clear(listType)
        upsertAll(items)
    }
}
