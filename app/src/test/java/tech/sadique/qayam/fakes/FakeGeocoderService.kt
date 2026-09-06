package tech.sadique.qayam.fakes

import tech.sadique.qayam.data.location.GeocoderService

class FakeGeocoderService(var stubbedResult: Pair<String, String> = "Makkah" to "Saudi Arabia") : GeocoderService {
    override suspend fun getCityAndCountry(lat: Double, lng: Double): Pair<String, String> = stubbedResult
}
