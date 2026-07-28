package com.arda.cineverse.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class AvatarPreset(val id: String, val icon: ImageVector, val color: Color)

// Avatar renkleri BİLİNÇLİ OLARAK sabit — kullanıcının seçtiği avatar,
// tema Koyu ya da Açık olsun her zaman aynı renkte görünmeli. Bu yüzden
// tema ile birlikte değişen Primary/Accent/ErrorColor yerine, o an aktif
// koyu temanın marka renklerine karşılık gelen sabit hex değerleri kullanıyoruz.
val avatarPresets = listOf(
    AvatarPreset("default", Icons.Filled.Person, Color(0xFF7C5CFC)),
    AvatarPreset("robot", Icons.Filled.SmartToy, Color(0xFF00D4C8)),
    AvatarPreset("star", Icons.Filled.Star, Color(0xFFFFC857)),
    AvatarPreset("movie", Icons.Filled.Theaters, Color(0xFFE0679A)),
    AvatarPreset("party", Icons.Filled.Celebration, Color(0xFFFF4D6D)),
    AvatarPreset("night", Icons.Filled.DarkMode, Color(0xFF5B9BD5)),
)

fun avatarPresetById(id: String?): AvatarPreset =
    avatarPresets.firstOrNull { it.id == id } ?: avatarPresets.first()