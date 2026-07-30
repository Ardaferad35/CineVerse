package com.arda.cineverse.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * TMDB'nin film ve dizi türlerinin hepsi için ayrı ikon ve renk.
 * Film ID'leri: TMDB genre/movie/list
 * Dizi ID'leri: TMDB genre/tv/list (bazı ID'ler film ile ortaktır, bazıları TV'ye özeldir)
 */
fun categoryIcon(genreId: Int): ImageVector = when (genreId) {
    // Film & Dizi ortak türler
    28 -> Icons.Filled.Bolt           // Aksiyon
    12 -> Icons.Filled.Explore        // Macera
    16 -> Icons.Filled.Brush          // Animasyon
    35 -> Icons.Filled.SentimentSatisfied // Komedi
    80 -> Icons.Filled.Gavel          // Suç
    99 -> Icons.Filled.Videocam       // Belgesel
    18 -> Icons.Filled.TheaterComedy  // Dram
    10751 -> Icons.Filled.Groups      // Aile
    14 -> Icons.Filled.AutoFixHigh    // Fantastik
    36 -> Icons.Filled.AccountBalance // Tarih
    27 -> Icons.Filled.DarkMode       // Korku
    10402 -> Icons.Filled.MusicNote   // Müzik
    9648 -> Icons.Filled.QuestionMark // Gizem
    10749 -> Icons.Filled.Favorite    // Romantik
    878 -> Icons.Filled.RocketLaunch  // Bilim Kurgu
    10770 -> Icons.Filled.Tv          // TV Filmi
    53 -> Icons.Filled.Whatshot       // Gerilim
    10752 -> Icons.Filled.MilitaryTech // Savaş
    37 -> Icons.Filled.Terrain        // Vahşi Batı
    // TV'ye özel türler
    10759 -> Icons.Filled.Shield      // Aksiyon & Macera (TV)
    10762 -> Icons.Filled.ChildCare   // Çocuk (TV)
    10763 -> Icons.Filled.Newspaper   // Haberler (TV)
    10764 -> Icons.Filled.LiveTv      // Gerçeklik (Reality, TV)
    10765 -> Icons.Filled.RocketLaunch // Bilim Kurgu & Fantastik (TV)
    10766 -> Icons.Filled.Favorite    // Pembe Dizi (Soap, TV)
    10767 -> Icons.Filled.Mic // Talk Show (TV)
    10768 -> Icons.Filled.MilitaryTech  // Savaş & Politika (TV)
    else -> Icons.Filled.Movie
}

fun categoryColor(genreId: Int): Color = when (genreId) {
    // Film & Dizi ortak türler
    28 -> Color(0xFFFF6B4A)   // Aksiyon
    12 -> Color(0xFF4ADE80)   // Macera
    16 -> Color(0xFFFF8FCF)   // Animasyon
    35 -> Color(0xFFFFC857)   // Komedi
    80 -> Color(0xFF94A3B8)   // Suç
    99 -> Color(0xFF38BDF8)   // Belgesel
    18 -> Color(0xFFE0679A)   // Dram
    10751 -> Color(0xFFFDBA74) // Aile
    14 -> Color(0xFFA78BFA)   // Fantastik
    36 -> Color(0xFFC4A484)   // Tarih
    27 -> Color(0xFFFF4D6D)   // Korku
    10402 -> Color(0xFFF472B6) // Müzik
    9648 -> Color(0xFF818CF8)  // Gizem
    10749 -> Color(0xFFFB7185) // Romantik
    878 -> Color(0xFF00D4C8)   // Bilim Kurgu
    10770 -> Color(0xFF60A5FA) // TV Filmi
    53 -> Color(0xFFF97316)    // Gerilim
    10752 -> Color(0xFF78716C) // Savaş
    37 -> Color(0xFFB45309)    // Vahşi Batı
    // TV'ye özel türler
    10759 -> Color(0xFFFF6B4A) // Aksiyon & Macera (TV)
    10762 -> Color(0xFF67E8F9) // Çocuk (TV)
    10763 -> Color(0xFF6EE7B7) // Haberler (TV)
    10764 -> Color(0xFFFBBF24) // Gerçeklik (TV)
    10765 -> Color(0xFF00D4C8) // Bilim Kurgu & Fantastik (TV)
    10766 -> Color(0xFFFB7185) // Pembe Dizi (TV)
    10767 -> Color(0xFFC084FC) // Talk Show (TV)
    10768 -> Color(0xFF78716C) // Savaş & Politika (TV)
    else -> Color(0xFF9AA0A6)
}