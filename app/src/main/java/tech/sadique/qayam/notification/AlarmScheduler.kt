package tech.sadique.qayam.notification

import tech.sadique.qayam.data.model.AdhanSoundType
import tech.sadique.qayam.data.model.PrayerType

interface AlarmScheduler {
    fun setExactAlarm(prayer: PrayerType, triggerTimeMillis: Long, soundType: AdhanSoundType, highPriority: Boolean)
    fun cancelAlarm(prayer: PrayerType)
    fun scheduleTestAlarm(
        delaySeconds: Int = 10,
        prayerType: PrayerType = PrayerType.FAJR,
        soundType: AdhanSoundType = AdhanSoundType.MAKKAH,
    )
    fun canScheduleExactAlarms(): Boolean
    fun areNotificationsEnabled(): Boolean
    fun isIgnoringBatteryOptimizations(): Boolean
}
