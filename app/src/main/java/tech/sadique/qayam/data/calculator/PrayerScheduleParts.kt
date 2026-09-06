package tech.sadique.qayam.data.calculator

import tech.sadique.qayam.data.model.CalculationMethod
import tech.sadique.qayam.data.model.HighLatitudeRule
import tech.sadique.qayam.data.model.JuristicMethod
import tech.sadique.qayam.data.model.PrayerSchedule
import tech.sadique.qayam.data.model.PrayerType
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt

internal data class DayTransits(
    val fajrTransit: Double,
    val sunriseTransit: Double,
    val dhuhrTransit: Double,
    val asrTransit: Double,
    val gurubTransit: Double,
    val maghribTransit: Double,
    val ishaTransit: Double,
    val sunsetTransit: Double,
    val midnightTransit: Double,
)

internal data class PrayerWindow(
    val currentPrayer: PrayerType,
    val nextPrayer: PrayerType,
    val nextPrayerTime: Calendar,
    val windowStartMillis: Long,
    val windowEndMillis: Long,
)

internal data class SunProgress(val isDaytime: Boolean, val progress: Float)

internal fun computeDayTransits(
    latitude: Double,
    longitude: Double,
    timezoneOffsetHours: Double,
    method: CalculationMethod,
    juristic: JuristicMethod,
    sun: SunPosition,
    dayNumber: Double,
): DayTransits {
    // Midday (Dhuhr) solar transit
    val dhuhrTransit = fixHour(MIDDAY_HOUR + timezoneOffsetHours - longitude / DEGREES_PER_HOUR - sun.equationOfTime)

    // Sunrise & Sunset at the standard refraction angle
    val sunriseHourAngle = hourAngle(SUNRISE_REFRACTION_ANGLE, latitude, sun.declination)
    val sunriseTransit = fixHour(dhuhrTransit - sunriseHourAngle / DEGREES_PER_HOUR)
    val sunsetTransit = fixHour(dhuhrTransit + sunriseHourAngle / DEGREES_PER_HOUR)

    // Fajr
    val fajrHourAngle = hourAngle(-method.fajrAngle, latitude, sun.declination)
    val fajrTransit = fixHour(dhuhrTransit - fajrHourAngle / DEGREES_PER_HOUR)

    // Asr altitude above horizon: arccot(shadow + tan(|lat - dec|))
    val shadowFactor = juristic.shadowFactor
    val asrAltitude = dAtan2(ASR_COTANGENT_NUMERATOR, shadowFactor + dTan(abs(latitude - sun.declination)))
    val asrHourAngle = hourAngle(asrAltitude, latitude, sun.declination)
    val asrTransit = fixHour(dhuhrTransit + asrHourAngle / DEGREES_PER_HOUR)

    // Gurub e Aftab (Sunset)
    val gurubTransit = sunsetTransit

    // Maghrib (Sunset + safety margin for complete sunset, or method angle e.g. Tehran)
    val maghribTransit = if (method.maghribAngle != null) {
        val maghribHourAngle = hourAngle(-method.maghribAngle, latitude, sun.declination)
        fixHour(dhuhrTransit + maghribHourAngle / DEGREES_PER_HOUR)
    } else {
        fixHour(sunsetTransit + MAGHRIB_SAFETY_MINUTES / MINUTES_PER_HOUR)
    }

    // Isha
    val ishaTransit = if (method.ishaMinutesAfterMaghrib != null) {
        fixHour(maghribTransit + method.ishaMinutesAfterMaghrib / MINUTES_PER_HOUR)
    } else {
        val ishaHourAngle = hourAngle(-method.ishaAngle, latitude, sun.declination)
        fixHour(dhuhrTransit + ishaHourAngle / DEGREES_PER_HOUR)
    }

    // Midnight (halfway between sunset and next-day sunrise, using tomorrow's
    // solar position so high-latitude / declination drift doesn't bias it)
    val tomorrowSun = sunPosition(dayNumber + NEXT_DAY_OFFSET)
    val tomorrowDhuhr = fixHour(
        MIDDAY_HOUR + timezoneOffsetHours - longitude / DEGREES_PER_HOUR - tomorrowSun.equationOfTime,
    )
    val tomorrowSunriseAngle = hourAngle(SUNRISE_REFRACTION_ANGLE, latitude, tomorrowSun.declination)
    val tomorrowSunriseTransit = fixHour(tomorrowDhuhr - tomorrowSunriseAngle / DEGREES_PER_HOUR)
    val nightToTomorrow = fixHour(tomorrowSunriseTransit + HOURS_PER_DAY - sunsetTransit)
    val midnightTransit = fixHour(sunsetTransit + nightToTomorrow / HALF_DIVISOR)

    return DayTransits(
        fajrTransit = fajrTransit,
        sunriseTransit = sunriseTransit,
        dhuhrTransit = dhuhrTransit,
        asrTransit = asrTransit,
        gurubTransit = gurubTransit,
        maghribTransit = maghribTransit,
        ishaTransit = ishaTransit,
        sunsetTransit = sunsetTransit,
        midnightTransit = midnightTransit,
    )
}

