package com.arda.cineverse.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arda.cineverse.data.model.Friend
import com.arda.cineverse.data.model.FriendRequest
import com.arda.cineverse.data.model.FriendSearchResult
import com.arda.cineverse.ui.components.ClearOfflineMessageAfterDelay
import com.arda.cineverse.ui.components.CVTextField
import com.arda.cineverse.ui.components.ListTabButton
import com.arda.cineverse.ui.components.OfflineActionSnackbar
import com.arda.cineverse.ui.components.avatarPresetById
import com.arda.cineverse.ui.theme.Background
import com.arda.cineverse.ui.theme.ErrorColor
import com.arda.cineverse.ui.theme.OnSurface
import com.arda.cineverse.ui.theme.Primary
import com.arda.cineverse.ui.theme.Surface
import com.arda.cineverse.ui.theme.TextSecondary
import com.arda.cineverse.viewmodel.FriendsTab
import com.arda.cineverse.viewmodel.FriendsViewModel

/**
 * Tam ekran arkadaşlar rotası. Ana sayfadaki kişiler butonu artık bunun
 * yerine [FriendsPanel]'i açıyor; bu rota, arkadaşlık isteği push bildirimine
 * tıklanınca (route = "friends", bkz. supabase/functions/friend-push) ve
 * Profil ekranındaki girişten geliniyor — o yüzden duruyor. İkisi de aynı
 * [FriendsContent]'i gösteriyor.
 */
@Composable
fun FriendsScreen(
    onBack: () -> Unit = {},
    viewModel: FriendsViewModel = viewModel(),
) {
    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        FriendsContent(isPanel = false, onClose = onBack, viewModel = viewModel)
    }
}

/**
 * Ana sayfanın üzerine sağdan kayarak gelen arkadaşlar paneli. Genişlik
 * [PANEL_WIDTH_FRACTION] — yarım ekranda arkadaş satırları (40dp avatar +
 * isim + @kullanıcı adı + aksiyon butonu) sığmıyor, isimler kesiliyordu.
 * Arkada kalan ana sayfa karartılıyor ve karartıya dokunmak paneli kapatıyor.
 */
@Composable
fun FriendsPanel(
    visible: Boolean,
    onDismiss: () -> Unit,
) {
    if (visible) BackHandler(onBack = onDismiss)

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.matchParentSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(PANEL_WIDTH_FRACTION)
                    .fillMaxHeight()
                    .background(Background)
                    // Panelin kendi üstüne yapılan dokunuşlar karartıya
                    // (dolayısıyla kapatmaya) geçmesin.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                FriendsContent(isPanel = true, onClose = onDismiss)
            }
        }
    }
}

private const val PANEL_WIDTH_FRACTION = 0.85f

