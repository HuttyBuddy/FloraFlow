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
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Unlock the ultimate garden design and diagnostic experience.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    val featuresList = @Composable {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PremiumFeatureRow(
                    title = "Restoration Journal",
                    description = "Access procedural binaural soundscapes and calculate your Neural Restoration Index (NRI) to reduce stress."
                )
                PremiumFeatureRow(
                    title = "Unlimited AI Botanist",
                    description = "Get expert, tailored diagnosis from our Gemini-powered Master Botanist without daily limits."
                )
                PremiumFeatureRow(
                    title = "Exotic Species DB",
                    description = "Gain access to 5,000+ premium rare and exotic plant templates for intricate climates."
                )
            }
        }
    }

    val ctaPricingCard = @Composable {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFFFFD54F), MaterialTheme.colorScheme.primary)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
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
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onUpgradeClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "Upgrade",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upgrade to PRO", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    val restoreButton = @Composable {
        Text(
            text = "Restore Purchases",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onRestoreClick() }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }

    val comparisonGrid = @Composable {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Plan Comparison Matrix",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Grid Headers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                        Text("Basic Chimes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, fontSize = 10.sp)
                    }
                    Box(modifier = Modifier.weight(0.25f), contentAlignment = Alignment.Center) {
                        Text("Full Binaural", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }

                // Feature Row 3
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("AI Master Botanist", modifier = Modifier.weight(0.5f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    Box(modifier = Modifier.weight(0.25f), contentAlignment = Alignment.Center) {
                        Text("5 / day", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, fontSize = 10.sp)
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
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                crownBadge()
                Spacer(modifier = Modifier.height(24.dp))
                titleSection()
                Spacer(modifier = Modifier.height(32.dp))
                featuresList()
                Spacer(modifier = Modifier.height(32.dp))
                ctaPricingCard()
                Spacer(modifier = Modifier.height(16.dp))
                restoreButton()
                Spacer(modifier = Modifier.height(48.dp))
            }
        } else {
            // WIDESCREEN / TABLET LANDSCAPE SPLIT LAYOUT
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT COLUMN (Branding, Crown Badge, CTA Pricing Card, Restore Button)
                Column(
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    crownBadge()
                    titleSection()
                    ctaPricingCard()
                    restoreButton()
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // RIGHT COLUMN (Detailed Value Props, Perks Matrix table)
                Column(
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
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
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
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
