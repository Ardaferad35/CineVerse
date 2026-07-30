package com.arda.cineverse.data.local.db

import androidx.room.TypeConverter
import com.arda.cineverse.data.local.entity.SectionType

private const val STRING_LIST_SEPARATOR = "§§"

class Converters {
    @TypeConverter
    fun fromIntList(value: List<Int>): String = value.joinToString(",")

    @TypeConverter
    fun toIntList(value: String): List<Int> =
        if (value.isBlank()) emptyList() else value.split(",").map { it.toInt() }

    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(STRING_LIST_SEPARATOR)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split(STRING_LIST_SEPARATOR)

    @TypeConverter
    fun fromSectionType(value: SectionType): String = value.name

    @TypeConverter
    fun toSectionType(value: String): SectionType = SectionType.valueOf(value)
}
