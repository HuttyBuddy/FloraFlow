package com.example.ui.screens.dashboard

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.R
import com.example.data.database.GardenDatabase
import com.example.data.model.MoodLog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class GardenWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val database = GardenDatabase.getDatabase(context)
        val dao = database.gardenDao()

        val (streak, nextTaskName, weatherInfo) = runBlocking {
            val moodLogs = dao.getAllMoodLogs().first()
            val pendingTasks = dao.getPendingCareTasks().first()

            val streakVal = calculateStreak(moodLogs)

            val nextTask = pendingTasks.firstOrNull { it.dueDate <= System.currentTimeMillis() }
                ?: pendingTasks.firstOrNull()
            val nextTaskStr = if (nextTask != null) {
                "Next: ${nextTask.taskType} ${nextTask.plantName}"
            } else {
                "Next: All tasks complete!"
            }

            val sharedPrefs = context.getSharedPreferences("floraflow_weather_prefs", Context.MODE_PRIVATE)
            val condition = sharedPrefs.getString("condition", "Clear") ?: "Clear"
            val temp = sharedPrefs.getFloat("temperature", 22.0f)
            val city = sharedPrefs.getString("city_name", "Garden") ?: "Garden"
            val weatherStr = "Weather: ${getWeatherEmoji(condition)} $city (${temp.toInt()}°C • $condition)"

            Triple(streakVal, nextTaskStr, weatherStr)
        }

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.garden_widget)
            views.setTextViewText(R.id.widget_streak, "🔥 $streak Day Streak")
            views.setTextViewText(R.id.widget_next_task, nextTaskName)
            views.setTextViewText(R.id.widget_weather, weatherInfo)

            val intent = Intent(context, com.example.MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_streak, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_next_task, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_weather, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun calculateStreak(logs: List<MoodLog>): Int {
        if (logs.isEmpty()) return 0
        val dayMillis = 24 * 60 * 60 * 1000
        val days = logs.map { it.timestamp / dayMillis }.distinct().sortedDescending()

        var streak = 1
        val today = System.currentTimeMillis() / dayMillis

        if (days.first() < today - 1) {
            return 0
        }

        for (i in 0 until days.size - 1) {
            if (days[i] - days[i+1] == 1L) {
                streak++
            } else {
                break
            }
        }
        return streak
    }

    private fun getWeatherEmoji(condition: String): String {
        return when (condition.lowercase()) {
            "rain" -> "🌧️"
            "snow" -> "❄️"
            "clear" -> "☀️"
            "clouds" -> "☁️"
            "heatwave" -> "🥵"
            "frost" -> "🥶"
            else -> "⛅"
        }
    }
}
