package com.example.weatherapp

import com.example.weatherapp.data.WeatherCode
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherCodeTest {

    @Test
    fun `code 0 clear sky`() {
        val code = WeatherCode(0)
        assertEquals("Clear", code.condition)
        assertEquals("Clear sky", code.description)
        assertEquals("☀️", code.emoji)
    }

    @Test
    fun `code 2 partly cloudy`() {
        val code = WeatherCode(2)
        assertEquals("Partly Cloudy", code.condition)
        assertEquals("⛅", code.emoji)
    }

    @Test
    fun `code 3 overcast`() {
        val code = WeatherCode(3)
        assertEquals("Overcast", code.condition)
        assertEquals("Overcast clouds", code.description)
        assertEquals("☁️", code.emoji)
    }

    @Test
    fun `fog codes 45 and 48 map to fog`() {
        assertEquals("Fog", WeatherCode(45).condition)
        assertEquals("Fog", WeatherCode(48).condition)
    }

    @Test
    fun `rain codes`() {
        assertEquals("Rain", WeatherCode(61).condition)
        assertEquals("Rain", WeatherCode(63).condition)
        assertEquals("Rain", WeatherCode(65).condition)
    }

    @Test
    fun `snow codes`() {
        assertEquals("Snow", WeatherCode(71).condition)
        assertEquals("Snow", WeatherCode(73).condition)
        assertEquals("Snow", WeatherCode(75).condition)
        assertEquals("Snow", WeatherCode(77).condition)
    }

    @Test
    fun `code 95 thunderstorm`() {
        val code = WeatherCode(95)
        assertEquals("Thunderstorm", code.condition)
        assertEquals("Thunderstorm", code.description)
        assertEquals("⛈️", code.emoji)
    }

    @Test
    fun `thunderstorm with hail`() {
        assertEquals("Thunderstorm with Hail", WeatherCode(96).condition)
        assertEquals("Thunderstorm with Hail", WeatherCode(99).condition)
    }

    @Test
    fun `unknown code returns fallback`() {
        val code = WeatherCode(999)
        assertEquals("Unknown", code.condition)
        assertEquals("", code.description)
        assertEquals("☁️", code.emoji)
    }
}
