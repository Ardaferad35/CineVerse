package com.arda.cineverse.data.local.entity

/**
 * Home ekranındaki her yatay bölümü (section) temsil eder. Kanonik
 * [MovieEntity]/[TvShowEntity] tablolarına referans veren cross-ref
 * tablolarında (movie_section_cross_ref / tv_section_cross_ref) hangi
 * bölüme ait olduğunu ayırt etmek için kullanılır.
 */
enum class SectionType {
    POPULAR,
    TOP_RATED,
    UPCOMING,
    FOR_YOU,
    TV_POPULAR,
    TV_ON_AIR,
    TV_FOR_YOU,
}
