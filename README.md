# WeatherApp-Compose — Android Weather App

A clean, production-ready native Android weather app built with **Kotlin + Jetpack Compose** that shows current conditions for any city or the device's current location, with full unit switching between metric and imperial.

---

## Features

| Feature | Details |
|---|---|
| **City Search** | Geocodes any city name via Open-Meteo's geocoding API, resolves to coordinates, then fetches weather |
| **Current Location** | One-shot GPS fix via `LocationManager`, reverse-geocoded to a display name with `Geocoder` |
| **Weather Display** | Temperature, feels-like, humidity, wind speed, visibility, pressure, sunrise/sunset |
| **Weather Conditions** | WMO weather code → human-readable condition + emoji (e.g. "Partly Cloudy ⛅") |
| **Unit Switching** | Temperature: °C / °F — Distance/Speed: Metric (km, m/s) / Imperial (mi, mph) |
| **Persistent Settings** | Unit preferences saved in `SharedPreferences`, survive app restart |
| **Rotation Survival** | `WeatherViewModel` extends `androidx.lifecycle.ViewModel` — state survives screen rotation |
| **Retry** | Retries the last successful action (city search or location lookup) |
| **No API Key** | Uses [Open-Meteo](https://open-meteo.com/) — a free, public API with no registration |
| **Zero Extra Libraries** | Networking via `HttpURLConnection`, JSON via `org.json.JSONObject` — both ship with Android |
| **Privacy** | No analytics, no crash reporting, no server-side state — all data stays on device |

---

## Project Structure

```
WeatherApp/
├── app/
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml                  # Permissions + Activity declarations
│       │   └── java/com/example/weatherapp/
│       │       ├── MainActivity.kt                  # Entry point — hosts WeatherScreen, owns ViewModel,
│       │       │                                    #   handles location permission flow
│       │       ├── SettingsActivity.kt              # Full-screen settings (started via Intent)
│       │       ├── data/
│       │       │   ├── AppSettings.kt               # TemperatureUnit / DistanceUnit enums + SharedPreferences
│       │       │   ├── LocationService.kt           # One-shot GPS fix + reverse geocoding
│       │       │   ├── LocationServiceInterface.kt  # Interface for testability
│       │       │   ├── WeatherModels.kt             # WeatherCode (WMO → text/emoji) + WeatherDisplay model
│       │       │   ├── WeatherService.kt            # Open-Meteo geocoding + forecast API client
│       │       │   └── WeatherServiceInterface.kt   # Interface for testability
│       │       └── ui/
│       │           ├── WeatherScreen.kt             # Main screen Composable functions
│       │           ├── WeatherViewModel.kt          # Screen state + actions (search, location, retry)
│       │           ├── SettingsScreen.kt            # Settings screen Composable functions
│       │           └── theme/
│       │               ├── Color.kt                 # App color palette
│       │               ├── Theme.kt                 # Material3 theme wiring
│       │               └── Type.kt                  # Typography scale
│       ├── test/                                    # JVM unit tests (no device needed)
│       │   └── java/com/example/weatherapp/
│       │       ├── WeatherCodeTest.kt               # WMO code → condition/description/emoji
│       │       ├── FormatterTest.kt                 # °C/°F conversions, km/mi, m/s/mph
│       │       ├── WeatherDisplayParsingTest.kt     # JSON parsing + time formatting
│       │       └── WeatherViewModelTest.kt          # ViewModel state transitions with fake services
│       └── androidTest/
│           └── ExampleInstrumentedTest.kt           # Device/emulator smoke test
├── gradle/
│   ├── libs.versions.toml                          # Single source of truth for all dependency versions
│   └── wrapper/
├── .claude/
│   └── skills/
│       ├── run/SKILL.md                            # /run skill: build + install to device
│       └── verify/SKILL.md                         # /verify skill: test + manual checklist
├── .editorconfig                                   # ktlint rules (4-space indent, 120-char limit)
├── CLAUDE.md                                       # Claude Code project instructions
├── GETTING_STARTED.md                              # iOS → Android concept mapping guide
├── build.gradle.kts                                # Root build config
├── settings.gradle.kts                             # Module and repo config
└── app/build.gradle.kts                            # App module dependencies + plugins
```

---

## Setup Instructions

### Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or later
- **JDK 11+** (bundled with Android Studio)
- **Android SDK** with API 26+ (app `minSdk = 26`, Android 8.0 Oreo)
- A physical device or emulator for location features

### Steps

1. **Clone the repository:**
   ```bash
   git clone https://github.com/mwapptest/WeatherApp-Compose.git
   cd WeatherApp-Compose
   ```

2. **Open in Android Studio:**
   - File → Open → select the `WeatherApp-Compose` folder
   - Android Studio will sync Gradle automatically on first open

3. **No API key needed** — the app uses [Open-Meteo](https://open-meteo.com/), which is free and requires no registration.

4. **Build & Run:**
   - Select a device or emulator from the toolbar
   - Click **Run ▶** (or `Shift+F10`)

   Alternatively, from the terminal:
   ```bash
   ./gradlew installDebug    # Build and install debug APK
   adb devices               # Confirm device is connected
   ```

### Location on Emulators

Some AVD images without Google Play don't return a real address from `Geocoder`. The app handles this gracefully — falling back to the label "Current Location" instead of an error.

---

## Architecture

### Pattern: MVVM

```
MainActivity / SettingsActivity  (View hosts)
        │
        ▼
WeatherScreen / SettingsScreen   (Composable — UI only, no logic)
        │  observes mutableStateOf properties
        ▼
WeatherViewModel                 (androidx.lifecycle.ViewModel)
        │  calls suspend functions
        ├──▶ WeatherService      (HTTP geocoding + forecast)
        └──▶ LocationService     (GPS + reverse geocoding)

AppSettings                      (SharedPreferences — read by both Activities)
```

### Key Design Decisions

| Decision | Reason |
|---|---|
| `androidx.lifecycle.ViewModel` | State survives screen rotation; `viewModelScope` auto-cancels coroutines on `onCleared()` |
| `mutableStateOf()` for all state | Compose reads these properties and recomposes automatically when they change — no `StateFlow.collect`, no `LiveData.observe` |
| Two Activities, no Navigation library | Simple and explicit for a two-screen app; avoids Navigation dependency overhead |
| `HttpURLConnection` instead of Retrofit | Zero extra dependencies; both the geocoding and forecast calls are straightforward GETs |
| `org.json.JSONObject` instead of Gson/Moshi | Ships with Android, no codegen, sufficient for a fixed API response structure |
| `LocationManager` instead of FusedLocationProvider | No Google Play Services dependency; works on AOSP devices |
| Interfaces for services (`WeatherServiceInterface`, `LocationServiceInterface`) | Allows fake implementations in unit tests without a mocking framework |
| Emoji for weather icons | Zero asset files; trivially swappable for real drawables later |

### State & Data Flow

Example — user taps the location button:

1. `WeatherScreen` calls the `onRequestLocationWeather` lambda it received as a parameter.
2. `MainActivity.requestLocationWeather()` checks `ACCESS_FINE_LOCATION` permission. If missing, it launches the system permission dialog via `registerForActivityResult(RequestPermission())`.
3. On grant, it calls `viewModel.loadWeatherForCurrentLocation()`.
4. `WeatherViewModel` launches a coroutine on `viewModelScope`:
   - `LocationService.requestCurrentLocation()` — suspends until a GPS fix arrives on `Dispatchers.Main`, using `suspendCancellableCoroutine` to wrap the callback-based `LocationManager.requestSingleUpdate()`.
   - `LocationService.placeName(lat, lon)` — reverse-geocodes on `Dispatchers.IO`.
   - `WeatherService.fetchWeather(lat, lon, city, country)` — HTTP GET on `Dispatchers.IO`.
5. Results are assigned to `var weather by mutableStateOf(...)` on the ViewModel.
6. Compose re-runs any Composable that read `viewModel.weather`, updating the UI automatically.

### Settings Sync

`MainActivity` and `SettingsActivity` each hold their own `AppSettings` instance, backed by the same `SharedPreferences` file. `MainActivity.onResume()` calls `settings.refresh()` after returning from `SettingsActivity` to pick up any changes.

---

## Frameworks & APIs

| Framework / API | Usage |
|---|---|
| **Jetpack Compose** | All UI (declarative, replaces XML layouts) |
| **Material3** | Design system — colors, typography, components |
| **androidx.lifecycle.ViewModel** | Rotation-safe state holder |
| **Kotlin Coroutines** | Async — `viewModelScope`, `suspend fun`, `Dispatchers.IO` |
| **HttpURLConnection** (JDK) | HTTP GET requests to Open-Meteo |
| **org.json.JSONObject** (Android) | JSON response parsing |
| **android.location.LocationManager** | One-shot GPS / network location fix |
| **android.location.Geocoder** | Reverse geocoding (coordinates → city name) |
| **SharedPreferences** | Persistent unit preferences |
| **Open-Meteo API** | Free weather + geocoding API, no key required |
| **ktlint** (via `org.jlleitschuh.gradle.ktlint`) | Kotlin formatting enforcement |

---

## Testing

### Overview

Unit tests run on the JVM — no device or emulator needed. `WeatherService` and `LocationService` are hidden behind interfaces so fakes can be injected without a mocking framework.

```bash
./gradlew testDebug              # Run all unit tests
./gradlew connectedAndroidTest   # Instrumented tests (requires device/emulator)
```

### Test Files

| File | What it covers |
|---|---|
| `WeatherCodeTest` | WMO code → condition, description, emoji for all major codes; unknown code fallback |
| `FormatterTest` | `TemperatureUnit` (°C/°F conversions including freezing, boiling, body temp); `DistanceUnit` (km/mi, m/s/mph) |
| `WeatherDisplayParsingTest` | `WeatherDisplay.fromForecastJson()` — field mapping, weather code propagation, time parsing, invalid time fallback to `"--"` |
| `WeatherViewModelTest` | State transitions: success clears error + search text; blank input sets error; `WeatherError.CityNotFound` sets message; network errors set message; `reportLocationPermissionDenied` sets error |

### Fake Service Pattern

Tests use handwritten fakes rather than a mocking library:

```kotlin
class FakeWeatherService(
    private val response: WeatherDisplay = fakeDisplay(),
    private val error: Exception? = null,
) : WeatherServiceInterface {
    override suspend fun fetchWeather(city: String): WeatherDisplay {
        error?.let { throw it }
        return response
    }
    // ...
}
```

ViewModel tests replace `Dispatchers.Main` with `UnconfinedTestDispatcher` so coroutines run eagerly without `advanceUntilIdle()`:

```kotlin
@Before fun setup() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
@After fun tearDown() { Dispatchers.resetMain() }
```

### What Isn't Unit-Tested

- **`WeatherService`** — uses `android.net.Uri` (Android stub) and makes real HTTP calls. Covered at integration level by running the app.
- **`LocationService`** — wraps hardware APIs (`LocationManager`, `Geocoder`). Requires a device; the `requestCurrentLocation()` path is not exercised in unit tests.
- **Composable UI** — no Compose UI tests are set up yet; UI correctness is verified by running on a device.

---

## API Reference — Open-Meteo

All network calls go to [open-meteo.com](https://open-meteo.com/). No API key is required.

| Endpoint | Used for |
|---|---|
| `https://geocoding-api.open-meteo.com/v1/search?name=<city>&count=1` | Resolve city name → latitude, longitude, country |
| `https://api.open-meteo.com/v1/forecast?latitude=<lat>&longitude=<lon>&current=...&daily=sunrise,sunset&timezone=auto&wind_speed_unit=ms` | Fetch current conditions + today's sunrise/sunset |

**Current weather fields requested:** `temperature_2m`, `relative_humidity_2m`, `apparent_temperature`, `weather_code`, `wind_speed_10m`, `surface_pressure`, `visibility`

**WMO Weather Code mapping** (a sample):

| Code | Condition | Emoji |
|---|---|---|
| 0 | Clear | ☀️ |
| 1 | Mainly Clear | ☀️ |
| 2 | Partly Cloudy | ⛅ |
| 3 | Overcast | ☁️ |
| 45, 48 | Fog | 🌫️ |
| 51–55 | Drizzle | 🌦️ |
| 61–65 | Rain | 🌧️ |
| 71–77 | Snow | 🌨️ |
| 80–82 | Rain Showers | 🌧️ |
| 95 | Thunderstorm | ⛈️ |
| 96, 99 | Thunderstorm with Hail | ⛈️ |

---

## iOS → Android Quick Reference

This project was ported from an iOS SwiftUI app. Key concept mapping:

| Concept | iOS (Swift/SwiftUI) | Android (Kotlin/Compose) |
|---|---|---|
| App entry point | `@main struct WeatherAppApp: App` | `AndroidManifest.xml` LAUNCHER `Activity` |
| A screen | `struct ContentView: View` | `@Composable fun WeatherScreen(...)` |
| Reactive state | `@Observable class ViewModel` | `var x by mutableStateOf(...)` |
| State survives recreation | `@StateObject` | `by viewModels { ... }` with `androidx.lifecycle.ViewModel` |
| Async | `async`/`await`, `Task {}` | `suspend fun`, `viewModelScope.launch {}` |
| Background thread | `URLSession` / GCD | `withContext(Dispatchers.IO) {}` |
| Networking | `URLSession` | `HttpURLConnection` |
| JSON parsing | `Codable` + `JSONDecoder` | `org.json.JSONObject` |
| Key-value persistence | `UserDefaults` | `SharedPreferences` |
| Current location | `CLLocationManager` | `android.location.LocationManager` |
| Reverse geocoding | `CLGeocoder` | `android.location.Geocoder` |
| Secondary screen | `.sheet { SettingsView() }` | Second `Activity` via `startActivity(Intent(...))` |
| Runtime permissions | Auto-prompted on first use | Explicit `registerForActivityResult(RequestPermission())` |
| Build config | Xcode + Swift Package Manager | `build.gradle.kts` + `gradle/libs.versions.toml` |

---

## License

This project is provided for personal and educational use.
