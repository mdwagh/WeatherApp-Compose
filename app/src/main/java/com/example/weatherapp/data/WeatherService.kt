package com.example.weatherapp.data

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/** Errors this service can throw, mirroring the iOS `WeatherService.WeatherError` enum. */
sealed class WeatherError(message: String) : Exception(message) {
    object CityNotFound : WeatherError("City not found")
    object InvalidResponse : WeatherError("Invalid response from server")
    class Network(cause: Throwable) : WeatherError("Network error: ${cause.message}")
    class Decoding(cause: Throwable) : WeatherError("Failed to decode response: ${cause.message}")
}

/**
 * Fetches weather from Open-Meteo (https://open-meteo.com) - a free, public API that needs no key.
 * City searches use Open-Meteo's geocoding endpoint to resolve a name to coordinates first.
 *
 * There's no Retrofit/OkHttp here on purpose: [HttpURLConnection] and [JSONObject] both ship
 * with the Android SDK, so this has zero extra dependencies - same spirit as iOS's `URLSession`.
 *
 * Every function is a Kotlin `suspend fun`. That's the Kotlin equivalent of Swift's `async`:
 * it lets us write this top-to-bottom like normal blocking code, while `withContext(Dispatchers.IO)`
 * makes sure the actual network call runs on a background thread, never the UI thread.
 */
class WeatherService : WeatherServiceInterface {
    private val geocodingUrl = "https://geocoding-api.open-meteo.com/v1/search"
    private val forecastUrl = "https://api.open-meteo.com/v1/forecast"

    /** Fetches weather data for a specific city, e.g. "London" or "New York". */
    override suspend fun fetchWeather(city: String): WeatherDisplay {
        val place = geocode(city)
        val country = place.country ?: place.countryCode ?: ""
        return fetchWeather(place.latitude, place.longitude, place.name, country)
    }

    /** Fetches weather using latitude/longitude, e.g. from the device's location. */
    override suspend fun fetchWeather(
        latitude: Double,
        longitude: Double,
        city: String,
        country: String,
    ): WeatherDisplay = withContext(Dispatchers.IO) {
        val url = Uri.parse(forecastUrl).buildUpon()
            .appendQueryParameter("latitude", latitude.toString())
            .appendQueryParameter("longitude", longitude.toString())
            .appendQueryParameter(
                "current",
                "temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m,surface_pressure,visibility"
            )
            .appendQueryParameter("daily", "sunrise,sunset")
            .appendQueryParameter("timezone", "auto")
            .appendQueryParameter("wind_speed_unit", "ms")
            .build()
            .toString()

        val json = requestJson(url)
        try {
            WeatherDisplay.fromForecastJson(json, city, country)
        } catch (e: Exception) {
            throw WeatherError.Decoding(e)
        }
    }

    /** Result of resolving a city name to a place: coordinates + display name. */
    private data class GeoResult(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val country: String?,
        val countryCode: String?,
    )

    /** Resolves a city name to its first matching geographic result. */
    private suspend fun geocode(city: String): GeoResult = withContext(Dispatchers.IO) {
        val url = Uri.parse(geocodingUrl).buildUpon()
            .appendQueryParameter("name", city)
            .appendQueryParameter("count", "1")
            .appendQueryParameter("language", "en")
            .appendQueryParameter("format", "json")
            .build()
            .toString()

        val json = requestJson(url)
        val results = json.optJSONArray("results")
        if (results == null || results.length() == 0) {
            throw WeatherError.CityNotFound
        }
        val first = results.getJSONObject(0)
        GeoResult(
            name = first.getString("name"),
            latitude = first.getDouble("latitude"),
            longitude = first.getDouble("longitude"),
            country = first.optString("country").ifEmpty { null },
            countryCode = first.optString("country_code").ifEmpty { null },
        )
    }

    /** Performs a GET request, validating the HTTP status and normalizing errors. */
    private fun requestJson(urlString: String): JSONObject {
        Log.d(TAG, "Request -> $urlString")

        val connection = try {
            (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000 // 15s, matches the iOS per-request timeout
                readTimeout = 15_000
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open connection to $urlString", e)
            throw WeatherError.Network(e)
        }

        try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                Log.e(TAG, "Request failed <- $urlString (HTTP $responseCode)")
                throw WeatherError.InvalidResponse
            }

            val body = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            return try {
                val json = JSONObject(body)
                Log.d(TAG, "Request succeeded <- $urlString (HTTP $responseCode)")
                Log.d(TAG, "Response body:\n${json.toString(2)}")
                json
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse JSON from $urlString:\n$body", e)
                throw WeatherError.Decoding(e)
            }
        } catch (e: WeatherError) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Request failed <- $urlString", e)
            throw WeatherError.Network(e)
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TAG = "WeatherService"
    }
}
