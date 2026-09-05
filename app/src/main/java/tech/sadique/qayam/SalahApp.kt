package tech.sadique.qayam

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.os.Build
import androidx.core.content.getSystemService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SalahApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        const val ADHAN_CHANNEL_ID = "salah_adhan_channel_high_priority"
        const val ADHAN_VIBRATE_CHANNEL_ID = "salah_adhan_channel_vibrate"
        const val ADHAN_SILENT_CHANNEL_ID = "salah_adhan_channel_silent"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        armUpcomingAlarms()
    }

    private fun armUpcomingAlarms() {
        applicationScope.launch {
            try {
                val appSettings = tech.sadique.qayam.data.preferences.AppSettings(this@SalahApp)
                val notificationManager = tech.sadique.qayam.notification.AdhanNotificationManager(this@SalahApp)
                notificationManager.scheduleUpcomingAlarms(appSettings.snapshot())
            } catch (e: Exception) {
                android.util.Log.e("SalahApp", "Failed to schedule upcoming alarms on app launch", e)
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService<NotificationManager>() ?: return

            // High Priority Channel for Audible Adhan
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
                setSound(null, adhanAudioAttributes) // Audio played directly via AdhanAudioSynthesizer / Foreground Service
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
            }

            // High Priority Vibrate-Only Channel
            val vibrateChannel = NotificationChannel(
                ADHAN_VIBRATE_CHANNEL_ID,
                "Prayer Alerts (Vibrate Only)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority haptic vibration alerts for Salah prayer times"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 600, 300, 600, 300, 1200)
                setSound(null, null)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
            }

            // Visual-Only Silent Channel
            val silentChannel = NotificationChannel(
                ADHAN_SILENT_CHANNEL_ID,
                "Prayer Alerts (Visual / Silent)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Visual-only notifications for Salah times without sound or vibration"
                enableVibration(false)
                setSound(null, null)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(adhanChannel)
            notificationManager.createNotificationChannel(vibrateChannel)
            notificationManager.createNotificationChannel(silentChannel)
        }
    }
}
