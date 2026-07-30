package com.arda.cineverse.data.local.entity

import androidx.room.Entity

@Entity(tableName = "categories", primaryKeys = ["id", "isTv"])
data class CategoryEntity(
    val id: String,
    val label: String,
    val genreId: Int,
    val isTv: Boolean,
    val position: Int,
)
