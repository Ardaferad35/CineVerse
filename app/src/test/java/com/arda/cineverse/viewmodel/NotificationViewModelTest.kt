@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.arda.cineverse.viewmodel

import com.arda.cineverse.data.model.AppNotification
import com.arda.cineverse.data.repository.NotificationRepository
import com.arda.cineverse.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NotificationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: NotificationRepository = mockk()

    private fun createViewModel(): NotificationViewModel {
        // init{} icinde refreshUnreadCount() cagriliyor, bu yuzden ViewModel
        // olusturulmadan once bir varsayilan stub sart.
        coEvery { repository.getUnreadCount() } returns Result.success(0)
        return NotificationViewModel(repository)
    }

    @Test
    fun `olusturulunca okunmamis sayisi repository'den yukleniyor`() = runTest {
        coEvery { repository.getUnreadCount() } returns Result.success(7)
        val viewModel = NotificationViewModel(repository)
        advanceUntilIdle()

        assertEquals(7, viewModel.unreadCount.value)
    }

    @Test
    fun `getUnreadCount basarisiz olunca sayi 0 kabul ediliyor`() = runTest {
        coEvery { repository.getUnreadCount() } returns Result.failure(Exception("network"))
        val viewModel = NotificationViewModel(repository)
        advanceUntilIdle()

        assertEquals(0, viewModel.unreadCount.value)
    }

    @Test
    fun `loadNotifications listeyi yukleyip hepsini okundu isaretliyor`() = runTest {
        val viewModel = createViewModel()
        val notifications = listOf(
            AppNotification(id = "1", type = "comment_reply", title = "t", body = "b"),
        )
        coEvery { repository.getNotifications() } returns Result.success(notifications)
        coEvery { repository.markAllAsRead() } returns Result.success(Unit)

        viewModel.loadNotifications()
        advanceUntilIdle()

        assertEquals(notifications, viewModel.uiState.value.notifications)
        assertEquals(false, viewModel.uiState.value.isLoading)
        assertEquals(0, viewModel.unreadCount.value)
        coVerify(exactly = 1) { repository.markAllAsRead() }
    }

    @Test
    fun `loadNotifications basarisiz olunca bos liste gosterip yine de okundu isaretliyor`() = runTest {
        // NOT: NotificationViewModel.loadNotifications() su anki halinde
        // getNotifications() basarisini kontrol etmeden markAllAsRead()
        // cagiriyor — bu test mevcut davranisi belgeliyor, degistirmiyor.
        val viewModel = createViewModel()
        coEvery { repository.getNotifications() } returns Result.failure(Exception("offline"))
        coEvery { repository.markAllAsRead() } returns Result.success(Unit)

        viewModel.loadNotifications()
        advanceUntilIdle()

        assertEquals(emptyList<AppNotification>(), viewModel.uiState.value.notifications)
        coVerify(exactly = 1) { repository.markAllAsRead() }
    }
}
