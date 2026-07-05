package com.example.ui.screens.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MoodLog
import com.example.ui.components.MoodPhoto
import com.example.ui.theme.SoilSageDark
import com.example.ui.theme.spacing

@Composable
fun MoodLogItemCard(
    log: MoodLog,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val emoji = when (log.mood) {
        "Peaceful" -> "🧘"
        "Energized" -> "⚡"
        "Refreshed" -> "🍃"
        "Happy" -> "☀️"
        "Anxious" -> "💨"
        "Tired" -> "☁️"
        else -> "🌱"
    }

    val isDark = MaterialTheme.colorScheme.primary == SoilSageDark
    val itemBgColor = if (isDark) {
        when (log.mood) {
            "Peaceful" -> Color(0xFF2C1E30)
            "Energized" -> Color(0xFF332B12)
            "Refreshed" -> Color(0xFF1B2E1E)
            "Happy" -> Color(0xFF302213)
            "Anxious" -> Color(0xFF212529)
            "Tired" -> Color(0xFF1C1D1F)
            else -> MaterialTheme.colorScheme.primaryContainer
        }
    } else {
        when (log.mood) {
            "Peaceful" -> Color(0xFFF3E5F5)
            "Energized" -> Color(0xFFFFFDE7)
            "Refreshed" -> Color(0xFFE8F5E9)
            "Happy" -> Color(0xFFFFF3E0)
            "Anxious" -> Color(0xFFECEFF1)
            "Tired" -> Color(0xFFF5F5F5)
            else -> MaterialTheme.colorScheme.surface
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = itemBgColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MoodPhoto(
                mood = log.mood,
                fallbackEmoji = emoji,
                modifier = Modifier.size(54.dp),
                shape = RoundedCornerShape(12.dp),
                emojiFontSize = 28.sp
            )

            Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${log.mood} Garden Sesh",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(log.moodScore) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "*",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
                ) {
                    Text(
                        text = "⏱️ ${log.activityMinutes} minutes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "🪴 Garden Index: ${log.growthIndex}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                if (log.waterCompleted || log.pruneCompleted || log.outdoorsCompleted) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        if (log.waterCompleted) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF0284C7).copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "💧 Watered",
                                    fontSize = 9.sp,
                                    color = Color(0xFF0284C7),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1, softWrap = false
                                )
                            }
                        }
                        if (log.pruneCompleted) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFF97316).copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "✂️ Pruned",
                                    fontSize = 9.sp,
                                    color = Color(0xFFF97316),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1, softWrap = false
                                )
                            }
                        }
                        if (log.outdoorsCompleted) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "🍃 Outdoors",
                                    fontSize = 9.sp,
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1, softWrap = false
                                )
                            }
                        }
                    }
                }

                if (log.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "\"${log.notes}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Delete Log",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
