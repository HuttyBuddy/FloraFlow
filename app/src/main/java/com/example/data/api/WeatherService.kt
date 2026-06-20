package com.example.data.api

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class WeatherResponse(
    val main: WeatherMain,
    val weather: List<WeatherDescription>,
    val name: String? = null
)

@JsonClass(generateAdapter = true)
data class WeatherMain(
    val temp: Double, // in Celsius
    val humidity: Double
)

@JsonClass(generateAdapter = true)
data class WeatherDescription(
    val main: String, // e.g. "Rain", "Clear", "Clouds", "Snow"
    val description: String
)

interface WeatherService {
    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("q") location: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): WeatherResponse
}
