package com.example.ui.screens.arlens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draws a spatial horizontal grid with a perspective/drift effect driven by time.
 */
fun DrawScope.drawPerspectiveGrid(lineColor: Color, climateTimeFactor: Float) {
    val columns = 6
    val rows = 8
    val gridOffset = (climateTimeFactor * 2f) % (size.height / rows)
    
    // Draw columns (vertical perspective)
    for (i in 0..columns) {
        val x = size.width / columns * i
        drawLine(
            color = lineColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1.dp.toPx()
        )
    }
    
    // Draw rows with perspective drift
    for (i in -1..rows) {
        val y = size.height / rows * i + gridOffset
        if (y in 0f..size.height) {
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

/**
 * Renders a spinning diagnostic bracket (reticle) around the selection.
 */
fun DrawScope.drawHoloReticle(
    center: Offset,
    radius: Float,
    rotationAngle: Float,
    glowColor: Color
) {
    // Outer arc segments
    drawArc(
        color = glowColor.copy(alpha = 0.5f),
        startAngle = rotationAngle,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(width = 2.dp.toPx())
    )
    drawArc(
        color = glowColor.copy(alpha = 0.5f),
        startAngle = rotationAngle + 180f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(width = 2.dp.toPx())
    )
    
    // Inner crosshair indicators
    val tickLen = 8.dp.toPx()
    drawLine(
        color = glowColor.copy(alpha = 0.7f),
        start = Offset(center.x - radius * 0.4f, center.y),
        end = Offset(center.x - radius * 0.4f + tickLen, center.y),
        strokeWidth = 1.5.dp.toPx()
    )
    drawLine(
        color = glowColor.copy(alpha = 0.7f),
        start = Offset(center.x + radius * 0.4f, center.y),
        end = Offset(center.x + radius * 0.4f - tickLen, center.y),
        strokeWidth = 1.5.dp.toPx()
    )
    drawLine(
        color = glowColor.copy(alpha = 0.7f),
        start = Offset(center.x, center.y - radius * 0.4f),
        end = Offset(center.x, center.y - radius * 0.4f + tickLen),
        strokeWidth = 1.5.dp.toPx()
    )
    drawLine(
        color = glowColor.copy(alpha = 0.7f),
        start = Offset(center.x, center.y + radius * 0.4f),
        end = Offset(center.x, center.y + radius * 0.4f - tickLen),
        strokeWidth = 1.5.dp.toPx()
    )
}

/**
 * Draws a glowing vector connecting synergized plants with animated flow dashes.
 */
fun DrawScope.drawVitalityFlow(
    start: Offset,
    end: Offset,
    climateTimeFactor: Float,
    glowColor: Color
) {
    val pulse = 0.8f + 0.2f * sin(climateTimeFactor * 0.3f)
    
    // Broad backing laser glow
    drawLine(
        color = glowColor.copy(alpha = 0.15f * pulse),
        start = start,
        end = end,
        strokeWidth = 10.dp.toPx() * pulse
    )
    
    // Strong core laser line
    drawLine(
        color = glowColor.copy(alpha = 0.85f * pulse),
        start = start,
        end = end,
        strokeWidth = 2.5.dp.toPx()
    )

    // Dashed sap-vitality flow along a curve
    val path = Path().apply {
        moveTo(start.x, start.y)
        val midX = (start.x + end.x) / 2f
        val midY = (start.y + end.y) / 2f - 35f * sin(climateTimeFactor * 0.08f)
        quadraticTo(midX, midY, end.x, end.y)
    }

    val dashPhase = -climateTimeFactor * 3.5f
    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 25f), dashPhase)
    drawPath(
        path = path,
        color = Color(0xFFEEFF41).copy(alpha = 0.75f),
        style = Stroke(width = 2.dp.toPx(), pathEffect = pathEffect)
    )
}
