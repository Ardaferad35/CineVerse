package com.arda.cineverse.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.arda.cineverse.data.local.entity.MovieEntity
import com.arda.cineverse.data.local.entity.MovieSectionCrossRefEntity
import com.arda.cineverse.data.local.entity.SectionType
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {

    @Query(
        """
        SELECT movies.* FROM movies
        INNER JOIN movie_section_cross_ref ON movies.id = movie_section_cross_ref.movieId
        WHERE movie_section_cross_ref.section = :section
        ORDER BY movie_section_cross_ref.position ASC
        """,
    )
    fun observeSection(section: SectionType): Flow<List<MovieEntity>>

    @Query("SELECT MIN(syncedAt) FROM movie_section_cross_ref WHERE section = :section")
    fun observeLastSyncedAt(section: SectionType): Flow<Long?>

    /** Offline detay fallback'i için: Home'da herhangi bir bölümde cache'lenmiş tekil film. */
    @Query("SELECT * FROM movies WHERE id = :movieId LIMIT 1")
    suspend fun getMovieById(movieId: Int): MovieEntity?

    @Upsert
    suspend fun upsertMovies(movies: List<MovieEntity>)

    @Query("DELETE FROM movie_section_cross_ref WHERE section = :section")
    suspend fun clearSection(section: SectionType)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(refs: List<MovieSectionCrossRefEntity>)

    /** Section'ı taze veriyle değiştirir: kanonik satırlar upsert edilir, eski cross-ref silinip yenisi yazılır. */
    @Transaction
    suspend fun replaceSection(section: SectionType, movies: List<MovieEntity>, syncedAt: Long) {
        upsertMovies(movies)
        clearSection(section)
        insertCrossRefs(movies.mapIndexed { index, movie -> MovieSectionCrossRefEntity(section, movie.id, index, syncedAt) })
    }
}
