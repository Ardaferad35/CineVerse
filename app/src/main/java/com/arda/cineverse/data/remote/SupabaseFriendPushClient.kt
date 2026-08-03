package com.arda.cineverse.data.remote

import android.util.Log
import com.arda.cineverse.BuildConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

private const val TAG = "CineVersePush"

/**
 * Firestore, Cloud Functions gibi Google'a özel bir tetikleyici mekanizmasına
 * sahip; Blaze'e geçmeden ücretsiz kalabilmek için bu proje onun yerine
 * Supabase Edge Functions kullanıyor (bkz. supabase/functions/friend-push).
 * Bu yüzden mimari FARKLI: otomatik tetiklenmiyor — Firestore'a yazdıktan
 * HEMEN SONRA bu istemci fonksiyonu KENDİSİ, açıkça çağırıyor (bkz.
 * FriendRepository.sendFriendRequest / acceptFriendRequest). Çağrı
 * başarısız olsa bile ana işlem (Firestore yazımı) zaten tamamlanmış olur —
 * bu sadece push bildiriminin gönderilmesini sağlar, başarısızlığı sessizce
 * yutulur (bkz. çağıran taraftaki runCatching).
 */
object SupabaseFriendPushClient {

    private val client = OkHttpClient()
    private val gson = Gson()
    private val jsonMediaType = "application/json".toMediaType()

    suspend fun notifyFriendRequest(
        idToken: String,
        targetUid: String,
        fromUid: String,
        fromUsername: String,
        fromFullName: String,
        fromAvatarId: String,
    ) {
        send(
            idToken,
            mapOf(
                "action" to "request",
                "targetUid" to targetUid,
                "fromUid" to fromUid,
                "fromUsername" to fromUsername,
                "fromFullName" to fromFullName,
                "fromAvatarId" to fromAvatarId,
            ),
        )
    }

    suspend fun notifyFriendRequestAccepted(idToken: String, uid: String, fromUid: String) {
        send(idToken, mapOf("action" to "accept", "uid" to uid, "fromUid" to fromUid))
    }

    private suspend fun send(idToken: String, body: Map<String, String>) {
        if (BuildConfig.SUPABASE_URL.isBlank()) {
            // BuildConfig.SUPABASE_URL boşsa, ya local.properties'e SUPABASE_URL
            // hiç eklenmemiş ya da eklendikten SONRA uygulama temiz bir şekilde
            // yeniden derlenip cihaza yüklenmemiştir (BuildConfig derleme
            // zamanında sabitlenir, local.properties'i sonradan değiştirmek
            // eski APK'yı GÜNCELLEMEZ).
            Log.w(TAG, "send: BuildConfig.SUPABASE_URL boş — local.properties dolu mu VE uygulama o değişiklikten sonra yeniden derlenip yüklendi mi kontrol edin. Çağrı atlanıyor: $body")
            return
        }
        Log.d(TAG, "send: friend-push çağrılıyor, body=$body")
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("${BuildConfig.SUPABASE_URL}/functions/v1/friend-push")
                .addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
                .addHeader("X-Firebase-Id-Token", idToken)
                .post(gson.toJson(body).toRequestBody(jsonMediaType))
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val responseBody = response.body?.string().orEmpty()
                    Log.e(TAG, "send: friend-push başarısız — HTTP ${response.code}, body=$responseBody")
                    throw IOException("Supabase friend-push çağrısı başarısız: ${response.code} $responseBody")
                }
                Log.d(TAG, "send: friend-push başarılı (HTTP ${response.code})")
            }
        }
    }
}
