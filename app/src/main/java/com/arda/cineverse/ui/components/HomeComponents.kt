package com.arda.cineverse.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.arda.cineverse.R
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
    onProfileClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onFriendsClick: () -> Unit = {},
    unreadNotificationCount: Int = 0,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Theaters, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.home_logo_cine), color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(
                stringResource(R.string.home_logo_verse),
                style = MaterialTheme.typography.titleLarge.copy(
                    brush = Brush.horizontalGradient(PrimaryGradient),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                ),
            )
            if (!ThemeState.isDarkTheme) {
                Text(
                    stringResource(R.string.home_logo_light_badge),
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
                    .clickable { onFriendsClick() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Group, contentDescription = stringResource(R.string.home_friends_cd), tint = TextSecondary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(SurfaceVariant)
                    .clickable { onNotificationsClick() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Notifications, contentDescription = stringResource(R.string.home_notifications_cd), tint = TextSecondary, modifier = Modifier.size(18.dp))
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
                            if (unreadNotificationCount > 9) stringResource(R.string.home_badge_9_plus) else "$unreadNotificationCount",
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
                Icon(Icons.Filled.Person, contentDescription = stringResource(R.string.home_profile_cd), tint = TextSecondary, modifier = Modifier.size(20.dp))
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

enum class HomeMode(@androidx.annotation.StringRes val labelRes: Int) {
    MOVIES(R.string.home_mode_movies),
    TV_SHOWS(R.string.home_mode_tv_shows),
}

@Composable
fun HomeModeSelector(
    selected: HomeMode,
    onSelectedChange: (HomeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Surface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        HomeMode.entries.forEach { mode ->
            val isSelected = mode == selected
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        brush = if (isSelected) {
                            Brush.horizontalGradient(PrimaryGradient)
                        } else {
                            Brush.horizontalGradient(listOf(SurfaceVariant, SurfaceVariant))
                        },
                    )
                    .clickable { onSelectedChange(mode) }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(mode.labelRes),
                    color = if (isSelected) OnPrimary else TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
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
                Text(stringResource(R.string.home_search_placeholder), color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
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
                contentDescription = stringResource(R.string.common_clear),
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
            Text(stringResource(R.string.home_ai_badge), color = OnPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeaturedMovieBanner(
    movie: FeaturedMovie,
    onDetailsClick: () -> Unit,
    onAddToListClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.home_movie_of_the_day),
    isInWatchlist: Boolean = false,
    pageCount: Int = 4,
    currentPage: Int = 0,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 340.dp, max = 380.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0B0B10))
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = { onDetailsClick() },
                        onLongClick = onLongClick,
                    )
                } else {
                    Modifier.clickable { onDetailsClick() }
                },
            ),
    ) {
        val isNarrow = maxWidth < 360.dp

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

        val bannerTitleColor = Color(0xFFFFFFFF)
        val bannerSubtleColor = Color(0xFFC7C7D1)

        val cardPadding = if (isNarrow) 10.dp else 14.dp
        val titleFontSize = if (isNarrow) 22.sp else 26.sp
        val titleLetterSpacing = if (isNarrow) 1.sp else 1.5.sp
        val buttonFontSize = if (isNarrow) 12.sp else 13.5.sp
        val buttonIconSize = if (isNarrow) 15.dp else 17.dp
        val buttonVertPadding = if (isNarrow) 6.dp else 8.dp
        val buttonHorizPadding = if (isNarrow) 6.dp else 8.dp

        Column(modifier = Modifier.fillMaxSize().padding(cardPadding)) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Surface)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = StarColor, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(label, color = OnSurface, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.weight(1f))

            Text(
                movie.title,
                color = bannerTitleColor,
                fontSize = titleFontSize,
                fontWeight = FontWeight.Bold,
                letterSpacing = titleLetterSpacing,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = StarColor, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("${movie.rating}", color = bannerTitleColor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Text("${movie.year}", color = bannerSubtleColor, style = MaterialTheme.typography.bodyMedium)
                if (movie.durationLabel.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        movie.durationLabel,
                        color = bannerSubtleColor,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (movie.genre.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            movie.genre,
                            color = OnSurface,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(
                movie.description,
                color = bannerSubtleColor,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (isNarrow) 8.dp else 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(brush = Brush.horizontalGradient(PrimaryGradient))
                        .clickable { onDetailsClick() }
                        .padding(horizontal = buttonHorizPadding, vertical = buttonVertPadding),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(buttonIconSize))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.home_view_details),
                        color = OnPrimary,
                        fontSize = buttonFontSize,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Surface)
                        .clickable { onAddToListClick() }
                        .padding(horizontal = buttonHorizPadding, vertical = buttonVertPadding),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (isInWatchlist) Icons.Filled.Check else Icons.Filled.Add,
                        contentDescription = null,
                        tint = OnSurface,
                        modifier = Modifier.size(buttonIconSize),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (isInWatchlist) stringResource(R.string.home_in_watchlist) else stringResource(R.string.home_add_to_watchlist),
                        color = OnSurface,
                        fontSize = buttonFontSize,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
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
                Text(stringResource(R.string.common_see_all), color = Primary, style = MaterialTheme.typography.bodyMedium)
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/**
 * Home ekranında Room cache'inden yararlanıldığını (çevrimdışıyken) veya
 * en son ne zaman taze veriyle senkronize edildiğini gösteren küçük bir
 * durum şeridi. isOffline true iken önceliklidir; aksi halde son
 * senkronizasyon zamanı gösterilir (hiç senkron olunmadıysa hiçbir şey
 * çizilmez).
 */
@Composable
fun OfflineStatusBanner(
    isOffline: Boolean,
    lastSyncedAt: Long?,
    modifier: Modifier = Modifier,
) {
    val label = when {
        isOffline -> stringResource(R.string.home_offline_mode_banner)
        lastSyncedAt != null -> stringResource(R.string.home_last_updated, formatLastSyncedAt(lastSyncedAt))
        else -> null
    } ?: return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isOffline) ErrorColor.copy(alpha = 0.12f) else Surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (isOffline) Icons.Filled.CloudOff else Icons.Filled.CloudDone,
            contentDescription = null,
            tint = if (isOffline) ErrorColor else TextSecondary,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
    }
}

/**
 * Sabit bir Türkçe ay-adı dizisi yerine Locale("tr") tabanlı biçimlendirme
 * kullanıyor — uygulama ileride başka bir dile taşınırsa bu fonksiyona
 * dokunmaya gerek kalmadan doğru ay adını üretmeye devam eder.
 */
private fun formatLastSyncedAt(timestampMillis: Long): String {
    val formatter = java.text.SimpleDateFormat("d MMM, HH:mm", java.util.Locale.forLanguageTag("tr"))
    return formatter.format(java.util.Date(timestampMillis))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PopularMovieCard(
    movie: Movie,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
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
                .then(
                    if (onLongClick != null) {
                        Modifier.combinedClickable(
                            onClick = { onClick() },
                            onLongClick = onLongClick,
                        )
                    } else {
                        Modifier.clickable { onClick() }
                    },
                ),
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
                    contentDescription = stringResource(R.string.home_add_to_favorites_cd),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UpcomingMovieCard(
    movie: UpcomingMovie,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.width(140.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(14.dp))
                .background(brush = Brush.verticalGradient(listOf(SurfaceVariant, Surface)))
                .then(
                    if (onLongClick != null) {
                        Modifier.combinedClickable(
                            onClick = { onClick() },
                            onLongClick = onLongClick,
                        )
                    } else {
                        Modifier.clickable { onClick() }
                    },
                ),
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
        BottomNavItem(stringResource(R.string.common_nav_home), Icons.Outlined.Home, Icons.Filled.Home),
        BottomNavItem(stringResource(R.string.common_nav_assistant), Icons.Outlined.AutoAwesome, Icons.Filled.AutoAwesome),
        BottomNavItem(stringResource(R.string.common_nav_my_list), Icons.Outlined.BookmarkBorder, Icons.Filled.Bookmark),
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
    onTvShowClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface),
    ) {
        suggestions.forEachIndexed { index, suggestion ->
            val clickable = suggestion.type == SuggestionType.MOVIE || suggestion.type == SuggestionType.TV
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (clickable) {
                            Modifier.clickable {
                                if (suggestion.type == SuggestionType.TV) {
                                    onTvShowClick(suggestion.id)
                                } else {
                                    onMovieClick(suggestion.id)
                                }
                            }
                        } else Modifier
                    )
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
                    Text(stringResource(R.string.common_coming_soon), color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                }
            }
            if (index != suggestions.lastIndex) {
                HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun FloatingDiceButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(62.dp)
            .clip(CircleShape)
            .background(Brush.horizontalGradient(listOf(Color(0xFF7C4DFF), Color(0xFFE040FB))))
            .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Casino,
            contentDescription = stringResource(R.string.home_dice_button_cd),
            tint = Color.White,
            modifier = Modifier.size(32.dp),
        )
    }
}

