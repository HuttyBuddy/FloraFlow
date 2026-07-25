package com.example.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.screens.AiStudioScreen
import com.example.ui.screens.dashboard.components.*
import com.example.ui.screens.settings.SettingsDialog
import com.example.ui.theme.extendedColors
import com.example.ui.viewmodel.GardenViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SanctuaryCardDeckScreen(
    viewModel: GardenViewModel,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    var showAiCounselSheet by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val activeLayout by viewModel.activeLayout.collectAsStateWithLifecycle()
    val activePlants by viewModel.activePlants.collectAsStateWithLifecycle()
    val assessmentScore by viewModel.assessmentScore.collectAsStateWithLifecycle()
    val lowestCategories by viewModel.lowestCategories.collectAsStateWithLifecycle()
    val isAssessmentSkipped by viewModel.isAssessmentSkipped.collectAsStateWithLifecycle()
    val weather by viewModel.currentWeather.collectAsStateWithLifecycle()

    val step1Completed by viewModel.step1Completed.collectAsStateWithLifecycle()
    val step2Completed by viewModel.step2Completed.collectAsStateWithLifecycle()
    val step3Completed by viewModel.step3Completed.collectAsStateWithLifecycle()
    val needsReassessment by viewModel.needsReassessment.collectAsStateWithLifecycle()

    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("sanctuary_card_deck_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(id = R.drawable.ic_logo_heart),
                            contentDescription = "FloraFlow Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Column {
                            Text(
                                text = "FloraFlow Sanctuary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = when (pagerState.currentPage) {
                                    0 -> "Card 1: Restorative Corner"
                                    1 -> "Card 2: Plant Companion Match"
                                    else -> "Card 3: Daily Tend & Soundscapes"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("deck_settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Sleek 3-Dot Pager Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isSelected) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                            .clickable {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                    )
                }
            }

            // Horizontal Card Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("sanctuary_card_pager")
            ) { page ->
                when (page) {
                    0 -> Card1RestorativeCorner(
                        viewModel = viewModel,
                        assessmentScore = assessmentScore,
                        lowestCategories = lowestCategories,
                        isAssessmentSkipped = isAssessmentSkipped,
                        needsReassessment = needsReassessment,
                        step1Completed = step1Completed,
                        step2Completed = step2Completed,
                        step3Completed = step3Completed,
                        weather = weather,
                        onOpenAiCounsel = { showAiCounselSheet = true },
                        onNavigateToPage = { targetPage ->
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(targetPage.coerceIn(0, 2))
                            }
                        }
                    )
                    1 -> Card2PlantMatch(
                        viewModel = viewModel,
                        activeLayout = activeLayout,
                        activePlants = activePlants,
                        onOpenAiCounsel = { showAiCounselSheet = true }
                    )
                    2 -> Card3DailyTendAndAudio(
                        viewModel = viewModel,
                        onOpenAiCounsel = { showAiCounselSheet = true }
                    )
                }
            }
        }
    }

    // AI Counsel Slide-Up Bottom Sheet
    if (showAiCounselSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAiCounselSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.testTag("ai_counsel_bottom_sheet")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(16.dp)
            ) {
                AiStudioScreen(viewModel = viewModel)
            }
        }
    }

    // Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            visible = showSettingsDialog,
            onDismiss = { showSettingsDialog = false },
            onFeedbackClick = { showSettingsDialog = false },
            onHelpClick = { showSettingsDialog = false },
            viewModel = viewModel
        )
    }
}

@Composable
private fun AiPlantCounselTopCard(
    onOpenAiCounsel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenAiCounsel() }
            .testTag("ai_counsel_fab")
            .testTag("ai_counsel_top_btn"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Ask AI Plant Counsel",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Chat with AI Plant Counsel",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Ask Dr. Julian about light matching, plant placement & care",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open AI Plant Counsel",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun Card1RestorativeCorner(
    viewModel: GardenViewModel,
    assessmentScore: Int?,
    lowestCategories: List<String>,
    isAssessmentSkipped: Boolean,
    needsReassessment: Boolean,
    step1Completed: Boolean,
    step2Completed: Boolean,
    step3Completed: Boolean,
    weather: com.example.data.repository.WeatherInfo,
    onOpenAiCounsel: () -> Unit,
    onNavigateToPage: (Int) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AiPlantCounselTopCard(onOpenAiCounsel = onOpenAiCounsel)

        if (needsReassessment) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Restorative Corner assessment is 30 days old. Refresh to keep layout guidance relevant.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.startRestorativeCornerAssessment() },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Retake", fontSize = 12.sp)
                    }
                }
            }
        }

        BiophilicProfileCard(
            score = assessmentScore ?: 0,
            lowestCategories = lowestCategories,
            onRetakeClick = { viewModel.startRestorativeCornerAssessment() },
            step1Completed = step1Completed,
            step2Completed = step2Completed,
            step3Completed = step3Completed,
            onStepToggle = { idx -> viewModel.toggleStepCompleted(idx) },
            onNavigate = onNavigateToPage,
            onSearchDatabase = { query -> viewModel.setLibrarySearchQuery(query) },
            onOpenVibeCheck = { viewModel.openRoomVibeCheck() }
        )

        WeatherSyncCard(
            weather = weather,
            onWeatherClick = {}
        )

        RealTimeLightMeterCard(viewModel = viewModel)
    }
}

