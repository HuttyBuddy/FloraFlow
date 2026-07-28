package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.ui.components.FloraFlowButton
import com.example.ui.components.PlantPhoto
import com.example.ui.viewmodel.GardenViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// Data model for Indoor Substrates (Flooring)
data class SoilTheme(
    val name: String,
    val bgColors: List<Color>,
    val outlineColor: Color,
    val slotBgColor: Color,
    val description: String
)

private val SOIL_THEMES = listOf(
    SoilTheme(
        name = "Light Oak 🪵",
        bgColors = listOf(Color(0xFFF5EBE0), Color(0xFFE3D5CA)),
        outlineColor = Color(0xFFD5BDAF),
        slotBgColor = Color(0xFF8D6E63).copy(alpha = 0.08f),
        description = "Warm organic oak flooring with subtle grain. Ideal for warm indoor sanctuaries."
    ),
    SoilTheme(
        name = "Tatami Mat 🌾",
        bgColors = listOf(Color(0xFFE8ECD6), Color(0xFFD0DBB3)),
        outlineColor = Color(0xFF9EAB78),
        slotBgColor = Color(0xFF556B2F).copy(alpha = 0.08f),
        description = "Traditional woven rush matting. Serene, natural texture for floor seating."
    ),
    SoilTheme(
        name = "Zen Gravel 🪨",
        bgColors = listOf(Color(0xFFECEFF1), Color(0xFFCFD8DC)),
        outlineColor = Color(0xFF78909C),
        slotBgColor = Color(0xFF37474F).copy(alpha = 0.08f),
        description = "Raked white pebbles and smoothed stones. Focus-enhancing mineral base."
    ),
    SoilTheme(
        name = "Natural Slate ⬛",
        bgColors = listOf(Color(0xFF37474F), Color(0xFF263238)),
        outlineColor = Color(0xFF546E7A),
        slotBgColor = Color(0xFFECEFF1).copy(alpha = 0.12f),
        description = "Cool dark mineral slate slabs. Earthy grounding contrast for lush foliage."
    ),
    SoilTheme(
        name = "Bamboo Fiber 🎋",
        bgColors = listOf(Color(0xFFEFEBE9), Color(0xFFD7CCC8)),
        outlineColor = Color(0xFFA1887F),
        slotBgColor = Color(0xFF4E342E).copy(alpha = 0.08f),
        description = "Compressed bamboo slats. Excellent natural acoustic dampening."
    )
)

// Companion & Synergy rules kept for full backward compatibility & unit testing
private val COMPANION_RULES = listOf(
    setOf("rose", "maple"),
    setOf("rose", "lavender"),
    setOf("lavender", "juniper"),
    setOf("cactus", "aloe"),
    setOf("marigold", "ivy"),
    setOf("ivy", "basil"),
    setOf("aster", "thyme"),
    setOf("columbine", "fern"),
    setOf("rosemary", "lavender"),
    setOf("rosemary", "thyme"),
    setOf("snake plant", "peace lily"),
    setOf("pothos", "monstera")
)

private val CONFLICT_RULES = listOf(
    setOf("ivy", "cactus"),
    setOf("fern", "cactus"),
    setOf("lavender", "fern"),
    setOf("rosemary", "fern"),
    setOf("basil", "cactus"),
    setOf("pothos", "cactus"),
    setOf("maple", "cactus"),
    setOf("rose", "cactus"),
    setOf("mint", "rose"),
    setOf("mint", "lavender")
)

fun plantMatchesRule(plantName: String, ruleName: String): Boolean {
    val words = plantName.lowercase().split(Regex("[^a-zA-Z0-9]+")).filter { it.isNotBlank() }
    return words.contains(ruleName.lowercase())
}

