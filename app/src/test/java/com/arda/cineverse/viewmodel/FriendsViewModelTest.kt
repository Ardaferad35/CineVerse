@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.arda.cineverse.viewmodel

import com.arda.cineverse.data.common.SyncResult
import com.arda.cineverse.data.model.Friend
import com.arda.cineverse.data.model.FriendRequest
import com.arda.cineverse.data.model.FriendSearchResult
import com.arda.cineverse.data.repository.FriendRepository
import com.arda.cineverse.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FriendsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: FriendRepository = mockk()
    private val friendsFlow = MutableStateFlow<List<Friend>>(emptyList())

    /** init{} icinde observeFriends() toplanmaya baslaniyor ve refresh() cagriliyor — ikisi de stub gerektiriyor. */
    private fun createViewModel(): FriendsViewModel {
        every { repository.observeFriends() } returns friendsFlow
        coEvery { repository.syncFriends() } returns SyncResult.Success
        coEvery { repository.getIncomingRequests() } returns Result.success(emptyList())
        return FriendsViewModel(repository)
    }

    @Test
    fun `olusturulunca observeFriends akisindaki liste state'e yansiyor`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        friendsFlow.value = listOf(Friend(friendUid = "u1", username = "ali"))
        advanceUntilIdle()

        assertEquals("u1", viewModel.uiState.value.friends.single().friendUid)
    }

    @Test
    fun `3 karakterden kisa sorguda arama tetiklenmiyor`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSearchQueryChange("ab")
        advanceTimeBy(1000)
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.searchUserByUsername(any()) }
    }

    @Test
    fun `debounce suresi dolunca arama otomatik tetikleniyor`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        coEvery { repository.searchUserByUsername("ali") } returns Result.success(null)

        viewModel.onSearchQueryChange("ali")
        // FriendsViewModel.SEARCH_DEBOUNCE_MS (private companion) = 400ms.
        advanceTimeBy(450)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.searchUserByUsername("ali") }
    }

    @Test
    fun `search kullanici bulamayinca hata mesaji gosteriyor`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        coEvery { repository.searchUserByUsername("ali") } returns Result.success(null)

        viewModel.onSearchQueryChange("ali")
        advanceUntilIdle()

        assertEquals("Kullanıcı bulunamadı", viewModel.uiState.value.searchError)
        assertNull(viewModel.uiState.value.searchResult)
    }

    @Test
    fun `search sonuc bulunca searchResult doluyor`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val result = FriendSearchResult(uid = "u2", username = "ali", fullName = "Ali", avatarId = "a1")
        coEvery { repository.searchUserByUsername("ali") } returns Result.success(result)

        viewModel.onSearchQueryChange("ali")
        advanceUntilIdle()

        assertEquals(result, viewModel.uiState.value.searchResult)
        assertNull(viewModel.uiState.value.searchError)
    }

    @Test
    fun `sendFriendRequest basarili olunca sentRequestUids'e ekleniyor`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val target = FriendSearchResult(uid = "u3", username = "veli", fullName = "Veli", avatarId = "a2")
        coEvery { repository.sendFriendRequest(target) } returns Result.success(Unit)

        viewModel.sendFriendRequest(target)
        advanceUntilIdle()

        assertTrue("u3" in viewModel.uiState.value.sentRequestUids)
        assertNull(viewModel.uiState.value.sendingRequestUid)
    }

    @Test
    fun `acceptRequest once istegi listeden kaldirir, basarisizlikta geri getirir`() = runTest {
        val viewModel = createViewModel()
        val request = FriendRequest(fromUid = "u4", fromUsername = "veli")
        coEvery { repository.getIncomingRequests() } returns Result.success(listOf(request))
        viewModel.selectTab(FriendsTab.REQUESTS)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.incomingRequests.size)

        coEvery { repository.acceptFriendRequest(request) } returns Result.failure(Exception("kabul edilemedi"))
        viewModel.acceptRequest(request)

        // Iyimser guncelleme: coroutine daha calismadan istek hemen kayboluyor.
        assertEquals(0, viewModel.uiState.value.incomingRequests.size)

        advanceUntilIdle()

        // Basarisizlik uzerine eski liste geri geldi.
        assertEquals(1, viewModel.uiState.value.incomingRequests.size)
        assertEquals("kabul edilemedi", viewModel.uiState.value.actionMessage)
    }

    @Test
    fun `removeFriend basarili olunca listede kalici olarak kayboluyor`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        friendsFlow.value = listOf(Friend(friendUid = "u5", username = "ayse"))
        advanceUntilIdle()
        coEvery { repository.removeFriend("u5") } returns Result.success(Unit)

        viewModel.removeFriend("u5")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.friends.isEmpty())
    }
}
