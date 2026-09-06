package tech.sadique.qayam.domain

import tech.sadique.qayam.data.location.GeocoderService
import tech.sadique.qayam.data.location.LocationProvider
import tech.sadique.qayam.data.model.LocationInfo
import tech.sadique.qayam.data.preferences.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RefreshLocationUseCase @Inject constructor(
    private val locationProvider: LocationProvider,
    private val geocoderService: GeocoderService,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(): Result<LocationInfo> {
        val coords = locationProvider.getCurrentLocation()
            ?: return Result.failure(IllegalStateException("GPS location unavailable"))

        val cityInfo = geocoderService.getCityAndCountry(coords.latitude, coords.longitude)
        val gpsLoc = LocationInfo(
            latitude = coords.latitude,
            longitude = coords.longitude,
            altitude = coords.altitude,
            cityName = cityInfo.first,
            countryName = cityInfo.second,
            isGpsBased = true,
            lastUpdatedMillis = System.currentTimeMillis()
        )
        settingsRepository.updateLocation(gpsLoc)
        return Result.success(gpsLoc)
    }
}
