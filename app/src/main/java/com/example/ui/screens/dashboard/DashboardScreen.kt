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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.example.data.model.CareTask
import com.example.data.model.Plant
import androidx.compose.ui.window.Dialog
import com.example.ui.viewmodel.GardenViewModel
import com.example.ui.theme.SoilSageDark
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import com.example.ui.viewmodel.WalkthroughStep
import com.example.ui.viewmodel.ScreenRect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class BreathingState {
    IDLE, INHALE, HOLD_IN, EXHALE, HOLD_OUT
}

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
        val headerGradient = Brush.horizontalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.secondary
            )
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerGradient)
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = com.example.R.drawable.ic_logo_heart),
                    contentDescription = "FloraFlow Logo",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(4.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FloraFlow Garden Space",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = if (activeLayout != null) {
                            "Project: ${activeLayout?.name}"
                        } else {
                            "Your garden beds are ready to be planted"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
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
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create layout", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Plant a New Seed", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            
            Button(
                onClick = { showLayoutSelector = true },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("action_choose_layout"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.Eco, contentDescription = "Choose layout", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Visit a Garden", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
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

            var isPressedTime by remember { mutableStateOf(false) }
            val scaleTime by animateFloatAsState(if (isPressedTime) 0.95f else 1f, label = "ScaleTime")

            Card(
                modifier = Modifier
                    .weight(1f)
                    .scale(scaleTime)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressedTime = true
                                tryAwaitRelease()
                                isPressedTime = false
                            },
                            onTap = {
                                showLogMoodDialog = true
                            }
                        )
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.2.dp, Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), Color.Transparent)))
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
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            var isPressedWellness by remember { mutableStateOf(false) }
            val scaleWellness by animateFloatAsState(if (isPressedWellness) 0.95f else 1f, label = "ScaleWellness")

            Card(
                modifier = Modifier
                    .weight(1f)
                    .scale(scaleWellness)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressedWellness = true
                                tryAwaitRelease()
                                isPressedWellness = false
                            },
                            onTap = {
                                showLogMoodDialog = true
                            }
                        )
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.2.dp, Brush.verticalGradient(listOf(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f), Color.Transparent)))
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
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            var isPressedPlants by remember { mutableStateOf(false) }
            val scalePlants by animateFloatAsState(if (isPressedPlants) 0.95f else 1f, label = "ScalePlants")

            Card(
                modifier = Modifier
                    .weight(1f)
                    .scale(scalePlants)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressedPlants = true
                                tryAwaitRelease()
                                isPressedPlants = false
                            },
                            onTap = {
                                viewModel.setCurrentTab(1)
                            }
                        )
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.2.dp, Brush.verticalGradient(listOf(MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), Color.Transparent)))
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
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    val communityPromoContent = @Composable {
        var isPressedComm by remember { mutableStateOf(false) }
        val scaleComm by animateFloatAsState(if (isPressedComm) 0.96f else 1f, label = "ScaleComm")
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scaleComm)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressedComm = true
                            tryAwaitRelease()
                            isPressedComm = false
                        },
                        onTap = { onCommunityClick() }
                    )
                }
                .testTag("dashboard_community_promo_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            ),
            border = BorderStroke(
                1.5.dp, 
                Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                    )
                )
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Forum,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                    )
                }
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(
                1.5.dp,
                Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                )
            )
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
            item { DailyHabitCard(viewModel = viewModel) }
            item { MindfulBreathingCard(viewModel = viewModel) }
            item { MonthlyWellnessDigestCard(moodLogs = moodLogs) }
            item { SeasonalCareCoachCard(activePlants = activePlants) }
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
                DailyHabitCard(viewModel = viewModel)
                MindfulBreathingCard(viewModel = viewModel)
                MonthlyWellnessDigestCard(moodLogs = moodLogs)
                SeasonalCareCoachCard(activePlants = activePlants)
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

    // --- Celebration Dialogs ---
    val firstBloomTrigger by viewModel.firstBloomTrigger.collectAsStateWithLifecycle()
    val streakCount = remember(moodLogs) { calculateStreak(moodLogs) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("floraflow_prefs", android.content.Context.MODE_PRIVATE) }
    var lastCelebratedStreak by remember { mutableStateOf(sharedPrefs.getInt("last_celebrated_streak", 0)) }
    
    var showStreakCelebration by remember { mutableStateOf(false) }
    var celebratedStreakValue by remember { mutableStateOf(0) }
    
    LaunchedEffect(streakCount) {
        if ((streakCount == 7 || streakCount == 30 || streakCount == 100) && streakCount > lastCelebratedStreak) {
            celebratedStreakValue = streakCount
            showStreakCelebration = true
        }
    }
    
    if (showStreakCelebration) {
        CelebrationDialog(
            title = "${celebratedStreakValue} Day Streak! 🔥",
            subtitle = "I've tended my garden for $celebratedStreakValue days straight on FloraFlow.",
            extraText = "I've tended my garden for $celebratedStreakValue days straight on FloraFlow! 🌸🌿 #FloraFlow",
            onDismiss = {
                sharedPrefs.edit().putInt("last_celebrated_streak", celebratedStreakValue).apply()
                lastCelebratedStreak = celebratedStreakValue
                showStreakCelebration = false
            }
        )
    }
    
    if (firstBloomTrigger != null) {
        val plantName = firstBloomTrigger ?: ""
        CelebrationDialog(
            title = "First Bloom! 🌸",
            subtitle = "My $plantName has bloomed on FloraFlow! 🎉",
            extraText = "My $plantName has bloomed on FloraFlow! 🌸🌿 #FloraFlow",
            onDismiss = {
                viewModel.clearFirstBloomTrigger()
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

    val isDark = MaterialTheme.colorScheme.primary == SoilSageDark
    val itemBgColor = if (isDark) {
        when (log.mood) {
            "Peaceful" -> Color(0xFF2C1E30)
            "Energized" -> Color(0xFF332B12)
            "Refreshed" -> Color(0xFF1B2E1E)
            "Happy" -> Color(0xFF302213)
            "Anxious" -> Color(0xFF212529)
            "Tired" -> Color(0xFF1C1D1F)
            else -> MaterialTheme.colorScheme.primaryContainer
        }
    } else {
        when (log.mood) {
            "Peaceful" -> Color(0xFFF3E5F5)
            "Energized" -> Color(0xFFFFFDE7)
            "Refreshed" -> Color(0xFFE8F5E9)
            "Happy" -> Color(0xFFFFF3E0)
            "Anxious" -> Color(0xFFECEFF1)
            "Tired" -> Color(0xFFF5F5F5)
            else -> MaterialTheme.colorScheme.surface
        }
    }

    val emojiBgColor = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = itemBgColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(emojiBgColor),
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
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
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
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "🪴 Garden Index: ${log.growthIndex}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                if (log.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "\"${log.notes}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Delete Log",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}


@Composable
fun MindfulBreathingCard(
    viewModel: GardenViewModel,
    modifier: Modifier = Modifier
) {
    var breathingState by remember { mutableStateOf(BreathingState.IDLE) }
    var secondsRemaining by remember { mutableIntStateOf(60) }
    var showSuccessState by remember { mutableStateOf(false) }
    var customNotes by remember { mutableStateOf("") }
    
    val targetProgress = when (breathingState) {
        BreathingState.INHALE -> 1.0f
        BreathingState.HOLD_IN -> 1.0f
        BreathingState.EXHALE -> 0.0f
        BreathingState.HOLD_OUT -> 0.0f
        BreathingState.IDLE -> 0.0f
    }
    
    val breathProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
        label = "BreathProgress"
    )
    
    val auraColor by animateColorAsState(
        targetValue = when (breathingState) {
            BreathingState.INHALE -> Color(0xFF80CBC4)
            BreathingState.HOLD_IN -> Color(0xFF81C784)
            BreathingState.EXHALE -> Color(0xFFB39DDB)
            BreathingState.HOLD_OUT -> Color(0xFF90A4AE)
            BreathingState.IDLE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        },
        animationSpec = tween(durationMillis = 1500),
        label = "AuraColor"
    )
    
    val stateText = when (breathingState) {
        BreathingState.INHALE -> "Breathe In..."
        BreathingState.HOLD_IN -> "Hold..."
        BreathingState.EXHALE -> "Breathe Out..."
        BreathingState.HOLD_OUT -> "Hold..."
        BreathingState.IDLE -> "Press Begin to start 1-minute box breathing"
    }

    LaunchedEffect(breathingState) {
        if (breathingState != BreathingState.IDLE && secondsRemaining > 0) {
            while (secondsRemaining > 0 && breathingState != BreathingState.IDLE) {
                delay(1000)
                secondsRemaining--
                if (secondsRemaining == 0) {
                    breathingState = BreathingState.IDLE
                    showSuccessState = true
                }
            }
        }
    }
    
    LaunchedEffect(breathingState) {
        if (breathingState != BreathingState.IDLE) {
            while (secondsRemaining > 0) {
                delay(4000)
                breathingState = when (breathingState) {
                    BreathingState.INHALE -> BreathingState.HOLD_IN
                    BreathingState.HOLD_IN -> BreathingState.EXHALE
                    BreathingState.EXHALE -> BreathingState.HOLD_OUT
                    BreathingState.HOLD_OUT -> BreathingState.INHALE
                    else -> BreathingState.IDLE
                }
            }
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.5.dp, 
            Brush.horizontalGradient(
                listOf(
                    MaterialTheme.colorScheme.tertiary, 
                    MaterialTheme.colorScheme.primary
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.SelfImprovement,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Mindful Garden Breath",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                if (breathingState != BreathingState.IDLE) {
                    Text(
                        text = "${secondsRemaining}s remaining",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            
            if (showSuccessState) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🌸 Excellent! You completed a breathing cycle.", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("This outdoor breathing minute correlates with 5% higher mental well-being and calmness indices.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    
                    OutlinedTextField(
                        value = customNotes,
                        onValueChange = { customNotes = it },
                        placeholder = { Text("How do you feel? (e.g. Cleared my mind)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().height(56.dp).testTag("breathing_gratitude_input"),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        TextButton(onClick = { 
                            showSuccessState = false
                            customNotes = ""
                        }) {
                            Text("Dismiss")
                        }
                        Button(
                            onClick = {
                                viewModel.logMood(
                                    mood = "Peaceful",
                                    score = 5,
                                    duration = 1,
                                    notes = customNotes.ifBlank { "Completed 1 minute of mindful breathing." }
                                )
                                showSuccessState = false
                                customNotes = ""
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Save Log", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val petalCount = 8
                        val scale = 0.4f + breathProgress * 0.6f
                        val center = Offset(size.width / 2, size.height / 2)
                        
                        // Glow aura background
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(auraColor.copy(alpha = 0.35f), Color.Transparent),
                                radius = size.minDimension / 1.6f
                            ),
                            center = center
                        )
                        
                        // Draw petals
                        for (i in 0 until petalCount) {
                            val angle = i * (360f / petalCount)
                            rotate(angle, pivot = center) {
                                val petalWidth = size.width * 0.16f * scale
                                val petalHeight = size.height * 0.36f * scale
                                drawOval(
                                    color = auraColor,
                                    topLeft = Offset(center.x - petalWidth / 2, center.y - petalHeight),
                                    size = Size(petalWidth, petalHeight),
                                    alpha = 0.8f
                                )
                            }
                        }
                        
                        // Gold center
                        drawCircle(
                            color = Color(0xFFFFD54F),
                            radius = size.minDimension * 0.08f * (0.8f + breathProgress * 0.2f),
                            center = center
                        )
                    }
                    
                    if (breathingState != BreathingState.IDLE) {
                        Text(
                            text = when (breathingState) {
                                BreathingState.INHALE -> "In"
                                BreathingState.HOLD_IN -> "Hold"
                                BreathingState.EXHALE -> "Out"
                                BreathingState.HOLD_OUT -> "Hold"
                                else -> ""
                            },
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp
                        )
                    }
                }
                
                Text(
                    text = stateText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                
                if (breathingState == BreathingState.IDLE) {
                    Button(
                        onClick = {
                            secondsRemaining = 60
                            breathingState = BreathingState.INHALE
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Begin Breathing")
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            breathingState = BreathingState.IDLE
                            secondsRemaining = 60
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Reset")
                    }
                }
            }
        }
    }
}

@Composable
fun DailyGrowthRing(
    completedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    val completionRatio = if (totalCount > 0) completedCount.toFloat() / totalCount else 0.0f
    
    // Animated sweep angle
    val animatedProgress by animateFloatAsState(
        targetValue = completionRatio,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "GrowthRingProgress"
    )

    val isFull = completionRatio >= 1.0f
    val emojiScale by animateFloatAsState(
        targetValue = if (isFull) 1.25f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "EmojiScale"
    )

    Box(
        modifier = modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)

            // Draw base track (earthy green background)
            drawCircle(
                color = Color(0xFFE2ECE9),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // Draw active progress (vibrant botanical green/gold sweep gradient)
            drawArc(
                brush = Brush.sweepGradient(
                    colors = if (isFull) {
                        listOf(Color(0xFF81C784), Color(0xFF4CAF50), Color(0xFFFFD54F), Color(0xFF81C784))
                    } else {
                        listOf(Color(0xFF81C784), Color(0xFF4CAF50), Color(0xFF2E7D32), Color(0xFF81C784))
                    }
                ),
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Draw small flower emoji or percentage inside
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(emojiScale)
        ) {
            Text(
                text = if (isFull) "🌸" else "🌱",
                fontSize = 20.sp
            )
            Text(
                text = "${(completionRatio * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isFull) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun DailyHabitCard(
    viewModel: GardenViewModel,
    modifier: Modifier = Modifier
) {
    val pendingTasks by viewModel.pendingCareTasks.collectAsStateWithLifecycle()
    val allTasks by viewModel.allCareTasks.collectAsStateWithLifecycle()
    val weather by viewModel.currentWeather.collectAsStateWithLifecycle()
    val moodLogs by viewModel.allMoodLogs.collectAsStateWithLifecycle()

    val completedTasksToday = remember(allTasks) {
        allTasks.count { it.completedDate != null && isToday(it.completedDate) }
    }
    val pendingTasksToday = remember(pendingTasks) {
        pendingTasks.count { it.dueDate <= System.currentTimeMillis() }
    }
    val totalTasksToday = completedTasksToday + pendingTasksToday

    // Calculate streak
    val streakCount = remember(moodLogs) {
        calculateStreak(moodLogs)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.5.dp,
            Brush.horizontalGradient(
                listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                )
            )
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Your Daily Growth Ring",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Nurture your plants and mind",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Streak badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("🔥", fontSize = 14.sp)
                    Text(
                        text = "$streakCount Day Streak",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Growth ring
                DailyGrowthRing(
                    completedCount = completedTasksToday,
                    totalCount = totalTasksToday,
                    modifier = Modifier.padding(8.dp)
                )

                // Weather and Care checklist column
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Local Eco-Zone Weather status
                    Text(
                        text = "Weather: ${getWeatherEmoji(weather.condition)} ${weather.cityName} (${weather.temperatureCelsius.toInt()}°C • ${weather.condition})",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    if (pendingTasksToday == 0) {
                        Text(
                            text = "✨ All tasks complete! Your garden is thriving.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        val hasGratitudeToday = remember(moodLogs) {
                            moodLogs.any { isToday(it.timestamp) && it.mood == "Peaceful" && it.notes.isNotEmpty() }
                        }
                        if (totalTasksToday > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            if (hasGratitudeToday) {
                                Text(
                                    text = "🍃 Thank you for saving a moment of gratitude today.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            } else {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "Name one thing in your garden that brought you peace today:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    var gratitudeText by remember { mutableStateOf("") }
                                    OutlinedTextField(
                                        value = gratitudeText,
                                        onValueChange = { gratitudeText = it },
                                        placeholder = { Text("e.g. The scent of lavender blossoms...", fontSize = 11.sp) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                            .testTag("gratitude_input"),
                                        singleLine = true,
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    )
                                    Button(
                                        onClick = {
                                            if (gratitudeText.isNotBlank()) {
                                                viewModel.logMood(mood = "Peaceful", score = 5, duration = 5, notes = gratitudeText.trim())
                                            }
                                        },
                                        enabled = gratitudeText.isNotBlank(),
                                        modifier = Modifier
                                            .align(Alignment.End)
                                            .height(32.dp)
                                            .testTag("save_gratitude_button"),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Save Reflection", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Due Today:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        val completedTodayList = remember(allTasks) {
                            allTasks.filter { it.completedDate != null && isToday(it.completedDate) }
                        }
                        val pendingTodayList = remember(pendingTasks) {
                            pendingTasks.filter { it.dueDate <= System.currentTimeMillis() }
                        }
                        val combinedTasks = remember(pendingTodayList, completedTodayList) {
                            (pendingTodayList + completedTodayList).take(3)
                        }

                        combinedTasks.forEach { task ->
                            val isCompleted = task.completedDate != null
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isCompleted) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.CheckCircle,
                                        contentDescription = "Completed",
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "${task.taskType} ${task.plantName}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            textDecoration = TextDecoration.LineThrough
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        maxLines = 1
                                    )
                                } else {
                                    var checked by remember { mutableStateOf(false) }
                                    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                                    val coroutineScope = rememberCoroutineScope()
                                    
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = { isChecked ->
                                            if (isChecked) {
                                                checked = true
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.Confirm)
                                                coroutineScope.launch {
                                                    delay(300)
                                                    viewModel.completeCareTask(task)
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = "${task.taskType} ${task.plantName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
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

@Composable
fun MonthlyWellnessDigestCard(
    moodLogs: List<MoodLog>,
    modifier: Modifier = Modifier
) {
    val calendar = java.util.Calendar.getInstance()
    val currentMonthName = java.text.SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.time)
    
    val currentMonthLogs = remember(moodLogs) {
        moodLogs.filter { log ->
            val logCal = java.util.Calendar.getInstance().apply { timeInMillis = log.timestamp }
            logCal.get(java.util.Calendar.MONTH) == calendar.get(java.util.Calendar.MONTH) &&
                    logCal.get(java.util.Calendar.YEAR) == calendar.get(java.util.Calendar.YEAR)
        }
    }
    
    val totalGardened = currentMonthLogs.size
    val peacefulCount = currentMonthLogs.count { it.mood == "Peaceful" }
    
    // Sort logs chronologically to get start/end wellness bloom
    val sortedLogs = remember(currentMonthLogs) { currentMonthLogs.sortedBy { it.timestamp } }
    val half = sortedLogs.size / 2
    val startAvg = if (half > 0) sortedLogs.take(half).map { it.moodScore }.average() else 3.0
    val endAvg = if (half > 0) sortedLogs.drop(half).map { it.moodScore }.average() else (if (currentMonthLogs.isNotEmpty()) currentMonthLogs.map { it.moodScore }.average() else 3.0)
    
    val overallAvg = if (moodLogs.isNotEmpty()) moodLogs.map { it.moodScore }.average() else 0.0
    val calmDeltaPercent = if (overallAvg > 3.0) {
        (((overallAvg - 3.0) / 3.0) * 100).toInt()
    } else {
        0
    }
    
    Card(
        modifier = modifier.fillMaxWidth().testTag("monthly_wellness_digest_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.5.dp,
            Brush.horizontalGradient(
                listOf(
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
            )
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Book,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Your $currentMonthName Garden Journal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (totalGardened == 0) {
                Text(
                    text = "Log your garden sessions today to begin tracking your monthly wellness digest and correlation insights! 🌿",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            } else {
                Text(
                    text = "You gardened $totalGardened times, felt Peaceful $peacefulCount times, and your wellness bloom average went from ${String.format(Locale.getDefault(), "%.1f", startAvg)} to ${String.format(Locale.getDefault(), "%.1f", endAvg)}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Medium
                )
                
                if (calmDeltaPercent > 0) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("✨", fontSize = 16.sp)
                        Text(
                            text = "You felt $calmDeltaPercent% calmer on days you spent time in the garden.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SeasonalCareCoachCard(
    activePlants: List<Plant>,
    modifier: Modifier = Modifier
) {
    val cal = java.util.Calendar.getInstance()
    val month = cal.get(java.util.Calendar.MONTH)
    val currentSeason = when (month) {
        2, 3, 4 -> "Spring"
        5, 6, 7 -> "Summer"
        8, 9, 10 -> "Autumn"
        else -> "Winter"
    }
    
    val seasonEmoji = when (currentSeason) {
        "Spring" -> "🌸"
        "Summer" -> "☀️"
        "Autumn" -> "🍁"
        else -> "❄️"
    }
    
    val plantToCoach = activePlants.firstOrNull()
    
    Card(
        modifier = modifier.fillMaxWidth().testTag("seasonal_coach_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.5.dp,
            Brush.horizontalGradient(
                listOf(
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f),
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                )
            )
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(seasonEmoji, fontSize = 24.sp)
                Text(
                    text = "$currentSeason Care Coach",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (plantToCoach != null) {
                val careTip = when (currentSeason) {
                    "Spring" -> plantToCoach.careSpring
                    "Summer" -> plantToCoach.careSummer
                    "Autumn" -> plantToCoach.careAutumn
                    else -> plantToCoach.careWinter
                }
                Text(
                    text = "Tip for your ${plantToCoach.name}:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = careTip,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "No plants sowed yet. Start sowing seeds in your planner grid to receive personalized $currentSeason care coaching!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CelebrationDialog(
    title: String,
    subtitle: String,
    extraText: String,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val colors = listOf(Color(0xFF4CAF50), Color(0xFF81C784), Color(0xFFFFD700), Color(0xFFFFA500), Color(0xFFC8E6C9), Color(0xFFFFF59D))
    
    val particles = remember {
        mutableStateListOf<ConfettiParticle>().apply {
            repeat(80) {
                add(
                    ConfettiParticle(
                        x = (0..1000).random().toFloat(),
                        y = -100f - (0..800).random().toFloat(),
                        vx = (-5..5).random().toFloat(),
                        vy = (5..15).random().toFloat(),
                        color = colors.random(),
                        size = (10..24).random().toFloat()
                    )
                )
            }
        }
    }
    
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(16)
            particles.forEach { p ->
                p.y += p.vy
                p.x += p.vx
                p.vy += 0.08f
                if (p.y > 2200f) {
                    p.y = -100f
                    p.x = (0..1000).random().toFloat()
                    p.vy = (5..15).random().toFloat()
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .testTag("celebration_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFCF9F1)),
            border = BorderStroke(2.dp, Color(0xFF1F483E))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    particles.forEach { p ->
                        val drawX = p.x % size.width
                        drawCircle(
                            color = p.color,
                            radius = p.size,
                            center = Offset(drawX, p.y)
                        )
                    }
                }
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🎉", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1F483E),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF43493E),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("close_celebration_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43493E)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Dismiss", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        
                        Button(
                            onClick = {
                                shareCelebrationCard(context, "Achievement!", subtitle, extraText)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("share_celebration_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F483E)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Particle data class
class ConfettiParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    val size: Float
)

fun shareCelebrationCard(
    context: android.content.Context,
    title: String,
    subtitle: String,
    extraText: String
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
        textSize = 54f
        isFakeBoldText = true
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
    }
    canvas.drawText(title, size / 2f, 300f, titlePaint)
    
    val subtitlePaint = android.graphics.Paint().apply {
        color = 0xFF1B1C17.toInt()
        textSize = 36f
        isFakeBoldText = true
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
    }
    
    if (subtitle.length > 35) {
        val middle = subtitle.indexOf(' ', subtitle.length / 2)
        val part1 = if (middle != -1) subtitle.substring(0, middle) else subtitle
        val part2 = if (middle != -1) subtitle.substring(middle + 1) else ""
        canvas.drawText(part1, size / 2f, 450f, subtitlePaint)
        if (part2.isNotEmpty()) {
            canvas.drawText(part2, size / 2f, 500f, subtitlePaint)
        }
    } else {
        canvas.drawText(subtitle, size / 2f, 470f, subtitlePaint)
    }
    
    try {
        val logoSrc = android.graphics.BitmapFactory.decodeResource(context.resources, com.example.R.drawable.ic_logo_heart)
        if (logoSrc != null) {
            val scaledLogo = android.graphics.Bitmap.createScaledBitmap(logoSrc, 120, 120, true)
            canvas.drawBitmap(scaledLogo, size / 2f - 60f, 580f, null)
        }
    } catch (_: Exception) {}
    
    val watermarkTitlePaint = android.graphics.Paint().apply {
        color = 0xFF1F483E.toInt()
        textSize = 36f
        isFakeBoldText = true
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
    }
    canvas.drawText("FloraFlow", size / 2f, 760f, watermarkTitlePaint)
    
    val watermarkSubPaint = android.graphics.Paint().apply {
        color = 0xFF43493E.toInt()
        textSize = 22f
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
    }
    canvas.drawText("Designed with FloraFlow", size / 2f, 810f, watermarkSubPaint)
    
    try {
        val cachePath = java.io.File(context.cacheDir, "shared_gardens")
        cachePath.mkdirs()
        val file = java.io.File(cachePath, "celebration_snapshot.png")
        val stream = java.io.FileOutputStream(file)
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()
        
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "My FloraFlow Achievement")
            putExtra(android.content.Intent.EXTRA_TEXT, extraText)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Achievement"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Failed to share: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}

// Helpers
private fun isToday(timestamp: Long): Boolean {
    val dayMillis = 24 * 60 * 60 * 1000
    val todayStart = (System.currentTimeMillis() / dayMillis) * dayMillis
    return timestamp >= todayStart
}

private fun getWeatherEmoji(condition: String): String {
    return when (condition.lowercase()) {
        "rain" -> "🌧️"
        "snow" -> "❄️"
        "clear" -> "☀️"
        "clouds" -> "☁️"
        "heatwave" -> "🥵"
        "frost" -> "🥶"
        else -> "⛅"
    }
}

private fun calculateStreak(logs: List<MoodLog>): Int {
    if (logs.isEmpty()) return 0
    val dayMillis = 24 * 60 * 60 * 1000
    val days = logs.map { it.timestamp / dayMillis }.distinct().sortedDescending()
    
    var streak = 1
    val today = System.currentTimeMillis() / dayMillis
    
    if (days.first() < today - 1) {
        return 0 // Streak broken
    }
    
    for (i in 0 until days.size - 1) {
        if (days[i] - days[i+1] == 1L) {
            streak++
        } else {
            break
        }
    }
    return streak
}


// Helper to resolve string based Material Symbols
fun imageIcons(name: String): androidx.compose.ui.graphics.vector.ImageVector = when (name) {
    "eco" -> Icons.Default.Eco
    "spa" -> Icons.Default.Spa
    else -> Icons.Default.Terrain
}


