package tech.sadique.qayam

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import tech.sadique.qayam.data.calculator.PrayerTimeCalculator
import tech.sadique.qayam.data.model.CalculationMethod
import tech.sadique.qayam.data.model.HighLatitudeRule
import tech.sadique.qayam.data.model.JuristicMethod
import tech.sadique.qayam.data.model.PrayerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Qayam", appName)
    }

    @Test
    fun `test prayer time calculation for Makkah`() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 1, 12, 0, 0)
        }
        // Makkah coordinates
        val schedule = PrayerTimeCalculator.calculateSchedule(
            date = cal,
            latitude = 21.4225,
            longitude = 39.8262,
            timezoneOffsetHours = 3.0,
            method = CalculationMethod.UMM_AL_QURA,
            juristic = JuristicMethod.STANDARD,
            highLatitudeRule = HighLatitudeRule.ANGLE_BASED
        )

        assertNotNull(schedule)
        assertNotNull(schedule.fajr)
        assertNotNull(schedule.sunrise)
        assertNotNull(schedule.israq)
        assertNotNull(schedule.dhuhr)
        assertNotNull(schedule.asr)
        assertNotNull(schedule.gurubAftab)
        assertNotNull(schedule.maghrib)
        assertNotNull(schedule.isha)

        // Sunrise (Tulub e Aftab) should be after Fajr
        assertTrue(schedule.sunrise.timeInMillis > schedule.fajr.timeInMillis)
        // Israq starts exactly 20 minutes after Sunrise
        assertEquals(schedule.sunrise.timeInMillis + 20 * 60 * 1000L, schedule.israq.timeInMillis)
        // Dhuhr should be after Israq
        assertTrue(schedule.dhuhr.timeInMillis > schedule.israq.timeInMillis)
        // Asr should be after Dhuhr
        assertTrue(schedule.asr.timeInMillis > schedule.dhuhr.timeInMillis)
        // Gurub e Aftab (Sunset) should be after Asr
        assertTrue(schedule.gurubAftab.timeInMillis > schedule.asr.timeInMillis)
        // Maghrib should be at or right after Gurub e Aftab
        assertTrue(schedule.maghrib.timeInMillis >= schedule.gurubAftab.timeInMillis)
        // Isha should be after Maghrib
        assertTrue(schedule.isha.timeInMillis > schedule.maghrib.timeInMillis)

        // Check naming
        assertEquals("Sunrise", PrayerType.SUNRISE.displayName)
        assertEquals("شُرُوق الشَّمْس", PrayerType.SUNRISE.arabicName)
        assertEquals("Israq", PrayerType.ISRAQ.displayName)
        assertEquals("Sunset", PrayerType.GURUB_E_AFTAB.displayName)
        assertEquals("غُروب الشَّمْس", PrayerType.GURUB_E_AFTAB.arabicName)
    }

    @Test
    fun `test build prayer notification for audible adhan uses primary channel and has stop action`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val notificationManager = tech.sadique.qayam.notification.AdhanNotificationManager(context)
        val notification = notificationManager.buildPrayerNotification(
            prayerType = PrayerType.FAJR,
            soundType = tech.sadique.qayam.data.model.AdhanSoundType.MAKKAH,
            highPriority = true
        )

        assertNotNull(notification)
        assertEquals(SalahApp.ADHAN_CHANNEL_ID, notification.channelId)
        assertEquals(android.R.drawable.ic_lock_idle_alarm, notification.smallIcon.resId)
        assertNotNull(notification.actions)
        assertTrue(notification.actions.any { it.title.toString().contains("Stop Adhan") })
    }

    @Test
    fun `test build prayer notification for SILENT uses silent channel and has no stop action`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val notificationManager = tech.sadique.qayam.notification.AdhanNotificationManager(context)
        val notification = notificationManager.buildPrayerNotification(
            prayerType = PrayerType.DHUHR,
            soundType = tech.sadique.qayam.data.model.AdhanSoundType.SILENT,
            highPriority = true
        )

        assertNotNull(notification)
        assertEquals(SalahApp.ADHAN_SILENT_CHANNEL_ID, notification.channelId)
        val hasStopAction = notification.actions?.any { it.title.toString().contains("Stop Adhan") } ?: false
        assertFalse(hasStopAction)
    }

    @Test
    fun `test build prayer notification for VIBRATE_ONLY uses vibrate channel and has no stop action`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val notificationManager = tech.sadique.qayam.notification.AdhanNotificationManager(context)
        val notification = notificationManager.buildPrayerNotification(
            prayerType = PrayerType.ASR,
            soundType = tech.sadique.qayam.data.model.AdhanSoundType.VIBRATE_ONLY,
            highPriority = true
        )

        assertNotNull(notification)
        assertEquals(SalahApp.ADHAN_VIBRATE_CHANNEL_ID, notification.channelId)
        val hasStopAction = notification.actions?.any { it.title.toString().contains("Stop Adhan") } ?: false
        assertFalse(hasStopAction)
    }

    @Test
    fun `test boot receiver can receive boot intent without error`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bootReceiver = tech.sadique.qayam.receiver.BootReceiver()
        val intent = android.content.Intent(android.content.Intent.ACTION_BOOT_COMPLETED)
        bootReceiver.onReceive(context, intent)
    }
}
