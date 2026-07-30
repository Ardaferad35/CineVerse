package com.arda.cineverse.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.arda.cineverse.data.local.entity.SectionType
import com.arda.cineverse.data.local.entity.TvSectionCrossRefEntity
import com.arda.cineverse.data.local.entity.TvShowEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TvShowDao {

    @Query(
        """
        SELECT tv_shows.* FROM tv_shows
        INNER JOIN tv_section_cross_ref ON tv_shows.id = tv_section_cross_ref.tvId
        WHERE tv_section_cross_ref.section = :section
        ORDER BY tv_section_cross_ref.position ASC
        """,
    )
    fun observeSection(section: SectionType): Flow<List<TvShowEntity>>

    @Query("SELECT MIN(syncedAt) FROM tv_section_cross_ref WHERE section = :section")
    fun observeLastSyncedAt(section: SectionType): Flow<Long?>

    /** Offline detay fallback'i için: Home'da herhangi bir bölümde cache'lenmiş tekil dizi. */
    @Query("SELECT * FROM tv_shows WHERE id = :tvId LIMIT 1")
    suspend fun getTvShowById(tvId: Int): TvShowEntity?

    @Upsert
    suspend fun upsertTvShows(shows: List<TvShowEntity>)

    @Query("DELETE FROM tv_section_cross_ref WHERE section = :section")
    suspend fun clearSection(section: SectionType)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(refs: List<TvSectionCrossRefEntity>)

    @Transaction
    suspend fun replaceSection(section: SectionType, shows: List<TvShowEntity>, syncedAt: Long) {
        upsertTvShows(shows)
        clearSection(section)
        insertCrossRefs(shows.mapIndexed { index, show -> TvSectionCrossRefEntity(section, show.id, index, syncedAt) })
    }
}
