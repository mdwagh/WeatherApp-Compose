package com.example.weatherapp

import com.example.weatherapp.data.DistanceUnit
import com.example.weatherapp.data.TemperatureUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatterTest {

    // ---- TemperatureUnit ----

    @Test
    fun `celsius formats with one decimal and unit symbol`() {
        assertEquals("20.0°C", TemperatureUnit.CELSIUS.format(20.0))
        assertEquals("-10.0°C", TemperatureUnit.CELSIUS.format(-10.0))
        assertEquals("0.0°C", TemperatureUnit.CELSIUS.format(0.0))
    }

    @Test
    fun `fahrenheit freezing point converts from 0 celsius`() {
        assertEquals("32.0°F", TemperatureUnit.FAHRENHEIT.format(0.0))
    }

    @Test
    fun `fahrenheit boiling point converts from 100 celsius`() {
        assertEquals("212.0°F", TemperatureUnit.FAHRENHEIT.format(100.0))
    }

    @Test
    fun `fahrenheit body temperature converts correctly`() {
        // 37°C = 98.6°F
        assertEquals("98.6°F", TemperatureUnit.FAHRENHEIT.format(37.0))
    }

    // ---- DistanceUnit ----

    @Test
    fun `metric distance formats meters to km`() {
        assertEquals("1.0 km", DistanceUnit.METRIC.formatDistance(1000.0))
        assertEquals("10.0 km", DistanceUnit.METRIC.formatDistance(10_000.0))
        assertEquals("0.5 km", DistanceUnit.METRIC.formatDistance(500.0))
    }

    @Test
    fun `imperial distance formats meters to miles`() {
        val expected = String.format("%.1f mi", 1609.344 / 1609.344)
        assertEquals(expected, DistanceUnit.IMPERIAL.formatDistance(1609.344))
    }

    @Test
    fun `metric speed formats meters per second`() {
        assertEquals("5.0 m/s", DistanceUnit.METRIC.formatSpeed(5.0))
        assertEquals("0.0 m/s", DistanceUnit.METRIC.formatSpeed(0.0))
    }

    @Test
    fun `imperial speed formats meters per second to mph`() {
        // 1 m/s = 2.236936 mph
        val expected = String.format("%.1f mph", 2.236936)
        assertEquals(expected, DistanceUnit.IMPERIAL.formatSpeed(1.0))
    }
}
