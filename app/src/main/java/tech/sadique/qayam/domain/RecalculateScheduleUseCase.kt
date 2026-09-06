package tech.sadique.qayam.domain

import tech.sadique.qayam.data.calculator.PrayerTimeCalculator
import tech.sadique.qayam.data.model.PrayerSchedule
import tech.sadique.qayam.data.preferences.UserSettings
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecalculateScheduleUseCase @Inject constructor() {

    operator fun invoke(
        settings: UserSettings,
        now: Calendar = Calendar.getInstance()
    ): PrayerSchedule {
        val loc = settings.currentLocation
        val tzOffset = now.timeZone.getOffset(now.timeInMillis) / 3600000.0

        return PrayerTimeCalculator.calculateSchedule(
            date = now,
            latitude = loc.latitude,
            longitude = loc.longitude,
            timezoneOffsetHours = tzOffset,
            method = settings.calculationMethod,
            juristic = settings.juristicMethod,
            highLatitudeRule = settings.highLatitudeRule,
            minuteOffsets = settings.minuteOffsets
        )
    }
}
