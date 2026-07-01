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
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.nativeCanvas

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

    var selectedSeedTemplate by remember { mutableStateOf<PlantTemplate?>(null) }
    var isUprootModeActive by remember { mutableStateOf(false) }
    var selectedTrayTab by remember { mutableStateOf(0) } // 0 = Indoor, 1 = Outdoor

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
            description = "Smoothed stones. Best suited for raked spaces."
        ),
        SoilTheme(
            name = "Clay 🟥",
            bgColors = listOf(Color(0xFFF4A460), Color(0xFFCD853F)),
            outlineColor = Color(0xFFD2691E),
            slotBgColor = Color(0xFF8B4513).copy(alpha = 0.08f),
            description = "Heavy nutrient-rich clay. Retains moisture for water-loving plants."
        ),
        SoilTheme(
            name = "Mulch 🟫",
            bgColors = listOf(Color(0xFF4E342E), Color(0xFF271C19)),
            outlineColor = Color(0xFF6D4C41),
            slotBgColor = Color(0xFF3E2723).copy(alpha = 0.08f),
            description = "Organic wood chips and compost. Great for weed prevention and warmth."
        )
    )
    var selectedSoilIdx by remember { mutableIntStateOf(0) }
    val currentSoilTheme = soilThemes[selectedSoilIdx]

    val currentLayout = activeLayout
    val activeGridItems = remember(currentLayout?.gridString) {
        parseGridString(currentLayout?.gridString ?: "")
    }

    var hasConsultedAi by remember { mutableStateOf(false) }
    var hasExportedBlueprint by remember { mutableStateOf(false) }

    val expandedTasks = remember { mutableStateMapOf<Int, Boolean>().apply {
        put(1, false)
        put(2, true)
        put(3, false)
        put(4, false)
        put(5, false)
    } }

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
            Column(
                modifier = Modifier.fillMaxWidth(),
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
                            hasConsultedAi = true
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
                            hasConsultedAi = true
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

        val substrateSelectorContent = @Composable {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Surface Substrate Type",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        soilThemes.forEachIndexed { index, theme ->
                            val isSelected = selectedSoilIdx == index
                            val animatedScale by animateFloatAsState(
                                targetValue = if (isSelected) 1.03f else 1.0f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                label = "substrateScale"
                            )
                            val animatedElevation by animateDpAsState(
                                targetValue = if (isSelected) 4.dp else 0.dp,
                                label = "substrateElevation"
                            )
                            
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .scale(animatedScale)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedSoilIdx = index },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                                ),
                                border = if (isSelected) {
                                    BorderStroke(2.dp, theme.outlineColor)
                                } else {
                                    null
                                },
                                elevation = CardDefaults.cardElevation(defaultElevation = animatedElevation)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Mini Substrate Preview Circle
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, theme.outlineColor.copy(alpha = 0.4f), CircleShape)
                                    ) {
                                        val colors = theme.bgColors
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            drawRect(Brush.verticalGradient(colors))
                                            // Speckle effects based on index
                                            val random = java.util.Random((index * 99).toLong())
                                            when (index) {
                                                0 -> { // Loam speckles
                                                    for (i in 0..10) {
                                                        drawCircle(
                                                            color = Color(0xFF3E2723).copy(alpha = 0.3f),
                                                            radius = 1f + random.nextFloat() * 1.5f,
                                                            center = Offset(random.nextFloat() * size.width, random.nextFloat() * size.height)
                                                        )
                                                    }
                                                }
                                                1 -> { // Sand grit
                                                    for (i in 0..15) {
                                                        drawCircle(
                                                            color = Color(0xFFFFB74D).copy(alpha = 0.5f),
                                                            radius = 0.8f,
                                                            center = Offset(random.nextFloat() * size.width, random.nextFloat() * size.height)
                                                        )
                                                    }
                                                }
                                                2 -> { // Rocks & Pebbles - draw overlapping smooth stones (ovals/circles with subtle borders)
                                                    for (i in 0..8) {
                                                        val centerX = random.nextFloat() * size.width
                                                        val centerY = random.nextFloat() * size.height
                                                        val w = 5f + random.nextFloat() * 5f
                                                        val h = 4f + random.nextFloat() * 4f
                                                        
                                                        // Draw pebble body
                                                        drawOval(
                                                            color = Color(0xFFB0BEC5),
                                                            topLeft = Offset(centerX - w/2, centerY - h/2),
                                                            size = androidx.compose.ui.geometry.Size(w, h)
                                                        )
                                                        // Draw pebble highlight/texture
                                                        drawOval(
                                                            color = Color(0xFFECEFF1),
                                                            topLeft = Offset(centerX - w/2 + 1f, centerY - h/2 + 1f),
                                                            size = androidx.compose.ui.geometry.Size(w * 0.6f, h * 0.6f)
                                                        )
                                                        // Draw subtle pebble outline
                                                        drawOval(
                                                            color = Color(0xFF78909C),
                                                            topLeft = Offset(centerX - w/2, centerY - h/2),
                                                            size = androidx.compose.ui.geometry.Size(w, h),
                                                            style = Stroke(width = 0.8f)
                                                        )
                                                    }
                                                }
                                                3 -> { // Clay - Terracotta detail
                                                    for (i in 0..5) {
                                                        drawCircle(
                                                            color = Color(0xFF8B4513).copy(alpha = 0.4f),
                                                            radius = 2f + random.nextFloat() * 3f,
                                                            center = Offset(random.nextFloat() * size.width, random.nextFloat() * size.height)
                                                        )
                                                    }
                                                }
                                                4 -> { // Mulch - wood chips
                                                    for (i in 0..8) {
                                                        val x1 = random.nextFloat() * size.width
                                                        val y1 = random.nextFloat() * size.height
                                                        val angle = random.nextFloat() * 2 * Math.PI
                                                        val len = 6f + random.nextFloat() * 6f
                                                        val x2 = x1 + (Math.cos(angle) * len).toFloat()
                                                        val y2 = y1 + (Math.sin(angle) * len).toFloat()
                                                        drawLine(
                                                            color = Color(0xFF3E2723),
                                                            start = Offset(x1, y1),
                                                            end = Offset(x2, y2),
                                                            strokeWidth = 2.5f
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    Text(
                                        text = theme.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) theme.outlineColor else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    val specText = when (index) {
                                        0 -> "Rich Nutrients"
                                        1 -> "Rapid Drain"
                                        2 -> "Rocks or Pebbles"
                                        3 -> "Moisture Heavy"
                                        else -> "Organic Shield"
                                    }
                                    Text(
                                        text = specText,
                                        fontSize = 8.sp,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                    
                    // Description Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                tint = currentSoilTheme.outlineColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentSoilTheme.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                lineHeight = 13.sp
                            )
                        }
                    }
            }
        }

        val actionsPanelContent = @Composable {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                    // Auto-Sow Seeds button
                    Button(
                        onClick = {
                            viewModel.autoSowClimateSeeds()
                            triggerLeafFlutter = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_sow_button")
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF2E7D32),
                                        Color(0xFF4CAF50)
                                    )
                                )
                            ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.Spa, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Auto-Sow Seeds 🌱", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    // Uproot All button
                    Button(
                        onClick = {
                            viewModel.clearLayoutGrid()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("clear_grid_button")
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFC62828),
                                        Color(0xFFE53935)
                                    )
                                )
                            ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Uproot All 🧹", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
            }
        }

        val gridPlannerContent = @Composable {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (highlightedPlantName != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
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

                    Box(modifier = Modifier.fillMaxWidth()) {
                        val gridMap = remember(activeGridItems) {
                            activeGridItems.associateBy { (it.x shl 16) or it.y }
                        }

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
                                        val item = gridMap[(r shl 16) or c]
                                        val emoji = getEmojiForPlantName(item?.plantName ?: "")
                                        val isHighlighted = highlightedPlantName != null && item?.plantName == highlightedPlantName

                                        var hasSynergy = false
                                        var hasConflict = false
                                        if (item != null) {
                                            val n1 = gridMap[((r - 1) shl 16) or c]
                                            val n2 = gridMap[((r + 1) shl 16) or c]
                                            val n3 = gridMap[(r shl 16) or (c - 1)]
                                            val n4 = gridMap[(r shl 16) or (c + 1)]

                                            if (n1 != null) {
                                                if (!hasSynergy) hasSynergy = checkPlantSynergy(item.plantName, n1.plantName)
                                                if (!hasConflict) hasConflict = checkPlantConflict(item.plantName, n1.plantName)
                                            }
                                            if (n2 != null && !(hasSynergy && hasConflict)) {
                                                if (!hasSynergy) hasSynergy = checkPlantSynergy(item.plantName, n2.plantName)
                                                if (!hasConflict) hasConflict = checkPlantConflict(item.plantName, n2.plantName)
                                            }
                                            if (n3 != null && !(hasSynergy && hasConflict)) {
                                                if (!hasSynergy) hasSynergy = checkPlantSynergy(item.plantName, n3.plantName)
                                                if (!hasConflict) hasConflict = checkPlantConflict(item.plantName, n3.plantName)
                                            }
                                            if (n4 != null && !(hasSynergy && hasConflict)) {
                                                if (!hasSynergy) hasSynergy = checkPlantSynergy(item.plantName, n4.plantName)
                                                if (!hasConflict) hasConflict = checkPlantConflict(item.plantName, n4.plantName)
                                            }
                                        }

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
                                                .border(
                                                    width = if (isHighlighted) 3.dp else if (hasConflict) 2.2.dp else if (hasSynergy) 2.2.dp else if (item != null) 2.dp else 1.dp,
                                                    color = if (isHighlighted) Color(0xFFF57F17)
                                                    else if (hasConflict) Color(0xFFE53935)
                                                    else if (hasSynergy) Color(0xFF4CAF50)
                                                    else if (item != null) MaterialTheme.colorScheme.primary
                                                    else currentSoilTheme.outlineColor.copy(alpha = 0.4f),
                                                    shape = RoundedCornerShape(16.dp)
                                                )
                                                .clickable {
                                                    if (isUprootModeActive) {
                                                        viewModel.placeGridPlant(r, c, "")
                                                    } else if (selectedSeedTemplate != null) {
                                                        viewModel.placeGridPlant(r, c, selectedSeedTemplate!!.name)
                                                        if (activePlants.none { it.name == selectedSeedTemplate!!.name }) {
                                                            viewModel.addPlant(
                                                                selectedSeedTemplate!!.name, 
                                                                selectedSeedTemplate!!.type, 
                                                                selectedSeedTemplate
                                                            )
                                                        }
                                                    } else {
                                                        showCellConfigDialog = Pair(r, c)
                                                    }
                                                }
                                                .testTag("grid_cell_${r}_${c}"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            // 1. Draw custom background substrate texture
                                            val baseSubstrateBgColors = when (selectedSoilIdx) {
                                                0 -> listOf(Color(0xFF8D6E63).copy(alpha = 0.08f), Color(0xFF5D4037).copy(alpha = 0.14f)) // Loam
                                                1 -> listOf(Color(0xFFFFF8E1).copy(alpha = 0.6f), Color(0xFFFFECB3).copy(alpha = 0.6f)) // Sand
                                                2 -> listOf(Color(0xFFECEFF1).copy(alpha = 0.7f), Color(0xFFCFD8DC).copy(alpha = 0.7f)) // Pebbles
                                                3 -> listOf(Color(0xFFF4A460).copy(alpha = 0.1f), Color(0xFFCD853F).copy(alpha = 0.15f)) // Clay
                                                else -> listOf(Color(0xFF4E342E).copy(alpha = 0.15f), Color(0xFF271C19).copy(alpha = 0.2f)) // Mulch
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Brush.verticalGradient(baseSubstrateBgColors))
                                            )
                                            
                                            // Custom canvas textures
                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                when (selectedSoilIdx) {
                                                    0 -> { // Loam - organic speckles
                                                        val random = java.util.Random((r * 13 + c * 37).toLong())
                                                        for (i in 0 until 8) {
                                                            val x = random.nextFloat() * size.width
                                                            val y = random.nextFloat() * size.height
                                                            val radius = 1f + random.nextFloat() * 1.5f
                                                            drawCircle(
                                                                color = Color(0xFF3E2723).copy(alpha = 0.2f),
                                                                radius = radius,
                                                                center = Offset(x, y)
                                                            )
                                                        }
                                                    }
                                                    1 -> { // Sand - granular texture
                                                        val random = java.util.Random((r * 29 + c * 43).toLong())
                                                        for (i in 0 until 12) {
                                                            val x = random.nextFloat() * size.width
                                                            val y = random.nextFloat() * size.height
                                                            drawCircle(
                                                                color = Color(0xFFFFB74D).copy(alpha = 0.35f),
                                                                radius = 0.8f,
                                                                center = Offset(x, y)
                                                            )
                                                        }
                                                    }
                                                    2 -> { // Pebbles - Raked paths
                                                        val stroke = Stroke(
                                                            width = 1.2f,
                                                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                                                        )
                                                        val rakedColor = Color(0xFF90A4AE).copy(alpha = 0.5f)
                                                        if (item != null) {
                                                            // Raked around plant
                                                            drawCircle(
                                                                color = rakedColor,
                                                                radius = size.width * 0.42f,
                                                                style = stroke
                                                            )
                                                            drawCircle(
                                                                color = rakedColor,
                                                                radius = size.width * 0.28f,
                                                                style = stroke
                                                            )
                                                        } else {
                                                            // Parallel wave lines
                                                            val step = size.height / 5f
                                                            for (i in 1..4) {
                                                                val y = i * step
                                                                drawLine(
                                                                    color = rakedColor,
                                                                    start = Offset(0f, y),
                                                                    end = Offset(size.width, y),
                                                                    strokeWidth = 1f,
                                                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                                                                )
                                                            }
                                                        }
                                                    }
                                                    3 -> { // Clay - Terracotta detail
                                                        val random = java.util.Random((r * 17 + c * 31).toLong())
                                                        for (i in 0 until 4) {
                                                            val x = random.nextFloat() * size.width
                                                            val y = random.nextFloat() * size.height
                                                            drawCircle(
                                                                color = Color(0xFF8B4513).copy(alpha = 0.15f),
                                                                radius = 3f + random.nextFloat() * 3f,
                                                                center = Offset(x, y)
                                                            )
                                                        }
                                                    }
                                                    4 -> { // Mulch - Wood chips
                                                        val random = java.util.Random((r * 23 + c * 19).toLong())
                                                        for (i in 0 until 6) {
                                                            val x1 = random.nextFloat() * size.width
                                                            val y1 = random.nextFloat() * size.height
                                                            val angle = random.nextFloat() * 2 * Math.PI
                                                            val len = 4f + random.nextFloat() * 6f
                                                            val x2 = x1 + (Math.cos(angle) * len).toFloat()
                                                            val y2 = y1 + (Math.sin(angle) * len).toFloat()
                                                            drawLine(
                                                                color = Color(0xFF3E2723).copy(alpha = 0.25f),
                                                                start = Offset(x1, y1),
                                                                end = Offset(x2, y2),
                                                                strokeWidth = 2f
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // Synergy glowing radial background
                                            if (hasSynergy) {
                                                val synergyTransition = rememberInfiniteTransition(label = "synergy_${r}_${c}")
                                                val glowAlpha by synergyTransition.animateFloat(
                                                    initialValue = 0.15f,
                                                    targetValue = 0.45f,
                                                    animationSpec = infiniteRepeatable(
                                                        animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
                                                        repeatMode = RepeatMode.Reverse
                                                     ),
                                                     label = "glow"
                                                 )
                                                 Canvas(modifier = Modifier.fillMaxSize()) {
                                                     val glowBrush = Brush.radialGradient(
                                                         colors = listOf(
                                                             Color(0xFF81C784).copy(alpha = glowAlpha),
                                                             Color(0xFF4CAF50).copy(alpha = 0f)
                                                         )
                                                     )
                                                     drawCircle(
                                                         brush = glowBrush,
                                                         radius = size.width * 0.5f
                                                     )
                                                 }
                                            }

                                            if (item != null) {
                                                val cellTransition = rememberInfiniteTransition(label = "cell_${r}_${c}")
                                                val swayAngle by cellTransition.animateFloat(
                                                    initialValue = -2.5f,
                                                    targetValue = 2.5f,
                                                    animationSpec = infiniteRepeatable(
                                                        animation = tween(durationMillis = 2000 + (r * 150) + (c * 200), easing = EaseInOutSine),
                                                        repeatMode = RepeatMode.Reverse
                                                    ),
                                                    label = "sway"
                                                )
                                                val scalePulse by cellTransition.animateFloat(
                                                    initialValue = 0.96f,
                                                    targetValue = 1.04f,
                                                    animationSpec = infiniteRepeatable(
                                                        animation = tween(durationMillis = 1800 + (r * 180) + (c * 120), easing = EaseInOutSine),
                                                        repeatMode = RepeatMode.Reverse
                                                    ),
                                                    label = "pulse"
                                                )
                                                val shakeOffset by cellTransition.animateFloat(
                                                    initialValue = -1.2f,
                                                    targetValue = 1.2f,
                                                    animationSpec = infiniteRepeatable(
                                                        animation = tween(durationMillis = 120, easing = LinearEasing),
                                                        repeatMode = RepeatMode.Reverse
                                                    ),
                                                    label = "shake"
                                                )
                                                
                                                val finalXOffset = if (hasConflict) shakeOffset.dp else 0.dp
                                                
                                                // Floating stars for synergy
                                                if (hasSynergy) {
                                                    val floatTransition = rememberInfiniteTransition(label = "stars_${r}_${c}")
                                                     val starYOffset by floatTransition.animateFloat(
                                                         initialValue = 2f,
                                                         targetValue = -10f,
                                                         animationSpec = infiniteRepeatable(
                                                             animation = tween(durationMillis = 2000, easing = LinearEasing),
                                                             repeatMode = RepeatMode.Restart
                                                         ),
                                                         label = "starY"
                                                     )
                                                     val starAlpha by floatTransition.animateFloat(
                                                         initialValue = 1f,
                                                         targetValue = 0f,
                                                         animationSpec = infiniteRepeatable(
                                                             animation = tween(durationMillis = 2000, easing = LinearEasing),
                                                             repeatMode = RepeatMode.Restart
                                                         ),
                                                         label = "starAlpha"
                                                     )
                                                     Box(modifier = Modifier.fillMaxSize()) {
                                                         Text(
                                                             text = "✨",
                                                             fontSize = 9.sp,
                                                             modifier = Modifier
                                                                 .align(Alignment.TopEnd)
                                                                 .offset(x = (-3).dp, y = starYOffset.dp)
                                                                 .graphicsLayer(alpha = starAlpha)
                                                         )
                                                         Text(
                                                             text = "✨",
                                                             fontSize = 7.sp,
                                                             modifier = Modifier
                                                                 .align(Alignment.BottomStart)
                                                                 .offset(x = 3.dp, y = (starYOffset + 8f).dp)
                                                                 .graphicsLayer(alpha = starAlpha)
                                                         )
                                                     }
                                                }

                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center,
                                                    modifier = Modifier
                                                        .padding(2.dp)
                                                        .offset(x = finalXOffset)
                                                        .graphicsLayer(
                                                            rotationZ = swayAngle,
                                                            scaleX = scalePulse,
                                                            scaleY = scalePulse
                                                        )
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
                             // Optimization: Provided unique 'key' to prevent unnecessary recompositions when the list changes.
                             items(distinctPlanted, key = { it }) { name ->
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

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Seed Tray Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Seed Tray Selector:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Tabs
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(2.dp)
                        ) {
                            val tabs = listOf("Indoor", "Outdoor")
                            tabs.forEachIndexed { index, label ->
                                val isSelected = selectedTrayTab == index
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { 
                                            selectedTrayTab = index
                                            selectedSeedTemplate = null
                                            isUprootModeActive = false
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // LazyRow Tray
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            val isSelected = isUprootModeActive
                            Card(
                                modifier = Modifier
                                    .width(76.dp)
                                    .clickable {
                                        isUprootModeActive = !isUprootModeActive
                                        selectedSeedTemplate = null
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFFC62828).copy(alpha = 0.15f) 
                                                     else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(0xFFC62828) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("🧹", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Clear", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    Text("Eraser", fontSize = 7.sp, color = if (isSelected) Color(0xFFC62828).copy(alpha = 0.8f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f), maxLines = 1)
                                }
                            }
                        }

                        val templates = ClimatePlants.getTemplatesForPlanner(
                            activeLayout?.climate ?: "",
                            isIndoor = selectedTrayTab == 0
                        )

                        items(templates, key = { it.name }) { template ->
                            val isSelected = selectedSeedTemplate?.name == template.name
                            val accentColor = if (selectedTrayTab == 0) Color(0xFF0284C7) else Color(0xFF10B981)
                            Card(
                                modifier = Modifier
                                    .width(76.dp)
                                    .clickable {
                                        if (isSelected) {
                                            selectedSeedTemplate = null
                                        } else {
                                            selectedSeedTemplate = template
                                            isUprootModeActive = false
                                        }
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) accentColor.copy(alpha = 0.15f) 
                                                     else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(template.iconEmoji, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(template.name, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                    Text(template.type, fontSize = 7.sp, color = if (isSelected) accentColor.copy(alpha = 0.8f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
            }
        }

        val layoutDesignChecklistContent = @Composable {
            val totalConflicts = activeGridItems.count { hasNeighborConflict(it.x, it.y, activeGridItems) }
            val totalSynergies = activeGridItems.count { hasNeighborSynergy(it.x, it.y, activeGridItems) }
            
            val task1Completed = true
            val task2Completed = activeGridItems.isNotEmpty()
            val task3Completed = activeGridItems.isNotEmpty() && totalConflicts == 0
            val task4Completed = hasConsultedAi
            val task5Completed = hasExportedBlueprint
            
            val completedCount = (if (task1Completed) 1 else 0) +
                                  (if (task2Completed) 1 else 0) +
                                  (if (task3Completed) 1 else 0) +
                                  (if (task4Completed) 1 else 0) +
                                  (if (task5Completed) 1 else 0)
            
            val progressFraction = completedCount.toFloat() / 5f
            val animatedProgress by animateFloatAsState(
                targetValue = progressFraction,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                label = "checklistProgress"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Layout Design Checklist 📋",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Complete each task to optimize your biophilic layout.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            color = if (completedCount == 5) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Text(
                            text = "$completedCount / 5 Tasks",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (completedCount == 5) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ChecklistItem(
                            title = "1. Select Surface Substrate",
                            description = "Choose loam, sand, pebbles, clay, or mulch matching your climate.",
                            statusText = "Selected: ${currentSoilTheme.name}",
                            isCompleted = task1Completed,
                            isExpanded = expandedTasks[1] == true,
                            onToggleExpand = { expandedTasks[1] = !(expandedTasks[1] ?: false) }
                        ) {
                            substrateSelectorContent()
                        }

                        ChecklistItem(
                            title = "2. Sow Seeds on Sector Grid",
                            description = "Tap sectors on the 5x5 layout grid to sow and place plants.",
                            statusText = "${activeGridItems.size} / 25 sectors occupied",
                            isCompleted = task2Completed,
                            isExpanded = expandedTasks[2] == true,
                            onToggleExpand = { expandedTasks[2] = !(expandedTasks[2] ?: false) }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                actionsPanelContent()
                                gridPlannerContent()
                            }
                        }

                        ChecklistItem(
                            title = "3. Optimize Companion Synergies",
                            description = "Arrange neighboring species to activate synergies and clear conflicts.",
                            statusText = "Synergies: $totalSynergies | Conflicts: $totalConflicts",
                            isCompleted = task3Completed,
                            isExpanded = expandedTasks[3] == true,
                            onToggleExpand = { expandedTasks[3] = !(expandedTasks[3] ?: false) }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Companion planting optimization analyzes neighboring plants in cardinal directions.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 11.sp
                                )

                                if (activeGridItems.isEmpty()) {
                                    Text(
                                        text = "Please sow plants on the grid in Task 2 to begin companion planting audit.",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                } else {
                                    val synergyPairs = mutableListOf<String>()
                                    val conflictPairs = mutableListOf<String>()
                                    
                                    activeGridItems.forEach { item1 ->
                                        activeGridItems.forEach { item2 ->
                                            if (item1 != item2) {
                                                val isNeighbor = (Math.abs(item1.x - item2.x) == 1 && item1.y == item2.y) || 
                                                                 (Math.abs(item1.y - item2.y) == 1 && item1.x == item2.x)
                                                if (isNeighbor) {
                                                    if (checkPlantSynergy(item1.plantName, item2.plantName)) {
                                                        val str = "${item1.plantName} ${getEmojiForPlantName(item1.plantName)} + ${item2.plantName} ${getEmojiForPlantName(item2.plantName)}"
                                                        val reverseStr = "${item2.plantName} ${getEmojiForPlantName(item2.plantName)} + ${item1.plantName} ${getEmojiForPlantName(item1.plantName)}"
                                                        if (!synergyPairs.contains(str) && !synergyPairs.contains(reverseStr)) {
                                                            synergyPairs.add(str)
                                                        }
                                                    }
                                                    if (checkPlantConflict(item1.plantName, item2.plantName)) {
                                                        val str = "${item1.plantName} ${getEmojiForPlantName(item1.plantName)} x ${item2.plantName} ${getEmojiForPlantName(item2.plantName)}"
                                                        val reverseStr = "${item2.plantName} ${getEmojiForPlantName(item2.plantName)} x ${item1.plantName} ${getEmojiForPlantName(item1.plantName)}"
                                                        if (!conflictPairs.contains(str) && !conflictPairs.contains(reverseStr)) {
                                                            conflictPairs.add(str)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (synergyPairs.isNotEmpty()) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text("✨ Active Synergies (No. $totalSynergies):", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 11.sp)
                                                synergyPairs.forEach { pair ->
                                                    Text("• $pair", fontSize = 10.sp, color = Color(0xFF1B5E20))
                                                }
                                            }
                                        }
                                    } else {
                                        Text("No active synergies found yet. Try placing synergistic plants adjacent to each other (e.g. Maple & Rose, Lavender & Rosemary, Cactus & Aloe).", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                                    }

                                    if (conflictPairs.isNotEmpty()) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text("⚠️ Active Conflicts (No. $totalConflicts):", fontWeight = FontWeight.Bold, color = Color(0xFFC62828), fontSize = 11.sp)
                                                conflictPairs.forEach { pair ->
                                                    Text("• $pair", fontSize = 10.sp, color = Color(0xFFB71C1C))
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text("Resolve conflicts by removing or relocating clashing species.", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFC62828))
                                            }
                                        }
                                    } else if (activeGridItems.isNotEmpty()) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                "🎉 No planting conflicts! Excellent layout architecture.",
                                                modifier = Modifier.padding(10.dp),
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2E7D32),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        ChecklistItem(
                            title = "4. Consult AI Advisor",
                            description = "Ask the AI Architect for custom layout reviews or design ideas.",
                            statusText = if (hasConsultedAi) "Consulted AI Advisor" else "Pending AI review",
                            isCompleted = task4Completed,
                            isExpanded = expandedTasks[4] == true,
                            onToggleExpand = { expandedTasks[4] = !(expandedTasks[4] ?: false) }
                        ) {
                            aiArchitectControlsContent()
                        }

                        ChecklistItem(
                            title = "5. Export CAD Blueprint",
                            description = "Render and download the final layout as an architect CAD vector file.",
                            statusText = if (hasExportedBlueprint) "Blueprint exported successfully" else "Ready to export",
                            isCompleted = task5Completed,
                            isExpanded = expandedTasks[5] == true,
                            onToggleExpand = { expandedTasks[5] = !(expandedTasks[5] ?: false) }
                        ) {
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
                    }
                }
            }
        }

        val progressDensityContent = @Composable {
            val countPlanted = activeGridItems.size
            val densityPercentage = (countPlanted * 100) / 25
            val progressFraction = countPlanted.toFloat() / 25f
            
            // Animate progress fraction for smooth loading
            val animatedProgress by animateFloatAsState(
                targetValue = progressFraction,
                animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                label = "progressPercentage"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tree Ring Gauge
                    Box(
                        modifier = Modifier
                            .size(84.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 8.dp.toPx()
                            val innerStrokeWidth = 1.dp.toPx()
                            val center = Offset(size.width / 2, size.height / 2)
                            val radius = (size.width - strokeWidth) / 2
                            
                            // concentric tree rings
                            drawCircle(
                                color = Color(0xFF8D6E63).copy(alpha = 0.08f),
                                radius = radius * 0.75f,
                                style = Stroke(width = innerStrokeWidth)
                            )
                            drawCircle(
                                color = Color(0xFF8D6E63).copy(alpha = 0.05f),
                                radius = radius * 0.5f,
                                style = Stroke(width = innerStrokeWidth)
                            )
                            
                            // background track ring
                            drawCircle(
                                color = Color(0xFFCBD0BE).copy(alpha = 0.3f),
                                radius = radius,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                            
                            // active progress arc (Tree ring growth)
                            val isFull = countPlanted == 25
                            val ringColor = if (isFull) Color(0xFFFFD54F) else Color(0xFF4CAF50)
                            
                            drawArc(
                                color = ringColor,
                                startAngle = -90f,
                                sweepAngle = animatedProgress * 360f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                            
                            // glowing effect if full
                            if (isFull) {
                                drawCircle(
                                    color = Color(0xFFFFD54F).copy(alpha = 0.15f),
                                    radius = radius + 4.dp.toPx(),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }
                        
                        // Center emoji indicator
                        Text(
                            text = if (countPlanted == 25) "🌸" else "🌱",
                            fontSize = 24.sp
                        )
                    }
                    
                    // Stats Info
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Yard Spacing Density",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        if (countPlanted == 25) {
                            Text(
                                text = "FULLY CULTIVATED ✨",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFB300),
                                letterSpacing = 0.5.sp
                            )
                        } else {
                            Text(
                                text = "$countPlanted / 25 Slots Occupied",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        Text(
                            text = "Active usage density: $densityPercentage%. " + 
                                   if (countPlanted == 25) "Your botanical sanctuary is fully sown and balanced!" 
                                   else "Plant more seeds to maximize companion synergies and density.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 10.sp,
                            lineHeight = 13.sp
                        )
                    }
                }
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
                layoutDesignChecklistContent()
                progressDensityContent()
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
                    layoutDesignChecklistContent()
                    progressDensityContent()
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

                        // Optimization: Provided unique 'key' to prevent unnecessary recompositions when the list changes.
                        items(allAvailablePlants, key = { it }) { pName ->
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
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF020E03), // Deep immersive dark matrix style
                border = BorderStroke(2.dp, Color(0xFF4CAF50)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
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
                         "5X5 VECTOR BLUEPRINT SCHEMATIC:",
                         color = Color.White,
                         fontSize = 11.sp,
                         fontWeight = FontWeight.Bold,
                         fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                     )

                     // Vector CAD Canvas drawing!
                     Box(
                         modifier = Modifier
                             .fillMaxWidth()
                             .aspectRatio(1f)
                             .background(Color.Black)
                             .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.4f))
                             .padding(10.dp)
                     ) {
                         val gridColor = Color(0xFF1B4D24).copy(alpha = 0.8f)
                         
                         Canvas(modifier = Modifier.fillMaxSize()) {
                             val cellWidth = size.width / 5.5f
                             val cellHeight = size.height / 5.5f
                             val startX = cellWidth * 0.25f
                             val startY = cellHeight * 0.25f
                             
                             // Boundary
                             drawRect(
                                 color = gridColor.copy(alpha = 0.1f),
                                 topLeft = Offset(startX, startY),
                                 size = androidx.compose.ui.geometry.Size(cellWidth * 5, cellHeight * 5)
                             )
                             
                             // Grid division lines
                             for (i in 0..5) {
                                 val x = startX + i * cellWidth
                                 val y = startY + i * cellHeight
                                 
                                 // Vertical line
                                 drawLine(
                                     color = gridColor,
                                     start = Offset(x, startY),
                                     end = Offset(x, startY + 5 * cellHeight),
                                     strokeWidth = 1f
                                 )
                                 
                                 // Horizontal line
                                 drawLine(
                                     color = gridColor,
                                     start = Offset(startX, y),
                                     end = Offset(startX + 5 * cellWidth, y),
                                     strokeWidth = 1f
                                 )
                             }
                             
                             // Synergy links
                             activeGridItems.forEach { item1 ->
                                 activeGridItems.forEach { item2 ->
                                     if (item1 != item2) {
                                         val isNeighbor = Math.abs(item1.x - item2.x) + Math.abs(item1.y - item2.y) == 1
                                         if (isNeighbor && checkPlantSynergy(item1.plantName, item2.plantName)) {
                                             val x1 = startX + (item1.y + 0.5f) * cellWidth
                                             val y1 = startY + (item1.x + 0.5f) * cellHeight
                                             val x2 = startX + (item2.y + 0.5f) * cellWidth
                                             val y2 = startY + (item2.x + 0.5f) * cellHeight
                                             
                                             drawLine(
                                                 color = Color(0xFFFFD54F),
                                                 start = Offset(x1, y1),
                                                 end = Offset(x2, y2),
                                                 strokeWidth = 3f,
                                                 pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
                                             )
                                         }
                                     }
                                 }
                             }
                             
                             // Nodes
                             for (r in 0..4) {
                                 for (c in 0..4) {
                                     val item = activeGridItems.firstOrNull { it.x == r && it.y == c }
                                     val cx = startX + (c + 0.5f) * cellWidth
                                     val cy = startY + (r + 0.5f) * cellHeight
                                     
                                     if (item != null) {
                                         val hasConflict = hasNeighborConflict(r, c, activeGridItems)
                                         val hasSynergy = hasNeighborSynergy(r, c, activeGridItems)
                                         
                                         // Node bg
                                         drawCircle(
                                             color = if (hasConflict) Color(0xFFC62828).copy(alpha = 0.25f)
                                                     else if (hasSynergy) Color(0xFF2E7D32).copy(alpha = 0.25f)
                                                     else Color(0xFF81C784).copy(alpha = 0.15f),
                                             radius = cellWidth * 0.35f,
                                             center = Offset(cx, cy)
                                         )
                                         
                                         // Node border
                                         drawCircle(
                                             color = if (hasConflict) Color(0xFFE53935)
                                                     else if (hasSynergy) Color(0xFF4CAF50)
                                                     else Color(0xFF81C784),
                                             radius = cellWidth * 0.35f,
                                             center = Offset(cx, cy),
                                             style = Stroke(width = 1.5f)
                                         )
                                         
                                         // Warning rings for conflicts
                                         if (hasConflict) {
                                             drawCircle(
                                                 color = Color(0xFFE53935).copy(alpha = 0.25f),
                                                 radius = cellWidth * 0.46f,
                                                 center = Offset(cx, cy),
                                                 style = Stroke(width = 1f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f))
                                             )
                                         }
                                         
                                         // Emoji drawing via native canvas
                                         drawContext.canvas.nativeCanvas.drawText(
                                             getEmojiForPlantName(item.plantName),
                                             cx,
                                             cy + (cellWidth * 0.14f),
                                             android.graphics.Paint().apply {
                                                 textSize = cellWidth * 0.45f
                                                 textAlign = android.graphics.Paint.Align.CENTER
                                             }
                                         )
                                     } else {
                                         // Empty crosshairs
                                         val crossSize = 6f
                                         drawLine(
                                             color = gridColor.copy(alpha = 0.4f),
                                             start = Offset(cx - crossSize, cy),
                                             end = Offset(cx + crossSize, cy),
                                             strokeWidth = 1f
                                         )
                                         drawLine(
                                             color = gridColor.copy(alpha = 0.4f),
                                             start = Offset(cx, cy - crossSize),
                                             end = Offset(cx, cy + crossSize),
                                             strokeWidth = 1f
                                         )
                                     }
                                 }
                             }
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
                                   val conflict = hasNeighborConflict(item.x, item.y, activeGridItems)
                                   val details = when {
                                       synergy -> " ✅ [COMPANION SYNERGY ACTIVE]"
                                       conflict -> " ⚠️ [NEIGHBOR CONFLICT WARNING]"
                                       else -> ""
                                   }
                                   Text(
                                       "-> PLOT [${item.x + 1}, ${item.y + 1}]: ${item.plantName} ${getEmojiForPlantName(item.plantName)} $details",
                                       color = when {
                                           synergy -> Color(0xFFFFD54F)
                                           conflict -> Color(0xFFEF5350)
                                           else -> Color(0xFFE8F5E9)
                                       },
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
                                 hasExportedBlueprint = true
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

private fun calculatePlantAges(currentIndex: Int, snapshots: List<Pair<Long, String>>): Array<IntArray> {
    val ages = Array(5) { IntArray(5) { 0 } }
    for (r in 0..4) {
        for (c in 0..4) {
            var age = 0
            for (i in 0..currentIndex) {
                val snap = snapshots.getOrNull(i)
                val snapGrid = parseGridString(snap?.second ?: "")
                if (snapGrid.any { it.x == r && it.y == c }) {
                    age++
                } else {
                    age = 0 // Reset if it was empty or uprooted
                }
            }
            ages[r][c] = age
        }
    }
    return ages
}

@Composable
fun TimelapseHeader(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "TIMELAPSE OBSERVER",
                color = Color(0xFF81C784),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            Text(
                text = "See Your Garden Grow",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
        }
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun TimelapseInfo(currentIndex: Int, totalSnapshots: Int, dateStr: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FRAME ${currentIndex + 1} OF $totalSnapshots",
                color = Color(0xFF81C784),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            Text(
                text = dateStr,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun TimelapseGrid(gridItems: List<GridPlantItem>, plantAges: Array<IntArray>) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .border(2.dp, Color(0xFF4CAF50).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .background(Color(0xFF0C140D)) // Dark green viewfinder
            .padding(12.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            for (r in 0..4) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    for (c in 0..4) {
                        val item = gridItems.firstOrNull { it.x == r && it.y == c }
                        val age = plantAges[r][c]

                        val cellBg = if (item != null) {
                            when (age) {
                                1 -> Color(0xFF1E351F) // Sprout stage
                                2 -> Color(0xFF2E5430) // Bud stage
                                else -> Color(0xFF1B4D24) // Fully grown
                            }
                        } else {
                            Color(0xFF1C281D).copy(alpha = 0.4f)
                        }

                        // Sprout / Growth visual representation
                        val animatedScale by animateFloatAsState(
                            targetValue = if (item != null) {
                                when (age) {
                                    1 -> 0.7f
                                    2 -> 0.85f
                                    else -> 1.0f
                                }
                            } else 0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                            label = "timelapseScale"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(cellBg)
                                .border(0.5.dp, Color(0xFF4CAF50).copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (item != null) {
                                val renderEmoji = when (age) {
                                    1 -> "🌱" // Sprout
                                    2 -> "🌿" // Leafy growth
                                    else -> getEmojiForPlantName(item.plantName) // Full flower/emoji
                                }
                                Text(
                                    text = renderEmoji,
                                    fontSize = 18.sp,
                                    modifier = Modifier.scale(animatedScale)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimelapseControls(
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Previous Frame",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        // Large Play/Pause Button
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .size(54.dp)
                .clip(CircleShape)
                .background(Color(0xFF4CAF50))
                .clickable { onTogglePlay() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.Black,
                modifier = Modifier.size(32.dp)
            )
        }

        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Next Frame",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun TimelapseTimeline(
    currentIndex: Int,
    totalSnapshots: Int,
    onIndexChanged: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = currentIndex.toFloat(),
            onValueChange = {
                onIndexChanged(it.toInt().coerceIn(0, totalSnapshots - 1))
            },
            valueRange = 0f..(totalSnapshots - 1).coerceAtLeast(0).toFloat(),
            steps = if (totalSnapshots > 2) totalSnapshots - 2 else 0,
            colors = SliderDefaults.colors(
                activeTrackColor = Color(0xFF4CAF50),
                inactiveTrackColor = Color(0xFF4CAF50).copy(alpha = 0.24f),
                thumbColor = Color(0xFF81C784)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Beginning", color = Color.Gray, fontSize = 9.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            Text("Present Day", color = Color.Gray, fontSize = 9.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        }
    }
}

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

    // Helper to calculate age of plant at r,c up to current index
    val plantAges = remember(currentIndex, snapshots) {
        calculatePlantAges(currentIndex, snapshots)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF131F14), // Deep forest dark theme
            border = BorderStroke(2.dp, Color(0xFF4CAF50)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TimelapseHeader(onDismiss = onDismiss)
                HorizontalDivider(color = Color(0xFF4CAF50).copy(alpha = 0.3f))
                TimelapseInfo(currentIndex = currentIndex, totalSnapshots = snapshots.size, dateStr = dateStr)
                TimelapseGrid(gridItems = gridItems, plantAges = plantAges)
                TimelapseControls(
                    isPlaying = isPlaying,
                    onTogglePlay = { isPlaying = !isPlaying },
                    onPrevious = {
                        if (snapshots.isNotEmpty()) {
                            currentIndex = (currentIndex - 1 + snapshots.size) % snapshots.size
                            isPlaying = false
                        }
                    },
                    onNext = {
                        if (snapshots.isNotEmpty()) {
                            currentIndex = (currentIndex + 1) % snapshots.size
                            isPlaying = false
                        }
                    }
                )
                TimelapseTimeline(
                    currentIndex = currentIndex,
                    totalSnapshots = snapshots.size,
                    onIndexChanged = { newIndex ->
                        currentIndex = newIndex
                        isPlaying = false
                    }
                )
            }
        }
    }
}

// --- Layout Design Checklist helper component ---
@Composable
private fun ChecklistItem(
    title: String,
    description: String,
    statusText: String,
    isCompleted: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (isExpanded) 1.5.dp else 1.dp,
            color = if (isExpanded) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            } else if (isCompleted) {
                Color(0xFF4CAF50).copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            }
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .border(2.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    if (statusText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isCompleted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                ) {
                    content()
                }
            }
        }
    }
}
