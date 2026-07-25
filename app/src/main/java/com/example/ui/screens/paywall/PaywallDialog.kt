package com.example.ui.screens.paywall

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSubscribe: (isAnnual: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    var selectedIsAnnual by remember { mutableStateOf(true) }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("paywall_dialog")
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFFE5A93C).copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Close & Badge Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFFE5A93C).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE5A93C))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFE5A93C), modifier = Modifier.size(14.dp))
                            Text("FLORAFLOW PRO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE5A93C))
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("paywall_close_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Paywall", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Headline
                Text(
                    text = "Transform Your Sanctuary",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Turn any dark indoor space into a calm, restorative biophilic haven with full AI power.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // PRO Features Checklist
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PaywallFeatureRow(
                        title = "Unlimited AI Plant Counsel",
                        subtitle = "Instant Leaf Doctor vision diagnosis & 24/7 care guidance"
                    )
                    PaywallFeatureRow(
                        title = "Real-Time Spatial Lux Meter",
                        subtitle = "Hardware sensor daylight mapping & window placement advice"
                    )
                    PaywallFeatureRow(
                        title = "Custom 4–40Hz Binaural Audio",
                        subtitle = "Alpha & Theta brainwave soundscapes for calm focus"
                    )
                    PaywallFeatureRow(
                        title = "Sanctuary Care Streak Rewards",
                        subtitle = "Unlock botanical caregiver badges & streak progress"
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Tier Selection Cards
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Annual Tier (Best Value)
                    PaywallTierCard(
                        selected = selectedIsAnnual,
                        title = "Annual Pass (3-Day Free Trial)",
                        price = "$49.99 / year",
                        subtext = "$4.16 / month — SAVE 58%",
                        badgeText = "BEST VALUE",
                        onClick = { selectedIsAnnual = true },
                        testTag = "tier_annual_card"
                    )

                    // Monthly Tier
                    PaywallTierCard(
                        selected = !selectedIsAnnual,
                        title = "Monthly Pass",
                        price = "$9.99 / month",
                        subtext = "Flexible billing, cancel anytime",
                        badgeText = null,
                        onClick = { selectedIsAnnual = false },
                        testTag = "tier_monthly_card"
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Primary CTA Button
                Button(
                    onClick = { onSubscribe(selectedIsAnnual) },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("subscribe_cta_btn")
                ) {
                    Text(
                        text = if (selectedIsAnnual) "Start 3-Day Free Trial" else "Subscribe Now ($9.99/mo)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Guarantee Subtext
                Text(
                    text = "🔒 Secured by Google Play. No commitment, cancel anytime.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PaywallFeatureRow(
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}

@Composable
private fun PaywallTierCard(
    selected: Boolean,
    title: String,
    price: String,
    subtext: String,
    badgeText: String?,
    onClick: () -> Unit,
    testTag: String
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val containerBg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (badgeText != null) 8.dp else 0.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = containerBg),
            border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (badgeText != null) 10.dp else 0.dp)
                .clickable { onClick() }
                .testTag(testTag)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtext,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 11.sp
                    )
                }

                Text(
                    text = price,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (badgeText != null) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 0.dp)
                    .border(2.dp, MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            ) {
                Text(
                    text = badgeText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }
}