@Composable
private fun FriendsContent(
    isPanel: Boolean,
    onClose: () -> Unit,
    viewModel: FriendsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // Panel her açıldığında yeniden composition'a girer; ViewModel ana sayfa
    // giriş noktasına bağlı olduğu için hayatta kalır, bu yüzden gelen
    // istekleri burada tazeliyoruz.
    LaunchedEffect(Unit) { viewModel.refresh() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isPanel) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Arkadaşlar",
                        color = OnSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Kapat", tint = OnSurface)
                    }
                } else {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = OnSurface)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Arkadaşlar", color = OnSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(8.dp))
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                CVTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    placeholder = "Kullanıcı adıyla ara",
                    leadingIcon = { Icon(Icons.Filled.PersonSearch, contentDescription = null, tint = TextSecondary) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                )
                // Arama yazdıkça otomatik yapılıyor (bkz.
                // FriendsViewModel.onSearchQueryChange) — ayrı bir "Ara"
                // butonu yok.
                if (uiState.searchQuery.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    when {
                        uiState.isSearching -> CircularProgressIndicator(color = Primary, modifier = Modifier.size(20.dp))
                        uiState.searchResult != null -> SearchResultRow(
                            result = uiState.searchResult!!,
                            alreadyFriend = uiState.isAlreadyFriend,
                            requestSent = uiState.isRequestSentToResult,
                            isSending = uiState.sendingRequestUid == uiState.searchResult!!.uid,
                            onSendRequest = { viewModel.sendFriendRequest(uiState.searchResult!!) },
                        )
                        uiState.searchError != null -> Text(
                            uiState.searchError!!,
                            color = ErrorColor,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ListTabButton(
                    text = "Arkadaşlarım",
                    icon = Icons.Filled.PersonAdd,
                    selected = uiState.selectedTab == FriendsTab.FRIENDS,
                    onClick = { viewModel.selectTab(FriendsTab.FRIENDS) },
                    modifier = Modifier.weight(1f),
                )
                ListTabButton(
                    text = "Gelen İstekler" + if (uiState.incomingRequests.isNotEmpty()) " (${uiState.incomingRequests.size})" else "",
                    icon = Icons.Filled.PersonSearch,
                    selected = uiState.selectedTab == FriendsTab.REQUESTS,
                    onClick = { viewModel.selectTab(FriendsTab.REQUESTS) },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(16.dp))

            when (uiState.selectedTab) {
                FriendsTab.FRIENDS -> {
                    if (uiState.friends.isEmpty()) {
                        EmptyState(text = "Henüz arkadaşınız yok. Kullanıcı adıyla arayıp istek gönderebilirsiniz.")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(uiState.friends, key = { it.friendUid }) { friend ->
                                FriendRow(friend = friend, onRemove = { viewModel.removeFriend(friend.friendUid) })
                            }
                        }
                    }
                }
                FriendsTab.REQUESTS -> {
                    if (uiState.isLoadingRequests) {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Primary)
                        }
                    } else if (uiState.incomingRequests.isEmpty()) {
                        EmptyState(text = "Gelen arkadaşlık isteği yok.")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(uiState.incomingRequests, key = { it.fromUid }) { request ->
                                FriendRequestRow(
                                    request = request,
                                    onAccept = { viewModel.acceptRequest(request) },
                                    onDecline = { viewModel.declineRequest(request.fromUid) },
                                )
                            }
                        }
                    }
                }
            }
        }

        ClearOfflineMessageAfterDelay(uiState.actionMessage) { viewModel.clearActionMessage() }
        uiState.actionMessage?.let {
            OfflineActionSnackbar(
                message = it,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text(text, color = TextSecondary, style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun SearchResultRow(
    result: FriendSearchResult,
    alreadyFriend: Boolean,
    requestSent: Boolean,
    isSending: Boolean,
    onSendRequest: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val preset = avatarPresetById(result.avatarId)
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(preset.color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(preset.icon, contentDescription = null, tint = preset.color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(result.fullName, color = OnSurface, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text("@${result.username}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        when {
            alreadyFriend -> Text(
                "Arkadaşsınız",
                color = Primary,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
            isSending -> CircularProgressIndicator(
                color = Primary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
            )
            requestSent -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    "Gönderildi",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            else -> IconButton(onClick = onSendRequest) {
                Icon(Icons.Filled.PersonAdd, contentDescription = "İstek gönder", tint = Primary)
            }
        }
    }
}

@Composable
private fun FriendRow(friend: Friend, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
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
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.PersonRemove, contentDescription = "Arkadaşlıktan çıkar", tint = ErrorColor)
        }
    }
}

@Composable
private fun FriendRequestRow(request: FriendRequest, onAccept: () -> Unit, onDecline: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val preset = avatarPresetById(request.fromAvatarId)
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(preset.color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(preset.icon, contentDescription = null, tint = preset.color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(request.fromFullName, color = OnSurface, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text("@${request.fromUsername}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = onAccept) {
            Icon(Icons.Filled.Check, contentDescription = "Kabul et", tint = Primary)
        }
        IconButton(onClick = onDecline) {
            Icon(Icons.Filled.Close, contentDescription = "Reddet", tint = ErrorColor)
        }
    }
}
