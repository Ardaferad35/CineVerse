package com.arda.cineverse.notifications

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * FCM push bildirimlerini alır. Sunucu tarafı (functions/src/index.ts,
 * onNotificationCreated) HER ZAMAN data-only payload gönderir ("notification"
 * bloğu YOK) — aksi halde uygulama arka plandayken Android bildirimi kendi
 * kendine gösterirdi ve bu metot hiç çağrılmaz, deep-link verisi (route)
 * sessizce kaybolurdu.
 */
@Suppress("DEPRECATION") // onNewToken/.token hâlâ Firebase'in resmi, çalışan FCM API'si — Kotlin derleyicisinin altta yatan Java uyarısı, işlevsel bir sorun değil.
class CineVerseMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch { runCatching { registerToken(token) } }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val title = data["title"]?.takeIf { it.isNotBlank() } ?: return
        val body = data["body"].orEmpty()
        val route = data["route"]
        val notificationId = data["notificationId"]?.hashCode() ?: System.currentTimeMillis().toInt()
        LocalNotificationHelper.show(applicationContext, notificationId, title, body, route)
    }

    companion object {
        /**
         * Sadece token ROTASYONUNDA çağrılan [onNewToken]'a güvenmek yetmez —
         * uygulama ilk kurulumda (login öncesi) ürettiği token'ı bir daha hiç
         * bildirmeyebilir. Bu yüzden login/register başarılı olduğunda da
         * AKTİF olarak mevcut token çekilip yazılıyor (bkz. AuthViewModel).
         */
        suspend fun registerCurrentToken() {
            val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull() ?: return
            registerToken(token)
        }

        private suspend fun registerToken(token: String) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("fcmTokens").document(token)
                .set(mapOf("createdAt" to System.currentTimeMillis(), "platform" to "android"))
                .await()
        }

        /** Çıkış yapılmadan ÖNCE çağrılmalı — silme isOwner gerektirir, signOut() sonrası kimlik geçersiz olur. */
        suspend fun deleteCurrentToken() {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull() ?: return
            runCatching {
                FirebaseFirestore.getInstance()
                    .collection("users").document(uid)
                    .collection("fcmTokens").document(token)
                    .delete()
                    .await()
            }
        }
    }
}
