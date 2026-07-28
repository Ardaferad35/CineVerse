package com.arda.cineverse.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Uygulama her açıldığında (MainActivity.onCreate) bir kez çağrılır.
 * WorkManager, işi zaten planlanmışsa (KEEP politikası) tekrar
 * planlamıyor — yani bu güvenle her açılışta çağrılabilir.
 */
object NotificationScheduler {

    fun scheduleAll(context: Context) {
        scheduleFilmOfTheDay(context)
        scheduleUpcomingCheck(context)
    }

    private fun scheduleFilmOfTheDay(context: Context) {
        val request = PeriodicWorkRequestBuilder<FilmOfTheDayWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(millisUntilNextMidnight(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "film_of_the_day_check",
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private fun scheduleUpcomingCheck(context: Context) {
        val request = PeriodicWorkRequestBuilder<UpcomingCheckWorker>(6, TimeUnit.HOURS)
            .setInitialDelay(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "upcoming_and_notifications_check",
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private fun millisUntilNextMidnight(): Long {
        val now = Calendar.getInstance()
        val nextMidnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return nextMidnight.timeInMillis - now.timeInMillis
    }
}