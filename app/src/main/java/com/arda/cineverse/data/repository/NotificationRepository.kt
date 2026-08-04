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