package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.ui.viewmodel.GardenViewModel

// Companion Planting Synergy Checking Logic
fun checkPlantSynergy(plant1: String, plant2: String): Boolean {
    val p1 = plant1.lowercase()
    val p2 = plant2.lowercase()
    if (p1 == p2) return false // diversity is key
    
    val companionRules = listOf(
        setOf("rose", "maple"),
        setOf("rose", "lavender"),
        setOf("lavender", "juniper"),
        setOf("cactus", "aloe"),
        setOf("marigold", "ivy"),
        setOf("ivy", "basil"),
        setOf("aster", "thyme"),
        setOf("columbine", "fern"),
        setOf("rosemary", "lavender"),
        setOf("rosemary", "thyme")
    )
    
    return companionRules.any { rule ->
        rule.any { r -> p1.contains(r) } && rule.any { r -> p2.contains(r) }
    }
}

fun checkPlantConflict(plant1: String, plant2: String): Boolean {
    val p1 = plant1.lowercase()
    val p2 = plant2.lowercase()
    if (p1 == p2) return false
    
    val conflictRules = listOf(
        setOf("ivy", "cactus"),         // Moisture conflict
        setOf("fern", "cactus"),        // Moisture conflict
        setOf("lavender", "fern"),      // Sunlight/shade conflict
        setOf("rosemary", "fern"),      // Moisture/shade mismatch
        setOf("basil", "cactus"),       // Water requirements mismatch
        setOf("pothos", "cactus"),      // Water mismatch
        setOf("maple", "cactus"),       // Water requirements mismatch
        setOf("rose", "cactus"),        // Water requirements conflict
        setOf("mint", "rose"),          // Invasive root competition
        setOf("mint", "lavender")       // Moisture conflict
    )
    
    return conflictRules.any { rule ->
        rule.any { r -> p1.contains(r) } && rule.any { r -> p2.contains(r) }
    }
}

fun hasNeighborSynergy(row: Int, col: Int, gridItems: List<GridPlantItem>): Boolean {
    val currentLoc = gridItems.firstOrNull { it.x == row && it.y == col } ?: return false
    val neighbors = listOf(
        Pair(row - 1, col),
        Pair(row + 1, col),
        Pair(row, col - 1),
        Pair(row, col + 1)
    )
    for (n in neighbors) {
        val neighborItem = gridItems.firstOrNull { it.x == n.first && it.y == n.second }
        if (neighborItem != null) {
            if (checkPlantSynergy(currentLoc.plantName, neighborItem.plantName)) {
                return true
            }
        }
    }
    return false
}

fun hasNeighborConflict(row: Int, col: Int, gridItems: List<GridPlantItem>): Boolean {
    val currentLoc = gridItems.firstOrNull { it.x == row && it.y == col } ?: return false
    val neighbors = listOf(
        Pair(row - 1, col),
        Pair(row + 1, col),
        Pair(row, col - 1),
        Pair(row, col + 1)
    )
    for (n in neighbors) {
        val neighborItem = gridItems.firstOrNull { it.x == n.first && it.y == n.second }
        if (neighborItem != null) {
            if (checkPlantConflict(currentLoc.plantName, neighborItem.plantName)) {
                return true
            }
        }
    }
    return false
}

