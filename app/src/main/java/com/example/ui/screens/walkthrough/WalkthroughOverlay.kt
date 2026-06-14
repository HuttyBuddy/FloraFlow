package com.example.ui.screens.walkthrough

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.GardenViewModel
import com.example.ui.viewmodel.WalkthroughStep
import com.example.ui.viewmodel.ScreenRect

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WalkthroughOverlay(
    viewModel: GardenViewModel,
    currentTab: Int,
    onTabChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val step by viewModel.currentWalkthroughStep.collectAsState()
    val targets by viewModel.walkthroughTargets.collectAsState()

    AnimatedVisibility(
        visible = step != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val currentStep = step ?: return@AnimatedVisibility
        val target = targets[currentStep]

        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .clickable(enabled = false) {} // block click through
        ) {
            val density = LocalDensity.current
            val screenHeightPx = with(density) { maxHeight.toPx() }
            val screenWidthPx = with(density) { maxWidth.toPx() }

            // Pre-calculate design sizes in pixels to avoid inside-canvas compilation/density issues
            val cornerRadiusPx = with(density) { 16.dp.toPx() }
            val borderPaddingPx = with(density) { 4.dp.toPx() }
            val borderWidthPx = with(density) { 2.5.dp.toPx() }
            val haloWidthPx = with(density) { 1.5.dp.toPx() }
            val innerBorderRadiusPx = with(density) { 20.dp.toPx() }
            val pulseTargetPx = with(density) { 6.dp.toPx() }

            // 1. Draw Spotlight Backdrop (if target exists and it's not the Welcome step)
            if (currentStep != WalkthroughStep.WELCOME && target != null) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                ) {
                    // Draw dark semi-transparent overlay
                    drawRect(color = Color.Black.copy(alpha = 0.75f))

                    // Clear target area
                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = Offset(target.left, target.top),
                        size = Size(target.width, target.height),
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                        blendMode = BlendMode.Clear
                    )
                }

                // Pulsing glow border around target
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = pulseTargetPx,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseScale"
                )
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.8f,
                    targetValue = 0.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseAlpha"
                )

                val primaryColor = MaterialTheme.colorScheme.primary

                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Solid inner border
                    drawRoundRect(
                        color = primaryColor,
                        topLeft = Offset(target.left - borderPaddingPx, target.top - borderPaddingPx),
                        size = Size(target.width + borderPaddingPx * 2, target.height + borderPaddingPx * 2),
                        cornerRadius = CornerRadius(innerBorderRadiusPx, innerBorderRadiusPx),
                        style = Stroke(width = borderWidthPx)
                    )
                    // Outer pulsing halo
                    drawRoundRect(
                        color = primaryColor.copy(alpha = pulseAlpha),
                        topLeft = Offset(
                            target.left - borderPaddingPx - pulseScale,
                            target.top - borderPaddingPx - pulseScale
                        ),
                        size = Size(
                            target.width + borderPaddingPx * 2 + pulseScale * 2,
                            target.height + borderPaddingPx * 2 + pulseScale * 2
                        ),
                        cornerRadius = CornerRadius(innerBorderRadiusPx + pulseScale, innerBorderRadiusPx + pulseScale),
                        style = Stroke(width = haloWidthPx)
                    )
                }
            } else {
                // Static dark backdrop when there is no target or on Welcome step
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f))
                )
            }

            // 2. Position Tooltip Card Dynamically
            var cardWidth by remember { mutableIntStateOf(0) }
            var cardHeight by remember { mutableIntStateOf(0) }

            val cardY = if (currentStep == WalkthroughStep.WELCOME || target == null) {
                // Centered fallback
                (screenHeightPx - cardHeight) / 2
            } else {
                val targetCenterY = target.centerY
                if (targetCenterY > screenHeightPx / 2) {
                    // Place card above the target: target.top - cardHeight - padding
                    (target.top - cardHeight - with(density) { 24.dp.toPx() })
                        .coerceAtLeast(with(density) { 24.dp.toPx() })
                } else {
                    // Place card below the target: target.bottom + padding
                    (target.bottom + with(density) { 24.dp.toPx() })
                        .coerceAtMost(screenHeightPx - cardHeight - with(density) { 24.dp.toPx() })
                }
            }

            val cardX = (screenWidthPx - cardWidth) / 2

            Box(
                modifier = Modifier
                    .offset { IntOffset(cardX.toInt(), cardY.toInt()) }
                    .width(if (currentStep == WalkthroughStep.WELCOME) 340.dp else 320.dp)
                    .onGloballyPositioned {
                        cardWidth = it.size.width
                        cardHeight = it.size.height
                    }
                    .padding(horizontal = 8.dp)
            ) {
                val arrowWidthPx = with(density) { 18.dp.toPx() }
                val arrowHeightPx = with(density) { 10.dp.toPx() }
                val arrowColor = MaterialTheme.colorScheme.surface

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    // If card is below target, place the UP-pointing arrow at the top of the column
                    if (currentStep != WalkthroughStep.WELCOME && target != null && target.centerY <= screenHeightPx / 2) {
                        val arrowX = (target.centerX - cardX - arrowWidthPx / 2)
                            .coerceIn(
                                with(density) { 20.dp.toPx() },
                                cardWidth - with(density) { 20.dp.toPx() } - arrowWidthPx
                            )

                        Canvas(
                            modifier = Modifier
                                .size(18.dp, 10.dp)
                                .offset { IntOffset(arrowX.toInt(), 0) }
                        ) {
                            val path = Path().apply {
                                moveTo(size.width / 2, 0f)
                                lineTo(0f, size.height)
                                lineTo(size.width, size.height)
                                close()
                            }
                            drawPath(path, color = arrowColor)
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .shadow(16.dp, shape = RoundedCornerShape(20.dp))
                            .testTag("walkthrough_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Badge Icon
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
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
                                Text(textOrIcon, fontSize = 24.sp)
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
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )

                            val description = when (currentStep) {
                                WalkthroughStep.WELCOME -> "Let's take a quick interactive tour to get familiar with your new therapeutic garden helper."
                                WalkthroughStep.DASHBOARD_GARDEN -> "This is your Garden Hub. Tap 'New Garden' to create Zen spaces or switch active layouts via 'Select Garden'."
                                WalkthroughStep.DASHBOARD_STATS -> "The Growth Sync chart correlates your physical gardening minutes with rating improvements to track stress reduction."
                                WalkthroughStep.PLANNER_TAB -> "Switch to the 2D Planner to drag-and-drop crops on a companion planting grid with spacing safety alerts."
                                WalkthroughStep.AI_ADVISOR_TAB -> "Consult Dr. Julian Greenleaf, your live AI advisor, for plant stress diagnostics, soil pH balance, and mindfulness support."
                                WalkthroughStep.AR_LENS_TAB -> "Enter the AR Lens to project digital flower decals directly in your physical room and check exact sizing overlays."
                            }

                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )

                            // Actions Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.skipWalkthrough() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Skip", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                Button(
                                    onClick = {
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
                                    modifier = Modifier.weight(1.3f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    val buttonText = if (currentStep == WalkthroughStep.AR_LENS_TAB) "Finish" else "Next"
                                    Text(buttonText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (currentStep == WalkthroughStep.AR_LENS_TAB) Icons.Default.Check else Icons.Default.ArrowForward,
                                        contentDescription = "Walkthrough Action",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    // If card is above target, place the DOWN-pointing arrow at the bottom of the column
                    if (currentStep != WalkthroughStep.WELCOME && target != null && target.centerY > screenHeightPx / 2) {
                        val arrowX = (target.centerX - cardX - arrowWidthPx / 2)
                            .coerceIn(
                                with(density) { 20.dp.toPx() },
                                cardWidth - with(density) { 20.dp.toPx() } - arrowWidthPx
                            )

                        Canvas(
                            modifier = Modifier
                                .size(18.dp, 10.dp)
                                .offset { IntOffset(arrowX.toInt(), 0) }
                        ) {
                            val path = Path().apply {
                                moveTo(0f, 0f)
                                lineTo(size.width, 0f)
                                lineTo(size.width / 2, size.height)
                                close()
                            }
                            drawPath(path, color = arrowColor)
                        }
                    }
                }
            }
        }
    }
}
