package com.arda.cineverse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.arda.cineverse.data.model.Category
import com.arda.cineverse.data.model.FeaturedMovie
import com.arda.cineverse.data.model.Movie
import com.arda.cineverse.data.model.SearchSuggestion
import com.arda.cineverse.data.model.SuggestionType
import com.arda.cineverse.data.model.UpcomingMovie
import com.arda.cineverse.ui.theme.*

private val StarColor = Color(0xFFFFC857)

@Composable
fun HomeTopBar(
    onMenuClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    unreadNotificationCount: Int = 0,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onMenuClick) {
            Icon(Icons.Filled.Menu, contentDescription = "Menü", tint = OnSurface)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Theaters, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text("Cine", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(
                "Verse",
                style = MaterialTheme.typography.titleLarge.copy(
                    brush = Brush.horizontalGradient(PrimaryGradient),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                ),
            )
            if (!ThemeState.isDarkTheme) {
                Text(
                    "Light",
                    color = Primary,
                    fontWeight = FontWeight.Medium,
                    fontStyle = FontStyle.Italic,
                    fontSize = 20.sp,
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(SurfaceVariant)
                    .clickable { onNotificationsClick() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Notifications, contentDescription = "Bildirimler", tint = TextSecondary, modifier = Modifier.size(18.dp))
                if (unreadNotificationCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(ErrorColor)
                            .border(2.dp, Background, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (unreadNotificationCount > 9) "9+" else "$unreadNotificationCount",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(SurfaceVariant)
                    .clickable { onProfileClick() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Person, contentDescription = "Profil", tint = TextSecondary, modifier = Modifier.size(20.dp))
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Accent)
                        .border(2.dp, Background, CircleShape),
                )
            }
        }
    }
}

@Composable
fun HomeSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onAiClick: () -> Unit,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Surface)
            .padding(start = 16.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text("Film, dizi veya oyuncu ara...", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = OnSurface),
                cursorBrush = SolidColor(Primary),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty() && onClear != null) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Temizle",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp).clickable { onClear() },
            )
            Spacer(Modifier.width(8.dp))
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(brush = Brush.horizontalGradient(PrimaryGradient))
                .clickable { onAiClick() }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("AI", color = OnPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun FeaturedMovieBanner(
    movie: FeaturedMovie,
    onDetailsClick: () -> Unit,
    onAddToListClick: () -> Unit,
    modifier: Modifier = Modifier,
    pageCount: Int = 4,
    currentPage: Int = 0,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(340.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0B0B10)),
    ) {
        if (movie.backdropUrl != null) {
            AsyncImage(
                model = movie.backdropUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = Brush.verticalGradient(listOf(Color(0xFF1B1C3E), Color(0xFF0B0B10)))),
            )
        }

        // Metnin okunabilirliği için alttan koyulaşan gölge
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0B0B10).copy(alpha = 0.15f),
                            Color(0xFF0B0B10).copy(alpha = 0.55f),
                            Color(0xFF0B0B10).copy(alpha = 0.96f),
                        ),
                    ),
                ),
        )

        // Bu metinler her zaman koyu bir karartmanın üzerinde durur (fotoğraf
        // okunabilirliği için scrim bilinçli olarak hep koyu) — bu yüzden
        // tema ile değişen OnSurface/TextSecondary yerine SABİT açık renkler
        // kullanıyoruz, aksi halde Açık temada koyu yazı koyu zemine karışır.
        val bannerTitleColor = Color(0xFFFFFFFF)
        val bannerSubtleColor = Color(0xFFC7C7D1)

        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Surface)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = StarColor, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("GÜNÜN FİLMİ", color = OnSurface, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.weight(1f))

            Text(movie.title, color = bannerTitleColor, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, maxLines = 2)
            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = StarColor, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("${movie.rating}", color = bannerTitleColor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(10.dp))
                Text("${movie.year}", color = bannerSubtleColor, style = MaterialTheme.typography.bodyMedium)
                if (movie.durationLabel.isNotEmpty()) {
                    Spacer(Modifier.width(10.dp))
                    Text(movie.durationLabel, color = bannerSubtleColor, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(movie.genre, color = OnSurface, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(movie.description, color = bannerSubtleColor, style = MaterialTheme.typography.bodyMedium, maxLines = 2)

            Spacer(Modifier.height(16.dp))
            Row {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(brush = Brush.horizontalGradient(PrimaryGradient))
                        .clickable { onDetailsClick() }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Detayları Gör", color = OnPrimary, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.width(12.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Surface)
                        .clickable { onAddToListClick() }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = OnSurface, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Listeme Ekle", color = OnSurface, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(pageCount) { index ->
                    val active = index == currentPage
                    Box(
                        modifier = Modifier
                            .size(width = if (active) 18.dp else 6.dp, height = 6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (active) Primary else TextSecondary.copy(alpha = 0.4f)),
                    )
                }
            }
        }
    }
}

