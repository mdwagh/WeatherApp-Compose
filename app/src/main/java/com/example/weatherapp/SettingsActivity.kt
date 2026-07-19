package com.example.weatherapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.weatherapp.data.AppSettings
import com.example.weatherapp.ui.SettingsScreen
import com.example.weatherapp.ui.theme.WeatherAppTheme

/**
 * A second, separate Activity - the Android counterpart to iOS presenting `SettingsView` as a
 * `.sheet`. Instead of a sheet, Android convention is to push a whole new screen; MainActivity
 * starts this with `startActivity(Intent(...))`, and the back arrow here just finishes it,
 * returning to MainActivity underneath.
 */
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settings = AppSettings(this)

        setContent {
            WeatherAppTheme {
                SettingsScreen(settings = settings, onDone = { finish() })
            }
        }
    }
}
