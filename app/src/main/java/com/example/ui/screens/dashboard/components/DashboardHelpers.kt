package com.example.ui.screens.dashboard.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.model.MoodLog

// Helpers
fun isToday(timestamp: Long): Boolean {
    val dayMillis = 24 * 60 * 60 * 1000
    val todayStart = (System.currentTimeMillis() / dayMillis) * dayMillis
    return timestamp >= todayStart
}

fun calculateStreak(logs: List<MoodLog>): Int {
    if (logs.isEmpty()) return 0
    val dayMillis = 24 * 60 * 60 * 1000
    val days = logs.map { it.timestamp / dayMillis }.distinct().sortedDescending()
    
    var streak = 1
    val today = System.currentTimeMillis() / dayMillis
    
    if (days.first() < today - 1) {
        return 0 // Streak broken
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

// Helper to resolve string based Material Symbols
fun imageIcons(name: String): ImageVector = when (name) {
    "eco" -> Icons.Default.Eco
    "spa" -> Icons.Default.Spa
    else -> Icons.Default.Terrain
}

data class SensoryRitual(
    val category: String,
    val text: String,
    val noteMarker: String
)

fun getRitualForCategory(category: String): SensoryRitual {
    return when (category.uppercase()) {
        "NATURE VIEWS" -> SensoryRitual("NATURE VIEWS", "Look out window at green spaces for 2m", "[Ritual: Nature Views Window Break]")
        "LIVING PLANTS" -> SensoryRitual("LIVING PLANTS", "Gently inspect your indoor plants' soil", "[Ritual: Plant Touch & Care]")
        "NATURAL LIGHT" -> SensoryRitual("NATURAL LIGHT", "Spend 5 minutes in natural window light", "[Ritual: Natural Daylight Rest]")
        "ACOUSTIC CALM" -> SensoryRitual("ACOUSTIC CALM", "Spend 3 minutes in mindful silence", "[Ritual: Acoustic Quiet Break]")
        "NATURAL MATERIALS" -> SensoryRitual("NATURAL MATERIALS", "Touch a natural texture (wood/clay pot)", "[Ritual: Natural Texture Contact]")
        "AIR & VENTILATION" -> SensoryRitual("AIR & VENTILATION", "Open window for 10m to aerate room", "[Ritual: Open Window Fresh Air]")
        "ORGANIC FORMS" -> SensoryRitual("ORGANIC FORMS", "Trace a curved plant leaf boundary", "[Ritual: Leaf Contour Trace]")
        "WATER FEATURES" -> SensoryRitual("WATER FEATURES", "Listen to water flow/rain sounds for 2m", "[Ritual: Water Soundscape Rest]")
        "SENSORY RICHNESS" -> SensoryRitual("SENSORY RICHNESS", "Scent break: Smell lavender, pine, or soil", "[Ritual: Natural Scent Breath]")
        "SEASONAL AWARENESS" -> SensoryRitual("SEASONAL AWARENESS", "Observe one outdoor seasonal change today", "[Ritual: Season Observance]")
        else -> SensoryRitual("GENERAL", "Take 5 deep breaths in your sanctuary", "[Ritual: General Deep Breaths]")
    }
}
