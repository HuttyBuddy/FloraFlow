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
        val allPlants = mutableListOf<Plant>()
        
        // Optimize: Fetch all plants in a single query instead of layout by layout (N+1 anti-pattern fix)
        val allPlantsFromDb = gardenRepository.allPlants.first()
        allPlants.addAll(allPlantsFromDb)

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

        val pendingTasks = gardenRepository.pendingCareTasks.first()
        val now = System.currentTimeMillis()
        val dueTodayCount = pendingTasks.count { it.dueDate <= now }

        // Check for Plant Rescue Alerts (WATER task overdue by 3+ days)
        val overdueWaterTasks = pendingTasks.filter {
            it.taskType == "WATER" && (now - it.dueDate) >= TimeUnit.DAYS.toMillis(3)
        }

        if (overdueWaterTasks.isNotEmpty()) {
            val task = overdueWaterTasks.minByOrNull { it.dueDate }!!
            val overdueDays = TimeUnit.MILLISECONDS.toDays(now - task.dueDate)
            val totalDays = task.intervalDays + overdueDays
            val title = "🚨 Plant Rescue Alert!"
            val message = "Your ${task.plantName} hasn't been watered in $totalDays days — it might be getting thirsty."
            NotificationHelper.sendCareReminder(context, title, message, androidx.core.app.NotificationCompat.PRIORITY_HIGH)
        } else if (dueTodayCount > 0) {
            // Weather-aware notification copies
            val firstTask = pendingTasks.firstOrNull { it.dueDate <= now }
            val plantName = firstTask?.plantName ?: "plants"
            val title = "🌸 FloraFlow Garden Care"
            
            val message = when {
                isRaining -> {
                    "Rain tonight in ${weather.cityName} — your $plantName can skip watering today."
                }
                isHeatwave -> {
                    "Heatwave alert! Give your $plantName extra shade and a deep soak."
                }
                else -> {
                    "Clear skies in ${weather.cityName} — your $plantName is ready for care!"
                }
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
                    "Temperature dropped to ${weather.temperatureFahrenheit.toInt()}°F. Protect your sensitive plants like ${tenderPlants.joinToString { it.name }} from frost!"
                )
            }
        }

        // Trigger home screen widget updates
        try {
            val widgetIntent = android.content.Intent(context, GardenWidgetProvider::class.java).apply {
                action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = android.appwidget.AppWidgetManager.getInstance(context)
                .getAppWidgetIds(android.content.ComponentName(context, GardenWidgetProvider::class.java))
            widgetIntent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(widgetIntent)
        } catch (_: Exception) {}
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
