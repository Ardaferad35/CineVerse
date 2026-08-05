package com.arda.cineverse.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.arda.cineverse.data.repository.UserListRepository

class OfflineSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userListRepository = UserListRepository.default()
        val success = userListRepository.processPendingActions()
        return if (success) Result.success() else Result.retry()
    }
}
