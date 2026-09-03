package com.example.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.util.Log
import com.example.data.model.LocationInfo
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

class LocationService(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    companion object {
        val CITY_PRESETS = listOf(
            LocationInfo(21.4225, 39.8262, 277.0, "Makkah", "Saudi Arabia", isGpsBased = false),
            LocationInfo(24.4672, 39.6111, 608.0, "Madinah", "Saudi Arabia", isGpsBased = false),
            LocationInfo(31.7761, 35.2358, 754.0, "Jerusalem (Al-Quds)", "Palestine", isGpsBased = false),
            LocationInfo(25.2048, 55.2708, 16.0, "Dubai", "United Arab Emirates", isGpsBased = false),
            LocationInfo(30.0444, 31.2357, 23.0, "Cairo", "Egypt", isGpsBased = false),
            LocationInfo(41.0082, 28.9784, 40.0, "Istanbul", "Turkey", isGpsBased = false),
            LocationInfo(24.8607, 67.0011, 8.0, "Karachi", "Pakistan", isGpsBased = false),
            LocationInfo(28.6139, 77.2090, 216.0, "New Delhi", "India", isGpsBased = false),
            LocationInfo(23.8103, 90.4125, 12.0, "Dhaka", "Bangladesh", isGpsBased = false),
            LocationInfo(3.1390, 101.6869, 66.0, "Kuala Lumpur", "Malaysia", isGpsBased = false),
            LocationInfo(-6.2088, 106.8456, 8.0, "Jakarta", "Indonesia", isGpsBased = false),
            LocationInfo(51.5074, -0.1278, 35.0, "London", "United Kingdom", isGpsBased = false),
            LocationInfo(40.7128, -74.0060, 10.0, "New York", "United States", isGpsBased = false),
            LocationInfo(43.6532, -79.3832, 76.0, "Toronto", "Canada", isGpsBased = false),
            LocationInfo(48.8566, 2.3522, 35.0, "Paris", "France", isGpsBased = false),
            LocationInfo(1.3521, 103.8198, 15.0, "Singapore", "Singapore", isGpsBased = false),
            LocationInfo(-33.8688, 151.2093, 3.0, "Sydney", "Australia", isGpsBased = false)
        )
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentGpsLocation(): LocationInfo? = withContext(Dispatchers.IO) {
        try {
            val cts = CancellationTokenSource()
            val location: Location? = suspendCancellableCoroutine { continuation ->
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { loc ->
                        if (continuation.isActive) continuation.resume(loc)
                    }
                    .addOnFailureListener {
                        // Fallback to last known location
                        fusedLocationClient.lastLocation
                            .addOnSuccessListener { lastLoc ->
                                if (continuation.isActive) continuation.resume(lastLoc)
                            }
                            .addOnFailureListener {
                                if (continuation.isActive) continuation.resume(null)
                            }
                    }

                continuation.invokeOnCancellation {
                    cts.cancel()
                }
            }

            if (location != null) {
                val cityInfo = getCityNameFromCoordinates(location.latitude, location.longitude)
                LocationInfo(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitude = location.altitude,
                    cityName = cityInfo.first,
                    countryName = cityInfo.second,
                    isGpsBased = true,
                    lastUpdatedMillis = System.currentTimeMillis()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("LocationService", "Error retrieving GPS location", e)
            null
        }
    }

    private fun getCityNameFromCoordinates(lat: Double, lng: Double): Pair<String, String> {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val city = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "Nearby"
                val country = addr.countryName ?: ""
                city to country
            } else {
                String.format(Locale.US, "%.2f°, %.2f°", lat, lng) to "GPS Location"
            }
        } catch (e: Exception) {
            String.format(Locale.US, "%.2f°, %.2f°", lat, lng) to "GPS Location"
        }
    }
}
