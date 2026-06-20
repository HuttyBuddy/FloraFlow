package com.example.ui.screens.dashboard

import android.content.Context
import android.util.Log
import com.example.data.model.CareTask
import com.example.data.model.Plant
import com.example.data.repository.GardenRepository
import com.example.data.repository.WeatherRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class CareScheduler(
    private val context: Context,
    private val gardenRepository: GardenRepository,
    private val weatherRepository: WeatherRepository
) {

    // Analyzes plants and generates missing care tasks, adjusting due dates based on weather conditions
    suspend fun syncCareSchedules() {
        val plants = gardenRepository.getPlantsForLayout(-1).first() // We can also retrieve layout specific ones
        val allPlants = mutableListOf<Plant>()
        
        // Let's get all layouts and gather their plants
        val layouts = gardenRepository.allLayouts.first()
        for (layout in layouts) {
            val layoutPlants = gardenRepository.getPlantsForLayout(layout.id).first()
            allPlants.addAll(layoutPlants)
        }

        val weather = weatherRepository.currentWeather.value
        val isRaining = weather.condition.equals("Rain", ignoreCase = true)
        val isHeatwave = weather.condition.equals("Heatwave", ignoreCase = true)
        val isFrost = weather.condition.equals("Frost", ignoreCase = true)

        val existingTasks = gardenRepository.allCareTasks.first()
        val existingPlantTaskTypes = existingTasks.map { it.plantId to it.taskType }.toSet()

        val newTasks = mutableListOf<CareTask>()

        for (plant in allPlants) {
            // Generate basic WATER task if not exists
            if (!existingPlantTaskTypes.contains(plant.id to "WATER")) {
                val intervalDays = getWateringIntervalDays(plant.wateringNeeds)
                val baseDueDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(intervalDays.toLong())
                
                // Adjust due date based on weather
                val adjustedDueDate = adjustDueDateForWeather(baseDueDate, "WATER", isRaining, isHeatwave)

                newTasks.add(
                    CareTask(
                        plantId = plant.id,
                        plantName = plant.name,
                        taskType = "WATER",
                        dueDate = adjustedDueDate,
                        intervalDays = intervalDays,
                        isRecurring = true
                    )
                )
            }

            // Generate FERTILIZE task if not exists
            if (!existingPlantTaskTypes.contains(plant.id to "FERTILIZE")) {
                val intervalDays = 30 // Fertilize monthly
                val baseDueDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(intervalDays.toLong())
                newTasks.add(
                    CareTask(
                        plantId = plant.id,
                        plantName = plant.name,
                        taskType = "FERTILIZE",
                        dueDate = baseDueDate,
                        intervalDays = intervalDays,
                        isRecurring = true
                    )
                )
            }

            // Generate PRUNE task if not exists (seasonal or monthly)
            if (!existingPlantTaskTypes.contains(plant.id to "PRUNE")) {
                val intervalDays = 60 // Prune every 2 months
                val baseDueDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(intervalDays.toLong())
                newTasks.add(
                    CareTask(
                        plantId = plant.id,
                        plantName = plant.name,
                        taskType = "PRUNE",
                        dueDate = baseDueDate,
                        intervalDays = intervalDays,
                        isRecurring = true
                    )
                )
            }
        }

        if (newTasks.isNotEmpty()) {
            gardenRepository.insertCareTasks(newTasks)
            Log.d("CareScheduler", "Generated ${newTasks.size} new care tasks.")
        }

        // Check if any task is due today, and trigger a local notification
        val pendingTasks = gardenRepository.pendingCareTasks.first()
        val now = System.currentTimeMillis()
        val dueTodayCount = pendingTasks.count { it.dueDate <= now }

        if (dueTodayCount > 0) {
            val title = "🌸 FloraFlow Garden Alert"
            val message = if (dueTodayCount == 1) {
                "You have 1 plant care task waiting in your garden today."
            } else {
                "You have $dueTodayCount plant care tasks waiting in your garden today."
            }
            NotificationHelper.sendCareReminder(context, title, message)
        }

        // Special weather alerts (e.g. Frost warnings)
        if (isFrost) {
            val tenderPlants = allPlants.filter { 
                it.type.equals("Succulent", ignoreCase = true) || it.type.equals("Fern", ignoreCase = true)
            }
            if (tenderPlants.isNotEmpty()) {
                NotificationHelper.sendCareReminder(
                    context,
                    "⚠️ Frost Warning Alert!",
                    "Temperature dropped to ${weather.temperatureCelsius}°C. Protect your sensitive plants like ${tenderPlants.joinToString { it.name }} from frost!"
                )
            }
        }
    }

    private fun getWateringIntervalDays(wateringNeeds: String): Int {
        return when (wateringNeeds.lowercase()) {
            "high", "daily", "frequent" -> 1
            "moderate", "medium" -> 4
            "low", "dry" -> 10
            else -> 5
        }
    }

    private fun adjustDueDateForWeather(
        baseDueDate: Long,
        taskType: String,
        isRaining: Boolean,
        isHeatwave: Boolean
    ): Long {
        if (taskType != "WATER") return baseDueDate

        return when {
            isRaining -> {
                // Delay watering by 1 day
                baseDueDate + TimeUnit.DAYS.toMillis(1)
            }
            isHeatwave -> {
                // Bring forward by 1 day (so it's watered sooner)
                baseDueDate - TimeUnit.DAYS.toMillis(1)
            }
            else -> baseDueDate
        }
    }

    // Complete a task and schedule the next instance
    suspend fun completeTask(task: CareTask) {
        val now = System.currentTimeMillis()
        gardenRepository.completeCareTask(task.id, now)

        if (task.isRecurring) {
            val nextDueDate = now + TimeUnit.DAYS.toMillis(task.intervalDays.toLong())
            val nextTask = CareTask(
                plantId = task.plantId,
                plantName = task.plantName,
                taskType = task.taskType,
                dueDate = nextDueDate,
                intervalDays = task.intervalDays,
                isRecurring = true
            )
            gardenRepository.insertCareTask(nextTask)
        }
    }
}
