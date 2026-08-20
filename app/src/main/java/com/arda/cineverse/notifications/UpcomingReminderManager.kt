package com.arda.cineverse.notifications

import android.content.Context
import com.arda.cineverse.navigation.CVRoutes
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object UpcomingReminderManager {
    private const val PREF_NAME = "cineverse_upcoming_reminders"
    private const val KEY_REMINDER_IDS = "reminder_movie_ids"
    private const val KEY_NOTIFIED_IDS = "notified_reminder_ids"

    fun getReminderMovieIds(context: Context): Set<Int> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_REMINDER_IDS, emptySet())
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet() ?: emptySet()
    }

    fun isReminderSet(context: Context, movieId: Int): Boolean {
        return getReminderMovieIds(context).contains(movieId)
    }

    /**
     * Hatırlatıcıyı açar veya kapatır.
     * @return Yeni durum (true = hatırlatıcı eklendi, false = kaldırıldı)
     */
    fun toggleReminder(context: Context, movieId: Int, title: String, releaseDateStr: String): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val current = getReminderMovieIds(context).toMutableSet()
        val isNowSet: Boolean

        if (current.contains(movieId)) {
            current.remove(movieId)
            isNowSet = false
            prefs.edit()
                .remove("movie_${movieId}_title")
                .remove("movie_${movieId}_date")
                .apply()
        } else {
            current.add(movieId)
            isNowSet = true
            prefs.edit()
                .putString("movie_${movieId}_title", title)
                .putString("movie_${movieId}_date", releaseDateStr)
                .apply()
        }

        prefs.edit().putStringSet(KEY_REMINDER_IDS, current.map { it.toString() }.toSet()).apply()
        return isNowSet
    }

    /**
     * Vizyona girmesine 3 gün ve daha az kalan filmler için sistem bildirimi tetikler.
     */
    fun checkAndSend3DayReminders(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val reminderIds = getReminderMovieIds(context)
        if (reminderIds.isEmpty()) return

        val notifiedIds = prefs.getStringSet(KEY_NOTIFIED_IDS, emptySet())
            ?.mapNotNull { it.toIntOrNull() }
            ?.toMutableSet() ?: mutableSetOf()

        val today = LocalDate.now()

        for (movieId in reminderIds) {
            if (notifiedIds.contains(movieId)) continue

            val releaseDateStr = prefs.getString("movie_${movieId}_date", null) ?: continue
            val title = prefs.getString("movie_${movieId}_title", "Film") ?: "Film"

            try {
                val releaseDate = LocalDate.parse(releaseDateStr)
                val daysLeft = ChronoUnit.DAYS.between(today, releaseDate)

                if (daysLeft in 0..3) {
                    val bodyText = if (daysLeft == 0L) {
                        "🎬 $title bugün vizyona girdi! Biletinizi almayı ve detayları incelemeyi unutmayın."
                    } else {
                        "🎬 $title vizyona girmesine son $daysLeft gün kaldı!"
                    }

                    LocalNotificationHelper.show(
                        context = context.applicationContext,
                        notificationId = 2000 + (movieId % 1000),
                        title = "Vizyon Hatırlatıcısı 🔔",
                        body = bodyText,
                        deepLinkRoute = CVRoutes.movieDetail(movieId),
                    )

                    notifiedIds.add(movieId)
                }
            } catch (e: Exception) {
                // Tarih parse edilemezse atla
            }
        }

        prefs.edit().putStringSet(KEY_NOTIFIED_IDS, notifiedIds.map { it.toString() }.toSet()).apply()
    }
}
