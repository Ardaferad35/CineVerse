package com.arda.cineverse.data.model

/**
 * Firestore'daki "movies/{movieId}/comments" koleksiyonunda tutulur.
 * Tüm alanlar varsayılan değerlere sahip olmalı (Firestore'un otomatik
 * nesneye çevirme işlemi için gerekli).
 */
data class Comment(
    val id: String = "",
    val movieId: Int = 0,
    val userId: String = "",
    val userName: String = "",
    val text: String = "",
    val rating: Int = 5,
    val isSpoiler: Boolean = false,
    val createdAt: Long = 0L,
    val editedAt: Long? = null,
    // Firestore'da SAKLANMIYOR — CommentRepository.getComments() tarafından,
    // yorum sahibinin O ANKİ güncel avatarına bakılarak gösterim anında
    // dolduruluyor. Böylece kullanıcı avatarını değiştirdiğinde geçmiş
    // yorumlarındaki avatar da otomatik güncellenmiş olur.
    val avatarId: String = "default",
    // Bu yorum başka bir yoruma yanıtsa, o yorumun ID'sini taşır. Ana
    // yorumlarda null'dır.
    val replyToCommentId: String? = null,
)