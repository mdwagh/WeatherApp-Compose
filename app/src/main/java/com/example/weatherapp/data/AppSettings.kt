package com.example.weatherapp.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// ---- Temperature unit ----
// Kotlin enums can hold data/behavior per case, same idea as Swift's `enum ...: String`.
enum class TemperatureUnit {
    CELSIUS,
    FAHRENHEIT;

    val label: String
        get() = when (this) {
            CELSIUS -> "Celsius (°C)"
            FAHRENHEIT -> "Fahrenheit (°F)"
        }

    /** Formats a raw Celsius value into this unit's display string. */
    fun format(celsius: Double): String = when (this) {
        CELSIUS -> String.format("%.1f°C", celsius)
        FAHRENHEIT -> String.format("%.1f°F", celsius * 9 / 5 + 32)
    }
}

// ---- Distance / speed unit ----
enum class DistanceUnit {
    METRIC,   // kilometers + meters/second
    IMPERIAL; // miles + miles/hour

    val label: String
        get() = when (this) {
            METRIC -> "Kilometers"
            IMPERIAL -> "Miles"
        }

    /** Formats a raw distance in meters into this unit's display string. */
    fun formatDistance(meters: Double): String = when (this) {
        METRIC -> String.format("%.1f km", meters / 1000)
        IMPERIAL -> String.format("%.1f mi", meters / 1609.344)
    }

    /** Formats a raw wind speed in meters/second into this unit's display string. */
    fun formatSpeed(metersPerSecond: Double): String = when (this) {
        METRIC -> String.format("%.1f m/s", metersPerSecond)
        IMPERIAL -> String.format("%.1f mph", metersPerSecond * 2.236936)
    }
}

/**
 * User unit preferences, persisted in SharedPreferences (Android's equivalent of UserDefaults).
 *
 * `by mutableStateOf(...)` is what makes Compose screens automatically redraw when these
 * properties change - the Compose equivalent of iOS's `@Observable`. We keep the setter
 * private and expose `set...()` functions instead, so every change is saved to disk too.
 */
class AppSettings(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var temperatureUnit by mutableStateOf(readTemperatureUnit())
        private set

    var distanceUnit by mutableStateOf(readDistanceUnit())
        private set

    fun updateTemperatureUnit(unit: TemperatureUnit) {
        temperatureUnit = unit
        prefs.edit().putString(KEY_TEMPERATURE, unit.name).apply()
    }

    fun updateDistanceUnit(unit: DistanceUnit) {
        distanceUnit = unit
        prefs.edit().putString(KEY_DISTANCE, unit.name).apply()
    }

    /**
     * Re-reads both units from SharedPreferences.
     *
     * MainActivity and SettingsActivity each construct their own `AppSettings`, so they don't
     * share the same in-memory State object - only the SharedPreferences file underneath them
     * is shared. Call this from `onResume()` after returning from SettingsActivity, so
     * MainActivity's copy picks up whatever the user just changed there.
     */
    fun refresh() {
        temperatureUnit = readTemperatureUnit()
        distanceUnit = readDistanceUnit()
    }

    private fun readTemperatureUnit(): TemperatureUnit =
        TemperatureUnit.entries.find { it.name == prefs.getString(KEY_TEMPERATURE, null) }
            ?: TemperatureUnit.CELSIUS

    private fun readDistanceUnit(): DistanceUnit =
        DistanceUnit.entries.find { it.name == prefs.getString(KEY_DISTANCE, null) }
            ?: DistanceUnit.METRIC

    private companion object {
        const val PREFS_NAME = "weather_app_settings"
        const val KEY_TEMPERATURE = "temperatureUnit"
        const val KEY_DISTANCE = "distanceUnit"
    }
}
