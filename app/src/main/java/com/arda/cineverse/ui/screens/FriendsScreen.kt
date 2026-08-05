package com.arda.cineverse.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.arda.cineverse.R
import com.arda.cineverse.data.model.Friend
import com.arda.cineverse.data.model.FriendActivity
import com.arda.cineverse.data.model.FriendActivityType
import com.arda.cineverse.data.model.FriendRequest
import com.arda.cineverse.data.model.FriendSearchResult
import com.arda.cineverse.ui.components.ClearOfflineMessageAfterDelay
import com.arda.cineverse.ui.components.CVTextField
import com.arda.cineverse.ui.components.ListTabButton
import com.arda.cineverse.ui.components.OfflineActionSnackbar
import com.arda.cineverse.ui.components.avatarPresetById
import com.arda.cineverse.ui.theme.Accent
import com.arda.cineverse.ui.theme.Background
import com.arda.cineverse.ui.theme.DividerColor
import com.arda.cineverse.ui.theme.ErrorColor
import com.arda.cineverse.ui.theme.OnSurface
import com.arda.cineverse.ui.theme.Primary
import com.arda.cineverse.ui.theme.Surface
import com.arda.cineverse.ui.theme.SurfaceVariant
import com.arda.cineverse.ui.theme.TextSecondary
import com.arda.cineverse.ui.theme.Warning
import com.arda.cineverse.viewmodel.FriendsTab
import com.arda.cineverse.viewmodel.FriendsViewModel

@Composable
fun FriendsScreen(
    onBack: () -> Unit = {},
    onMovieClick: (Int) -> Unit = {},
    onTvShowClick: (Int) -> Unit = {},
    viewModel: FriendsViewModel = hiltViewModel(),
) {
    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        FriendsContent(
            isPanel = false,
            onClose = onBack,
            onMovieClick = onMovieClick,
            onTvShowClick = onTvShowClick,
            viewModel = viewModel,
        )
    }
}

@Composable
fun FriendsPanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    onMovieClick: (Int) -> Unit = {},
    onTvShowClick: (Int) -> Unit = {},
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
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                FriendsContent(
                    isPanel = true,
                    onClose = onDismiss,
                    onMovieClick = onMovieClick,
                    onTvShowClick = onTvShowClick,
                )
            }
        }
    }
}

private const val PANEL_WIDTH_FRACTION = 0.85f

@Composable
private fun FriendsContent(
    isPanel: Boolean,
    onClose: () -> Unit,
    onMovieClick: (Int) -> Unit = {},
    onTvShowClick: (Int) -> Unit = {},
    viewModel: FriendsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var friendPendingDelete by remember { mutableStateOf<Friend?>(null) }

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
                        stringResource(R.string.friends_title),
                        color = OnSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.common_close), tint = OnSurface)
                    }
                } else {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = OnSurface)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.friends_title), color = OnSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(8.dp))
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                CVTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    placeholder = stringResource(R.string.friends_search_placeholder),
                    leadingIcon = { Icon(Icons.Filled.PersonSearch, contentDescription = null, tint = TextSecondary) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                )
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
                    text = stringResource(R.string.friends_tab_friends),
                    icon = Icons.Filled.PersonAdd,
                    selected = uiState.selectedTab == FriendsTab.FRIENDS,
                    onClick = { viewModel.selectTab(FriendsTab.FRIENDS) },
                    modifier = Modifier.weight(1f),
                )
                ListTabButton(
                    text = stringResource(R.string.friends_tab_requests) + if (uiState.incomingRequests.isNotEmpty()) {
                        stringResource(R.string.friends_tab_requests_count_suffix, uiState.incomingRequests.size)
                    } else {
                        ""
                    },
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
                        EmptyState(text = stringResource(R.string.friends_empty_friends))
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(uiState.friends, key = { it.friendUid }) { friend ->
                                FriendRow(
                                    friend = friend,
                                    onClick = { viewModel.selectFriendForActivity(friend) },
                                    onRemove = { friendPendingDelete = friend },
                                )
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
                        EmptyState(text = stringResource(R.string.friends_empty_requests))
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

        // Arkadaş aktivite detayı Sheet
        uiState.selectedFriendForActivity?.let { friend ->
            FriendActivityBottomSheet(
                friend = friend,
                activities = uiState.friendActivities,
                isLoading = uiState.isLoadingActivities,
                onMovieClick = { movieId ->
                    onClose()
                    onMovieClick(movieId)
                },
                onTvShowClick = { tvId ->
                    onClose()
                    onTvShowClick(tvId)
                },
                onDismiss = { viewModel.selectFriendForActivity(null) },
            )
        }

        // Arkadaş silme onay diyalogu
        friendPendingDelete?.let { friend ->
            AlertDialog(
                onDismissRequest = { friendPendingDelete = null },
                title = { Text(stringResource(R.string.friends_remove_confirm_title)) },
                text = { Text(stringResource(R.string.friends_remove_confirm_body, friend.fullName)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.removeFriend(friend.friendUid)
                            friendPendingDelete = null
                        },
                    ) {
                        Text(stringResource(R.string.friends_remove_action), color = ErrorColor)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { friendPendingDelete = null }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                },
            )
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
                stringResource(R.string.friends_already_friends),
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
                    stringResource(R.string.friends_request_sent_label),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            else -> IconButton(onClick = onSendRequest) {
                Icon(Icons.Filled.PersonAdd, contentDescription = stringResource(R.string.friends_send_request_cd), tint = Primary)
            }
        }
    }
}

