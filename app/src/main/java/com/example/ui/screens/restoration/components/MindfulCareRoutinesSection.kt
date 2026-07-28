package com.example.ui.screens.restoration.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

data class CareRoutine(
    val id: String,
    val title: String,
    val durationSeconds: Int,
    val description: String,
    val instructionSteps: List<String>,
    val soundscapeName: String
)

val DEFAULT_ROUTINES = listOf(
    CareRoutine(
        id = "leaf_wiping",
        title = "Leaf Wiping & Deep Breathwork",
        durationSeconds = 120,
        description = "Clean dust from leaves slowly while syncing your breathing with binaural Theta waves.",
        instructionSteps = listOf(
            "Hold a soft damp cloth and gently support the underside of a leaf.",
            "Inhale deeply for 4s as you wipe from stem to leaf tip.",
            "Exhale for 4s releasing tension while admiring the natural green shine."
        ),
        soundscapeName = "Gentle Rain 🌧️"
    ),
    CareRoutine(
        id = "soil_grounding",
        title = "Soil Check & Mindful Grounding",
        durationSeconds = 90,
        description = "Feel the moisture and soil texture to ground yourself in physical nature.",
        instructionSteps = listOf(
            "Press your fingertip 1 inch into the topsoil of your houseplant.",
            "Notice the temperature, dampness, and organic scent of the potting soil.",
            "Take 3 slow breaths, letting your thoughts quiet down."
        ),
        soundscapeName = "Pine Mountain Canopy 🌲"
    ),
    CareRoutine(
        id = "misting_gratitude",
        title = "Foliage Misting & Gratitude",
        durationSeconds = 180,
        description = "Spray a fine mist over humidity-loving plants while practicing silent gratitude.",
        instructionSteps = listOf(
            "Hold the mister 6 inches from the plant's leaves.",
            "Squeeze the spray pump gently, observing water droplets forming on foliage.",
            "Mentally thank your indoor sanctuary for purifying your room air."
        ),
        soundscapeName = "Bamboo Wind & Stream 🎋"
    )
)

@Composable
fun MindfulCareRoutinesSection(
    onCompleteRoutine: (title: String, durationSeconds: Int, startMood: Int, endMood: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeRoutine by remember { mutableStateOf<CareRoutine?>(null) }
    var routineRunning by remember { mutableStateOf(false) }
    var secondsLeft by remember { mutableIntStateOf(0) }
    var currentStepIndex by remember { mutableIntStateOf(0) }
    var startingMood by remember { mutableIntStateOf(3) }
    var endingMood by remember { mutableIntStateOf(5) }
    var showCompletionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(routineRunning, secondsLeft) {
        if (routineRunning && secondsLeft > 0) {
            delay(1000L)
            secondsLeft -= 1
        } else if (routineRunning && secondsLeft <= 0) {
            routineRunning = false
            showCompletionDialog = true
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Guided Mindful Care Routines",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFA8E6CF)
        )
        Text(
            text = "Combine routine plant maintenance with restorative breathwork & binaural audio",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (activeRoutine == null) {
            // Routine selection cards
            DEFAULT_ROUTINES.forEach { routine ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            activeRoutine = routine
                            secondsLeft = routine.durationSeconds
                            currentStepIndex = 0
                            routineRunning = true
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Spa,
                                contentDescription = null,
                                tint = Color(0xFFA8E6CF)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = routine.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = routine.description,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = Color(0xFFFFB74D),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${routine.durationSeconds / 60}m",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB74D)
                            )
                        }
                    }
                }
            }
        } else {
            // Active routine player view
            val routine = activeRoutine!!
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                border = BorderStroke(1.5.dp, Color(0xFFA8E6CF).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = routine.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA8E6CF)
                        )
                        Text(
                            text = "${secondsLeft / 60}:${(secondsLeft % 60).toString().padStart(2, '0')}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB74D)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Animated Breathwork Visualizer Circle
                    val infiniteTransition = rememberInfiniteTransition(label = "BreathworkTransition")
                    val breathScale by infiniteTransition.animateFloat(
                        initialValue = 0.7f,
                        targetValue = 1.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 4000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "breathScale"
                    )

                    Box(
                        modifier = Modifier
                            .size((90 * breathScale).dp)
                            .clip(CircleShape)
                            .background(Color(0x331F483E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Spa,
                            contentDescription = null,
                            tint = Color(0xFFA8E6CF),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Instruction Step Card
                    if (routine.instructionSteps.isNotEmpty()) {
                        Text(
                            text = routine.instructionSteps[currentStepIndex % routine.instructionSteps.size],
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { routineRunning = !routineRunning },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = if (routineRunning) Icons.Default.Timer else Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (routineRunning) "Pause" else "Resume")
                        }

                        Button(
                            onClick = {
                                activeRoutine = null
                                routineRunning = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f))
                        ) {
                            Text("Exit Routine", color = Color.White)
                        }
                    }

                    AnimatedVisibility(visible = showCompletionDialog) {
                        Column(
                            modifier = Modifier.padding(top = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFFA8E6CF)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Routine Complete! Restored +15 NRI",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFA8E6CF)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    onCompleteRoutine(routine.title, routine.durationSeconds, startingMood, endingMood)
                                    showCompletionDialog = false
                                    activeRoutine = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97724))
                            ) {
                                Text("Log Session & Continue", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
