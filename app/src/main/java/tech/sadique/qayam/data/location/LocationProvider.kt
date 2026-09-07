package tech.sadique.qayam.data.location

interface LocationProvider {
    suspend fun getCurrentLocation(): Coordinates?
}
