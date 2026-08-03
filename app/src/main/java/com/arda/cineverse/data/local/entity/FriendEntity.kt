package com.arda.cineverse.data.local.entity

import androidx.room.Entity

/**
 * Firestore'daki "users/{uid}/friends" koleksiyonunun salt-okunur yerel
 * aynası. Yazma işlemleri hâlâ doğrudan Firestore'a gider; bu tablo sadece
 * internet olmadan da arkadaş listesinin görüntülenebilmesi için var
 * (bkz. SavedMovieEntity'deki aynı desen).
 */
@Entity(tableName = "friends", primaryKeys = ["friendUid"])
data class FriendEntity(
    val friendUid: String,
    val username: String,
    val fullName: String,
    val avatarId: String,
    val since: Long,
)
