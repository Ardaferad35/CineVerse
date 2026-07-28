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

    fun show(context: Context, notificationId: Int, title: String, body: String) {
        ensureChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
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