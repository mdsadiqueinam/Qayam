package tech.sadique.qayam.data.calculator

import tech.sadique.qayam.data.model.CalculationMethod
import tech.sadique.qayam.data.model.CurrentPrayerState
import tech.sadique.qayam.data.model.HighLatitudeRule
import tech.sadique.qayam.data.model.JuristicMethod
import tech.sadique.qayam.data.model.PrayerSchedule
import tech.sadique.qayam.data.model.PrayerType
import java.util.Calendar

object PrayerTimeCalculator {

    /**
     * Calculates full prayer schedule for a given date, coordinates, and preferences.
     */
    fun calculateSchedule(
        date: Calendar,
        latitude: Double,
        longitude: Double,
        timezoneOffsetHours: Double,
        method: CalculationMethod,
        juristic: JuristicMethod,
        highLatitudeRule: HighLatitudeRule = HighLatitudeRule.ANGLE_BASED,
        minuteOffsets: Map<PrayerType, Int> = emptyMap(),
    ): PrayerSchedule {
        val year = date.get(Calendar.YEAR)
        val month = date.get(Calendar.MONTH) + 1
        val day = date.get(Calendar.DAY_OF_MONTH)

        val dayNumber = julianDay(year = year, month = month, day = day)
        val sun = sunPosition(julianDayNumber = dayNumber)
        val transits = computeDayTransits(
            latitude = latitude,
            longitude = longitude,
            timezoneOffsetHours = timezoneOffsetHours,
            method = method,
            juristic = juristic,
            sun = sun,
            dayNumber = dayNumber,
        )
        val adjusted = applyHighLatitudeAdjustment(
            transits = transits,
            latitude = latitude,
            method = method,
            highLatitudeRule = highLatitudeRule,
        )
        return buildSchedule(date = date, transits = adjusted, minuteOffsets = minuteOffsets)
    }

    /**
     * Calculates the current solar elevation angle (degrees above/below horizon)
     * and current prayer state at [currentTime].
     */
    fun calculateCurrentState(
        currentTime: Calendar,
        schedule: PrayerSchedule,
        latitude: Double,
        longitude: Double,
    ): CurrentPrayerState {
        val nowMillis = currentTime.timeInMillis
        val sunAltitude = computeSolarAltitude(latitude = latitude, longitude = longitude, currentTime = currentTime)
        val window = resolvePrayerWindow(nowMillis = nowMillis, schedule = schedule)
        val sunProgress = computeSunProgress(
            nowMillis = nowMillis,
            sunriseTimeMillis = schedule.sunrise.timeInMillis,
            gurubTimeMillis = schedule.gurubAftab.timeInMillis,
        )
        return buildCurrentState(
            nowMillis = nowMillis,
            window = window,
            sunAltitude = sunAltitude,
            sunProgress = sunProgress,
        )
    }
}
