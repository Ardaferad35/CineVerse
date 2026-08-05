package com.arda.cineverse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_actions")
data class PendingActionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val actionType: String, // ADD_FAVORITE, REMOVE_FAVORITE, ADD_WATCHLIST, REMOVE_WATCHLIST
    val mediaId: Int,
    val mediaType: String,
    val payloadJson: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
)
