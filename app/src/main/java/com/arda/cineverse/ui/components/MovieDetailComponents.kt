package com.arda.cineverse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.arda.cineverse.data.model.CastMember
import com.arda.cineverse.data.model.Comment
import com.arda.cineverse.ui.theme.*

private val StarColor = Color(0xFFFFC857)

@Composable
fun ExpandableOverview(text: String, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Text(
            text,
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis,
        )
        if (text.length > 120) {
            Spacer(Modifier.height(4.dp))
            Text(
                if (expanded) "Daha az" else "Daha fazla",
                color = Primary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { expanded = !expanded },
            )
        }
    }
}

@Composable
fun CastMemberChip(cast: CastMember, modifier: Modifier = Modifier) {
    Column(modifier = modifier.width(72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(64.dp).clip(CircleShape).background(SurfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (cast.profileUrl != null) {
                AsyncImage(
                    model = cast.profileUrl,
                    contentDescription = cast.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
            } else {
                Icon(Icons.Filled.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(cast.name, color = OnSurface, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1, textAlign = TextAlign.Center)
        Text(cast.character, color = TextSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1, textAlign = TextAlign.Center)
    }
}

@Composable
fun RatingDistributionBar(star: Int, count: Int, total: Int, modifier: Modifier = Modifier) {
    val percent = if (total > 0) count.toFloat() / total else 0f
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("$star", color = TextSecondary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(12.dp))
        Icon(Icons.Filled.Star, contentDescription = null, tint = StarColor, modifier = Modifier.size(10.dp))
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(50)).background(SurfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(percent)
                    .clip(RoundedCornerShape(50))
                    .background(Primary),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text("${(percent * 100).toInt()}%", color = TextSecondary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(36.dp))
    }
}

private fun timeAgo(millis: Long): String {
    val diff = System.currentTimeMillis() - millis
    val minutes = diff / 60000
    val hours = diff / 3600000
    val days = diff / 86400000
    return when {
        minutes < 1 -> "az önce"
        minutes < 60 -> "$minutes dakika önce"
        hours < 24 -> "$hours saat önce"
        days < 7 -> "$days gün önce"
        else -> "${days / 7} hafta önce"
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    isOwner: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var spoilerRevealed by remember { mutableStateOf(false) }
    Row(modifier = modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.Top) {
        val preset = avatarPresetById(comment.avatarId)
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(preset.color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(preset.icon, contentDescription = null, tint = preset.color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(comment.userName, color = OnSurface, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Text(timeAgo(comment.createdAt), color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                if (comment.editedAt != null) {
                    Spacer(Modifier.width(6.dp))
                    Text("(düzenlendi)", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(2.dp))
            Row {
                repeat(5) { i ->
                    Icon(
                        if (i < comment.rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = null,
                        tint = StarColor,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            if (comment.isSpoiler && !spoilerRevealed) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = StarColor, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Bu yorum spoiler içeriyor", color = TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Text(
                        "Göster",
                        color = Primary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { spoilerRevealed = true },
                    )
                }
            } else {
                Text(comment.text, color = OnSurface, style = MaterialTheme.typography.bodyMedium)
            }
            if (isOwner) {
                Spacer(Modifier.height(6.dp))
                Row {
                    Text(
                        "Düzenle",
                        color = Primary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onEditClick() },
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        "Sil",
                        color = ErrorColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onDeleteClick() },
                    )
                }
            }
        }
    }
}

@Composable
fun CommentInputBox(
    initialText: String = "",
    initialRating: Int = 5,
    initialSpoiler: Boolean = false,
    submitLabel: String = "Yorumu Paylaş",
    onSubmit: (text: String, rating: Int, isSpoiler: Boolean) -> Unit,
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf(initialText) }
    var rating by remember { mutableStateOf(initialRating) }
    var isSpoiler by remember { mutableStateOf(initialSpoiler) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(5) { i ->
                Icon(
                    if (i < rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = "${i + 1} yıldız",
                    tint = StarColor,
                    modifier = Modifier.size(26.dp).clickable { rating = i + 1 },
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Background)
                .padding(10.dp),
        ) {
            if (text.isEmpty()) {
                Text("Film hakkında düşüncelerini yaz...", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = OnSurface),
                cursorBrush = SolidColor(Primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { isSpoiler = !isSpoiler },
            ) {
                Icon(
                    if (isSpoiler) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                    contentDescription = null,
                    tint = if (isSpoiler) Primary else TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("Spoiler içeriyor", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.weight(1f))
            if (onCancel != null) {
                Text(
                    "Vazgeç",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable { onCancel() }.padding(end = 14.dp),
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        brush = if (text.isNotBlank()) {
                            Brush.horizontalGradient(PrimaryGradient)
                        } else {
                            Brush.horizontalGradient(listOf(SurfaceVariant, SurfaceVariant))
                        },
                    )
                    .clickable(enabled = text.isNotBlank()) {
                        onSubmit(text.trim(), rating, isSpoiler)
                        if (onCancel == null) {
                            text = ""
                            rating = 5
                            isSpoiler = false
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(submitLabel, color = OnPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun MovieDetailStickyBar(
    isFavorite: Boolean,
    isSaved: Boolean,
    onHomeClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onSaveClick: () -> Unit,
    onRateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Surface)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StickyBarItem(icon = Icons.Filled.Home, label = "Ana Sayfa", tint = TextSecondary, onClick = onHomeClick)
        StickyBarItem(
            icon = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            label = "Favori",
            tint = if (isFavorite) ErrorColor else TextSecondary,
            onClick = onFavoriteClick,
        )
        StickyBarItem(
            icon = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
            label = "Kaydet",
            tint = if (isSaved) Primary else TextSecondary,
            onClick = onSaveClick,
        )
        StickyBarItem(icon = Icons.Filled.Star, label = "Puan Ver", tint = StarColor, onClick = onRateClick)
    }
}

@Composable
private fun StickyBarItem(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() },
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, color = tint, style = MaterialTheme.typography.labelSmall)
    }
}