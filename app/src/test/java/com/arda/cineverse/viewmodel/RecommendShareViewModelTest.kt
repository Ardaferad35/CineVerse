@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.arda.cineverse.viewmodel

import com.arda.cineverse.data.common.SyncResult
import com.arda.cineverse.data.model.Friend
import com.arda.cineverse.data.repository.FriendRepository
import com.arda.cineverse.data.repository.RecommendationQuotaException
import com.arda.cineverse.data.repository.RecommendationShareRepository
import com.arda.cineverse.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RecommendShareViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val friendRepository: FriendRepository = mockk()
    private val shareRepository: RecommendationShareRepository = mockk()
    private val friendsFlow = MutableStateFlow<List<Friend>>(
        listOf(Friend(friendUid = "u1"), Friend(friendUid = "u2")),
    )

    private fun createViewModel(remainingQuota: Int = RecommendationShareRepository.DAILY_LIMIT): RecommendShareViewModel {
        every { friendRepository.observeFriends() } returns friendsFlow
        coEvery { friendRepository.syncFriends() } returns SyncResult.Success
        coEvery { shareRepository.getRemainingQuota() } returns Result.success(remainingQuota)
        return RecommendShareViewModel(friendRepository, shareRepository)
    }

    @Test
    fun `olusturulunca arkadaslar ve kalan kota yukleniyor`() = runTest {
        val viewModel = createViewModel(remainingQuota = 4)
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.friends.size)
        assertEquals(4, viewModel.uiState.value.remainingQuota)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `toggleFriend secimi ekleyip cikariyor`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleFriend("u1")
        assertTrue("u1" in viewModel.uiState.value.selectedUids)

        viewModel.toggleFriend("u1")
        assertFalse("u1" in viewModel.uiState.value.selectedUids)
    }

    @Test
    fun `onNoteChange notu MAX_NOTE_LENGTH ile sinirliyor`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onNoteChange("x".repeat(200))

        assertEquals(RecommendShareViewModel.MAX_NOTE_LENGTH, viewModel.uiState.value.note.length)
    }

    @Test
    fun `hicbir arkadas secilmemisse canSend false`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.canSend)
    }

    @Test
    fun `secim kalan kotayi asarsa isOverQuota true ve canSend false olur`() = runTest {
        val viewModel = createViewModel(remainingQuota = 1)
        advanceUntilIdle()

        viewModel.toggleFriend("u1")
        viewModel.toggleFriend("u2")

        assertTrue(viewModel.uiState.value.isOverQuota)
        assertFalse(viewModel.uiState.value.canSend)
    }

    @Test
    fun `send basarili olunca sentCount secili kisi sayisina esitleniyor`() = runTest {
        val viewModel = createViewModel(remainingQuota = 5)
        advanceUntilIdle()
        viewModel.toggleFriend("u1")
        coEvery {
            shareRepository.send(
                targetUids = listOf("u1"),
                mediaId = 42,
                mediaType = "movie",
                mediaTitle = "Test Film",
                posterUrl = null,
                note = "",
            )
        } returns Result.success(Unit)

        viewModel.send(mediaId = 42, mediaType = "movie", mediaTitle = "Test Film")
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.sentCount)
        assertFalse(viewModel.uiState.value.isSending)
    }

    @Test
    fun `send kota dolunca kotayi yeniden okuyup ozel mesaj gosteriyor`() = runTest {
        val viewModel = createViewModel(remainingQuota = 5)
        advanceUntilIdle()
        viewModel.toggleFriend("u1")
        coEvery { shareRepository.send(any(), any(), any(), any(), any(), any()) } returns
            Result.failure(RecommendationQuotaException())
        coEvery { shareRepository.getRemainingQuota() } returns Result.success(0)

        viewModel.send(mediaId = 42, mediaType = "movie", mediaTitle = "Test Film")
        advanceUntilIdle()

        assertEquals("Günlük öneri hakkın doldu, yarın tekrar dene.", viewModel.uiState.value.errorMessage)
        assertEquals(0, viewModel.uiState.value.remainingQuota)
        coVerify(atLeast = 2) { shareRepository.getRemainingQuota() } // init + kota-dolu sonrasi yeniden okuma
    }

    @Test
    fun `send genel bir hatada kota tekrar okunmuyor`() = runTest {
        val viewModel = createViewModel(remainingQuota = 5)
        advanceUntilIdle()
        viewModel.toggleFriend("u1")
        coEvery { shareRepository.send(any(), any(), any(), any(), any(), any()) } returns
            Result.failure(Exception("sunucu hatasi"))

        viewModel.send(mediaId = 42, mediaType = "movie", mediaTitle = "Test Film")
        advanceUntilIdle()

        assertEquals("Gönderilemedi, tekrar dene.", viewModel.uiState.value.errorMessage)
        coVerify(exactly = 1) { shareRepository.getRemainingQuota() } // sadece init'teki
    }

    @Test
    fun `reset secim ve notu temizleyip kotayi yeniliyor`() = runTest {
        val viewModel = createViewModel(remainingQuota = 5)
        advanceUntilIdle()
        viewModel.toggleFriend("u1")
        viewModel.onNoteChange("merhaba")

        viewModel.reset()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.selectedUids.isEmpty())
        assertEquals("", viewModel.uiState.value.note)
        assertNull(viewModel.uiState.value.sentCount)
    }
}
