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

    private data class WidgetData(
        val streak: Int,
        val nextTask: String,
        val weather: String,
        val neuralLoad: String,
        val stepsProgress: String
    )

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val database = GardenDatabase.getDatabase(context)
        val dao = database.gardenDao()

        val widgetData = runBlocking {
            val moodLogs = dao.getAllMoodLogs().first()
            val pendingTasks = dao.getPendingCareTasks().first()
            val assessments = dao.getAllAssessmentResults().first()

            val streakVal = calculateStreak(moodLogs)

            val nextTask = pendingTasks.firstOrNull { it.dueDate <= System.currentTimeMillis() }
                ?: pendingTasks.firstOrNull()
            val nextTaskStr = if (nextTask != null) {
                "Next: ${nextTask.taskType} ${nextTask.plantName}"
            } else {
                "Next: All tasks complete!"
            }

            val weatherPrefs = context.getSharedPreferences("floraflow_weather_prefs", Context.MODE_PRIVATE)
            val condition = weatherPrefs.getString("cached_condition", "Clear") ?: "Clear"
            val temp = weatherPrefs.getFloat("cached_temp", 71.6f)
            val city = weatherPrefs.getString("cached_city", "Garden") ?: "Garden"
            val weatherStr = "Weather: ${getWeatherEmoji(condition)} $city (${temp.toInt()}°F • $condition)"

            val latestAssessment = assessments.firstOrNull()
            val neuralStr = if (latestAssessment != null) {
                val score = latestAssessment.score
                val zone = when (score) {
                    in 15..20 -> "Green Zone"
                    in 8..14 -> "Yellow Zone"
                    else -> "Red Zone"
                }
                "Neural Load: $score/20 ($zone)"
            } else {
                "Neural Load: Not Taken"
            }

            val appPrefs = context.getSharedPreferences("floraflow_billing_prefs", Context.MODE_PRIVATE)
            val isAssessmentSkipped = appPrefs.getBoolean("assessment_skipped", false)
            val finalNeuralStr = if (isAssessmentSkipped) "Neural Load: Skipped" else neuralStr

            val step1 = appPrefs.getBoolean("step_1_completed", false)
            val step2 = appPrefs.getBoolean("step_2_completed", false)
            val step3 = appPrefs.getBoolean("step_3_completed", false)
            var completedCount = 0
            if (step1) completedCount++
            if (step2) completedCount++
            if (step3) completedCount++
            val stepsStr = "Next Steps: $completedCount/3 Completed"

            WidgetData(streakVal, nextTaskStr, weatherStr, finalNeuralStr, stepsStr)
        }

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.garden_widget)
            views.setTextViewText(R.id.widget_streak, "🔥 ${widgetData.streak} Day Streak")
            views.setTextViewText(R.id.widget_next_task, widgetData.nextTask)
            views.setTextViewText(R.id.widget_weather, widgetData.weather)
            views.setTextViewText(R.id.widget_neural_load, widgetData.neuralLoad)
            views.setTextViewText(R.id.widget_steps_progress, widgetData.stepsProgress)

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
            views.setOnClickPendingIntent(R.id.widget_neural_load, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_steps_progress, pendingIntent)

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
