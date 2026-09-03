package com.example.data.calculator

import com.example.data.model.CalculationMethod
import com.example.data.model.CurrentPrayerState
import com.example.data.model.HighLatitudeRule
import com.example.data.model.JuristicMethod
import com.example.data.model.PrayerSchedule
import com.example.data.model.PrayerType
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tan

object PrayerTimeCalculator {

    private const val RAD = Math.PI / 180.0
    private const val DEG = 180.0 / Math.PI

    private fun dSin(d: Double) = sin(d * RAD)
    private fun dCos(d: Double) = cos(d * RAD)
    private fun dTan(d: Double) = tan(d * RAD)
    private fun dAsin(x: Double) = asin(x.coerceIn(-1.0, 1.0)) * DEG
    private fun dAcos(x: Double) = acos(x.coerceIn(-1.0, 1.0)) * DEG
    private fun dAtan2(y: Double, x: Double) = atan2(y, x) * DEG

    private fun fixAngle(a: Double): Double {
        var res = a - 360.0 * floor(a / 360.0)
        if (res < 0) res += 360.0
        return res
    }

    private fun fixHour(h: Double): Double {
        var res = h - 24.0 * floor(h / 24.0)
        if (res < 0) res += 24.0
        return res
    }

    private fun julianDay(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private data class SunPosition(val declination: Double, val equationOfTime: Double)

    private fun sunPosition(jd: Double): SunPosition {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * dSin(g) + 0.020 * dSin(2 * g))

        val e = 23.439 - 0.00000036 * d
        val ra = fixAngle(dAtan2(dCos(e) * dSin(l), dCos(l))) / 15.0

        val declination = dAsin(dSin(e) * dSin(l))
        val equationOfTime = q / 15.0 - ra
        return SunPosition(declination, equationOfTime)
    }

