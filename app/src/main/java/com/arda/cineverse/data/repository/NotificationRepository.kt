package com.arda.cineverse.data.repository

import com.arda.cineverse.data.model.AppNotification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class NotificationRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    private fun notificationsCollection(uid: String) =
        firestore.collection("users").document(uid).collection("notifications")

    /**
     * Belirli bir kullanıcıya bildirim yazar. "targetUserId" bildirimi alacak
     * kişidir — örneğin bir yoruma yanıt verildiğinde, yanıtı yazan kişinin
     * cihazı doğrudan YORUM SAHİBİNİN bildirim kutusuna yazar (sunucu kodu
     * gerekmez, Firestore güvenlik kurallarının buna izin vermesi yeterli).
     */
    suspend fun createNotification(
        targetUserId: String,
        type: String,
        title: String,
        body: String,
        movieId: Int? = null,
    ): Result<Unit> = runCatching {
        val notification = hashMapOf(
            "type" to type,
            "title" to title,
            "body" to body,
            "movieId" to movieId,
            "isRead" to false,
            "createdAt" to System.currentTimeMillis(),
        )
        notificationsCollection(targetUserId).add(notification).await()
    }

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

    suspend fun getUnreadCount(): Result<Int> = runCatching {
        val uid = auth.currentUser?.uid ?: return@runCatching 0
        notificationsCollection(uid)
            .whereEqualTo("isRead", false)
            .get()
            .await()
            .documents
            .size
    }

    suspend fun markAsRead(notificationId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: return@runCatching
        notificationsCollection(uid).document(notificationId).update("isRead", true).await()
    }

    suspend fun markAllAsRead(): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: return@runCatching
        val unread = notificationsCollection(uid).whereEqualTo("isRead", false).get().await()
        unread.documents.forEach { doc -> doc.reference.update("isRead", true).await() }
    }
}