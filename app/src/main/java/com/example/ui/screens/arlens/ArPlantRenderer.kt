package com.example.ui.screens.arlens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ProceduralArPlant(
    decalOverride: PlantExtraProps,
    emoji: String,
    name: String,
    isSelected: Boolean,
    climateTimeFactor: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val moisture = decalOverride.moisture
    val growthStage = decalOverride.growthStage
    val customLabel = decalOverride.customLabel

    // Determine base plant attributes
    val isLavender = name.lowercase().contains("lavender")
    val isJuniperOrMaple = name.lowercase().contains("juniper") || name.lowercase().contains("maple")
    val isFern = name.lowercase().contains("fern")
    val isCactus = name.lowercase().contains("cactus") || name.lowercase().contains("aloe")

    // Determine colors based on moisture / health state
    val healthyGreen = Color(0xFF2E7D32)
    val dryYellow = Color(0xFFC0CA33)
    val dryBrown = Color(0xFF8D6E63)
    val waterloggedTeal = Color(0xFF00796B)

    val leafColor = when {
        moisture < 0.2f -> dryBrown
        moisture < 0.4f -> dryYellow
        moisture > 0.85f -> waterloggedTeal
        else -> healthyGreen
    }

    val flowerColor = when {
        isLavender -> Color(0xFFAB47BC) // Purple
        isJuniperOrMaple -> Color(0xFFFF8A80) // Soft Pink
        isCactus -> Color(0xFFFFB300) // Yellow blooms
        else -> Color(0xFFFFD54F) // Gold/yellow
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = modifier
            .wrapContentSize()
            .clickable(onClick = onClick)
    ) {
        // 1. Sleek Floating Telemetry Label above the plant
        val borderGlow = if (isSelected) Color(0xFF00FF66) else Color.White.copy(alpha = 0.25f)
        val bgContainer = if (isSelected) Color.Black.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.5f)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .shadow(
                    elevation = if (isSelected) 8.dp else 2.dp,
                    shape = RoundedCornerShape(12.dp)
                )
                .clip(RoundedCornerShape(12.dp))
                .background(bgContainer)
                .border(1.dp, borderGlow, RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .animateContentSize()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(emoji, fontSize = 16.sp)
                Text(
                    text = name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Color.White
                )
            }

            if (isSelected) {
                Spacer(modifier = Modifier.height(2.dp))
                val stateText = when {
                    moisture < 0.3f -> "⚠️ WILTING"
                    moisture > 0.85f -> "💧 WATERLOGGED"
                    else -> "♻️ OPTIMAL"
                }
                val stateColor = when {
                    moisture < 0.3f -> Color(0xFFFF9800)
                    moisture > 0.85f -> Color(0xFF00E5FF)
                    else -> Color(0xFF00FF66)
                }
                Text(
                    text = "GS: ${growthStage.uppercase()} | $stateText",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 8.sp,
                    color = stateColor,
                    fontFamily = FontFamily.Monospace
                )
                if (customLabel.isNotEmpty()) {
                    Text(
                        text = "Note: $customLabel",
                        fontSize = 8.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
            }
        }

        // Dotted connection line from the tag to the plant base
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
        ) {
            val start = Offset(size.width / 2, 0f)
            val end = Offset(size.width / 2, size.height)
            drawLine(
                color = if (isSelected) Color(0xFF00FF66).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.3f),
                start = start,
                end = end,
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )
        }

        // 2. Procedural Canvas Plant
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .size(width = 110.dp, height = 120.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val centerX = w / 2f
                val baseY = h * 0.9f

                // --- Ground Ring (AR anchor) ---
                val ringPulse = 0.85f + 0.15f * sin(climateTimeFactor * 0.15f)
                val ringRadiusX = 35f * ringPulse
                val ringRadiusY = 12f * ringPulse
                val ringColor = if (isSelected) Color(0xFF00FF66) else Color(0x7700FFCC)
                
                drawContext.canvas.save()
                drawOval(
                    color = ringColor.copy(alpha = 0.15f),
                    topLeft = Offset(centerX - ringRadiusX, baseY - ringRadiusY),
                    size = Size(ringRadiusX * 2, ringRadiusY * 2)
                )
                drawOval(
                    color = ringColor,
                    topLeft = Offset(centerX - ringRadiusX, baseY - ringRadiusY),
                    size = Size(ringRadiusX * 2, ringRadiusY * 2),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawContext.canvas.restore()

                // --- Procedural Pot ---
                val potWidth = 32f
                val potHeight = 22f
                val potPath = Path().apply {
                    moveTo(centerX - potWidth / 2, baseY - 2f)
                    lineTo(centerX + potWidth / 2, baseY - 2f)
                    lineTo(centerX + potWidth * 0.4f, baseY + potHeight)
                    lineTo(centerX - potWidth * 0.4f, baseY + potHeight)
                    close()
                }
                drawPath(
                    path = potPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFD84315), Color(0xFFBF360C))
                    )
                )
                // Draw pot rim
                drawRect(
                    color = Color(0xFFE64A19),
                    topLeft = Offset(centerX - potWidth * 0.55f, baseY - 5f),
                    size = Size(potWidth * 1.1f, 5f)
                )

                // --- Growth Configuration ---
                val heightScale = when (growthStage) {
                    "Sprout" -> 0.3f
                    "Young" -> 0.6f
                    "Colossal" -> 1.4f
                    else -> 1.0f // Mature
                }

                // Moisture Droop Factor
                val droop = if (moisture < 0.3f) (1f - moisture / 0.3f) * 15f else 0f
                // Wind Sway Angle
                val sway = sin(climateTimeFactor * 0.06f + name.hashCode() * 0.1f) * (8f + 4f * heightScale)

                // Draw central stem/trunk
                val trunkLength = 50f * heightScale
                val trunkControlX = centerX + sway + droop
                val trunkEndX = centerX + sway * 1.5f + droop * 1.3f
                val trunkEndY = baseY - trunkLength

                val trunkPath = Path().apply {
                    moveTo(centerX, baseY - 4f)
                    quadraticTo(
                        trunkControlX, baseY - trunkLength * 0.5f,
                        trunkEndX, trunkEndY
                    )
                }
                
                val trunkStroke = if (growthStage == "Colossal") 5.dp.toPx() else 3.dp.toPx()
                drawPath(
                    path = trunkPath,
                    color = leafColor,
                    style = Stroke(width = trunkStroke)
                )

                var b1EndX = 0f
                var b1EndY = 0f

                if (growthStage != "Sprout") {
                    // Left branch
                    b1EndX = trunkEndX - 24f * heightScale + sway * 0.5f
                    b1EndY = trunkEndY + 12f * heightScale + droop * 0.7f
                    val b1Path = Path().apply {
                        moveTo(trunkControlX, baseY - trunkLength * 0.5f)
                        quadraticTo(
                            trunkControlX - 12f, baseY - trunkLength * 0.65f,
                            b1EndX, b1EndY
                        )
                    }
                    drawPath(path = b1Path, color = leafColor, style = Stroke(width = 2.dp.toPx()))

                    // Right branch
                    val b2EndX = trunkEndX + 26f * heightScale + sway * 0.5f
                    val b2EndY = trunkEndY + 6f * heightScale + droop * 0.7f
                    val b2Path = Path().apply {
                        moveTo(trunkControlX, baseY - trunkLength * 0.4f)
                        quadraticTo(
                            trunkControlX + 14f, baseY - trunkLength * 0.55f,
                            b2EndX, b2EndY
                        )
                    }
                    drawPath(path = b2Path, color = leafColor, style = Stroke(width = 2.dp.toPx()))

                    // foliage / leaf clumps
                    val foliageRadius = if (growthStage == "Colossal") 20f else 12f
                    
                    // Branch 1 leaves
                    drawCircle(color = leafColor.copy(alpha = 0.9f), radius = foliageRadius, center = Offset(b1EndX, b1EndY))
                    // Branch 2 leaves
                    drawCircle(color = leafColor.copy(alpha = 0.9f), radius = foliageRadius, center = Offset(b2EndX, b2EndY))
                    // Top head leaves
                    drawCircle(color = leafColor.copy(alpha = 0.9f), radius = foliageRadius * 1.2f, center = Offset(trunkEndX, trunkEndY))

                    // --- Render Species Specific Features ---
                    if (isLavender) {
                        // Lavender flower spikes (purple droplets rising from branch tips)
                        val points = listOf(
                            Offset(b1EndX, b1EndY - 10f),
                            Offset(b2EndX, b2EndY - 12f),
                            Offset(trunkEndX, trunkEndY - 15f)
                        )
                        points.forEach { pt ->
                            for (i in 0..2) {
                                drawCircle(
                                    color = flowerColor,
                                    radius = 3.5f,
                                    center = Offset(pt.x, pt.y - i * 6f)
                                )
                            }
                        }
                    } else if (isJuniperOrMaple) {
                        // Soft pink blossoms/maple leaves
                        val points = listOf(
                            Offset(b1EndX - 4f, b1EndY - 4f),
                            Offset(b1EndX + 6f, b1EndY + 4f),
                            Offset(b2EndX - 5f, b2EndY - 3f),
                            Offset(b2EndX + 5f, b2EndY + 5f),
                            Offset(trunkEndX - 8f, trunkEndY - 6f),
                            Offset(trunkEndX + 8f, trunkEndY + 4f)
                        )
                        points.forEach { pt ->
                            drawCircle(color = flowerColor, radius = 5f, center = pt)
                            drawCircle(color = Color.White, radius = 1.5f, center = pt)
                        }
                    } else if (isFern) {
                        // Draw extra frond lines
                        for (i in 1..4) {
                            drawLine(
                                color = leafColor,
                                start = Offset(trunkControlX, baseY - trunkLength * 0.4f),
                                end = Offset(trunkControlX - 18f, baseY - trunkLength * 0.4f - i * 6f),
                                strokeWidth = 1.5f
                            )
                            drawLine(
                                color = leafColor,
                                start = Offset(trunkControlX, baseY - trunkLength * 0.4f),
                                end = Offset(trunkControlX + 18f, baseY - trunkLength * 0.4f - i * 6f),
                                strokeWidth = 1.5f
                            )
                        }
                    } else if (isCactus) {
                        // Yellow spine needles on pads
                        val points = listOf(
                            Offset(b1EndX, b1EndY),
                            Offset(b2EndX, b2EndY),
                            Offset(trunkEndX, trunkEndY)
                        )
                        points.forEach { pt ->
                            // spines
                            drawLine(Color(0xFFFFF9C4), Offset(pt.x - 8f, pt.y), Offset(pt.x - 12f, pt.y), 1f)
                            drawLine(Color(0xFFFFF9C4), Offset(pt.x + 8f, pt.y), Offset(pt.x + 12f, pt.y), 1f)
                            drawLine(Color(0xFFFFF9C4), Offset(pt.x, pt.y - 8f), Offset(pt.x, pt.y - 12f), 1f)
                            // Golden bloom
                            drawCircle(color = flowerColor, radius = 4f, center = Offset(pt.x, pt.y - 10f))
                        }
                    } else {
                        // Generic small yellow flowers
                        drawCircle(color = flowerColor, radius = 4f, center = Offset(trunkEndX - 12f, trunkEndY - 8f))
                        drawCircle(color = flowerColor, radius = 4f, center = Offset(b2EndX + 10f, b2EndY - 6f))
                    }

                } else {
                    // Sprout tiny dual leaves
                    drawCircle(color = leafColor, radius = 6f, center = Offset(trunkEndX - 8f, trunkEndY - 2f))
                    drawCircle(color = leafColor, radius = 6f, center = Offset(trunkEndX + 8f, trunkEndY - 2f))
                }

                // --- Dropping water droplets if overwatered ---
                if (moisture > 0.85f) {
                    val dropY = (climateTimeFactor * 4f) % 40f
                    drawCircle(
                        color = Color(0xFF00E5FF),
                        radius = 2.5f,
                        center = Offset(trunkEndX - 8f, trunkEndY + 10f + dropY)
                    )
                    if (growthStage != "Sprout") {
                        drawCircle(
                            color = Color(0xFF00E5FF),
                            radius = 2.5f,
                            center = Offset(b1EndX + 6f, b1EndY + 10f + dropY)
                        )
                    } else {
                        drawCircle(
                            color = Color(0xFF00E5FF),
                            radius = 2.5f,
                            center = Offset(trunkEndX + 8f, trunkEndY + 10f + dropY)
                        )
                    }
                }
            }
        }
    }
}
