package com.example.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.window.Dialog
import com.example.ui.viewmodel.GardenViewModel
import com.example.ui.theme.extendedColors
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import com.example.ui.viewmodel.WalkthroughStep
import com.example.ui.viewmodel.ScreenRect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.core.content.edit

import androidx.compose.ui.draw.blur
import com.example.ui.screens.dashboard.components.*

@Composable
fun DashboardScreen(
    viewModel: GardenViewModel,
    modifier: Modifier = Modifier
) {
    val layouts by viewModel.allLayouts.collectAsStateWithLifecycle()
    val activeLayout by viewModel.activeLayout.collectAsStateWithLifecycle()
    val activePlants by viewModel.activePlants.collectAsStateWithLifecycle()
    val moodLogs by viewModel.allMoodLogs.collectAsStateWithLifecycle()
    val isAssessmentSkipped by viewModel.isAssessmentSkipped.collectAsStateWithLifecycle()
    val assessmentScore by viewModel.assessmentScore.collectAsStateWithLifecycle()
    val lowestCategories by viewModel.lowestCategories.collectAsStateWithLifecycle()
    val weather by viewModel.currentWeather.collectAsStateWithLifecycle()

    val step1Completed by viewModel.step1Completed.collectAsStateWithLifecycle()
    val step2Completed by viewModel.step2Completed.collectAsStateWithLifecycle()
    val step3Completed by viewModel.step3Completed.collectAsStateWithLifecycle()
    val needsReassessment by viewModel.needsReassessment.collectAsStateWithLifecycle()
    val assessmentHistory by viewModel.allAssessmentResults.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()

    val todayLog = remember(moodLogs) {
        val todayCal = java.util.Calendar.getInstance()
        moodLogs.find { log ->
            val logCal = java.util.Calendar.getInstance().apply { timeInMillis = log.timestamp }
            logCal.get(java.util.Calendar.YEAR) == todayCal.get(java.util.Calendar.YEAR) &&
            logCal.get(java.util.Calendar.DAY_OF_YEAR) == todayCal.get(java.util.Calendar.DAY_OF_YEAR)
        }
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var showLayoutSelector by remember { mutableStateOf(false) }
    var showLogMoodDialog by remember { mutableStateOf(false) }
    var showZipDialog by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val isWideScreen = with(density) { windowInfo.containerSize.width.toDp() >= 600.dp }

    val skippedAssessmentBanner = @Composable {
        val isDark = androidx.compose.foundation.isSystemInDarkTheme()
        val bannerBg = if (isDark) Color(0xFF2D2517) else Color(0xFFFFF8E1)
        val bannerBorder = if (isDark) Color(0xFF4E3D20) else Color(0xFFFFE082)
        val bannerText = if (isDark) Color(0xFFFFD54F) else Color(0xFF5D4037)
        val buttonBg = if (isDark) Color(0xFF4E3D20) else Color(0xFFFFF3CD)
        val buttonText = if (isDark) Color(0xFFFFD54F) else Color(0xFF856404)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("skipped_assessment_banner"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = bannerBg
            ),
            border = BorderStroke(1.dp, bannerBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Personalize your indoor or outdoor space with a 2-minute biophilic assessment.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = bannerText,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = { viewModel.resetAssessment() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonBg,
                        contentColor = buttonText
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Personalize My Space",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    val reassessmentPromptBanner = @Composable {
        val isDark = androidx.compose.foundation.isSystemInDarkTheme()
        val bannerBg = if (isDark) Color(0xFF1B3B32) else Color(0xFFE8F5E9)
        val bannerBorder = if (isDark) Color(0xFF2E7D32) else Color(0xFFC8E6C9)
        val bannerText = if (isDark) Color(0xFFA5D6A7) else Color(0xFF1B5E20)
        val buttonBg = if (isDark) Color(0xFF2E7D32) else Color(0xFFC8E6C9)
        val buttonText = if (isDark) Color(0xFFE8F5E9) else Color(0xFF1B5E20)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reassessment_prompt_banner"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = bannerBg
            ),
            border = BorderStroke(1.dp, bannerBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Your space assessment is 30 days old. Refresh it to keep design, care, and restoration guidance relevant.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = bannerText,
                    textAlign = TextAlign.Center
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.startRestorativeCornerAssessment() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = buttonBg,
                            contentColor = buttonText
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Retake Assessment",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    OutlinedButton(
                        onClick = { viewModel.snoozeReassessment() },
                        border = BorderStroke(1.dp, bannerText.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = bannerText
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("reassessment_snooze_btn")
                    ) {
                        Text(
                            text = "Remind Me Later",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }

    val scoreHistoryCardContent = @Composable {
        if (assessmentHistory.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_score_history_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        1.5.dp,
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                            )
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .run {
                                if (!isPremium) blur(4.dp) else this
                            }
                    ) {
                        Text(
                            text = "Assessment Score History",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Track your biophilic restoration progress over time",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        ScoreHistoryChart(assessmentHistory = assessmentHistory)

                        Spacer(modifier = Modifier.height(16.dp))

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Past Audits",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val sdf = remember { java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US) }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            assessmentHistory.take(5).forEach { result ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = sdf.format(java.util.Date(result.timestamp)),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Lowest: ${result.lowestCategories.split(",").joinToString(", ")}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = when (result.score) {
                                                    in 15..20 -> MaterialTheme.extendedColors.success.copy(alpha = 0.15f)
                                                    in 8..14 -> MaterialTheme.extendedColors.warning.copy(alpha = 0.15f)
                                                    else -> MaterialTheme.extendedColors.error.copy(alpha = 0.15f)
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${result.score}/20",
                                            fontWeight = FontWeight.Bold,
                                            color = when (result.score) {
                                                in 15..20 -> MaterialTheme.extendedColors.success
                                                in 8..14 -> MaterialTheme.extendedColors.warning
                                                else -> MaterialTheme.extendedColors.error
                                            },
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (!isPremium) {
                    com.example.ui.components.PremiumLockOverlay(
                        onUpgradeClick = { viewModel.setBillingDialogVisible(true, "dashboard_score_history") },
                        modifier = Modifier.matchParentSize()
                    )
                }
            }
        }
    }

    val spaceDiagnosisPromoContent = @Composable {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    viewModel.setCurrentTab(3) // AI Advisor tab
                    viewModel.sendAiChatMessage("I want to run a detailed Space Diagnosis of my environment.")
                }
                .testTag("dashboard_space_diagnosis_card")
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                ),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
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
                        .background(MaterialTheme.colorScheme.onPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ManageSearch,
                        contentDescription = "Space Diagnosis",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Conversational Space Diagnosis",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = "Let Dr. Julian guide you through an audit of the state of your biophilic space",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Go to Space Diagnosis",
                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    modifier = Modifier.size(22.dp)
                )
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
                            "Your space is ready to be planted"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
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
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
                            text = "Circular Botanical Rhythm",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sync daily habits with mental well-being",
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

                CircularBotanicalRhythm(
                    todayLog = todayLog,
                    onToggleHabit = { habit -> viewModel.toggleHabitForToday(habit) },
                    onLogMoodClick = { showLogMoodDialog = true }
                )
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
            if (needsReassessment) {
                item { reassessmentPromptBanner() }
            }
            if (isAssessmentSkipped) {
                item { skippedAssessmentBanner() }
            } else if (assessmentScore != null) {
                item {
                    BiophilicProfileCard(
                        score = assessmentScore ?: 0,
                        lowestCategories = lowestCategories,
                        onRetakeClick = { viewModel.startRestorativeCornerAssessment() },
                        step1Completed = step1Completed,
                        step2Completed = step2Completed,
                        step3Completed = step3Completed,
                        onStepToggle = { idx -> viewModel.toggleStepCompleted(idx) },
                        onNavigate = { viewModel.setCurrentTab(it) },
                        onSearchDatabase = { viewModel.setLibrarySearchQuery(it) }
                    )
                }
            }
            item { headerContent() }
            item {
                WeatherSyncCard(
                    weather = weather,
                    onWeatherClick = { showZipDialog = true }
                )
            }
            item { DailyHabitCard(viewModel = viewModel) }
            item { MindfulBreathingCard(viewModel = viewModel) }
            item { CompanionSynergyCard(activeLayout = activeLayout) }
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .run {
                                if (!isPremium) blur(4.dp) else this
                            }
                    ) {
                        MonthlyWellnessDigestCard(moodLogs = moodLogs)
                    }
                    if (!isPremium) {
                        com.example.ui.components.PremiumLockOverlay(
                            onUpgradeClick = { viewModel.setBillingDialogVisible(true, "dashboard_wellness_digest") },
                            modifier = Modifier.matchParentSize()
                        )
                    }
                }
            }
            item { SeasonalCareCoachCard(activePlants = activePlants) }
            item { quickActionsContent() }
            item { statisticsContent() }
            item { canvasChartContent() }
            item { scoreHistoryCardContent() }
            item { spaceDiagnosisPromoContent() }
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
                if (needsReassessment) {
                    reassessmentPromptBanner()
                }
                if (isAssessmentSkipped) {
                    skippedAssessmentBanner()
                } else if (assessmentScore != null) {
                    BiophilicProfileCard(
                        score = assessmentScore ?: 0,
                        lowestCategories = lowestCategories,
                        onRetakeClick = { viewModel.startRestorativeCornerAssessment() },
                        step1Completed = step1Completed,
                        step2Completed = step2Completed,
                        step3Completed = step3Completed,
                        onStepToggle = { idx -> viewModel.toggleStepCompleted(idx) },
                        onNavigate = { viewModel.setCurrentTab(it) },
                        onSearchDatabase = { viewModel.setLibrarySearchQuery(it) }
                    )
                }
                headerContent()
                WeatherSyncCard(
                    weather = weather,
                    onWeatherClick = { showZipDialog = true }
                )
                DailyHabitCard(viewModel = viewModel)
                MindfulBreathingCard(viewModel = viewModel)
                CompanionSynergyCard(activeLayout = activeLayout)
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .run {
                                if (!isPremium) blur(4.dp) else this
                            }
                    ) {
                        MonthlyWellnessDigestCard(moodLogs = moodLogs)
                    }
                    if (!isPremium) {
                        com.example.ui.components.PremiumLockOverlay(
                            onUpgradeClick = { viewModel.setBillingDialogVisible(true, "dashboard_wellness_digest") },
                            modifier = Modifier.matchParentSize()
                        )
                    }
                }
                SeasonalCareCoachCard(activePlants = activePlants)
                quickActionsContent()
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
                scoreHistoryCardContent()
                spaceDiagnosisPromoContent()
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

    // --- DIALOG: Change Zip Code ---
    if (showZipDialog) {
        Dialog(onDismissRequest = { showZipDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier
                    .padding(16.dp)
                    .testTag("change_zip_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Update Location Zip Code",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    var zipInput by remember { mutableStateOf(viewModel.getWeatherLocationZip()) }

                    OutlinedTextField(
                        value = zipInput,
                        onValueChange = { zipInput = it },
                        label = { Text("Zip Code") },
                        modifier = Modifier.fillMaxWidth().testTag("zip_code_input"),
                        placeholder = { Text("e.g. 90210") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showZipDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (zipInput.isNotBlank()) {
                                    viewModel.updateWeatherLocation(zipInput.trim())
                                    showZipDialog = false
                                }
                            },
                            enabled = zipInput.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("submit_zip_code")
                        ) {
                            Text("Update")
                        }
                    }
                }
            }
        }
    }

    // --- Celebration Dialogs ---
    val firstBloomTrigger by viewModel.firstBloomTrigger.collectAsStateWithLifecycle()
    val streakCount = remember(moodLogs) { calculateStreak(moodLogs) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("floraflow_prefs", android.content.Context.MODE_PRIVATE) }
    var lastCelebratedStreak by remember { mutableIntStateOf(sharedPrefs.getInt("last_celebrated_streak", 0)) }
    
    var showStreakCelebration by remember { mutableStateOf(false) }
    var celebratedStreakValue by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(streakCount) {
        if ((streakCount == 7 || streakCount == 30 || streakCount == 100) && streakCount > lastCelebratedStreak) {
            celebratedStreakValue = streakCount
            showStreakCelebration = true
        }
    }
    
    if (showStreakCelebration) {
        CelebrationDialog(
            title = "$celebratedStreakValue Day Streak! 🔥",
            subtitle = "I've tended my garden for $celebratedStreakValue days straight on FloraFlow.",
            extraText = "I've tended my garden for $celebratedStreakValue days straight on FloraFlow! 🌸🌿 #FloraFlow",
            onDismiss = {
                sharedPrefs.edit { putInt("last_celebrated_streak", celebratedStreakValue) }
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
