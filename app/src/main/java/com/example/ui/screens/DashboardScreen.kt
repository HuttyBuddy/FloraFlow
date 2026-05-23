package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ClimatePlants
import com.example.data.model.GardenLayout
import com.example.data.model.MoodLog
import com.example.ui.viewmodel.GardenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: GardenViewModel,
    modifier: Modifier = Modifier
) {
    val layouts by viewModel.allLayouts.collectAsStateWithLifecycle()
    val activeLayout by viewModel.activeLayout.collectAsStateWithLifecycle()
    val activePlants by viewModel.activePlants.collectAsStateWithLifecycle()
    val moodLogs by viewModel.allMoodLogs.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showLogMoodDialog by remember { mutableStateOf(false) }
    var showLayoutSelector by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        // --- Header Section ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("welcome_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "My Dream Garden",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Therapeutic Botanical Design",
                                style = MaterialTheme.typography.displaySmall.copy(fontSize = 24.sp),
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        
                        val isPro by viewModel.isPremium.collectAsStateWithLifecycle()
                        if (isPro) {
                            Icon(
                                Icons.Default.WorkspacePremium,
                                contentDescription = "Pro Member",
                                tint = Color(0xFFFFB74D),
                                modifier = Modifier.size(40.dp)
                            )
                        } else {
                            SuggestionChip(
                                onClick = { viewModel.upgradeToPremium() },
                                label = { Text("GO PRO ✨", fontWeight = FontWeight.Bold, color = Color(0xFF6A994E)) },
                                border = SuggestionChipDefaults.suggestionChipBorder(
                                    enabled = true,
                                    borderColor = Color(0xFF6A994E)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (activeLayout != null) {
                        Text(
                            text = "Currently Cultivating: ${activeLayout!!.name}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text(activeLayout!!.style) },
                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.background)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            SuggestionChip(
                                onClick = {},
                                label = { Text(activeLayout!!.climate) },
                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.background)
                            )
                        }
                    } else {
                        Text(
                            text = "Choose or create a themed garden layout template below to get started with your layout generated design!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // --- Quick Actions ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("create_layout_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Garden", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { showLayoutSelector = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("switch_layout_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Switch")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Switch Style", fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- Quick Statistics Row ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card for total minutes gardening
                val totalMinutes = moodLogs.sumOf { it.activityMinutes }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = "Time", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (totalMinutes >= 60) "${totalMinutes / 60}h ${totalMinutes % 60}m" else "${totalMinutes}m",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Outdoor Sesh",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                // Card for average well-being mood score
                val avgMood = if (moodLogs.isNotEmpty()) moodLogs.map { it.moodScore }.average() else 0.0
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = "Wellness", tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (avgMood > 0) String.format("%.1f", avgMood) + " / 5" else "N/A",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Wellness Rating",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                // Card for cultivated plants count
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Spa, contentDescription = "Plants", tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${activePlants.size} active",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Cared Items",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        // --- Custom Canvas Visual Graph (The therapeutic wellness correlation) ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Therapeutic Growth Sync",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Outdoor Gardening Time vs. Personal Well-being",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        IconButton(onClick = { showLogMoodDialog = true }) {
                            Icon(
                                Icons.Default.EditCalendar,
                                contentDescription = "Log Session",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (moodLogs.size >= 2) {
                        InteractiveTherapyChart(logs = moodLogs.take(8).reversed())
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(
                                    MaterialTheme.colorScheme.background,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    "✨ First insights require at least two logs",
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    "Log therapeutic session minutes and mood ratings while gardening daily to correlate variables.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { showLogMoodDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Log Session Now")
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Recent Mood Logs List ---
        item {
            Text(
                text = "Gardening & Mental Well-being Logs",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (moodLogs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Spa,
                            contentDescription = "Empty Log",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Mood & Garden Logs Yet",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Record how you feel after spending time outdoors styling plants. Your botanic health directly syncs with mental release!",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        } else {
            items(moodLogs) { log ->
                MoodLogItemCard(log = log, onDelete = { viewModel.deleteMoodLog(log.id) })
            }
        }
    }

    // --- DIALOG: Create Layout ---
    if (showCreateDialog) {
        CreateLayoutDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, style, climate ->
                viewModel.createLayout(name, style, climate)
                showCreateDialog = false
            }
        )
    }

    // --- DIALOG: Choose Active Layout ---
    if (showLayoutSelector) {
        Dialog(onDismissRequest = { showLayoutSelector = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Coactive Garden",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { showLayoutSelector = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (layouts.isEmpty()) {
                        Text(
                            text = "No custom designs created. Create a new garden above!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(vertical = 24.dp),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            items(layouts) { lay ->
                                val isSelected = activeLayout?.id == lay.id
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.selectLayout(lay)
                                            showLayoutSelector = false
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Eco,
                                            contentDescription = "eco",
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = lay.name,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${lay.style} | ${lay.climate}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Active",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        } else {
                                            IconButton(onClick = { viewModel.deleteLayout(lay) }) {
                                                Icon(
                                                    Icons.Default.DeleteOutline,
                                                    contentDescription = "Delete",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(20.dp)
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
        }
    }

    // --- DIALOG: Log Mood ---
    if (showLogMoodDialog) {
        LogMoodDialog(
            onDismiss = { showLogMoodDialog = false },
            onLog = { mood, score, duration, notes ->
                viewModel.logMood(mood, score, duration, notes)
                showLogMoodDialog = false
            }
        )
    }
}

// --- Custom Interactive Canvas Chart ---
@Composable
fun InteractiveTherapyChart(
    logs: List<MoodLog>
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.tertiary
    val grayColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    val textStyleColor = MaterialTheme.colorScheme.onSurface

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(vertical = 8.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                val paddingX = 40f
                val paddingY = 30f

                val chartWidth = width - (paddingX * 2)
                val chartHeight = height - (paddingY * 2)

                // Grid background guidelines
                for (i in 0..4) {
                    val y = paddingY + (chartHeight * i / 4)
                    drawLine(
                        color = grayColor,
                        start = Offset(paddingX, y),
                        end = Offset(width - paddingX, y),
                        strokeWidth = 2f
                    )
                }

                // Parse max bounds
                val maxMinutes = logs.maxOf { it.activityMinutes }.coerceAtLeast(1).toFloat()
                val minMinutes = 0f
                val count = logs.size

                val minScore = 1f
                val maxScore = 5f

                // Draw Activity Duration Line (Primary - Forest Green)
                val pointsDuration = mutableListOf<Offset>()
                // Draw Mood Score Line (Secondary - Gold/Terracotta)
                val pointsMood = mutableListOf<Offset>()

                for (idx in logs.indices) {
                    val x = paddingX + (chartWidth * idx / (count - 1).coerceAtLeast(1))
                    
                    // Duration point mapping
                    val durVal = logs[idx].activityMinutes.toFloat()
                    val dY = paddingY + chartHeight - (chartHeight * (durVal - minMinutes) / maxMinutes)
                    pointsDuration.add(Offset(x, dY))

                    // Mood point mapping
                    val moodVal = logs[idx].moodScore.toFloat()
                    val mY = paddingY + chartHeight - (chartHeight * (moodVal - minScore) / (maxScore - minScore))
                    pointsMood.add(Offset(x, mY))
                }

                // Draw lines
                for (idx in 0 until pointsDuration.size - 1) {
                    drawLine(
                        color = primaryColor,
                        start = pointsDuration[idx],
                        end = pointsDuration[idx + 1],
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = secondaryColor,
                        start = pointsMood[idx],
                        end = pointsMood[idx + 1],
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                }

                // Draw Dots
                for (idx in pointsDuration.indices) {
                    drawCircle(color = primaryColor, radius = 10f, center = pointsDuration[idx])
                    drawCircle(color = Color.White, radius = 5f, center = pointsDuration[idx])

                    drawCircle(color = secondaryColor, radius = 10f, center = pointsMood[idx])
                    drawCircle(color = Color.White, radius = 5f, center = pointsMood[idx])
                }
            }
        }

        // Legend indicators
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(primaryColor))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Gardening Minutes", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = 16.dp))

            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(secondaryColor))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Therapy Rating", style = MaterialTheme.typography.bodySmall)
        }
    }
}

// --- Specific Mood Log Item Card ---
@Composable
fun MoodLogItemCard(
    log: MoodLog,
    onDelete: () -> Unit
) {
    val emoji = when (log.mood) {
        "Peaceful" -> "🧘"
        "Energized" -> "⚡"
        "Refreshed" -> "🍃"
        "Happy" -> "☀️"
        "Anxious" -> "💨"
        "Tired" -> "☁️"
        else -> "🌱"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${log.mood} Garden Sesh",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(log.moodScore) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "*",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "⏱️ ${log.activityMinutes} minutes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "🪴 Garden Index: ${log.growthIndex}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                if (log.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "\"${log.notes}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Delete Log",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// --- DIALOG: Add/Create Layout ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateLayoutDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, style: String, climate: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf(ClimatePlants.STYLES[0]) }
    var selectedClimate by remember { mutableStateOf(ClimatePlants.CLIMATES[0]) }

    var expandedStyle by remember { mutableStateOf(false) }
    var expandedClimate by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Design Botanic Haven",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Garden Project Name") },
                    modifier = Modifier.fillMaxWidth().testTag("add_layout_name"),
                    placeholder = { Text("My Courtyard Oasis") },
                    shape = RoundedCornerShape(12.dp)
                )

                // Style Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedStyle,
                    onExpandedChange = { expandedStyle = it }
                ) {
                    OutlinedTextField(
                        value = selectedStyle,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Design Theme Style") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStyle) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedStyle,
                        onDismissRequest = { expandedStyle = false }
                    ) {
                        ClimatePlants.STYLES.forEach { style ->
                            DropdownMenuItem(
                                text = { Text(style) },
                                onClick = {
                                    selectedStyle = style
                                    expandedStyle = false
                                }
                            )
                        }
                    }
                }

                // Climate Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedClimate,
                    onExpandedChange = { expandedClimate = it }
                ) {
                    OutlinedTextField(
                        value = selectedClimate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Physical Soil Climate") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedClimate) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedClimate,
                        onDismissRequest = { expandedClimate = false }
                    ) {
                        ClimatePlants.CLIMATES.forEach { clim ->
                            DropdownMenuItem(
                                text = { Text(clim) },
                                onClick = {
                                    selectedClimate = clim
                                    expandedClimate = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

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
                                onCreate(name, selectedStyle, selectedClimate)
                            }
                        },
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("submit_create_layout")
                    ) {
                        Text("Seed Blueprint")
                    }
                }
            }
        }
    }
}