@Composable
fun HomeSectionHeader(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    onSeeAllClick: () -> Unit = {},
    showSeeAll: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, color = OnSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (showSeeAll) {
            Row(
                modifier = Modifier.clickable { onSeeAllClick() },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Tümünü Gör", color = Primary, style = MaterialTheme.typography.bodyMedium)
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun PopularMovieCard(
    movie: Movie,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.width(140.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(14.dp))
                .background(brush = Brush.verticalGradient(listOf(SurfaceVariant, Surface)))
                .clickable { onClick() },
        ) {
            if (movie.posterUrl != null) {
                AsyncImage(
                    model = movie.posterUrl,
                    contentDescription = movie.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    movie.title,
                    color = OnSurface,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    modifier = Modifier.align(Alignment.Center).padding(10.dp),
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Background.copy(alpha = 0.75f))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = StarColor, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(3.dp))
                Text("${movie.rating}", color = OnSurface, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier.align(Alignment.TopEnd).size(32.dp),
            ) {
                Icon(
                    if (movie.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorilere ekle",
                    tint = if (movie.isFavorite) ErrorColor else Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(movie.title, color = OnSurface, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text("${movie.year ?: "—"} • ${movie.genre}", color = TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}

@Composable
fun UpcomingMovieCard(
    movie: UpcomingMovie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.width(140.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(14.dp))
                .background(brush = Brush.verticalGradient(listOf(SurfaceVariant, Surface)))
                .clickable { onClick() },
        ) {
            if (movie.posterUrl != null) {
                AsyncImage(
                    model = movie.posterUrl,
                    contentDescription = movie.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    movie.title,
                    color = OnSurface,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    modifier = Modifier.align(Alignment.Center).padding(10.dp),
                )
            }
            if (movie.releaseDateLabel.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(brush = Brush.horizontalGradient(PrimaryGradient))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(movie.releaseDateLabel, color = OnPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(movie.title, color = OnSurface, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text("${movie.year ?: "—"}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun CategoryChip(
    category: Category,
    icon: ImageVector,
    iconTint: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(84.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Primary.copy(alpha = 0.15f) else Surface)
            .then(if (selected) Modifier.border(1.5.dp, Primary, RoundedCornerShape(14.dp)) else Modifier)
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) Primary else iconTint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(6.dp))
        Text(
            category.label,
            color = if (selected) OnSurface else TextSecondary,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

private data class BottomNavItem(val label: String, val outlined: ImageVector, val filled: ImageVector)

@Composable
fun CVBottomNavBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        BottomNavItem("Ana Sayfa", Icons.Outlined.Home, Icons.Filled.Home),
        BottomNavItem("Arama", Icons.Outlined.Search, Icons.Filled.Search),
        BottomNavItem("Listem", Icons.Outlined.BookmarkBorder, Icons.Filled.Bookmark),
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(Surface)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = index == selectedIndex
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) Primary.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable { onItemSelected(index) }
                    .padding(horizontal = if (isSelected) 16.dp else 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (isSelected) item.filled else item.outlined,
                    contentDescription = item.label,
                    tint = if (isSelected) Primary else TextSecondary,
                    modifier = Modifier.size(20.dp),
                )
                if (isSelected) {
                    Spacer(Modifier.width(6.dp))
                    Text(item.label, color = Primary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun SearchSuggestionsList(
    suggestions: List<SearchSuggestion>,
    onMovieClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface),
    ) {
        suggestions.forEachIndexed { index, suggestion ->
            val clickable = suggestion.type == SuggestionType.MOVIE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (clickable) Modifier.clickable { onMovieClick(suggestion.id) } else Modifier)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val isPerson = suggestion.type == SuggestionType.PERSON
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(if (isPerson) CircleShape else RoundedCornerShape(8.dp))
                        .background(SurfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (suggestion.imageUrl != null) {
                        AsyncImage(
                            model = suggestion.imageUrl,
                            contentDescription = suggestion.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(if (isPerson) CircleShape else RoundedCornerShape(8.dp)),
                        )
                    } else {
                        Icon(
                            when (suggestion.type) {
                                SuggestionType.MOVIE -> Icons.Filled.Movie
                                SuggestionType.TV -> Icons.Filled.Tv
                                SuggestionType.PERSON -> Icons.Filled.Person
                            },
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        suggestion.title,
                        color = if (clickable) OnSurface else TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                    Text(suggestion.subtitle, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                }
                if (!clickable) {
                    Text("Yakında", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                }
            }
            if (index != suggestions.lastIndex) {
                HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
            }
        }
    }
}