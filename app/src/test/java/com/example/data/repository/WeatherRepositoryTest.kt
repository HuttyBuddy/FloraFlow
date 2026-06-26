package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class WeatherRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: WeatherRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = WeatherRepository(context)
    }

    @Test
    fun testDefaultWeatherCachedValues() {
        val weather = repository.currentWeather.value
        assertEquals("Home Haven", weather.cityName)
        assertEquals(71.6, weather.temperatureFahrenheit, 0.1)
        assertEquals(55.0, weather.humidity, 0.1)
        assertEquals("Clear", weather.condition)
    }

    @Test
    fun testMockWeatherFallbackForNonDigits() = runBlocking {
        // Since we are running in a unit test environment, fetchWeather will skip live network calls
        // and fall back to the mock weather generator.
        val rainWeather = repository.fetchWeather("rain")
        assertEquals("Rain", rainWeather.condition)
        assertEquals(16.0 * 9.0 / 5.0 + 32.0, rainWeather.temperatureFahrenheit, 0.1)
        assertEquals(85.0, rainWeather.humidity, 0.1)

        val clearWeather = repository.fetchWeather("90210") // 90210 is a digit ZIP, but in test it falls back to mock Clear
        assertEquals("Clear", clearWeather.condition)
        assertEquals(23.0 * 9.0 / 5.0 + 32.0, clearWeather.temperatureFahrenheit, 0.1)
        assertEquals(50.0, clearWeather.humidity, 0.1)
    }

    @Test
    fun testSimulateWeatherOverriding() {
        repository.simulateWeather("Snow", -5.0, 45.0)
        val weather = repository.currentWeather.value
        assertEquals("Simulated Eco-Zone", weather.cityName)
        assertEquals("Snow", weather.condition)
        assertEquals(-5.0 * 9.0 / 5.0 + 32.0, weather.temperatureFahrenheit, 0.1)
        assertEquals(45.0, weather.humidity, 0.1)
    }
}
