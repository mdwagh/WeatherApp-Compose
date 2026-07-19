package com.example.weatherapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weatherapp.data.AppSettings
import com.example.weatherapp.data.DistanceUnit
import com.example.weatherapp.data.TemperatureUnit
import com.example.weatherapp.data.WeatherDisplay

/**
 * Main weather screen - the Composable counterpart to iOS's `ContentView`.
 * A Composable is just a function that describes UI for a given state; Compose re-runs it
 * whenever state it reads (like `viewModel.weather`) changes, similar to how a SwiftUI
 * `body` re-evaluates.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel,
    settings: AppSettings,
    onSettingsClick: () -> Unit,
    onRequestLocationWeather: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Runs once when this Composable first appears - the Compose equivalent of `.onAppear`.
    LaunchedEffect(Unit) {
        onRequestLocationWeather()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Weather App") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1E88E5).copy(alpha = 0.9f),
                            Color(0xFF00BCD4).copy(alpha = 0.3f),
                        )
                    )
                ),
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                SearchBar(
                    searchText = viewModel.searchText,
                    onSearchTextChange = { viewModel.searchText = it },
                    onSearch = { viewModel.searchWeather(viewModel.searchText) },
                    onUseLocation = onRequestLocationWeather,
                )

                Spacer(modifier = Modifier.height(16.dp))

                when {
                    viewModel.isLoading -> LoadingState()
                    viewModel.errorMessage != null -> ErrorState(
                        message = viewModel.errorMessage!!,
                        onRetry = { viewModel.retry() },
                    )
                    viewModel.weather != null -> WeatherDetails(viewModel.weather!!, settings)
                    else -> EmptyState()
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onSearch: () -> Unit,
    onUseLocation: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            placeholder = { Text("Search city...") },
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
        IconButton(onClick = onSearch) {
            Icon(Icons.Default.Search, contentDescription = "Search")
        }
        IconButton(onClick = onUseLocation) {
            Icon(Icons.Default.LocationOn, contentDescription = "Use current location")
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(12.dp))
        Text("Fetching weather...", color = Color.DarkGray)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Search for a city to see weather", color = Color.DarkGray)
    }
}

@Composable
private fun WeatherDetails(weather: WeatherDisplay, settings: AppSettings) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${weather.city}, ${weather.country}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(weather.condition, color = MaterialTheme.colorScheme.primary)
                Text(weather.description, color = Color.DarkGray)
            }
        }
        item {
            Text(weather.emoji, fontSize = 64.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
        item {
            InfoCard {
                LabelValueRow("Temperature:", weather.temperature(settings.temperatureUnit))
                LabelValueRow("Feels Like:", weather.feelsLike(settings.temperatureUnit))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailCard("Humidity", weather.humidity, Modifier.weight(1f))
                DetailCard("Wind", weather.windSpeed(settings.distanceUnit), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailCard("Pressure", weather.pressure, Modifier.weight(1f))
                DetailCard("Visibility", weather.visibility(settings.distanceUnit), Modifier.weight(1f))
            }
        }
        item {
            InfoCard {
                LabelValueRow("🌅 Sunrise:", weather.sunrise)
                LabelValueRow("🌇 Sunset:", weather.sunset)
            }
        }
    }
}

@Composable
private fun InfoCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}

@Composable
private fun LabelValueRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.DarkGray)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DetailCard(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, color = Color.DarkGray, fontSize = 12.sp)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}
