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

// ----------------------------------------------------
// SPATIAL ROOM STUDIO DATA MODELS & ZONE DEFINITIONS
// ----------------------------------------------------

data class RoomZoneOption(
    val id: String,
    val name: String,
    val iconEmoji: String,
    val o2Rating: Int, // 1 to 5
    val acousticRating: Int, // 1 to 5
    val calmingScent: String,
    val description: String
)

data class RoomZone(
    val zoneId: String,
    val title: String,
    val subtitle: String,
    val iconEmoji: String,
    val options: List<RoomZoneOption>
)

private val ROOM_ZONES = listOf(
    RoomZone(
        zoneId = "canopy",
        title = "Canopy & Wall Feature",
        subtitle = "Vertical greenery & sound-absorbing foliage",
        iconEmoji = "🖼️",
        options = listOf(
            RoomZoneOption("moss_wall", "Preserved Moss Wall", "🖼️", 3, 5, "Earthy Cedar", "Absorbs up to 40% of ambient acoustic echo."),
            RoomZoneOption("cascading_pothos", "Cascading Pothos Vines", "🍃", 4, 4, "Fresh Canopy", "Lush trailing greenery that purifies indoor air."),
            RoomZoneOption("monstera_canopy", "Monstera Split Canopy", "🌿", 4, 4, "Tropical Glow", "Broad architectural leaves scattering high-frequency noise."),
            RoomZoneOption("hydro_living_wall", "Hydroponic Living Wall", "🌱", 5, 5, "Humidifying Oasis", "Integrated vertical hydro-wall boosting room humidity.")
        )
    ),
    RoomZone(
        zoneId = "foliage",
        title = "Air Purifiers & Companions",
        subtitle = "Toxin-filtering plants & oxygen boosters",
        iconEmoji = "🪴",
        options = listOf(
            RoomZoneOption("snake_plant_trio", "Snake Plant Trio", "🪴", 5, 2, "Night Oxygen", "Produces oxygen overnight & filters formaldehyde."),
            RoomZoneOption("peace_lily_bloom", "Peace Lily Bloom", "🌸", 5, 3, "Clean Humidity", "Filters mold spores & airborne particulates."),
            RoomZoneOption("boston_fern_cloud", "Boston Fern Cloud", "🌱", 4, 4, "Micro-Moisture", "Restores essential room moisture for deep breathwork."),
            RoomZoneOption("aromatic_lavender", "Aromatic Lavender Pot", "🪻", 3, 2, "Linalool Terpene", "Emits natural linalool molecules proven to ease stress.")
        )
    ),
    RoomZone(
        zoneId = "seating",
        title = "Grounding & Seating Base",
        subtitle = "Ergonomic posture support & floor textures",
        iconEmoji = "🧘",
        options = listOf(
            RoomZoneOption("zafu_tatami", "Zafu Cushion on Tatami", "🧘", 1, 5, "Zazen Ergonomics", "Buckwheat floor cushion paired with woven rush matting."),
            RoomZoneOption("oak_bench", "Natural Light Oak Bench", "🪵", 1, 3, "Minimalist Bench", "Solid white oak bench for seated posture & tea ritual."),
            RoomZoneOption("zen_pebbles", "Raked Zen Gravel Tray", "🪨", 1, 4, "Tactile Mindfulness", "Smoothed river stones & raked sand for grounding."),
            RoomZoneOption("rattan_lounger", "Cane Rattan Lounger", "🪑", 1, 3, "Restorative Cradle", "Natural woven cane chair for deep body relaxation.")
        )
    ),
    RoomZone(
        zoneId = "sensory",
        title = "Sensory & Hydro-Acoustics",
        subtitle = "Mindful sound, warmth & aromatherapy anchors",
        iconEmoji = "🎋",
        options = listOf(
            RoomZoneOption("bamboo_spout", "Bamboo Water Spout", "🎋", 2, 5, "Hydro White Noise", "Continuous trickling water sound masking urban noise."),
            RoomZoneOption("singing_bowl", "Brass 432Hz Singing Bowl", "🥣", 1, 3, "Harmonic Resonance", "Hand-hammered bowl emitting 432Hz meditation tone."),
            RoomZoneOption("salt_lamp", "Himalayan Salt Glow Lamp", "🪔", 1, 2, "2700K Amber Ionization", "Soft warm ionization lamp for circadian relaxation."),
            RoomZoneOption("tea_set", "Cast Iron Tea Ritual", "🫖", 1, 2, "Herbal Mindfulness", "Handcrafted teapot for post-session tea ceremony."),
            RoomZoneOption("aroma_diffuser", "Ultrasound Mist Diffuser", "🕯️", 2, 2, "Eucalyptus Mist", "Disperses pure eucalyptus & cedarwood essential mist.")
        )
    )
)

