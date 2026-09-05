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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AdhanAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ADHAN_ALARM = "tech.sadique.qayam.ACTION_ADHAN_ALARM"
        private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        receiverScope.launch {
            try {
                handleIntent(context, intent)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleIntent(context: Context, intent: Intent) {
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

                val currentSettings = appSettings.snapshot()
                val isEnabled = currentSettings.prayerAlertEnabled[prayerType] ?: prayerType.defaultAlertEnabled

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
                        if (notificationManager.areNotificationsEnabled()) {
                            notificationManager.showPrayerNotification(prayerType, soundType, highPriority)
                        } else {
                            Log.w("AdhanReceiver", "POST_NOTIFICATIONS denied; skipping visual alert for ${prayerType.id}")
                        }
                    }
                }

                // Reschedule for subsequent prayers
                notificationManager.scheduleUpcomingAlarms(currentSettings)
            }
        }
    }
}
