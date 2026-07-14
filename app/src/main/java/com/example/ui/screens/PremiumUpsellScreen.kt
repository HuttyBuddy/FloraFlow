package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
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
import com.example.ui.components.ButtonVariant
import com.example.ui.components.FloraFlowButton
import com.example.ui.components.FloraFlowCard
import com.example.ui.theme.extendedColors
import com.example.ui.theme.spacing

@Composable
fun PremiumUpsellScreen(
    onCloseClick: () -> Unit,
    onUpgradeClick: (isAnnual: Boolean) -> Unit,
    onRestoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    // Real, Play-verified price + trial length for each plan. Null means
    // "not yet known" (still loading, or unavailable/mock mode) — the UI
    // falls back to static placeholder copy in that case rather than block.
    monthlyOffer: com.example.billing.BillingManager.OfferInfo? = null,
    annualOffer: com.example.billing.BillingManager.OfferInfo? = null
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp || configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.smallestScreenWidthDp >= 600
    val isWideScreen = isLandscape && (isTablet || configuration.screenWidthDp >= 600)

    var selectAnnual by remember { mutableStateOf(true) }

    val crownBadge = @Composable {
        Box(
            modifier = Modifier
                .size(88.dp)
                .border(
                    2.dp,
                    MaterialTheme.extendedColors.premiumGold,
                    CircleShape
                )
                .padding(4.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFF8E1), Color(0xFFFFECB3))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.WorkspacePremium,
                contentDescription = "Premium Badge",
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.extendedColors.premiumGold
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
                text = "Design a space you'll actually return to. Unlock deeper guidance, unlimited plans, and restorative routines for every indoor or outdoor space.",
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
                    title = "Unlimited spaces and blueprints",
                    description = "Create, compare, and refine plans for every room, patio, balcony, or garden."
                )
                PremiumFeatureRow(
                    title = "AI design guidance",
                    description = "Get practical plant, light, layout, and companion-plant advice for your active space."
                )
                PremiumFeatureRow(
                    title = "Restoration journal and soundscapes",
                    description = "Build calming breaks around your plants and track the routines that help you reset."
                )
                PremiumFeatureRow(
                    title = "Advanced space insights",
                    description = "Revisit assessments, see priority improvements, and keep each space's plan focused."
                )
            }
        }
    }

    // Fall back to static placeholder copy only while the real Play offer is
    // unknown (loading, or debug mock mode) — never claim a trial length
    // that Play Console doesn't actually have configured.
    val annualPrice = annualOffer?.formattedPrice ?: "$49.99 Yearly"
    val annualTrialDays = 3
    val annualTrialLabel = "$annualTrialDays days free, then $annualPrice"

    val monthlyPrice = monthlyOffer?.formattedPrice ?: "$4.99"
    val monthlyTrialDays = 3
    val monthlyTrialLabel = "$monthlyTrialDays days free, then $monthlyPrice/mo"

    val selectedTrialDays = if (selectAnnual) annualTrialDays else monthlyTrialDays

    val ctaPricingCard = @Composable {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Annual Plan Choice Card
            Card(
                onClick = { selectAnnual = true },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectAnnual) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(
                    width = if (selectAnnual) 2.dp else 1.dp,
                    color = if (selectAnnual) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectAnnual,
                        onClick = { selectAnnual = true }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Annual Plan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.extendedColors.success, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "BEST VALUE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.extendedColors.onSuccess
                                )
                            }
                        }
                        Text(annualTrialLabel, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Text(annualPrice, fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
            }

            // Monthly Plan Choice Card
            Card(
                onClick = { selectAnnual = false },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (!selectAnnual) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(
                    width = if (!selectAnnual) 2.dp else 1.dp,
                    color = if (!selectAnnual) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !selectAnnual,
                        onClick = { selectAnnual = false }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Monthly Plan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(monthlyTrialLabel, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(monthlyPrice, fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            FloraFlowButton(
                text = if (selectedTrialDays > 0) "Start free for $selectedTrialDays days" else "Subscribe Now",
                onClick = { onUpgradeClick(selectAnnual) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                variant = ButtonVariant.Premium,
                leadingIcon = Icons.Default.AutoAwesome
            )
            Text(
                text = "Cancel anytime in Google Play. No charge until your trial ends.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
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
                Spacer(modifier = Modifier.height(28.dp))
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
                    Spacer(modifier = Modifier.height(36.dp))
                    crownBadge()
                    titleSection()
                    ctaPricingCard()
                    restoreButton()
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.mediumSmall))
                }
 
                // RIGHT COLUMN (Detailed Value Props)
                Column(
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(36.dp))
                    featuresList()
                }
            }
        }
 
        // Close button at top end (absolute positioned overlay)
        IconButton(
            onClick = onCloseClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), CircleShape)
                .size(48.dp) // 48dp meets minimum touch target size!
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close paywall",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
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
