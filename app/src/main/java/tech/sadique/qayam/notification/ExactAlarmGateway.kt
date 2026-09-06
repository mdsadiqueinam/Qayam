package tech.sadique.qayam.notification

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import tech.sadique.qayam.MainActivity
import tech.sadique.qayam.data.model.AdhanSoundType
import tech.sadique.qayam.data.model.PrayerType
import tech.sadique.qayam.receiver.AdhanAlarmReceiver
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExactAlarmGateway @Inject constructor(
    @ApplicationContext private val context: Context,
    private val capabilities: AlarmCapabilities
) : AlarmScheduler {

    constructor(@ApplicationContext context: Context) : this(
        context,
        AlarmCapabilities(context)
    )

    private val alarmManager = context.getSystemService<AlarmManager>()

    companion object {
        private const val TAG = "ExactAlarmGateway"
        const val EXTRA_PRAYER_ID = "extra_prayer_id"
    }

    @SuppressLint("ScheduleExactAlarm")
    override fun setExactAlarm(
        prayer: PrayerType,
        triggerTimeMillis: Long,
        soundType: AdhanSoundType,
        highPriority: Boolean
    ) {
        if (alarmManager == null) return

        val intent = Intent(context, AdhanAlarmReceiver::class.java).apply {
            action = AdhanAlarmReceiver.ACTION_ADHAN_ALARM
            putExtra(EXTRA_PRAYER_ID, prayer.id)
            putExtra("extra_sound_type", soundType.id)
            putExtra("extra_high_priority", highPriority)
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            prayer.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PRAYER_ID, prayer.id)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            prayer.ordinal,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canExact = capabilities.canScheduleExactAlarms()

        try {
            if (canExact) {
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTimeMillis, openAppPendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Alarm scheduled for ${prayer.displayName} at $triggerTimeMillis (exact: $canExact)")
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm permission denied, falling back to inexact alarm", e)
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Failed to schedule fallback alarm", fallbackEx)
            }
        }
    }

    override fun cancelAlarm(prayer: PrayerType) {
        val intent = Intent(context, AdhanAlarmReceiver::class.java).apply {
            action = AdhanAlarmReceiver.ACTION_ADHAN_ALARM
            putExtra(EXTRA_PRAYER_ID, prayer.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            prayer.ordinal,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null && alarmManager != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    override fun scheduleTestAlarm(
        delaySeconds: Int,
        prayerType: PrayerType,
        soundType: AdhanSoundType
    ) {
        val triggerMillis = System.currentTimeMillis() + (delaySeconds * 1000L)
        setExactAlarm(
            prayer = prayerType,
            triggerTimeMillis = triggerMillis,
            soundType = soundType,
            highPriority = true
        )
    }

    override fun canScheduleExactAlarms(): Boolean = capabilities.canScheduleExactAlarms()

    override fun areNotificationsEnabled(): Boolean = capabilities.areNotificationsEnabled()

    override fun isIgnoringBatteryOptimizations(): Boolean = capabilities.isIgnoringBatteryOptimizations()
}
