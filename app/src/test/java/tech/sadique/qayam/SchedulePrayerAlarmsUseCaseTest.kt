package tech.sadique.qayam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.sadique.qayam.data.model.AdhanSoundType
import tech.sadique.qayam.data.model.PrayerType
import tech.sadique.qayam.data.preferences.UserSettings
import tech.sadique.qayam.fakes.FakeAlarmScheduler
import tech.sadique.qayam.notification.SchedulePrayerAlarmsUseCase

class SchedulePrayerAlarmsUseCaseTest {

    @Test
    fun `schedules alarms for all enabled daily prayers`() {
        val fakeScheduler = FakeAlarmScheduler()
        val useCase = SchedulePrayerAlarmsUseCase(fakeScheduler)
        val settings = UserSettings()

        useCase(settings)

        // 6 daily prayers enabled by default (FAJR, ISRAQ, DHUHR, ASR, MAGHRIB, ISHA)
        assertEquals(6, fakeScheduler.scheduledAlarms.size)
        assertTrue(fakeScheduler.scheduledAlarms.containsKey(PrayerType.FAJR))
        assertTrue(fakeScheduler.scheduledAlarms.containsKey(PrayerType.ISRAQ))
        assertTrue(fakeScheduler.scheduledAlarms.containsKey(PrayerType.DHUHR))
        assertTrue(fakeScheduler.scheduledAlarms.containsKey(PrayerType.ASR))
        assertTrue(fakeScheduler.scheduledAlarms.containsKey(PrayerType.MAGHRIB))
        assertTrue(fakeScheduler.scheduledAlarms.containsKey(PrayerType.ISHA))
    }

    @Test
    fun `cancels disabled prayers and does not schedule them`() {
        val fakeScheduler = FakeAlarmScheduler()
        val useCase = SchedulePrayerAlarmsUseCase(fakeScheduler)
        val settings = UserSettings(
            prayerAlertEnabled = mapOf(
                PrayerType.FAJR to false,
                PrayerType.DHUHR to true
            )
        )

        useCase(settings)

        assertFalse(fakeScheduler.scheduledAlarms.containsKey(PrayerType.FAJR))
        assertTrue(fakeScheduler.cancelledAlarms.contains(PrayerType.FAJR))
        assertTrue(fakeScheduler.scheduledAlarms.containsKey(PrayerType.DHUHR))
    }

    @Test
    fun `honors custom sound and priority settings`() {
        val fakeScheduler = FakeAlarmScheduler()
        val useCase = SchedulePrayerAlarmsUseCase(fakeScheduler)
        val settings = UserSettings(
            highPrioritySound = true,
            prayerAlertSounds = mapOf(
                PrayerType.FAJR to AdhanSoundType.MADINAH
            )
        )

        useCase(settings)

        val fajrAlarm = fakeScheduler.scheduledAlarms[PrayerType.FAJR]
        assertEquals(AdhanSoundType.MADINAH, fajrAlarm?.soundType)
        assertEquals(true, fajrAlarm?.highPriority)
    }
}
