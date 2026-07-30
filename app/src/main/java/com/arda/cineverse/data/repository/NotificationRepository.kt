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
     * yok, bu yüzden gerçek yetkilendirme firestore.rules'taki
     * `isValidNotification()` kuralında yapılıyor — buradaki kontroller sadece
     * erken/anlamlı bir hata vermek için, GÜVENLİK SINIRI DEĞİL).
     */
    suspend fun createNotification(
        targetUserId: String,
        type: String,
        title: String,
        body: String,
        movieId: Int? = null,
        tvId: Int? = null,
        mediaType: String? = null,
    ): Result<Unit> = runCatching {
        auth.currentUser?.uid ?: error("Bu işlem için giriş yapmalısınız")
        require(type in ALLOWED_NOTIFICATION_TYPES) { "Geçersiz bildirim tipi: $type" }
        require(title.isNotBlank() && title.length <= MAX_TITLE_LENGTH) { "Geçersiz bildirim başlığı" }
        require(body.isNotBlank() && body.length <= MAX_BODY_LENGTH) { "Geçersiz bildirim içeriği" }

        val notification = hashMapOf(
            "type" to type,
            "title" to title.take(MAX_TITLE_LENGTH),
            "body" to body.take(MAX_BODY_LENGTH),
            "movieId" to movieId,
            "tvId" to tvId,
            "mediaType" to mediaType,
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

    companion object {
        // firestore.rules'taki isValidNotification() ile senkron tutulmalı.
        private val ALLOWED_NOTIFICATION_TYPES = setOf("comment_reply")
        private const val MAX_TITLE_LENGTH = 150
        private const val MAX_BODY_LENGTH = 500
    }
}