package com.arda.cineverse.data.model

/**
 * Firestore'daki "users/{uid}/friends" koleksiyonunda tutulur. Kabul edilen
 * bir arkadaşlık isteğinden sonra HER İKİ kullanıcının belgesinde de karşılıklı
 * olarak bir kopya bulunur.
 */
data class Friend(
    val friendUid: String = "",
    val username: String = "",
    val fullName: String = "",
    val avatarId: String = "",
    val since: Long = 0L,
)
