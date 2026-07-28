package com.arda.cineverse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arda.cineverse.ui.components.avatarPresetById
import com.arda.cineverse.ui.components.avatarPresets
import com.arda.cineverse.ui.theme.Background
import com.arda.cineverse.ui.theme.DividerColor
import com.arda.cineverse.ui.theme.ErrorColor
import com.arda.cineverse.ui.theme.OnPrimary
import com.arda.cineverse.ui.theme.OnSurface
import com.arda.cineverse.ui.theme.Primary
import com.arda.cineverse.ui.theme.Surface
import com.arda.cineverse.ui.theme.SurfaceVariant
import com.arda.cineverse.ui.theme.TextSecondary
import com.arda.cineverse.ui.theme.ThemeState
import com.arda.cineverse.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    onBack: () -> Unit = {},
    onSignedOut: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    var showSignOutConfirm by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showComingSoonDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }

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
            Text("Profil", color = OnSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(Modifier.height(8.dp))

                // Profil kartı
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Surface)
                        .padding(20.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val preset = avatarPresetById(uiState.avatarId)
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(preset.color.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(preset.icon, contentDescription = null, tint = preset.color, modifier = Modifier.size(34.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(uiState.fullName, color = OnSurface, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(2.dp))
                            Text(uiState.email, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Primary.copy(alpha = 0.18f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text("CineVerse Üyesi", color = Primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceVariant)
                            .clickable { showEditNameDialog = true }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, tint = OnSurface, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Profili Düzenle", color = OnSurface, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // İstatistikler
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Surface)
                        .padding(vertical = 16.dp),
                ) {
                    ProfileStat(
                        icon = Icons.Filled.Favorite,
                        iconTint = ErrorColor,
                        value = "${uiState.favoritesCount}",
                        label = "Favoriler",
                        modifier = Modifier.weight(1f),
                    )
                    ProfileStat(
                        icon = Icons.Filled.Bookmark,
                        iconTint = Primary,
                        value = "${uiState.watchlistCount}",
                        label = "İzleme Listem",
                        modifier = Modifier.weight(1f),
                    )
                    ProfileStat(
                        icon = Icons.Filled.Star,
                        iconTint = Color(0xFFFFC857),
                        value = if (uiState.ratingsGivenCount > 0) "${uiState.averageRatingGiven}" else "—",
                        label = "Ortalama Puanım",
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text("Avatar Seç", color = OnSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(avatarPresets, key = { it.id }) { preset ->
                        val isSelected = preset.id == uiState.avatarId
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(preset.color.copy(alpha = 0.18f))
                                .then(
                                    if (isSelected) {
                                        Modifier.border(2.dp, preset.color, CircleShape)
                                    } else {
                                        Modifier
                                    },
                                )
                                .clickable { viewModel.updateAvatar(preset.id) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(preset.icon, contentDescription = null, tint = preset.color, modifier = Modifier.size(26.dp))
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Ayarlar listesi
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Surface)
                        .padding(horizontal = 8.dp),
                ) {
                    SettingsRow(
                        icon = Icons.Filled.Lock,
                        title = "Şifre Değiştir",
                        subtitle = "Hesap şifrenizi güncelleyin",
                        onClick = { showChangePasswordDialog = true },
                    )
                    HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                    SettingsRow(
                        icon = Icons.Filled.Palette,
                        title = "Görünüm",
                        subtitle = "Tema ve görünüm seçimi",
                        trailingText = if (ThemeState.isDarkTheme) "Koyu" else "Açık",
                        onClick = { showThemeDialog = true },
                    )
                    HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                    SettingsRowWithSwitch(
                        icon = Icons.Filled.Info,
                        title = "Bildirimler",
                        subtitle = "Bildirim tercihlerinizi yönetin",
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it },
                    )
                    HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                    SettingsRow(
                        icon = Icons.Filled.Help,
                        title = "Yardım & Destek",
                        subtitle = "Yardım alın ve bizimle iletişime geçin",
                        onClick = { showComingSoonDialog = true },
                    )
                    HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                    SettingsRow(
                        icon = Icons.Filled.Info,
                        title = "CineVerse Hakkında",
                        subtitle = "Uygulama sürümü ve bilgileri",
                        onClick = { showAboutDialog = true },
                    )
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ErrorColor.copy(alpha = 0.1f))
                        .border(1.dp, ErrorColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .clickable { showSignOutConfirm = true }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = ErrorColor, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Çıkış Yap", color = ErrorColor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("Çıkış yap") },
            text = { Text("Hesabınızdan çıkış yapmak istediğinize emin misiniz?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.signOut()
                    showSignOutConfirm = false
                    onSignedOut()
                }) { Text("Çıkış Yap", color = ErrorColor) }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) { Text("Vazgeç") }
            },
        )
    }

    if (showEditNameDialog) {
        var nameInput by remember { mutableStateOf(uiState.fullName) }
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Adını düzenle") },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    singleLine = true,
                    placeholder = { Text("Adınız") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateFullName(nameInput.trim())
                    showEditNameDialog = false
                }) { Text("Kaydet", color = Primary) }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) { Text("Vazgeç") }
            },
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("CineVerse Hakkında") },
            text = { Text("CineVerse — sürüm 1.0.0\n\nKotlin ve Jetpack Compose ile geliştirilen, yapay zekâ destekli film keşif uygulaması.") },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("Kapat", color = Primary) }
            },
        )
    }

    if (showComingSoonDialog) {
        AlertDialog(
            onDismissRequest = { showComingSoonDialog = false },
            title = { Text("Yakında") },
            text = { Text("Bu özellik henüz hazır değil, yakında eklenecek.") },
            confirmButton = {
                TextButton(onClick = { showComingSoonDialog = false }) { Text("Tamam", color = Primary) }
            },
        )
    }

    if (showThemeDialog) {
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Görünüm") },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                ThemeState.setDarkTheme(context, true)
                                showThemeDialog = false
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = ThemeState.isDarkTheme, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Koyu", color = OnSurface)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                ThemeState.setDarkTheme(context, false)
                                showThemeDialog = false
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = !ThemeState.isDarkTheme, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Açık", color = OnSurface)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Kapat", color = Primary) }
            },
        )
    }

    if (showChangePasswordDialog) {
        var currentPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var isSubmitting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showChangePasswordDialog = false },
            title = { Text("Şifre Değiştir") },
            text = {
                Column {
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it; errorMessage = null },
                        label = { Text("Mevcut şifre") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it; errorMessage = null },
                        label = { Text("Yeni şifre") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; errorMessage = null },
                        label = { Text("Yeni şifre (tekrar)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (errorMessage != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(errorMessage!!, color = ErrorColor, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isSubmitting,
                    onClick = {
                        when {
                            currentPassword.isBlank() -> errorMessage = "Mevcut şifrenizi girin"
                            newPassword.length < 6 -> errorMessage = "Yeni şifre en az 6 karakter olmalı"
                            newPassword != confirmPassword -> errorMessage = "Yeni şifreler eşleşmiyor"
                            else -> {
                                isSubmitting = true
                                viewModel.changePassword(currentPassword, newPassword) { success, error ->
                                    isSubmitting = false
                                    if (success) {
                                        showChangePasswordDialog = false
                                    } else {
                                        errorMessage = error
                                    }
                                }
                            }
                        }
                    },
                ) {
                    Text(if (isSubmitting) "Kaydediliyor..." else "Kaydet", color = Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { if (!isSubmitting) showChangePasswordDialog = false }) { Text("Vazgeç") }
            },
        )
    }
}

@Composable
private fun ProfileStat(
    icon: ImageVector,
    iconTint: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(6.dp))
        Text(value, color = OnSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailingText: String? = null,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(SurfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = OnSurface, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
        }
        if (trailingText != null) {
            Text(trailingText, color = Primary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(4.dp))
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SettingsRowWithSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(SurfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = OnSurface, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Primary, checkedThumbColor = OnPrimary),
        )
    }
}