data class SoilTheme(
    val name: String,
    val bgColors: List<Color>,
    val outlineColor: Color,
    val slotBgColor: Color,
    val description: String
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    viewModel: GardenViewModel,
    switchToChatTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeLayout by viewModel.activeLayout.collectAsStateWithLifecycle()
    val activePlants by viewModel.activePlants.collectAsStateWithLifecycle()

    var showCellConfigDialog by remember { mutableStateOf<Pair<Int, Int>?>(null) } // Row, Col of clicked cell
    var highlightedPlantName by remember { mutableStateOf<String?>(null) }
    var showBlueprintDialog by remember { mutableStateOf(false) }
    var showTimelapseDialog by remember { mutableStateOf(false) }
    var triggerLeafFlutter by remember { mutableStateOf(false) }
    var hasTriggeredFullGarden by remember(activeLayout?.id) { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp || configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.smallestScreenWidthDp >= 600
    val isWideScreen = isLandscape && (isTablet || configuration.screenWidthDp >= 600)

    val soilThemes = listOf(
        SoilTheme(
            name = "Loam 🟫",
            bgColors = listOf(Color(0xFFEFEBE9), Color(0xFFD7CCC8)),
            outlineColor = Color(0xFF8D6E63),
            slotBgColor = Color(0xFF5D4037).copy(alpha = 0.08f),
            description = "Rich organic tilled loam soil with excellent moisture absorption."
        ),
        SoilTheme(
            name = "Sand 🟧",
            bgColors = listOf(Color(0xFFFFF8E1), Color(0xFFFFECB3)),
            outlineColor = Color(0xFFFFB74D),
            slotBgColor = Color(0xFFE65100).copy(alpha = 0.08f),
            description = "Coarse gritty sand. Extremely well draining workspace."
        ),
        SoilTheme(
            name = "Pebbles ⬜",
            bgColors = listOf(Color(0xFFECEFF1), Color(0xFFCFD8DC)),
            outlineColor = Color(0xFF78909C),
            slotBgColor = Color(0xFF37474F).copy(alpha = 0.08f),
            description = "Smoothed stones. Best suited for raked Zen spaces."
        )
    )
    var selectedSoilIdx by remember { mutableIntStateOf(0) }
    val currentSoilTheme = soilThemes[selectedSoilIdx]

    val currentLayout = activeLayout
    val activeGridItems = remember(currentLayout?.gridString) {
        parseGridString(currentLayout?.gridString ?: "")
    }

    LaunchedEffect(activeGridItems.size) {
        if (activeGridItems.size == 25 && !hasTriggeredFullGarden) {
            hasTriggeredFullGarden = true
            triggerLeafFlutter = true
        } else if (activeGridItems.size < 25) {
            hasTriggeredFullGarden = false
        }
    }

    val particles = remember { mutableStateListOf<LeafParticle>() }
    LaunchedEffect(triggerLeafFlutter) {
        if (triggerLeafFlutter) {
            particles.clear()
            for (i in 0 until 30) {
                particles.add(
                    LeafParticle(
                        x = (0..100).random().toFloat() / 100f,
                        y = -0.2f - (0..50).random().toFloat() / 100f,
                        vx = ((0..40).random() - 20).toFloat() / 300f,
                        vy = (30..80).random().toFloat() / 300f,
                        scale = (5..15).random().toFloat() / 10f,
                        rotation = (0..360).random().toFloat(),
                        rotationSpeed = ((0..10).random() - 5).toFloat()
                    )
                )
            }
            val start = System.currentTimeMillis()
            while (System.currentTimeMillis() - start < 3500) {
                val updated = particles.map { p ->
                    p.copy(
                        x = (p.x + p.vx).coerceIn(0f, 1f),
                        y = p.y + p.vy,
                        rotation = p.rotation + p.rotationSpeed
                    )
                }
                particles.clear()
                particles.addAll(updated)
                kotlinx.coroutines.delay(16)
            }
            triggerLeafFlutter = false
        }
    }

    if (currentLayout == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Dashboard,
                contentDescription = "No layout",
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Select a plot to begin designing your sanctuary.",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Please select or generate a garden workspace in the Dashboard screen first to layout specific plant variables on the interactive grid blueprint.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    } else {
        val layoutInfoContent = @Composable {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Design Blueprint Workspace",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentLayout.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        val snapshots = remember(currentLayout.id, currentLayout.gridString) {
                            viewModel.getGridSnapshots(currentLayout.id)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (snapshots.size >= 4) {
                                IconButton(
                                    onClick = { showTimelapseDialog = true },
                                    modifier = Modifier.testTag("timelapse_garden_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = "See your garden grow",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    shareGardenSnapshot(context, currentLayout, activeGridItems, currentSoilTheme)
                                },
                                modifier = Modifier.testTag("share_garden_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share My Garden",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Climate: ${currentLayout.climate}. Substrate: ${currentSoilTheme.name}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        val aiArchitectControlsContent = @Composable {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "AI Assistant",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.askAiForLayoutAdvice()
                                switchToChatTab()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = "AI",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Layout Review",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.generateAILayoutSuggestion()
                                switchToChatTab()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.Spa,
                                    contentDescription = "Sow",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Blueprint Suggestion",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        val substrateSelectorContent = @Composable {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Surface Substrate Type:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        soilThemes.forEachIndexed { index, theme ->
                            val isSelected = selectedSoilIdx == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) theme.outlineColor.copy(alpha = 0.12f) else Color.Transparent)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) theme.outlineColor else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedSoilIdx = index }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = theme.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) theme.outlineColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Text(
                        text = currentSoilTheme.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 10.sp
                    )
                }
            }
        }

        val actionsPanelContent = @Composable {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.autoSowClimateSeeds()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.weight(1f).testTag("quick_sow_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Spa, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Auto-Sow Seeds 🌱", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = {
                        viewModel.clearLayoutGrid()
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.weight(1f).testTag("clear_grid_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Uproot All 🧹", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        val gridPlannerContent = @Composable {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Plot Blueprint Editor",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (highlightedPlantName != null) {
                            Text(
                                text = "FILTER ACTIVE 🎯",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC62828),
                                modifier = Modifier
                                    .clickable { highlightedPlantName = null }
                                    .background(Color(0xFFFFEBEE), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Touch any sector block below to choose and assign seeds.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (r in 0..4) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    for (c in 0..4) {
                                        val item = activeGridItems.firstOrNull { it.x == r && it.y == c }
                                        val emoji = getEmojiForPlantName(item?.plantName ?: "")
                                        val isHighlighted = highlightedPlantName != null && item?.plantName == highlightedPlantName
                                        val hasSynergy = item != null && hasNeighborSynergy(r, c, activeGridItems)
                                        val hasConflict = item != null && hasNeighborConflict(r, c, activeGridItems)

                                        val scaleVal by animateFloatAsState(
                                            targetValue = if (isHighlighted) 1.08f else 1f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            ),
                                            label = "gridHighlighter"
                                        )

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .scale(scaleVal)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(
                                                    if (item != null) {
                                                        if (isHighlighted) Color(0xFFFFD54F).copy(alpha = 0.5f)
                                                         else if (hasConflict) Color(0xFFFFEBEE)
                                                         else if (hasSynergy) Color(0xFFE8F5E9)
                                                         else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                    } else {
                                                        currentSoilTheme.slotBgColor
                                                    }
                                                )
                                                .border(
                                                    width = if (isHighlighted) 3.dp else if (hasConflict) 2.2.dp else if (hasSynergy) 2.2.dp else if (item != null) 2.dp else 1.dp,
                                                    color = if (isHighlighted) Color(0xFFF57F17)
                                                    else if (hasConflict) Color(0xFFE53935)
                                                    else if (hasSynergy) Color(0xFF4CAF50)
                                                    else if (item != null) MaterialTheme.colorScheme.primary
                                                    else currentSoilTheme.outlineColor.copy(alpha = 0.4f),
                                                    shape = RoundedCornerShape(16.dp)
                                                )
                                                .clickable { showCellConfigDialog = Pair(r, c) }
                                                .testTag("grid_cell_${r}_${c}"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (item != null) {
                                                 Column(
                                                     horizontalAlignment = Alignment.CenterHorizontally,
                                                     verticalArrangement = Arrangement.Center,
                                                     modifier = Modifier.padding(2.dp)
                                                 ) {
                                                     Box(
                                                         modifier = Modifier
                                                             .size(38.dp)
                                                             .border(
                                                                 width = 1.dp,
                                                                 color = if (hasConflict) Color(0xFFE53935).copy(alpha = 0.4f)
                                                                 else if (hasSynergy) Color(0xFF4CAF50).copy(alpha = 0.4f)
                                                                 else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                                                 shape = CircleShape
                                                             )
                                                             .background(
                                                                 if (hasConflict) Color(0xFFFFCDD2).copy(alpha = 0.4f)
                                                                 else if (hasSynergy) Color(0xFFC8E6C9).copy(alpha = 0.4f)
                                                                 else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                                                 CircleShape
                                                             ),
                                                         contentAlignment = Alignment.Center
                                                     ) {
                                                         Text(emoji, fontSize = 22.sp)
                                                     }
                                                     Spacer(modifier = Modifier.height(2.dp))
                                                     Row(
                                                         verticalAlignment = Alignment.CenterVertically,
                                                         horizontalArrangement = Arrangement.Center
                                                     ) {
                                                         Text(
                                                             text = item.plantName.take(6),
                                                             fontSize = 8.5.sp,
                                                             fontWeight = FontWeight.ExtraBold,
                                                             color = if (hasConflict) Color(0xFFC62828) else if (hasSynergy) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                                                             textAlign = TextAlign.Center
                                                         )
                                                         if (hasConflict) {
                                                             Text("⚠️", fontSize = 7.sp, modifier = Modifier.padding(start = 1.dp))
                                                         } else if (hasSynergy) {
                                                             Text("✨", fontSize = 7.sp, modifier = Modifier.padding(start = 1.dp))
                                                         }
                                                     }
                                                 }
                                            } else {
                                                 Box(
                                                     modifier = Modifier
                                                         .size(26.dp)
                                                         .border(
                                                             width = 1.dp,
                                                             color = currentSoilTheme.outlineColor.copy(alpha = 0.15f),
                                                             shape = CircleShape
                                                         ),
                                                     contentAlignment = Alignment.Center
                                                 ) {
                                                     Icon(
                                                         Icons.Default.Add,
                                                         contentDescription = "Empty Plot",
                                                         tint = currentSoilTheme.outlineColor.copy(alpha = 0.5f),
                                                         modifier = Modifier.size(12.dp)
                                                     )
                                                 }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (triggerLeafFlutter) {
                            androidx.compose.foundation.Canvas(
                                modifier = Modifier.matchParentSize()
                            ) {
                                particles.forEach { p ->
                                    val px = p.x * size.width
                                    val py = p.y * size.height
                                    
                                    drawContext.canvas.save()
                                    drawContext.canvas.translate(px, py)
                                    drawContext.canvas.rotate(p.rotation)
                                    drawContext.canvas.scale(p.scale, p.scale)
                                    
                                    val path = androidx.compose.ui.graphics.Path().apply {
                                        moveTo(0f, -10f)
                                        quadraticTo(10f, 0f, 0f, 10f)
                                        quadraticTo(-10f, 0f, 0f, -10f)
                                        close()
                                    }
                                    drawPath(path, color = Color(0xFF4CAF50).copy(alpha = 0.7f))
                                    
                                    drawContext.canvas.restore()
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        "Legend (Tap to highlight on layout):",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.Start).padding(bottom = 6.dp)
                    )
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val distinctPlanted = activeGridItems.map { it.plantName }.distinct()
                        if (distinctPlanted.isEmpty()) {
                             item {
                                 Text(
                                     "This plot is waiting for its first seeds. Tap the grid below to sow your garden.",
                                     style = MaterialTheme.typography.bodySmall,
                                     color = MaterialTheme.colorScheme.secondary,
                                     fontSize = 11.sp
                                 )
                             }
                        } else {
                             items(distinctPlanted) { name ->
                                 val isSelected = highlightedPlantName == name
                                 val emoji = getEmojiForPlantName(name)
                                 Row(
                                     modifier = Modifier
                                         .clip(RoundedCornerShape(8.dp))
                                         .background(
                                             if (isSelected) MaterialTheme.colorScheme.primary
                                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                         )
                                         .clickable {
                                             highlightedPlantName = if (isSelected) null else name
                                         }
                                         .padding(horizontal = 10.dp, vertical = 6.dp),
                                     verticalAlignment = Alignment.CenterVertically
                                 ) {
                                     Text(emoji, fontSize = 12.sp)
                                     Spacer(modifier = Modifier.width(5.dp))
                                     Text(
                                         text = name,
                                         fontSize = 11.sp,
                                         fontWeight = FontWeight.Bold,
                                         color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                     )
                                 }
                             }
                        }
                    }
                }
            }
        }

        val progressDensityContent = @Composable {
            val countPlanted = activeGridItems.size
            val densityPercentage = (countPlanted * 100) / 25
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Yard Spacing Utilisation Density",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "$countPlanted / 25 Slots ($densityPercentage%)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                     }
                     Spacer(modifier = Modifier.height(8.dp))
                     LinearProgressIndicator(
                         progress = { countPlanted.toFloat() / 25f },
                         modifier = Modifier
                             .fillMaxWidth()
                             .height(8.dp)
                             .clip(RoundedCornerShape(4.dp)),
                         color = MaterialTheme.colorScheme.primary,
                         trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                     )
                }
            }
        }

        val cadBlueprintExportContent = @Composable {
            Button(
                onClick = { showBlueprintDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.fillMaxWidth().testTag("view_blueprint_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("RENDER ARCHITECT CAD BLUEPRINT 📐", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        if (!isWideScreen) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                layoutInfoContent()
                aiArchitectControlsContent()
                substrateSelectorContent()
                actionsPanelContent()
                gridPlannerContent()
                progressDensityContent()
                cadBlueprintExportContent()
                Spacer(modifier = Modifier.height(16.dp))
            }
        } else {
            Row(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    gridPlannerContent()
                    progressDensityContent()
                    cadBlueprintExportContent()
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Column(
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    layoutInfoContent()
                    aiArchitectControlsContent()
                    substrateSelectorContent()
                    actionsPanelContent()
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // --- Cell Configuration Dialog ---
    val cellDialogCoords = showCellConfigDialog
    if (cellDialogCoords != null) {
        val row = cellDialogCoords.first
        val col = cellDialogCoords.second
        val currentOccupant = activeGridItems.firstOrNull { it.x == row && it.y == col }
        val hasSynergy = currentOccupant != null && hasNeighborSynergy(row, col, activeGridItems)
        val hasConflict = currentOccupant != null && hasNeighborConflict(row, col, activeGridItems)

        Dialog(onDismissRequest = { showCellConfigDialog = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Plot Dimension Details (${row + 1}, ${col + 1})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (currentOccupant != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (hasConflict) Color(0xFFFFEBEE) else if (hasSynergy) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Occupant: ${currentOccupant.plantName} ${getEmojiForPlantName(currentOccupant.plantName)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (hasConflict) Color(0xFFC62828) else if (hasSynergy) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                            )
                            if (hasSynergy) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "✨ COMPANION SYNERGY ACTIVE: This species benefits beautifully from neighboring root enzymes and shared microclimate shade!",
                                    fontSize = 11.sp,
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 15.sp
                                )
                            }
                            if (hasConflict) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "⚠️ COMPANION CONFLICT WARNING: Staged too close to an incompatible species! These plants compete for nutrients, attract pests, or clash in water needs.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFC62828),
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    Text(
                        text = "Change plant or insert a custom vegetation seed below:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    // Selection lists
                    Text("Recommended Climate Seed Bank:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 140.dp)
                    ) {
                        val defaultRecommendations = ClimatePlants.getTemplatesForClimate(activeLayout?.climate ?: "")
                        val allAvailablePlants = (activePlants.map { it.name } + defaultRecommendations.map { it.name }).distinct()

                        items(allAvailablePlants) { pName ->
                            val tpl = defaultRecommendations.firstOrNull { it.name == pName }
                            val emoji = getEmojiForPlantName(pName)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.placeGridPlant(row, col, pName)
                                        // Auto add to inventory if not there
                                        if (activePlants.none { it.name == pName }) {
                                            viewModel.addPlant(pName, tpl?.type ?: "Flower", tpl)
                                        }
                                        showCellConfigDialog = null
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(8.dp)
                             ) {
                                 Row(
                                     modifier = Modifier.padding(8.dp),
                                     verticalAlignment = Alignment.CenterVertically
                                 ) {
                                     Text(emoji, fontSize = 16.sp)
                                     Spacer(modifier = Modifier.width(6.dp))
                                     Text(pName, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                 }
                             }
                        }
                    }

                    HorizontalDivider()

                    // Text Field for Custom Plant name
                    var customName by remember { mutableStateOf("") }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            placeholder = { Text("Custom, e.g. Tulips") },
                            modifier = Modifier.weight(1f).testTag("custom_grid_plant_input"),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (customName.isNotBlank()) {
                                    viewModel.placeGridPlant(row, col, customName)
                                    viewModel.addPlant(customName, "Flower", null)
                                    showCellConfigDialog = null
                                }
                            },
                            enabled = customName.isNotBlank(),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("submit_custom_grid_plant")
                        ) {
                            Text("Add")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentOccupant != null) {
                            TextButton(
                                onClick = {
                                    viewModel.placeGridPlant(row, col, "") // Empty removes
                                    showCellConfigDialog = null
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Uproot")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Uproot")
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        TextButton(onClick = { showCellConfigDialog = null }) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }

    // --- Cyber CAD Blueprint Render Dialog ---
    if (showBlueprintDialog) {
        Dialog(onDismissRequest = { showBlueprintDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF031604), // Immersive deep cyber blueprints
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .border(2.dp, Color(0xFF4CAF50), RoundedCornerShape(20.dp))
            ) {
                Column(
                     modifier = Modifier
                         .padding(20.dp)
                         .verticalScroll(rememberScrollState()),
                     verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                     Row(
                         modifier = Modifier.fillMaxWidth(),
                         horizontalArrangement = Arrangement.SpaceBetween,
                         verticalAlignment = Alignment.Top
                     ) {
                          Column {
                              Text(
                                  text = "FLORAFLOW CAD SHEET PL-5",
                                  color = Color(0xFF81C784),
                                  fontSize = 9.sp,
                                  fontWeight = FontWeight.Bold,
                                  fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                              )
                              Text(
                                  text = "Vector Blueprint",
                                  color = Color.White,
                                  fontSize = 20.sp,
                                  fontWeight = FontWeight.Black
                              )
                          }
                          IconButton(onClick = { showBlueprintDialog = false }) {
                              Icon(
                                  Icons.Default.Close,
                                  contentDescription = "Close Blueprint Overlay",
                                  tint = Color.White
                              )
                          }
                     }

                     HorizontalDivider(color = Color(0xFF4CAF50).copy(alpha = 0.4f))

                     Column(
                         modifier = Modifier
                             .fillMaxWidth()
                             .background(Color.Black.copy(alpha = 0.5f))
                             .padding(12.dp)
                             .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.25f)),
                         verticalArrangement = Arrangement.spacedBy(4.dp)
                     ) {
                          val formatTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
                          Text("PROJECT-NAME : ${currentLayout?.name?.uppercase() ?: ""}", color = Color(0xFF81C784), fontSize = 9.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                          Text("STYLE-THEME : ${currentLayout?.style?.uppercase() ?: ""}", color = Color(0xFF81C784), fontSize = 9.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                          Text("ZONE-CLIMATE: ${currentLayout?.climate?.uppercase() ?: ""}", color = Color(0xFF81C784), fontSize = 9.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                          Text("SOIL-DENSITY: ${currentSoilTheme.name.uppercase()}", color = Color(0xFF81C784), fontSize = 9.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                          Text("TIMESTAMP   : ${formatTime.format(java.util.Date())} UTC", color = Color(0xFF81C784), fontSize = 9.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                     }

                     Text(
                         "5X5 GRID MAP SYSTEM:",
                         color = Color.White,
                         fontSize = 11.sp,
                         fontWeight = FontWeight.Bold,
                         fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                     )

                     Column(
                         modifier = Modifier
                             .fillMaxWidth()
                             .background(Color.Black)
                             .padding(14.dp)
                             .border(1.dp, Color(0xFF4CAF50)),
                         verticalArrangement = Arrangement.spacedBy(4.dp),
                         horizontalAlignment = Alignment.CenterHorizontally
                     ) {
                          for (r in 0..4) {
                              var rowStr = "["
                              for (c in 0..4) {
                                  val item = activeGridItems.firstOrNull { it.x == r && it.y == c }
                                  rowStr += if (item != null) " ${getEmojiForPlantName(item.plantName)} " else " . "
                              }
                              rowStr += "]"
                              Text(
                                  text = rowStr,
                                  color = Color(0xFF4CAF50),
                                  fontSize = 14.sp,
                                  fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                  fontWeight = FontWeight.Bold,
                                  letterSpacing = 2.sp
                              )
                          }
                     }

                     Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                          Text(
                              "SECTOR LIST & SYNERGY AUDIT:",
                              color = Color.White,
                              fontSize = 11.sp,
                              fontWeight = FontWeight.Bold
                          )
                          if (activeGridItems.isEmpty()) {
                              Text(
                                  "A quiet patch of soil. Tap the grid to plant your first seeds and watch them grow.",
                                  color = Color.Gray,
                                  fontSize = 10.sp,
                                  fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                              )
                          } else {
                              activeGridItems.forEach { item ->
                                  val synergy = hasNeighborSynergy(item.x, item.y, activeGridItems)
                                  val synergyDetails = if (synergy) " ✅ [COMPANION SYNERGY ACTIVE]" else ""
                                  Text(
                                      "-> PLOT [${item.x + 1}, ${item.y + 1}]: ${item.plantName} ${getEmojiForPlantName(item.plantName)} $synergyDetails",
                                      color = if (synergy) Color(0xFFFFD54F) else Color(0xFFE8F5E9),
                                      fontSize = 10.sp,
                                      fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                  )
                              }
                          }
                     }

                     Spacer(modifier = Modifier.height(6.dp))

                     var isSimulatingSave by remember { mutableStateOf(false) }
                     val currentContext = androidx.compose.ui.platform.LocalContext.current

                     Button(
                         onClick = {
                             isSimulatingSave = true
                         },
                         modifier = Modifier.fillMaxWidth(),
                         colors = ButtonDefaults.buttonColors(
                             containerColor = Color(0xFF4CAF50),
                             contentColor = Color.Black
                         ),
                         shape = RoundedCornerShape(10.dp)
                     ) {
                         if (isSimulatingSave) {
                             CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp))
                             LaunchedEffect(Unit) {
                                 kotlinx.coroutines.delay(1500)
                                 isSimulatingSave = false
                                 android.widget.Toast.makeText(currentContext, "Blueprint saved as FloraFlow_Blueprint.cad 📂✨", android.widget.Toast.LENGTH_SHORT).show()
                                 showBlueprintDialog = false
                             }
                         } else {
                             Icon(Icons.Default.Download, contentDescription = "Download")
                             Spacer(modifier = Modifier.width(8.dp))
                             Text(
                                 "DOWNLOAD VECTOR BLUEPRINT 💾",
                                 fontWeight = FontWeight.ExtraBold,
                                 fontSize = 11.sp
                             )
                         }
                     }
                 }
             }
         }
     }

     if (showTimelapseDialog && currentLayout != null) {
          val snapshots = remember(currentLayout.id, currentLayout.gridString) {
              viewModel.getGridSnapshots(currentLayout.id)
          }
          TimelapseDialog(
              snapshots = snapshots,
              onDismiss = { showTimelapseDialog = false }
          )
      }
}

// Utility to assign emojis based on plant name keywords
fun getEmojiForPlantName(name: String): String {
    val low = name.lowercase()
    
    // First, look for exact or substring matches in ClimatePlants.ALL_TEMPLATES
    val exactMatch = ClimatePlants.ALL_TEMPLATES.find { it.name.equals(name, ignoreCase = true) }
    if (exactMatch != null) return exactMatch.iconEmoji
    
    val substringMatch = ClimatePlants.ALL_TEMPLATES.find {
        name.contains(it.name, ignoreCase = true) || it.name.contains(name, ignoreCase = true)
    }
    if (substringMatch != null) return substringMatch.iconEmoji

    return when {
         low.contains("bonsai") -> "🪴"
         low.contains("juniper") -> "🌲"
         low.contains("rose") -> "🌹"
         low.contains("cact") || low.contains("aloe") -> "🌵"
         low.contains("marigold") || low.contains("aster") -> "🌼"
         low.contains("lavender") -> "🪻"
         low.contains("orchid") -> "🌸"
         low.contains("hibiscus") || low.contains("bougainvillea") -> "🌺"
         low.contains("maple") || low.contains("tree") -> "🌳"
         low.contains("fern") || low.contains("monstera") || low.contains("shoot") -> "🌿"
         low.contains("basil") || low.contains("rosemary") || low.contains("herb") || low.contains("thyme") -> "🌱"
         else -> "🌱"
    }
}

fun shareGardenSnapshot(
    context: android.content.Context,
    layout: GardenLayout,
    gridItems: List<GridPlantItem>,
    soilTheme: SoilTheme
) {
    val size = 1000
    val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    
    val bgPaint = android.graphics.Paint().apply {
        color = 0xFFFCF9F1.toInt()
        style = android.graphics.Paint.Style.FILL
    }
    canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)
    
    val borderPaint = android.graphics.Paint().apply {
        color = 0xFF1F483E.toInt()
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 16f
    }
    canvas.drawRect(20f, 20f, size.toFloat() - 20f, size.toFloat() - 20f, borderPaint)
    
    val thinBorderPaint = android.graphics.Paint().apply {
        color = 0xFF1F483E.toInt()
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 4f
    }
    canvas.drawRect(32f, 32f, size.toFloat() - 32f, size.toFloat() - 32f, thinBorderPaint)

    val titlePaint = android.graphics.Paint().apply {
        color = 0xFF1F483E.toInt()
        textSize = 48f
        isFakeBoldText = true
        isAntiAlias = true
    }
    canvas.drawText("FloraFlow Botanical Layout", 60f, 100f, titlePaint)
    
    val namePaint = android.graphics.Paint().apply {
        color = 0xFF1B1C17.toInt()
        textSize = 36f
        isFakeBoldText = true
        isAntiAlias = true
    }
    canvas.drawText("Garden: ${layout.name}", 60f, 150f, namePaint)
    
    val subtitlePaint = android.graphics.Paint().apply {
        color = 0xFF43493E.toInt()
        textSize = 28f
        isAntiAlias = true
    }
    canvas.drawText("Theme: ${layout.style} | Climate: ${layout.climate}", 60f, 190f, subtitlePaint)
    canvas.drawText("Substrate: ${soilTheme.name}", 60f, 225f, subtitlePaint)

    val gridStartX = 150f
    val gridStartY = 280f
    val gridWidth = 700f
    val cellSize = gridWidth / 5f
    
    val gridBgPaint = android.graphics.Paint().apply {
        color = 0xFFE2E6D5.toInt()
        style = android.graphics.Paint.Style.FILL
    }
    canvas.drawRect(gridStartX, gridStartY, gridStartX + gridWidth, gridStartY + gridWidth, gridBgPaint)
    
    val gridLinePaint = android.graphics.Paint().apply {
        color = 0xFFCBD0BE.toInt()
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 6f
    }
    
    for (r in 0..5) {
        val y = gridStartY + r * cellSize
        canvas.drawLine(gridStartX, y, gridStartX + gridWidth, y, gridLinePaint)
    }
    for (c in 0..5) {
        val x = gridStartX + c * cellSize
        canvas.drawLine(x, gridStartY, x, gridStartY + gridWidth, gridLinePaint)
    }
    
    val emojiPaint = android.graphics.Paint().apply {
        textSize = 64f
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
    }
    
    val cellLabelPaint = android.graphics.Paint().apply {
        color = 0xFF1F483E.toInt()
        textSize = 16f
        isFakeBoldText = true
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
    }

    for (item in gridItems) {
        val cellCenterX = gridStartX + item.y * cellSize + cellSize / 2f
        val cellCenterY = gridStartY + item.x * cellSize + cellSize / 2f
        
        val emoji = getEmojiForPlantName(item.plantName)
        canvas.drawText(emoji, cellCenterX, cellCenterY + 12f, emojiPaint)
        
        val displayName = if (item.plantName.length > 8) item.plantName.take(7) + ".." else item.plantName
        canvas.drawText(displayName, cellCenterX, cellCenterY + cellSize / 2f - 12f, cellLabelPaint)
    }
    
    try {
        val logoSrc = android.graphics.BitmapFactory.decodeResource(context.resources, com.example.R.drawable.ic_logo_heart)
        if (logoSrc != null) {
            val scaledLogo = android.graphics.Bitmap.createScaledBitmap(logoSrc, 80, 80, true)
            canvas.drawBitmap(scaledLogo, size - 260f, size - 140f, null)
        }
    } catch (_: Exception) {}
    
    val watermarkTitlePaint = android.graphics.Paint().apply {
        color = 0xFF1F483E.toInt()
        textSize = 30f
        isFakeBoldText = true
        isAntiAlias = true
    }
    canvas.drawText("FloraFlow", size - 160f, size - 100f, watermarkTitlePaint)
    
    val watermarkSubPaint = android.graphics.Paint().apply {
        color = 0xFF43493E.toInt()
        textSize = 18f
        isAntiAlias = true
    }
    canvas.drawText("Therapeutic Garden", size - 220f, size - 50f, watermarkSubPaint)
    
    val totalPlants = gridItems.size
    val synergyCount = gridItems.count { hasNeighborSynergy(it.x, it.y, gridItems) }
    
    val statsPaint = android.graphics.Paint().apply {
        color = 0xFF1F483E.toInt()
        textSize = 24f
        isFakeBoldText = true
        isAntiAlias = true
    }
    canvas.drawText("Tended Plants: $totalPlants", 60f, size - 110f, statsPaint)
    canvas.drawText("Active Synergies: $synergyCount ✨", 60f, size - 70f, statsPaint)
    
    try {
        val cachePath = java.io.File(context.cacheDir, "shared_gardens")
        cachePath.mkdirs()
        val file = java.io.File(cachePath, "garden_snapshot.png")
        val stream = java.io.FileOutputStream(file)
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()
        
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "My FloraFlow Sanctuary")
            putExtra(android.content.Intent.EXTRA_TEXT, "Look at the therapeutic sanctuary I just designed using FloraFlow! 🌸🌿 #FloraFlow")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share My Garden"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Failed to share: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}

data class LeafParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val scale: Float,
    val rotation: Float,
    val rotationSpeed: Float
)

@Composable
fun TimelapseDialog(
    snapshots: List<Pair<Long, String>>,
    onDismiss: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                kotlinx.coroutines.delay(1000)
                currentIndex = (currentIndex + 1) % snapshots.size
            }
        }
    }

    val currentSnapshot = snapshots.getOrNull(currentIndex)
    val gridItems = remember(currentSnapshot) {
        parseGridString(currentSnapshot?.second ?: "")
    }
    val timestamp = currentSnapshot?.first ?: 0L
    val dateStr = remember(timestamp) {
        if (timestamp == 0L) "" else {
            val sdf = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
            sdf.format(java.util.Date(timestamp))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = {
            Text("See Your Garden Grow 🌿", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Snapshot ${currentIndex + 1} of ${snapshots.size} • $dateStr",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary
                )

                // Render 5x5 preview grid
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFE2E6D5))
                        .padding(8.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        for (r in 0..4) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                for (c in 0..4) {
                                    val item = gridItems.firstOrNull { it.x == r && it.y == c }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (item != null) Color(0xFFC8E6C9)
                                                else Color(0xFFCBD0BE).copy(alpha = 0.5f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (item != null) {
                                            Text(getEmojiForPlantName(item.plantName), fontSize = 16.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Controls
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = {
                            currentIndex = (currentIndex - 1 + snapshots.size) % snapshots.size
                            isPlaying = false
                        }
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
                    }
                    IconButton(
                        onClick = { isPlaying = !isPlaying }
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play"
                        )
                    }
                    IconButton(
                        onClick = {
                            currentIndex = (currentIndex + 1) % snapshots.size
                            isPlaying = false
                        }
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next")
                    }
                }

                // Slider
                Slider(
                    value = currentIndex.toFloat(),
                    onValueChange = {
                        currentIndex = it.toInt().coerceIn(0, snapshots.size - 1)
                        isPlaying = false
                    },
                    valueRange = 0f..(snapshots.size - 1).toFloat(),
                    steps = if (snapshots.size > 2) snapshots.size - 2 else 0,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}
