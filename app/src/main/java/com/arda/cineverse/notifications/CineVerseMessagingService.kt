package com.arda.cineverse.notifications

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "CineVersePush"

/**
 * FCM push bildirimlerini alır. Sunucu tarafı (supabase/functions/friend-push)
 * HER ZAMAN data-only payload gönderir ("notification" bloğu YOK) — aksi
 * halde uygulama arka plandayken Android bildirimi kendi kendine gösterirdi
 * ve bu metot hiç çağrılmaz, deep-link verisi (route) sessizce kaybolurdu.
 */
@Suppress("DEPRECATION") // onNewToken/.token hâlâ Firebase'in resmi, çalışan FCM API'si — Kotlin derleyicisinin altta yatan Java uyarısı, işlevsel bir sorun değil.
class CineVerseMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "onNewToken: yeni FCM token alındı, Firestore'a yazılıyor")
        scope.launch {
            runCatching { registerToken(token) }
                .onSuccess { Log.d(TAG, "onNewToken: fcmTokens'a yazma başarılı") }
                .onFailure { Log.e(TAG, "onNewToken: fcmTokens'a yazma BAŞARISIZ", it) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "onMessageReceived: veri=${message.data}")
        val data = message.data
        val title = data["title"]?.takeIf { it.isNotBlank() }
        if (title == null) {
            Log.w(TAG, "onMessageReceived: 'title' alanı yok/boş, bildirim gösterilmiyor")
            return
        }
        val body = data["body"].orEmpty()
        val route = data["route"]
        val notificationId = data["notificationId"]?.hashCode() ?: System.currentTimeMillis().toInt()
        LocalNotificationHelper.show(applicationContext, notificationId, title, body, route)
        Log.d(TAG, "onMessageReceived: bildirim gösterildi (id=$notificationId, route=$route)")
    }

    companion object {
        /**
         * Sadece token ROTASYONUNDA çağrılan [onNewToken]'a güvenmek yetmez —
         * uygulama ilk kurulumda (login öncesi) ürettiği token'ı bir daha hiç
         * bildirmeyebilir. Bu yüzden login/register başarılı olduğunda da
         * AKTİF olarak mevcut token çekilip yazılıyor (bkz. AuthViewModel).
         */
        suspend fun registerCurrentToken() {
            var token: String? = null
            var lastError: Throwable? = null

            // Google Play Services bazen uygulama açılışında paket görünürlüğü veya
            // bağlama gecikmesi nedeniyle "Unknown calling package" hatası verebilir.
            // Küçük bir bekleme ve retry mekanizması bu geçici sorunu çözer.
            for (attempt in 1..3) {
                try {
                    // "No service published for: persistent_data_block" gibi sistem hatalarını yakala
                    val messaging = runCatching { FirebaseMessaging.getInstance() }.getOrNull()
                    if (messaging == null) {
                        Log.e(TAG, "registerCurrentToken: FirebaseMessaging başlatılamadı")
                        return
                    }
                    token = messaging.token.await()
                    if (token != null) break
                } catch (e: Exception) {
                    lastError = e
                    Log.w(TAG, "registerCurrentToken: deneme $attempt başarısız", e)
                    kotlinx.coroutines.delay(attempt * 1000L)
                }
            }

            val finalToken = token
            if (finalToken == null) {
                Log.e(TAG, "registerCurrentToken: FCM token alınamadı (Google Play Services hatası)", lastError)
                return
            }

            runCatching { registerToken(finalToken) }
                .onSuccess { Log.d(TAG, "registerCurrentToken: fcmTokens'a yazma başarılı") }
                .onFailure { Log.e(TAG, "registerCurrentToken: fcmTokens'a yazma BAŞARISIZ", it) }
        }

        private suspend fun registerToken(token: String) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                Log.w(TAG, "registerToken: giriş yapmış kullanıcı yok, atlanıyor")
                return
            }
            FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("fcmTokens").document(token)
                .set(mapOf("createdAt" to System.currentTimeMillis(), "platform" to "android"))
                .await()
        }

        /** Çıkış yapılmadan ÖNCE çağrılmalı — silme isOwner gerektirir, signOut() sonrası kimlik geçersiz olur. */
        suspend fun deleteCurrentToken() {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val token = runCatching {
                val messaging = FirebaseMessaging.getInstance()
                messaging.token.await()
            }.getOrNull() ?: return
            runCatching {
                FirebaseFirestore.getInstance()
                    .collection("users").document(uid)
                    .collection("fcmTokens").document(token)
                    .delete()
                    .await()
            }.onFailure { Log.e(TAG, "deleteCurrentToken: token silinemedi", it) }
        }
    }
}
