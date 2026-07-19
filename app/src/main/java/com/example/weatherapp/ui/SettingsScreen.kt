package com.example.weatherapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.weatherapp.data.AppSettings
import com.example.weatherapp.data.DistanceUnit
import com.example.weatherapp.data.TemperatureUnit

/**
 * Settings screen - the Composable counterpart to iOS's `SettingsView`, which was presented
 * as a `.sheet`. On Android we present it as a separate screen/Activity instead (see
 * `SettingsActivity`), which is the more common pattern here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settings: AppSettings, onDone: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Done")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            SectionTitle("Temperature")
            TemperatureUnit.entries.forEach { unit ->
                UnitRow(
                    label = unit.label,
                    selected = settings.temperatureUnit == unit,
                    onSelect = { settings.updateTemperatureUnit(unit) },
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            SectionTitle("Distance & Wind Speed")
            DistanceUnit.entries.forEach { unit ->
                UnitRow(
                    label = unit.label,
                    selected = settings.distanceUnit == unit,
                    onSelect = { settings.updateDistanceUnit(unit) },
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun UnitRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}
