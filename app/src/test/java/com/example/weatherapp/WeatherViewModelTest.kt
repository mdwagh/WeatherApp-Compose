package com.example.weatherapp

import com.example.weatherapp.data.LocationServiceInterface
import com.example.weatherapp.data.PlaceName
import com.example.weatherapp.data.WeatherCode
import com.example.weatherapp.data.WeatherDisplay
import com.example.weatherapp.data.WeatherError
import com.example.weatherapp.data.WeatherServiceInterface
import com.example.weatherapp.ui.WeatherViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ---- Fakes ----

    private class FakeWeatherService(
        private val response: WeatherDisplay = fakeDisplay(),
        private val error: Exception? = null,
    ) : WeatherServiceInterface {
        override suspend fun fetchWeather(city: String): WeatherDisplay {
            error?.let { throw it }
            return response
        }

        override suspend fun fetchWeather(
            latitude: Double,
            longitude: Double,
            city: String,
            country: String,
        ): WeatherDisplay {
            error?.let { throw it }
            return response
        }
    }

    private class FakeLocationService : LocationServiceInterface {
        override suspend fun requestCurrentLocation(): android.location.Location =
            throw NotImplementedError("Use instrumented tests for location")

        override suspend fun placeName(latitude: Double, longitude: Double): PlaceName =
            PlaceName("TestCity", "TestCountry")
    }

    // ---- Tests: searchWeather ----

    @Test
    fun `searchWeather with valid city sets weather and clears error`() = runTest {
        val vm = WeatherViewModel(FakeWeatherService(), FakeLocationService())
        vm.searchWeather("London")
        assertNotNull(vm.weather)
        assertNull(vm.errorMessage)
        assertFalse(vm.isLoading)
    }

    @Test
    fun `searchWeather clears search text on success`() = runTest {
        val vm = WeatherViewModel(FakeWeatherService(), FakeLocationService())
        vm.searchText = "London"
        vm.searchWeather("London")
        assertEquals("", vm.searchText)
    }

    @Test
    fun `searchWeather with blank input sets error without fetching`() = runTest {
        val vm = WeatherViewModel(FakeWeatherService(), FakeLocationService())
        vm.searchWeather("   ")
        assertNotNull(vm.errorMessage)
        assertNull(vm.weather)
    }

    @Test
    fun `searchWeather on WeatherError sets error message and clears weather`() = runTest {
        val vm = WeatherViewModel(
            FakeWeatherService(error = WeatherError.CityNotFound),
            FakeLocationService(),
        )
        vm.searchWeather("Fakecity")
        assertEquals("City not found", vm.errorMessage)
        assertNull(vm.weather)
        assertFalse(vm.isLoading)
    }

    @Test
    fun `searchWeather on network error sets error message`() = runTest {
        val vm = WeatherViewModel(
            FakeWeatherService(error = WeatherError.Network(Exception("timeout"))),
            FakeLocationService(),
        )
        vm.searchWeather("London")
        assertNotNull(vm.errorMessage)
        assertNull(vm.weather)
    }

    @Test
    fun `searchWeather on unexpected exception sets generic error`() = runTest {
        val vm = WeatherViewModel(
            FakeWeatherService(error = RuntimeException("boom")),
            FakeLocationService(),
        )
        vm.searchWeather("London")
        assertEquals("Unexpected error occurred", vm.errorMessage)
    }

    // ---- Tests: reportLocationPermissionDenied ----

    @Test
    fun `reportLocationPermissionDenied sets error and clears loading`() {
        val vm = WeatherViewModel(FakeWeatherService(), FakeLocationService())
        vm.reportLocationPermissionDenied()
        assertNotNull(vm.errorMessage)
        assertFalse(vm.isLoading)
    }
}

private fun fakeDisplay() = WeatherDisplay(
    city = "London",
    country = "UK",
    temperatureC = 20.0,
    feelsLikeC = 18.0,
    humidityPercent = 65,
    windSpeedMS = 5.2,
    visibilityM = 10_000.0,
    pressureHPa = 1013.0,
    weatherCode = WeatherCode(0),
    sunrise = "7:30 AM",
    sunset = "5:45 PM",
)
