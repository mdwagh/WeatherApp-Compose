package com.example.weatherapp

import com.example.weatherapp.data.WeatherDisplay
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class WeatherDisplayParsingTest {

    private fun forecastJson(
        tempC: Double = 20.0,
        feelsLike: Double = 18.0,
        humidity: Int = 65,
        windSpeed: Double = 5.2,
        visibility: Double = 10_000.0,
        pressure: Double = 1013.0,
        weatherCode: Int = 0,
        sunrise: String = "2024-01-15T07:30",
        sunset: String = "2024-01-15T17:45",
    ): JSONObject = JSONObject(
        """
        {
          "current": {
            "temperature_2m": $tempC,
            "apparent_temperature": $feelsLike,
            "relative_humidity_2m": $humidity,
            "wind_speed_10m": $windSpeed,
            "visibility": $visibility,
            "surface_pressure": $pressure,
            "weather_code": $weatherCode
          },
          "daily": {
            "sunrise": ["$sunrise"],
            "sunset": ["$sunset"]
          }
        }
        """.trimIndent()
    )

    @Test
    fun `city and country come from arguments not json`() {
        val display = WeatherDisplay.fromForecastJson(forecastJson(), "London", "United Kingdom")
        assertEquals("London", display.city)
        assertEquals("United Kingdom", display.country)
    }

    @Test
    fun `weather condition maps from code`() {
        val display = WeatherDisplay.fromForecastJson(forecastJson(weatherCode = 3), "London", "UK")
        assertEquals("Overcast", display.condition)
        assertEquals("Overcast clouds", display.description)
        assertEquals("☁️", display.emoji)
    }

    @Test
    fun `humidity formatted with percent sign`() {
        val display = WeatherDisplay.fromForecastJson(forecastJson(humidity = 72), "London", "UK")
        assertEquals("72%", display.humidity)
    }

    @Test
    fun `pressure formatted with hPa`() {
        val display = WeatherDisplay.fromForecastJson(forecastJson(pressure = 1013.0), "London", "UK")
        assertEquals("1013 hPa", display.pressure)
    }

    @Test
    fun `valid sunrise and sunset are parsed to non-placeholder strings`() {
        val display = WeatherDisplay.fromForecastJson(forecastJson(), "London", "UK")
        assertNotEquals("--", display.sunrise)
        assertNotEquals("--", display.sunset)
    }

    @Test
    fun `invalid time string falls back to placeholder`() {
        val json = forecastJson(sunrise = "not-a-time", sunset = "")
        val display = WeatherDisplay.fromForecastJson(json, "London", "UK")
        assertEquals("--", display.sunrise)
        assertEquals("--", display.sunset)
    }

    @Test
    fun `empty city arg is preserved`() {
        val display = WeatherDisplay.fromForecastJson(forecastJson(), "", "")
        assertEquals("", display.city)
        assertEquals("", display.country)
    }
}