// --- DIALOG: Log Session ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogMoodDialog(
    onDismiss: () -> Unit,
    onLog: (mood: String, score: Int, activityMinutes: Int, notes: String) -> Unit
) {
    var mood by remember { mutableStateOf("Peaceful") }
    var moodScore by remember { mutableStateOf(5) }
    var activityMinutes by remember { mutableStateOf("30") }
    var notes by remember { mutableStateOf("") }

    val moodsList = listOf("Peaceful", "Refreshed", "Energized", "Happy", "Anxious", "Tired")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Record Outdoor Session",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Select Mood
                Text("How do you feel after gardening?", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val moodEmoList = listOf("Peaceful" to "🧘", "Refreshed" to "🍃", "Energized" to "⚡", "Happy" to "☀️", "Anxious" to "💨")
                    moodEmoList.forEach { pair ->
                        val isSelected = mood == pair.first
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                )
                                .clickable { mood = pair.first },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(pair.second, fontSize = 18.sp)
                                Text(
                                    pair.first,
                                    fontSize = 8.sp,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Mood Rating (1 to 5 Stars scale)
                Text("Self Wellness Level: $moodScore / 5", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (score in 1..5) {
                        val isPicked = score <= moodScore
                        IconButton(onClick = { moodScore = score }) {
                            Icon(
                                if (isPicked) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = score.toString(),
                                tint = if (isPicked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                // Duration Textfield
                OutlinedTextField(
                    value = activityMinutes,
                    onValueChange = { activityMinutes = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Activity Time (Minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("add_mood_duration"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Notes Description
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Botanical Reflections") },
                    placeholder = { Text("I felt incredibly refreshed while pruning the maples!") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                            val duration = activityMinutes.toIntOrNull() ?: 15
                            onLog(mood, moodScore, duration, notes)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("submit_log_mood")
                    ) {
                        Text("Log Well-being")
                    }
                }
            }
        }
    }
}

// Helper to resolve string based Material Symbols
fun imageIcons(name: String) = when (name) {
    "eco" -> Icons.Default.Eco
    "spa" -> Icons.Default.Spa
    else -> Icons.Default.Terrain
}
