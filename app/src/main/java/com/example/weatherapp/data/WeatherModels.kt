package com.example.weatherapp.data

import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Maps Open-Meteo's numeric WMO weather codes to human-readable text and an emoji icon.
 * Reference: https://open-meteo.com/en/docs (WMO Weather interpretation codes)
 *
 * We use emoji instead of an icon library or SF-Symbol-style icon font, so no extra
 * dependency is needed to show a weather glyph.
 */
class WeatherCode(private val code: Int) {

    val condition: String
        get() = when (code) {
            0 -> "Clear"
            1 -> "Mainly Clear"
            2 -> "Partly Cloudy"
            3 -> "Overcast"
            45, 48 -> "Fog"
            51, 53, 55 -> "Drizzle"
            56, 57 -> "Freezing Drizzle"
            61, 63, 65 -> "Rain"
            66, 67 -> "Freezing Rain"
            71, 73, 75, 77 -> "Snow"
            80, 81, 82 -> "Rain Showers"
            85, 86 -> "Snow Showers"
            95 -> "Thunderstorm"
            96, 99 -> "Thunderstorm with Hail"
            else -> "Unknown"
        }

    val description: String
        get() = when (code) {
            0 -> "Clear sky"
            1 -> "Mainly clear sky"
            2 -> "Partly cloudy"
            3 -> "Overcast clouds"
            45 -> "Foggy conditions"
            48 -> "Depositing rime fog"
            51 -> "Light drizzle"
            53 -> "Moderate drizzle"
            55 -> "Dense drizzle"
            56, 57 -> "Freezing drizzle"
            61 -> "Slight rain"
            63 -> "Moderate rain"
            65 -> "Heavy rain"
            66, 67 -> "Freezing rain"
            71 -> "Slight snowfall"
            73 -> "Moderate snowfall"
            75 -> "Heavy snowfall"
            77 -> "Snow grains"
            80 -> "Slight rain showers"
            81 -> "Moderate rain showers"
            82 -> "Violent rain showers"
            85 -> "Slight snow showers"
            86 -> "Heavy snow showers"
            95 -> "Thunderstorm"
            96, 99 -> "Thunderstorm with hail"
            else -> ""
        }

    /** Emoji standing in for iOS's SF Symbol icon. */
    val emoji: String
        get() = when (code) {
            0, 1 -> "☀️"
            2 -> "⛅"
            3 -> "☁️"
            45, 48 -> "🌫️"
            51, 53, 55, 56, 57 -> "🌦️"
            61, 63, 66, 67, 80, 81 -> "🌧️"
            65, 82 -> "🌧️"
            71, 73, 75, 77, 85, 86 -> "🌨️"
            95, 96, 99 -> "⛈️"
            else -> "☁️"
        }
}

/**
 * UI-friendly weather model. Holds raw metric values from the API; display strings are
 * produced on demand for the caller's chosen units, so changing a unit reformats without
 * re-fetching - same idea as the iOS `WeatherDisplay` struct.
 */
class WeatherDisplay(
    val city: String,
    val country: String,
    private val temperatureC: Double,
    private val feelsLikeC: Double,
    private val humidityPercent: Int,
    private val windSpeedMS: Double,
    private val visibilityM: Double,
    private val pressureHPa: Double,
    private val weatherCode: WeatherCode,
    val sunrise: String,
    val sunset: String,
) {
    val condition: String get() = weatherCode.condition
    val description: String get() = weatherCode.description
    val emoji: String get() = weatherCode.emoji

    fun temperature(unit: TemperatureUnit): String = unit.format(temperatureC)
    fun feelsLike(unit: TemperatureUnit): String = unit.format(feelsLikeC)
    fun windSpeed(unit: DistanceUnit): String = unit.formatSpeed(windSpeedMS)
    fun visibility(unit: DistanceUnit): String = unit.formatDistance(visibilityM)

    val humidity: String get() = "$humidityPercent%"
    val pressure: String get() = "${pressureHPa.toInt()} hPa"

    companion object {
        // Open-Meteo returns local time like "2026-07-09T04:55" (no timezone offset/seconds).
        private val API_TIME_PARSER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
        private val DISPLAY_TIME_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

        private fun formatTime(isoString: String?): String {
            if (isoString == null) return "--"
            return try {
                LocalDateTime.parse(isoString, API_TIME_PARSER).format(DISPLAY_TIME_FORMAT)
            } catch (e: Exception) {
                "--"
            }
        }

        /**
         * Builds a [WeatherDisplay] from Open-Meteo's `/v1/forecast` JSON body.
         * City/country come from the geocoding step, since the forecast API doesn't return them.
         */
        fun fromForecastJson(json: JSONObject, city: String, country: String): WeatherDisplay {
            val current = json.getJSONObject("current")
            val daily = json.getJSONObject("daily")
            val code = WeatherCode(current.getInt("weather_code"))

            return WeatherDisplay(
                city = city,
                country = country,
                temperatureC = current.getDouble("temperature_2m"),
                feelsLikeC = current.getDouble("apparent_temperature"),
                humidityPercent = current.getInt("relative_humidity_2m"),
                windSpeedMS = current.getDouble("wind_speed_10m"),
                visibilityM = current.getDouble("visibility"),
                pressureHPa = current.getDouble("surface_pressure"),
                weatherCode = code,
                sunrise = formatTime(daily.getJSONArray("sunrise").optString(0).ifEmpty { null }),
                sunset = formatTime(daily.getJSONArray("sunset").optString(0).ifEmpty { null }),
            )
        }
    }
}
