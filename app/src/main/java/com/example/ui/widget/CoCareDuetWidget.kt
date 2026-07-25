package com.example.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R

class CoCareDuetWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val prefs = context.getSharedPreferences("floraflow_billing_prefs", Context.MODE_PRIVATE)
        val careStreak = prefs.getInt("care_streak_days", 3)
        val partnerName = prefs.getString("partner_name", "Partner Alex") ?: "Partner Alex"

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.garden_widget)
            views.setTextViewText(R.id.widget_streak, "🌸 Co-Care Duet • 🔥 $careStreak Days")
            views.setTextViewText(R.id.widget_next_task, "🌿 Living Monstera Node: Blooming")
            views.setTextViewText(R.id.widget_weather, "• You: Watered today ✅")
            views.setTextViewText(R.id.widget_neural_load, "• $partnerName: Mindful Focus 10m ago 🌸")
            views.setTextViewText(R.id.widget_steps_progress, "Tap to open Co-Care Sanctuary Hub →")

            val intent = Intent(context, MainActivity::class.java)
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
}
