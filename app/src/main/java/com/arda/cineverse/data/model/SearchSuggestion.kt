package com.arda.cineverse.data.model

enum class SuggestionType { MOVIE, TV, PERSON }

data class SearchSuggestion(
    val id: Int,
    val type: SuggestionType,
    val title: String,
    val subtitle: String,
    val imageUrl: String?,
)