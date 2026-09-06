package tech.sadique.qayam

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import tech.sadique.qayam.data.preferences.SettingsRepository
import tech.sadique.qayam.di.ApplicationScope
import tech.sadique.qayam.notification.NotificationChannelManager
import tech.sadique.qayam.notification.SchedulePrayerAlarmsUseCase
import javax.inject.Inject

@HiltAndroidApp
class QayamApp : Application() {

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var schedulePrayerAlarmsUseCase: SchedulePrayerAlarmsUseCase

    @Inject
    lateinit var notificationChannelManager: NotificationChannelManager

    companion object {
        const val ADHAN_CHANNEL_ID = NotificationChannelManager.ADHAN_CHANNEL_ID
        const val ADHAN_VIBRATE_CHANNEL_ID = NotificationChannelManager.ADHAN_VIBRATE_CHANNEL_ID
        const val ADHAN_SILENT_CHANNEL_ID = NotificationChannelManager.ADHAN_SILENT_CHANNEL_ID
    }

    override fun onCreate() {
        super.onCreate()
        notificationChannelManager.createNotificationChannels()
        armUpcomingAlarms()
    }

    private fun armUpcomingAlarms() {
        applicationScope.launch {
            try {
                schedulePrayerAlarmsUseCase(settingsRepository.snapshot())
            } catch (e: Exception) {
                Log.e("QayamApp", "Failed to schedule upcoming alarms on app launch", e)
            }
        }
    }
}
