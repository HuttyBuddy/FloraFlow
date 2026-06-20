package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ClimatePlants
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.example.data.model.Plant
import com.example.data.model.PlantTemplate
import com.example.ui.viewmodel.GardenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: GardenViewModel,
    switchToChatTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeLayout by viewModel.activeLayout.collectAsStateWithLifecycle()
    val activePlants by viewModel.activePlants.collectAsStateWithLifecycle()

    var showAddPlantDialog by remember { mutableStateOf(false) }
    var expandedPlantId by remember { mutableStateOf<Int?>(null) }
    var selectedTabState by remember { mutableIntStateOf(0) } // 0 = My Garden, 1 = Species Encyclopedia

    val climateName = activeLayout?.climate ?: "Temperate"
    val companionTemplates = remember(climateName) {
        ClimatePlants.getTemplatesForClimate(climateName)
    }

    // Encyclopedia Search & Filter States
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("All") }
    var selectedClimateFilter by remember { mutableStateOf("All") }
    var selectedWaterFilter by remember { mutableStateOf("All") }
    var selectedBloomFilter by remember { mutableStateOf("All") }
    var expandedSpeciesName by remember { mutableStateOf<String?>(null) }

    val filteredTemplates = remember(searchQuery, selectedTypeFilter, selectedClimateFilter, selectedWaterFilter, selectedBloomFilter) {
        ClimatePlants.ALL_TEMPLATES.filter { tpl ->
            val matchesSearch = tpl.name.contains(searchQuery, ignoreCase = true) ||
                    tpl.type.contains(searchQuery, ignoreCase = true) ||
                    tpl.soilType.contains(searchQuery, ignoreCase = true) ||
                    tpl.sunlight.contains(searchQuery, ignoreCase = true) ||
                    tpl.pestsDiseases.contains(searchQuery, ignoreCase = true)
            
            val matchesType = selectedTypeFilter == "All" || tpl.type.lowercase() == selectedTypeFilter.lowercase()
            val matchesClimate = selectedClimateFilter == "All" || tpl.compatibleClimate.contains(selectedClimateFilter, ignoreCase = true)
            val matchesWater = selectedWaterFilter == "All" || tpl.wateringNeeds.contains(selectedWaterFilter, ignoreCase = true)
            val matchesBloom = selectedBloomFilter == "All" || tpl.bloomTime.contains(selectedBloomFilter, ignoreCase = true)
            
            matchesSearch && matchesType && matchesClimate && matchesWater && matchesBloom
        }
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp || configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.smallestScreenWidthDp >= 600
    val isWideScreen = isLandscape && (isTablet || configuration.screenWidthDp >= 600)

    // ----------------------------------------------------
    // LOCAL COMPOSABLES (extracted layout segments)
    // ----------------------------------------------------
    val tabBarContent = @Composable {
        // Aesthetic Natural Pill Tab Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selectedTabState == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { selectedTabState = 0 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Spa,
                        contentDescription = "My Garden",
                        tint = if (selectedTabState == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "My Garden Care", 
                        color = if (selectedTabState == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selectedTabState == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { selectedTabState = 1 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = "Encyclopedia",
                        tint = if (selectedTabState == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Species Encyclopedia", 
                        color = if (selectedTabState == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    val climateRecommendationsContent = @Composable {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Climate Recommendations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Compatible plant templates for your $climateName climate. Tap a recommendation to plant it in your active garden layout.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }

    val companionTemplatesContent = @Composable {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(companionTemplates) { tpl ->
                val isCultivated = activePlants.any { it.name.lowercase() == tpl.name.lowercase() }
                
                InputChip(
                    selected = isCultivated,
                    onClick = {
                        if (!isCultivated) {
                            viewModel.addPlant(tpl.name, tpl.type, tpl)
                        }
                    },
                    label = { Text(tpl.name, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = { Text(tpl.iconEmoji, fontSize = 14.sp) },
                    trailingIcon = {
                        if (isCultivated) {
                            Icon(Icons.Default.Check, contentDescription = "Cultivated", modifier = Modifier.size(12.dp))
                        } else {
                            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(12.dp))
                        }
                    }
                )
            }
        }
    }

    val activePlantsHeaderContent = @Composable {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Cultivated Vegetation Hub",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Seasonal care trackers and growth indices",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            IconButton(
                onClick = { showAddPlantDialog = true },
                modifier = Modifier.testTag("add_custom_plant_fab")
            ) {
                Icon(
                    Icons.Default.AddCircle,
                    contentDescription = "Add plant",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }

    val activePlantsListContent = @Composable {
        if (activeLayout == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Choose a Garden First",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Select or create a new garden project in the Dashboard to list and manage your plants.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        } else if (activePlants.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Your Greenhouse is Empty",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Plant one of the $climateName recommendations above, place them on the grid canvas, or click the add button to insert custom vegetation.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                activePlants.forEach { plant ->
                    val isExpanded = expandedPlantId == plant.id
                    PlantCareTrackerCard(
                        plant = plant,
                        isExpanded = isExpanded,
                        onExpandClick = {
                            expandedPlantId = if (isExpanded) null else plant.id
                        },
                        onGrowthChange = { progress ->
                            viewModel.updatePlantProgress(plant.id, progress)
                        },
                        onDeletePlant = {
                            viewModel.deletePlant(plant.id)
                        },
                        onConsultAi = {
                            viewModel.sendAiChatMessage("Give me extreme care and growth advice for cultivating my ${plant.name} in details.")
                            switchToChatTab()
                        }
                    )
                }
            }
        }
    }

    val encyclopediaHeaderContent = @Composable {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Botanical Encyclopedia",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Explore specific mature dimensions, watering schedules, bloom cycles, and ideal soil categories.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            
            Spacer(modifier = Modifier.height(6.dp))

            // Real-Time Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search 23+ species, pests, or soils...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search input")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("species_search_input"),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    }

    val encyclopediaFiltersContent = @Composable {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            // Category Type Filter
            Column {
                Text("Plant Group Type:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val types = listOf("All", "Flower", "Shrub", "Succulent", "Herb", "Tree", "Fern")
                    items(types) { t ->
                        FilterChip(
                            selected = selectedTypeFilter == t,
                            onClick = { selectedTypeFilter = t },
                            label = { Text(t, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // Climate Compatibility Filter
            Column {
                Text("Climate Compatibility:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val climates = listOf("All", "Temperate", "Arid", "Tropical", "Mediterranean", "Mountainous")
                    items(climates) { c ->
                        FilterChip(
                            selected = selectedClimateFilter == c,
                            onClick = { selectedClimateFilter = c },
                            label = { Text(c, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // Watering Profile Filter
            Column {
                Text("Watering Intensity:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val waterFilters = listOf("All", "Low", "Moderate", "High")
                    items(waterFilters) { w ->
                        FilterChip(
                            selected = selectedWaterFilter == w,
                            onClick = { selectedWaterFilter = w },
                            label = { Text(w, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
            
            // Bloom Time Filter
            Column {
                Text("Bloom Season:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val bloomFilters = listOf("All", "Spring", "Summer", "Autumn", "Winter", "Year-round")
                    items(bloomFilters) { b ->
                        FilterChip(
                            selected = selectedBloomFilter == b,
                            onClick = { selectedBloomFilter = b },
                            label = { Text(b, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
            
            // Clear filters helper
            if (selectedTypeFilter != "All" || selectedClimateFilter != "All" || selectedWaterFilter != "All" || selectedBloomFilter != "All" || searchQuery.isNotEmpty()) {
                TextButton(
                    onClick = {
                        searchQuery = ""
                        selectedTypeFilter = "All"
                        selectedClimateFilter = "All"
                        selectedWaterFilter = "All"
                        selectedBloomFilter = "All"
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = "Reset filter", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset All Filters", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    val encyclopediaResultsCountContent = @Composable {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filteredTemplates.size} species catalogued",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            if (searchQuery.isNotEmpty() || selectedTypeFilter != "All" || selectedClimateFilter != "All" || selectedWaterFilter != "All" || selectedBloomFilter != "All") {
                SuggestionChip(
                    onClick = {},
                    label = { Text("Filtered", fontSize = 9.sp) },
                    icon = { Icon(Icons.Default.FilterList, contentDescription = "Filtered info", modifier = Modifier.size(10.dp)) }
                )
            }
        }
    }

    val encyclopediaListContent = @Composable {
        if (filteredTemplates.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.SearchOff,
                        contentDescription = "No results",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Even in a vast forest, some seeds are rare. Try adjusting your search filters to find a match.",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Try adjusting your search keywords or switching filter chips to find compatible varieties.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // List of filtered species templates
                filteredTemplates.forEach { tpl ->
                    val isExpanded = expandedSpeciesName == tpl.name
                    val isPlanted = activePlants.any { it.name.lowercase() == tpl.name.lowercase() }

                    SpeciesEncyclopediaCard(
                        template = tpl,
                        isExpanded = isExpanded,
                        onExpandClick = {
                            expandedSpeciesName = if (isExpanded) null else tpl.name
                        },
                        isPlanted = isPlanted,
                        activeLayoutAvailable = activeLayout != null,
                        onSowPlant = {
                            viewModel.addPlant(tpl.name, tpl.type, tpl)
                        }
                    )
                }
            }
        }
    }

    if (!isWideScreen) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            item { tabBarContent() }
            if (selectedTabState == 0) {
                item { climateRecommendationsContent() }
                item { companionTemplatesContent() }
                item { activePlantsHeaderContent() }
                item { activePlantsListContent() }
            } else {
                item { encyclopediaHeaderContent() }
                item { encyclopediaFiltersContent() }
                item { encyclopediaResultsCountContent() }
                item { encyclopediaListContent() }
            }
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Left Pane (Filters, selectors, tabs): Weight 0.38f
            Column(
                modifier = Modifier
                    .weight(0.38f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                tabBarContent()
                if (selectedTabState == 0) {
                    climateRecommendationsContent()
                    companionTemplatesContent()
                    activePlantsHeaderContent()
                } else {
                    encyclopediaHeaderContent()
                    encyclopediaFiltersContent()
                }
            }

            // Right Pane (The List/Results area): Weight 0.62f
            Column(
                modifier = Modifier
                    .weight(0.62f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (selectedTabState == 0) {
                    Text(
                        "Cultivated Sandboxes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    activePlantsListContent()
                } else {
                    encyclopediaResultsCountContent()
                    encyclopediaListContent()
                }
                Spacer(modifier = Modifier.height(80.dp)) // Padding for bottom/navigation bar
            }
        }
    }

    // Add Custom Plant Dialog
    if (showAddPlantDialog) {
        AddCustomPlantDialog(
            onDismiss = { showAddPlantDialog = false },
            onAdd = { pName, pType ->
                viewModel.addPlant(pName, pType, null)
                showAddPlantDialog = false
            }
        )
    }
}

@Composable
fun GrowthTreeRingsIndicator(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val ringColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    val woodColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)

    Canvas(modifier = modifier) {
        val sizeMin = size.minDimension
        val center = Offset(size.width / 2, size.height / 2)

        // 1. Draw organic inner tree rings (concentric circles)
        val ringCount = 3
        for (i in 1..ringCount) {
            val fraction = i.toFloat() / (ringCount + 1)
            val radius = (sizeMin / 2) * fraction
            drawCircle(
                color = woodColor,
                radius = radius,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // 2. Draw outer track (the full circle representing 100% potential)
        val outerRadius = sizeMin / 2 - 4.dp.toPx()
        drawCircle(
            color = trackColor,
            radius = outerRadius,
            style = Stroke(width = 3.dp.toPx())
        )

        // 3. Draw progress arc (the growth rings completed)
        val sweepAngle = (progress / 100f) * 360f
        drawArc(
            color = ringColor,
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
            size = Size(outerRadius * 2, outerRadius * 2),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

// --- Specific Plant Care Info Expandable Card ---
@Composable
fun PlantCareTrackerCard(
    plant: Plant,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    onGrowthChange: (Int) -> Unit,
    onDeletePlant: () -> Unit,
    onConsultAi: () -> Unit
) {
    val emoji = getEmojiForPlantName(plant.name)
    var localGrowthProgress by remember(plant.growthProgress) { mutableFloatStateOf(plant.growthProgress.toFloat()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("plant_care_card_${plant.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Summary header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandClick() }
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 24.sp)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = plant.name,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Growth: ${plant.growthProgress}%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                            GrowthTreeRingsIndicator(
                                progress = plant.growthProgress.toFloat(),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = plant.type,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "•",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Text(
                            text = plant.sunlight,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1
                        )
                    }
                }

                IconButton(onClick = onExpandClick) {
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand details"
                    )
                }
            }

            // Expandable details (Seasons Care & Growth adjustments)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Core Botanical specifications grid
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Sunlight: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(plant.sunlight, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Soil Substrate: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(plant.soilType, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Watering Needs: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(plant.wateringNeeds, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Bloom Time: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(plant.bloomTime, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Pests / Hazards: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(plant.pestsDiseases, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    // Season trackers
                    Text("Seasonal Care Guidelines:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SeasonCareBadge("Spring", plant.careSpring, modifier = Modifier.weight(1f))
                        SeasonCareBadge("Summer", plant.careSummer, modifier = Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SeasonCareBadge("Autumn", plant.careAutumn, modifier = Modifier.weight(1f))
                        SeasonCareBadge("Winter", plant.careWinter, modifier = Modifier.weight(1f))
                    }

                    val template = remember(plant.name) {
                        com.example.data.model.ClimatePlants.ALL_TEMPLATES.find { it.name.equals(plant.name, ignoreCase = true) }
                    }
                    val funFact = remember(template) {
                        template?.funFacts?.randomOrNull() ?: ""
                    }
                    if (funFact.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "💡 Did you know?",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = funFact,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }

                    // Growth Progress Adjustment Slider
                    Text("Update Live Growth progress index:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Slider(
                            value = localGrowthProgress,
                            onValueChange = { localGrowthProgress = it },
                            onValueChangeFinished = {
                                onGrowthChange(localGrowthProgress.toInt())
                            },
                            valueRange = 0f..100f,
                            modifier = Modifier.weight(1f).testTag("growth_slider_${plant.id}")
                        )
                        Text("${localGrowthProgress.toInt()}%", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }

                    // Card Actions panel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onConsultAi,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.5f).testTag("consult_ai_plant_btn")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Consult AI Bot", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onDeletePlant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("delete_plant_btn")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Uproot", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// --- Specific Plant Encyclopedia Information Card ---
@Composable
fun SpeciesEncyclopediaCard(
    template: PlantTemplate,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    isPlanted: Boolean,
    activeLayoutAvailable: Boolean,
    onSowPlant: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("species_card_${template.name.replace(" ", "_")}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandClick() }
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(template.iconEmoji, fontSize = 24.sp)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        template.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = template.type,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "•",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Text(
                            text = "Climate: ${template.compatibleClimate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                IconButton(onClick = onExpandClick) {
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand details"
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Care parameters overview table
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Ideal Sun Exposure: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(template.sunlight, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Substrate / Soil: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(template.soilType, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Water Index level: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(template.wateringNeeds, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Mature Dimensions: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(template.matureSize, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Bloom Timeframe: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(template.bloomTime, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Pests & Pathologies: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(template.pestsDiseases, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    // Season care guidelines
                    Text("Seasonal Guideline Summary:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SeasonCareBadge("Spring", template.careSpring, modifier = Modifier.weight(1f))
                        SeasonCareBadge("Summer", template.careSummer, modifier = Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SeasonCareBadge("Autumn", template.careAutumn, modifier = Modifier.weight(1f))
                        SeasonCareBadge("Winter", template.careWinter, modifier = Modifier.weight(1f))
                    }

                    val funFact = remember(template.name) {
                        template.funFacts.randomOrNull() ?: ""
                    }
                    if (funFact.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "💡 Did you know?",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = funFact,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }

                    // Add button to inventory
                    if (isPlanted) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Celebration, contentDescription = "Planted success icon", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Planted & Growing in Greenhouse Layout!",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                if (activeLayoutAvailable) {
                                    onSowPlant()
                                }
                            },
                            enabled = activeLayoutAvailable,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.LocalFlorist, contentDescription = "Cultivate icons", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (activeLayoutAvailable) "Sow in My Active Garden" else "Choose primary layout in Dashboard first",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Season Care Text Cards ---
@Composable
fun SeasonCareBadge(
    season: String,
    careInfo: String,
    modifier: Modifier = Modifier
) {
    val (color, emoji) = when (season) {
        "Spring" -> Color(0xFFE8F5E9) to "🌸"
        "Summer" -> Color(0xFFFFFDE7) to "☀️"
        "Autumn" -> Color(0xFFFFE0B2) to "🍁"
        else -> Color(0xFFE1F5FE) to "❄️"
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color)
            .padding(10.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(season, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = careInfo,
                fontSize = 9.sp,
                lineHeight = 12.sp,
                color = Color.DarkGray,
                maxLines = 3
            )
        }
    }
}

// --- DIALOG: Add Custom Plant ---
@Composable
fun AddCustomPlantDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, type: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Flower") }

    val typeOptions = listOf("Flower", "Shrub", "Succulent", "Herb", "Fern", "Tree")

    Dialog(onDismissRequest = onDismiss) {
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
                    text = "Plant Custom Vegetation",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Plant / Crop Name") },
                    placeholder = { Text("Golden Pothos") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_custom_plant_name"),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("Plant Category Type:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(typeOptions) { opt ->
                        val isSelected = type == opt
                        FilterChip(
                            selected = isSelected,
                            onClick = { type = opt },
                            label = { Text(opt) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onAdd(name, type)
                            }
                        },
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("submit_add_custom_plant")
                    ) {
                        Text("Cultivate")
                    }
                }
            }
        }
    }
}
