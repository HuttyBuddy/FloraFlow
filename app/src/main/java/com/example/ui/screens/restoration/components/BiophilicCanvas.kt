package com.example.ui.screens.restoration.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.ui.components.graphics.dappledSunlightOverlay

/**
 * BiophilicCanvas renders live, organic ambient visual graphics (light rays, foliage ripples,
 * pulsing sanctuary aura rings) synchronized with eco-acoustic playback.
 */
@Composable
fun BiophilicCanvas(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "BiophilicCanvasTransition")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPlaying) 4000 else 8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val lightRayAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "lightRayAngle"
    )

    Box(modifier = modifier.dappledSunlightOverlay(enabled = isPlaying, intensity = 0.15f)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val center = Offset(width * 0.5f, height * 0.35f)

            // 1. Subtle radial ambient canopy glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x331F483E),
                        Color(0x1A43493E),
                        Color.Transparent
                    ),
                    center = center,
                    radius = width * 0.8f * pulseScale
                ),
                center = center,
                radius = width * 0.8f * pulseScale
            )

            // 2. Concentric biophilic aura rings
            val ringColor = if (isPlaying) Color(0x26A8E6CF) else Color(0x12A8E6CF)
            drawCircle(
                color = ringColor,
                center = center,
                radius = width * 0.35f * pulseScale,
                style = Stroke(width = 2f)
            )
            drawCircle(
                color = ringColor.copy(alpha = ringColor.alpha * 0.6f),
                center = center,
                radius = width * 0.55f * pulseScale,
                style = Stroke(width = 1.5f)
            )

            // 3. Canopy light filtration ray
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x22D97724),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.15f, height * 0.1f),
                    radius = width * 0.6f
                ),
                center = Offset(width * 0.15f, height * 0.1f),
                radius = width * 0.6f
            )
        }

        content()
    }
}
