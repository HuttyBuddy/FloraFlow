package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class PlantParentArchetype(
    val title: String,
    val icon: String,
    val description: String,
    val badgeColorHex: String
) {
    JUNGLE_MAXIMALIST(
        title = "Jungle Maximalist",
        icon = "🌿",
        description = "Surrounded by living foliage, maximizing natural light and humidity zones.",
        badgeColorHex = "#2E7D32"
    ),
    CACTUS_SURVIVOR(
        title = "Cactus Survivor",
        icon = "🌵",
        description = "Minimalist practitioner specializing in ultra-resilient, low-water species.",
        badgeColorHex = "#E65100"
    ),
    SERIAL_OVERWATERER(
        title = "Serial Overwaterer",
        icon = "💦",
        description = "Deeply attentive care-giver who loves frequent misting and soil checks.",
        badgeColorHex = "#0288D1"
    ),
    CYBERPUNK_BOTANIST(
        title = "Cyberpunk Botanist",
        icon = "⚡",
        description = "Thrives in artificial LED zones with high binaural brainwave focus.",
        badgeColorHex = "#7B1FA2"
    ),
    SANCTUARY_MASTER(
        title = "Sanctuary Master",
        icon = "✨",
        description = "Optimal biophilic harmony achieved with ideal daylight, air, and acoustics.",
        badgeColorHex = "#D4AF37"
    );

    companion object {
        fun calculateArchetype(
            score: Int,
            lowestCategories: List<String> = emptyList(),
            careStreak: Int = 0
        ): PlantParentArchetype {
            val upperCategories = lowestCategories.map { it.uppercase() }
            return when {
                score >= 16 -> SANCTUARY_MASTER
                upperCategories.any { it.contains("PLANT") || it.contains("GREEN") } && score >= 12 -> JUNGLE_MAXIMALIST
                upperCategories.any { it.contains("LIGHT") || it.contains("DAYLIGHT") } -> CYBERPUNK_BOTANIST
                careStreak > 5 -> SERIAL_OVERWATERER
                score in 1..7 -> CACTUS_SURVIVOR
                else -> JUNGLE_MAXIMALIST
            }
        }
    }
}
