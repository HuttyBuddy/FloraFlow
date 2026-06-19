package com.example.ui.screens.arlens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.cos
import kotlin.math.sin

/**
 * Particle object for local weather system rendering
 */
data class ArParticle(
    var x: Float,
    var y: Float,
    val speed: Float,
    val size: Float,
    var alpha: Float,
    var driftAngle: Float
)

/**
 * Draws the active environmental weather particles onto the Canvas.
 */
fun DrawScope.drawWeatherParticles(
    activeWeather: String,
    particlesList: List<ArParticle>,
    climateTimeFactor: Float
) {
    if (activeWeather == "Clear Sky" || particlesList.isEmpty()) return

    particlesList.forEach { particle ->
        when (activeWeather) {
            "Gentle Rain" -> {
                // Draw thin slanted raindrop lines
                val rawY = (particle.y + particle.speed * climateTimeFactor) % 1000f
                val currentY = if (rawY < 0) rawY + 1000f else rawY
                val rawX = (particle.x + 1.5f * climateTimeFactor) % 1000f
                val currentX = if (rawX < 0) rawX + 1000f else rawX

                drawLine(
                    color = Color.White.copy(alpha = particle.alpha),
                    start = Offset(currentX, currentY),
                    end = Offset(currentX + 2f, currentY + 14f),
                    strokeWidth = particle.size
                )
            }
            "Cherry Blossoms" -> {
                // Draw soft pink cherry blossom drifting petals
                val rawY = (particle.y + particle.speed * 0.6f * climateTimeFactor) % 1000f
                val currentY = if (rawY < 0) rawY + 1000f else rawY
                val rawX = (particle.x + 0.8f * climateTimeFactor) % 1000f
                val currentX = if (rawX < 0) rawX + 1000f else rawX

                drawCircle(
                    color = Color(0xFFFFC0CB).copy(alpha = particle.alpha),
                    radius = particle.size,
                    center = Offset(currentX, currentY)
                )
            }
            "Fireflies Spark" -> {
                // Draw glowing yellow magical fireflies pulsing
                val rawY = (particle.y + sin(climateTimeFactor * 0.05f + particle.driftAngle) * 50f) % 1000f
                val currentY = if (rawY < 0) rawY + 1000f else rawY
                val rawX = (particle.x + cos(climateTimeFactor * 0.05f + particle.driftAngle) * 50f) % 1000f
                val currentX = if (rawX < 0) rawX + 1000f else rawX
                val pulseAlpha = particle.alpha * (0.4f + 0.6f * sin(climateTimeFactor * 0.1f + particle.driftAngle))

                // Glow aura
                drawCircle(
                    color = Color(0xFFCCFF00).copy(alpha = pulseAlpha * 0.3f),
                    radius = particle.size * 2.2f,
                    center = Offset(currentX, currentY)
                )
                // Center core
                drawCircle(
                    color = Color(0xFFEEFF41).copy(alpha = pulseAlpha),
                    radius = particle.size * 0.8f,
                    center = Offset(currentX, currentY)
                )
            }
        }
    }
}
