package com.arda.cineverse.data.repository

import com.arda.cineverse.data.model.AppNotification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class NotificationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) {
    private fun notificationsCollection(uid: String) =
        firestore.collection("users").document(uid).collection("notifications")

    // Bildirim OLUŞTURMA burada yok — bilerek. Bildirim belgelerini artık
    // yalnızca supabase/functions/friend-push Edge Function'ı bir servis
    // hesabıyla yazıyor; istemcinin "notifications" koleksiyonuna create izni
    // tamamen kapalı (bkz. firestore.rules). Eskiden her istemci, herhangi bir
    // kullanıcının bildirim kutusuna dilediği başlık/metni yazabiliyordu.

    suspend fun getNotifications(): Result<List<AppNotification>> = runCatching {
        val uid = auth.currentUser?.uid ?: return@runCatching emptyList()
        notificationsCollection(uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .await()
            .documents
            .mapNotNull { doc -> doc.toObject(AppNotification::class.java)?.copy(id = doc.id) }
    }

    // count() aggregate sorgusu: belgelerin kendisini indirmeden sunucuda
    // sayar — eskiden tüm okunmamış belgeler indirilip .size alınıyordu
    // (hem gereksiz bant genişliği hem belge başına okuma maliyeti).
    suspend fun getUnreadCount(): Result<Int> = runCatching {
        val uid = auth.currentUser?.uid ?: return@runCatching 0
        notificationsCollection(uid)
            .whereEqualTo("isRead", false)
            .count()
            .get(AggregateSource.SERVER)
            .await()
            .count
            .toInt()
    }

    suspend fun markAsRead(notificationId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: return@runCatching
        notificationsCollection(uid).document(notificationId).update("isRead", true).await()
    }

    /**
     * Okunmamışları tek tek değil TEK batch'te işaretler — eskiden her belge
     * için ayrı bir gidiş-dönüş yapılıyordu, 50 bildirimde 50 ardışık yazma
     * demekti. Batch sınırı 500 olduğu için yine de parçalıyoruz.
     */
    suspend fun markAllAsRead(): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: return@runCatching
        val unread = notificationsCollection(uid).whereEqualTo("isRead", false).get().await()
        unread.documents.chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { doc -> batch.update(doc.reference, "isRead", true) }
            batch.commit().await()
        }
    }

    // bkz. MovieRepository.Companion.default() — aynı gerekçe (WorkManager
    // Worker'ları henüz Hilt'e taşınmadı, bkz. UpcomingCheckWorker).
    companion object {
        fun default(): NotificationRepository = NotificationRepository(
            firestore = FirebaseFirestore.getInstance(),
            auth = FirebaseAuth.getInstance(),
        )
    }
}