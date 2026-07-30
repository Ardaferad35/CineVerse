package com.arda.cineverse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arda.cineverse.data.model.AppNotification
import com.arda.cineverse.ui.components.timeAgo
import com.arda.cineverse.ui.theme.Background
import com.arda.cineverse.ui.theme.OnSurface
import com.arda.cineverse.ui.theme.Primary
import com.arda.cineverse.ui.theme.Surface
import com.arda.cineverse.ui.theme.SurfaceVariant
import com.arda.cineverse.ui.theme.TextSecondary
import com.arda.cineverse.viewmodel.NotificationViewModel

@Composable
fun NotificationsScreen(
    onBack: () -> Unit = {},
    onNotificationClick: (AppNotification) -> Unit = {},
    viewModel: NotificationViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // Liste yükleme + "hepsini okundu say" yan etkisi kasıtlı olarak burada,
    // sadece bu ekran gerçekten açıldığında tetikleniyor (bkz.
    // NotificationViewModel.init dokümantasyonu).
    LaunchedEffect(Unit) { viewModel.loadNotifications() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = OnSurface)
            }
            Spacer(Modifier.width(8.dp))
            Text("Bildirimler", color = OnSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            uiState.notifications.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Filled.NotificationsNone, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Henüz bildiriminiz yok", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(uiState.notifications, key = { it.id }) { notification ->
                        NotificationRow(notification = notification, onClick = { onNotificationClick(notification) })
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(notification: AppNotification, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (notification.isRead) Surface else Primary.copy(alpha = 0.08f))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        val icon = iconForType(notification.type)
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(SurfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(notification.title, color = OnSurface, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(notification.body, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            Text(timeAgo(notification.createdAt), color = TextSecondary, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun iconForType(type: String): ImageVector = when (type) {
    "comment_reply" -> Icons.Filled.ChatBubble
    "upcoming_movie" -> Icons.Filled.CalendarMonth
    "film_of_the_day" -> Icons.Filled.MovieFilter
    else -> Icons.Filled.Notifications
}