package tech.sadique.qayam

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import tech.sadique.qayam.data.model.AdhanSoundType
import tech.sadique.qayam.data.model.AppThemeMode
import tech.sadique.qayam.data.model.CalculationMethod
import tech.sadique.qayam.data.model.HighLatitudeRule
import tech.sadique.qayam.data.model.JuristicMethod
import tech.sadique.qayam.data.model.PrayerType
import tech.sadique.qayam.data.preferences.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsDefaultsTest {

    @Test
    fun `fresh settings match documented defaults`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings = AppSettings(context).settings.value

        assertEquals(CalculationMethod.MUSLIM_WORLD_LEAGUE, settings.calculationMethod)
        assertEquals(JuristicMethod.STANDARD, settings.juristicMethod)
        assertEquals(HighLatitudeRule.ANGLE_BASED, settings.highLatitudeRule)
        assertEquals(AppThemeMode.SYSTEM, settings.themeMode)
        assertEquals(21.4225, settings.currentLocation.latitude, 0.0001)
        assertEquals(39.8262, settings.currentLocation.longitude, 0.0001)

        // Alert defaults must equal the single source of truth on PrayerType.
        val expectedEnabled = PrayerType.dailyPrayers.associateWith { it.defaultAlertEnabled }
        assertEquals(expectedEnabled, settings.prayerAlertEnabled)

        assertEquals(AdhanSoundType.SILENT, settings.prayerAlertSounds[PrayerType.SUNRISE])
        assertEquals(AdhanSoundType.SILENT, settings.prayerAlertSounds[PrayerType.GURUB_E_AFTAB])
        assertEquals(AdhanSoundType.MAKKAH, settings.prayerAlertSounds[PrayerType.FAJR])
    }
}
