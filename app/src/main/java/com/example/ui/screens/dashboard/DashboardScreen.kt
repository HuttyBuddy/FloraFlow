package com.example.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.intl.Locale as ComposeLocale
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

import com.example.ui.screens.dashboard.components.*

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
    val assessmentScore by viewModel.assessmentScore.collectAsStateWithLifecycle()
    val lowestCategories by viewModel.lowestCategories.collectAsStateWithLifecycle()
    val weather by viewModel.currentWeather.collectAsStateWithLifecycle()

    val step1Completed by viewModel.step1Completed.collectAsStateWithLifecycle()
    val step2Completed by viewModel.step2Completed.collectAsStateWithLifecycle()
    val step3Completed by viewModel.step3Completed.collectAsStateWithLifecycle()
    val needsReassessment by viewModel.needsReassessment.collectAsStateWithLifecycle()
    val assessmentHistory by viewModel.allAssessmentResults.collectAsStateWithLifecycle()

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

    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600

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
                    text = "You haven't taken your Neural Load assessment yet. Take a 2-minute assessment to personalize your space.",
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
                        text = "Begin Your Garden Journey",
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
                    text = "Your last Neural Load assessment was completed 30 days ago. Audit your biophilic sanctuary progress now!",
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
                        onClick = { viewModel.resetAssessment() },
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
                Column(modifier = Modifier.padding(16.dp)) {
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

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        assessmentHistory.take(5).forEach { result ->
                            val sdf = java.text.SimpleDateFormat("MMMM dd, yyyy - hh:mm a", java.util.Locale.US)
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
                                                in 15..20 -> Color(0xFFE8F5E9)
                                                in 8..14 -> Color(0xFFFFFDE7)
                                                else -> Color(0xFFFFEBEE)
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${result.score}/20",
                                        fontWeight = FontWeight.Bold,
                                        color = when (result.score) {
                                            in 15..20 -> Color(0xFF2E7D32)
                                            in 8..14 -> Color(0xFFF57F17)
                                            else -> Color(0xFFC62828)
                                        },
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
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
                .testTag("dashboard_space_diagnosis_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)),
            border = BorderStroke(
                1.5.dp,
                Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
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
                        .background(MaterialTheme.colorScheme.tertiary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ManageSearch,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Conversational Space Diagnosis",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "Let Dr. Julian guide you through an audit of your room's biophilic states",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
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
                    Icons.AutoMirrored.Filled.ArrowForward,
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
                        onRetakeClick = { viewModel.resetAssessment() },
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
            item { MonthlyWellnessDigestCard(moodLogs = moodLogs) }
            item { SeasonalCareCoachCard(activePlants = activePlants) }
            item { quickActionsContent() }
            item { communityPromoContent() }
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
                        onRetakeClick = { viewModel.resetAssessment() },
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
