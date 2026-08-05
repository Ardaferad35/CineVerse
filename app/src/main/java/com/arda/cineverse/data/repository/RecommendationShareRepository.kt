package com.arda.cineverse.data.repository

import com.arda.cineverse.data.remote.SupabasePushClient
import com.arda.cineverse.data.remote.SupabasePushException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.Date
import javax.inject.Inject

/** Günlük öneri hakkı doldu — kullanıcıya ayrı bir mesaj gösteriliyor. */
class RecommendationQuotaException : Exception("Günlük öneri hakkın doldu")

/**
 * Bir film/diziyi arkadaşlara önerme ve bunun günlük kotası.
 *
 * Bildirim belgesini istemci YAZAMAZ (firestore.rules'ta notifications için
 * `allow create: if false`), bu yüzden gönderim Edge Function üzerinden
 * gidiyor. Kota da orada düşülüyor; buradaki okuma sadece arayüzün kalan
 * hakkı gösterip butonu kilitlemesi için — asıl sınır sunucuda.
 */
class RecommendationShareRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) {
    private fun requireUid(): String = auth.currentUser?.uid ?: error("Giriş yapmalısınız")

    /**
     * Bugün kalan gönderim hakkı. Belge yoksa ya da başka bir güne aitse
     * kullanıcı bugün hiç göndermemiştir.
     */
    suspend fun getRemainingQuota(): Result<Int> = runCatching {
        val snapshot = firestore.collection("users").document(requireUid())
            .collection("limits").document("recommendations")
            .get().await()

        val day = snapshot.getString("day")
        val used = if (day == todayKey()) snapshot.getLong("count")?.toInt() ?: 0 else 0
        (DAILY_LIMIT - used).coerceIn(0, DAILY_LIMIT)
    }

    /**
     * Seçili arkadaşlara öneriyi gönderir. Dönen değer sunucunun bildirdiği
     * kalan hak; kota dolduysa [RecommendationQuotaException] fırlatır.
     */
    suspend fun send(
        targetUids: List<String>,
        mediaId: Int,
        mediaType: String,
        mediaTitle: String,
        posterUrl: String? = null,
        note: String,
    ): Result<Unit> = runCatching {
        require(targetUids.isNotEmpty()) { "En az bir arkadaş seçmelisin" }
        val idToken = auth.currentUser?.getIdToken(false)?.await()?.token
            ?: error("Oturum doğrulanamadı, tekrar giriş yapın")

        try {
            SupabasePushClient.notifyRecommendation(
                idToken = idToken,
                targetUids = targetUids,
                mediaId = mediaId,
                mediaType = mediaType,
                mediaTitle = mediaTitle,
                note = note,
            )

            runCatching {
                val now = System.currentTimeMillis()
                val actId = "rec_${mediaType}_${mediaId}_${now}"
                firestore.collection("users").document(requireUid())
                    .collection("activities").document(actId).set(
                        hashMapOf(
                            "id" to actId,
                            "type" to "RECOMMENDATION",
                            "mediaId" to mediaId,
                            "mediaTitle" to mediaTitle,
                            "mediaType" to mediaType,
                            "posterUrl" to posterUrl,
                            "timestamp" to now,
                            "note" to note,
                        ),
                    ).await()
            }
        } catch (e: SupabasePushException) {
            if (e.code == 429) throw RecommendationQuotaException() else throw e
        }
    }

    /**
     * Kotanın günü, Edge Function'daki [quotaDayKey] ile AYNI olmalı — orası
     * da Europe/Istanbul kullanıyor. Cihazın saat dilimi kullanılsaydı yurt
     * dışındaki bir kullanıcı sunucununkinden farklı bir gün hesaplar ve
     * "kalan hak" yanlış görünürdü.
     */
    private fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("Europe/Istanbul") }
            .format(Date())

    companion object {
        /** Edge Function'daki DAILY_RECOMMENDATION_LIMIT ile aynı olmalı. */
        const val DAILY_LIMIT = 10
    }
}
