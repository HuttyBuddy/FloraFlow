package com.example.ui.screens.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MoodLog
import com.example.ui.theme.spacing
import androidx.compose.ui.text.intl.Locale as ComposeLocale

@Composable
fun MonthlyWellnessDigestCard(
    moodLogs: List<MoodLog>,
    modifier: Modifier = Modifier
) {
    val currentLocale = ComposeLocale.current.platformLocale
    val calendar = java.util.Calendar.getInstance()
    val currentMonthName = java.text.SimpleDateFormat("MMMM", currentLocale).format(calendar.time)
    
    val currentMonthLogs = remember(moodLogs) {
        moodLogs.filter { log ->
            val logCal = java.util.Calendar.getInstance().apply { timeInMillis = log.timestamp }
            logCal.get(java.util.Calendar.MONTH) == calendar.get(java.util.Calendar.MONTH) &&
                    logCal.get(java.util.Calendar.YEAR) == calendar.get(java.util.Calendar.YEAR)
        }
    }
    
    val totalGardened = currentMonthLogs.size
    val peacefulCount = currentMonthLogs.count { it.mood == "Peaceful" }
    
    // Sort logs chronologically to get start/end wellness bloom
    val sortedLogs = remember(currentMonthLogs) { currentMonthLogs.sortedBy { it.timestamp } }
    val half = sortedLogs.size / 2
    val startAvg = if (half > 0) sortedLogs.take(half).map { it.moodScore }.average() else 3.0
    val endAvg = if (half > 0) sortedLogs.drop(half).map { it.moodScore }.average() else (if (currentMonthLogs.isNotEmpty()) currentMonthLogs.map { it.moodScore }.average() else 3.0)
    
    val overallAvg = if (moodLogs.isNotEmpty()) moodLogs.map { it.moodScore }.average() else 0.0
    val calmDeltaPercent = if (overallAvg > 3.0) {
        (((overallAvg - 3.0) / 3.0) * 100).toInt()
    } else {
        0
    }
    
    Card(
        modifier = modifier.fillMaxWidth().testTag("monthly_wellness_digest_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.5.dp,
            Brush.horizontalGradient(
                listOf(
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Book,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                Text(
                    text = "Your $currentMonthName Garden Journal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (totalGardened == 0) {
                Text(
                    text = "Log your garden sessions today to begin tracking your monthly wellness digest and correlation insights! 🌿",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            } else {
                Text(
                    text = "You gardened $totalGardened times, felt Peaceful $peacefulCount times, and your wellness bloom average went from ${String.format(currentLocale, "%.1f", startAvg)} to ${String.format(currentLocale, "%.1f", endAvg)}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Medium
                )
                
                if (calmDeltaPercent > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("✨", fontSize = 16.sp)
                        Text(
                            text = "You felt $calmDeltaPercent% calmer on days you spent time in the garden.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
