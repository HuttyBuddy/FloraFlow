package com.example.ui.screens.dashboard

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.database.GardenDatabase
import com.example.data.repository.GardenRepository
import com.example.data.repository.WeatherRepository

class CareSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (com.example.BuildConfig.DEBUG) {
            Log.d("CareSyncWorker", "Executing background garden care synchronization...")
        }
        try {
            val database = GardenDatabase.getDatabase(applicationContext)
            val gardenRepository = GardenRepository(database.gardenDao())
            val weatherRepository = WeatherRepository(applicationContext)

            // Fetch latest weather parameters
            weatherRepository.fetchWeather()

            // Run the CareScheduler sync
            val scheduler = CareScheduler(applicationContext, gardenRepository, weatherRepository)
            scheduler.syncCareSchedules()

            return Result.success()
        } catch (e: Exception) {
            if (com.example.BuildConfig.DEBUG) {
                Log.e("CareSyncWorker", "Error running care scheduler sync: ${e.message}")
            }
            return Result.retry()
        }
    }
}
