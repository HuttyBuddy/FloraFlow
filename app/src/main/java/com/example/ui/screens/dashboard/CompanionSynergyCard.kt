package com.example.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GardenLayout
import com.example.data.model.GridPlantItem
import com.example.data.model.parseGridString
import com.example.ui.theme.extendedColors
import com.example.ui.screens.checkPlantSynergy
import com.example.ui.screens.checkPlantConflict
import kotlin.math.sqrt

@Composable
fun CompanionSynergyCard(
    activeLayout: GardenLayout?,
    modifier: Modifier = Modifier
) {
    if (activeLayout == null) return

    val gridItems = remember(activeLayout.gridString) {
        parseGridString(activeLayout.gridString)
    }

    var expanded by remember { mutableStateOf(false) }

    // Pairwise companion checks for adjacent plants (distance <= 1.5, squared distance <= 2.25)
    val (synergies, conflicts) = remember(gridItems) {
        val synergiesList = mutableListOf<Pair<GridPlantItem, GridPlantItem>>()
        val conflictsList = mutableListOf<Pair<GridPlantItem, GridPlantItem>>()
        for (i in 0 until gridItems.size) {
            for (j in i + 1 until gridItems.size) {
                val item1 = gridItems[i]
                val item2 = gridItems[j]

                val dx = item1.x - item2.x
                if (dx > 1 || dx < -1) continue

                val dy = item1.y - item2.y
                if (dy > 1 || dy < -1) continue

                val distSq = dx * dx + dy * dy
                if (distSq <= 2.25) {
                    if (checkPlantSynergy(item1.plantName, item2.plantName)) {
                        synergiesList.add(Pair(item1, item2))
                    }
                    if (checkPlantConflict(item1.plantName, item2.plantName)) {
                        conflictsList.add(Pair(item1, item2))
                    }
                }
            }
        }
        Pair(synergiesList, conflictsList)
    }

    val hasIssues = conflicts.isNotEmpty()
    val hasSynergies = synergies.isNotEmpty()

    val cardBorderBrush = Brush.horizontalGradient(
        if (hasIssues) {
            listOf(Color(0xFFC62828).copy(alpha = 0.8f), MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
        } else if (hasSynergies) {
            listOf(Color(0xFF2E7D32).copy(alpha = 0.8f), MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
        } else {
            listOf(MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f), Color.Transparent)
        }
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("companion_synergy_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, cardBorderBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (hasIssues) Icons.Default.Warning else Icons.Default.Eco,
                        contentDescription = null,
                        tint = if (hasIssues) Color(0xFFC62828) else if (hasSynergies) Color(0xFF2E7D32) else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Companion Compatibility Insights",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (gridItems.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (hasIssues) Color(0xFFFFEBEE) else if (hasSynergies) Color(0xFFE8F5E9) else Color(0xFFE2ECE9),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (hasIssues) "${conflicts.size} Mismatches" else if (hasSynergies) "${synergies.size} Synergies" else "Stable",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (hasIssues) Color(0xFFC62828) else if (hasSynergies) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (gridItems.isEmpty()) {
                Text(
                    text = "No plants sowed in this layout grid yet. Visit the Garden Planner tab to sow companion seeds like Lavender and Rose to unlock companion synergies!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Sowed Vegetation: ${gridItems.size} items in '${activeLayout.name}'. We analyze adjacent cells to ensure optimal biophilic coexistence.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

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
                        text = "View Grid Companionship Details",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse companionship" else "Expand companionship",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                AnimatedVisibility(visible = expanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (synergies.isEmpty() && conflicts.isEmpty()) {
                            Text(
                                text = "✨ The sowed plants are neutral and have stable companion indexes. Sow Lavender + Rosemary or Cactus + Aloe next to each other to activate dynamic biophilic synergies (+15% Growth progress bonus).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        synergies.forEach { (item1, item2) ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.extendedColors.success.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                    .padding(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Synergy Check",
                                        tint = MaterialTheme.extendedColors.success,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Active Synergy: ${item1.plantName} & ${item2.plantName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.extendedColors.success
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Symbiotic companion pairing detected at grid cells (${item1.x + 1}, ${item1.y + 1}) & (${item2.x + 1}, ${item2.y + 1}). Restorative bio-fragrances sync to enhance room biophilic energy.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        conflicts.forEach { (item1, item2) ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.extendedColors.error.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                    .padding(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = "Conflict Cancel",
                                        tint = MaterialTheme.extendedColors.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Mismatched Partnership: ${item1.plantName} & ${item2.plantName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.extendedColors.error
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Soil moisture or sun exposure requirements conflict at cells (${item1.x + 1}, ${item1.y + 1}) & (${item2.x + 1}, ${item2.y + 1}). Consider moving them apart to optimize care schedules.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
