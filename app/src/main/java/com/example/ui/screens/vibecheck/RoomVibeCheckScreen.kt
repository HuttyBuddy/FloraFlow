package com.example.ui.screens.vibecheck

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.share.ShareCardData
import com.example.ui.screens.share.ShareCardOverlay
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomVibeCheckScreen(
    onBack: () -> Unit,
    onTriggerPaywall: () -> Unit
) {
    var selectedPreset by remember { mutableStateOf("My Work Desk") }
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisDone by remember { mutableStateOf(false) }
    var showShareOverlay by remember { mutableStateOf(false) }

    val presets = listOf("My Work Desk", "Living Room Corner", "Bedroom Nightstand", "Balcony Garden")

    val vibeTags = mapOf(
        "My Work Desk" to "Cyberpunk Sanctuary (Low Oxygen Zone)",
        "Living Room Corner" to "Lush Botanical Haven",
        "Bedroom Nightstand" to "Mindful Rest Oasis",
        "Balcony Garden" to "Sun-Drenched Jungle"
    )

    val scores = mapOf(
        "My Work Desk" to 64,
        "Living Room Corner" to 88,
        "Bedroom Nightstand" to 76,
        "Balcony Garden" to 92
    )

    val currentScore = scores[selectedPreset] ?: 75
    val currentVibeTag = vibeTags[selectedPreset] ?: "Custom Room Sanctuary"

    val currentUpgrades = when (selectedPreset) {
        "My Work Desk" -> listOf(
            "Add Snake Plant for night CO2 reduction",
            "Introduce 10Hz Alpha binaural ambient stream",
            "Move monitor 3 inches away from direct foliage glow"
        )
        "Living Room Corner" -> listOf(
            "Group Monstera & Peace Lily for humidity synergy",
            "Schedule weekly misting during warm hours",
            "Incorporate indirect 4000K daylight LED"
        )
        "Bedroom Nightstand" -> listOf(
            "Add Lavender/Peace Lily for natural air purification",
            "Enable Theta 6Hz soundscape before sleep",
            "Maintain 40-60% relative room humidity"
        )
        else -> listOf(
            "Rotate pots weekly for uniform sun absorption",
            "Use fast-draining loamy substrate",
            "Monitor natural lux levels during midday peak"
        )
    }

    if (showShareOverlay) {
        ShareCardOverlay(
            data = ShareCardData(
                vibeTag = currentVibeTag,
                score = currentScore,
                scoreDelta = 100 - currentScore,
                topUpgrades = currentUpgrades
            ),
            onDismiss = { showShareOverlay = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF7FE3B5),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "AI Room Vibe Check",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE0F7ED)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFFE0F7ED)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF061A12)
                )
            )
        },
        containerColor = Color(0xFF061A12)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Intro Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F3022)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF337FE3B5))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Instant Biophilic Space Scan",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7FE3B5)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Select or scan your room setup below to calculate your Sanctuary Vitality Score & export your custom story card.",
                        fontSize = 14.sp,
                        color = Color(0xFFC3EBD9)
                    )
                }
            }

            // Preset Selection Chips
            Text(
                text = "1. Select Environment to Analyze:",
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE0F7ED),
                fontSize = 16.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.take(2).forEach { preset ->
                    val isSelected = selectedPreset == preset
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedPreset = preset
                            analysisDone = false
                        },
                        label = { Text(preset, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF7FE3B5),
                            selectedLabelColor = Color(0xFF03140C),
                            containerColor = Color(0xFF143B2B),
                            labelColor = Color(0xFFC3EBD9)
                        )
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.drop(2).forEach { preset ->
                    val isSelected = selectedPreset == preset
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedPreset = preset
                            analysisDone = false
                        },
                        label = { Text(preset, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF7FE3B5),
                            selectedLabelColor = Color(0xFF03140C),
                            containerColor = Color(0xFF143B2B),
                            labelColor = Color(0xFFC3EBD9)
                        )
                    )
                }
            }

            // Analyze Action Button
            Button(
                onClick = {
                    isAnalyzing = true
                    analysisDone = false
                },
                enabled = !isAnalyzing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7FE3B5),
                    contentColor = Color(0xFF03140C)
                )
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color(0xFF03140C),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Scanning Space Biophilia...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Run AI Space Diagnostic", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            LaunchedEffect(isAnalyzing) {
                if (isAnalyzing) {
                    delay(1200)
                    isAnalyzing = false
                    analysisDone = true
                }
            }

            // Results View
            AnimatedVisibility(
                visible = analysisDone,
                enter = fadeIn() + expandVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Result Summary Box
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D291D)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7FE3B5))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF267FE3B5),
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Text(
                                    text = "✨ $currentVibeTag",
                                    color = Color(0xFF7FE3B5),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                            }

                            Text(
                                text = "$currentScore%",
                                fontSize = 56.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Biophilic Vitality Score",
                                fontSize = 14.sp,
                                color = Color(0xFF9AE6C4),
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = Color.White.copy(alpha = 0.1f))
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Top Recommended Upgrades:",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE0F7ED),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            currentUpgrades.forEach { upgrade ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF7FE3B5),
                                        modifier = Modifier
                                            .size(18.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = upgrade,
                                        fontSize = 14.sp,
                                        color = Color(0xFFC3EBD9)
                                    )
                                }
                            }
                        }
                    }

                    // Share Story Card Trigger
                    Button(
                        onClick = { showShareOverlay = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E523A),
                            contentColor = Color(0xFF7FE3B5)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7FE3B5))
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Export Story Card to Social Media",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    // Upsell Trigger
                    OutlinedButton(
                        onClick = onTriggerPaywall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFE0F7ED)
                        )
                    ) {
                        Text("Unlock Deep AI Room Transformation (FloraFlow PRO)")
                    }
                }
            }
        }
    }
}
