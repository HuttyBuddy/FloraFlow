package com.example.ui.screens.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MoodLog
import com.example.ui.components.MoodImages
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt

@Composable
fun CircularBotanicalRhythm(
    todayLog: MoodLog?,
    onToggleHabit: (String) -> Unit,
    onLogMoodClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Habit states
    val isWatered = todayLog?.waterCompleted == true
    val isPruned = todayLog?.pruneCompleted == true
    val isOutdoors = todayLog?.outdoorsCompleted == true

    // Animations for sweep angles (0° to 110°)
    val waterSweep by animateFloatAsState(
        targetValue = if (isWatered) 110f else 0f,
        animationSpec = tween(durationMillis = 500)
    )
    val pruneSweep by animateFloatAsState(
        targetValue = if (isPruned) 110f else 0f,
        animationSpec = tween(durationMillis = 500)
    )
    val outdoorsSweep by animateFloatAsState(
        targetValue = if (isOutdoors) 110f else 0f,
        animationSpec = tween(durationMillis = 500)
    )

    // Layout configuration
    val strokeWidthDp = 16.dp
    val spacingDp = 12.dp

    // Harmonic colors
    val waterColor = Color(0xFF0284C7)      // Vibrant sky blue
    val pruneColor = Color(0xFFF97316)      // Warm sunset orange
    val outdoorsColor = Color(0xFF10B981)   // Radiant spring emerald
    val trackBgColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    // Inner mood bubble gradient definition
    val moodEmoji = when (todayLog?.mood) {
        "Peaceful" -> "🧘"
        "Energized" -> "⚡"
        "Refreshed" -> "🍃"
        "Happy" -> "☀️"
        "Anxious" -> "💨"
        "Tired" -> "☁️"
        else -> if (todayLog != null) "🌱" else "➕"
    }

    val moodLabel = todayLog?.mood ?: "Tap to Log"

    val innerColors = when (todayLog?.mood) {
        "Peaceful" -> listOf(Color(0xFF818CF8), Color(0xFFC084FC))
        "Energized" -> listOf(Color(0xFFFBBF24), Color(0xFFF87171))
        "Refreshed" -> listOf(Color(0xFF34D399), Color(0xFF60A5FA))
        "Happy" -> listOf(Color(0xFFF472B6), Color(0xFFFB7185))
        "Anxious" -> listOf(Color(0xFF9CA3AF), Color(0xFF4B5563))
        "Tired" -> listOf(Color(0xFFE5E7EB), Color(0xFF9CA3AF))
        else -> if (todayLog != null) {
            listOf(Color(0xFF34D399), Color(0xFF059669))
        } else {
            listOf(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(todayLog, isWatered, isPruned, isOutdoors) {
                        detectTapGestures { offset ->
                            val width = size.width
                            val height = size.height
                            val centerX = width / 2f
                            val centerY = height / 2f

                            val dx = offset.x - centerX
                            val dy = offset.y - centerY
                            val dist = sqrt((dx * dx) + (dy * dy))

                            val strokeWidthPx = strokeWidthDp.toPx()
                            val spacingPx = spacingDp.toPx()
                            
                            val outerRadius = min(width, height) / 2f - strokeWidthPx / 2f
                            val innerRadius = outerRadius - strokeWidthPx - spacingPx

                            // Tapped inside the center wellness circle
                            if (dist < innerRadius) {
                                onLogMoodClick()
                            }
                            // Tapped inside the outer habit rings
                            else if (dist >= innerRadius - spacingPx && dist <= outerRadius + strokeWidthPx) {
                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                if (angle < 0) angle += 360f

                                // Check which segment (arc + gap range)
                                when (angle) {
                                    in 0f..110f -> onToggleHabit("water")
                                    in 120f..230f -> onToggleHabit("prune")
                                    in 240f..350f -> onToggleHabit("outdoors")
                                }
                            }
                        }
                    }
            ) {
                val width = size.width
                val height = size.height
                val centerX = width / 2f
                val centerY = height / 2f

                val strokeWidthPx = strokeWidthDp.toPx()
                val radius = min(width, height) / 2f - strokeWidthPx / 2f

                val arcSize = Size(radius * 2, radius * 2)
                val topLeft = Offset(centerX - radius, centerY - radius)

                // 1. Draw background arcs for the habits
                // Watering arc
                drawArc(
                    color = trackBgColor,
                    startAngle = 0f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )

                // Pruning arc
                drawArc(
                    color = trackBgColor,
                    startAngle = 120f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )

                // Outdoors arc
                drawArc(
                    color = trackBgColor,
                    startAngle = 240f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )

                // 2. Draw progress arcs
                if (waterSweep > 0f) {
                    drawArc(
                        color = waterColor,
                        startAngle = 0f,
                        sweepAngle = waterSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                    )
                }

                if (pruneSweep > 0f) {
                    drawArc(
                        color = pruneColor,
                        startAngle = 120f,
                        sweepAngle = pruneSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                    )
                }

                if (outdoorsSweep > 0f) {
                    drawArc(
                        color = outdoorsColor,
                        startAngle = 240f,
                        sweepAngle = outdoorsSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                    )
                }
            }

            // 3. Central Wellness Bubble (Mood Circle)
            val innerSize = 130.dp
            val moodPhotoRes = MoodImages.forMood(todayLog?.mood)
            Box(
                modifier = Modifier
                    .size(innerSize)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(innerColors))
                    .pointerInput(Unit) {
                        detectTapGestures {
                            onLogMoodClick()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (moodPhotoRes != null) {
                    Image(
                        painter = painterResource(id = moodPhotoRes),
                        contentDescription = "$moodLabel mood photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                    // Scrim so the mood label stays legible over any photo
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))))
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (moodPhotoRes == null) {
                        Text(
                            text = moodEmoji,
                            fontSize = 32.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = moodLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (todayLog != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (todayLog != null) "Mood Score: ${todayLog.moodScore}/5" else "Record Today",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (todayLog != null) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.secondary,
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Interactivity Instructions / Habit Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HabitLegendCard(
                icon = Icons.Default.WaterDrop,
                title = "Watering",
                isChecked = isWatered,
                activeColor = waterColor,
                modifier = Modifier.weight(1f),
                onClick = { onToggleHabit("water") }
            )
            HabitLegendCard(
                icon = Icons.Default.Eco,
                title = "Pruning",
                isChecked = isPruned,
                activeColor = pruneColor,
                modifier = Modifier.weight(1f),
                onClick = { onToggleHabit("prune") }
            )
            HabitLegendCard(
                icon = Icons.Default.SelfImprovement,
                title = "Outdoors",
                isChecked = isOutdoors,
                activeColor = outdoorsColor,
                modifier = Modifier.weight(1f),
                onClick = { onToggleHabit("outdoors") }
            )
        }
    }
}

@Composable
private fun HabitLegendCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    isChecked: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isChecked) activeColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = if (isChecked) {
            androidx.compose.foundation.BorderStroke(1.2.dp, activeColor)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        }
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isChecked) activeColor else MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isChecked) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (isChecked) "Done" else "Tap to check",
                fontSize = 9.sp,
                color = if (isChecked) activeColor.copy(alpha = 0.8f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
            )
        }
    }
}
