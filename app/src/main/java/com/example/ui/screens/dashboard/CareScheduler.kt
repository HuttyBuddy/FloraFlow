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
        val allPlants = gardenRepository.allPlants.first()

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

        val notifiedPrefs = context.getSharedPreferences("floraflow_notifications", Context.MODE_PRIVATE)
        val notifiedIds = notifiedPrefs.getStringSet("notified_task_ids", emptySet())?.toMutableSet() ?: mutableSetOf()

        // Check for Plant Rescue Alerts (WATER task overdue by 3+ days)
        val overdueWaterTasks = pendingTasks.filter {
            it.taskType == "WATER" && (now - it.dueDate) >= TimeUnit.DAYS.toMillis(3)
        }

        if (overdueWaterTasks.isNotEmpty()) {
            val task = overdueWaterTasks.minByOrNull { it.dueDate }!!
            val taskIdStr = task.id.toString()
            val notifKey = "rescue_$taskIdStr"
            if (!notifiedIds.contains(notifKey)) {
                val overdueDays = TimeUnit.MILLISECONDS.toDays(now - task.dueDate)
                val totalDays = task.intervalDays + overdueDays
                val title = "🚨 Plant Rescue Alert!"
                val message = "Your ${task.plantName} hasn't been watered in $totalDays days — it might be getting thirsty."
                NotificationHelper.sendCareReminder(context, title, message, androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                
                notifiedIds.add(notifKey)
                notifiedPrefs.edit().putStringSet("notified_task_ids", notifiedIds).apply()
            }
        } else if (dueTodayCount > 0) {
            // Weather-aware notification copies
            val firstTask = pendingTasks.firstOrNull { it.dueDate <= now }
            if (firstTask != null) {
                val taskIdStr = firstTask.id.toString()
                val notifKey = "due_$taskIdStr"
                if (!notifiedIds.contains(notifKey)) {
                    val billingPrefs = context.getSharedPreferences("floraflow_billing_prefs", Context.MODE_PRIVATE)
                    val assessmentCategories = billingPrefs.getString("assessment_categories", "")?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
                    val categoryPersonalizedTip = when (assessmentCategories.firstOrNull()) {
                        "NATURE VIEWS" -> "💡 Tip: Spend a moment looking at natural green spaces today to lower stress."
                        "LIVING PLANTS" -> "💡 Tip: Spend some mindful time near your plants to rest."
                        "NATURAL LIGHT" -> "💡 Tip: Try to work or relax in bright, natural light."
                        "ACOUSTIC CALM" -> "💡 Tip: Rest your mind with a soothing soundscape session."
                        "NATURAL MATERIALS" -> "💡 Tip: Touch or surround yourself with wood or stone to ground."
                        "AIR & VENTILATION" -> "💡 Tip: Get 10 minutes of fresh air to refresh your space."
                        "ORGANIC FORMS" -> "💡 Tip: Soften your view by looking at organic, curved designs."
                        "WATER FEATURES" -> "💡 Tip: Listen to the calming sound of water to slow down."
                        "SENSORY RICHNESS" -> "💡 Tip: Enjoy a natural scent (like cedarwood or lavender) today."
                        "SEASONAL AWARENESS" -> "💡 Tip: Connect with the season: notice the weather changes."
                        else -> "💡 Tip: Spend a few minutes nurturing your plants to restore calm."
                    }
                    val plantName = firstTask.plantName
                    val title = "🌸 FloraFlow Garden Care"
                    val baseMessage = when {
                        isRaining -> "Rain tonight in ${weather.cityName} — your $plantName can skip watering today."
                        isHeatwave -> "Heatwave alert! Give your $plantName extra shade and a deep soak."
                        else -> "Clear skies in ${weather.cityName} — your $plantName is ready for care!"
                    }
                    val message = "$baseMessage $categoryPersonalizedTip"
                    NotificationHelper.sendCareReminder(context, title, message)
                    
                    notifiedIds.add(notifKey)
                    notifiedPrefs.edit().putStringSet("notified_task_ids", notifiedIds).apply()
                }
            }
        }

        // Clean up old notified IDs to avoid SharedPreferences bloating
        val currentPendingIds = pendingTasks.map { it.id.toString() }.toSet()
        val prunedNotifiedIds = notifiedIds.filter { id ->
            val actualId = id.substringAfter("_")
            currentPendingIds.contains(actualId)
        }.toSet()
        notifiedPrefs.edit().putStringSet("notified_task_ids", prunedNotifiedIds).apply()

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
