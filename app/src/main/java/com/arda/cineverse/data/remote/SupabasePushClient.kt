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
 * Edge Function'ın HTTP kodunu taşır. Çağıranların çoğu hatayı yutup logluyor
 * ama öneri gönderiminde 429 (günlük kota doldu) ile gerçek bir arıza farklı
 * mesajlar gerektiriyor.
 */
class SupabasePushException(val code: Int, message: String) : IOException(message)

/**
 * Firestore, Cloud Functions gibi Google'a özel bir tetikleyici mekanizmasına
 * sahip; Blaze'e geçmeden ücretsiz kalabilmek için bu proje onun yerine
 * Supabase Edge Functions kullanıyor (bkz. supabase/functions/friend-push).
 * Bu yüzden mimari FARKLI: otomatik tetiklenmiyor — asıl işlem (arkadaşlık
 * isteği, yorum yanıtı) Firestore'a yazıldıktan HEMEN SONRA bu istemci
 * fonksiyonu KENDİSİ, açıkça çağırıyor.
 *
 * Fonksiyonun uzaktaki adı hâlâ "friend-push" — tarihsel, ilk olarak sadece
 * arkadaşlık için yazılmıştı; deploy edilmiş URL'i kırmamak için değiştirilmedi.
 *
 * DİKKAT: Bu çağrı artık best-effort DEĞİL — bildirim belgesini de Edge
 * Function yazıyor (client'ın "notifications" koleksiyonuna yazma izni yok,
 * bkz. firestore.rules). Çağrı başarısız olursa karşı taraf ne push ne de
 * uygulama içi bildirim görür; asıl işlem (yorum/istek) yine de kaydedilmiş
 * olur, bu yüzden çağıranlar hatayı yutup logluyor.
 */
object SupabasePushClient {

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

    /**
     * Yanıtın KİME gideceği bilerek gönderilmiyor — Edge Function
     * [parentCommentId]'yi Firestore'dan okuyup yorumun sahibini kendisi
     * buluyor. Kendi yorumuna yanıt verme durumunu da sunucu ayrıca eliyor.
     */
    suspend fun notifyCommentReply(
        idToken: String,
        rootCollection: String,
        mediaId: Int,
        mediaTitle: String,
        parentCommentId: String,
        text: String,
    ) {
        send(
            idToken,
            mapOf(
                "action" to "comment_reply",
                "rootCollection" to rootCollection,
                "mediaId" to mediaId,
                "mediaTitle" to mediaTitle,
                "parentCommentId" to parentCommentId,
                "text" to text,
            ),
        )
    }

    /**
     * Film/dizi önerisi. Alıcı listesi gönderiliyor ama Edge Function her uid
     * için arkadaşlığı kendisi doğruluyor ve günlük kotayı da orada düşüyor —
     * bu yüzden çağrı, diğerlerinin aksine hatayı YUTMUYOR: kota dolduğunda ya
     * da gönderim başarısız olduğunda kullanıcının bunu görmesi gerekiyor.
     */
    suspend fun notifyRecommendation(
        idToken: String,
        targetUids: List<String>,
        mediaId: Int,
        mediaType: String,
        mediaTitle: String,
        note: String,
    ) {
        send(
            idToken,
            mapOf(
                "action" to "recommend",
                "targetUids" to targetUids,
                "mediaId" to mediaId,
                "mediaType" to mediaType,
                "mediaTitle" to mediaTitle,
                "note" to note,
            ),
        )
    }

    private suspend fun send(idToken: String, body: Map<String, Any?>) {
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
                    throw SupabasePushException(
                        response.code,
                        "Supabase friend-push çağrısı başarısız: ${response.code} $responseBody",
                    )
                }
                Log.d(TAG, "send: friend-push başarılı (HTTP ${response.code})")
            }
        }
    }
}
