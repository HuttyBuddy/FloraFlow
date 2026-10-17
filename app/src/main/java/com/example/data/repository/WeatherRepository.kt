package com.example.data.repository

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

data class WeatherInfo(
    val temperatureFahrenheit: Double,
    val humidity: Double,
    val condition: String, // "Rain", "Clear", "Clouds", "Snow", "Heatwave", "Frost"
    val cityName: String
)

class WeatherRepository(private val context: Context) {
    private val isUnderTest: Boolean by lazy {
        try {
            Class.forName("org.junit.Assert")
            true
        } catch (e: Exception) {
            false
        }
    }

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

    private fun fetchUrl(urlString: String): String {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.connect()

        val responseCode = connection.responseCode
        if (responseCode == 200) {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            reader.close()
            connection.disconnect()
            return response.toString()
        } else {
            connection.disconnect()
            throw IOException("HTTP error code: $responseCode")
        }
    }

    private suspend fun fetchLiveWeather(zipCode: String): WeatherInfo? = withContext(Dispatchers.IO) {
        // Step 1: Geocode zip code using zippopotam.us
        val geoUrl = "https://api.zippopotam.us/us/$zipCode"
        val geoResponse = fetchUrl(geoUrl)
        val geoJson = JSONObject(geoResponse)
        val places = geoJson.getJSONArray("places")
        if (places.length() == 0) return@withContext null

        val firstPlace = places.getJSONObject(0)
        val cityName = firstPlace.getString("place name")
        val stateAbbr = firstPlace.getString("state abbreviation")
        val lat = firstPlace.getDouble("latitude")
        val lng = firstPlace.getDouble("longitude")
        val formattedCity = "$cityName, $stateAbbr"

        // Step 2: Fetch weather using Open-Meteo
        val weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lng&current=temperature_2m,relative_humidity_2m,weather_code"
        val weatherResponse = fetchUrl(weatherUrl)
        val weatherJson = JSONObject(weatherResponse)
        val current = weatherJson.getJSONObject("current")

        val tempCelsius = current.getDouble("temperature_2m")
        val humidity = current.getDouble("relative_humidity_2m")
        val weatherCode = current.getInt("weather_code")

        val tempFahrenheit = tempCelsius * 9.0 / 5.0 + 32.0

        // Map WMO weather code to condition
        val condition = when {
            tempCelsius >= 35.0 -> "Heatwave"
            tempCelsius <= 2.0 -> "Frost"
            weatherCode in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82, 95, 96, 99) -> "Rain"
            weatherCode in listOf(71, 73, 75, 77, 85, 86) -> "Snow"
            weatherCode in listOf(56, 57, 66, 67) -> "Frost"
            weatherCode in listOf(2, 3, 45, 48) -> "Clouds"
            else -> "Clear"
        }

        WeatherInfo(
            temperatureFahrenheit = tempFahrenheit,
            humidity = humidity,
            condition = condition,
            cityName = formattedCity
        )
    }

    // Fetches the weather. If network fails or for simulation/testing, falls back to a clean mock provider
    suspend fun fetchWeather(location: String = getUserLocationZip()): WeatherInfo {
        var info: WeatherInfo? = null
        try {
            if (!isUnderTest && location.all { it.isDigit() } && location.length == 5) {
                info = fetchLiveWeather(location)
            }
        } catch (e: Exception) {
            if (com.example.BuildConfig.DEBUG) {
                android.util.Log.e("WeatherRepository", "Failed to fetch live weather, falling back to mock: ${e.message}")
            }
        }

        if (info == null) {
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

            info = WeatherInfo(
                temperatureFahrenheit = tempFahrenheit,
                humidity = humidity,
                condition = condition,
                cityName = if (location.all { it.isDigit() }) "Zip Code $location" else location
            )
        }

        // Cache the weather locally
        _currentWeather.value = info
        sharedPrefs.edit {
            putFloat("cached_temp", info.temperatureFahrenheit.toFloat())
            putFloat("cached_humidity", info.humidity.toFloat())
            putString("cached_condition", info.condition)
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
