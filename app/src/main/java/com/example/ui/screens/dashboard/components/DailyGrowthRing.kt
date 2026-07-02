package com.example.ui.screens.dashboard.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DailyGrowthRing(
    completedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    val completionRatio = if (totalCount > 0) completedCount.toFloat() / totalCount else 0.0f
    
    // Animated sweep angle
    val animatedProgress by animateFloatAsState(
        targetValue = completionRatio,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "GrowthRingProgress"
    )

    val isFull = completionRatio >= 1.0f
    val emojiScale by animateFloatAsState(
        targetValue = if (isFull) 1.25f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "EmojiScale"
    )

    Box(
        modifier = modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)

            // Draw base track (earthy green background)
            drawCircle(
                color = Color(0xFFE2ECE9),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // Draw active progress (vibrant botanical green/gold sweep gradient)
            drawArc(
                brush = Brush.sweepGradient(
                    colors = if (isFull) {
                        listOf(Color(0xFF81C784), Color(0xFF4CAF50), Color(0xFFFFD54F), Color(0xFF81C784))
                    } else {
                        listOf(Color(0xFF81C784), Color(0xFF4CAF50), Color(0xFF2E7D32), Color(0xFF81C784))
                    }
                ),
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Draw small flower emoji or percentage inside
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(emojiScale)
        ) {
            Text(
                text = if (isFull) "🌸" else "🌱",
                fontSize = 20.sp
            )
            Text(
                text = "${(completionRatio * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isFull) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                fontSize = 10.sp
            )
        }
    }
}