fun checkPlantSynergy(plant1: String, plant2: String): Boolean {
    val p1 = plant1.lowercase()
    val p2 = plant2.lowercase()
    if (p1 == p2) return false
    return COMPANION_RULES.any { rule ->
        rule.any { r -> plantMatchesRule(p1, r) } && rule.any { r -> plantMatchesRule(p2, r) }
    }
}

fun checkPlantConflict(plant1: String, plant2: String): Boolean {
    val p1 = plant1.lowercase()
    val p2 = plant2.lowercase()
    if (p1 == p2) return false
    return CONFLICT_RULES.any { rule ->
        rule.any { r -> plantMatchesRule(p1, r) } && rule.any { r -> plantMatchesRule(p2, r) }
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
        if (neighborItem != null && checkPlantSynergy(currentLoc.plantName, neighborItem.plantName)) {
            return true
        }
    }
    return false
}

fun hasNeighborSynergyOptimized(row: Int, col: Int, gridMap: Array<GridPlantItem?>): Boolean {
    val currentLoc = gridMap.getOrNull(row * 5 + col) ?: return false
    val neighbors = arrayOf(
        if (row > 0) gridMap[(row - 1) * 5 + col] else null,
        if (row < 4) gridMap[(row + 1) * 5 + col] else null,
        if (col > 0) gridMap[row * 5 + col - 1] else null,
        if (col < 4) gridMap[row * 5 + col + 1] else null
    )
    for (neighborItem in neighbors) {
        if (neighborItem != null && checkPlantSynergy(currentLoc.plantName, neighborItem.plantName)) {
            return true
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
        if (neighborItem != null && checkPlantConflict(currentLoc.plantName, neighborItem.plantName)) {
            return true
        }
    }
    return false
}

fun hasNeighborConflictOptimized(row: Int, col: Int, gridMap: Array<GridPlantItem?>): Boolean {
    val currentLoc = gridMap.getOrNull(row * 5 + col) ?: return false
    val neighbors = arrayOf(
        if (row > 0) gridMap[(row - 1) * 5 + col] else null,
        if (row < 4) gridMap[(row + 1) * 5 + col] else null,
        if (col > 0) gridMap[row * 5 + col - 1] else null,
        if (col < 4) gridMap[row * 5 + col + 1] else null
    )
    for (neighborItem in neighbors) {
        if (neighborItem != null && checkPlantConflict(currentLoc.plantName, neighborItem.plantName)) {
            return true
        }
    }
    return false
}

// Biophilic Indoor Element Catalog Item Definition
data class BiophilicItem(
    val id: String,
    val name: String,
    val category: String, // "Air Purifier", "Zen Artifact", "Acoustic Accent"
    val iconEmoji: String,
    val o2Rating: Int, // 1 to 5
    val acousticRating: Int, // 1 to 5
    val calmingScent: String, // e.g. "Clean Oxygen", "Eucalyptus", "Lavender", "Bamboo Fresh"
    val description: String
)

private val BIOPHILIC_CATALOG = listOf(
    BiophilicItem("snake_plant", "Snake Plant", "Air Purifier", "🪴", 5, 2, "Night Oxygen Booster", "Converts CO2 to O2 overnight. Purifies formaldehyde & benzene."),
    BiophilicItem("peace_lily", "Peace Lily", "Air Purifier", "🌸", 5, 3, "Micro-Moisture & Clean", "Filters mold spores & airborne pollutants. Boosts humidity by 5%."),
    BiophilicItem("pothos", "Golden Pothos", "Air Purifier", "🍃", 4, 3, "Fresh Canopy", "Fast-growing vine that purifies indoor air and dampens sound."),
    BiophilicItem("monstera", "Monstera Deliciosa", "Air Purifier", "🌿", 4, 4, "Acoustic Shield", "Broad split leaves scatter high-frequency echoes for quiet posture."),
    BiophilicItem("boston_fern", "Boston Fern", "Air Purifier", "🌱", 4, 4, "Humidifying Cloud", "Restores essential indoor humidity for deep nasal breathwork."),
    BiophilicItem("english_ivy", "English Ivy", "Air Purifier", "🪻", 5, 2, "Particulate Filter", "Top air-purifying vine for reducing airborne dust & particulate matter."),
    BiophilicItem("lavender_pot", "Aromatic Lavender", "Air Purifier", "🪻", 3, 2, "Linalool Terpene", "Emits linalool aromatic molecules proven to lower heart rate."),
    
    BiophilicItem("zafu_cushion", "Zafu Cushion", "Zen Artifact", "🧘", 1, 5, "Ergonomic Grounding", "Buckwheat-filled floor cushion for spinal alignment during Zazen."),
    BiophilicItem("bamboo_spout", "Bamboo Water Spout", "Zen Artifact", "🎋", 2, 4, "Hydro-Acoustic White Noise", "Continuous trickling water sound masks urban ambient noise."),
    BiophilicItem("raked_gravel", "Raked Zen Gravel", "Zen Artifact", "🪨", 1, 4, "Tactile Focus", "Smoothed river stones and raked sand for mindfulness meditation."),
    BiophilicItem("salt_lamp", "Himalayan Salt Lamp", "Zen Artifact", "🪔", 1, 2, "Warm Amber Glow", "Soft 2700K ionization lamp for relaxing circadian transition."),
    BiophilicItem("singing_bowl", "Brass Singing Bowl", "Zen Artifact", "🥣", 1, 3, "432Hz Resonance", "Hand-hammered brass bowl emitting harmonic meditation frequencies."),
    BiophilicItem("tea_set", "Cast Iron Tea Set", "Zen Artifact", "🫖", 1, 2, "Mindful Brew", "Handcrafted iron teapot for post-session restorative herbal tea."),
    BiophilicItem("diffuser", "Aroma Diffuser", "Zen Artifact", "🕯️", 2, 2, "Ultrasound Mist", "Disperses pure cedarwood & eucalyptus essential oils."),
    
    BiophilicItem("moss_wall", "Moss Wall Panel", "Acoustic Accent", "🖼️", 3, 5, "Absorptive Canopy", "Preserved reindeer moss panel absorbing up to 40% of ambient echo."),
    BiophilicItem("linen_screen", "Natural Linen Screen", "Acoustic Accent", "🪟", 1, 4, "Dappled Light Diffuser", "Softens direct sunlight into calming dappled illumination."),
    BiophilicItem("rattan_chair", "Rattan Lounger", "Acoustic Accent", "🪑", 1, 3, "Restorative Cradle", "Ergonomic natural cane lounge chair for deep relaxation.")
)

// Intentions
data class SanctuaryIntention(
    val id: String,
    val title: String,
    val emoji: String,
    val subtitle: String,
    val targetO2: Int,
    val recommendedSubstrateIndex: Int,
    val presetItems: List<Pair<Int, String>> // (Index 0..24, ItemId)
)

private val INTENTIONS = listOf(
    SanctuaryIntention(
        id = "meditation",
        title = "Meditation Nook",
        emoji = "🧘",
        subtitle = "High O2, zen acoustic dampening & zafu seating for Zazen focus",
        targetO2 = 85,
        recommendedSubstrateIndex = 1, // Tatami Mat
        presetItems = listOf(
            Pair(12, "zafu_cushion"),
            Pair(7, "snake_plant"),
            Pair(11, "bamboo_spout"),
            Pair(13, "salt_lamp"),
            Pair(17, "peace_lily"),
            Pair(2, "monstera"),
            Pair(22, "moss_wall")
        )
    ),
    SanctuaryIntention(
        id = "breathing",
        title = "Mindful Breathing",
        emoji = "🌬️",
        subtitle = "Oxygen-rich canopy & humidifying ferns with rhythm lighting",
        targetO2 = 95,
        recommendedSubstrateIndex = 0, // Light Oak
        presetItems = listOf(
            Pair(12, "zafu_cushion"),
            Pair(6, "snake_plant"),
            Pair(8, "peace_lily"),
            Pair(16, "boston_fern"),
            Pair(18, "english_ivy"),
            Pair(2, "pothos"),
            Pair(22, "diffuser")
        )
    ),
    SanctuaryIntention(
        id = "relaxation",
        title = "Deep Relaxation",
        emoji = "🌿",
        subtitle = "Lush foliage canopy, acoustic moss walls & soothing rain",
        targetO2 = 88,
        recommendedSubstrateIndex = 4, // Bamboo Fiber
        presetItems = listOf(
            Pair(12, "rattan_chair"),
            Pair(7, "monstera"),
            Pair(17, "moss_wall"),
            Pair(11, "bamboo_spout"),
            Pair(13, "lavender_pot"),
            Pair(1, "pothos"),
            Pair(23, "singing_bowl")
        )
    ),
    SanctuaryIntention(
        id = "tea_nook",
        title = "Restorative Tea",
        emoji = "☕",
        subtitle = "Warm natural wood, quiet tea ritual & gentle amber lighting",
        targetO2 = 75,
        recommendedSubstrateIndex = 0, // Light Oak
        presetItems = listOf(
            Pair(12, "tea_set"),
            Pair(7, "raked_gravel"),
            Pair(11, "salt_lamp"),
            Pair(13, "peace_lily"),
            Pair(17, "snake_plant"),
            Pair(3, "linen_screen")
        )
    )
)

// Lighting Moods
data class LightingMood(
    val name: String,
    val emoji: String,
    val colorOverlay: Color,
    val description: String
)

private val LIGHTING_MOODS = listOf(
    LightingMood("Dawn Gold", "🌅", Color(0xFFFFF8E7).copy(alpha = 0.15f), "Warm 4000K morning sunlight"),
    LightingMood("Forest Dappled", "🌲", Color(0xFFE8F5E9).copy(alpha = 0.25f), "Soft emerald 520nm canopy filter"),
    LightingMood("Sunset Amber", "🌇", Color(0xFFFFECB3).copy(alpha = 0.30f), "Relaxing 2700K dusk glow"),
    LightingMood("Moonlit Indigo", "🌙", Color(0xFFE8EAF6).copy(alpha = 0.20f), "Cool serene night twilight")
)

// Soundscapes
data class AmbientSoundscape(
    val name: String,
    val emoji: String,
    val hzInfo: String
)

private val SOUNDSCAPES = listOf(
    AmbientSoundscape("Bamboo Fountain", "🎋", "White noise trickling water"),
    AmbientSoundscape("Pine Forest Wind", "🌲", "Soft low-frequency breeze"),
    AmbientSoundscape("432Hz Singing Bowl", "🔔", "Harmonic meditation tone"),
    AmbientSoundscape("Rainforest Rain", "🌧️", "Dappled foliage raindrops")
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    viewModel: GardenViewModel,
    switchToChatTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeLayout by viewModel.activeLayout.collectAsStateWithLifecycle()

    var selectedIntention by remember { mutableStateOf(INTENTIONS[0]) }
    var selectedSubstrateIdx by remember { mutableIntStateOf(1) } // Tatami Mat
    var selectedLightingIdx by remember { mutableIntStateOf(0) } // Dawn Gold
    var selectedSoundscapeIdx by remember { mutableIntStateOf(0) }
    var isSoundPlaying by remember { mutableStateOf(false) }

    var selectedCategoryTab by remember { mutableIntStateOf(0) } // 0=All, 1=Air Purifiers, 2=Zen Artifacts, 3=Accents
    var selectedCatalogItem by remember { mutableStateOf<BiophilicItem?>(null) }
    var isUprootModeActive by remember { mutableStateOf(false) }

    var showCellConfigDialog by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var showBreathingModal by remember { mutableStateOf(false) }

    val currentSoilTheme = SOIL_THEMES[selectedSubstrateIdx.coerceIn(0, SOIL_THEMES.size - 1)]

    val currentLayout = activeLayout
    val activeGridItems = remember(currentLayout?.gridString) {
        parseGridString(currentLayout?.gridString ?: "")
    }

    // Helper to calculate live metrics
    val airPurificationScore = remember(activeGridItems) {
        val totalO2 = activeGridItems.sumOf { item ->
            val match = BIOPHILIC_CATALOG.find { it.name.equals(item.plantName, ignoreCase = true) || it.id.equals(item.plantName, ignoreCase = true) }
            match?.o2Rating ?: 2
        }
        ((totalO2.toFloat() / 35f) * 100f).coerceIn(12f, 98f).roundToInt()
    }

    val acousticScore = remember(activeGridItems) {
        val totalAcoustics = activeGridItems.sumOf { item ->
            val match = BIOPHILIC_CATALOG.find { it.name.equals(item.plantName, ignoreCase = true) || it.id.equals(item.plantName, ignoreCase = true) }
            match?.acousticRating ?: 2
        }
        ((totalAcoustics.toFloat() / 30f) * 100f).coerceIn(15f, 95f).roundToInt()
    }

    val visualCalmScore = remember(activeGridItems, selectedSubstrateIdx) {
        val count = activeGridItems.size
        ((count * 4f + 35f) + (selectedSubstrateIdx * 3)).coerceIn(20f, 99f).roundToInt()
    }

    val activeTerpenes = remember(activeGridItems) {
        val scents = activeGridItems.mapNotNull { item ->
            BIOPHILIC_CATALOG.find { it.name.equals(item.plantName, ignoreCase = true) }?.calmingScent
        }.distinct()
        if (scents.isEmpty()) listOf("Clean Oxygen", "Natural Wood") else scents
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        currentSoilTheme.bgColors[0].copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
            .testTag("plot_workspace")
    ) {
        // Top Header - Biophilic Design Studio
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Biophilic Design Studio",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = "INDOOR",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Design a serene indoor space for meditation, relaxation & mindful breathing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Mindful Breathing CTA Button
                    Button(
                        onClick = { showBreathingModal = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.SelfImprovement, contentDescription = "Breathwork", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Breathe", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Intention Selector Tabs
                Text(
                    text = "SANCTUARY INTENTION",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(INTENTIONS) { intention ->
                        val isSelected = selectedIntention.id == intention.id
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedIntention = intention
                                selectedSubstrateIdx = intention.recommendedSubstrateIndex
                                // Apply preset layout to grid if empty or requested
                                viewModel.clearLayoutGrid()
                                intention.presetItems.forEach { (idx, itemId) ->
                                    val r = idx / 5
                                    val c = idx % 5
                                    val item = BIOPHILIC_CATALOG.find { it.id == itemId }
                                    if (item != null) {
                                        viewModel.placeGridPlant(r, c, item.name)
                                    }
                                }
                            },
                            label = {
                                Text(
                                    text = "${intention.emoji} ${intention.title}",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main Content Container
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {

            // Intention Banner Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(selectedIntention.emoji, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedIntention.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = selectedIntention.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(
                        onClick = {
                            viewModel.clearLayoutGrid()
                            selectedIntention.presetItems.forEach { (idx, itemId) ->
                                val r = idx / 5
                                val c = idx % 5
                                val item = BIOPHILIC_CATALOG.find { it.id == itemId }
                                if (item != null) {
                                    viewModel.placeGridPlant(r, c, item.name)
                                }
                            }
                        }
                    ) {
                        Text("Apply Preset", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Substrate / Flooring Picker & Lighting Controls Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FLOOR SUBSTRATE & ATMOSPHERE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = currentSoilTheme.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(SOIL_THEMES.indices.toList()) { idx ->
                            val theme = SOIL_THEMES[idx]
                            val isSelected = selectedSubstrateIdx == idx
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) theme.bgColors[0] else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) theme.outlineColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier
                                    .clickable { selectedSubstrateIdx = idx }
                                    .padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = theme.name,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Lighting Mood selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WbSunny, contentDescription = "Lighting", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Lighting Mood:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(LIGHTING_MOODS.indices.toList()) { idx ->
                                val mood = LIGHTING_MOODS[idx]
                                val isSelected = selectedLightingIdx == idx
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.clickable { selectedLightingIdx = idx }
                                ) {
                                    Text(
                                        text = "${mood.emoji} ${mood.name}",
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5x5 Indoor Biophilic Space Blueprint Canvas
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("plot_workspace"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = currentSoilTheme.bgColors[0]),
                border = BorderStroke(2.dp, currentSoilTheme.outlineColor.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LIGHTING_MOODS[selectedLightingIdx].colorOverlay)
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "SANCTUARY LAYOUT CANVAS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Tap cell to place or swap biophilic elements",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Clear Grid Action
                                IconButton(
                                    onClick = { viewModel.clearLayoutGrid() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Clear Space", tint = MaterialTheme.colorScheme.error)
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // Toggle Uproot / Remove Mode
                                FilterChip(
                                    selected = isUprootModeActive,
                                    onClick = { isUprootModeActive = !isUprootModeActive },
                                    label = { Text(if (isUprootModeActive) "Remove Mode ON" else "Remove Mode", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 5x5 Grid Canvas
                        val gridMap = remember(activeGridItems) {
                            Array(25) { idx ->
                                val r = idx / 5
                                val c = idx % 5
                                activeGridItems.firstOrNull { it.x == r && it.y == c }
                            }
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                        ) {
                            items(25) { idx ->
                                val row = idx / 5
                                val col = idx % 5
                                val item = gridMap[idx]

                                val isSynergy = item != null && hasNeighborSynergyOptimized(row, col, gridMap)

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (item != null) currentSoilTheme.bgColors[1].copy(alpha = 0.9f) else currentSoilTheme.slotBgColor,
                                    border = BorderStroke(
                                        width = if (isSynergy) 2.dp else 1.dp,
                                        color = when {
                                            isSynergy -> Color(0xFF4CAF50)
                                            item != null -> currentSoilTheme.outlineColor
                                            else -> currentSoilTheme.outlineColor.copy(alpha = 0.25f)
                                        }
                                    ),
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clickable {
                                            if (isUprootModeActive) {
                                                viewModel.placeGridPlant(row, col, "")
                                            } else if (selectedCatalogItem != null) {
                                                viewModel.placeGridPlant(row, col, selectedCatalogItem!!.name)
                                            } else {
                                                showCellConfigDialog = Pair(row, col)
                                            }
                                        }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        if (item != null) {
                                            val matchedCatalog = BIOPHILIC_CATALOG.find { it.name.equals(item.plantName, ignoreCase = true) }
                                            val emoji = matchedCatalog?.iconEmoji ?: getEmojiForPlantName(item.plantName)

                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier.padding(2.dp)
                                            ) {
                                                Text(emoji, fontSize = 22.sp)
                                                Text(
                                                    text = item.plantName,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center,
                                                    color = Color.Black.copy(alpha = 0.8f)
                                                )
                                            }

                                            if (isSynergy) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(2.dp)
                                                        .size(8.dp)
                                                        .background(Color(0xFF4CAF50), CircleShape)
                                                )
                                            }
                                        } else {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "Empty Slot",
                                                tint = currentSoilTheme.outlineColor.copy(alpha = 0.4f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Indoor Biophilic Catalog Tray
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "BIOPHILIC ELEMENT PALETTE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )

                        if (selectedCatalogItem != null) {
                            TextButton(
                                onClick = { selectedCatalogItem = null },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Clear Selection (${selectedCatalogItem!!.name})", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Category Tabs
                    val categories = listOf("All", "Air Purifiers", "Zen Artifacts", "Accents")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        categories.forEachIndexed { idx, cat ->
                            val isSelected = selectedCategoryTab == idx
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.clickable { selectedCategoryTab = idx }
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val filteredCatalog = remember(selectedCategoryTab) {
                        when (selectedCategoryTab) {
                            1 -> BIOPHILIC_CATALOG.filter { it.category == "Air Purifier" }
                            2 -> BIOPHILIC_CATALOG.filter { it.category == "Zen Artifact" }
                            3 -> BIOPHILIC_CATALOG.filter { it.category == "Acoustic Accent" }
                            else -> BIOPHILIC_CATALOG
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filteredCatalog) { biophilicItem ->
                            val isSelected = selectedCatalogItem?.id == biophilicItem.id
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier
                                    .width(130.dp)
                                    .clickable {
                                        selectedCatalogItem = if (isSelected) null else biophilicItem
                                    }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(biophilicItem.iconEmoji, fontSize = 24.sp)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surface
                                        ) {
                                            Text(
                                                text = "O2 +${biophilicItem.o2Rating}",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2E7D32),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = biophilicItem.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = biophilicItem.calmingScent,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.secondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live Biophilic Serenity Metrics Card Dashboard
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE BIOPHILIC SERENITY METRICS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Calculated in Real-Time",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Metric 1: Air Purification & O2
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFE8F5E9)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🍃", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Air & O2", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("$airPurificationScore%", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF2E7D32))
                                LinearProgressIndicator(
                                    progress = { airPurificationScore.toFloat() / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = Color(0xFF4CAF50),
                                    trackColor = Color(0xFFA5D6A7)
                                )
                            }
                        }

                        // Metric 2: Acoustic Dampening
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFE0F2F1)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🔊", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Acoustics", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF004D40))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("$acousticScore%", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF00796B))
                                LinearProgressIndicator(
                                    progress = { acousticScore.toFloat() / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = Color(0xFF009688),
                                    trackColor = Color(0xFF80CBC4)
                                )
                            }
                        }

                        // Metric 3: Visual Calm & Harmony
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFFF8E1)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🧘", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Zen Calm", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF57F17))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("$visualCalmScore%", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFF57C00))
                                LinearProgressIndicator(
                                    progress = { visualCalmScore.toFloat() / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = Color(0xFFFFB74D),
                                    trackColor = Color(0xFFFFE082)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Terpene & Aromatherapy Profile
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Spa, contentDescription = "Aroma", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Active Aromatherapy Notes: ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = activeTerpenes.joinToString(", "),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Soundscape Player & AI Counsel Integration
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎶", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "BIOPHILIC SOUNDSCAPE SYNC",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                        }

                        IconButton(
                            onClick = { isSoundPlaying = !isSoundPlaying },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isSoundPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                contentDescription = "Play Soundscape",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(SOUNDSCAPES.indices.toList()) { idx ->
                            val sound = SOUNDSCAPES[idx]
                            val isSelected = selectedSoundscapeIdx == idx
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.clickable { selectedSoundscapeIdx = idx }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(sound.emoji, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(sound.name, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text(sound.hzInfo, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // AI Biophilic Layout Counsel CTA
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ask Garden Counsel AI",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Get personalized advice on plant placement & airflow for meditation",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { switchToChatTab() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Consult AI", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Consult", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog: Cell Details / Swap Dialog
    if (showCellConfigDialog != null) {
        val (row, col) = showCellConfigDialog!!
        val currentItem = activeGridItems.firstOrNull { it.x == row && it.y == col }

        AlertDialog(
            onDismissRequest = { showCellConfigDialog = null },
            title = {
                Text(
                    text = "Sanctuary Slot (${row + 1}, ${col + 1})",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    if (currentItem != null) {
                        Text(
                            text = "Currently placed: ${currentItem.plantName}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    } else {
                        Text("This space slot is currently empty.", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text("Choose a biophilic element to place:", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState())
                    ) {
                        BIOPHILIC_CATALOG.forEach { item ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.placeGridPlant(row, col, item.name)
                                        showCellConfigDialog = null
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Text(item.iconEmoji, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(item.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCellConfigDialog = null }) {
                    Text("Close")
                }
            },
            dismissButton = {
                if (currentItem != null) {
                    TextButton(
                        onClick = {
                            viewModel.placeGridPlant(row, col, "")
                            showCellConfigDialog = null
                        }
                    ) {
                        Text("Clear Slot", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        )
    }

    // Modal Dialog: Mindful 4-7-8 Breathing Visualizer
    if (showBreathingModal) {
        Dialog(onDismissRequest = { showBreathingModal = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                var breathPhase by remember { mutableStateOf("Inhale") }
                var breathSeconds by remember { mutableIntStateOf(4) }
                val infiniteTransition = rememberInfiniteTransition(label = "breath")
                val scaleAnim by infiniteTransition.animateFloat(
                    initialValue = 0.8f,
                    targetValue = 1.35f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 4000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "breath_scale"
                )

                LaunchedEffect(Unit) {
                    while (true) {
                        breathPhase = "Inhale..."
                        for (i in 4 downTo 1) {
                            breathSeconds = i
                            delay(1000)
                        }
                        breathPhase = "Hold..."
                        for (i in 7 downTo 1) {
                            breathSeconds = i
                            delay(1000)
                        }
                        breathPhase = "Exhale..."
                        for (i in 8 downTo 1) {
                            breathSeconds = i
                            delay(1000)
                        }
                    }
                }

                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🧘", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mindful Breathing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { showBreathingModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Glowing Breathing Circle Animation
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(180.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(scaleAnim)
                        ) {}

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(110.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = breathPhase,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "$breathSeconds s",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Synchronized with your sanctuary's air-purifying foliage & ambient acoustics.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showBreathingModal = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Complete Session", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Utility to assign emojis based on plant name keywords
fun getEmojiForPlantName(name: String): String {
    val low = name.lowercase()
    
    val exactMatch = ClimatePlants.ALL_TEMPLATES.find { it.name.equals(name, ignoreCase = true) }
    if (exactMatch != null) return exactMatch.iconEmoji
    
    val substringMatch = ClimatePlants.ALL_TEMPLATES.find {
        name.contains(it.name, ignoreCase = true) || it.name.contains(name, ignoreCase = true)
    }
    if (substringMatch != null) return substringMatch.iconEmoji

    return when {
         low.contains("snake") -> "🪴"
         low.contains("peace") || low.contains("lily") -> "🌸"
         low.contains("pothos") -> "🍃"
         low.contains("monstera") -> "🌿"
         low.contains("fern") -> "🌱"
         low.contains("ivy") -> "🪻"
         low.contains("zafu") || low.contains("cushion") -> "🧘"
         low.contains("bamboo") || low.contains("spout") -> "🎋"
         low.contains("gravel") || low.contains("stone") -> "🪨"
         low.contains("salt") || low.contains("lamp") -> "🪔"
         low.contains("bowl") -> "🥣"
         low.contains("tea") -> "🫖"
         low.contains("diffuser") -> "🕯️"
         low.contains("moss") -> "🖼️"
         low.contains("linen") || low.contains("screen") -> "🪟"
         low.contains("rattan") || low.contains("chair") -> "🪑"
         low.contains("bonsai") -> "🪴"
         low.contains("juniper") -> "🌲"
         low.contains("rose") -> "🌹"
         low.contains("cact") || low.contains("aloe") -> "🌵"
         low.contains("lavender") -> "🪻"
         else -> "🌿"
    }
}
