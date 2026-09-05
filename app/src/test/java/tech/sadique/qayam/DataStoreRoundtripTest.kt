package tech.sadique.qayam

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import tech.sadique.qayam.data.model.CalculationMethod
import tech.sadique.qayam.data.model.PrayerType
import tech.sadique.qayam.data.preferences.AppSettings
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** DataStore integration: write-then-read roundtrip (self-cleaning). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DataStoreRoundtripTest {

    @Test
    fun `datastore write then snapshot roundtrip`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings = AppSettings(context)
        try {
            settings.updateCalculationMethod(CalculationMethod.KARACHI)
            settings.updatePrayerMinuteOffset(PrayerType.FAJR, 5)

            val snapshot = AppSettings(context).snapshot()
            assertEquals(CalculationMethod.KARACHI, snapshot.calculationMethod)
            assertEquals(5, snapshot.minuteOffsets[PrayerType.FAJR])
        } finally {
            settings.resetToDefaults()
        }
    }
}