@Composable
private fun FriendRow(
    friend: Friend,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
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
            Text(stringResource(R.string.friends_row_subtitle, friend.username), color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.PersonRemove, contentDescription = stringResource(R.string.friends_remove_cd), tint = ErrorColor.copy(alpha = 0.8f))
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
            Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.friends_accept_cd), tint = Primary)
        }
        IconButton(onClick = onDecline) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.friends_decline_cd), tint = ErrorColor)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FriendActivityBottomSheet(
    friend: Friend,
    activities: List<FriendActivity>,
    isLoading: Boolean,
    onMovieClick: (Int) -> Unit,
    onTvShowClick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Background,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 28.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val preset = avatarPresetById(friend.avatarId)
                    Box(
                        modifier = Modifier.size(46.dp).clip(CircleShape).background(preset.color.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(preset.icon, contentDescription = null, tint = preset.color, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(friend.fullName, color = OnSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("@${friend.username}", color = TextSecondary, fontSize = 13.sp)
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.common_close), tint = TextSecondary)
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.friends_activity_sheet_subtitle),
                color = OnSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(Modifier.height(12.dp))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = Primary, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                    }
                }
                activities.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Filled.FavoriteBorder, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(10.dp))
                        Text(
                            stringResource(R.string.friends_activity_empty),
                            color = TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(activities, key = { it.id }) { activity ->
                            FriendActivityItem(
                                activity = activity,
                                onClick = {
                                    onDismiss()
                                    if (activity.mediaType == "tv") {
                                        onTvShowClick(activity.mediaId)
                                    } else {
                                        onMovieClick(activity.mediaId)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendActivityItem(
    activity: FriendActivity,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .border(1.dp, DividerColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (activity.posterUrl != null) {
            AsyncImage(
                model = activity.posterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 46.dp, height = 66.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(width = 46.dp, height = 66.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Theaters, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (badgeIcon, badgeTint, label) = when (activity.type) {
                    FriendActivityType.FAVORITE -> Triple(Icons.Filled.Favorite, ErrorColor, stringResource(R.string.friends_activity_favorite))
                    FriendActivityType.WATCHLIST -> Triple(Icons.Filled.Bookmark, Primary, stringResource(R.string.friends_activity_watchlist))
                    FriendActivityType.COMMENT -> Triple(Icons.Filled.ChatBubble, Accent, stringResource(R.string.friends_activity_comment))
                    FriendActivityType.RECOMMENDATION -> Triple(Icons.AutoMirrored.Filled.Send, Warning, stringResource(R.string.friends_activity_recommendation))
                }

                Icon(badgeIcon, contentDescription = null, tint = badgeTint, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(label, color = badgeTint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = activity.mediaTitle,
                color = OnSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )

            if (!activity.commentText.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "\"${activity.commentText}\"",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 2,
                )
            }

            if (activity.rating != null && activity.rating > 0) {
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = Warning, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("${activity.rating}", color = OnSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

