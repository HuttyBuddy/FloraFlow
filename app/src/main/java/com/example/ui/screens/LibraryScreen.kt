package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.ui.theme.extendedColors
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.example.data.model.Plant
import com.example.data.model.PlantTemplate
import com.example.ui.viewmodel.GardenViewModel
import com.example.data.model.CareTask
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.ui.components.PlantImages
import com.example.ui.components.PlantPhoto

// ⚡ Bolt Performance Optimization:
// Extracted static lists to top-level constants. By moving these outside of the
// composable function, we avoid allocating new list objects on every recomposition
// of the LibraryScreen. This reduces GC pressure and improves rendering performance,
// particularly when filtering or scrolling causes frequent UI updates.
private val FILTER_TYPES = listOf("All", "Flower", "Shrub", "Succulent", "Herb", "Tree", "Fern")
private val FILTER_CLIMATES = listOf("All", "Temperate", "Arid", "Tropical", "Mediterranean", "Mountainous")
private val FILTER_WATER = listOf("All", "Low", "Moderate", "High")
private val FILTER_BLOOMS = listOf("All", "Spring", "Summer", "Autumn", "Winter", "Year-round")
private val ADD_CUSTOM_PLANT_TYPES = listOf("Flower", "Shrub", "Succulent", "Herb", "Fern", "Tree")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: GardenViewModel,
    switchToChatTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeLayout by viewModel.activeLayout.collectAsStateWithLifecycle()
    val activePlants by viewModel.activePlants.collectAsStateWithLifecycle()
    val pendingTasks by viewModel.pendingCareTasks.collectAsStateWithLifecycle()

    // Calculate greenhouse climate stats reactively
    val avgGrowth = remember(activePlants) {
        if (activePlants.isEmpty()) 0 else activePlants.map { it.growthProgress }.average().toInt()
    }
    val avgHydration = remember(activePlants, pendingTasks) {
        if (activePlants.isEmpty()) 1f else {
            val now = System.currentTimeMillis()
            activePlants.map { plant ->
                val waterTask = pendingTasks.find { it.plantId == plant.id && it.taskType == "WATER" }
                if (waterTask == null) 1f else {
                    val timeLeft = waterTask.dueDate - now
                    val totalTime = waterTask.intervalDays * 24 * 60 * 60 * 1000L
                    (timeLeft.toFloat() / totalTime.toFloat()).coerceIn(0f, 1f)
                }
            }.average().toFloat()
        }
    }
    val dueTasksCount = remember(activePlants, pendingTasks) {
        val activePlantIds = activePlants.map { it.id }.toSet()
        pendingTasks.filter { it.plantId in activePlantIds && it.dueDate <= System.currentTimeMillis() }.size
    }

    var showAddPlantDialog by remember { mutableStateOf(false) }
    var expandedPlantId by remember { mutableStateOf<Int?>(null) }
    var selectedTabState by remember { mutableIntStateOf(0) } // 0 = My Garden, 1 = Species Encyclopedia

    val climateName = activeLayout?.climate ?: "Temperate"
    val companionTemplates = remember(climateName) {
        ClimatePlants.getTemplatesForClimate(climateName)
    }

    // Encyclopedia Search & Filter States
    val currentWeather by viewModel.currentWeather.collectAsStateWithLifecycle()
    var zipInput by remember(currentWeather) { mutableStateOf(viewModel.getWeatherLocationZip()) }
    val searchQuery by viewModel.librarySearchQuery.collectAsStateWithLifecycle()
    val smartSuggestions by viewModel.smartSearchSuggestions.collectAsStateWithLifecycle()
    val dynamicPlaceholder by viewModel.dynamicSearchPlaceholder.collectAsStateWithLifecycle()
    var selectedSuggestionExplanation by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("All") }
    var selectedClimateFilter by remember { mutableStateOf("All") }
    var selectedWaterFilter by remember { mutableStateOf("All") }
    var selectedBloomFilter by remember { mutableStateOf("All") }
    var expandedSpeciesName by remember { mutableStateOf<String?>(null) }

    val filteredTemplates = remember(searchQuery, selectedTypeFilter, selectedClimateFilter, selectedWaterFilter, selectedBloomFilter, zipInput) {
        val zipClimate = ClimatePlants.mapZipToClimate(zipInput)
        val zipClimateShort = when {
            zipClimate.contains("Tropical") -> "Tropical"
            zipClimate.contains("Arid") -> "Arid"
            else -> zipClimate
        }
        ClimatePlants.ALL_TEMPLATES.filter { tpl ->
            val matchesSearch = tpl.name.contains(searchQuery, ignoreCase = true) ||
                    tpl.type.contains(searchQuery, ignoreCase = true)
            
            val matchesType = selectedTypeFilter == "All" || tpl.type.lowercase() == selectedTypeFilter.lowercase()
            val matchesZipClimate = zipInput.isBlank() || tpl.compatibleClimate.contains(zipClimateShort, ignoreCase = true)
            val matchesClimate = selectedClimateFilter == "All" || tpl.compatibleClimate.contains(selectedClimateFilter, ignoreCase = true)
            val matchesWater = selectedWaterFilter == "All" || tpl.wateringNeeds.contains(selectedWaterFilter, ignoreCase = true)
            val matchesBloom = selectedBloomFilter == "All" || tpl.bloomTime.contains(selectedBloomFilter, ignoreCase = true)
            
            matchesSearch && matchesType && matchesZipClimate && matchesClimate && matchesWater && matchesBloom
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
            items(companionTemplates, key = { it.name }) { tpl ->
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
                    val plantTasks = remember(pendingTasks, plant.id) {
                        pendingTasks.filter { it.plantId == plant.id }
                    }
                    val waterTask = remember(plantTasks) { plantTasks.find { it.taskType == "WATER" } }
                    val fertilizeTask = remember(plantTasks) { plantTasks.find { it.taskType == "FERTILIZE" } }
                    val pruneTask = remember(plantTasks) { plantTasks.find { it.taskType == "PRUNE" } }

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
                        },
                        waterTask = waterTask,
                        fertilizeTask = fertilizeTask,
                        pruneTask = pruneTask,
                        onCompleteTask = { task ->
                            viewModel.completeCareTask(task)
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

            // Zip Code Field
            OutlinedTextField(
                value = zipInput,
                onValueChange = { 
                    zipInput = it
                    if (it.length == 5 && it.all { c -> c.isDigit() }) {
                        viewModel.updateWeatherLocation(it)
                    }
                },
                placeholder = { Text("Zip Code") },
                label = { Text("Local Zip") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = "Zip Code Location") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("encyclopedia_zip_input"),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setLibrarySearchQuery(it) },
                placeholder = { Text(dynamicPlaceholder) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { 
                            viewModel.setLibrarySearchQuery("") 
                            selectedSuggestionExplanation = ""
                        }) {
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

            // Smart Suggestions UI Panel
            AnimatedVisibility(
                visible = searchQuery.isEmpty() && smartSuggestions.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Suggestion icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Predicted for You",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Based on activity",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            IconButton(
                                onClick = {
                                    viewModel.clearLearningHistory()
                                    selectedSuggestionExplanation = ""
                                },
                                modifier = Modifier.size(20.dp).testTag("clear_learning_history_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = "Reset History",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    // Scrolling chips list
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(smartSuggestions) { suggestion ->
                            val isChosen = searchQuery == suggestion.query
                            FilterChip(
                                selected = isChosen,
                                onClick = {
                                    viewModel.setLibrarySearchQuery(suggestion.query)
                                    selectedSuggestionExplanation = suggestion.explanation
                                },
                                label = {
                                    Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                        Text(
                                            text = suggestion.displayText,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = suggestion.category,
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("suggestion_chip_${suggestion.query.lowercase().replace(" ", "_")}")
                            )
                        }
                    }
                    
                    // Selected suggestion rationale
                    if (selectedSuggestionExplanation.isNotEmpty()) {
                        Text(
                            text = "💡 $selectedSuggestionExplanation",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f),
                            modifier = Modifier.padding(top = 4.dp).testTag("suggestion_rationale")
                        )
                    }
                }
            }

            // Info row showing resolved climate and the Clear All Filters button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val resolvedClimate = remember(zipInput) { ClimatePlants.mapZipToClimate(zipInput) }
                Text(
                    text = if (zipInput.isNotBlank()) "Climate: $resolvedClimate" else "Showing species from all around the world",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                
                Button(
                    onClick = {
                        viewModel.setLibrarySearchQuery("")
                        selectedTypeFilter = "All"
                        selectedClimateFilter = "All"
                        selectedWaterFilter = "All"
                        selectedBloomFilter = "All"
                        zipInput = ""
                        viewModel.updateWeatherLocation("")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("clear_all_filters_btn")
                ) {
                    Text("Clear All Filters", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
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
                    items(FILTER_TYPES, key = { it }) { t ->
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
                    items(FILTER_CLIMATES, key = { it }) { c ->
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
                    items(FILTER_WATER, key = { it }) { w ->
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
                    items(FILTER_BLOOMS, key = { it }) { b ->
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
                        viewModel.setLibrarySearchQuery("")
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
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            viewModel.setLibrarySearchQuery("")
                            selectedTypeFilter = "All"
                            selectedClimateFilter = "All"
                            selectedWaterFilter = "All"
                            selectedBloomFilter = "All"
                            zipInput = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Clear All Filters", fontWeight = FontWeight.Bold)
                    }
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
                            expandedSpeciesName = if (isExpanded) null else {
                                viewModel.recordSpeciesViewed(tpl.name)
                                tpl.name
                            }
                        },
                        isPlanted = isPlanted,
                        activeLayoutAvailable = activeLayout != null,
                        onSowPlant = {
                            viewModel.addPlant(tpl.name, tpl.type, tpl)
                        }
                    )
                }

                Text(
                    text = "Botanical photography courtesy of Wikimedia Commons contributors.",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
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
                item { GreenhouseStatsSection(avgGrowth = avgGrowth, avgHydration = avgHydration, dueTasksCount = dueTasksCount) }
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
                    GreenhouseStatsSection(avgGrowth = avgGrowth, avgHydration = avgHydration, dueTasksCount = dueTasksCount)
                    Spacer(modifier = Modifier.height(4.dp))
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
    onConsultAi: () -> Unit,
    waterTask: CareTask?,
    fertilizeTask: CareTask?,
    pruneTask: CareTask?,
    onCompleteTask: (CareTask) -> Unit
) {
    val emoji = getEmojiForPlantName(plant.name)
    var localGrowthProgress by remember(plant.growthProgress) { mutableFloatStateOf(plant.growthProgress.toFloat()) }

    val now = System.currentTimeMillis()
    val hydration = remember(waterTask, now) {
        if (waterTask == null) 1f else {
            val timeLeft = waterTask.dueDate - now
            val totalTime = waterTask.intervalDays * 24 * 60 * 60 * 1000L
            (timeLeft.toFloat() / totalTime.toFloat()).coerceIn(0f, 1f)
        }
    }
    val nutrients = remember(fertilizeTask, now) {
        if (fertilizeTask == null) 1f else {
            val timeLeft = fertilizeTask.dueDate - now
            val totalTime = fertilizeTask.intervalDays * 24 * 60 * 60 * 1000L
            (timeLeft.toFloat() / totalTime.toFloat()).coerceIn(0f, 1f)
        }
    }
    val pruning = remember(pruneTask, now) {
        if (pruneTask == null) 1f else {
            val timeLeft = pruneTask.dueDate - now
            val totalTime = pruneTask.intervalDays * 24 * 60 * 60 * 1000L
            (timeLeft.toFloat() / totalTime.toFloat()).coerceIn(0f, 1f)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("plant_care_card_${plant.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
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
                PlantPhoto(
                    plantName = plant.name,
                    fallbackEmoji = emoji,
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = plant.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (hydration < 0.25f) {
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.extendedColors.error.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "⚠️ Thirsty",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.extendedColors.error,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
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

                Spacer(modifier = Modifier.width(8.dp))

                // Quick Water action droplet
                if (waterTask != null) {
                    IconButton(
                        onClick = { onCompleteTask(waterTask) },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF29B6F6).copy(alpha = 0.12f), CircleShape)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            Icons.Default.WaterDrop,
                            contentDescription = "Log Water Quick",
                            tint = Color(0xFF0288D1),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.LightGray.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Watered Quick",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = onExpandClick) {
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse details" else "Expand details"
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Botanical Vitality Monitor Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        VitalityMetricColumn(
                            title = "Moisture",
                            value = "${(hydration * 100).toInt()}%",
                            status = when {
                                hydration < 0.25f -> "Dry 🚨"
                                hydration < 0.5f -> "Thirsty ⚠️"
                                else -> "Optimal 💧"
                            },
                            meter = {
                                WaterLevelMeter(
                                    hydration = hydration,
                                    modifier = Modifier.size(width = 36.dp, height = 70.dp)
                                )
                            },
                            buttonText = "Water",
                            task = waterTask,
                            onComplete = onCompleteTask,
                            modifier = Modifier.weight(1f)
                        )

                        VitalityMetricColumn(
                            title = "Nutrients",
                            value = "${(nutrients * 100).toInt()}%",
                            status = when {
                                nutrients < 0.2f -> "Starved 🧪"
                                nutrients < 0.5f -> "Depleted ⚠️"
                                else -> "Rich 🌱"
                            },
                            meter = {
                                VitalityLevelMeter(
                                    level = nutrients,
                                    color = Color(0xFF81C784),
                                    modifier = Modifier.size(width = 36.dp, height = 70.dp)
                                )
                            },
                            buttonText = "Feed",
                            task = fertilizeTask,
                            onComplete = onCompleteTask,
                            modifier = Modifier.weight(1f)
                        )

                        VitalityMetricColumn(
                            title = "Structure",
                            value = "${(pruning * 100).toInt()}%",
                            status = when {
                                pruning < 0.2f -> "Overgrown ✂️"
                                pruning < 0.5f -> "Untidy ⚠️"
                                else -> "Pruned ✨"
                            },
                            meter = {
                                VitalityLevelMeter(
                                    level = pruning,
                                    color = Color(0xFFBA68C8),
                                    modifier = Modifier.size(width = 36.dp, height = 70.dp)
                                )
                            },
                            buttonText = "Prune",
                            task = pruneTask,
                            onComplete = onCompleteTask,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Core Botanical specifications grid
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Sunlight Exposure: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(plant.sunlight, style = MaterialTheme.typography.bodySmall)
                            }
                            SunExposureIcons(plant.sunlight)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Soil Substrate: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(plant.soilType, style = MaterialTheme.typography.bodySmall)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Watering Needs: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(plant.wateringNeeds, style = MaterialTheme.typography.bodySmall)
                            }
                            WaterNeedsIcons(plant.wateringNeeds)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Bloom Time: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(plant.bloomTime, style = MaterialTheme.typography.bodySmall)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
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
                PlantPhoto(
                    plantName = template.name,
                    fallbackEmoji = template.iconEmoji,
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(12.dp)
                )

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
                        contentDescription = if (isExpanded) "Collapse details" else "Expand details"
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
                    // Full-width botanical hero photograph
                    val heroImageRes = remember(template.name) { PlantImages.forPlantName(template.name) }
                    if (heroImageRes != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .clip(RoundedCornerShape(14.dp))
                        ) {
                            Image(
                                painter = painterResource(id = heroImageRes),
                                contentDescription = "${template.name} photograph",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Species label overlay for a polished editorial look
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(10.dp)
                                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${template.iconEmoji} ${template.name}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Care parameters overview table
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Ideal Sun Exposure: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(template.sunlight, style = MaterialTheme.typography.bodySmall)
                            }
                            SunExposureIcons(template.sunlight)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Substrate / Soil: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(template.soilType, style = MaterialTheme.typography.bodySmall)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Water Index level: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(template.wateringNeeds, style = MaterialTheme.typography.bodySmall)
                            }
                            WaterNeedsIcons(template.wateringNeeds)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Mature Dimensions: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(template.matureSize, style = MaterialTheme.typography.bodySmall)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Bloom Timeframe: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(template.bloomTime, style = MaterialTheme.typography.bodySmall)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
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
                    items(ADD_CUSTOM_PLANT_TYPES, key = { it }) { opt ->
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

// --- Dynamic Greenhouse Stats Cards ---
@Composable
fun GreenhouseStatsSection(
    avgGrowth: Int,
    avgHydration: Float,
    dueTasksCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("greenhouse_stats_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Greenhouse Climate & Vitality Monitor",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Stat 1: Vitality / Average Growth
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = "Growth",
                            tint = Color(0xFF66BB6A),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Vitality index", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
                        Text("$avgGrowth%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                // Stat 2: Hydration Index
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.WaterDrop,
                            contentDescription = "Hydration",
                            tint = Color(0xFF29B6F6),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Hydration", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
                        Text("${(avgHydration * 100).toInt()}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                // Stat 3: Care Tasks checklist
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Eco,
                            contentDescription = "Tasks due",
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Tasks due", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
                        Text("$dueTasksCount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

// --- Custom Canvas Water Level Wave Meter ---
@Composable
fun WaterLevelMeter(
    hydration: Float,
    modifier: Modifier = Modifier
) {
    val color = when {
        hydration < 0.25f -> Color(0xFFEF5350)
        hydration < 0.5f -> Color(0xFFFFA726)
        else -> Color(0xFF29B6F6)
    }

    val outlineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    val bgColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val path = androidx.compose.ui.graphics.Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    rect = androidx.compose.ui.geometry.Rect(0f, 0f, width, height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )
            )
        }

        drawRoundRect(
            color = outlineColor,
            size = size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx(), 10.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx())
        )

        clipPath(path) {
            drawRect(
                color = bgColor,
                size = size
            )

            val waterHeight = height * hydration
            val topY = height - waterHeight

            if (hydration > 0f) {
                val waterPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, topY)
                    val waveHeight = 2.5.dp.toPx()
                    val waveLength = width
                    val segments = 20
                    for (i in 1..segments) {
                        val x = width * (i.toFloat() / segments)
                        val y = topY + kotlin.math.sin((x / waveLength) * 2 * kotlin.math.PI.toFloat() + 1.5f) * waveHeight
                        lineTo(x, y)
                    }
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }
                drawPath(path = waterPath, color = color)
            }
        }
    }
}

// --- Custom Canvas Vitality Level Bar ---
@Composable
fun VitalityLevelMeter(
    level: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val outlineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    val bgColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val path = androidx.compose.ui.graphics.Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    rect = androidx.compose.ui.geometry.Rect(0f, 0f, width, height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )
            )
        }

        drawRoundRect(
            color = outlineColor,
            size = size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx(), 10.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx())
        )

        clipPath(path) {
            drawRect(
                color = bgColor,
                size = size
            )

            val levelHeight = height * level
            val topY = height - levelHeight

            if (level > 0f) {
                drawRect(
                    color = color,
                    topLeft = Offset(0f, topY),
                    size = Size(width, levelHeight)
                )
            }
        }
    }
}

// --- Dynamic Vitality Column ---
@Composable
fun VitalityMetricColumn(
    title: String,
    value: String,
    status: String,
    meter: @Composable () -> Unit,
    buttonText: String,
    task: CareTask?,
    onComplete: (CareTask) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            meter()

            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(status, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)

            Button(
                onClick = { if (task != null) onComplete(task) },
                enabled = task != null,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )
            ) {
                Text(
                    text = if (task == null) "Watered" else buttonText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// --- Sunlight & Water Exposure Graphical Badges ---
@Composable
fun SunExposureIcons(sunlight: String) {
    val icons = when {
        sunlight.contains("full", ignoreCase = true) -> "☀️☀️☀️"
        sunlight.contains("partial", ignoreCase = true) || sunlight.contains("moderate", ignoreCase = true) -> "🌤️🌤️"
        else -> "🌑"
    }
    Text(icons, fontSize = 13.sp)
}

@Composable
fun WaterNeedsIcons(needs: String) {
    val icons = when {
        needs.contains("high", ignoreCase = true) || needs.contains("daily", ignoreCase = true) -> "💧💧💧"
        needs.contains("moderate", ignoreCase = true) || needs.contains("medium", ignoreCase = true) -> "💧💧"
        else -> "💧"
    }
    Text(icons, fontSize = 13.sp)
}
