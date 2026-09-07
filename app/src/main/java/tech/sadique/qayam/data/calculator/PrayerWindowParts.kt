package tech.sadique.qayam.data.calculator

import tech.sadique.qayam.data.model.CurrentPrayerState
import tech.sadique.qayam.data.model.PrayerSchedule
import tech.sadique.qayam.data.model.PrayerType
import java.util.Calendar
import kotlin.math.max

internal fun resolvePrayerWindow(nowMillis: Long, schedule: PrayerSchedule): PrayerWindow =
    resolveEarlyWindow(nowMillis, schedule) ?: resolveLateWindow(nowMillis, schedule)

internal fun resolveEarlyWindow(nowMillis: Long, schedule: PrayerSchedule): PrayerWindow? {
    val fajrTime = schedule.fajr.timeInMillis
    val sunriseTime = schedule.sunrise.timeInMillis
    val israqTime = schedule.israq.timeInMillis
    val dhuhrTime = schedule.dhuhr.timeInMillis
    return when {
        nowMillis < fajrTime -> PrayerWindow(
            currentPrayer = PrayerType.ISHA,
            nextPrayer = PrayerType.FAJR,
            nextPrayerTime = schedule.fajr,
            windowStartMillis = schedule.isha.timeInMillis - MILLIS_PER_DAY,
            windowEndMillis = fajrTime,
        )

        nowMillis < sunriseTime -> PrayerWindow(
            currentPrayer = PrayerType.FAJR,
            nextPrayer = PrayerType.SUNRISE,
            nextPrayerTime = schedule.sunrise,
            windowStartMillis = fajrTime,
            windowEndMillis = sunriseTime,
        )

        nowMillis < israqTime -> PrayerWindow(
            currentPrayer = PrayerType.SUNRISE,
            nextPrayer = PrayerType.ISRAQ,
            nextPrayerTime = schedule.israq,
            windowStartMillis = sunriseTime,
            windowEndMillis = israqTime,
        )

        nowMillis < dhuhrTime -> PrayerWindow(
            currentPrayer = PrayerType.ISRAQ,
            nextPrayer = PrayerType.DHUHR,
            nextPrayerTime = schedule.dhuhr,
            windowStartMillis = israqTime,
            windowEndMillis = dhuhrTime,
        )

        else -> null
    }
}

internal fun resolveLateWindow(nowMillis: Long, schedule: PrayerSchedule): PrayerWindow {
    val dhuhrTime = schedule.dhuhr.timeInMillis
    val asrTime = schedule.asr.timeInMillis
    val gurubTime = schedule.gurubAftab.timeInMillis
    val maghribTime = schedule.maghrib.timeInMillis
    val ishaTime = schedule.isha.timeInMillis
    return when {
        nowMillis < asrTime -> PrayerWindow(
            currentPrayer = PrayerType.DHUHR,
            nextPrayer = PrayerType.ASR,
            nextPrayerTime = schedule.asr,
            windowStartMillis = dhuhrTime,
            windowEndMillis = asrTime,
        )

        nowMillis < gurubTime -> PrayerWindow(
            currentPrayer = PrayerType.ASR,
            nextPrayer = PrayerType.GURUB_E_AFTAB,
            nextPrayerTime = schedule.gurubAftab,
            windowStartMillis = asrTime,
            windowEndMillis = gurubTime,
        )

        nowMillis < maghribTime -> PrayerWindow(
            currentPrayer = PrayerType.GURUB_E_AFTAB,
            nextPrayer = PrayerType.MAGHRIB,
            nextPrayerTime = schedule.maghrib,
            windowStartMillis = gurubTime,
            windowEndMillis = maghribTime,
        )

        nowMillis < ishaTime -> PrayerWindow(
            currentPrayer = PrayerType.MAGHRIB,
            nextPrayer = PrayerType.ISHA,
            nextPrayerTime = schedule.isha,
            windowStartMillis = maghribTime,
            windowEndMillis = ishaTime,
        )

        else -> {
            // Next fajr is tomorrow
            val tomorrowFajr = schedule.fajr.clone() as Calendar
            tomorrowFajr.add(Calendar.DAY_OF_YEAR, 1)
            PrayerWindow(
                currentPrayer = PrayerType.ISHA,
                nextPrayer = PrayerType.FAJR,
                nextPrayerTime = tomorrowFajr,
                windowStartMillis = ishaTime,
                windowEndMillis = tomorrowFajr.timeInMillis,
            )
        }
    }
}

internal fun computeWindowProgress(nowMillis: Long, windowStartMillis: Long, windowEndMillis: Long): Float {
    val totalDuration = max(MIN_WINDOW_MILLIS, windowEndMillis - windowStartMillis)
    return ((nowMillis - windowStartMillis).toFloat() / totalDuration.toFloat()).coerceIn(PROGRESS_MIN, PROGRESS_MAX)
}

internal fun computeSunProgress(nowMillis: Long, sunriseTimeMillis: Long, gurubTimeMillis: Long): SunProgress {
    val isDaytime = nowMillis in sunriseTimeMillis..gurubTimeMillis
    val progress = if (isDaytime) {
        // Sun day progress: 0.0 at sunrise (Tulub e Aftab), 1.0 at sunset (Gurub e Aftab)
        computeWindowProgress(nowMillis, sunriseTimeMillis, gurubTimeMillis)
    } else {
        // Night arc: 0.0 at sunset, 1.0 at next sunrise
        val nightStart = if (nowMillis < sunriseTimeMillis) gurubTimeMillis - MILLIS_PER_DAY else gurubTimeMillis
        val nightEnd = if (nowMillis < sunriseTimeMillis) sunriseTimeMillis else sunriseTimeMillis + MILLIS_PER_DAY
        computeWindowProgress(nowMillis, nightStart, nightEnd)
    }
    return SunProgress(isDaytime = isDaytime, progress = progress)
}

internal fun buildCurrentState(
    nowMillis: Long,
    window: PrayerWindow,
    sunAltitude: Double,
    sunProgress: SunProgress,
): CurrentPrayerState {
    val timeRemaining = max(ZERO_MILLIS, window.nextPrayerTime.timeInMillis - nowMillis)
    val totalDuration = max(MIN_WINDOW_MILLIS, window.windowEndMillis - window.windowStartMillis)
    val progress = computeWindowProgress(nowMillis, window.windowStartMillis, window.windowEndMillis)
    return CurrentPrayerState(
        currentPrayer = window.currentPrayer,
        nextPrayer = window.nextPrayer,
        nextPrayerTime = window.nextPrayerTime,
        timeRemainingMillis = timeRemaining,
        totalWindowDurationMillis = totalDuration,
        progressInWindow = progress,
        sunAltitudeDegrees = sunAltitude,
        sunProgressPercent = sunProgress.progress,
        isDaytime = sunProgress.isDaytime,
    )
}
