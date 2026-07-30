package com.arda.cineverse.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.arda.cineverse.data.repository.MovieRepository
import com.arda.cineverse.di.AppGraph
import com.arda.cineverse.navigation.CVRoutes
import kotlinx.coroutines.flow.first

/**
 * Günde bir kez (gece yarısına yakın) çalışır. O günün önerilen filmini
 * çeker; bir önceki çalışmadan farklıysa (yani gerçekten değiştiyse)
 * sistem bildirimi gösterir. Birkaç dakikalık gecikme sorun değil.
 */
class FilmOfTheDayWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    private val prefs = context.getSharedPreferences("cineverse_notifications_prefs", Context.MODE_PRIVATE)

    override suspend fun doWork(): Result {
        // Kullanıcı Profil > Bildirimler'i kapattıysa hiç göstermiyoruz.
        if (!AppGraph.userPreferencesRepository.userPreferences.first().notificationsEnabled) {
            return Result.success()
        }

        val movieRepository = MovieRepository()
        val featured = movieRepository.getFeaturedMovie().getOrNull() ?: return Result.retry()

        val lastNotifiedId = prefs.getInt("last_film_of_the_day_id", -1)
        if (featured.id != lastNotifiedId) {
            LocalNotificationHelper.show(
                context = applicationContext,
                notificationId = 1001,
                title = "Bugünün Filmi: ${featured.title}",
                body = featured.description.take(120),
                deepLinkRoute = CVRoutes.movieDetail(featured.id),
            )
            prefs.edit().putInt("last_film_of_the_day_id", featured.id).apply()
        }

        return Result.success()
    }
}