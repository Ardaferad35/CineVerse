package com.arda.cineverse.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.arda.cineverse.data.model.AiChatMessage
import com.arda.cineverse.data.model.AiIntent
import com.arda.cineverse.data.model.AiMovieSuggestion
import com.arda.cineverse.ui.theme.*

private val StarColor = Color(0xFFFFC857)

/** Boş sohbet ekranında gösterilen, dokununca direkt gönderilen hazır başlangıçlar. */
private val MovieStarters = listOf(
    "Bugün moralim bozuk, iyi gelecek bir film öner",
    "Sonu şaşırtan bir gerilim istiyorum",
    "Adını unuttum: uzayda geçen baba–kız filmi",
    "Arkadaşlarla izlemelik bir komedi",
)

private val TvStarters = listOf(
    "Bir çırpıda bitireceğim bir dizi öner",
    "Sonu şaşırtan bir gerilim dizisi istiyorum",
    "Adını unuttum: bir kimya öğretmeninin suça bulaştığı dizi",
    "Breaking Bad'i sevdiysem ne izlemeliyim?",
)

fun aiChatStarters(isTvMode: Boolean): List<String> = if (isTvMode) TvStarters else MovieStarters

fun aiAssistantTitle(isTvMode: Boolean): String = if (isTvMode) "Dizi Asistanı" else "Film Asistanı"

