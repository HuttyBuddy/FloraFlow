package com.example.ui.screens.walkthrough

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.GardenViewModel
import com.example.ui.viewmodel.WalkthroughStep

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WalkthroughOverlay(
    viewModel: GardenViewModel,
    currentTab: Int,
    onTabChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val step by viewModel.currentWalkthroughStep.collectAsState()

    AnimatedVisibility(
        visible = step != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val currentStep = step ?: return@AnimatedVisibility

        // Full screen blocking backdrop
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable(enabled = false) {} // block click through
        ) {
            // Position tooltip dynamically based on the current step
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                contentAlignment = when (currentStep) {
                    WalkthroughStep.WELCOME -> Alignment.Center
                    WalkthroughStep.DASHBOARD_GARDEN -> Alignment.TopCenter
                    WalkthroughStep.DASHBOARD_STATS -> Alignment.Center
                    WalkthroughStep.PLANNER_TAB -> Alignment.BottomCenter
                    WalkthroughStep.AI_ADVISOR_TAB -> Alignment.BottomCenter
                    WalkthroughStep.AR_LENS_TAB -> Alignment.BottomCenter
                }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth(if (currentStep == WalkthroughStep.WELCOME) 1f else 0.95f)
                        .animateContentSize()
                ) {
                    if (currentStep == WalkthroughStep.DASHBOARD_GARDEN) {
                        Spacer(modifier = Modifier.height(64.dp)) // Push down to point to dashboard header
                    }

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .shadow(24.dp, shape = RoundedCornerShape(24.dp))
                            .testTag("walkthrough_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Badge icon/emoji
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                val textOrIcon = when (currentStep) {
                                    WalkthroughStep.WELCOME -> "👋"
                                    WalkthroughStep.DASHBOARD_GARDEN -> "🏡"
                                    WalkthroughStep.DASHBOARD_STATS -> "📈"
                                    WalkthroughStep.PLANNER_TAB -> "📐"
                                    WalkthroughStep.AI_ADVISOR_TAB -> "🔬"
                                    WalkthroughStep.AR_LENS_TAB -> "🪐"
                                }
                                Text(textOrIcon, fontSize = 28.sp)
                            }

                            val title = when (currentStep) {
                                WalkthroughStep.WELCOME -> "Welcome to FloraFlow!"
                                WalkthroughStep.DASHBOARD_GARDEN -> "Active Garden Control"
                                WalkthroughStep.DASHBOARD_STATS -> "Mindfulness Tracker"
                                WalkthroughStep.PLANNER_TAB -> "2D Companion Planner"
                                WalkthroughStep.AI_ADVISOR_TAB -> "Live AI Advisor"
                                WalkthroughStep.AR_LENS_TAB -> "AR Hologram Lens"
                            }

                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )

                            val description = when (currentStep) {
                                WalkthroughStep.WELCOME -> "Let's take a quick 4-step interactive tour to get familiar with your new therapeutic garden helper."
                                WalkthroughStep.DASHBOARD_GARDEN -> "This is your Garden Hub. Tap 'New Garden' to create Zen spaces, Xeriscapes, or Greenhouses, or switch active layouts via 'Select Garden'."
                                WalkthroughStep.DASHBOARD_STATS -> "The Growth Sync chart correlates your physical gardening minutes with rating improvements to track mental stress reduction."
                                WalkthroughStep.PLANNER_TAB -> "Switch to the 2D Planner to drag-and-drop crops on a companion planting grid with automatic spacing safety alerts."
                                WalkthroughStep.AI_ADVISOR_TAB -> "Consult Dr. Julian Greenleaf, your live AI advisor, for plant stress diagnostics, pH balance advice, and mindfulness support."
                                WalkthroughStep.AR_LENS_TAB -> "Enter the AR Lens to project digital flower decals directly in your physical room and check exact sizing overlays."
                            }

                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )

                            // Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.skipWalkthrough() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Skip", fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        // Auto tab switching behaviors for interactive feeling
                                        when (currentStep) {
                                            WalkthroughStep.WELCOME -> {
                                                onTabChange(0) // Go to Dashboard
                                                viewModel.nextWalkthroughStep()
                                            }
                                            WalkthroughStep.DASHBOARD_GARDEN -> {
                                                viewModel.nextWalkthroughStep()
                                            }
                                            WalkthroughStep.DASHBOARD_STATS -> {
                                                onTabChange(1) // Auto-switch to 2D Planner
                                                viewModel.nextWalkthroughStep()
                                            }
                                            WalkthroughStep.PLANNER_TAB -> {
                                                onTabChange(3) // Auto-switch to AI Advisor
                                                viewModel.nextWalkthroughStep()
                                            }
                                            WalkthroughStep.AI_ADVISOR_TAB -> {
                                                onTabChange(4) // Auto-switch to AR Lens
                                                viewModel.nextWalkthroughStep()
                                            }
                                            WalkthroughStep.AR_LENS_TAB -> {
                                                onTabChange(0) // Return to Dashboard
                                                viewModel.nextWalkthroughStep()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1.5f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    val buttonText = if (currentStep == WalkthroughStep.AR_LENS_TAB) "Finish Tour" else "Next"
                                    Text(buttonText, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (currentStep == WalkthroughStep.AR_LENS_TAB) Icons.Default.Check else Icons.Default.ArrowForward,
                                        contentDescription = "Walkthrough Action",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Floating Pointer Arrow Indicators (Except Welcome screen)
                    if (currentStep != WalkthroughStep.WELCOME) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when (currentStep) {
                                WalkthroughStep.DASHBOARD_GARDEN -> "▲ pointing to active garden controls ▲"
                                WalkthroughStep.DASHBOARD_STATS -> "▼ pointing to growth sync tracker below ▼"
                                WalkthroughStep.PLANNER_TAB -> "▼ pointing to planner navigator below ▼"
                                WalkthroughStep.AI_ADVISOR_TAB -> "▼ pointing to AI Advisor chat below ▼"
                                WalkthroughStep.AR_LENS_TAB -> "▼ pointing to AR camera lens below ▼"
                                else -> ""
                            },
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    if (currentStep == WalkthroughStep.PLANNER_TAB ||
                        currentStep == WalkthroughStep.AI_ADVISOR_TAB ||
                        currentStep == WalkthroughStep.AR_LENS_TAB) {
                        Spacer(modifier = Modifier.height(72.dp)) // Make space for bottom bar pointers
                    }
                }
            }
        }
    }
}
