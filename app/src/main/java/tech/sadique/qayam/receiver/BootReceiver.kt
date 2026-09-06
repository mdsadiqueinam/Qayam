package tech.sadique.qayam.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import tech.sadique.qayam.data.preferences.SettingsRepository
import tech.sadique.qayam.di.ApplicationScope
import tech.sadique.qayam.notification.SchedulePrayerAlarmsUseCase
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var schedulePrayerAlarmsUseCase: SchedulePrayerAlarmsUseCase

    @Inject
    @ApplicationScope
    lateinit var receiverScope: CoroutineScope

    companion object {
        private const val TAG = "BootReceiver"

        // AlarmManager exact-alarm permission change (API 31+); kept as string to
        // avoid referencing S-only constants from all code paths.
        private const val ACTION_SCHEDULE_EXACT_ALARM_STATE_CHANGED =
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
    }

    @Suppress("TooGenericExceptionCaught")
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "Boot or Time changed action received: $action. Rescheduling alarms...")

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            "android.intent.action.TIME_SET",
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_USER_UNLOCKED,
            ACTION_SCHEDULE_EXACT_ALARM_STATE_CHANGED,
            -> {
                val pendingResult = goAsync()
                receiverScope.launch {
                    try {
                        schedulePrayerAlarmsUseCase(settingsRepository.snapshot())
                        Log.d(TAG, "Successfully rescheduled all upcoming prayer alarms.")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error rescheduling alarms on boot/time change", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
