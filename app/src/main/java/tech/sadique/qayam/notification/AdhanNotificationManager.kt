package tech.sadique.qayam.notification

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import tech.sadique.qayam.MainActivity
import tech.sadique.qayam.SalahApp
import tech.sadique.qayam.audio.AdhanAudioSynthesizer
import tech.sadique.qayam.data.calculator.PrayerTimeCalculator
import tech.sadique.qayam.data.model.AdhanSoundType
import tech.sadique.qayam.data.model.PrayerSchedule
import tech.sadique.qayam.data.model.PrayerType
import tech.sadique.qayam.data.preferences.AppSettings
import tech.sadique.qayam.receiver.AdhanAlarmReceiver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AdhanNotificationManager(private val context: Context) {

    private val notificationManager = context.getSystemService<NotificationManager>()
    private val alarmManager = context.getSystemService<AlarmManager>()

    companion object {
        const val NOTIFICATION_ID_BASE = 1000
        const val ACTION_STOP_ADHAN = "tech.sadique.qayam.ACTION_STOP_ADHAN"
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_PRAYER_ID = "extra_prayer_id"
    }

    fun showPrayerNotification(prayerType: PrayerType, soundType: AdhanSoundType, highPriority: Boolean) {
        val channelId = if (soundType == AdhanSoundType.SILENT) {
            SalahApp.ADHAN_SILENT_CHANNEL_ID
        } else {
            SalahApp.ADHAN_CHANNEL_ID
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PRAYER_ID, prayerType.id)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            prayerType.ordinal,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, AdhanAlarmReceiver::class.java).apply {
            action = ACTION_STOP_ADHAN
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            9999,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        val currentTimeStr = timeFormatter.format(Calendar.getInstance().time)

        val title = if (prayerType.isMainPrayer) {
            "Hayya 'alas-Salah: ${prayerType.displayName} Time"
        } else {
            "${prayerType.displayName} Time"
        }
        val body = if (prayerType.isMainPrayer) {
            "It is now time for ${prayerType.displayName} (${prayerType.arabicName}) prayer • $currentTimeStr"
        } else {
            "It is now time for ${prayerType.displayName} (${prayerType.arabicName}) • $currentTimeStr"
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$body\nMay Allah accept your prayers."))
            .setPriority(if (highPriority) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .setOngoing(false)

        if (soundType != AdhanSoundType.SILENT && soundType != AdhanSoundType.VIBRATE_ONLY) {
            builder.addAction(
                android.R.drawable.ic_media_pause,
                "Stop Adhan Audio",
                stopPendingIntent
            )
        }

        if (soundType == AdhanSoundType.VIBRATE_ONLY || soundType != AdhanSoundType.SILENT) {
            builder.setVibrate(longArrayOf(0, 600, 300, 600, 300, 1200))
        }

        notificationManager?.notify(NOTIFICATION_ID_BASE + prayerType.ordinal, builder.build())

        // Play the synthesized or system Adhan audio
        if (soundType != AdhanSoundType.SILENT && soundType != AdhanSoundType.VIBRATE_ONLY) {
            AdhanAudioSynthesizer.playSound(
                context = context,
                soundType = soundType,
                highPriorityAlarm = highPriority,
                volume = 1.0f
            )
        }
    }

    fun dismissPrayerNotification(prayerType: PrayerType) {
        notificationManager?.cancel(NOTIFICATION_ID_BASE + prayerType.ordinal)
    }

    /**
     * Schedules the next exact alarms for all upcoming daily prayers.
     */
    @SuppressLint("ScheduleExactAlarm")
    fun scheduleUpcomingAlarms(appSettings: AppSettings) {
        val settings = appSettings.settings.value
        val loc = settings.currentLocation
        val now = Calendar.getInstance()
        val tzOffset = now.timeZone.getOffset(now.timeInMillis) / 3600000.0

        // Calculate schedule for today and tomorrow
        val todaySchedule = PrayerTimeCalculator.calculateSchedule(
            date = now,
            latitude = loc.latitude,
            longitude = loc.longitude,
            timezoneOffsetHours = tzOffset,
            method = settings.calculationMethod,
            juristic = settings.juristicMethod,
            highLatitudeRule = settings.highLatitudeRule,
            minuteOffsets = settings.minuteOffsets
        )

        val tomorrowCal = now.clone() as Calendar
        tomorrowCal.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrowSchedule = PrayerTimeCalculator.calculateSchedule(
            date = tomorrowCal,
            latitude = loc.latitude,
            longitude = loc.longitude,
            timezoneOffsetHours = tzOffset,
            method = settings.calculationMethod,
            juristic = settings.juristicMethod,
            highLatitudeRule = settings.highLatitudeRule,
            minuteOffsets = settings.minuteOffsets
        )

        val prayers = PrayerType.dailyPrayers
        val nowMillis = now.timeInMillis

        for (prayer in prayers) {
            val isEnabled = settings.prayerAlertEnabled[prayer] ?: false
            if (!isEnabled) {
                cancelAlarm(prayer)
                continue
            }

            val todayTime = todaySchedule.getTime(prayer).timeInMillis
            val triggerMillis = if (todayTime > nowMillis) {
                todayTime
            } else {
                tomorrowSchedule.getTime(prayer).timeInMillis
            }

            val soundType = settings.prayerAlertSounds[prayer] ?: AdhanSoundType.MAKKAH
            setExactAlarm(prayer, triggerMillis, soundType, settings.highPrioritySound)
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun setExactAlarm(
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
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            prayer.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            }
            Log.d("AdhanAlarm", "Alarm scheduled for ${prayer.displayName} at $triggerTimeMillis")
        } catch (e: SecurityException) {
            Log.w("AdhanAlarm", "Exact alarm permission denied", e)
        }
    }

    private fun cancelAlarm(prayer: PrayerType) {
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
}
