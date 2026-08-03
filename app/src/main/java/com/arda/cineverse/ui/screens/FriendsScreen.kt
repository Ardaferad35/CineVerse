package com.arda.cineverse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@Composable
fun FriendsScreen(
    onBack: () -> Unit = {},
    viewModel: FriendsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = OnSurface)
                }
                Spacer(Modifier.width(8.dp))
                Text("Arkadaşlar", color = OnSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                if (uiState.searchQuery.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    when {
                        uiState.isSearching -> CircularProgressIndicator(color = Primary, modifier = Modifier.size(20.dp))
                        uiState.searchError != null -> Text(
                            uiState.searchError!!,
                            color = ErrorColor,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        uiState.searchResult != null -> SearchResultRow(
                            result = uiState.searchResult!!,
                            alreadyFriend = uiState.isAlreadyFriend,
                            onSendRequest = { viewModel.sendFriendRequest(uiState.searchResult!!) },
                        )
                        else -> IconButton(onClick = viewModel::search) {
                            Icon(Icons.Filled.Search, contentDescription = "Ara", tint = Primary)
                        }
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
private fun SearchResultRow(result: FriendSearchResult, alreadyFriend: Boolean, onSendRequest: () -> Unit) {
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
        if (alreadyFriend) {
            Text("Arkadaşsınız", color = Primary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        } else {
            IconButton(onClick = onSendRequest) {
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
