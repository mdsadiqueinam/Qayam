package tech.sadique.qayam

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import tech.sadique.qayam.data.model.AdhanSoundType
import tech.sadique.qayam.data.model.CalculationMethod
import tech.sadique.qayam.data.model.JuristicMethod
import tech.sadique.qayam.data.model.PrayerType
import tech.sadique.qayam.data.preferences.toUserSettings
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure mapping tests: defaults + explicitly stored values. */
class PreferencesMappingTest {

    @Test
    fun `empty prefs map to documented defaults`() {
        val s = mutablePreferencesOf().toUserSettings()
        assertEquals(CalculationMethod.MUSLIM_WORLD_LEAGUE, s.calculationMethod)
        assertEquals(JuristicMethod.STANDARD, s.juristicMethod)
        assertEquals(21.4225, s.currentLocation.latitude, 0.0)
        assertEquals(39.8262, s.currentLocation.longitude, 0.0)
        assertEquals("Makkah", s.currentLocation.cityName)
        assertEquals(
            PrayerType.dailyPrayers.associateWith { it.defaultAlertEnabled },
            s.prayerAlertEnabled
        )
        assertEquals(AdhanSoundType.SILENT, s.prayerAlertSounds[PrayerType.SUNRISE])
    }

    @Test
    fun `stored ids and values map correctly`() {
        val s = mutablePreferencesOf(
            stringPreferencesKey("calc_method") to CalculationMethod.EGYPT.id,
            stringPreferencesKey("juristic_method") to JuristicMethod.HANAFI.id,
            doublePreferencesKey("loc_lat") to 30.0444,
            doublePreferencesKey("loc_lng") to 31.2357,
            stringPreferencesKey("loc_city") to "Cairo",
            stringPreferencesKey("loc_country") to "Egypt",
            booleanPreferencesKey("enabled_${PrayerType.SUNRISE.id}") to true,
            intPreferencesKey("offset_${PrayerType.FAJR.id}") to 5,
            stringPreferencesKey("sound_${PrayerType.ASR.id}") to AdhanSoundType.MADINAH.id
        ).toUserSettings()
        assertEquals(CalculationMethod.EGYPT, s.calculationMethod)
        assertEquals(JuristicMethod.HANAFI, s.juristicMethod)
        assertEquals(30.0444, s.currentLocation.latitude, 0.0)
        assertEquals(31.2357, s.currentLocation.longitude, 0.0)
        assertEquals("Cairo", s.currentLocation.cityName)
        assertEquals(true, s.prayerAlertEnabled[PrayerType.SUNRISE])
        assertEquals(5, s.minuteOffsets[PrayerType.FAJR])
        assertEquals(AdhanSoundType.MADINAH, s.prayerAlertSounds[PrayerType.ASR])
    }
}
