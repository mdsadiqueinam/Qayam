package tech.sadique.qayam.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import tech.sadique.qayam.audio.AdhanAudioSynthesizer
import tech.sadique.qayam.data.model.AdhanSoundType
import tech.sadique.qayam.data.model.PrayerType
import tech.sadique.qayam.data.preferences.AppSettings
import tech.sadique.qayam.notification.AdhanNotificationManager

import tech.sadique.qayam.service.AdhanPlaybackService

class AdhanAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ADHAN_ALARM = "tech.sadique.qayam.ACTION_ADHAN_ALARM"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d("AdhanReceiver", "Received action: $action")

        val appSettings = AppSettings(context.applicationContext)
        val notificationManager = AdhanNotificationManager(context.applicationContext)

        when (action) {
            AdhanNotificationManager.ACTION_STOP_ADHAN -> {
                AdhanPlaybackService.stop(context.applicationContext)
                AdhanAudioSynthesizer.stopSound()
            }

            ACTION_ADHAN_ALARM -> {
                val prayerId = intent.getStringExtra(AdhanNotificationManager.EXTRA_PRAYER_ID) ?: PrayerType.FAJR.id
                val prayerType = PrayerType.fromId(prayerId)

                val currentSettings = appSettings.settings.value
                val isEnabled = currentSettings.prayerAlertEnabled[prayerType] ?: true

                if (isEnabled) {
                    val soundType = currentSettings.prayerAlertSounds[prayerType] ?: AdhanSoundType.MAKKAH
                    val highPriority = currentSettings.highPrioritySound

                    if (soundType != AdhanSoundType.SILENT && soundType != AdhanSoundType.VIBRATE_ONLY) {
                        AdhanPlaybackService.start(
                            context = context.applicationContext,
                            prayerType = prayerType,
                            soundType = soundType,
                            highPriority = highPriority
                        )
                    } else {
                        notificationManager.showPrayerNotification(prayerType, soundType, highPriority)
                    }
                }

                // Reschedule for subsequent prayers
                notificationManager.scheduleUpcomingAlarms(appSettings)
            }
        }
    }
}
