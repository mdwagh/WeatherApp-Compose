# Getting Started — WeatherApp (Android)

This is a Kotlin/Jetpack Compose port of the iOS SwiftUI WeatherApp. Same feature, same
free Open-Meteo API, same unit-settings idea — rebuilt with native Android tools only
(no Retrofit, no Glide, no Firebase, no Dagger). This doc maps what you already know from
iOS onto the Android/Kotlin equivalent, then explains how the project is organized.

## 1. Concept map: iOS → Android

| Concept | iOS (Swift/SwiftUI) | Android (Kotlin/Compose) |
|---|---|---|
| App entry point | `@main struct WeatherAppApp: App` | `AndroidManifest.xml` marks an `Activity` with the `LAUNCHER` intent-filter — here, `MainActivity` |
| A screen | `struct ContentView: View { var body: some View }` | `@Composable fun WeatherScreen(...)` — a function that describes UI |
| Reactive state | `@Observable class WeatherViewModel` | Plain class with `var x by mutableStateOf(...)` properties |
| Passing state into a screen | `ContentView(viewModel: viewModel)` | `WeatherScreen(viewModel = viewModel)` — same idea, just a function parameter |
| Async code | `async`/`await`, `Task { }` | `suspend fun`, coroutine `scope.launch { }` |
| Background thread | GCD / `URLSession` does it for you | `withContext(Dispatchers.IO) { }` |
| Networking | `URLSession` | `HttpURLConnection` (built into the JVM) |
| JSON parsing | `Codable` + `JSONDecoder` | `org.json.JSONObject` (built into Android) |
| Key-value persistence | `UserDefaults` | `SharedPreferences` |
| Current location | `CLLocationManager` | `android.location.LocationManager` |
| Coordinates → place name | `CLGeocoder` | `android.location.Geocoder` |
| App permissions declared | `Info.plist` usage-description keys | `AndroidManifest.xml` `<uses-permission>` tags |
| Asking the user for permission | iOS prompts automatically the first time you use the API | You must explicitly call `registerForActivityResult(RequestPermission())` and `.launch(...)` — see `MainActivity.kt` |
| Presenting a secondary screen | `.sheet { SettingsView() }` (modal over the current screen) | A second `Activity` (`SettingsActivity`) started with `startActivity(Intent(...))` — a full screen, not a modal |
| Build/dependency config | Xcode project settings + Swift Package Manager | `build.gradle.kts` (per-module) + `gradle/libs.versions.toml` (a single place listing every dependency's name and version) |

## 2. Project structure

```
app/src/main/java/com/example/weatherapp/
├── MainActivity.kt          Entry point. Hosts WeatherScreen, owns the ViewModel,
│                             requests location permission.
├── SettingsActivity.kt       Second screen for unit settings (started via Intent).
├── ui/
│   ├── WeatherScreen.kt      The main screen's UI (Composable functions).
│   ├── SettingsScreen.kt     The settings screen's UI.
│   └── WeatherViewModel.kt   Screen state + the actions the UI can trigger
│                             (search, use my location, retry).
└── data/
    ├── AppSettings.kt        TemperatureUnit/DistanceUnit enums + SharedPreferences
    │                         read/write.
    ├── WeatherModels.kt      WMO weather-code → text/emoji mapping, and the
    │                         WeatherDisplay model built from the API's JSON.
    ├── WeatherService.kt     Talks to Open-Meteo (geocoding + forecast) over HTTP.
    └── LocationService.kt    One-shot current-location lookup + reverse geocoding.
```

## 3. How data flows for one user action

Example: the user taps the location button.

1. `WeatherScreen` calls the `onRequestLocationWeather` lambda it was given.
2. `MainActivity.requestLocationWeather()` checks the location permission. If missing,
   it launches the system permission dialog; if granted, it calls
   `viewModel.loadWeatherForCurrentLocation()`.
3. `WeatherViewModel` launches a coroutine that calls `LocationService.requestCurrentLocation()`,
   then `LocationService.placeName(...)`, then `WeatherService.fetchWeather(...)`.
4. Each of those `suspend fun`s does its blocking work (GPS wait, network call) on a
   background dispatcher and returns the result.
5. `WeatherViewModel` assigns the result to its `weather` property (`by mutableStateOf`).
6. Because `WeatherScreen` reads `viewModel.weather` while composing, Compose
   automatically re-runs that part of the UI to show the new data. You never manually
   call anything like "refresh the view" — this is the same mental model as SwiftUI
   reacting to `@Observable` changes.

## 4. Deliberate simplifications (and how to grow past them)

- **`WeatherViewModel` is a plain class, not `androidx.lifecycle.ViewModel`.** That means
  its state is lost if the OS recreates the Activity (e.g. on rotation). If you want to
  survive rotation, the next step is adding the `androidx.lifecycle:lifecycle-viewmodel-compose`
  dependency and extending `ViewModel`.
- **No dependency injection.** `MainActivity` constructs `WeatherService`/`LocationService`
  directly. Fine at this size; a larger app would introduce a DI approach once you have
  many screens sharing services.
- **Emoji instead of icon assets** for weather conditions — zero extra files, easy to
  swap for real drawables/vectors later if you want a custom look.
- **Two Activities instead of one Activity + Navigation library.** Simple and explicit
  for a first app. If you add more screens later, look into Jetpack Navigation.

## 5. Building & running

```bash
cd /Users/Manoj/Work/AndroidCodeBase/Android-AI-Projects/WeatherApp

# Build a debug APK
./gradlew assembleDebug

# With a device/emulator connected (check with `adb devices`):
./gradlew installDebug
adb shell am start -n com.example.weatherapp/.MainActivity
```

Or just click **Run ▶** in Android Studio with an emulator selected — that does all of
the above for you.

**Note on the emulator and location:** some AVD images without Google Play don't return
a real address from `Geocoder`. The app already handles this the same way iOS does —
falling back to the label "Current Location" instead of failing.
