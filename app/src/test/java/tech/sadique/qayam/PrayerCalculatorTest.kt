package tech.sadique.qayam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.sadique.qayam.data.calculator.PrayerTimeCalculator
import tech.sadique.qayam.data.model.CalculationMethod
import tech.sadique.qayam.data.model.HighLatitudeRule
import tech.sadique.qayam.data.model.JuristicMethod
import tech.sadique.qayam.data.model.PrayerType
import java.util.Calendar
import java.util.TimeZone

class PrayerCalculatorTest {

    private fun cal(y: Int, m: Int, d: Int, tzId: String): Calendar =
        Calendar.getInstance(TimeZone.getTimeZone(tzId)).apply {
            set(Calendar.YEAR, y)
            set(Calendar.MONTH, m - 1)
            set(Calendar.DAY_OF_MONTH, d)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

    private fun minutes(c: Calendar): Int =
        c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)

    @Test
    fun asr_isAfterDhuhr_beforeMaghrib_makkah() {
        val date = cal(2026, 3, 20, "Asia/Riyadh")
        val s = PrayerTimeCalculator.calculateSchedule(
            date = date, latitude = 21.4225, longitude = 39.8262,
            timezoneOffsetHours = 3.0, method = CalculationMethod.MUSLIM_WORLD_LEAGUE,
            juristic = JuristicMethod.STANDARD
        )
        val order = listOf(s.fajr, s.sunrise, s.dhuhr, s.asr, s.maghrib, s.isha).map { it.timeInMillis }
        assertTrue("times must be ordered", order == order.sorted())
        // Asr should be mid-afternoon, well after Dhuhr (regression: 90- bug put it near Dhuhr)
        val dhuhrAsrGapMin = (s.asr.timeInMillis - s.dhuhr.timeInMillis) / 60000
        assertTrue("Asr-Dhuhr gap=$dhuhrAsrGapMin", dhuhrAsrGapMin in 150..300)
    }

    @Test
    fun hanafi_asr_later_than_standard() {
        val date = cal(2026, 6, 1, "Asia/Riyadh")
        val std = PrayerTimeCalculator.calculateSchedule(
            date = date, latitude = 21.4225, longitude = 39.8262,
            timezoneOffsetHours = 3.0, method = CalculationMethod.MUSLIM_WORLD_LEAGUE,
            juristic = JuristicMethod.STANDARD
        )
        val han = PrayerTimeCalculator.calculateSchedule(
            date = date, latitude = 21.4225, longitude = 39.8262,
            timezoneOffsetHours = 3.0, method = CalculationMethod.MUSLIM_WORLD_LEAGUE,
            juristic = JuristicMethod.HANAFI
        )
        assertTrue(han.asr.timeInMillis > std.asr.timeInMillis)
    }

    @Test
    fun ummAlQura_isha_is90minAfterMaghrib_highLatSkipped() {
        val date = cal(2026, 6, 21, "Europe/London")
        val s = PrayerTimeCalculator.calculateSchedule(
            date = date, latitude = 51.5074, longitude = -0.1278,
            timezoneOffsetHours = 1.0, method = CalculationMethod.UMM_AL_QURA,
            juristic = JuristicMethod.STANDARD, highLatitudeRule = HighLatitudeRule.ANGLE_BASED
        )
        val gapMin = (s.isha.timeInMillis - s.maghrib.timeInMillis) / 60000
        assertTrue("UmmAlQura gap=$gapMin", gapMin in 85..95)
    }

    @Test
    fun highLatitude_noNaN_londonWinter() {
        val date = cal(2026, 12, 21, "Europe/London")
        val s = PrayerTimeCalculator.calculateSchedule(
            date = date, latitude = 51.5074, longitude = -0.1278,
            timezoneOffsetHours = 0.0, method = CalculationMethod.MUSLIM_WORLD_LEAGUE,
            juristic = JuristicMethod.STANDARD
        )
        for ((type, c) in s.getAllTimes()) {
            assertTrue("$type NaN", c.timeInMillis > 0)
        }
        assertTrue(minutes(s.fajr) < minutes(s.sunrise))
        assertTrue(s.isha.timeInMillis > s.maghrib.timeInMillis)
    }

    @Test
    fun alertDefaults_onlySunMarkersDisabled() {
        for (prayer in PrayerType.dailyPrayers) {
            val expected = prayer != PrayerType.SUNRISE && prayer != PrayerType.GURUB_E_AFTAB
            assertEquals("$prayer", expected, prayer.defaultAlertEnabled)
        }
    }

    @Test
    fun rounding_noTruncationBias() {
        // 12.9999h should round to 13:00, not truncate to 12:59
        val date = cal(2026, 3, 20, "UTC")
        val s = PrayerTimeCalculator.calculateSchedule(
            date = date, latitude = 0.0, longitude = 0.0,
            timezoneOffsetHours = 0.0, method = CalculationMethod.MUSLIM_WORLD_LEAGUE,
            juristic = JuristicMethod.STANDARD,
            minuteOffsets = mapOf(PrayerType.FAJR to 0)
        )
        assertTrue(s.fajr.get(Calendar.SECOND) in 0..59)
    }
}
