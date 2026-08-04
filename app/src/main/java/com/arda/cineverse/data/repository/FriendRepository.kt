package com.arda.cineverse.data.repository

import android.util.Log
import com.arda.cineverse.data.common.OfflineWriteException
import com.arda.cineverse.data.common.SyncResult
import com.arda.cineverse.data.connectivity.ConnectivityObserver
import com.arda.cineverse.data.local.dao.FriendDao
import com.arda.cineverse.data.local.entity.FriendEntity
import com.arda.cineverse.data.model.Comment
import com.arda.cineverse.data.model.Friend
import com.arda.cineverse.data.model.FriendActivity
import com.arda.cineverse.data.model.FriendActivityType
import com.arda.cineverse.data.model.FriendRequest
import com.arda.cineverse.data.model.FriendSearchResult
import com.arda.cineverse.data.model.SavedMovie
import com.arda.cineverse.data.remote.SupabasePushClient
import com.arda.cineverse.di.AppGraph
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val TAG = "CineVersePush"

/** Kullanıcı adı alınmışsa fırlatılır (kayıt/isim değişikliği sırasında). */
class UsernameTakenException : Exception("Bu kullanıcı adı zaten alınmış")

/**
 * Arkadaşlık: kullanıcı adıyla arama, istek gönder/kabul/red, arkadaş listesi.
 * Favoriler/İzleme Listesi'ndeki (UserListRepository) aynı desen: Firestore
 * kaynak, Room ("friends") SALT-OKUNUR bir çevrimdışı ayna.
 *
 * Push bildirimi: arkadaşlık isteği/kabulü sonrası Supabase Edge Function'ı
 * (bkz. supabase/functions/friend-push, [SupabasePushClient]) HTTP ile
 * çağrılır — Firestore'un Cloud Functions gibi otomatik bir tetikleyicisi
 * Supabase'e görünmediği için bu çağrı burada AÇIKÇA yapılıyor. Bu çağrı
 * başarısız olsa bile asıl işlem (Firestore yazımı) zaten tamamlanmıştır,
 * bu yüzden sadece push'un başarısızlığı sessizce yutulur (runCatching).
 */
