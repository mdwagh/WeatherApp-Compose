# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Architecture

Single-module Android app (`com.example.weatherapp`) using Jetpack Compose + MVVM.

**ViewModel:** `WeatherViewModel` extends `androidx.lifecycle.ViewModel` and uses `viewModelScope` for coroutines. State survives screen rotation — the framework reuses the same ViewModel instance across Activity recreations (the Android equivalent of a SwiftUI `@StateObject`). `MainActivity` creates it via `by viewModels { ... }` with a factory that passes `LocationService`.

**Two Activities** (no Jetpack Navigation):
- `MainActivity` — main weather display
- `SettingsActivity` — unit preferences (temperature, wind, precipitation)

**Settings sync:** `SettingsActivity` writes to `SharedPreferences`; `MainActivity` must call `settings.refresh()` in `onResume()` to pick up changes. The two Activities have separate `AppSettings` instances backed by the same SharedPreferences file.

**No DI framework** — services are instantiated directly. No Retrofit; networking uses `HttpURLConnection` + `org.json.JSONObject`.

**Testability:** `WeatherService` and `LocationService` implement `WeatherServiceInterface` / `LocationServiceInterface`. `WeatherViewModel` takes these interfaces so unit tests can inject fakes without any mocking framework.

**Weather API:** [Open-Meteo](https://open-meteo.com/) — free, no API key required.

## Build, Test & Lint Commands

```bash
./gradlew assembleDebug           # Build debug APK
./gradlew installDebug            # Build + install on connected device/emulator
./gradlew testDebug               # Unit tests (pure JVM, no device needed)
./gradlew connectedAndroidTest    # Instrumented tests (requires device/emulator)
./gradlew ktlintCheck             # Check formatting
./gradlew ktlintFormat            # Auto-fix formatting
```

## Gotchas

- **Geocoder** is unavailable on some emulator images (non-Google Play builds). Code falls back to "Current Location" — this is expected, not a bug.
- **Location API** uses `LocationManager.requestSingleUpdate()` (deprecated, suppressed via `@Suppress("DEPRECATION")`) — intentional; don't replace with FusedLocationProvider without discussion.
- **Compose BOM 2026.02.01** is a pre-release version. Watch for API changes in Compose updates.
- **Version catalog** (`gradle/libs.versions.toml`) is the single source of truth for dependency versions — don't add version strings directly in `build.gradle.kts`.

## Code Style

- Kotlin only (no Java).
- ktlint enforces formatting (4-space indent, 120-char line limit). Run `./gradlew ktlintFormat` before committing.
