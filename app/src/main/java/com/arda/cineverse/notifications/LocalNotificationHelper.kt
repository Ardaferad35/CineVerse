package com.arda.cineverse.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.arda.cineverse.MainActivity
import com.arda.cineverse.R

/**
 * Android'in sistem bildirim çubuğuna (kilit ekranında/bildirim panelinde
 * görünen gerçek bildirimler) düşen yerel bildirimleri gösterir. Sunucu
 * tarafı (Cloud Functions/FCM) gerektirmez — tamamen cihaz üzerinde,
 * WorkManager görevleri tarafından tetiklenir.
 */
object LocalNotificationHelper {
    private const val CHANNEL_ID = "cineverse_general"
    private const val CHANNEL_NAME = "CineVerse Bildirimleri"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Günün filmi, yeni vizyona girecekler ve yorum yanıtları"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /** Bildirime dokunulduğunda [MainActivity]'nin bu extra'yı okuyup navigasyon yapması için route string'i. */
    const val EXTRA_DEEP_LINK_ROUTE = "deep_link_route"

    fun show(context: Context, notificationId: Int, title: String, body: String, deepLinkRoute: String? = null) {
        ensureChannel(context)

        // NOT: FLAG_ACTIVITY_CLEAR_TOP kasıtlı olarak KULLANILMIYOR. MainActivity
        // manifest'te launchMode="singleTask" — bu yüzden uygulama zaten açıkken
        // bildirime dokunmak Activity'yi yok edip yeniden yaratmak (onCreate,
        // splash'ın tekrar oynaması, nav state kaybı) yerine mevcut örneğe
        // onNewIntent() ile ulaşır.
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            deepLinkRoute?.let { putExtra(EXTRA_DEEP_LINK_ROUTE, it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // POST_NOTIFICATIONS izni (Android 13+) reddedilmiş olabilir —
        // bu durumda sessizce hiçbir şey göstermiyoruz, uygulama çökmüyor.
        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }
}