package com.example.weatherapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.weatherapp.data.AppSettings
import com.example.weatherapp.data.LocationService
import com.example.weatherapp.ui.WeatherScreen
import com.example.weatherapp.ui.WeatherViewModel
import com.example.weatherapp.ui.theme.WeatherAppTheme

/**
 * App entry point - the Android counterpart to iOS's `WeatherAppApp` + `ContentView` combined.
 * `AndroidManifest.xml` marks this as the LAUNCHER activity, so it's what opens on app start.
 */
class MainActivity : ComponentActivity() {

    private val settings by lazy { AppSettings(this) }

    // viewModels() is the Android equivalent of @StateObject: the ViewModel survives rotation
    // because the framework holds it outside the Activity lifecycle.
    private val viewModel: WeatherViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                WeatherViewModel(locationService = LocationService(applicationContext)) as T
        }
    }

    private val requestLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.loadWeatherForCurrentLocation()
        } else {
            viewModel.reportLocationPermissionDenied()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WeatherAppTheme {
                WeatherScreen(
                    viewModel = viewModel,
                    settings = settings,
                    modifier = Modifier.fillMaxSize(),
                    onSettingsClick = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    },
                    onRequestLocationWeather = { requestLocationWeather() },
                )
            }
        }
    }

    private fun requestLocationWeather() {
        val hasPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.loadWeatherForCurrentLocation()
        } else {
            requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    override fun onResume() {
        super.onResume()
        settings.refresh()
    }
}
