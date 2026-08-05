@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.arda.cineverse.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.arda.cineverse.data.common.GENERIC_WRITE_FAILURE_MESSAGE
import com.arda.cineverse.data.common.OfflineWriteException
import com.arda.cineverse.data.model.Movie
import com.arda.cineverse.data.model.MovieDetail
import com.arda.cineverse.data.model.SavedMovie
import com.arda.cineverse.data.repository.MovieRepository
import com.arda.cineverse.data.repository.RecommendationRepository
import com.arda.cineverse.data.repository.UserListRepository
import com.arda.cineverse.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private const val MOVIE_ID = 42

class MovieDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val movieRepository: MovieRepository = mockk()
    private val userListRepository: UserListRepository = mockk()
    private val recommendationRepository: RecommendationRepository = mockk()

    private val movieDetail = MovieDetail(
        id = MOVIE_ID,
        title = "Test Film",
        backdropUrl = null,
        posterUrl = null,
        tmdbRating = 8.0,
        year = 2024,
        durationLabel = "2s 10dk",
        genres = listOf("Aksiyon"),
        genreIds = listOf(28),
        overview = "",
        director = null,
        cast = emptyList(),
        trailerKey = null,
        similarMovies = emptyList(),
    )

    /** loadUserListStatus() init'te her zaman cagriliyor, bu yuzden varsayilan stub sart. */
    private fun createViewModel(): MovieDetailViewModel {
        coEvery { userListRepository.isFavorite(MOVIE_ID) } returns false
        coEvery { userListRepository.isInWatchlist(MOVIE_ID) } returns false
        return MovieDetailViewModel(
            SavedStateHandle(mapOf("movieId" to MOVIE_ID)),
            movieRepository,
            userListRepository,
            recommendationRepository,
        )
    }

    @Test
    fun `load basarili olunca filmi gosterip goruntulenme sinyalini kaydediyor`() = runTest {
        coEvery { movieRepository.getMovieDetailFull(MOVIE_ID) } returns Result.success(movieDetail)
        coEvery { recommendationRepository.recordView(MOVIE_ID, listOf(28)) } returns Unit

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(movieDetail, viewModel.uiState.value.movie)
        assertEquals(false, viewModel.uiState.value.isOfflineFallback)
        assertEquals(false, viewModel.uiState.value.isLoading)
        coVerify(exactly = 1) { recommendationRepository.recordView(MOVIE_ID, listOf(28)) }
    }

    @Test
    fun `load basarisiz ama cache'te film varsa kismi cevrimdisi gorunum gosteriyor`() = runTest {
        coEvery { movieRepository.getMovieDetailFull(MOVIE_ID) } returns Result.failure(Exception("offline"))
        val cached = Movie(id = MOVIE_ID, title = "Test Film", year = 2024, rating = 8.0)
        coEvery { movieRepository.getCachedMovie(MOVIE_ID) } returns cached

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.isOfflineFallback)
        assertEquals("Test Film", viewModel.uiState.value.movie?.title)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `load basarisiz ve cache'te de yoksa hata mesaji gosteriyor`() = runTest {
        coEvery { movieRepository.getMovieDetailFull(MOVIE_ID) } returns Result.failure(Exception("offline"))
        coEvery { movieRepository.getCachedMovie(MOVIE_ID) } returns null

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(
            "Film bilgileri yüklenemedi. İnternet bağlantınızı kontrol edin.",
            viewModel.uiState.value.errorMessage,
        )
        assertNull(viewModel.uiState.value.movie)
    }

    @Test
    fun `toggleFavorite basarili olunca favori isaretleyip oneri sinyali kaydediyor`() = runTest {
        coEvery { movieRepository.getMovieDetailFull(MOVIE_ID) } returns Result.success(movieDetail)
        coEvery { recommendationRepository.recordView(any(), any()) } returns Unit
        val viewModel = createViewModel()
        advanceUntilIdle()

        coEvery { userListRepository.addFavorite(any<SavedMovie>()) } returns Result.success(Unit)
        coEvery { recommendationRepository.recordFavorite(MOVIE_ID, listOf(28)) } returns Unit

        viewModel.toggleFavorite()
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.isFavorite)
        coVerify(exactly = 1) { recommendationRepository.recordFavorite(MOVIE_ID, listOf(28)) }
    }

    @Test
    fun `toggleFavorite offline'ken basarisiz olunca iyimser guncelleme geri aliniyor`() = runTest {
        coEvery { movieRepository.getMovieDetailFull(MOVIE_ID) } returns Result.success(movieDetail)
        coEvery { recommendationRepository.recordView(any(), any()) } returns Unit
        val viewModel = createViewModel()
        advanceUntilIdle()

        coEvery { userListRepository.addFavorite(any<SavedMovie>()) } returns Result.failure(OfflineWriteException())

        viewModel.toggleFavorite()
        // Iyimser guncelleme senkron: coroutine daha calismadan true gorunur.
        assertEquals(true, viewModel.uiState.value.isFavorite)

        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isFavorite)
        assertEquals("Bu işlem için internet bağlantısı gerekiyor", viewModel.uiState.value.offlineMessage)
    }

    @Test
    fun `toggleWatchlist genel bir hatada genel mesajla geri aliniyor`() = runTest {
        coEvery { movieRepository.getMovieDetailFull(MOVIE_ID) } returns Result.success(movieDetail)
        coEvery { recommendationRepository.recordView(any(), any()) } returns Unit
        val viewModel = createViewModel()
        advanceUntilIdle()

        coEvery { userListRepository.addToWatchlist(any<SavedMovie>()) } returns Result.failure(Exception("baska bir sebep"))

        viewModel.toggleWatchlist()
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isSaved)
        assertEquals(GENERIC_WRITE_FAILURE_MESSAGE, viewModel.uiState.value.offlineMessage)
    }

    @Test
    fun `clearOfflineMessage mesaji temizliyor`() = runTest {
        coEvery { movieRepository.getMovieDetailFull(MOVIE_ID) } returns Result.success(movieDetail)
        coEvery { recommendationRepository.recordView(any(), any()) } returns Unit
        val viewModel = createViewModel()
        advanceUntilIdle()
        coEvery { userListRepository.addFavorite(any<SavedMovie>()) } returns Result.failure(OfflineWriteException())
        viewModel.toggleFavorite()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.offlineMessage != null)

        viewModel.clearOfflineMessage()

        assertNull(viewModel.uiState.value.offlineMessage)
    }
}
