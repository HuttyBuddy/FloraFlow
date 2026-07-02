package com.example.ui.screens.dashboard.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AssessmentResult

@Composable
fun ScoreHistoryChart(
    assessmentHistory: List<AssessmentResult>,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFE5E5E5)
    val dotColor = MaterialTheme.colorScheme.secondary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    // Reverse history to display chronologically from left to right
    val chronologicalHistory = remember(assessmentHistory) {
        assessmentHistory.reversed()
    }

    if (chronologicalHistory.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Take your first assessment to start tracking your progress.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    if (chronologicalHistory.size == 1) {
        val result = chronologicalHistory.first()
        Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(horizontal = 8.dp)
            ) {
                val width = size.width
                val height = size.height
                val maxScore = 20f

                // Draw Y-axis grids (5, 10, 15, 20)
                val gridStepY = height / 4
                for (i in 0..4) {
                    val y = height - (i * gridStepY)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Draw single point in the center
                val x = width / 2
                val normalizedScore = result.score.coerceIn(0, 20).toFloat()
                val y = height - (normalizedScore / maxScore * height)

                drawCircle(
                    color = dotColor,
                    radius = 6.dp.toPx(),
                    center = Offset(x, y)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.5.dp.toPx(),
                    center = Offset(x, y)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Initial Audit: ${result.score}/20",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Complete future audits to visualize your biophilic trend line.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(horizontal = 8.dp)
        ) {
            val width = size.width
            val height = size.height
            val pointsCount = chronologicalHistory.size
            val stepX = width / (pointsCount - 1)
            val maxScore = 20f

            // Draw Y-axis grids (5, 10, 15, 20)
            val gridStepY = height / 4
            for (i in 0..4) {
                val y = height - (i * gridStepY)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Draw trend line and points
            val path = Path()
            chronologicalHistory.forEachIndexed { index, result ->
                val x = index * stepX
                val normalizedScore = result.score.coerceIn(0, 20).toFloat()
                val y = height - (normalizedScore / maxScore * height)

                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw point dots
            chronologicalHistory.forEachIndexed { index, result ->
                val x = index * stepX
                val normalizedScore = result.score.coerceIn(0, 20).toFloat()
                val y = height - (normalizedScore / maxScore * height)

                drawCircle(
                    color = dotColor,
                    radius = 5.dp.toPx(),
                    center = Offset(x, y)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.5.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        // Draw date labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val sdf = java.text.SimpleDateFormat("MMM dd", java.util.Locale.US)
            Text(
                text = sdf.format(java.util.Date(chronologicalHistory.first().timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = labelColor,
                fontSize = 10.sp
            )
            if (chronologicalHistory.size > 2) {
                Text(
                    text = "Sanctuary Audit Trendline",
                    style = MaterialTheme.typography.bodySmall,
                    color = labelColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
            Text(
                text = sdf.format(java.util.Date(chronologicalHistory.last().timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = labelColor,
                fontSize = 10.sp
            )
        }
    }
}
