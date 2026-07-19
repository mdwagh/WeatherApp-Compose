package com.example.weatherapp.data

import android.location.Location

interface LocationServiceInterface {
    suspend fun requestCurrentLocation(): Location

    suspend fun placeName(latitude: Double, longitude: Double): PlaceName
}
