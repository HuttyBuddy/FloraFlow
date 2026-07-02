package com.example.ui.screens.dashboard.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.GardenViewModel
import com.example.ui.theme.spacing
import kotlinx.coroutines.delay

enum class BreathingState {
    IDLE, INHALE, HOLD_IN, EXHALE, HOLD_OUT
}

@Composable
fun MindfulBreathingCard(
    viewModel: GardenViewModel,
    modifier: Modifier = Modifier
) {
    var breathingState by remember { mutableStateOf(BreathingState.IDLE) }
    var secondsRemaining by remember { mutableIntStateOf(60) }
    var showSuccessState by remember { mutableStateOf(false) }
    var customNotes by remember { mutableStateOf("") }
    
    val targetProgress = when (breathingState) {
        BreathingState.INHALE -> 1.0f
        BreathingState.HOLD_IN -> 1.0f
        BreathingState.EXHALE -> 0.0f
        BreathingState.HOLD_OUT -> 0.0f
        BreathingState.IDLE -> 0.0f
    }
    
    val breathProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
        label = "BreathProgress"
    )
    
    val auraColor by animateColorAsState(
        targetValue = when (breathingState) {
            BreathingState.INHALE -> Color(0xFF80CBC4)
            BreathingState.HOLD_IN -> Color(0xFF81C784)
            BreathingState.EXHALE -> Color(0xFFB39DDB)
            BreathingState.HOLD_OUT -> Color(0xFF90A4AE)
            BreathingState.IDLE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        },
        animationSpec = tween(durationMillis = 1500),
        label = "AuraColor"
    )
    
    val stateText = when (breathingState) {
        BreathingState.INHALE -> "Breathe In..."
        BreathingState.HOLD_IN -> "Hold..."
        BreathingState.EXHALE -> "Breathe Out..."
        BreathingState.HOLD_OUT -> "Hold..."
        BreathingState.IDLE -> "Press Begin to start 1-minute box breathing"
    }

    LaunchedEffect(breathingState) {
        if (breathingState != BreathingState.IDLE && secondsRemaining > 0) {
            while (secondsRemaining > 0 && breathingState != BreathingState.IDLE) {
                delay(1000)
                secondsRemaining--
                if (secondsRemaining == 0) {
                    breathingState = BreathingState.IDLE
                    showSuccessState = true
                }
            }
        }
    }
    
    LaunchedEffect(breathingState) {
        if (breathingState != BreathingState.IDLE) {
            while (secondsRemaining > 0) {
                delay(4000)
                breathingState = when (breathingState) {
                    BreathingState.INHALE -> BreathingState.HOLD_IN
                    BreathingState.HOLD_IN -> BreathingState.EXHALE
                    BreathingState.EXHALE -> BreathingState.HOLD_OUT
                    BreathingState.HOLD_OUT -> BreathingState.INHALE
                    else -> BreathingState.IDLE
                }
            }
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.5.dp, 
            Brush.horizontalGradient(
                listOf(
                    MaterialTheme.colorScheme.tertiary, 
                    MaterialTheme.colorScheme.primary
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.mediumSmall)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.SelfImprovement,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                Text(
                    text = "Mindful Garden Breath",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                if (breathingState != BreathingState.IDLE) {
                    Text(
                        text = "${secondsRemaining}s remaining",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            
            if (showSuccessState) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🌸 Excellent! You completed a breathing cycle.", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("This outdoor breathing minute correlates with 5% higher mental well-being and calmness indices.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    
                    OutlinedTextField(
                        value = customNotes,
                        onValueChange = { customNotes = it },
                        placeholder = { Text("How do you feel? (e.g. Cleared my mind)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().height(56.dp).testTag("breathing_gratitude_input"),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.mediumSmall),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        TextButton(onClick = { 
                            showSuccessState = false
                            customNotes = ""
                        }) {
                            Text("Dismiss")
                        }
                        Button(
                            onClick = {
                                viewModel.logMood(
                                    mood = "Peaceful",
                                    score = 5,
                                    duration = 1,
                                    notes = customNotes.ifBlank { "Completed 1 minute of mindful breathing." }
                                )
                                showSuccessState = false
                                customNotes = ""
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Save Log", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .padding(MaterialTheme.spacing.small),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val petalCount = 8
                        val scale = 0.4f + breathProgress * 0.6f
                        val center = Offset(size.width / 2, size.height / 2)
                        
                        // Glow aura background
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(auraColor.copy(alpha = 0.35f), Color.Transparent),
                                radius = size.minDimension / 1.6f
                            ),
                            center = center
                        )
                        
                        // Draw petals
                        for (i in 0 until petalCount) {
                            val angle = i * (360f / petalCount)
                            rotate(angle, pivot = center) {
                                val petalWidth = size.width * 0.16f * scale
                                val petalHeight = size.height * 0.36f * scale
                                drawOval(
                                    color = auraColor,
                                    topLeft = Offset(center.x - petalWidth / 2, center.y - petalHeight),
                                    size = Size(petalWidth, petalHeight),
                                    alpha = 0.8f
                                )
                            }
                        }
                        
                        // Gold center
                        drawCircle(
                            color = Color(0xFFFFD54F),
                            radius = size.minDimension * 0.08f * (0.8f + breathProgress * 0.2f),
                            center = center
                        )
                    }
                    
                    if (breathingState != BreathingState.IDLE) {
                        Text(
                            text = when (breathingState) {
                                BreathingState.INHALE -> "In"
                                BreathingState.HOLD_IN -> "Hold"
                                BreathingState.EXHALE -> "Out"
                                BreathingState.HOLD_OUT -> "Hold"
                                else -> ""
                            },
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp
                        )
                    }
                }
                
                Text(
                    text = stateText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                
                if (breathingState == BreathingState.IDLE) {
                    Button(
                        onClick = {
                            secondsRemaining = 60
                            breathingState = BreathingState.INHALE
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Begin Breathing")
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            breathingState = BreathingState.IDLE
                            secondsRemaining = 60
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Reset")
                    }
                }
            }
        }
    }
}
