package tech.sadique.qayam.data.location

import android.content.Context
import android.location.Geocoder
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidGeocoderServiceImpl @Inject constructor(@ApplicationContext private val context: Context) :
    GeocoderService {

    override suspend fun getCityAndCountry(lat: Double, lng: Double): Pair<String, String> =
        withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Geocoder.isPresent()) {
                    return@withContext String.format(Locale.US, "%.2f°, %.2f°", lat, lng) to "GPS Location"
                }
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
            } catch (e: java.io.IOException) {
                Log.w("AndroidGeocoder", "Failed to reverse geocode ($lat, $lng)", e)
                String.format(Locale.US, "%.2f°, %.2f°", lat, lng) to "GPS Location"
            } catch (e: IllegalArgumentException) {
                Log.w("AndroidGeocoder", "Failed to reverse geocode ($lat, $lng)", e)
                String.format(Locale.US, "%.2f°, %.2f°", lat, lng) to "GPS Location"
            } catch (e: IllegalStateException) {
                Log.w("AndroidGeocoder", "Failed to reverse geocode ($lat, $lng)", e)
                String.format(Locale.US, "%.2f°, %.2f°", lat, lng) to "GPS Location"
            }
        }
}
