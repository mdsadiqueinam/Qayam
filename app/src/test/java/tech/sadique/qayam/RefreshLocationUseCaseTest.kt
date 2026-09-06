package tech.sadique.qayam

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.sadique.qayam.data.location.Coordinates
import tech.sadique.qayam.domain.RefreshLocationUseCase
import tech.sadique.qayam.fakes.FakeGeocoderService
import tech.sadique.qayam.fakes.FakeLocationProvider
import tech.sadique.qayam.fakes.FakeSettingsRepository

class RefreshLocationUseCaseTest {

    @Test
    fun `successful GPS fetch updates SettingsRepository and returns location`() = runTest {
        val fakeLocation = FakeLocationProvider(Coordinates(21.4225, 39.8262, 277.0))
        val fakeGeocoder = FakeGeocoderService("Makkah" to "Saudi Arabia")
        val fakeRepo = FakeSettingsRepository()
        val useCase = RefreshLocationUseCase(fakeLocation, fakeGeocoder, fakeRepo)

        val result = useCase()

        assertTrue(result.isSuccess)
        val loc = result.getOrThrow()
        assertEquals("Makkah", loc.cityName)
        assertEquals("Saudi Arabia", loc.countryName)
        assertEquals(21.4225, loc.latitude, 0.001)
        assertEquals(39.8262, loc.longitude, 0.001)
        assertTrue(loc.isGpsBased)

        val savedLoc = fakeRepo.snapshot().currentLocation
        assertEquals("Makkah", savedLoc.cityName)
        assertEquals(21.4225, savedLoc.latitude, 0.001)
    }

    @Test
    fun `failure when GPS location is unavailable`() = runTest {
        val fakeLocation = FakeLocationProvider(stubbedCoordinates = null)
        val fakeGeocoder = FakeGeocoderService("Makkah" to "Saudi Arabia")
        val fakeRepo = FakeSettingsRepository()
        val initialLoc = fakeRepo.snapshot().currentLocation
        val useCase = RefreshLocationUseCase(fakeLocation, fakeGeocoder, fakeRepo)

        val result = useCase()

        assertFalse(result.isSuccess)
        // Repository remains unchanged
        assertEquals(initialLoc, fakeRepo.snapshot().currentLocation)
    }
}
