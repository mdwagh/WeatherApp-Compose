package com.example.weatherapp.data

interface WeatherServiceInterface {
    suspend fun fetchWeather(city: String): WeatherDisplay

    suspend fun fetchWeather(
        latitude: Double,
        longitude: Double,
        city: String = "Current Location",
        country: String = "",
    ): WeatherDisplay
}