internal fun applyHighLatitudeAdjustment(
    transits: DayTransits,
    latitude: Double,
    method: CalculationMethod,
    highLatitudeRule: HighLatitudeRule,
): DayTransits {
    if (highLatitudeRule == HighLatitudeRule.NONE || abs(latitude) <= HIGH_LATITUDE_THRESHOLD) {
        return transits
    }
    val nightDuration = fixHour(transits.sunriseTransit - transits.sunsetTransit)
    val adjustedFajr = clampFajrTransit(transits, method, highLatitudeRule, nightDuration)
    val adjustedIsha = clampIshaTransit(transits, method, highLatitudeRule, nightDuration)
    return transits.copy(fajrTransit = adjustedFajr, ishaTransit = adjustedIsha)
}

private fun fajrPortion(rule: HighLatitudeRule, method: CalculationMethod): Double = when (rule) {
    HighLatitudeRule.ANGLE_BASED -> method.fajrAngle / MINUTES_PER_HOUR
    HighLatitudeRule.MID_NIGHT -> MID_NIGHT_PORTION
    HighLatitudeRule.ONE_SEVENTH -> ONE_SEVENTH_NIGHT_PORTION
    HighLatitudeRule.NONE -> method.fajrAngle / MINUTES_PER_HOUR
}

private fun ishaPortion(rule: HighLatitudeRule, method: CalculationMethod): Double = when (rule) {
    HighLatitudeRule.ANGLE_BASED -> ishaAngleOrDefault(method) / MINUTES_PER_HOUR
    HighLatitudeRule.MID_NIGHT -> MID_NIGHT_PORTION
    HighLatitudeRule.ONE_SEVENTH -> ONE_SEVENTH_NIGHT_PORTION
    HighLatitudeRule.NONE -> ishaAngleOrDefault(method) / MINUTES_PER_HOUR
}

private fun ishaAngleOrDefault(method: CalculationMethod): Double =
    if (method.ishaAngle > 0) method.ishaAngle else DEFAULT_ISHA_ANGLE

private fun clampFajrTransit(
    transits: DayTransits,
    method: CalculationMethod,
    rule: HighLatitudeRule,
    nightDuration: Double,
): Double {
    val maxFajrDiff = nightDuration * fajrPortion(rule, method)
    val actualFajrDiff = fixHour(transits.sunriseTransit - transits.fajrTransit)
    if (actualFajrDiff > maxFajrDiff || actualFajrDiff.isNaN()) {
        return fixHour(transits.sunriseTransit - maxFajrDiff)
    }
    return transits.fajrTransit
}

private fun clampIshaTransit(
    transits: DayTransits,
    method: CalculationMethod,
    rule: HighLatitudeRule,
    nightDuration: Double,
): Double {
    // Skip Isha clamp for interval-based methods (e.g. Umm al-Qura Isha +90m),
    // which intentionally exceed the angle-based portion of the night.
    val adjusted = clampIntervalIsha(transits, method, rule, nightDuration)
    return adjusted ?: transits.ishaTransit
}

private fun clampIntervalIsha(
    transits: DayTransits,
    method: CalculationMethod,
    rule: HighLatitudeRule,
    nightDuration: Double,
): Double? {
    val maxIshaDiff = nightDuration * ishaPortion(rule, method)
    val actualIshaDiff = fixHour(transits.ishaTransit - transits.sunsetTransit)
    val needsClamp = method.ishaMinutesAfterMaghrib == null &&
        (actualIshaDiff > maxIshaDiff || actualIshaDiff.isNaN())
    return if (needsClamp) {
        fixHour(transits.sunsetTransit + maxIshaDiff)
    } else {
        null
    }
}

