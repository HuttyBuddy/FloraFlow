package com.example.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

@Composable
fun BiophilicProfileCard(
    score: Int,
    lowestCategories: List<String>,
    onRetakeClick: () -> Unit,
    step1Completed: Boolean = false,
    step2Completed: Boolean = false,
    step3Completed: Boolean = false,
    onStepToggle: (Int) -> Unit = {},
    onNavigate: (Int) -> Unit = {},
    onSearchDatabase: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    // Zone Determination
    val zoneInfo = when (score) {
        in 15..20 -> BiophilicZoneInfo(
            "Green Zone",
            "Environment supports nervous system calm and natural focus.",
            Color(0xFF2E7D32),
            Color(0xFFE8F5E9)
        )
        in 8..14 -> BiophilicZoneInfo(
            "Yellow Zone",
            "A few elements are missing, quietly draining focus/energy.",
            Color(0xFFF57F17),
            Color(0xFFFFFDE7)
        )
        else -> BiophilicZoneInfo(
            "Red Zone",
            "Lack of biophilic elements may increase sensory stress.",
            Color(0xFFC62828),
            Color(0xFFFFEBEE)
        )
    }
    val zoneName = zoneInfo.zoneName
    val zoneDesc = zoneInfo.zoneDesc
    val zoneColor = zoneInfo.zoneColor
    val zoneBg = zoneInfo.zoneBg

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("biophilic_profile_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.5.dp,
            Brush.horizontalGradient(
                listOf(zoneColor.copy(alpha = 0.8f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.SelfImprovement,
                        contentDescription = null,
                        tint = zoneColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Biophilic Sanctuary Profile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Box(
                    modifier = Modifier
                        .background(zoneBg, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$score/20",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = zoneColor
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "$zoneName — Active State",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = zoneColor
                )
                Text(
                    text = zoneDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (lowestCategories.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Top Sanctuary Opportunities",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse opportunities" else "Expand opportunities",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                AnimatedVisibility(visible = expanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        lowestCategories.forEach { category ->
                            val (tipTitle, tipDetail) = getBiophilicCategoryTip(category)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                    .padding(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Spa,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = tipTitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = tipDetail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            if (lowestCategories.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                
                Text(
                    text = "Personalized Next Steps Checklist",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                val stepsToRender = lowestCategories.take(3)
                stepsToRender.forEachIndexed { index, category ->
                    val isChecked = when (index) {
                        0 -> step1Completed
                        1 -> step2Completed
                        else -> step3Completed
                    }
                    val stepTitle = getBiophilicCategoryTip(category).first

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStepToggle(index + 1) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { onStepToggle(index + 1) },
                            modifier = Modifier.testTag("step_checkbox_${index + 1}")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stepTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            textDecoration = if (isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                            color = if (isChecked) MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        val (shortcutIcon, shortcutAction) = remember(category) {
                            when (category.uppercase()) {
                                "NATURE VIEWS", "LIVING PLANTS", "NATURAL LIGHT", "NATURAL MATERIALS", "AIR & VENTILATION", "ORGANIC FORMS" -> {
                                    Pair(Icons.Default.Explore) { onNavigate(1) } // Tab 1: Planner
                                }
                                "ACOUSTIC CALM", "WATER FEATURES" -> {
                                    Pair(Icons.Default.Spa) { onNavigate(4) } // Tab 4: Restoration
                                }
                                "SENSORY RICHNESS", "SEASONAL AWARENESS" -> {
                                    Pair(Icons.Default.Search) {
                                        val query = if (category.uppercase() == "SENSORY RICHNESS") "lavender" else "summer"
                                        onSearchDatabase(query)
                                        onNavigate(2) // Tab 2: Database
                                    }
                                }
                                else -> {
                                    Pair(Icons.Default.ArrowForward) { onNavigate(0) }
                                }
                            }
                        }
                        IconButton(
                            onClick = shortcutAction,
                            modifier = Modifier.size(36.dp).testTag("step_shortcut_btn_${index + 1}")
                        ) {
                            Icon(
                                imageVector = shortcutIcon,
                                contentDescription = "Go to corresponding screen",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                
                var completedCount = 0
                if (step1Completed) completedCount++
                if (step2Completed) completedCount++
                if (step3Completed) completedCount++
                
                LinearProgressIndicator(
                    progress = { completedCount / 3.0f },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )
                
                Text(
                    text = "Progress: $completedCount/3 steps completed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }

            OutlinedButton(
                onClick = onRetakeClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("biophilic_retake_assessment_btn")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Retake Onboarding Assessment", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private data class BiophilicZoneInfo(
    val zoneName: String,
    val zoneDesc: String,
    val zoneColor: Color,
    val zoneBg: Color
)

private fun getBiophilicCategoryTip(category: String): Pair<String, String> {
    return when (category.uppercase()) {
        "NATURE VIEWS" -> Pair("Optimize nature views", "Clear objects blocking window views. Place your indoor plant layout directly in line of sight to rest eye muscles.")
        "LIVING PLANTS" -> Pair("Add plant variety", "Sow at least 2-3 distinct indoor plants (e.g., ivy, bonsai) in your space to lower sympathetic nervous system arousal.")
        "NATURAL LIGHT" -> Pair("Maximize daylight", "Set your desk/sitting area within 5 feet of a natural window to regulate sleep cycles and cortisol.")
        "ACOUSTIC CALM" -> Pair("Introduce sound masking", "Place a tabletop moving water feature or play soothing botanical rain sounds to mask distracting background hums.")
        "NATURAL MATERIALS" -> Pair("Introduce natural textures", "Integrate materials like wood, cork, or a clay plant pot into your setup to stabilize stress baselines.")
        "AIR & VENTILATION" -> Pair("Enhance ventilation", "Open windows for 10 minutes twice daily, or place a quiet oscillating fan near plants to simulate natural breeze.")
        "ORGANIC FORMS" -> Pair("Adopt organic layouts", "Incorporate rounded pots, curved paths, or prints of leaf shapes to soften sharp institutional wall corners.")
        "WATER FEATURES" -> Pair("Water soundscape sync", "Sow moisture-loving plants or stream rainfall sounds when gardening to lower blood pressure and anxiety.")
        "SENSORY RICHNESS" -> Pair("Stimulate sensory triggers", "Introduce highly aromatic herbs (lavender, pine, mint) near your workspace. Inhale their scent during breaks.")
        "SEASONAL AWARENESS" -> Pair("Align with natural cycles", "Adjust indoor light durations to match current seasons. Keep a seasonal plant blooming inside.")
        else -> Pair("Enhance natural presence", "Add organic shapes, living greenery, and natural daylight zones to complete your biophilic shelter.")
    }
}
