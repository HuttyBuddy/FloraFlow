package com.example.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MoodLog
import kotlin.math.abs

@Composable
fun InteractiveTherapyChart(
    logs: List<MoodLog>,
    modifier: Modifier = Modifier
) {
    if (logs.isEmpty()) return

    val primaryColor = MaterialTheme.colorScheme.primary // Forest Green for Garden Growth
    val secondaryColor = MaterialTheme.colorScheme.tertiary // Terracotta/Gold for Wellness
    val grayColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    val syncFillColor = Color(0xFF00FF66)

    var selectedIndex by remember(logs) { mutableStateOf<Int?>(null) }
    
    val pointsWellness = remember(logs) { mutableStateListOf<Offset>() }
    val pointsGrowth = remember(logs) { mutableStateListOf<Offset>() }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(vertical = 8.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(logs) {
                        detectTapGestures { offset ->
                            if (pointsWellness.isNotEmpty()) {
                                var bestIndex = 0
                                var minDist = Float.MAX_VALUE
                                for (i in pointsWellness.indices) {
                                    val dist = abs(pointsWellness[i].x - offset.x)
                                    if (dist < minDist) {
                                        minDist = dist
                                        bestIndex = i
                                    }
                                }
                                if (minDist < 50f) {
                                    selectedIndex = if (selectedIndex == bestIndex) null else bestIndex
                                } else {
                                    selectedIndex = null
                                }
                            }
                        }
                    }
            ) {
                val width = size.width
                val height = size.height

                val paddingX = 45f
                val paddingY = 30f

                val chartWidth = width - (paddingX * 2)
                val chartHeight = height - (paddingY * 2)

                // Grid background guidelines
                for (i in 0..4) {
                    val y = paddingY + (chartHeight * i / 4f)
                    drawLine(
                        color = grayColor,
                        start = Offset(paddingX, y),
                        end = Offset(width - paddingX, y),
                        strokeWidth = 1.5f
                    )
                }

                val count = logs.size

                // Map points
                pointsWellness.clear()
                pointsGrowth.clear()

                for (idx in logs.indices) {
                    val x = paddingX + (chartWidth * idx / (count - 1).coerceAtLeast(1))
                    
                    // 1. Wellness / Mood Rating mapping (1..5 -> 0f..1f)
                    val wPercent = (logs[idx].moodScore.toFloat() - 1f) / 4f
                    val wY = paddingY + chartHeight - (chartHeight * wPercent)
                    pointsWellness.add(Offset(x, wY))

                    // 2. Garden Growth Index mapping (0..100 -> 0f..1f)
                    val gPercent = logs[idx].growthIndex.toFloat() / 100f
                    val gY = paddingY + chartHeight - (chartHeight * gPercent)
                    pointsGrowth.add(Offset(x, gY))
                }

                // 1. Draw "Botanical Sync" Gradient between the curves
                val pathSync = Path().apply {
                    if (pointsWellness.isNotEmpty() && pointsGrowth.isNotEmpty()) {
                        moveTo(pointsWellness[0].x, pointsWellness[0].y)
                        // Trace Wellness left-to-right (Smooth S-curve Bezier)
                        for (i in 0 until pointsWellness.size - 1) {
                            val pStart = pointsWellness[i]
                            val pEnd = pointsWellness[i + 1]
                            val cp1 = Offset((pStart.x + pEnd.x) / 2f, pStart.y)
                            val cp2 = Offset((pStart.x + pEnd.x) / 2f, pEnd.y)
                            cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, pEnd.x, pEnd.y)
                        }
                        // Line to end of Growth
                        lineTo(pointsGrowth.last().x, pointsGrowth.last().y)
                        // Trace Growth right-to-left
                        for (i in pointsGrowth.size - 1 downTo 1) {
                            val pStart = pointsGrowth[i]
                            val pEnd = pointsGrowth[i - 1]
                            val cp1 = Offset((pStart.x + pEnd.x) / 2f, pStart.y)
                            val cp2 = Offset((pStart.x + pEnd.x) / 2f, pEnd.y)
                            cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, pEnd.x, pEnd.y)
                        }
                        close()
                    }
                }
                drawPath(
                    path = pathSync,
                    brush = Brush.verticalGradient(
                        colors = listOf(syncFillColor.copy(alpha = 0.22f), Color.Transparent),
                        startY = paddingY,
                        endY = paddingY + chartHeight
                    )
                )

                // Draw Wellness Area Gradient
                val pathWellnessArea = Path().apply {
                    if (pointsWellness.isNotEmpty()) {
                        moveTo(pointsWellness[0].x, paddingY + chartHeight)
                        lineTo(pointsWellness[0].x, pointsWellness[0].y)
                        for (i in 0 until pointsWellness.size - 1) {
                            val pStart = pointsWellness[i]
                            val pEnd = pointsWellness[i + 1]
                            val cp1 = Offset((pStart.x + pEnd.x) / 2f, pStart.y)
                            val cp2 = Offset((pStart.x + pEnd.x) / 2f, pEnd.y)
                            cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, pEnd.x, pEnd.y)
                        }
                        lineTo(pointsWellness.last().x, paddingY + chartHeight)
                        close()
                    }
                }
                drawPath(
                    path = pathWellnessArea,
                    brush = Brush.verticalGradient(
                        colors = listOf(secondaryColor.copy(alpha = 0.15f), Color.Transparent),
                        startY = paddingY,
                        endY = paddingY + chartHeight
                    )
                )

                // Draw Garden Growth Area Gradient
                val pathGrowthArea = Path().apply {
                    if (pointsGrowth.isNotEmpty()) {
                        moveTo(pointsGrowth[0].x, paddingY + chartHeight)
                        lineTo(pointsGrowth[0].x, pointsGrowth[0].y)
                        for (i in 0 until pointsGrowth.size - 1) {
                            val pStart = pointsGrowth[i]
                            val pEnd = pointsGrowth[i + 1]
                            val cp1 = Offset((pStart.x + pEnd.x) / 2f, pStart.y)
                            val cp2 = Offset((pStart.x + pEnd.x) / 2f, pEnd.y)
                            cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, pEnd.x, pEnd.y)
                        }
                        lineTo(pointsGrowth.last().x, paddingY + chartHeight)
                        close()
                    }
                }
                drawPath(
                    path = pathGrowthArea,
                    brush = Brush.verticalGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.15f), Color.Transparent),
                        startY = paddingY,
                        endY = paddingY + chartHeight
                    )
                )

                // 2. Draw Wellness Curve (Cubic S-curves) with soft Glow
                val pathWellness = Path().apply {
                    if (pointsWellness.isNotEmpty()) {
                        moveTo(pointsWellness[0].x, pointsWellness[0].y)
                        for (i in 0 until pointsWellness.size - 1) {
                            val pStart = pointsWellness[i]
                            val pEnd = pointsWellness[i + 1]
                            val cp1 = Offset((pStart.x + pEnd.x) / 2f, pStart.y)
                            val cp2 = Offset((pStart.x + pEnd.x) / 2f, pEnd.y)
                            cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, pEnd.x, pEnd.y)
                        }
                    }
                }
                drawPath(
                    path = pathWellness,
                    color = secondaryColor.copy(alpha = 0.25f),
                    style = Stroke(width = 12f, cap = StrokeCap.Round)
                )
                drawPath(
                    path = pathWellness,
                    color = secondaryColor,
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )

                // 3. Draw Garden Growth Curve (Cubic S-curves) with soft Glow
                val pathGrowth = Path().apply {
                    if (pointsGrowth.isNotEmpty()) {
                        moveTo(pointsGrowth[0].x, pointsGrowth[0].y)
                        for (i in 0 until pointsGrowth.size - 1) {
                            val pStart = pointsGrowth[i]
                            val pEnd = pointsGrowth[i + 1]
                            val cp1 = Offset((pStart.x + pEnd.x) / 2f, pStart.y)
                            val cp2 = Offset((pStart.x + pEnd.x) / 2f, pEnd.y)
                            cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, pEnd.x, pEnd.y)
                        }
                    }
                }
                drawPath(
                    path = pathGrowth,
                    color = primaryColor.copy(alpha = 0.25f),
                    style = Stroke(width = 12f, cap = StrokeCap.Round)
                )
                drawPath(
                    path = pathGrowth,
                    color = primaryColor,
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )

                // 4. Draw Highlight selection vertical line
                selectedIndex?.let { idx ->
                    val selX = pointsWellness[idx].x
                    drawLine(
                        color = Color(0xFF00E5FF).copy(alpha = 0.8f),
                        start = Offset(selX, paddingY),
                        end = Offset(selX, paddingY + chartHeight),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }

                // 5. Draw interactive nodes/dots
                for (idx in pointsWellness.indices) {
                    val isSelected = selectedIndex == idx
                    val selectRadiusOffset = if (isSelected) 5f else 0f

                    // Wellness node (Gold/Terracotta)
                    drawCircle(color = secondaryColor.copy(alpha = 0.25f), radius = 15f + selectRadiusOffset, center = pointsWellness[idx])
                    drawCircle(color = secondaryColor, radius = 8f + selectRadiusOffset * 0.5f, center = pointsWellness[idx])
                    drawCircle(color = Color.White, radius = 3.5f, center = pointsWellness[idx])

                    // Growth node (Forest Green)
                    drawCircle(color = primaryColor.copy(alpha = 0.25f), radius = 15f + selectRadiusOffset, center = pointsGrowth[idx])
                    drawCircle(color = primaryColor, radius = 8f + selectRadiusOffset * 0.5f, center = pointsGrowth[idx])
                    drawCircle(color = Color.White, radius = 3.5f, center = pointsGrowth[idx])
                }
            }
        }

        // Legend indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(secondaryColor))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "Therapy Rating", 
                style = MaterialTheme.typography.bodySmall, 
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 16.dp)
            )

            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(primaryColor))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "Garden Growth Index", 
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }

        // Interactive Tooltip Card
        AnimatedVisibility(
            visible = selectedIndex != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            selectedIndex?.let { idx ->
                val log = logs[idx]
                val syncPercentage = (100 - abs(((log.moodScore - 1f) / 4f * 100) - log.growthIndex)).toInt().coerceIn(0, 100)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Session ${idx + 1} Analytics",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF00FF66).copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "🍀 Sync: $syncPercentage%",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00FF66)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.SelfImprovement, contentDescription = "Mood", tint = secondaryColor, modifier = Modifier.size(14.dp))
                                Text(
                                    text = "Rating: ${log.moodScore}/5 (${log.mood})",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Eco, contentDescription = "Growth", tint = primaryColor, modifier = Modifier.size(14.dp))
                                Text(
                                    text = "Growth: ${log.growthIndex}%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Info, contentDescription = "Time", tint = Color.Gray, modifier = Modifier.size(14.dp))
                                Text(
                                    text = "Duration: ${log.activityMinutes}m",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (log.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "\"${log.notes}\"",
                                fontSize = 10.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}