internal fun toCalendar(date: Calendar, hourDecimal: Double, offsetMinutes: Int): Calendar {
    val result = date.clone() as Calendar
    val totalSeconds = (hourDecimal * SECONDS_PER_HOUR).roundToInt() + offsetMinutes * SECONDS_PER_MINUTE_INT
    val hours = totalSeconds / SECONDS_PER_HOUR_INT
    val minutes = totalSeconds % SECONDS_PER_HOUR_INT / SECONDS_PER_MINUTE_INT
    val seconds = totalSeconds % SECONDS_PER_MINUTE_INT
    result.set(Calendar.HOUR_OF_DAY, hours % HOURS_PER_DAY_INT)
    result.set(Calendar.MINUTE, minutes)
    result.set(Calendar.SECOND, seconds)
    result.set(Calendar.MILLISECOND, 0)
    return result
}

internal fun buildSchedule(date: Calendar, transits: DayTransits, minuteOffsets: Map<PrayerType, Int>): PrayerSchedule {
    // Slight standard safety buffer for Dhuhr (2 min after transit)
    val dhuhrFinal = fixHour(transits.dhuhrTransit + DHUHR_BUFFER_MINUTES / MINUTES_PER_HOUR)
    val sunriseCal = toCalendar(date, transits.sunriseTransit, minuteOffsets[PrayerType.SUNRISE] ?: 0)
    // Israq starts 20 minutes after sunrise
    val israqCal = (sunriseCal.clone() as Calendar).apply {
        add(Calendar.MINUTE, ISRAQ_DELAY_MINUTES + (minuteOffsets[PrayerType.ISRAQ] ?: 0))
    }

    return PrayerSchedule(
        date = date.clone() as Calendar,
        fajr = toCalendar(date, transits.fajrTransit, minuteOffsets[PrayerType.FAJR] ?: 0),
        sunrise = sunriseCal,
        israq = israqCal,
        dhuhr = toCalendar(date, dhuhrFinal, minuteOffsets[PrayerType.DHUHR] ?: 0),
        asr = toCalendar(date, transits.asrTransit, minuteOffsets[PrayerType.ASR] ?: 0),
        gurubAftab = toCalendar(date, transits.gurubTransit, minuteOffsets[PrayerType.GURUB_E_AFTAB] ?: 0),
        maghrib = toCalendar(date, transits.maghribTransit, minuteOffsets[PrayerType.MAGHRIB] ?: 0),
        isha = toCalendar(date, transits.ishaTransit, minuteOffsets[PrayerType.ISHA] ?: 0),
        midnight = toCalendar(date, transits.midnightTransit, 0),
    )
}

internal fun computeSolarAltitude(latitude: Double, longitude: Double, currentTime: Calendar): Double {
    val nowMillis = currentTime.timeInMillis
    val offsetHours = currentTime.timeZone.getOffset(nowMillis) / MILLIS_PER_HOUR

    val year = currentTime.get(Calendar.YEAR)
    val month = currentTime.get(Calendar.MONTH) + 1
    val day = currentTime.get(Calendar.DAY_OF_MONTH)
    val hour = currentTime.get(Calendar.HOUR_OF_DAY)
    val minute = currentTime.get(Calendar.MINUTE)
    val second = currentTime.get(Calendar.SECOND)

    val dayNumber = julianDay(year, month, day) +
        (hour + minute / MINUTES_PER_HOUR + second / SECONDS_PER_HOUR - offsetHours) / HOURS_PER_DAY
    val sun = sunPosition(dayNumber)

    // Solar altitude: sin(alt) = sin(lat)*sin(dec) + cos(lat)*cos(dec)*cos(H)
    val localSolarTime = hour + minute / MINUTES_PER_HOUR + second / SECONDS_PER_HOUR -
        offsetHours + longitude / DEGREES_PER_HOUR + sun.equationOfTime
    val hourAngleDeg = (localSolarTime - MIDDAY_HOUR) * DEGREES_PER_HOUR
    val sinAltitude = dSin(latitude) * dSin(sun.declination) +
        dCos(latitude) * dCos(sun.declination) * dCos(hourAngleDeg)
    return dAsin(sinAltitude)
}