class FriendRepository @Inject constructor(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val friendDao: FriendDao = AppGraph.friendDao,
    private val connectivityObserver: ConnectivityObserver = AppGraph.connectivityObserver,
) {
    private fun requireUid(): String = auth.currentUser?.uid ?: error("Giriş yapmalısınız")

    private suspend fun currentIdToken(): String? =
        runCatching { auth.currentUser?.getIdToken(false)?.await()?.token }
            .onFailure { Log.e(TAG, "currentIdToken: ID token alınamadı", it) }
            .getOrNull()

    private fun userDoc(uid: String) = firestore.collection("users").document(uid)
    private fun usernameDoc(username: String) = firestore.collection("usernames").document(username)
    private fun friendRequestsCollection(uid: String) = userDoc(uid).collection("friendRequests")
    private fun friendsCollection(uid: String) = userDoc(uid).collection("friends")

    // --- Kullanıcı adı ---------------------------------------------------

    suspend fun checkUsernameAvailable(username: String): Result<Boolean> = runCatching {
        require(username.matches(USERNAME_REGEX)) { "Geçersiz kullanıcı adı" }
        !usernameDoc(username).get().await().exists()
    }

    /**
     * Mevcut kullanıcının adını (varsa) yeni bir kullanıcı adıyla değiştirir.
     * Tek bir transaction içinde: yeni ad boşsa al, users/{uid}.username VE
     * "fullName" güncelle (ayrı bir ad-soyad alanı yok — kimlik tamamen
     * kullanıcı adı, bkz. AuthViewModel.register), eski adın claim belgesini
     * sil (üzerine yazma yok, her zaman sil+yeniden-oluştur).
     */
    suspend fun changeUsername(newUsername: String): Result<Unit> {
        if (!connectivityObserver.isCurrentlyOnline()) return Result.failure(OfflineWriteException())
        require(newUsername.matches(USERNAME_REGEX)) { "Geçersiz kullanıcı adı" }
        val uid = requireUid()
        return runCatching {
            firestore.runTransaction { txn ->
                val newUsernameRef = usernameDoc(newUsername)
                val userRef = userDoc(uid)
                val newUsernameSnapshot = txn.get(newUsernameRef)
                val userSnapshot = txn.get(userRef)
                if (newUsernameSnapshot.exists()) throw UsernameTakenException()

                val oldUsername = userSnapshot.getString("username")
                txn.set(newUsernameRef, mapOf("uid" to uid))
                txn.update(userRef, mapOf("username" to newUsername, "fullName" to newUsername))
                if (!oldUsername.isNullOrBlank() && oldUsername != newUsername) {
                    txn.delete(usernameDoc(oldUsername))
                }
            }.await()
            Unit
        }
    }

    // --- Arama -------------------------------------------------------------

    suspend fun searchUserByUsername(username: String): Result<FriendSearchResult?> = runCatching {
        val usernameSnapshot = usernameDoc(username).get().await()
        val targetUid = usernameSnapshot.getString("uid") ?: return@runCatching null
        if (targetUid == requireUid()) return@runCatching null // kendini arayamaz
        val profile = userDoc(targetUid).get().await()
        FriendSearchResult(
            uid = targetUid,
            username = username,
            fullName = profile.getString("fullName").orEmpty(),
            avatarId = profile.getString("avatarId").orEmpty(),
        )
    }

    // --- İstek gönder/kabul/red -------------------------------------------

    /**
     * [target]'a arkadaşlık isteği gönderir. Eğer [target] zaten bana istek
     * göndermişse (çapraz istek), yeni istek atmak yerine doğrudan o isteği
     * kabul eder.
     */
    suspend fun sendFriendRequest(target: FriendSearchResult): Result<Unit> {
        if (!connectivityObserver.isCurrentlyOnline()) return Result.failure(OfflineWriteException())
        val uid = requireUid()
        return runCatching {
            val crossedRequest = friendRequestsCollection(uid).document(target.uid).get().await()
            if (crossedRequest.exists()) {
                val request = crossedRequest.toFriendRequest() ?: error("Geçersiz istek verisi")
                acceptFriendRequest(request).getOrThrow()
                return@runCatching
            }

            val myProfile = userDoc(uid).get().await()
            val fromUsername = myProfile.getString("username").orEmpty()
            val fromFullName = myProfile.getString("fullName").orEmpty()
            val fromAvatarId = myProfile.getString("avatarId").orEmpty()
            val data = hashMapOf(
                "fromUid" to uid,
                "fromUsername" to fromUsername,
                "fromFullName" to fromFullName,
                "fromAvatarId" to fromAvatarId,
                "status" to "pending",
                "createdAt" to System.currentTimeMillis(),
            )
            try {
                friendRequestsCollection(target.uid).document(uid).set(data).await()
            } catch (e: FirebaseFirestoreException) {
                if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    error("Bu kullanıcıya zaten bir istek gönderilmiş")
                }
                throw e
            }

            val idToken = currentIdToken()
            if (idToken == null) {
                Log.w(TAG, "sendFriendRequest: ID token yok, Supabase push çağrısı ATLANIYOR")
            } else {
                runCatching {
                    SupabasePushClient.notifyFriendRequest(
                        idToken = idToken,
                        targetUid = target.uid,
                        fromUid = uid,
                        fromUsername = fromUsername,
                        fromFullName = fromFullName,
                        fromAvatarId = fromAvatarId,
                    )
                }.onFailure { Log.e(TAG, "sendFriendRequest: Supabase push çağrısı BAŞARISIZ", it) }
            }
            Unit
        }
    }

    // Not: ayrı bir "status == pending" filtresi eklemiyoruz — bilerek. Kabul
    // edilen bir istek [acceptFriendRequest] içinde belgeden SİLİNİYOR (bkz.
    // aşağısı), bu yüzden bu koleksiyonda kalan her belge zaten pending'dir.
    // Bu, equality-filter + orderBy(farklı alan) kombinasyonunun gerektirdiği
    // composite Firestore index'inden kaçınmamızı sağlıyor.
    suspend fun getIncomingRequests(): Result<List<FriendRequest>> = runCatching {
        val uid = requireUid()
        friendRequestsCollection(uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get().await()
            .documents.mapNotNull { it.toFriendRequest() }
    }

    /**
     * İsteği kabul eder. Sadece kendi friendRequests/{fromUid} belgesindeki
     * "status" alanını "accepted" yapar — friends/ belgelerinin HER İKİ
     * tarafta da yazılması, kabul bildirimi ve isteğin silinmesi artık
     * supabase/functions/friend-push Edge Function'ının işi (bir servis
     * hesabıyla, Admin SDK gibi rules'ı bypass ederek; client'ın "friends/"
     * koleksiyonuna artık create izni yok, bkz. firestore.rules). Bu yüzden
     * kabul ettikten hemen sonra [syncFriends] çağırmak yeni arkadaşı garanti
     * göstermeyebilir (Edge Function çağrısı asenkron/best-effort) — bir
     * sonraki [refresh] onu yakalar.
     */
    suspend fun acceptFriendRequest(request: FriendRequest): Result<Unit> {
        if (!connectivityObserver.isCurrentlyOnline()) return Result.failure(OfflineWriteException())
        val uid = requireUid()
        return runCatching {
            friendRequestsCollection(uid).document(request.fromUid).update("status", "accepted").await()

            val idToken = currentIdToken()
            if (idToken == null) {
                Log.w(TAG, "acceptFriendRequest: ID token yok, Supabase push çağrısı ATLANIYOR")
            } else {
                runCatching {
                    SupabasePushClient.notifyFriendRequestAccepted(
                        idToken = idToken,
                        uid = uid,
                        fromUid = request.fromUid,
                    )
                }.onFailure { Log.e(TAG, "acceptFriendRequest: Supabase push çağrısı BAŞARISIZ", it) }
            }
            Unit
        }.onSuccess { syncFriends() }
    }

    suspend fun declineFriendRequest(fromUid: String): Result<Unit> {
        if (!connectivityObserver.isCurrentlyOnline()) return Result.failure(OfflineWriteException())
        val uid = requireUid()
        return runCatching {
            friendRequestsCollection(uid).document(fromUid).delete().await()
            Unit
        }
    }

    // --- Arkadaş listesi -----------------------------------------------------

    fun observeFriends(): Flow<List<Friend>> =
        friendDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun removeFriend(friendUid: String): Result<Unit> {
        if (!connectivityObserver.isCurrentlyOnline()) return Result.failure(OfflineWriteException())
        val uid = requireUid()
        return runCatching {
            firestore.batch()
                .delete(friendsCollection(uid).document(friendUid))
                .delete(friendsCollection(friendUid).document(uid))
                .commit()
                .await()
            Unit
        }.onSuccess { syncFriends() }
    }

    /** Çevrimiçiyken çağrılır: Firestore'dan çekip Room'u tamamen değiştirir. */
    suspend fun syncFriends(): SyncResult {
        if (!connectivityObserver.isCurrentlyOnline()) return SyncResult.Offline
        return runCatching {
            val uid = requireUid()
            val friends = friendsCollection(uid).get().await()
                .documents.mapNotNull { it.toObject(Friend::class.java) }
            friendDao.replaceAll(friends.map { it.toEntity() })
        }.fold(onSuccess = { SyncResult.Success }, onFailure = { SyncResult.Error(it) })
    }

    /**
     * Seçilen arkadaşın son favoriye alma, izleme listesine ekleme ve yorum yapma gibi
     * son 20 etkileşimini getirir.
     */
    suspend fun getFriendRecentActivities(friendUid: String): Result<List<FriendActivity>> = runCatching {
        // 1. Doğrudan 'activities' koleksiyonunu sorgulayalım (En Hızlı & Kesin Yöntem)
        val directActivities = runCatching {
            userDoc(friendUid).collection("activities")
                .get().await()
                .documents.mapNotNull { doc ->
                    val typeStr = doc.getString("type") ?: return@mapNotNull null
                    val type = runCatching { FriendActivityType.valueOf(typeStr) }.getOrNull() ?: return@mapNotNull null
                    val mediaId = doc.getLong("mediaId")?.toInt() ?: 0
                    val mediaTitle = doc.getString("mediaTitle") ?: ""
                    val mediaType = doc.getString("mediaType") ?: "movie"
                    val posterUrl = doc.getString("posterUrl")
                    val timestamp = doc.getLong("timestamp") ?: 0L
                    val rating = doc.getDouble("rating")
                    val commentText = doc.getString("commentText")
                    val note = doc.getString("note")

                    FriendActivity(
                        id = doc.id,
                        type = type,
                        mediaId = mediaId,
                        mediaTitle = mediaTitle,
                        mediaType = mediaType,
                        posterUrl = posterUrl,
                        timestamp = timestamp,
                        rating = rating,
                        commentText = commentText,
                        note = note,
                    )
                }
        }.onFailure { Log.e(TAG, "getFriendRecentActivities: Direk aktiviteler okunamadı ($friendUid)", it) }
            .getOrDefault(emptyList())

        if (directActivities.isNotEmpty()) {
            val resolvedActivities = directActivities.map { activity ->
                if (activity.posterUrl.isNullOrBlank() && activity.mediaId > 0) {
                    val ratingsCollection = if (activity.mediaType == "tv") "tv_show_ratings" else "movie_ratings"
                    val ratingDoc = runCatching {
                        firestore.collection(ratingsCollection).document(activity.mediaId.toString()).get().await()
                    }.getOrNull()
                    val poster = ratingDoc?.getString("posterUrl")
                    if (!poster.isNullOrBlank()) {
                        activity.copy(posterUrl = poster)
                    } else activity
                } else activity
            }
            return@runCatching resolvedActivities.sortedByDescending { it.timestamp }.take(20)
        }

        // 2. Geriye Dönük Uyumluluk (Legacy Fallback): Eski veriler için favoriler, watchlist & yorumları tara
        val rawActivities = mutableListOf<FriendActivity>()

        coroutineScope {
            // 1. Favoriler
            val favTask = async {
                runCatching {
                    userDoc(friendUid).collection("favorites")
                        .get().await()
                        .documents.mapNotNull { it.toObject(SavedMovie::class.java) }
                }.onFailure { Log.e(TAG, "getFriendRecentActivities: Favoriler okunamadı ($friendUid)", it) }
                    .getOrDefault(emptyList())
            }

            // 2. İzleme Listesi
            val watchTask = async {
                runCatching {
                    userDoc(friendUid).collection("watchlist")
                        .get().await()
                        .documents.mapNotNull { it.toObject(SavedMovie::class.java) }
                }.onFailure { Log.e(TAG, "getFriendRecentActivities: İzleme listesi okunamadı ($friendUid)", it) }
                    .getOrDefault(emptyList())
            }

            // 3. Yorumlar / Puanlar
            val commentsTask = async {
                runCatching {
                    firestore.collectionGroup("comments")
                        .whereEqualTo("userId", friendUid)
                        .limit(20)
                        .get().await()
                        .documents.mapNotNull { doc ->
                            val comment = doc.toObject(Comment::class.java)?.copy(id = doc.id) ?: return@mapNotNull null
                            val path = doc.reference.path
                            val mediaType = if (path.contains("tv_shows")) "tv" else "movie"
                            Pair(comment, mediaType)
                        }
                }.onFailure { Log.e(TAG, "getFriendRecentActivities: Yorumlar okunamadı ($friendUid)", it) }
                    .getOrDefault(emptyList())
            }

            val favs = favTask.await()
            val watchlist = watchTask.await()
            val commentsWithMediaType = commentsTask.await()

            // Favorilerden başlık ve poster haritası hazırlayalım
            val mediaTitleMap = mutableMapOf<String, String>()
            val mediaPosterMap = mutableMapOf<String, String?>()

            favs.forEach { item ->
                val key = "${item.mediaType}_${item.id}"
                mediaTitleMap[key] = item.title
                mediaPosterMap[key] = item.posterUrl

                rawActivities.add(
                    FriendActivity(
                        id = "fav_${item.mediaType}_${item.id}",
                        type = FriendActivityType.FAVORITE,
                        mediaId = item.id,
                        mediaTitle = item.title,
                        mediaType = item.mediaType,
                        posterUrl = item.posterUrl,
                        timestamp = if (item.addedAt > 0) item.addedAt else System.currentTimeMillis(),
                        rating = item.rating,
                    ),
                )
            }

            watchlist.forEach { item ->
                val key = "${item.mediaType}_${item.id}"
                if (!mediaTitleMap.containsKey(key)) {
                    mediaTitleMap[key] = item.title
                    mediaPosterMap[key] = item.posterUrl
                }

                rawActivities.add(
                    FriendActivity(
                        id = "watch_${item.mediaType}_${item.id}",
                        type = FriendActivityType.WATCHLIST,
                        mediaId = item.id,
                        mediaTitle = item.title,
                        mediaType = item.mediaType,
                        posterUrl = item.posterUrl,
                        timestamp = if (item.addedAt > 0) item.addedAt else System.currentTimeMillis(),
                        rating = item.rating,
                    ),
                )
            }

            commentsWithMediaType.forEach { (comment, mediaType) ->
                val key = "${mediaType}_${comment.movieId}"
                var title = mediaTitleMap[key]
                var poster = mediaPosterMap[key]

                if (title == null) {
                    val ratingsCollection = if (mediaType == "tv") "tv_show_ratings" else "movie_ratings"
                    val ratingDoc = runCatching {
                        firestore.collection(ratingsCollection).document(comment.movieId.toString()).get().await()
                    }.getOrNull()

                    if (ratingDoc != null && ratingDoc.exists()) {
                        title = ratingDoc.getString("title")
                        poster = ratingDoc.getString("posterUrl")
                    }
                }

                val finalTitle = title ?: if (mediaType == "tv") "Dizi #${comment.movieId}" else "Film #${comment.movieId}"

                rawActivities.add(
                    FriendActivity(
                        id = "comment_${comment.id}",
                        type = FriendActivityType.COMMENT,
                        mediaId = comment.movieId,
                        mediaTitle = finalTitle,
                        mediaType = mediaType,
                        posterUrl = poster,
                        timestamp = if (comment.createdAt > 0) comment.createdAt else System.currentTimeMillis(),
                        rating = comment.rating.toDouble(),
                        commentText = comment.text,
                    ),
                )
            }
        }

        rawActivities.sortedByDescending { it.timestamp }.take(20)
    }

    companion object {
        val USERNAME_REGEX = Regex("^[a-z0-9_]{3,20}$")
    }
}

private fun DocumentSnapshot.toFriendRequest(): FriendRequest? =
    toObject(FriendRequest::class.java)

private fun Friend.toEntity() = FriendEntity(
    friendUid = friendUid,
    username = username,
    fullName = fullName,
    avatarId = avatarId,
    since = since,
)

private fun FriendEntity.toDomain() = Friend(
    friendUid = friendUid,
    username = username,
    fullName = fullName,
    avatarId = avatarId,
    since = since,
)