@Composable
private fun RealTimeLightMeterCard(
    viewModel: GardenViewModel
) {
    val currentLux by viewModel.currentLux.collectAsStateWithLifecycle()
    val lightZone by viewModel.lightZone.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        viewModel.startLightSensor()
        onDispose { viewModel.stopLightSensor() }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("real_time_light_meter_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = "Real-Time Light Meter",
                        tint = MaterialTheme.extendedColors.premiumGold
                    )
                    Text(
                        text = "Real-Time Daylight Sensor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${currentLux.toInt()} Lux",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Text(
                text = "Zone: ${lightZone.label} • ${lightZone.description}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val progress = (currentLux / 2000f).coerceIn(0.05f, 1.0f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.extendedColors.premiumGold,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun Card2PlantMatch(
    viewModel: GardenViewModel,
    activeLayout: com.example.data.model.GardenLayout?,
    activePlants: List<com.example.data.model.Plant>,
    onOpenAiCounsel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AiPlantCounselTopCard(onOpenAiCounsel = onOpenAiCounsel)

        CompanionSynergyCard(activeLayout = activeLayout)

        // Plant Recommendations Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("recommended_plants_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Spa,
                        contentDescription = "Recommended Living Plants",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Plants for Your Corner Light",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "Top living plants matched to your corner's natural light, humidity, and acoustic goals:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val recommendedSpecies = listOf(
                    Triple("Lavender", "Aromatic • Lowers Sympathetic Arousal", "Bright Indirect Light"),
                    Triple("Peace Lily", "Acoustic Masking • Air Purifier", "Medium/Low Light"),
                    Triple("Bonsai Juniper", "Mindful Micro-Tending • Focus", "Bright Daylight")
                )

                recommendedSpecies.forEach { (name, desc, light) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = light,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Button(
                                onClick = {
                                    viewModel.sendAiChatMessage("Tell me how to place $name in my restorative corner.")
                                    onOpenAiCounsel()
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Placement Tip", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Card3DailyTendAndAudio(
    viewModel: GardenViewModel,
    onOpenAiCounsel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AiPlantCounselTopCard(onOpenAiCounsel = onOpenAiCounsel)

        PlantCareStreakCard(viewModel = viewModel)

        DailyHabitCard(viewModel = viewModel)

        MindfulBreathingCard(viewModel = viewModel)

        CustomBinauralStudioCard(viewModel = viewModel)
    }
}

@Composable
private fun PlantCareStreakCard(
    viewModel: GardenViewModel
) {
    val streakDays by viewModel.careStreakDays.collectAsStateWithLifecycle()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("care_streak_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔥", fontSize = 20.sp)
                }

                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "$streakDays-Day Plant Care Streak",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when {
                            streakDays < 7 -> "Badge Unlocked: Seedling Tender 🌱"
                            streakDays < 30 -> "Badge Unlocked: Botanical Caregiver 🌿"
                            else -> "Badge Unlocked: Master Sanctuary Keeper 🌳"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Button(
                onClick = { viewModel.incrementCareStreak() },
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Log Care",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun CustomBinauralStudioCard(
    viewModel: GardenViewModel
) {
    val frequencyHz by viewModel.binauralFrequencyHz.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("custom_binaural_studio_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Binaural Brainwave Studio",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = "Custom Binaural Studio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                if (!isPremium) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.clickable { viewModel.triggerPaywall() }
                    ) {
                        Text(
                            text = "PRO Studio",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Interactive Preset Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                BinauralPresetChip(
                    label = "Alpha 10Hz",
                    selected = frequencyHz == 10f,
                    onClick = {
                        if (isPremium) viewModel.setBinauralFrequency(10f) else viewModel.triggerPaywall()
                    },
                    modifier = Modifier.weight(1f)
                )
                BinauralPresetChip(
                    label = "Theta 6Hz",
                    selected = frequencyHz == 6f,
                    onClick = {
                        if (isPremium) viewModel.setBinauralFrequency(6f) else viewModel.triggerPaywall()
                    },
                    modifier = Modifier.weight(1f)
                )
                BinauralPresetChip(
                    label = "Gamma 40Hz",
                    selected = frequencyHz == 40f,
                    onClick = {
                        if (isPremium) viewModel.setBinauralFrequency(40f) else viewModel.triggerPaywall()
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Slider(
                value = frequencyHz,
                onValueChange = { newValue ->
                    if (isPremium) {
                        viewModel.setBinauralFrequency(newValue)
                    } else {
                        viewModel.triggerPaywall()
                    }
                },
                valueRange = 4f..40f,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun BinauralPresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerBg = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
    val contentColor = if (selected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onTertiaryContainer

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = containerBg,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
        )
    }
}
