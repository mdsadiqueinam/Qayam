package tech.sadique.qayam.notification

import tech.sadique.qayam.data.calculator.PrayerTimeCalculator
import tech.sadique.qayam.data.model.AdhanSoundType
import tech.sadique.qayam.data.model.PrayerType
import tech.sadique.qayam.data.preferences.UserSettings
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchedulePrayerAlarmsUseCase @Inject constructor(
    private val alarmScheduler: AlarmScheduler
) {
    operator fun invoke(settings: UserSettings) {
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
            val isEnabled = settings.prayerAlertEnabled[prayer] ?: prayer.defaultAlertEnabled
            if (!isEnabled) {
                alarmScheduler.cancelAlarm(prayer)
                continue
            }

            val todayTime = todaySchedule.getTime(prayer).timeInMillis
            val triggerMillis = if (todayTime > nowMillis) {
                todayTime
            } else {
                tomorrowSchedule.getTime(prayer).timeInMillis
            }

            val soundType = settings.prayerAlertSounds[prayer] ?: AdhanSoundType.MAKKAH
            alarmScheduler.setExactAlarm(prayer, triggerMillis, soundType, settings.highPrioritySound)
        }
    }
}
