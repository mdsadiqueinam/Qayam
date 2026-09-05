package tech.sadique.qayam.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import tech.sadique.qayam.data.preferences.AppSettings
import tech.sadique.qayam.notification.AdhanNotificationManager

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
        // AlarmManager exact-alarm permission change (API 31+); kept as string to
        // avoid referencing S-only constants from all code paths.
        private const val ACTION_SCHEDULE_EXACT_ALARM_STATE_CHANGED =
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "Boot or Time changed action received: $action. Rescheduling alarms...")

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_USER_UNLOCKED,
            ACTION_SCHEDULE_EXACT_ALARM_STATE_CHANGED -> {
                try {
                    val appSettings = AppSettings(context.applicationContext)
                    val notificationManager = AdhanNotificationManager(context.applicationContext)
                    notificationManager.scheduleUpcomingAlarms(appSettings)
                    Log.d(TAG, "Successfully rescheduled all upcoming prayer alarms.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error rescheduling alarms on boot/time change", e)
                }
            }
        }
    }
}
