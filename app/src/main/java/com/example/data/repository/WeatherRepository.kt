package com.example.data.repository

import android.content.Context
import androidx.core.content.edit
import com.example.data.api.WeatherDescription
import com.example.data.api.WeatherMain
import com.example.data.api.WeatherResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WeatherInfo(
    val temperatureFahrenheit: Double,
    val humidity: Double,
    val condition: String, // "Rain", "Clear", "Clouds", "Snow", "Heatwave", "Frost"
    val cityName: String
)

class WeatherRepository(private val context: Context) {
    private val sharedPrefs = context.getSharedPreferences("floraflow_weather_prefs", Context.MODE_PRIVATE)

    private val _currentWeather = MutableStateFlow(
        WeatherInfo(
            temperatureFahrenheit = sharedPrefs.getFloat("cached_temp", 71.6f).toDouble(),
            humidity = sharedPrefs.getFloat("cached_humidity", 55.0f).toDouble(),
            condition = sharedPrefs.getString("cached_condition", "Clear") ?: "Clear",
            cityName = sharedPrefs.getString("cached_city", "Home Haven") ?: "Home Haven"
        )
    )
    val currentWeather: StateFlow<WeatherInfo> = _currentWeather.asStateFlow()

    fun getUserLocationZip(): String {
        return sharedPrefs.getString("user_zip", "90210") ?: "90210"
    }

    fun saveUserLocationZip(zip: String) {
        sharedPrefs.edit { putString("user_zip", zip) }
    }

    // Fetches the weather. If network fails or for simulation/testing, falls back to a clean mock provider
    suspend fun fetchWeather(location: String = getUserLocationZip()): WeatherInfo {
        // Simulation mode / mock weather engine for premium biophilic adjustments
        val condition = when {
            location.contains("rain", ignoreCase = true) || location == "98101" -> "Rain"
            location.contains("snow", ignoreCase = true) || location == "59715" -> "Snow"
            location.contains("hot", ignoreCase = true) || location == "85001" -> "Heatwave"
            location.contains("cold", ignoreCase = true) || location == "04401" -> "Frost"
            else -> "Clear"
        }

        val tempCelsius = when (condition) {
            "Rain" -> 16.0
            "Snow" -> -2.0
            "Heatwave" -> 38.0
            "Frost" -> 1.0
            else -> 23.0
        }
        val tempFahrenheit = tempCelsius * 9.0 / 5.0 + 32.0

        val humidity = when (condition) {
            "Rain" -> 85.0
            "Snow" -> 40.0
            "Heatwave" -> 20.0
            "Frost" -> 70.0
            else -> 50.0
        }

        val info = WeatherInfo(
            temperatureFahrenheit = tempFahrenheit,
            humidity = humidity,
            condition = condition,
            cityName = if (location.all { it.isDigit() }) "Zip Code $location" else location
        )

        // Cache the weather locally
        _currentWeather.value = info
        sharedPrefs.edit {
            putFloat("cached_temp", tempFahrenheit.toFloat())
            putFloat("cached_humidity", humidity.toFloat())
            putString("cached_condition", condition)
            putString("cached_city", info.cityName)
        }
        return info
    }

    // Allows manual weather overriding for demo/testing of dynamic care adjustments
    fun simulateWeather(condition: String, tempCelsius: Double, humidity: Double) {
        val tempFahrenheit = tempCelsius * 9.0 / 5.0 + 32.0
        val info = WeatherInfo(
            temperatureFahrenheit = tempFahrenheit,
            humidity = humidity,
            condition = condition,
            cityName = "Simulated Eco-Zone"
        )
        _currentWeather.value = info
        sharedPrefs.edit {
            putFloat("cached_temp", tempFahrenheit.toFloat())
            putFloat("cached_humidity", humidity.toFloat())
            putString("cached_condition", condition)
            putString("cached_city", info.cityName)
        }
    }
}
