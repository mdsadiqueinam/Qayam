package tech.sadique.qayam.data.calculator

import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

internal const val FULL_CIRCLE_DEGREES = 360.0
internal const val STRAIGHT_ANGLE_DEGREES = 180.0
internal const val HOURS_PER_DAY = 24.0
internal const val HOURS_PER_DAY_INT = 24
internal const val DEGREES_PER_HOUR = 15.0
internal const val MIDDAY_HOUR = 12.0
internal const val MINUTES_PER_HOUR = 60.0
internal const val SECONDS_PER_HOUR = 3600.0
internal const val SECONDS_PER_HOUR_INT = 3600
internal const val SECONDS_PER_MINUTE_INT = 60
internal const val MILLIS_PER_HOUR = 3600000.0
internal const val MILLIS_PER_DAY = 86_400_000L
internal const val ZERO_MILLIS = 0L
internal const val MIN_WINDOW_MILLIS = 1L
internal const val PROGRESS_MIN = 0f
internal const val PROGRESS_MAX = 1f
internal const val SUNRISE_REFRACTION_ANGLE = -0.8333
internal const val DHUHR_BUFFER_MINUTES = 2.0
internal const val MAGHRIB_SAFETY_MINUTES = 3.0
internal const val ISRAQ_DELAY_MINUTES = 20
internal const val HIGH_LATITUDE_THRESHOLD = 48.0
internal const val JULIAN_EPOCH = 2451545.0
internal const val NEXT_DAY_OFFSET = 1.0
internal const val HALF_DIVISOR = 2.0
internal const val MID_NIGHT_PORTION = 0.5
internal const val ONE_SEVENTH_NIGHT_PORTION = 1.0 / 7.0
internal const val DEFAULT_ISHA_ANGLE = 18.0
internal const val ASR_COTANGENT_NUMERATOR = 1.0
internal const val TRIG_DOMAIN_MIN = -1.0
internal const val TRIG_DOMAIN_MAX = 1.0
internal const val ZERO_ANGLE = 0.0
internal const val SOLAR_MEAN_ANOMALY_BASE = 357.529
internal const val SOLAR_MEAN_ANOMALY_RATE = 0.98560028
internal const val SOLAR_MEAN_LONGITUDE_BASE = 280.459
internal const val SOLAR_MEAN_LONGITUDE_RATE = 0.98564736
internal const val SOLAR_CENTER_COEFF_FIRST = 1.915
internal const val SOLAR_CENTER_COEFF_SECOND = 0.020
internal const val EARTH_OBLIQUITY_BASE = 23.439
internal const val EARTH_OBLIQUITY_RATE = 0.00000036
internal const val FEBRUARY_MONTH = 2
internal const val MONTHS_PER_YEAR = 12
internal const val CENTURY_DIVISOR = 100.0
internal const val LEAP_CYCLE_DIVISOR = 4.0
internal const val JULIAN_YEAR_COEFFICIENT = 365.25
internal const val JULIAN_YEAR_OFFSET = 4716
internal const val JULIAN_MONTH_COEFFICIENT = 30.6001
internal const val JULIAN_EPOCH_CORRECTION = 1524.5
internal const val GREGORIAN_CALENDAR_CORRECTION = 2.0

private const val DEGREES_TO_RADIANS = Math.PI / 180.0
private const val RADIANS_TO_DEGREES = 180.0 / Math.PI

internal data class SunPosition(val declination: Double, val equationOfTime: Double)

internal fun dSin(degrees: Double) = sin(degrees * DEGREES_TO_RADIANS)

internal fun dCos(degrees: Double) = cos(degrees * DEGREES_TO_RADIANS)

internal fun dTan(degrees: Double) = tan(degrees * DEGREES_TO_RADIANS)

internal fun dAsin(value: Double) = asin(value.coerceIn(TRIG_DOMAIN_MIN, TRIG_DOMAIN_MAX)) * RADIANS_TO_DEGREES

internal fun dAcos(value: Double) = acos(value.coerceIn(TRIG_DOMAIN_MIN, TRIG_DOMAIN_MAX)) * RADIANS_TO_DEGREES

internal fun dAtan2(y: Double, x: Double) = atan2(y, x) * RADIANS_TO_DEGREES

internal fun fixAngle(angle: Double): Double {
    var result = angle - FULL_CIRCLE_DEGREES * floor(angle / FULL_CIRCLE_DEGREES)
    if (result < ZERO_ANGLE) result += FULL_CIRCLE_DEGREES
    return result
}

internal fun fixHour(hour: Double): Double {
    var result = hour - HOURS_PER_DAY * floor(hour / HOURS_PER_DAY)
    if (result < ZERO_ANGLE) result += HOURS_PER_DAY
    return result
}

internal fun julianDay(year: Int, month: Int, day: Int): Double {
    var julianYear = year
    var julianMonth = month
    if (julianMonth <= FEBRUARY_MONTH) {
        julianYear -= 1
        julianMonth += MONTHS_PER_YEAR
    }
    val century = floor(julianYear / CENTURY_DIVISOR)
    val gregorianCorrection = GREGORIAN_CALENDAR_CORRECTION - century + floor(century / LEAP_CYCLE_DIVISOR)
    return floor(JULIAN_YEAR_COEFFICIENT * (julianYear + JULIAN_YEAR_OFFSET)) +
        floor(JULIAN_MONTH_COEFFICIENT * (julianMonth + 1)) + day + gregorianCorrection -
        JULIAN_EPOCH_CORRECTION
}

internal fun sunPosition(julianDayNumber: Double): SunPosition {
    val daysSinceEpoch = julianDayNumber - JULIAN_EPOCH
    val meanAnomaly = fixAngle(SOLAR_MEAN_ANOMALY_BASE + SOLAR_MEAN_ANOMALY_RATE * daysSinceEpoch)
    val meanLongitude = fixAngle(SOLAR_MEAN_LONGITUDE_BASE + SOLAR_MEAN_LONGITUDE_RATE * daysSinceEpoch)
    val eclipticLongitude = fixAngle(
        meanLongitude + SOLAR_CENTER_COEFF_FIRST * dSin(meanAnomaly) +
            SOLAR_CENTER_COEFF_SECOND * dSin(2 * meanAnomaly),
    )

    val obliquity = EARTH_OBLIQUITY_BASE - EARTH_OBLIQUITY_RATE * daysSinceEpoch
    val rightAscension = fixAngle(
        dAtan2(dCos(obliquity) * dSin(eclipticLongitude), dCos(eclipticLongitude)),
    ) / DEGREES_PER_HOUR

    val declination = dAsin(dSin(obliquity) * dSin(eclipticLongitude))
    val equationOfTime = meanLongitude / DEGREES_PER_HOUR - rightAscension
    return SunPosition(declination = declination, equationOfTime = equationOfTime)
}

/**
 * Compute hour angle for a given solar altitude angle.
 */
internal fun hourAngle(altitude: Double, latitude: Double, declination: Double): Double {
    val cosHour = (dSin(altitude) - dSin(latitude) * dSin(declination)) /
        (dCos(latitude) * dCos(declination))
    return if (cosHour < TRIG_DOMAIN_MIN) {
        STRAIGHT_ANGLE_DEGREES
    } else if (cosHour > TRIG_DOMAIN_MAX) {
        ZERO_ANGLE
    } else {
        dAcos(cosHour)
    }
}