data class SanctuaryPreset(
    val id: String,
    val title: String,
    val emoji: String,
    val subtitle: String,
    val substrateIndex: Int,
    val lightingIndex: Int,
    val selectedOptionIds: Map<String, String> // ZoneId -> OptionId
)

private val SANCTUARY_PRESETS = listOf(
    SanctuaryPreset(
        id = "zen_meditation",
        title = "Zen Meditation Nook",
        emoji = "🧘",
        subtitle = "Quiet acoustics, zafu seating & night oxygen",
        substrateIndex = 1, // Tatami Mat
        lightingIndex = 0, // Dawn Gold
        selectedOptionIds = mapOf(
            "canopy" to "moss_wall",
            "foliage" to "snake_plant_trio",
            "seating" to "zafu_tatami",
            "sensory" to "bamboo_spout"
        )
    ),
    SanctuaryPreset(
        id = "mindful_oxygen",
        title = "Mindful Oxygen Lounge",
        emoji = "🌬️",
        subtitle = "High-output oxygen foliage & humidifying ferns",
        substrateIndex = 0, // Light Oak
        lightingIndex = 1, // Forest Dappled
        selectedOptionIds = mapOf(
            "canopy" to "hydro_living_wall",
            "foliage" to "peace_lily_bloom",
            "seating" to "oak_bench",
            "sensory" to "aroma_diffuser"
        )
    ),
    SanctuaryPreset(
        id = "rainforest_relax",
        title = "Rainforest Relaxation",
        emoji = "🌿",
        subtitle = "Lush split-leaf canopy & soothing rain water",
        substrateIndex = 4, // Bamboo Fiber
        lightingIndex = 2, // Sunset Amber
        selectedOptionIds = mapOf(
            "canopy" to "monstera_canopy",
            "foliage" to "boston_fern_cloud",
            "seating" to "rattan_lounger",
            "sensory" to "singing_bowl"
        )
    ),
    SanctuaryPreset(
        id = "tea_nook",
        title = "Restorative Tea Nook",
        emoji = "☕",
        subtitle = "Warm wood, quiet tea ceremony & amber glow",
        substrateIndex = 0, // Light Oak
        lightingIndex = 2, // Sunset Amber
        selectedOptionIds = mapOf(
            "canopy" to "cascading_pothos",
            "foliage" to "aromatic_lavender",
            "seating" to "zen_pebbles",
            "sensory" to "tea_set"
        )
    )
)

data class LightingMood(
    val name: String,
    val emoji: String,
    val colorOverlay: Color,
    val description: String
)

private val LIGHTING_MOODS = listOf(
    LightingMood("Dawn Gold", "🌅", Color(0xFFFFF8E7).copy(alpha = 0.18f), "Warm 4000K morning light"),
    LightingMood("Forest Dappled", "🌲", Color(0xFFE8F5E9).copy(alpha = 0.28f), "Soft emerald 520nm filter"),
    LightingMood("Sunset Amber", "🌇", Color(0xFFFFECB3).copy(alpha = 0.32f), "Relaxing 2700K dusk glow"),
    LightingMood("Moonlit Indigo", "🌙", Color(0xFFE8EAF6).copy(alpha = 0.22f), "Cool serene night twilight")
)

