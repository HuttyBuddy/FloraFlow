package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.GardenViewModel

data class OnboardingStep(
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val emoji: String,
    val accentColor: Color,
    val imageRes: Int? = null
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    viewModel: GardenViewModel,
    modifier: Modifier = Modifier
) {
    var currentStepIdx by remember { mutableIntStateOf(0) }

    val steps = listOf(
        OnboardingStep(
            title = "FloraFlow",
            subtitle = "Therapeutic Space Planner",
            description = "Design gorgeous therapeutic gardens. Model customized Zen courtyards, desert xeriscapes, or tropical greenhouses on a perfect grid and measure plant dimensions.",
            icon = Icons.Default.Spa,
            emoji = "",
            accentColor = Color(0xFF386641), // Rich green
            imageRes = com.example.R.drawable.ic_logo_heart
        ),
        OnboardingStep(
            title = "Dr. Julian Greenleaf",
            subtitle = "Synaptic Live AI Botanist",
            description = "Meet your resident PhD botanist, powered by live Gemini Pro. Consult in real-time about soil moisture, microclimates, leaf pathologies, and stress-reduction therapy.",
            icon = Icons.Default.AutoAwesome,
            emoji = "🧑‍🔬",
            accentColor = Color(0xFF6A994E) // Soft sprout green
        ),
        OnboardingStep(
            title = "AR Lens Holograms",
            subtitle = "Simulated Spatial Placement",
            description = "Preview floral plans before purchasing. Place high-resolution digital plant stickers directly in your space using interactive gestures, rotation, and custom scale controls.",
            icon = Icons.Default.Videocam,
            emoji = "🪐",
            accentColor = Color(0xFFBC4749) // Terra Cotta / Warm Red
        )
    )

    val activeStep = steps[currentStepIdx]

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp || configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.smallestScreenWidthDp >= 600
    val isWideScreen = isLandscape && (isTablet || configuration.screenWidthDp >= 600)

    // Common indicators component
    val Indicators = @Composable {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { idx, _ ->
                val isSelected = currentStepIdx == idx
                val dotWidth by animateDpAsState(
                    targetValue = if (isSelected) 24.dp else 8.dp,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                )
                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .width(dotWidth)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }
        }
    }

    // Common primary action button component
    val NextButton = @Composable {
        Button(
            onClick = {
                if (currentStepIdx < steps.size - 1) {
                    currentStepIdx++
                } else {
                    viewModel.completeOnboarding()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("onboarding_next_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (currentStepIdx == steps.size - 1) "ENTER FLORAL SPACE ✨" else "CONTINUE TOUR",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next onboard slide action",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }

    // Common header top branding bar
    val BrandingHeader = @Composable {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_logo_heart),
                contentDescription = "FloraFlow App Icon",
                modifier = Modifier
                    .size(24.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
            )
            Text(
                text = "FloraFlow",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    // Common Skip button
    val SkipButton = @Composable {
        Text(
            text = "SKIP",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .testTag("onboarding_skip_button")
                .clip(RoundedCornerShape(8.dp))
                .clickable { viewModel.completeOnboarding() }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .padding(24.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        if (!isWideScreen) {
            // PORTRAIT / MOBILE LAYOUT
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BrandingHeader()
                    SkipButton()
                }

                // Central Carousel Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = activeStep,
                        transitionSpec = {
                            if (targetState.title != initialState.title) {
                                slideInHorizontally { width -> width } + fadeIn() togetherWith
                                        slideOutHorizontally { width -> -width } + fadeOut()
                            } else {
                                fadeIn() togetherWith fadeOut()
                            }
                        },
                        label = "carouselScroll"
                    ) { step ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .testTag("onboarding_slide_card"),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.background
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                if (step.imageRes != null) {
                                    androidx.compose.foundation.Image(
                                        painter = androidx.compose.ui.res.painterResource(id = step.imageRes),
                                        contentDescription = "FloraFlow Logo",
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(RoundedCornerShape(28.dp))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(RoundedCornerShape(28.dp))
                                            .background(step.accentColor.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(RoundedCornerShape(22.dp))
                                                .background(step.accentColor.copy(alpha = 0.22f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(step.emoji, fontSize = 42.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = step.title,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = step.subtitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = step.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Indicators and primary button
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Indicators()
                    NextButton()
                }
            }
        } else {
            // WIDESCREEN / TABLET LANDSCAPE SPLIT LAYOUT
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT COLUMN (Branding, beautiful animated badge, indicators)
                Column(
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Bar Brand
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BrandingHeader()
                    }

                    // Mid Dynamic Visual Enclosing Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = activeStep,
                            transitionSpec = {
                                scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn() togetherWith
                                        scaleOut() + fadeOut()
                            },
                            label = "wideVisual"
                        ) { step ->
                            if (step.imageRes != null) {
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(id = step.imageRes),
                                    contentDescription = "FloraFlow Logo",
                                    modifier = Modifier
                                        .size(180.dp)
                                        .clip(RoundedCornerShape(44.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(180.dp)
                                        .clip(RoundedCornerShape(44.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    step.accentColor.copy(alpha = 0.15f),
                                                    step.accentColor.copy(alpha = 0.05f)
                                                )
                                            )
                                        )
                                        .border(
                                            width = 1.5.dp,
                                            color = step.accentColor.copy(alpha = 0.25f),
                                            shape = RoundedCornerShape(44.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(130.dp)
                                            .clip(RoundedCornerShape(32.dp))
                                            .background(step.accentColor.copy(alpha = 0.25f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(step.emoji, fontSize = 72.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Indicators at the bottom
                    Box(
                        modifier = Modifier.padding(vertical = 16.dp)
                    ) {
                        Indicators()
                    }
                }

                // RIGHT COLUMN (Scrollable details card, CTA buttons)
                Column(
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header skip bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SkipButton()
                    }

                    // Slide step details enclosed in a beautiful premium Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .padding(vertical = 12.dp)
                            .testTag("onboarding_slide_card"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.background
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = activeStep.title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = activeStep.subtitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = activeStep.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }

                    // Action buttons at the bottom of Right Column
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        NextButton()
                    }
                }
            }
        }
    }
}
