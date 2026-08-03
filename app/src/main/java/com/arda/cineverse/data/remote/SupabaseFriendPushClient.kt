package com.arda.cineverse.data.remote

import com.arda.cineverse.BuildConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

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
        if (BuildConfig.SUPABASE_URL.isBlank()) return // Supabase henüz kurulmadıysa sessizce atla
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("${BuildConfig.SUPABASE_URL}/functions/v1/friend-push")
                .addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
                .addHeader("X-Firebase-Id-Token", idToken)
                .post(gson.toJson(body).toRequestBody(jsonMediaType))
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Supabase friend-push çağrısı başarısız: ${response.code}")
                }
            }
        }
    }
}
