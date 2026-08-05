package com.arda.cineverse.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.arda.cineverse.data.repository.MovieRepository
import com.arda.cineverse.data.repository.NotificationRepository
import com.arda.cineverse.di.AppGraph
import com.arda.cineverse.navigation.CVRoutes
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first

/**
 * Periyodik olarak (yaklaşık her birkaç saatte bir) çalışır. İki şeyi
 * kontrol eder:
 * 1. "Yakında Vizyona Girecekler" listesine yeni bir film eklenmiş mi?
 * 2. Firestore'daki bildirim kutusunda (örn. yorum yanıtları) okunmamış
 *    yeni bir şey var mı?
 * İkisi için de gerçek bir değişiklik varsa sistem bildirimi gösterir.
 */
class UpcomingCheckWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    private val prefs = context.getSharedPreferences("cineverse_notifications_prefs", Context.MODE_PRIVATE)

    override suspend fun doWork(): Result {
        // Sadece giriş yapmış kullanıcı için anlamlı
        if (FirebaseAuth.getInstance().currentUser == null) return Result.success()
        // Kullanıcı Profil > Bildirimler'i kapattıysa hiç göstermiyoruz.
        if (!AppGraph.userPreferencesRepository.userPreferences.first().notificationsEnabled) {
            return Result.success()
        }

        checkUpcomingMovies()
        checkUnreadNotifications()

        return Result.success()
    }

    private suspend fun checkUpcomingMovies() {
        val movieRepository = MovieRepository.default()
        val upcoming = movieRepository.getUpcomingMovies().getOrNull() ?: return

        val knownIds = prefs.getStringSet("known_upcoming_ids", emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet()
        val currentIds = upcoming.map { it.id }.toSet()
        val newMovies = upcoming.filter { it.id !in knownIds }

        // İlk çalıştırmada (knownIds boşsa) bildirim göstermiyoruz —
        // sadece listeyi hafızaya alıyoruz, aksi halde ilk açılışta
        // onlarca film için bildirim yağardı.
        if (knownIds.isNotEmpty() && newMovies.isNotEmpty()) {
            val body: String
            val deepLinkRoute: String
            if (newMovies.size == 1) {
                body = "${newMovies.first().title} yakında vizyonda!"
                deepLinkRoute = CVRoutes.movieDetail(newMovies.first().id)
            } else {
                body = "${newMovies.first().title} ve ${newMovies.size - 1} film daha yakında vizyonda!"
                deepLinkRoute = CVRoutes.movieList("upcoming")
            }
            LocalNotificationHelper.show(
                context = applicationContext,
                notificationId = 1002,
                title = "Yakında Vizyona Girecekler",
                body = body,
                deepLinkRoute = deepLinkRoute,
            )
        }

        prefs.edit().putStringSet("known_upcoming_ids", currentIds.map { it.toString() }.toSet()).apply()
    }

    private suspend fun checkUnreadNotifications() {
        val notificationRepository = NotificationRepository.default()
        val unreadCount = notificationRepository.getUnreadCount().getOrNull() ?: return

        val lastNotifiedCount = prefs.getInt("last_unread_notified_count", 0)
        // Sadece sayı ARTTIĞINDA bildir — "!=" kullanılsaydı kullanıcı bir
        // bildirimi okuyup sayı azaldığında da (örn. 3 -> 2) yeni hiçbir şey
        // gelmediği halde tekrar bildirim düşerdi. Azalışlar aşağıda yine de
        // kaydediliyor ki sonraki artış doğru tabana göre kıyaslansın.
        if (unreadCount > lastNotifiedCount) {
            LocalNotificationHelper.show(
                context = applicationContext,
                notificationId = 1003,
                title = "Yeni Bildiriminiz Var",
                body = if (unreadCount == 1) "1 yeni bildiriminiz var" else "$unreadCount yeni bildiriminiz var",
                deepLinkRoute = CVRoutes.NOTIFICATIONS,
            )
        }
        prefs.edit().putInt("last_unread_notified_count", unreadCount).apply()
    }
}