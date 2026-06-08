package com.example.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogMoodDialog(
    onDismiss: () -> Unit,
    onLog: (mood: String, score: Int, duration: Int, notes: String) -> Unit
) {
    var mood by remember { mutableStateOf("Peaceful") }
    var moodScore by remember { mutableStateOf(5) }
    var activityMinutes by remember { mutableStateOf("30") }
    var notes by remember { mutableStateOf("") }

    val moodEmoList = listOf(
        "Peaceful" to "🧘", 
        "Refreshed" to "🍃", 
        "Energized" to "⚡", 
        "Happy" to "☀️", 
        "Anxious" to "💨"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Record Outdoor Session",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Select Mood
                Text(
                    "How do you feel after gardening?", 
                    fontWeight = FontWeight.Bold, 
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    moodEmoList.forEach { pair ->
                        val isSelected = mood == pair.first
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable { mood = pair.first },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(pair.second, fontSize = 18.sp)
                                Text(
                                    pair.first,
                                    fontSize = 8.sp,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Mood Rating (1 to 5 Stars scale)
                Text(
                    "Self Wellness Level: $moodScore / 5", 
                    fontWeight = FontWeight.Bold, 
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (score in 1..5) {
                        val isPicked = score <= moodScore
                        IconButton(
                            onClick = { moodScore = score },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isPicked) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Star $score",
                                tint = if (isPicked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                // Session Duration
                Text(
                    "Gardening Duration", 
                    fontWeight = FontWeight.Bold, 
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = activityMinutes,
                    onValueChange = { activityMinutes = it.filter { char -> char.isDigit() } },
                    label = { Text("Minutes Spent") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("30") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("add_log_minutes")
                )

                // Notes
                Text(
                    "Notes & Reflection", 
                    fontWeight = FontWeight.Bold, 
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Diary Entry (Optional)") },
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("Watered lavender and weeded zen space. Felt grounded.") },
                    modifier = Modifier.fillMaxWidth().testTag("add_log_notes"),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val duration = activityMinutes.toIntOrNull() ?: 30
                            onLog(mood, moodScore, duration, notes)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("submit_log_mood")
                    ) {
                        Text("Save Log")
                    }
                }
            }
        }
    }
}
