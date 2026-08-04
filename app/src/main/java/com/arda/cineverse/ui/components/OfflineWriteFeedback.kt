package com.arda.cineverse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.arda.cineverse.data.common.GENERIC_WRITE_FAILURE_MESSAGE
import com.arda.cineverse.data.common.OfflineWriteException
import com.arda.cineverse.ui.theme.Surface
import com.arda.cineverse.ui.theme.TextSecondary
import kotlinx.coroutines.delay

private const val OFFLINE_MESSAGE_DURATION_MS = 2500L

/**
 * Sadece Composable içinde local state ile favori/izleme listesi toggle'ı
 * yapan ekranlar (ViewModel'e bağlı olmayan yerler) için: offline'da yazma
 * başarısız olduğunda iyimser UI değişikliğini geri alır ve kullanıcıya
 * kısa süreli bir mesaj gösterir. ViewModel'e bağlı ekranlarde bunun yerine
 * ilgili UiState'teki `offlineMessage` alanı + [OfflineActionSnackbar] kullanılır.
 */
@Stable
class OfflineWriteMessageState {
    var message by mutableStateOf<String?>(null)
        private set

    /**
     * Yazma hatasında [revert]'i çalıştırıp kullanıcıya nedenini gösterir.
     * Hata offline yazma hatası olmasa da (ör. Firestore izin/zaman aşımı)
     * geri alma yapılır — aksi halde ikon "kaydedildi" gösterip yazma
     * gerçekleşmemiş olurdu. Offline hatasıysa true döner.
     */
    fun handle(error: Throwable, revert: () -> Unit): Boolean {
        revert()
        message = if (error is OfflineWriteException) error.message else GENERIC_WRITE_FAILURE_MESSAGE
        return error is OfflineWriteException
    }

    fun clear() {
        message = null
    }
}

@Composable
fun rememberOfflineWriteMessageState(): OfflineWriteMessageState {
    val state = remember { OfflineWriteMessageState() }
    LaunchedEffect(state.message) {
        if (state.message != null) {
            delay(OFFLINE_MESSAGE_DURATION_MS)
            state.clear()
        }
    }
    return state
}

/** [message] belli bir süre sonra otomatik temizlensin diye [onClear]'ı çağırır — ViewModel'e bağlı ekranlar için. */
@Composable
fun ClearOfflineMessageAfterDelay(message: String?, onClear: () -> Unit) {
    LaunchedEffect(message) {
        if (message != null) {
            delay(OFFLINE_MESSAGE_DURATION_MS)
            onClear()
        }
    }
}

/** Offline yazma hatasını bildiren, ekranın altında yüzen küçük bir bilgi kutusu. */
@Composable
fun OfflineActionSnackbar(message: String?, modifier: Modifier = Modifier) {
    if (message != null) {
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(message, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}
