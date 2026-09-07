package tech.sadique.qayam.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import tech.sadique.qayam.audio.AudioPlayer
import tech.sadique.qayam.data.model.AdhanSoundType
import tech.sadique.qayam.data.model.PrayerType
import tech.sadique.qayam.data.preferences.SettingsRepository
import tech.sadique.qayam.di.ApplicationScope
import tech.sadique.qayam.notification.AlarmScheduler
import tech.sadique.qayam.notification.ExactAlarmGateway
import tech.sadique.qayam.notification.PrayerNotificationNotifier
import tech.sadique.qayam.notification.SchedulePrayerAlarmsUseCase
import tech.sadique.qayam.service.AdhanPlaybackService
import javax.inject.Inject

@AndroidEntryPoint
class AdhanAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    @Inject
    lateinit var schedulePrayerAlarmsUseCase: SchedulePrayerAlarmsUseCase

    @Inject
    lateinit var notifier: PrayerNotificationNotifier

    @Inject
    lateinit var audioPlayer: AudioPlayer

    @Inject
    @ApplicationScope
    lateinit var receiverScope: CoroutineScope

    companion object {
        const val ACTION_ADHAN_ALARM = "tech.sadique.qayam.ACTION_ADHAN_ALARM"
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

        when (action) {
            PrayerNotificationNotifier.ACTION_STOP_ADHAN -> {
                AdhanPlaybackService.stop(context.applicationContext)
                audioPlayer.stopSound()
            }

            ACTION_ADHAN_ALARM -> {
                val prayerId = intent.getStringExtra(ExactAlarmGateway.EXTRA_PRAYER_ID) ?: PrayerType.FAJR.id
                val prayerType = PrayerType.fromId(prayerId)

                val currentSettings = settingsRepository.snapshot()
                val isEnabled = currentSettings.prayerAlertEnabled[prayerType] ?: prayerType.defaultAlertEnabled

                if (isEnabled) {
                    val soundType = currentSettings.prayerAlertSounds[prayerType] ?: AdhanSoundType.MAKKAH
                    val highPriority = currentSettings.highPrioritySound

                    if (soundType != AdhanSoundType.SILENT && soundType != AdhanSoundType.VIBRATE_ONLY) {
                        AdhanPlaybackService.start(
                            context = context.applicationContext,
                            prayerType = prayerType,
                            soundType = soundType,
                            highPriority = highPriority,
                        )
                    } else {
                        if (alarmScheduler.areNotificationsEnabled()) {
                            notifier.showPrayerNotification(prayerType, soundType, highPriority)
                        } else {
                            Log.w(
                                "AdhanReceiver",
                                "POST_NOTIFICATIONS denied; skipping visual alert for ${prayerType.id}",
                            )
                        }
                    }
                }

                // Reschedule for subsequent prayers
                schedulePrayerAlarmsUseCase(currentSettings)
            }
        }
    }
}
