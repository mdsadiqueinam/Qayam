package tech.sadique.qayam.fakes

import tech.sadique.qayam.data.location.Coordinates
import tech.sadique.qayam.data.location.LocationProvider

class FakeLocationProvider(var stubbedCoordinates: Coordinates? = Coordinates(21.4225, 39.8262, 277.0)) :
    LocationProvider {
    override suspend fun getCurrentLocation(): Coordinates? = stubbedCoordinates
}
