package com.example.ui.screens.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.GardenViewModel
import com.example.ui.theme.spacing
import com.example.ui.components.FloraFlowCard
import com.example.ui.components.FloraFlowButton
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DailyHabitCard(
    viewModel: GardenViewModel,
    modifier: Modifier = Modifier
) {
    val pendingTasks by viewModel.pendingCareTasks.collectAsStateWithLifecycle()
    val allTasks by viewModel.allCareTasks.collectAsStateWithLifecycle()
    val moodLogs by viewModel.allMoodLogs.collectAsStateWithLifecycle()
    val lowestCategories by viewModel.lowestCategories.collectAsStateWithLifecycle()

    val completedTasksToday = remember(allTasks) {
        allTasks.count { it.completedDate != null && isToday(it.completedDate) }
    }
    val pendingTasksToday = remember(pendingTasks) {
        pendingTasks.count { it.dueDate <= System.currentTimeMillis() }
    }
    val totalTasksToday = completedTasksToday + pendingTasksToday

    // Dynamic sensory rituals based on lowest assessment categories
    val activeRituals = remember(lowestCategories) {
        if (lowestCategories.isEmpty()) {
            listOf(SensoryRitual("GENERAL", "Take 5 deep breaths in your sanctuary", "[Ritual: General Deep Breaths]"))
        } else {
            lowestCategories.take(2).map { getRitualForCategory(it) }
        }
    }

    val completedRitualMarkers = remember(moodLogs) {
        moodLogs.asSequence()
            .filter { isToday(it.timestamp) }
            .map { it.notes }
            .toSet()
    }

    val completedRitualsCount = activeRituals.count { completedRitualMarkers.contains(it.noteMarker) }
    val totalRitualsCount = activeRituals.size

    val totalCompletedRing = completedTasksToday + completedRitualsCount
    val totalAllRing = totalTasksToday + totalRitualsCount

    // Calculate streak
    val streakCount = remember(moodLogs) {
        calculateStreak(moodLogs)
    }

    FloraFlowCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.5.dp,
            Brush.horizontalGradient(
                listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                )
            )
        ),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.mediumSmall)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Your Daily Growth Ring",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Nurture your plants and mind",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Streak badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("🔥", fontSize = 14.sp)
                    Text(
                        text = "$streakCount Day Streak",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Growth ring
                DailyGrowthRing(
                    completedCount = totalCompletedRing,
                    totalCount = totalAllRing,
                    modifier = Modifier.padding(MaterialTheme.spacing.small)
                )

                // Care checklist & Eco-Rituals column
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (pendingTasksToday == 0 && completedRitualsCount == totalRitualsCount) {
                        Text(
                            text = "✨ All tasks and rituals complete! Your garden is thriving.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        val hasGratitudeToday = remember(moodLogs) {
                            moodLogs.any { isToday(it.timestamp) && it.mood == "Peaceful" && it.notes.isNotEmpty() && !it.notes.startsWith("[Ritual:") }
                        }
                        if (totalAllRing > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            if (hasGratitudeToday) {
                                Text(
                                    text = "🍃 Thank you for saving a moment of gratitude today.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            } else {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "Name one thing in your garden that brought you peace today:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    var gratitudeText by remember { mutableStateOf("") }
                                    OutlinedTextField(
                                        value = gratitudeText,
                                        onValueChange = { gratitudeText = it },
                                        placeholder = { Text("e.g. The scent of lavender blossoms...", fontSize = 11.sp) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                            .testTag("gratitude_input"),
                                        singleLine = true,
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    )
                                    Button(
                                        onClick = {
                                            if (gratitudeText.isNotBlank()) {
                                                viewModel.logMood(mood = "Peaceful", score = 5, duration = 5, notes = gratitudeText.trim())
                                            }
                                        },
                                        enabled = gratitudeText.isNotBlank(),
                                        modifier = Modifier
                                            .align(Alignment.End)
                                            .height(32.dp)
                                            .testTag("save_gratitude_button"),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Save Reflection", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        // Section 1: Care Tasks
                        if (pendingTasksToday > 0 || completedTasksToday > 0) {
                            Text(
                                text = "Plant Care Tasks:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            val completedTodayList = remember(allTasks) {
                                allTasks.filter { it.completedDate != null && isToday(it.completedDate) }
                            }
                            val pendingTodayList = remember(pendingTasks) {
                                pendingTasks.filter { it.dueDate <= System.currentTimeMillis() }
                            }
                            val combinedTasks = remember(pendingTodayList, completedTodayList) {
                                (pendingTodayList + completedTodayList).take(3)
                            }

                            combinedTasks.forEach { task ->
                                val isCompleted = task.completedDate != null
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 1.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isCompleted) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Completed",
                                            tint = Color(0xFF4CAF50),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "${task.taskType} ${task.plantName}",
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                textDecoration = TextDecoration.LineThrough
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            maxLines = 2
                                        )
                                    } else {
                                        var checked by remember { mutableStateOf(false) }
                                        val haptic = LocalHapticFeedback.current
                                        val coroutineScope = rememberCoroutineScope()
                                        
                                        Checkbox(
                                            checked = checked,
                                            onCheckedChange = { isChecked ->
                                                if (isChecked) {
                                                    checked = true
                                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.Confirm)
                                                    coroutineScope.launch {
                                                        delay(300)
                                                        viewModel.completeCareTask(task)
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = "${task.taskType} ${task.plantName}",
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // Section 2: Biophilic Sensory Rituals
                        Text(
                            text = "Daily Biophilic Rituals:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        activeRituals.forEach { ritual ->
                            val isCompleted = completedRitualMarkers.contains(ritual.noteMarker)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isCompleted) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Completed",
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = ritual.text,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            textDecoration = TextDecoration.LineThrough
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        maxLines = 2
                                    )
                                } else {
                                    var checked by remember { mutableStateOf(false) }
                                    val haptic = LocalHapticFeedback.current
                                    val coroutineScope = rememberCoroutineScope()
                                    
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = { isChecked ->
                                            if (isChecked) {
                                                checked = true
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.Confirm)
                                                coroutineScope.launch {
                                                    delay(300)
                                                    viewModel.logMood(
                                                        mood = "Peaceful",
                                                        score = 5,
                                                        duration = 3,
                                                        notes = ritual.noteMarker
                                                    )
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(24.dp).testTag("ritual_checkbox_${ritual.category}")
                                    )
                                    Text(
                                        text = ritual.text,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
