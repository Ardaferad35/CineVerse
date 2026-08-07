package com.arda.cineverse.data.local.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * Firestore'daki "users/{uid}/favorites" ve "users/{uid}/watchlist"
 * koleksiyonlarının salt-okunur yerel aynası. Yazma işlemleri hâlâ
 * doğrudan Firestore'a gider; bu tablo sadece internet olmadan da
 * favoriler/izleme listesinin görüntülenebilmesi için var.
 */
@Entity(
    tableName = "saved_movies",
    primaryKeys = ["mediaId", "mediaType", "listType"],
    indices = [
        Index(value = ["listType"]),
        Index(value = ["listType", "addedAt"]),
    ],
)
data class SavedMovieEntity(
    val mediaId: Int,
    val mediaType: String,
    val listType: String,
    val title: String,
    val posterUrl: String?,
    val rating: Double,
    val year: Int?,
    val addedAt: Long,
    val genreIds: List<Int>,
)
