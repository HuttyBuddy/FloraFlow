package com.example.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.ui.viewmodel.GardenViewModel

@Composable
fun PlannerScreen(
    viewModel: GardenViewModel,
    switchToChatTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeLayout by viewModel.activeLayout.collectAsStateWithLifecycle()
    val activePlants by viewModel.activePlants.collectAsStateWithLifecycle()
    val layouts by viewModel.allLayouts.collectAsStateWithLifecycle()

    var showCellConfigDialog by remember { mutableStateOf<Pair<Int, Int>?>(null) } // Row, Col of clicked cell
    var selectedPlantNameForCell by remember { mutableStateOf("") }
    var selectedPlantTheme by remember { mutableStateOf("Rose") }

    val currentLayout = activeLayout
    val activeGridItems = remember(currentLayout?.gridString) {
        parseGridString(currentLayout?.gridString ?: "")
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
                text = "No Garden Outline Open",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
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
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Layout Info Card
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
                        Column {
                            Text(
                                text = "Design Blueprint",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = currentLayout.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${currentLayout.style} | 5x5",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Climate: ${currentLayout.climate}. Tap any plot below to choose and assign vegetation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Quick AI Architect Controls
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "AI Layout Helper:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    AssistChip(
                        onClick = {
                            viewModel.askAiForLayoutAdvice()
                            switchToChatTab()
                        },
                        label = { Text("Design Review") },
                        leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI", modifier = Modifier.size(16.dp)) }
                    )

                    AssistChip(
                        onClick = {
                            viewModel.generateAILayoutSuggestion()
                            switchToChatTab()
                        },
                        label = { Text("Sow AI Plants") },
                        leadingIcon = { Icon(Icons.Default.Spa, contentDescription = "Sow", modifier = Modifier.size(16.dp)) }
                    )
                }
            }

            // Interactive 5x5 Grid Planner
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Yard Space Grid Blueprint",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // 5x5 Matrix implementation using standard nested Columns and Rows inside a scrollable screen
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

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                if (item != null) {
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                } else {
                                                    // Clay tilled tilling soil shade
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                                }
                                            )
                                            .border(
                                                width = if (item != null) 2.dp else 1.dp,
                                                color = if (item != null) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
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
                                                // Outer concentric ring representing root nourishment limit
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .border(
                                                            width = 1.dp,
                                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                                            shape = CircleShape
                                                        )
                                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(emoji, fontSize = 26.sp)
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = item.plantName.take(7),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        } else {
                                            // Tilled ground layout detailing concentric circular dot guide
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .border(
                                                        width = 1.dp,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Add,
                                                    contentDescription = "Empty",
                                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Legend description
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(activeGridItems.map { it.plantName }.distinct()) { name ->
                            val emoji = getEmojiForPlantName(name)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(emoji, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(name, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // --- Cell Configuration Dialog ---
    val cellDialogCoords = showCellConfigDialog
    if (cellDialogCoords != null) {
        val row = cellDialogCoords.first
        val col = cellDialogCoords.second
        val currentOccupant = activeGridItems.firstOrNull { it.x == row && it.y == col }

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
                        text = "Plot Configuration (${row + 1}, ${col + 1})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (currentOccupant != null) {
                        Text(
                            text = "Current: ${currentOccupant.plantName} ${getEmojiForPlantName(currentOccupant.plantName)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = "Change plant or insert a custom vegetation seed below:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    // Selection lists
                    Text("My Layout Cultivated Items:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 140.dp)
                    ) {
                        // Quick add recommendation list
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
                                Text("Uproot Plant")
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
}

// Utility to assign emojis based on plant name keywords
fun getEmojiForPlantName(name: String): String {
    val low = name.lowercase()
    return when {
        low.contains("rose") -> "🌹"
        low.contains("cact") || low.contains("aloe") -> "🌵"
        low.contains("marigold") || low.contains("aster") -> "🌼"
        low.contains("lavender") -> "🪻"
        low.contains("orchid") -> "🌸"
        low.contains("hibiscus") || low.contains("bougainvillea") -> "🌺"
        low.contains("tomato") -> "🍅"
        low.contains("sweet potato") || low.contains("yam") -> "🍠"
        low.contains("fig") -> "🍋"
        low.contains("olive") || low.contains("maple") || low.contains("tree") -> "🌳"
        low.contains("fern") || low.contains("monstera") || low.contains("shoot") -> "🌿"
        low.contains("basil") || low.contains("rosemary") || low.contains("herb") || low.contains("thyme") -> "🌱"
        else -> "🌱"
    }
}

// Placeholder icon mapping to avoid extended icons compilation errors if any
fun imageVectorIcons(name: String) = when (name) {
    "leaf" -> Icons.Default.Eco
    else -> Icons.Default.Terrain
}

// End of Planner Screen
