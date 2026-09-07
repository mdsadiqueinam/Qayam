package tech.sadique.qayam.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import tech.sadique.qayam.MainActivity
import tech.sadique.qayam.data.model.AdhanSoundType
import tech.sadique.qayam.data.model.PrayerType
import tech.sadique.qayam.receiver.AdhanAlarmReceiver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerNotificationNotifier @Inject constructor(@ApplicationContext private val context: Context) {
    companion object {
        const val NOTIFICATION_ID_BASE = 1000
        const val ACTION_STOP_ADHAN = "tech.sadique.qayam.ACTION_STOP_ADHAN"
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_PRAYER_ID = "extra_prayer_id"
        private val VIBRATION_PATTERN = longArrayOf(0, 600L, 300L, 600L, 300L, 1200L)
    }

    private val notificationManager = context.getSystemService<NotificationManager>()

    fun buildPrayerNotification(
        prayerType: PrayerType,
        soundType: AdhanSoundType,
        highPriority: Boolean,
    ): Notification {
        val channelId = when (soundType) {
            AdhanSoundType.SILENT -> NotificationChannelManager.ADHAN_SILENT_CHANNEL_ID
            AdhanSoundType.VIBRATE_ONLY -> NotificationChannelManager.ADHAN_VIBRATE_CHANNEL_ID
            else -> NotificationChannelManager.ADHAN_CHANNEL_ID
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PRAYER_ID, prayerType.id)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            prayerType.ordinal,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = Intent(context, AdhanAlarmReceiver::class.java).apply {
            action = ACTION_STOP_ADHAN
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            9999,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        val currentTimeStr = timeFormatter.format(Calendar.getInstance().time)

        val title = if (prayerType.isMainPrayer) {
            "Hayya 'alas-Salah: ${prayerType.displayName} Time"
        } else {
            "${prayerType.displayName} Time"
        }
        val body = if (prayerType.isMainPrayer) {
            "It is now time for ${prayerType.displayName} (${prayerType.arabicName}) prayer • $currentTimeStr"
        } else {
            "It is now time for ${prayerType.displayName} (${prayerType.arabicName}) • $currentTimeStr"
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$body\nMay Allah accept your prayers."))
            .setPriority(if (highPriority) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .setOngoing(false)

        if (soundType != AdhanSoundType.SILENT && soundType != AdhanSoundType.VIBRATE_ONLY) {
            builder.addAction(
                android.R.drawable.ic_media_pause,
                "Stop Adhan Audio",
                stopPendingIntent,
            )
        }

        if (soundType != AdhanSoundType.SILENT) {
            builder.setVibrate(VIBRATION_PATTERN)
        }

        return builder.build()
    }

    fun showPrayerNotification(prayerType: PrayerType, soundType: AdhanSoundType, highPriority: Boolean) {
        val notification = buildPrayerNotification(prayerType, soundType, highPriority)
        notificationManager?.notify(NOTIFICATION_ID_BASE + prayerType.ordinal, notification)
    }

    fun dismissPrayerNotification(prayerType: PrayerType) {
        notificationManager?.cancel(NOTIFICATION_ID_BASE + prayerType.ordinal)
    }
}
