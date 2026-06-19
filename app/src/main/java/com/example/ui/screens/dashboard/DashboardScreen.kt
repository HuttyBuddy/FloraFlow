package com.example.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.GardenLayout
import com.example.data.model.MoodLog
import com.example.ui.viewmodel.GardenViewModel
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import com.example.ui.viewmodel.WalkthroughStep
import com.example.ui.viewmodel.ScreenRect

@Composable
fun DashboardScreen(
    viewModel: GardenViewModel,
    onCommunityClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val layouts by viewModel.allLayouts.collectAsStateWithLifecycle()
    val activeLayout by viewModel.activeLayout.collectAsStateWithLifecycle()
    val activePlants by viewModel.activePlants.collectAsStateWithLifecycle()
    val moodLogs by viewModel.allMoodLogs.collectAsStateWithLifecycle()
    val isAssessmentSkipped by viewModel.isAssessmentSkipped.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showLayoutSelector by remember { mutableStateOf(false) }
    var showLogMoodDialog by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600

    val skippedAssessmentBanner = @Composable {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("skipped_assessment_banner"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "You haven't taken your Neural Load assessment yet. Take a 2-minute assessment to personalize your space.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = { viewModel.resetAssessment() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Begin Your Garden Journey",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            }
        }
    }

    val headerContent = @Composable {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Spa,
                        contentDescription = "Flower",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FloraFlow Garden Space",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (activeLayout != null) {
                            "Project: ${activeLayout?.name} | Style: ${activeLayout?.style}"
                        } else {
                            "Your garden beds are ready to be planted"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }

    val quickActionsContent = @Composable {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    val rect = coordinates.boundsInRoot()
                    viewModel.updateWalkthroughTarget(
                        WalkthroughStep.DASHBOARD_GARDEN,
                        ScreenRect(rect.left, rect.top, rect.right, rect.bottom)
                    )
                },
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { showCreateDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("action_create_layout"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create layout", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Plant a New Seed", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            
            Button(
                onClick = { showLayoutSelector = true },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("action_choose_layout"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.Eco, contentDescription = "Choose layout", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Visit a Garden", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    val statisticsContent = @Composable {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val totalMinutes = moodLogs.sumOf { it.activityMinutes }
            val avgMood = if (moodLogs.isNotEmpty()) moodLogs.map { it.moodScore }.average() else 0.0

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Timer, contentDescription = "Timer", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$totalMinutes mins",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Time in the Garden",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.FavoriteBorder, contentDescription = "Wellness", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    val locale = LocalConfiguration.current.locales[0]
                    Text(
                        text = String.format(locale, "%.1f/5", avgMood),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Wellness Bloom",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Spa, contentDescription = "Plants count", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${activePlants.size} items",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Plants Tended",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }

    val communityPromoContent = @Composable {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCommunityClick() }
                .testTag("dashboard_community_promo_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Forum,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "The Garden Gate",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Step through the gate and grow together",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    val canvasChartContent = @Composable {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    val rect = coordinates.boundsInRoot()
                    viewModel.updateWalkthroughTarget(
                        WalkthroughStep.DASHBOARD_STATS,
                        ScreenRect(rect.left, rect.top, rect.right, rect.bottom)
                    )
                },
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
                            text = "Therapeutic Mood Rating vs. Garden Growth Index",
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

    val logsHeaderContent = @Composable {
        Text(
            text = "Gardening & Mental Well-being Logs",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )
    }

    val logsListContent = @Composable {
        if (moodLogs.isEmpty()) {
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
                        text = "No mood entries yet — every garden begins with a single breath.",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Record how you feel after spending time outdoors styling plants. Your botanic health directly syncs with mental release!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                moodLogs.forEach { log ->
                    MoodLogItemCard(log = log, onDelete = { viewModel.deleteMoodLog(log.id) })
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
            if (isAssessmentSkipped) {
                item { skippedAssessmentBanner() }
            }
            item { headerContent() }
            item { quickActionsContent() }
            item { communityPromoContent() }
            item { statisticsContent() }
            item { canvasChartContent() }
            item { logsHeaderContent() }
            item { logsListContent() }
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
                    .weight(0.5f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isAssessmentSkipped) {
                    skippedAssessmentBanner()
                }
                headerContent()
                quickActionsContent()
                communityPromoContent()
                statisticsContent()
                Spacer(modifier = Modifier.height(16.dp))
            }
            Column(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                canvasChartContent()
                logsHeaderContent()
                logsListContent()
                Spacer(modifier = Modifier.height(16.dp))
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
        ChooseLayoutDialog(
            layouts = layouts,
            activeLayout = activeLayout,
            onDismiss = { showLayoutSelector = false },
            onSelect = { lay ->
                viewModel.selectLayout(lay)
                showLayoutSelector = false
            },
            onDelete = { lay ->
                viewModel.deleteLayout(lay)
            }
        )
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

// Helper to resolve string based Material Symbols
fun imageIcons(name: String): androidx.compose.ui.graphics.vector.ImageVector = when (name) {
    "eco" -> Icons.Default.Eco
    "spa" -> Icons.Default.Spa
    else -> Icons.Default.Terrain
}

