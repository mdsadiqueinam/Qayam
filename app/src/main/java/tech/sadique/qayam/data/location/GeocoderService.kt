package tech.sadique.qayam.data.location

interface GeocoderService {
    suspend fun getCityAndCountry(lat: Double, lng: Double): Pair<String, String>
}
