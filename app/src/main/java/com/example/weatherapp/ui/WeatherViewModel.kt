package com.example.weatherapp.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.data.LocationError
import com.example.weatherapp.data.LocationServiceInterface
import com.example.weatherapp.data.WeatherDisplay
import com.example.weatherapp.data.WeatherError
import com.example.weatherapp.data.WeatherService
import com.example.weatherapp.data.WeatherServiceInterface
import kotlinx.coroutines.launch

/**
 * Holds the weather screen's state and business logic.
 *
 * Extends [ViewModel] so state survives screen rotation — the framework recreates the Activity
 * but reuses the same ViewModel instance. This is the Android equivalent of a SwiftUI
 * `@StateObject`: the object outlives individual view lifecycles.
 *
 * Every mutable property uses `by mutableStateOf(...)` to make Compose recompose automatically
 * when it changes — same role `@Observable` plays in SwiftUI.
 */
class WeatherViewModel(
    private val weatherService: WeatherServiceInterface = WeatherService(),
    private val locationService: LocationServiceInterface,
) : ViewModel() {
    var weather by mutableStateOf<WeatherDisplay?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var searchText by mutableStateOf("")

    private var lastLoadedFromLocation = false

    fun searchWeather(city: String) {
        viewModelScope.launch { search(city) }
    }

    private suspend fun search(city: String) {
        if (city.isBlank()) {
            errorMessage = "Please enter a city name"
            return
        }

        isLoading = true
        errorMessage = null

        try {
            weather = weatherService.fetchWeather(city)
            errorMessage = null
            searchText = ""
            lastLoadedFromLocation = false
        } catch (e: WeatherError) {
            errorMessage = e.message
            weather = null
        } catch (e: Exception) {
            errorMessage = "Unexpected error occurred"
            weather = null
        }

        isLoading = false
    }

    fun loadWeatherForCurrentLocation() {
        viewModelScope.launch { loadCurrentLocation() }
    }

    private suspend fun loadCurrentLocation() {
        isLoading = true
        errorMessage = null

        try {
            val location = locationService.requestCurrentLocation()
            val place = locationService.placeName(location.latitude, location.longitude)
            weather = weatherService.fetchWeather(
                latitude = location.latitude,
                longitude = location.longitude,
                city = place.city,
                country = place.country,
            )
            lastLoadedFromLocation = true
        } catch (e: LocationError) {
            errorMessage = e.message
        } catch (e: WeatherError) {
            errorMessage = e.message
        } catch (e: Exception) {
            errorMessage = "Failed to load weather for your location"
        }

        isLoading = false
    }

    fun retry() {
        viewModelScope.launch {
            if (lastLoadedFromLocation) {
                loadCurrentLocation()
            } else {
                val lastCity = weather?.city
                if (lastCity != null) search(lastCity) else loadCurrentLocation()
            }
        }
    }

    fun reportLocationPermissionDenied() {
        errorMessage = "Location access denied. Enable it in Settings to use your location."
        isLoading = false
    }
}