@Composable
fun DicePickerDialog(
    popularMovies: List<Movie>,
    popularTvShows: List<Movie>,
    topRatedMovies: List<Movie>,
    onMovieClick: (Int) -> Unit,
    onTvShowClick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedFilter by remember { mutableStateOf("all") }
    var resultMovie by remember { mutableStateOf<Movie?>(null) }
    var isSpinning by remember { mutableStateOf(false) }

    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val rolledHistory = remember { mutableSetOf<Pair<Int, String>>() }

    fun rollDice() {
        if (isSpinning) return
        isSpinning = true
        resultMovie = null

        scope.launch {
            rotation.snapTo(0f)
            rotation.animateTo(
                targetValue = 1440f,
                animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            )

            val rawCandidates = mutableListOf<Movie>()
            when (selectedFilter) {
                "movie" -> {
                    rawCandidates.addAll(popularMovies.map { it.copy(mediaType = "movie") })
                    rawCandidates.addAll(topRatedMovies.map { it.copy(mediaType = "movie") })
                }
                "tv" -> {
                    rawCandidates.addAll(popularTvShows.map { it.copy(mediaType = "tv") })
                }
                else -> {
                    rawCandidates.addAll(popularMovies.map { it.copy(mediaType = "movie") })
                    rawCandidates.addAll(topRatedMovies.map { it.copy(mediaType = "movie") })
                    rawCandidates.addAll(popularTvShows.map { it.copy(mediaType = "tv") })
                }
            }

            val distinctCandidates = rawCandidates.distinctBy { Pair(it.id, it.mediaType) }
            var unrolled = distinctCandidates.filterNot { Pair(it.id, it.mediaType) in rolledHistory }
            if (unrolled.isEmpty() && distinctCandidates.isNotEmpty()) {
                rolledHistory.clear()
                unrolled = distinctCandidates
            }

            val picked = if (unrolled.isNotEmpty()) unrolled.random() else null
            picked?.let { rolledHistory.add(Pair(it.id, it.mediaType)) }
            resultMovie = picked
            isSpinning = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .widthIn(max = 335.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFF161224))
                .border(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color(0xFFAB47BC),
                            Color(0xFF7C4DFF),
                            Color(0xFFAB47BC).copy(alpha = 0.3f),
                        ),
                    ),
                    shape = RoundedCornerShape(26.dp),
                )
                .padding(20.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.align(Alignment.CenterStart),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Casino,
                            contentDescription = null,
                            tint = StarColor,
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer { rotationZ = rotation.value },
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.home_dice_dialog_title),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2C2443)),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.common_close), tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "all" to stringResource(R.string.home_dice_filter_all),
                            "movie" to stringResource(R.string.home_dice_filter_movie),
                            "tv" to stringResource(R.string.home_dice_filter_tv),
                        ).forEach { (type, label) ->
                            val isSelected = selectedFilter == type
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0xFF7C4DFF) else Color(0xFF29213D))
                                    .clickable { selectedFilter = type }
                                    .padding(horizontal = 13.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    label,
                                    color = if (isSelected) Color.White else Color(0xFFC0C0E0),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .size(82.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(Color(0xFF7C4DFF), Color(0xFFE040FB))))
                        .clickable(enabled = !isSpinning) { rollDice() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Casino,
                        contentDescription = stringResource(R.string.home_dice_roll_cd),
                        tint = Color.White,
                        modifier = Modifier
                            .size(44.dp)
                            .graphicsLayer { rotationZ = rotation.value },
                    )
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    if (isSpinning) stringResource(R.string.home_dice_spinning) else stringResource(R.string.home_dice_tap_to_roll),
                    color = Color(0xFFE0E0FF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )

                resultMovie?.let { movie ->
                    Spacer(Modifier.height(16.dp))
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + scaleIn(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF231C38))
                                .border(1.5.dp, Color(0xFF7C4DFF).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (movie.posterUrl != null) {
                                    AsyncImage(
                                        model = movie.posterUrl,
                                        contentDescription = movie.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(width = 54.dp, height = 78.dp)
                                            .clip(RoundedCornerShape(10.dp)),
                                    )
                                    Spacer(Modifier.width(12.dp))
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF7C4DFF))
                                                .padding(horizontal = 7.dp, vertical = 2.5.dp),
                                        ) {
                                            Text(
                                                if (movie.mediaType == "tv") stringResource(R.string.home_dice_result_tv_badge) else stringResource(R.string.home_dice_result_movie_badge),
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                        Spacer(Modifier.width(6.dp))
                                        Icon(Icons.Filled.Star, contentDescription = null, tint = StarColor, modifier = Modifier.size(13.dp))
                                        Spacer(Modifier.width(3.dp))
                                        Text("${movie.rating}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        movie.year?.let { y ->
                                            Spacer(Modifier.width(6.dp))
                                            Text("• $y", color = Color(0xFFB0B0D0), fontSize = 11.sp)
                                        }
                                    }

                                    Spacer(Modifier.height(5.dp))

                                    Text(
                                        movie.title,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF7C4DFF))
                                        .clickable {
                                            onDismiss()
                                            if (movie.mediaType == "tv") onTvShowClick(movie.id) else onMovieClick(movie.id)
                                        }
                                        .padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        stringResource(R.string.home_dice_view_details),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF352B52))
                                        .clickable { rollDice() }
                                        .padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(stringResource(R.string.home_dice_roll_again), color = Color(0xFFE0E0FF), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class QuickPreviewData(
    val id: Int,
    val title: String,
    val overview: String = "",
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val rating: Double = 0.0,
    val year: Int? = null,
    val genre: String = "",
    val mediaType: String = "movie",
    val isFavorite: Boolean = false,
)

@Composable
fun QuickPreviewSheet(
    data: QuickPreviewData,
    onNavigateDetail: () -> Unit,
    onShareWithFriends: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Surface)
                .border(1.dp, Brush.verticalGradient(listOf(Primary.copy(alpha = 0.6f), Color.Transparent)), RoundedCornerShape(22.dp)),
        ) {
            Column {
                // Header Image Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                ) {
                    if (data.backdropUrl != null || data.posterUrl != null) {
                        AsyncImage(
                            model = data.backdropUrl ?: data.posterUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(listOf(SurfaceVariant, Surface))),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.2f),
                                        Color.Black.copy(alpha = 0.85f),
                                    ),
                                ),
                            ),
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f)),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.common_close), tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                // Info Section
                Column(
                    modifier = Modifier.padding(14.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (data.posterUrl != null) {
                            AsyncImage(
                                model = data.posterUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 46.dp, height = 66.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Primary.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                            )
                            Spacer(Modifier.width(10.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = data.title,
                                color = OnSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                            )

                            Spacer(Modifier.height(4.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (data.rating > 0) {
                                    Icon(Icons.Filled.Star, contentDescription = null, tint = StarColor, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text("${data.rating}", color = OnSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(6.dp))
                                }

                                if (data.year != null && data.year > 0) {
                                    Text("${data.year}", color = TextSecondary, fontSize = 11.sp)
                                    Spacer(Modifier.width(6.dp))
                                }

                                if (data.genre.isNotBlank()) {
                                    Text(data.genre, color = TextSecondary, fontSize = 11.sp, maxLines = 1)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = if (data.overview.isNotBlank()) data.overview else stringResource(R.string.home_no_overview),
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.horizontalGradient(PrimaryGradient))
                                .clickable {
                                    onDismiss()
                                    onShareWithFriends()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.home_quick_preview_recommend), color = OnPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceVariant)
                                .border(1.dp, Primary.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .clickable {
                                    onDismiss()
                                    onNavigateDetail()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Info, contentDescription = null, tint = OnSurface, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    stringResource(R.string.common_details),
                                    color = OnSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}