package com.arda.cineverse.data.repository

import android.util.Log
import com.arda.cineverse.data.model.Comment
import com.arda.cineverse.data.model.Movie
import com.arda.cineverse.data.remote.SupabasePushClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlin.math.round

private const val TAG = "CineVersePush"

class CommentRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val commentsRootCollection: String = "movies",
    private val ratingsRootCollection: String = "movie_ratings",
) {
    private fun commentsCollection(movieId: Int) =
        firestore.collection(commentsRootCollection).document(movieId.toString()).collection("comments")

    private fun ratingsCollection() = firestore.collection(ratingsRootCollection)

    fun currentUserId(): String? = auth.currentUser?.uid

    suspend fun getComments(movieId: Int): Result<List<Comment>> = runCatching {
        val comments = commentsCollection(movieId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()
            .documents
            .mapNotNull { doc -> doc.toObject(Comment::class.java)?.copy(id = doc.id) }

        // Her yorum sahibinin O ANKİ güncel avatarını canlı olarak çekiyoruz
        // (yorumun içine gömmüyoruz) — böylece kullanıcı avatarını
        // değiştirdiğinde geçmiş yorumlarında da otomatik yansır.
        //
        // Kullanıcı başına AYRI bir belge okuması yapmıyoruz: 20 farklı kişinin
        // yorumladığı bir film 20 ardışık gidiş-dönüş demekti. Bunun yerine
        // whereIn ile toplu çekiyoruz; "in" filtresinin 30 değer sınırı olduğu
        // için gruplara bölüp o grupları paralel çalıştırıyoruz.
        val userIds = comments.map { it.userId }.filter { it.isNotBlank() }.distinct()
        val avatarByUserId = coroutineScope {
            userIds.chunked(30)
                .map { chunk ->
                    async {
                        runCatching {
                            firestore.collection("users")
                                .whereIn(FieldPath.documentId(), chunk)
                                .get()
                                .await()
                                .documents
                                .associate { doc -> doc.id to doc.getString("avatarId").orEmpty() }
                        }.getOrElse { emptyMap() }
                    }
                }
                .awaitAll()
                .fold(emptyMap<String, String>()) { acc, chunkResult -> acc + chunkResult }
        }

        comments.map { comment ->
            val avatarId = avatarByUserId[comment.userId]?.takeIf { it.isNotBlank() } ?: "default"
            comment.copy(avatarId = avatarId)
        }
    }

    /**
     * Bir yoruma yanıt yazar. Asıl yorumlardan farklı olarak film puanını
     * ETKİLEMEZ (movie_ratings'e dahil edilmez, kullanıcının kendi puan
     * istatistiğini de değiştirmez) — sadece bir sohbet cevabıdır.
     *
     * Yanıt, o yorumun sahibine (kendi yorumunuza yanıt vermiyorsanız) hem
     * uygulama içi bildirim hem FCM push gönderir. İkisini de Supabase Edge
     * Function yazıyor/gönderiyor (bkz. supabase/functions/friend-push,
     * [SupabasePushClient]) — istemcinin "notifications" koleksiyonuna
     * doğrudan yazma izni YOK (bkz. firestore.rules), çünkü eskiden herkes
     * herkesin bildirim kutusuna dilediği metni yazabiliyordu. Bu çağrı
     * başarısız olsa bile yanıtın kendisi zaten kaydedilmiştir, bu yüzden
     * hata yutulup loglanıyor.
     */
    suspend fun addReply(
        movieId: Int,
        parentCommentId: String,
        parentCommentUserId: String,
        text: String,
        movieTitle: String,
    ): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("Yanıt yazmak için giriş yapmalısınız")

        val userDoc = firestore.collection("users").document(user.uid).get().await()
        val displayName = userDoc.getString("fullName")
            ?: user.email?.substringBefore("@")
            ?: "Kullanıcı"

        val reply = hashMapOf(
            "movieId" to movieId,
            "userId" to user.uid,
            "userName" to displayName,
            "text" to text,
            "rating" to 0,
            "isSpoiler" to false,
            "createdAt" to System.currentTimeMillis(),
            "editedAt" to null,
            "replyToCommentId" to parentCommentId,
        )
        commentsCollection(movieId).add(reply).await()

        // Kendi yorumunuza yanıt veriyorsanız kendinize bildirim gitmesin.
        // (Sunucu bunu ayrıca kendi de eliyor — burada sadece gereksiz bir
        // HTTP çağrısından kaçınıyoruz.)
        if (parentCommentUserId == user.uid) return@runCatching

        val idToken = runCatching { user.getIdToken(false).await()?.token }
            .onFailure { Log.e(TAG, "addReply: ID token alınamadı", it) }
            .getOrNull()
        if (idToken == null) {
            Log.w(TAG, "addReply: ID token yok, yorum yanıtı bildirimi ATLANIYOR")
            return@runCatching
        }
        runCatching {
            SupabasePushClient.notifyCommentReply(
                idToken = idToken,
                rootCollection = commentsRootCollection,
                mediaId = movieId,
                mediaTitle = movieTitle,
                parentCommentId = parentCommentId,
                text = text,
            )
        }.onFailure { Log.e(TAG, "addReply: yorum yanıtı bildirimi BAŞARISIZ", it) }
    }

    suspend fun addComment(
        movieId: Int,
        text: String,
        rating: Int,
        isSpoiler: Boolean,
        movieTitle: String,
        moviePosterUrl: String?,
        movieYear: Int?,
        movieGenreIds: List<Int> = emptyList(),
    ): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("Yorum yapmak için giriş yapmalısınız")

        val userDoc = firestore.collection("users").document(user.uid).get().await()
        val displayName = userDoc.getString("fullName")
            ?: user.email?.substringBefore("@")
            ?: "Kullanıcı"

        val comment = hashMapOf(
            "movieId" to movieId,
            "userId" to user.uid,
            "userName" to displayName,
            "text" to text,
            "rating" to rating,
            "isSpoiler" to isSpoiler,
            "createdAt" to System.currentTimeMillis(),
            "editedAt" to null,
        )
        commentsCollection(movieId).add(comment).await()
        adjustUserRatingStats(user.uid, sumDelta = rating, countDelta = 1)
        recalculateAggregate(movieId, movieTitle, moviePosterUrl, movieYear, movieGenreIds)
    }

    suspend fun updateComment(
        movieId: Int,
        commentId: String,
        text: String,
        rating: Int,
        isSpoiler: Boolean,
        movieTitle: String,
        moviePosterUrl: String?,
        movieYear: Int?,
        movieGenreIds: List<Int> = emptyList(),
    ): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Bu işlem için giriş yapmalısınız")
        val docRef = commentsCollection(movieId).document(commentId)
        val existing = docRef.get().await()
        val userId = existing.getString("userId")
        // Sahiplik kontrolü: bu, Firestore güvenlik kurallarının yerini TUTMAZ
        // (kurallar zorunlu, bkz. firestore.rules) — burada erken ve net bir
        // hata vermek için tekrarlanıyor.
        if (userId != uid) error("Bu yorumu düzenleme yetkiniz yok")
        val oldRating = existing.getLong("rating")?.toInt() ?: rating

        val updates = mapOf(
            "text" to text,
            "rating" to rating,
            "isSpoiler" to isSpoiler,
            "editedAt" to System.currentTimeMillis(),
        )
        docRef.update(updates).await()

        if (oldRating != rating) {
            adjustUserRatingStats(uid, sumDelta = rating - oldRating, countDelta = 0)
        }
        recalculateAggregate(movieId, movieTitle, moviePosterUrl, movieYear, movieGenreIds)
    }

    suspend fun deleteComment(
        movieId: Int,
        commentId: String,
        movieTitle: String,
        moviePosterUrl: String?,
        movieYear: Int?,
        movieGenreIds: List<Int> = emptyList(),
    ): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Bu işlem için giriş yapmalısınız")
        val docRef = commentsCollection(movieId).document(commentId)
        val existing = docRef.get().await()
        val userId = existing.getString("userId")
        // Sahiplik kontrolü: bu, Firestore güvenlik kurallarının yerini TUTMAZ
        // (kurallar zorunlu, bkz. firestore.rules) — burada erken ve net bir
        // hata vermek için tekrarlanıyor.
        if (userId != uid) error("Bu yorumu silme yetkiniz yok")
        val oldRating = existing.getLong("rating")?.toInt()
        val isReply = existing.getString("replyToCommentId") != null

        docRef.delete().await()

        // Yanıtların hiçbir zaman kullanıcı puan istatistiğine dahil edilmediği
        // için (addReply bunu güncellemiyor), silinirken de dokunmuyoruz —
        // aksi halde sayaç gerçek dışı şekilde eksiye düşerdi.
        if (!isReply && oldRating != null) {
            adjustUserRatingStats(uid, sumDelta = -oldRating, countDelta = -1)
        }
        recalculateAggregate(movieId, movieTitle, moviePosterUrl, movieYear, movieGenreIds)
    }

    /**
     * Kullanıcının "users/{uid}" belgesindeki ratingSum/ratingCount alanlarını
     * atomik olarak (FieldValue.increment) günceller. Bu sayede Profil ekranı,
     * hiçbir arama/tarama (collectionGroup) sorgusuna ihtiyaç duymadan —
     * dolayısıyla hiçbir özel Firestore index'ine gerek kalmadan — kullanıcının
     * verdiği ortalama puanı anında hesaplayabiliyor.
     */
    private suspend fun adjustUserRatingStats(userId: String, sumDelta: Int, countDelta: Int) {
        runCatching {
            firestore.collection("users").document(userId).update(
                mapOf(
                    "ratingSum" to FieldValue.increment(sumDelta.toLong()),
                    "ratingCount" to FieldValue.increment(countDelta.toLong()),
                ),
            ).await()
        }
    }

    /**
     * Bir filmin tüm yorumlarını yeniden okuyup ortalama puanı hesaplar ve
     * "movie_ratings/{movieId}" belgesine (tür ID'leri dahil) yazar. Hiç yorum
     * kalmadıysa belgeyi tamamen siler.
     */
    private suspend fun recalculateAggregate(
        movieId: Int,
        movieTitle: String,
        moviePosterUrl: String?,
        movieYear: Int?,
        movieGenreIds: List<Int>,
    ) {
        val comments = commentsCollection(movieId).get().await()
            .documents.mapNotNull { it.toObject(Comment::class.java) }
            .filter { it.replyToCommentId == null } // yanıtlar film puanını etkilemez

        val ratingDoc = ratingsCollection().document(movieId.toString())
        if (comments.isEmpty()) {
            ratingDoc.delete().await()
        } else {
            val average = round(comments.map { it.rating }.average() * 10) / 10.0
            val data = hashMapOf(
                "movieId" to movieId,
                "title" to movieTitle,
                "posterUrl" to moviePosterUrl,
                "year" to movieYear,
                "averageRating" to average,
                "ratingCount" to comments.size,
                "genreIds" to movieGenreIds,
            )
            ratingDoc.set(data).await()
        }
    }

    /**
     * "Uygulama İçi Puan" sıralaması (genel Popüler Filmler ekranı için):
     * kullanıcılarımızın puanladığı tüm filmleri ortalama puana göre getirir.
     */
    suspend fun getTopRatedByAppUsers(page: Int = 1): Result<List<Movie>> = runCatching {
        if (page > 1) return@runCatching emptyList()

        ratingsCollection()
            .orderBy("averageRating", Query.Direction.DESCENDING)
            .limit(100)
            .get()
            .await()
            .documents
            .mapNotNull { doc -> docToMovie(doc) }
    }

    /**
     * "Uygulama İçi Puan" sıralaması (belirli bir tür/kategori için):
     * yalnızca o türe ait ve kullanıcılarımızca puanlanmış filmleri getirir.
     */
    suspend fun getTopRatedByAppUsersForGenre(genreId: Int, page: Int = 1): Result<List<Movie>> = runCatching {
        if (page > 1) return@runCatching emptyList()

        ratingsCollection()
            .whereArrayContains("genreIds", genreId)
            .orderBy("averageRating", Query.Direction.DESCENDING)
            .limit(100)
            .get()
            .await()
            .documents
            .mapNotNull { doc -> docToMovie(doc) }
    }

    private fun docToMovie(doc: com.google.firebase.firestore.DocumentSnapshot): Movie? {
        val movieId = doc.getLong("movieId")?.toInt() ?: return null
        return Movie(
            id = movieId,
            title = doc.getString("title") ?: "",
            year = doc.getLong("year")?.toInt(),
            genre = "",
            rating = doc.getDouble("averageRating") ?: 0.0,
            posterUrl = doc.getString("posterUrl"),
        )
    }
}
