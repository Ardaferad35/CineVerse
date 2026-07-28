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
import com.arda.cineverse.ui.theme.Accent
import com.arda.cineverse.ui.theme.ErrorColor
import com.arda.cineverse.ui.theme.Primary

data class AvatarPreset(val id: String, val icon: ImageVector, val color: Color)

val avatarPresets = listOf(
    AvatarPreset("default", Icons.Filled.Person, Primary),
    AvatarPreset("robot", Icons.Filled.SmartToy, Accent),
    AvatarPreset("star", Icons.Filled.Star, Color(0xFFFFC857)),
    AvatarPreset("movie", Icons.Filled.Theaters, Color(0xFFE0679A)),
    AvatarPreset("party", Icons.Filled.Celebration, ErrorColor),
    AvatarPreset("night", Icons.Filled.DarkMode, Color(0xFF5B9BD5)),
)

fun avatarPresetById(id: String?): AvatarPreset =
    avatarPresets.firstOrNull { it.id == id } ?: avatarPresets.first()