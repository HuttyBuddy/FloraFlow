package com.example.ui.screens.dashboard.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.model.MoodLog

// Helpers
fun isToday(timestamp: Long): Boolean {
    val localDate = java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    val todayDate = java.time.LocalDate.now(java.time.ZoneId.systemDefault())
    return localDate == todayDate
}

fun calculateStreak(logs: List<MoodLog>): Int {
    if (logs.isEmpty()) return 0
    val localZone = java.time.ZoneId.systemDefault()
    val today = java.time.LocalDate.now(localZone)
    
    val dates = logs.map { 
        java.time.Instant.ofEpochMilli(it.timestamp).atZone(localZone).toLocalDate() 
    }.distinct().sortedDescending()
    
    if (dates.isEmpty()) return 0
    
    val firstDate = dates.first()
    if (firstDate != today && firstDate != today.minusDays(1)) {
        return 0
    }
    
    var streak = 1
    for (i in 0 until dates.size - 1) {
        if (dates[i].minusDays(1) == dates[i+1]) {
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
