package tech.sadique.qayam.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

@Singleton
class FusedLocationProviderImpl @Inject constructor(@ApplicationContext private val context: Context) :
    LocationProvider {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    override suspend fun getCurrentLocation(): Coordinates? = withContext(Dispatchers.IO) {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        if (!hasPerm) {
            Log.w("LocationProvider", "Location permission not granted")
            return@withContext null
        }
        try {
            val location: Location? = withTimeoutOrNull(10.seconds) {
                suspendCancellableCoroutine { continuation ->
                    val cts = CancellationTokenSource()
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                        .addOnSuccessListener { loc ->
                            if (continuation.isActive) continuation.resume(loc)
                        }
                        .addOnFailureListener {
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
            } ?: fetchLastKnownLocation()

            val resolved = location ?: fetchLastKnownLocation()

            resolved?.let {
                Coordinates(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    altitude = it.altitude,
                )
            }
        } catch (e: SecurityException) {
            Log.e("LocationProvider", "Error retrieving GPS location", e)
            null
        } catch (e: IllegalStateException) {
            Log.e("LocationProvider", "Error retrieving GPS location", e)
            null
        } catch (e: IllegalArgumentException) {
            Log.e("LocationProvider", "Error retrieving GPS location", e)
            null
        }
    }

    private suspend fun fetchLastKnownLocation(): Location? = suspendCancellableCoroutine { continuation ->
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
                .addOnFailureListener { if (continuation.isActive) continuation.resume(null) }
        } catch (e: SecurityException) {
            Log.w("LocationProvider", "Security exception fetching last known location", e)
            if (continuation.isActive) continuation.resume(null)
        }
    }
}
