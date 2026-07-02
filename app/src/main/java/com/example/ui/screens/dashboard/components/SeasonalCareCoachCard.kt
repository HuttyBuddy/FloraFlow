package com.example.ui.screens.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Plant
import com.example.ui.theme.spacing
import com.example.ui.components.FloraFlowCard

@Composable
fun SeasonalCareCoachCard(
    activePlants: List<Plant>,
    modifier: Modifier = Modifier
) {
    val cal = java.util.Calendar.getInstance()
    val month = cal.get(java.util.Calendar.MONTH)
    val currentSeason = when (month) {
        2, 3, 4 -> "Spring"
        5, 6, 7 -> "Summer"
        8, 9, 10 -> "Autumn"
        else -> "Winter"
    }
    
    val seasonEmoji = when (currentSeason) {
        "Spring" -> "🌸"
        "Summer" -> "☀️"
        "Autumn" -> "🍁"
        else -> "❄️"
    }
    
    val plantToCoach = activePlants.firstOrNull()
    
    FloraFlowCard(
        modifier = modifier.fillMaxWidth().testTag("seasonal_coach_card"),
        containerColor = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.5.dp,
            Brush.horizontalGradient(
                listOf(
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f),
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                )
            )
        ),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(seasonEmoji, fontSize = 24.sp)
                Text(
                    text = "$currentSeason Care Coach",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (plantToCoach != null) {
                val careTip = when (currentSeason) {
                    "Spring" -> plantToCoach.careSpring
                    "Summer" -> plantToCoach.careSummer
                    "Autumn" -> plantToCoach.careAutumn
                    else -> plantToCoach.careWinter
                }
                Text(
                    text = "Tip for your ${plantToCoach.name}:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = careTip,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "No plants sowed yet. Start sowing seeds in your planner grid to receive personalized $currentSeason care coaching!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