    /**
     * Compute hour angle for a given solar altitude angle
     */
    private fun hourAngle(altitude: Double, latitude: Double, declination: Double): Double {
        val cosH = (dSin(altitude) - dSin(latitude) * dSin(declination)) / (dCos(latitude) * dCos(declination))
        return if (cosH < -1.0) 180.0 else if (cosH > 1.0) 0.0 else dAcos(cosH)
    }

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
        minuteOffsets: Map<PrayerType, Int> = emptyMap()
    ): PrayerSchedule {
        val year = date.get(Calendar.YEAR)
        val month = date.get(Calendar.MONTH) + 1
        val day = date.get(Calendar.DAY_OF_MONTH)

        val jd = julianDay(year, month, day)
        val sun = sunPosition(jd)

        // Midday (Dhuhr) solar transit
        val dhuhrTransit = fixHour(12.0 + timezoneOffsetHours - longitude / 15.0 - sun.equationOfTime)

        // Sunrise & Sunset standard refraction angle = -0.8333
        val sunriseHourAngle = hourAngle(-0.8333, latitude, sun.declination)
        val sunriseTransit = fixHour(dhuhrTransit - sunriseHourAngle / 15.0)
        val sunsetTransit = fixHour(dhuhrTransit + sunriseHourAngle / 15.0)

        // Fajr
        val fajrHourAngle = hourAngle(-method.fajrAngle, latitude, sun.declination)
        var fajrTransit = fixHour(dhuhrTransit - fajrHourAngle / 15.0)

        // Asr
        val shadowFactor = juristic.shadowFactor
        val asrAltitude = dAcos(1.0 / (shadowFactor + dTan(abs(latitude - sun.declination))))
        // Asr angle above horizon: arccot(t + tan(|lat-dec|)) = 90 - atan(t + tan)
        val asrAngle = 90.0 - (atan2(1.0, shadowFactor + dTan(abs(latitude - sun.declination))) * DEG)
        val asrHourAngle = hourAngle(asrAngle, latitude, sun.declination)
        val asrTransit = fixHour(dhuhrTransit + asrHourAngle / 15.0)

        // Maghrib
        var maghribTransit = if (method.maghribAngle != null) {
            val maghribHourAngle = hourAngle(-method.maghribAngle, latitude, sun.declination)
            fixHour(dhuhrTransit + maghribHourAngle / 15.0)
        } else {
            sunsetTransit
        }

        // Isha
        var ishaTransit = if (method.ishaMinutesAfterMaghrib != null) {
            fixHour(maghribTransit + method.ishaMinutesAfterMaghrib / 60.0)
        } else {
            val ishaHourAngle = hourAngle(-method.ishaAngle, latitude, sun.declination)
            fixHour(dhuhrTransit + ishaHourAngle / 15.0)
        }

        // High Latitude Adjustments if necessary
        val nightDuration = fixHour(sunriseTransit - sunsetTransit)
        if (highLatitudeRule != HighLatitudeRule.NONE && abs(latitude) > 48.0) {
            val portionFajr = when (highLatitudeRule) {
                HighLatitudeRule.ANGLE_BASED -> method.fajrAngle / 60.0
                HighLatitudeRule.MID_NIGHT -> 0.5
                HighLatitudeRule.ONE_SEVENTH -> 1.0 / 7.0
                else -> method.fajrAngle / 60.0
            }
            val portionIsha = when (highLatitudeRule) {
                HighLatitudeRule.ANGLE_BASED -> (if (method.ishaAngle > 0) method.ishaAngle else 18.0) / 60.0
                HighLatitudeRule.MID_NIGHT -> 0.5
                HighLatitudeRule.ONE_SEVENTH -> 1.0 / 7.0
                else -> (if (method.ishaAngle > 0) method.ishaAngle else 18.0) / 60.0
            }

            val maxFajrDiff = nightDuration * portionFajr
            val actualFajrDiff = fixHour(sunriseTransit - fajrTransit)
            if (actualFajrDiff > maxFajrDiff || actualFajrDiff.isNaN()) {
                fajrTransit = fixHour(sunriseTransit - maxFajrDiff)
            }

            val maxIshaDiff = nightDuration * portionIsha
            val actualIshaDiff = fixHour(ishaTransit - sunsetTransit)
            if (actualIshaDiff > maxIshaDiff || actualIshaDiff.isNaN()) {
                ishaTransit = fixHour(sunsetTransit + maxIshaDiff)
            }
        }

        // Slight standard safety buffer for Dhuhr (1 min after transit)
        val dhuhrFinal = fixHour(dhuhrTransit + 2.0 / 60.0)

        // Midnight (halfway between Sunset and next Sunrise)
        val midnightTransit = fixHour(sunsetTransit + nightDuration / 2.0)

        fun toCalendar(hourDecimal: Double, offsetMinutes: Int): Calendar {
            val cal = date.clone() as Calendar
            val totalSeconds = (hourDecimal * 3600).toInt() + (offsetMinutes * 60)
            val h = totalSeconds / 3600
            val m = (totalSeconds % 3600) / 60
            val s = totalSeconds % 60
            cal.set(Calendar.HOUR_OF_DAY, h % 24)
            cal.set(Calendar.MINUTE, m)
            cal.set(Calendar.SECOND, s)
            cal.set(Calendar.MILLISECOND, 0)
            return cal
        }

        return PrayerSchedule(
            date = date.clone() as Calendar,
            fajr = toCalendar(fajrTransit, minuteOffsets[PrayerType.FAJR] ?: 0),
            sunrise = toCalendar(sunriseTransit, minuteOffsets[PrayerType.SUNRISE] ?: 0),
            dhuhr = toCalendar(dhuhrFinal, minuteOffsets[PrayerType.DHUHR] ?: 0),
            asr = toCalendar(asrTransit, minuteOffsets[PrayerType.ASR] ?: 0),
            maghrib = toCalendar(maghribTransit, minuteOffsets[PrayerType.MAGHRIB] ?: 0),
            isha = toCalendar(ishaTransit, minuteOffsets[PrayerType.ISHA] ?: 0),
            midnight = toCalendar(midnightTransit, 0)
        )
    }

    /**
     * Calculates the current solar elevation angle (degrees above/below horizon)
     * and current prayer state at [currentTime].
     */
    fun calculateCurrentState(
        currentTime: Calendar,
        schedule: PrayerSchedule,
        latitude: Double,
        longitude: Double
    ): CurrentPrayerState {
        val nowMillis = currentTime.timeInMillis
        val tz = currentTime.timeZone
        val offsetHours = tz.getOffset(nowMillis) / 3600000.0

        val year = currentTime.get(Calendar.YEAR)
        val month = currentTime.get(Calendar.MONTH) + 1
        val day = currentTime.get(Calendar.DAY_OF_MONTH)
        val hour = currentTime.get(Calendar.HOUR_OF_DAY)
        val minute = currentTime.get(Calendar.MINUTE)
        val second = currentTime.get(Calendar.SECOND)

        val jd = julianDay(year, month, day) + (hour + minute / 60.0 + second / 3600.0 - offsetHours) / 24.0
        val sun = sunPosition(jd)

        // Solar altitude calculation: sin(alt) = sin(lat)*sin(dec) + cos(lat)*cos(dec)*cos(H)
        val localSolarTime = (hour + minute / 60.0 + second / 3600.0) - offsetHours + longitude / 15.0 + sun.equationOfTime
        val hourAngleDeg = (localSolarTime - 12.0) * 15.0
        val sinAlt = dSin(latitude) * dSin(sun.declination) + dCos(latitude) * dCos(sun.declination) * dCos(hourAngleDeg)
        val sunAltitude = dAsin(sinAlt)

        val fajrTime = schedule.fajr.timeInMillis
        val sunriseTime = schedule.sunrise.timeInMillis
        val dhuhrTime = schedule.dhuhr.timeInMillis
        val asrTime = schedule.asr.timeInMillis
        val maghribTime = schedule.maghrib.timeInMillis
        val ishaTime = schedule.isha.timeInMillis

        val currentPrayer: PrayerType
        val nextPrayer: PrayerType
        val nextPrayerTime: Calendar
        val windowStart: Long
        val windowEnd: Long

        when {
            nowMillis < fajrTime -> {
                currentPrayer = PrayerType.ISHA
                nextPrayer = PrayerType.FAJR
                nextPrayerTime = schedule.fajr
                windowStart = schedule.isha.timeInMillis - 24 * 3600 * 1000L
                windowEnd = fajrTime
            }
            nowMillis < sunriseTime -> {
                currentPrayer = PrayerType.FAJR
                nextPrayer = PrayerType.SUNRISE
                nextPrayerTime = schedule.sunrise
                windowStart = fajrTime
                windowEnd = sunriseTime
            }
            nowMillis < dhuhrTime -> {
                currentPrayer = PrayerType.SUNRISE
                nextPrayer = PrayerType.DHUHR
                nextPrayerTime = schedule.dhuhr
                windowStart = sunriseTime
                windowEnd = dhuhrTime
            }
            nowMillis < asrTime -> {
                currentPrayer = PrayerType.DHUHR
                nextPrayer = PrayerType.ASR
                nextPrayerTime = schedule.asr
                windowStart = dhuhrTime
                windowEnd = asrTime
            }
            nowMillis < maghribTime -> {
                currentPrayer = PrayerType.ASR
                nextPrayer = PrayerType.MAGHRIB
                nextPrayerTime = schedule.maghrib
                windowStart = asrTime
                windowEnd = maghribTime
            }
            nowMillis < ishaTime -> {
                currentPrayer = PrayerType.MAGHRIB
                nextPrayer = PrayerType.ISHA
                nextPrayerTime = schedule.isha
                windowStart = maghribTime
                windowEnd = ishaTime
            }
            else -> {
                currentPrayer = PrayerType.ISHA
                nextPrayer = PrayerType.FAJR
                // Next fajr is tomorrow
                val tomorrowFajr = schedule.fajr.clone() as Calendar
                tomorrowFajr.add(Calendar.DAY_OF_YEAR, 1)
                nextPrayerTime = tomorrowFajr
                windowStart = ishaTime
                windowEnd = tomorrowFajr.timeInMillis
            }
        }

        val timeRemaining = max(0L, nextPrayerTime.timeInMillis - nowMillis)
        val totalDuration = max(1L, windowEnd - windowStart)
        val progress = ((nowMillis - windowStart).toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)

        // Sun day progress: 0.0 at sunrise, 0.5 at dhuhr, 1.0 at sunset
        val isDaytime = nowMillis in sunriseTime..maghribTime
        val dayDuration = max(1L, maghribTime - sunriseTime)
        val sunProgressPercent = if (isDaytime) {
            ((nowMillis - sunriseTime).toFloat() / dayDuration.toFloat()).coerceIn(0f, 1f)
        } else {
            // Night arc: 0.0 at sunset, 1.0 at next sunrise
            val nightStart = if (nowMillis < sunriseTime) maghribTime - 24 * 3600 * 1000L else maghribTime
            val nightEnd = if (nowMillis < sunriseTime) sunriseTime else sunriseTime + 24 * 3600 * 1000L
            val nightTotal = max(1L, nightEnd - nightStart)
            ((nowMillis - nightStart).toFloat() / nightTotal.toFloat()).coerceIn(0f, 1f)
        }

        return CurrentPrayerState(
            currentPrayer = currentPrayer,
            nextPrayer = nextPrayer,
            nextPrayerTime = nextPrayerTime,
            timeRemainingMillis = timeRemaining,
            totalWindowDurationMillis = totalDuration,
            progressInWindow = progress,
            sunAltitudeDegrees = sunAltitude,
            sunProgressPercent = sunProgressPercent,
            isDaytime = isDaytime
        )
    }
}
