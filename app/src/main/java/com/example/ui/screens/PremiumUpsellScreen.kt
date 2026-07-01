package com.example.ui.screens

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ButtonVariant
import com.example.ui.components.FloraFlowButton
import com.example.ui.components.FloraFlowCard
import com.example.ui.theme.extendedColors
import com.example.ui.theme.spacing

@Composable
fun PremiumUpsellScreen(
    onUpgradeClick: () -> Unit,
    onRestoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp || configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.smallestScreenWidthDp >= 600
    val isWideScreen = isLandscape && (isTablet || configuration.screenWidthDp >= 600)

    val crownBadge = @Composable {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFFFFD54F), Color(0xFFFFB74D))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.WorkspacePremium,
                contentDescription = "Premium Badge",
                modifier = Modifier.size(48.dp),
                tint = Color.White
            )
        }
    }

    val titleSection = @Composable {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "FloraFlow PRO",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            Text(
                text = "Point your phone at a space, envision, plan, and bring a garden to life. Unlock premium eco-acoustic soundscapes and biophilic design neuroscience.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    val featuresList = @Composable {
        FloraFlowCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(MaterialTheme.spacing.large),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
            ) {
                PremiumFeatureRow(
                    title = "Eco-Acoustic Restoration Journal",
                    description = "Immersive eco-acoustic binaural soundscapes & Neural Restoration Journal with dynamic stress metrics."
                )
                PremiumFeatureRow(
                    title = "Gemini AI Advisor",
                    description = "Biophilic design neuroscience assistant. Point your phone at any space to envision, plan, and get expert botany advice."
                )
                PremiumFeatureRow(
                    title = "Unlimited Blueprints & Layouts",
                    description = "Plan professional-grade garden layouts on the interactive grid blueprint without design training."
                )
            }
        }
    }

    val ctaPricingCard = @Composable {
        FloraFlowCard(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFFFFD54F), MaterialTheme.colorScheme.primary)
                    ),
                    shape = MaterialTheme.shapes.medium
                ),
            elevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(MaterialTheme.spacing.medium),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$4.99 / month",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Cancel anytime. Billed monthly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                
                FloraFlowButton(
                    text = "Upgrade to FloraFlow PRO",
                    onClick = onUpgradeClick,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = Icons.Default.AutoAwesome
                )
            }
        }
    }

    val restoreButton = @Composable {
        FloraFlowButton(
            text = "Restore Existing Purchase",
            onClick = onRestoreClick,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.small),
            variant = ButtonVariant.Text
        )
    }

    val comparisonGrid = @Composable {
        FloraFlowCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(MaterialTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.mediumSmall)
            ) {
                Text(
                    "Compare Plan Benefits",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Feature", modifier = Modifier.weight(0.5f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("FREE", modifier = Modifier.weight(0.25f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("PRO", modifier = Modifier.weight(0.25f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.primary)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Feature Row 1
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("2D Space Planner", modifier = Modifier.weight(0.5f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    Box(modifier = Modifier.weight(0.25f), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, contentDescription = "Yes", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                    Box(modifier = Modifier.weight(0.25f), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, contentDescription = "Yes", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                }

                // Feature Row 2
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Restoration Journal", modifier = Modifier.weight(0.5f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    Box(modifier = Modifier.weight(0.25f), contentAlignment = Alignment.Center) {
                        Text("3 free plays", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, fontSize = 10.sp)
                    }
                    Box(modifier = Modifier.weight(0.25f), contentAlignment = Alignment.Center) {
                        Text("Full Binaural", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }

                // Feature Row 3
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("AI Master Botanist", modifier = Modifier.weight(0.5f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    Box(modifier = Modifier.weight(0.25f), contentAlignment = Alignment.Center) {
                        Text("3 free queries", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, fontSize = 10.sp)
                    }
                    Box(modifier = Modifier.weight(0.25f), contentAlignment = Alignment.Center) {
                        Text("Unlimited", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }

                // Feature Row 4
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Botanical Catalog", modifier = Modifier.weight(0.5f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    Box(modifier = Modifier.weight(0.25f), contentAlignment = Alignment.Center) {
                        Text("Basic Only", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, fontSize = 10.sp)
                    }
                    Box(modifier = Modifier.weight(0.25f), contentAlignment = Alignment.Center) {
                        Text("5000+ Exotic", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }

                // Feature Row 5
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Lab Soundwave Diagnostics", modifier = Modifier.weight(0.5f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    Box(modifier = Modifier.weight(0.25f), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Close, contentDescription = "No", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                    }
                    Box(modifier = Modifier.weight(0.25f), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, contentDescription = "Yes", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                )
            )
    ) {
        if (!isWideScreen) {
            // PORTRAIT / MOBILE LAYOUT
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(MaterialTheme.spacing.large)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                crownBadge()
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
                titleSection()
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraLarge))
                featuresList()
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraLarge))
                ctaPricingCard()
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                restoreButton()
                Spacer(modifier = Modifier.height(48.dp))
            }
        } else {
            // WIDESCREEN / TABLET LANDSCAPE SPLIT LAYOUT
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(MaterialTheme.spacing.large),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT COLUMN (Branding, Crown Badge, CTA Pricing Card, Restore Button)
                Column(
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.mediumSmall))
                    crownBadge()
                    titleSection()
                    ctaPricingCard()
                    restoreButton()
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.mediumSmall))
                }

                // RIGHT COLUMN (Detailed Value Props, Perks Matrix table)
                Column(
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    featuresList()
                    comparisonGrid()
                }
            }
        }
    }
}

@Composable
fun PremiumFeatureRow(title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = "Check",
            tint = MaterialTheme.extendedColors.success,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
