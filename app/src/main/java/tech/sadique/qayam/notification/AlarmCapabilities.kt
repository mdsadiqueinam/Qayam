package tech.sadique.qayam.notification

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmCapabilities @Inject constructor(@ApplicationContext private val context: Context) {
    private val alarmManager = context.getSystemService<AlarmManager>()
    private val notificationManager = context.getSystemService<NotificationManager>()
    private val powerManager = context.getSystemService<PowerManager>()

    fun canScheduleExactAlarms(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        alarmManager?.canScheduleExactAlarms() ?: false
    } else {
        true
    }

    fun areNotificationsEnabled(): Boolean {
        val nm = notificationManager ?: return false
        return nm.areNotificationsEnabled()
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = powerManager ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }
}
