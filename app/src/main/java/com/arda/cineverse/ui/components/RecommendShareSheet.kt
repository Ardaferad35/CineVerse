package com.arda.cineverse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.arda.cineverse.R
import com.arda.cineverse.data.model.Friend
import com.arda.cineverse.ui.theme.*
import com.arda.cineverse.viewmodel.RecommendShareUiState

/**
 * Film/dizi detayındaki paylaş butonunun açtığı yarım ekran sayfa: arkadaş
 * seç, istersen kısa bir not ekle, gönder. Günlük hak burada sadece gösterilip
 * butonu kilitliyor; gerçek sınır Edge Function'da (bkz. handleRecommend).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendShareSheet(
    state: RecommendShareUiState,
    mediaTitle: String,
    posterUrl: String?,
    isTvShow: Boolean,
    onToggleFriend: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSend: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Background,
        dragHandle = null,
    ) {
        // Sheet yüksekliği içeriğe göre büyüyor ama liste alanı sınırlı: tek
        // arkadaşı olan kullanıcıda yarım ekran boşluk kalmasın, kalabalık
        // listede de sheet ekranı yutmasın diye liste kendi içinde kayıyor.
        val listHeight = Modifier.heightIn(min = 140.dp, max = 300.dp)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 12.dp),
        ) {
            ShareHeader(mediaTitle = mediaTitle, posterUrl = posterUrl, isTvShow = isTvShow)
            Spacer(Modifier.height(14.dp))
            QuotaBadge(remaining = state.remainingQuota, isOverQuota = state.isOverQuota, selected = state.selectedUids.size)
            Spacer(Modifier.height(14.dp))

            when {
                state.isLoading -> {
                    Box(listHeight.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                    }
                }
                state.friends.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Filled.Group,
                        title = stringResource(R.string.share_no_friends_title),
                        subtitle = stringResource(R.string.share_no_friends_subtitle),
                        modifier = listHeight,
                    )
                }
                state.isQuotaExhausted -> {
                    EmptyState(
                        icon = Icons.Filled.HourglassEmpty,
                        title = stringResource(R.string.share_quota_exhausted_title),
                        subtitle = stringResource(R.string.share_quota_exhausted_subtitle),
                        modifier = listHeight,
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = listHeight,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.friends, key = { it.friendUid }) { friend ->
                            SelectableFriendRow(
                                friend = friend,
                                isSelected = friend.friendUid in state.selectedUids,
                                onClick = { onToggleFriend(friend.friendUid) },
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    NoteField(value = state.note, onValueChange = onNoteChange)
                }
            }

            state.errorMessage?.let { message ->
                Spacer(Modifier.height(10.dp))
                Text(message, color = ErrorColor, style = MaterialTheme.typography.bodySmall)
            }

            if (state.friends.isNotEmpty() && !state.isQuotaExhausted) {
                Spacer(Modifier.height(12.dp))
                SendButton(
                    selectedCount = state.selectedUids.size,
                    enabled = state.canSend,
                    isSending = state.isSending,
                    onClick = onSend,
                )
            }
        }
    }
}

@Composable
private fun ShareHeader(mediaTitle: String, posterUrl: String?, isTvShow: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(66.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(brush = Brush.verticalGradient(listOf(SurfaceVariant, Surface))),
        ) {
            if (posterUrl != null) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (isTvShow) stringResource(R.string.share_header_tv) else stringResource(R.string.share_header_movie),
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                mediaTitle,
                color = OnSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun QuotaBadge(remaining: Int, isOverQuota: Boolean, selected: Int) {
    val text = when {
        isOverQuota -> stringResource(R.string.share_quota_over, selected, remaining)
        remaining <= 0 -> stringResource(R.string.share_quota_none_left)
        else -> stringResource(R.string.share_quota_remaining, remaining)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isOverQuota) ErrorColor.copy(alpha = 0.12f) else Primary.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            color = if (isOverQuota) ErrorColor else Primary,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SelectableFriendRow(friend: Friend, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .then(if (isSelected) Modifier.border(1.5.dp, Primary, RoundedCornerShape(14.dp)) else Modifier)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val preset = avatarPresetById(friend.avatarId)
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(preset.color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(preset.icon, contentDescription = null, tint = preset.color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(friend.fullName, color = OnSurface, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text("@${friend.username}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isSelected) Primary else SurfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.share_selected_cd), tint = OnPrimary, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun NoteField(value: String, onValueChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .border(1.dp, DividerColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) {
            Text(
                stringResource(R.string.share_note_placeholder),
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = OnSurface),
            cursorBrush = SolidColor(Primary),
            maxLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SendButton(selectedCount: Int, enabled: Boolean, isSending: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                brush = if (enabled) {
                    Brush.horizontalGradient(PrimaryGradient)
                } else {
                    Brush.horizontalGradient(listOf(SurfaceVariant, SurfaceVariant))
                },
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isSending) {
            CircularProgressIndicator(color = OnPrimary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        } else {
            Icon(
                Icons.Filled.Send,
                contentDescription = null,
                tint = if (enabled) OnPrimary else TextSecondary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (selectedCount > 0) stringResource(R.string.share_send_with_count, selectedCount) else stringResource(R.string.share_send),
                color = if (enabled) OnPrimary else TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(10.dp))
        Text(title, color = OnSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
}

/**
 * Film/dizi detay ekranında gösterilen dikkat çekici "Arkadaşlarına Tavsiye Et" banner kartı.
 */
@Composable
fun RecommendShareBannerCard(
    onRecommendClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.horizontalGradient(
                    listOf(
                        Primary.copy(alpha = 0.18f),
                        Accent.copy(alpha = 0.14f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(Primary.copy(alpha = 0.6f), Accent.copy(alpha = 0.6f))
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onRecommendClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(PrimaryGradient)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = null,
                        tint = OnPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.share_banner_title),
                        color = OnSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.share_banner_subtitle),
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Primary)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_quick_preview_recommend),
                    color = OnPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

