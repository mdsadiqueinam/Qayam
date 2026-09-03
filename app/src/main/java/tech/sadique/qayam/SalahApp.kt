package tech.sadique.qayam

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.os.Build
import androidx.core.content.getSystemService

class SalahApp : Application() {

    companion object {
        const val ADHAN_CHANNEL_ID = "salah_adhan_channel_high_priority"
        const val ADHAN_SILENT_CHANNEL_ID = "salah_adhan_channel_silent"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService<NotificationManager>() ?: return

            // High Priority Channel that can bypass silent mode / alarm usage
            val adhanAudioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val adhanChannel = NotificationChannel(
                ADHAN_CHANNEL_ID,
                "Adhan Prayer Alerts (High Priority)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Plays Adhan audio and high-priority alerts for Salah prayer times"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 1000)
                setSound(null, adhanAudioAttributes) // Audio is played directly via AdhanAudioSynthesizer for full sound control
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
            }

            val silentChannel = NotificationChannel(
                ADHAN_SILENT_CHANNEL_ID,
                "Prayer Alerts (Standard)",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Silent or standard notifications for Salah times"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(adhanChannel)
            notificationManager.createNotificationChannel(silentChannel)
        }
    }
}