@Composable
fun AiChatEmptyState(
    isTvMode: Boolean,
    onStarterClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(brush = Brush.linearGradient(PrimaryGradient)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(aiAssistantTitle(isTvMode), color = OnSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            if (isTvMode) {
                "Nasıl hissettiğini yaz, sana göre bir dizi bulayım. Ya da adını hatırlamadığın diziyi tarif et, birlikte çıkaralım."
            } else {
                "Nasıl hissettiğini yaz, sana göre bir film bulayım. Ya da adını hatırlamadığın filmi tarif et, birlikte çıkaralım."
            },
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        aiChatStarters(isTvMode).forEach { starter ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Surface)
                    .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
                    .clickable { onStarterClick(starter) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    starter,
                    color = OnSurface,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun UserChatBubble(text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Text(
            text,
            color = OnPrimary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp))
                .background(brush = Brush.horizontalGradient(PrimaryGradient))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

@Composable
fun AiChatBubble(
    message: AiChatMessage,
    favoriteIds: Set<Int>,
    watchlistIds: Set<Int>,
    onOpenClick: (AiMovieSuggestion) -> Unit,
    onFavoriteClick: (AiMovieSuggestion) -> Unit,
    onWatchlistClick: (AiMovieSuggestion) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            AiAvatar()
            Spacer(Modifier.width(10.dp))
            Text(
                message.text,
                color = OnSurface,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                    .background(Surface)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
        if (message.suggestions.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Column(modifier = Modifier.padding(start = 42.dp)) {
                message.suggestions.forEach { suggestion ->
                    AiSuggestionCard(
                        suggestion = suggestion,
                        intent = message.intent,
                        isFavorite = suggestion.movie.id in favoriteIds,
                        isInWatchlist = suggestion.movie.id in watchlistIds,
                        onOpenClick = { onOpenClick(suggestion) },
                        onFavoriteClick = { onFavoriteClick(suggestion) },
                        onWatchlistClick = { onWatchlistClick(suggestion) },
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AiSuggestionCard(
    suggestion: AiMovieSuggestion,
    intent: AiIntent,
    isFavorite: Boolean,
    isInWatchlist: Boolean,
    onOpenClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onWatchlistClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val movie = suggestion.movie
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
            .clickable { onOpenClick() }
            .padding(12.dp),
    ) {
        Row {
            Box(
                modifier = Modifier
                    .width(58.dp)
                    .height(87.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(brush = Brush.verticalGradient(listOf(SurfaceVariant, Surface))),
            ) {
                if (movie.posterUrl != null) {
                    AsyncImage(
                        model = movie.posterUrl,
                        contentDescription = movie.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        movie.title,
                        color = OnSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFavorite) "Favorilerden çıkar" else "Favorilere ekle",
                        tint = if (isFavorite) ErrorColor else TextSecondary,
                        modifier = Modifier.size(18.dp).clickable { onFavoriteClick() },
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = StarColor, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("${movie.rating}", color = TextSecondary, fontSize = 12.sp)
                    Text(
                        "  •  ${movie.year ?: "—"}  •  ${if (movie.mediaType == "tv") "Dizi" else "Film"}",
                        color = TextSecondary,
                        fontSize = 12.sp,
                    )
                }
                if (suggestion.note.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        suggestion.note,
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 3,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CardActionButton(
                label = when {
                    intent == AiIntent.IDENTIFY -> "Detaya git"
                    movie.mediaType == "tv" -> "Diziye git"
                    else -> "Filme git"
                },
                icon = Icons.Filled.ArrowForward,
                contentColor = Primary,
                background = Primary.copy(alpha = 0.14f),
                onClick = onOpenClick,
                modifier = Modifier.weight(1f),
            )
            CardActionButton(
                label = if (isInWatchlist) "Listemde" else "Listeme ekle",
                icon = if (isInWatchlist) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                contentColor = if (isInWatchlist) Primary else TextSecondary,
                background = if (isInWatchlist) Primary.copy(alpha = 0.14f) else SurfaceVariant,
                onClick = onWatchlistClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CardActionButton(
    label: String,
    icon: ImageVector,
    contentColor: Color,
    background: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color = contentColor,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
fun ChatErrorBubble(
    text: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        AiAvatar()
        Spacer(Modifier.width(10.dp))
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                .background(ErrorColor.copy(alpha = 0.12f))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(text, color = OnSurface, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.clickable { onRetry() },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Tekrar dene", color = Primary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Asistan düşünürken görünen üç noktalı balon. */
@Composable
fun AiThinkingBubble(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        AiAvatar()
        Spacer(Modifier.width(10.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                .background(Surface)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val transition = rememberInfiniteTransition(label = "thinking")
            repeat(3) { index ->
                val alpha by transition.animateFloat(
                    initialValue = 0.25f,
                    targetValue = 0.25f,
                    animationSpec = infiniteRepeatable(
                        animation = keyframes {
                            durationMillis = 900
                            0.25f at 0 using FastOutSlowInEasing
                            1f at 300 using FastOutSlowInEasing
                            0.25f at 600 using FastOutSlowInEasing
                        },
                        repeatMode = RepeatMode.Restart,
                        initialStartOffset = StartOffset(index * 150),
                    ),
                    label = "dot$index",
                )
                Box(
                    modifier = Modifier
                        .padding(end = if (index < 2) 5.dp else 0.dp)
                        .size(7.dp)
                        .alpha(alpha)
                        .clip(CircleShape)
                        .background(Primary),
                )
            }
        }
    }
}

@Composable
private fun AiAvatar() {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(brush = Brush.linearGradient(PrimaryGradient)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun AiChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isThinking: Boolean,
    modifier: Modifier = Modifier,
) {
    val canSend = value.isNotBlank() && !isThinking
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Surface)
            .border(1.dp, DividerColor, RoundedCornerShape(26.dp))
            .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(modifier = Modifier.weight(1f).padding(vertical = 10.dp)) {
            if (value.isEmpty()) {
                Text(
                    "Ne izlemek istersin?",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = OnSurface),
                cursorBrush = SolidColor(Primary),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    brush = if (canSend) {
                        Brush.horizontalGradient(PrimaryGradient)
                    } else {
                        Brush.horizontalGradient(listOf(SurfaceVariant, SurfaceVariant))
                    },
                )
                .clickable(enabled = canSend) { onSend() },
            contentAlignment = Alignment.Center,
        ) {
            if (isThinking) {
                CircularProgressIndicator(color = TextSecondary, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
            } else {
                Icon(
                    Icons.Filled.Send,
                    contentDescription = "Gönder",
                    tint = if (canSend) OnPrimary else TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** Sohbeti sıfırlayan üst bar — geçmiş oturum içinde tutulduğu için gerekli. */
@Composable
fun AiChatHeader(
    isTvMode: Boolean,
    onClearClick: () -> Unit,
    canClear: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AiAvatar()
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(aiAssistantTitle(isTvMode), color = OnSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                if (isTvMode) "Öneri ister, adını unuttuğun diziyi sorarsın" else "Öneri ister, adını unuttuğun filmi sorarsın",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
            )
        }
        if (canClear) {
            IconButton(onClick = onClearClick) {
                Icon(Icons.Filled.Refresh, contentDescription = "Sohbeti temizle", tint = TextSecondary, modifier = Modifier.size(20.dp))
            }
        }
    }
}
