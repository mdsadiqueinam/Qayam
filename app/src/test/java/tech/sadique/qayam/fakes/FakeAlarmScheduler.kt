package tech.sadique.qayam.fakes

import tech.sadique.qayam.data.model.AdhanSoundType
import tech.sadique.qayam.data.model.PrayerType
import tech.sadique.qayam.notification.AlarmScheduler

class FakeAlarmScheduler : AlarmScheduler {

    data class ScheduledAlarm(
        val prayer: PrayerType,
        val triggerTimeMillis: Long,
        val soundType: AdhanSoundType,
        val highPriority: Boolean
    )

    val scheduledAlarms = mutableMapOf<PrayerType, ScheduledAlarm>()
    val cancelledAlarms = mutableListOf<PrayerType>()
    var exactAlarmsAllowed = true
    var notificationsAllowed = true
    var batteryOptimizationsIgnored = true
    var testAlarmScheduled: Pair<Int, PrayerType>? = null

    override fun setExactAlarm(
        prayer: PrayerType,
        triggerTimeMillis: Long,
        soundType: AdhanSoundType,
        highPriority: Boolean
    ) {
        scheduledAlarms[prayer] = ScheduledAlarm(prayer, triggerTimeMillis, soundType, highPriority)
    }

    override fun cancelAlarm(prayer: PrayerType) {
        scheduledAlarms.remove(prayer)
        cancelledAlarms.add(prayer)
    }

    override fun scheduleTestAlarm(
        delaySeconds: Int,
        prayerType: PrayerType,
        soundType: AdhanSoundType
    ) {
        testAlarmScheduled = delaySeconds to prayerType
    }

    override fun canScheduleExactAlarms(): Boolean = exactAlarmsAllowed

    override fun areNotificationsEnabled(): Boolean = notificationsAllowed

    override fun isIgnoringBatteryOptimizations(): Boolean = batteryOptimizationsIgnored
}