data class AmbientSoundscape(
    val name: String,
    val emoji: String,
    val hzInfo: String
)

private val SOUNDSCAPES = listOf(
    AmbientSoundscape("Bamboo Fountain", "🎋", "White noise water drip"),
    AmbientSoundscape("Pine Forest Wind", "🌲", "Soft low-frequency breeze"),
    AmbientSoundscape("432Hz Singing Bowl", "🔔", "Harmonic meditation tone"),
    AmbientSoundscape("Rainforest Rain", "🌧️", "Dappled foliage droplets")
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    viewModel: GardenViewModel,
    switchToChatTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeLayout by viewModel.activeLayout.collectAsStateWithLifecycle()

    var selectedPreset by remember { mutableStateOf(SANCTUARY_PRESETS[0]) }
    var selectedSubstrateIdx by remember { mutableIntStateOf(1) } // Tatami Mat
    var selectedLightingIdx by remember { mutableIntStateOf(0) } // Dawn Gold
    var selectedSoundscapeIdx by remember { mutableIntStateOf(0) }
    var isSoundPlaying by remember { mutableStateOf(false) }

    // Map of Zone ID to selected Option ID
    val activeZoneSelection = remember {
        mutableStateMapOf<String, String>().apply {
            putAll(SANCTUARY_PRESETS[0].selectedOptionIds)
        }
    }

    val currentSoilTheme = SOIL_THEMES[selectedSubstrateIdx.coerceIn(0, SOIL_THEMES.size - 1)]
    val currentLayout = activeLayout

    val activeGridItems = remember(currentLayout?.gridString) {
        parseGridString(currentLayout?.gridString ?: "")
    }

    // Active zone options
    val activeZoneOptions = remember(activeZoneSelection.toMap()) {
        ROOM_ZONES.mapNotNull { zone ->
            val optId = activeZoneSelection[zone.zoneId]
            zone.options.find { it.id == optId }
        }
    }

    // Live Metrics Calculations
    val airPurificationScore = remember(activeZoneOptions) {
        val o2Sum = activeZoneOptions.sumOf { it.o2Rating }
        ((o2Sum.toFloat() / 16f) * 100f).coerceIn(40f, 99f).roundToInt()
    }

    val acousticScore = remember(activeZoneOptions, selectedSubstrateIdx) {
        val acousticSum = activeZoneOptions.sumOf { it.acousticRating }
        ((acousticSum.toFloat() / 16f) * 90f + selectedSubstrateIdx * 2).coerceIn(45f, 98f).roundToInt()
    }

    val visualZenScore = remember(activeZoneOptions, selectedSubstrateIdx, selectedLightingIdx) {
        (78 + activeZoneOptions.size * 4 + selectedSubstrateIdx * 2).coerceIn(60, 99)
    }

    val activeTerpenes = remember(activeZoneOptions) {
        activeZoneOptions.map { it.calmingScent }.distinct()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        currentSoilTheme.bgColors[0].copy(alpha = 0.30f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
            .testTag("plot_workspace")
    ) {
        // Top Header - Clean Title without duplicate Breathe button
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
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
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Biophilic Design Studio",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = "ROOM STUDIO",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Compose a serene indoor room for meditation, relaxation & mindful breathing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Intention Presets Carousel
                Text(
                    text = "SANCTUARY PRESETS",
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
                    items(SANCTUARY_PRESETS) { preset ->
                        val isSelected = selectedPreset.id == preset.id
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedPreset = preset
                                selectedSubstrateIdx = preset.substrateIndex
                                selectedLightingIdx = preset.lightingIndex
                                activeZoneSelection.clear()
                                activeZoneSelection.putAll(preset.selectedOptionIds)
                            },
                            label = {
                                Text(
                                    text = "${preset.emoji} ${preset.title}",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main Content Area
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {

            // SPATIAL ROOM SCENE VISUALIZER SHOWCASE CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = currentSoilTheme.bgColors[0]),
                border = BorderStroke(2.dp, currentSoilTheme.outlineColor.copy(alpha = 0.4f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LIGHTING_MOODS[selectedLightingIdx].colorOverlay)
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            ) {
                                Text(selectedPreset.emoji, fontSize = 22.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = selectedPreset.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = selectedPreset.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
                            ) {
                                Text(
                                    text = currentSoilTheme.name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Visual Room Scene Rendering Showcase
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = currentSoilTheme.bgColors[1].copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, currentSoilTheme.outlineColor.copy(alpha = 0.3f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Top Row: Canopy Feature (Left) & Lighting Mood (Right)
                                    val canopyOpt = activeZoneOptions.find { opt -> ROOM_ZONES[0].options.any { it.id == opt.id } }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(canopyOpt?.iconEmoji ?: "🖼️", fontSize = 16.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = canopyOpt?.name ?: "Wall Feature",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        // Lighting badge
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.90f)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(LIGHTING_MOODS[selectedLightingIdx].emoji, fontSize = 16.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = LIGHTING_MOODS[selectedLightingIdx].name,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }

                                    // Center Row: Air Purifying Foliage Layer
                                    val foliageOpt = activeZoneOptions.find { opt -> ROOM_ZONES[1].options.any { it.id == opt.id } }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                            shadowElevation = 4.dp
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                            ) {
                                                Text(foliageOpt?.iconEmoji ?: "🪴", fontSize = 22.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = foliageOpt?.name ?: "Air Purifier",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = foliageOpt?.calmingScent ?: "Active Oxygen",
                                                        fontSize = 9.sp,
                                                        color = Color(0xFF2E7D32),
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Bottom Row: Seating & Hydro-Acoustic Anchors
                                    val seatingOpt = activeZoneOptions.find { opt -> ROOM_ZONES[2].options.any { it.id == opt.id } }
                                    val sensoryOpt = activeZoneOptions.find { opt -> ROOM_ZONES[3].options.any { it.id == opt.id } }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(seatingOpt?.iconEmoji ?: "🧘", fontSize = 16.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = seatingOpt?.name ?: "Seating",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(sensoryOpt?.iconEmoji ?: "🎋", fontSize = 16.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = sensoryOpt?.name ?: "Sensory",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Substrate & Atmosphere Controls
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "FLOOR SUBSTRATE & ATMOSPHERE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )

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
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

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

            // MODULAR ZONE-BASED ROOM COMPOSER (4 Core Zones)
            Text(
                text = "MODULAR ROOM COMPOSER",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ROOM_ZONES.forEach { zone ->
                    val selectedOptId = activeZoneSelection[zone.zoneId]
                    val selectedOpt = zone.options.find { it.id == selectedOptId }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                ) {
                                    Text(zone.iconEmoji, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = zone.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = zone.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (selectedOpt != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = selectedOpt.name,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Options selector chips
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(zone.options) { option ->
                                    val isSelected = selectedOptId == option.id
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                        ),
                                        modifier = Modifier
                                            .clickable { activeZoneSelection[zone.zoneId] = option.id }
                                            .width(135.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(option.iconEmoji, fontSize = 20.sp)
                                                Text(
                                                    text = "O2 +${option.o2Rating}",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF2E7D32)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = option.name,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = option.calmingScent,
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
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // LIVE BIOPHILIC SERENITY METRICS DASHBOARD
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
                            letterSpacing = 1.sp,
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Real-Time Spatial Output",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
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

                        // Metric 3: Visual Zen Balance
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
                                Text("$visualZenScore%", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFF57C00))
                                LinearProgressIndicator(
                                    progress = { visualZenScore.toFloat() / 100f },
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

                    // Active Aromatherapy Profile
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
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Soundscape Player
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
