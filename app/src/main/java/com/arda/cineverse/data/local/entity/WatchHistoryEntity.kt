package com.arda.cineverse.data.local.entity

import androidx.room.Entity

/**
 * "For You" motorunun GİRDİSİ: Firestore'daki recentlyViewed/recentlyViewedTv
 * kayan pencerelerinin (en fazla 20 öğe) yerel aynası. FOR_YOU section
 * cross-ref'i ise motorun ÇIKTISI (hesaplanmış öneri listesi) — ikisi
 * karıştırılmamalı.
 */
@Entity(tableName = "watch_history", primaryKeys = ["mediaId", "mediaType"])
data class WatchHistoryEntity(
    val mediaId: Int,
    val mediaType: String,
    val genreIds: List<Int>,
    val viewedAt: Long,
    val position: Int,
)
