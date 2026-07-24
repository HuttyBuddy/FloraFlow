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

    val surfaceBg = MaterialTheme.colorScheme.background
    val defaultBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

    val zoneInfo = when {
        score >= 15 -> BiophilicZoneInfo(
            "Green Zone",
            "Restorative corner supports nervous system calm and natural focus.",
            Color(0xFF2E7D32),
            Color(0xFFE8F5E9)
        )
        score in 8..14 -> BiophilicZoneInfo(
            "Yellow Zone",
            "A few elements are missing from your restorative corner, quietly draining focus.",
            Color(0xFFF57F17),
            Color(0xFFFFFDE7)
        )
        score in 1..7 -> BiophilicZoneInfo(
            "Red Zone",
            "Restorative corner needs lighting and plant additions to lower stress.",
            Color(0xFFC62828),
            Color(0xFFFFEBEE)
        )
        else -> BiophilicZoneInfo(
            "Not Assessed",
            "Take the 2-minute Restorative Corner Assessment to discover your score.",
            MaterialTheme.colorScheme.primary,
            surfaceBg
        )
    }
    val zoneName = zoneInfo.zoneName
    val zoneDesc = zoneInfo.zoneDesc
    val zoneColor = zoneInfo.zoneColor
    val zoneBg = zoneInfo.zoneBg

    val cardBorder = if (score > 0) {
        BorderStroke(
            1.5.dp,
            Brush.horizontalGradient(
                listOf(zoneColor.copy(alpha = 0.8f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
            )
        )
    } else {
        BorderStroke(1.dp, defaultBorderColor)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("biophilic_profile_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = cardBorder
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
                        contentDescription = "Restorative Corner Profile",
                        tint = zoneColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Restorative Corner Profile",
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
                        text = if (score > 0) "$score/20" else "Not Assessed",
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
                                        contentDescription = "Spa",
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

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            
            Text(
                text = "Personalized Next Steps Checklist",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            val candidateCategories = lowestCategories + listOf("NATURAL LIGHT", "LIVING PLANTS", "ACOUSTIC CALM", "NATURAL MATERIALS", "AIR & VENTILATION")
            val distinctSteps = mutableListOf<BiophilicStepInfo>()
            val seenTitles = mutableSetOf<String>()

            for (cat in candidateCategories) {
                val info = getBiophilicStepInfo(cat)
                if (seenTitles.add(info.title)) {
                    distinctSteps.add(info)
                    if (distinctSteps.size >= 3) break
                }
            }

            distinctSteps.forEachIndexed { index, stepInfo ->
                val isChecked = when (index) {
                    0 -> step1Completed
                    1 -> step2Completed
                    else -> step3Completed
                }

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
                        text = stepInfo.title,
                        style = MaterialTheme.typography.bodyMedium,
                        textDecoration = if (isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                        color = if (isChecked) MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    
                    val destinationPage = if (stepInfo.targetPage != 0) stepInfo.targetPage else if (index == 0) 1 else 2

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable { onNavigate(destinationPage) }
                            .testTag("step_shortcut_btn_${index + 1}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Go to corresponding recommendation card",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRetakeClick,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("biophilic_retake_assessment_btn")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Retake Restorative Assessment", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Retake Assessment", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { onNavigate(2) }, // Navigate to Card 3 (Daily Tend & Audio)
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share to Community", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share Score", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
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

private data class BiophilicStepInfo(
    val title: String,
    val detail: String,
    val targetPage: Int
)

private fun getBiophilicStepInfo(category: String): BiophilicStepInfo {
    val upper = category.uppercase()
    return when {
        upper.contains("LIGHT") || upper.contains("DAYLIGHT") -> BiophilicStepInfo("Maximize natural daylight", "Set your desk/sitting area within 5 feet of natural light to regulate sleep cycles and cortisol.", 1)
        upper.contains("PLANT") || upper.contains("GREEN") -> BiophilicStepInfo("Add companion living plants", "Sow at least 2-3 distinct plants (e.g., ivy, bonsai) in your space to lower sympathetic nervous system arousal.", 1)
        upper.contains("ACOUSTIC") || upper.contains("SOUND") || upper.contains("WATER") || upper.contains("MASKING") -> BiophilicStepInfo("Introduce sound masking", "Place a tabletop moving water feature or play soothing botanical rain sounds to mask distracting background hums.", 2)
        upper.contains("MATERIAL") || upper.contains("TEXTURE") -> BiophilicStepInfo("Introduce natural textures", "Integrate materials like wood, cork, or a clay plant pot into your setup to stabilize stress baselines.", 1)
        upper.contains("AIR") || upper.contains("VENTILATION") -> BiophilicStepInfo("Enhance room ventilation", "Get 10 minutes of fresh air twice daily, or place a quiet oscillating fan near plants to simulate natural breeze.", 2)
        upper.contains("SENSORY") || upper.contains("SCENT") -> BiophilicStepInfo("Stimulate sensory triggers", "Introduce highly aromatic herbs (lavender, pine, mint) near your workspace. Inhale their scent during breaks.", 2)
        upper.contains("SEASON") -> BiophilicStepInfo("Align with natural cycles", "Adjust light durations to match current seasons. Keep a seasonal plant blooming nearby.", 1)
        else -> BiophilicStepInfo("Enhance natural presence", "Add organic shapes, living greenery, and natural daylight zones to complete your biophilic shelter.", 1)
    }
}

private fun getBiophilicCategoryTip(category: String): Pair<String, String> {
    val info = getBiophilicStepInfo(category)
    return Pair(info.title, info.detail)
}
