package com.example.weatherapp.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Errors this service can throw, mirroring the iOS `LocationManager.LocationError` enum. */
sealed class LocationError(message: String) : Exception(message) {
    object Unavailable : LocationError("Unable to determine your current location.")
}

/** A resolved place name for display, e.g. ("London", "United Kingdom"). */
data class PlaceName(val city: String, val country: String)

/**
 * Wraps Android's built-in [LocationManager] to provide a single, one-shot "current location"
 * lookup - the Android counterpart to iOS's `CLLocationManager`.
 *
 * We deliberately use `android.location.LocationManager` (part of the Android SDK) instead of
 * Google Play Services' `FusedLocationProviderClient`, since the latter is an extra library.
 *
 * This class assumes the location permission has already been granted - requesting the
 * permission from the user is a UI concern and is handled by MainActivity.
 */
class LocationService(private val context: Context) : LocationServiceInterface {
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    /** Requests the device's current location once. Suspends until a fix arrives or fails. */
    @SuppressLint("MissingPermission") // Caller (MainActivity) checks the permission first.
    override suspend fun requestCurrentLocation(): Location = suspendCancellableCoroutine { continuation ->
        val provider = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }

        if (provider == null) {
            continuation.resumeWithException(LocationError.Unavailable)
            return@suspendCancellableCoroutine
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                locationManager.removeUpdates(this)
                if (continuation.isActive) continuation.resume(location)
            }
        }

        continuation.invokeOnCancellation { locationManager.removeUpdates(listener) }

        // requestSingleUpdate is deprecated but remains the simplest one-shot API on all
        // supported versions; we run it on the main looper since it only fires once.
        @Suppress("DEPRECATION")
        locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
    }

    /**
     * Reverse-geocodes a coordinate into a display-friendly city and country.
     * Falls back to "Current Location" if no address is found (e.g. on some emulators
     * without Google Play services, where the on-device geocoder is unavailable).
     */
    override suspend fun placeName(latitude: Double, longitude: Double): PlaceName =
        withContext(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION") // The synchronous overload is fine off the main thread.
                val addresses = Geocoder(context, Locale.getDefault())
                    .getFromLocation(latitude, longitude, 1)
                val address = addresses?.firstOrNull()
                PlaceName(
                    city = address?.locality ?: address?.subAdminArea ?: "Current Location",
                    country = address?.countryName ?: "",
                )
            } catch (e: Exception) {
                PlaceName(city = "Current Location", country = "")
            }
        }
}
