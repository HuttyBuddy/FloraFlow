package com.example.ui.screens.arlens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

/**
 * Data class representing visual properties adjusted dynamically by plant health/growth state.
 */
data class BotanicalVisuals(
    val scale: Float,
    val rotationOffset: Float,
    val colorFilter: ColorFilter?,
    val healthStatus: String,
    val healthColor: Color,
    val showWaterParticles: Boolean
)

/**
 * Computes the correct visual styling overrides for a plant based on its moisture and growth stage.
 */
fun getBotanicalVisuals(moisture: Float, growthStage: String): BotanicalVisuals {
    // 1. Stage-to-Scale mappings
    val scale = when (growthStage) {
        "Sprout" -> 0.4f
        "Young" -> 0.7f
        "Colossal" -> 1.5f
        else -> 1.0f // Mature
    }

    // 2. Moisture-dependent effects
    return when {
        moisture < 0.3f -> {
            val severity = 1f - (moisture / 0.3f)
            // Color matrix reducing blue channel to create a yellow-brownish drought look
            val matrix = ColorMatrix(floatArrayOf(
                0.9f, 0.1f, 0.0f, 0.0f, 0.0f,
                0.1f, 0.8f, 0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.5f + (0.3f * (1f - severity)), 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f, 0.0f
            ))
            BotanicalVisuals(
                scale = scale,
                rotationOffset = 12f * severity, // Drooping rotation angle
                colorFilter = ColorFilter.colorMatrix(matrix),
                healthStatus = "DROUGHT / WILTING",
                healthColor = Color(0xFFFF9800),
                showWaterParticles = false
            )
        }
        moisture > 0.85f -> {
            val severity = (moisture - 0.85f) / 0.15f
            // Color matrix reducing red channel and boosting blue/green to create a waterlogged look
            val matrix = ColorMatrix(floatArrayOf(
                0.6f + (0.2f * (1f - severity)), 0.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 1.1f, 0.1f, 0.0f, 0.0f,
                0.0f, 0.1f, 1.2f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f, 0.0f
            ))
            BotanicalVisuals(
                scale = scale,
                rotationOffset = 0f,
                colorFilter = ColorFilter.colorMatrix(matrix),
                healthStatus = "WATERLOGGED / OVERWATERED",
                healthColor = Color(0xFF00E5FF),
                showWaterParticles = true
            )
        }
        else -> {
            BotanicalVisuals(
                scale = scale,
                rotationOffset = 0f,
                colorFilter = null,
                healthStatus = "OPTIMAL HYDRATION",
                healthColor = Color(0xFF00FF66),
                showWaterParticles = false
            )
        }
    }
}
