package com.example.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.graphics.BotanicalSeason
import com.example.ui.components.graphics.SeasonalBadgeChip
import com.example.ui.screens.dashboard.components.*
import com.example.ui.screens.settings.SettingsDialog
import com.example.ui.theme.extendedColors
import com.example.ui.viewmodel.GardenViewModel

/**
 * The dashboard: one vertical scroll, grouped into a few labelled sections.
 *
 * This replaced a three-page horizontal card deck. The deck hid two thirds of the
 * dashboard behind a swipe most users never made, repeated the same AI Counsel card on
 * every page, and — because each page was a fixed-height pane rather than part of the
 * page scroll — clipped its own content. Everything is now on one surface in priority
 * order, so nothing needs discovering and nothing gets cut off.
 *
 * Cards that duplicated a whole tab were removed rather than moved: the binaural studio
 * (the Restoration tab has the full soundscape controls) and a hardcoded three-plant
 * "recommendations" list that ignored the user's actual plants (the Library tab has real
 * search and filtering).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SanctuaryCardDeckScreen(
    viewModel: GardenViewModel,
    modifier: Modifier = Modifier
) {
    var showSettingsDialog by remember { mutableStateOf(false) }

    val assessmentScore by viewModel.assessmentScore.collectAsStateWithLifecycle()
    val lowestCategories by viewModel.lowestCategories.collectAsStateWithLifecycle()
    val needsReassessment by viewModel.needsReassessment.collectAsStateWithLifecycle()

    val step1Completed by viewModel.step1Completed.collectAsStateWithLifecycle()
    val step2Completed by viewModel.step2Completed.collectAsStateWithLifecycle()
    val step3Completed by viewModel.step3Completed.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("sanctuary_card_deck_screen"),
        topBar = {
            TopAppBar(
                // Just the wordmark and one action. The previous bar packed a two-line
                // title, a seasonal chip and three icon buttons into the same row, which
                // squeezed the chip until its label wrapped one character per line.
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(id = R.drawable.ic_logo_heart),
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(9.dp))
                        )
                        Text(
                            text = "FloraFlow",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .testTag("sanctuary_dashboard_scroll"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            GreetingRow()

            if (needsReassessment) {
                ReassessmentPrompt(
                    onRetake = { viewModel.startRestorativeCornerAssessment() }
                )
            }

            BiophilicProfileCard(
                score = assessmentScore ?: 0,
                lowestCategories = lowestCategories,
                onRetakeClick = { viewModel.startRestorativeCornerAssessment() },
                step1Completed = step1Completed,
                step2Completed = step2Completed,
                step3Completed = step3Completed,
                onStepToggle = { idx -> viewModel.toggleStepCompleted(idx) },
                // These shortcuts used to scroll the pager. They now jump to the tab that
                // actually owns the work: plants to Library, daily tending to Restoration.
                onNavigate = { target -> viewModel.setCurrentTab(if (target <= 1) 1 else 4) },
                onSearchDatabase = { query -> viewModel.setLibrarySearchQuery(query) },
                onOpenVibeCheck = { viewModel.openRoomVibeCheck() },
                onOpenReelsExporter = { viewModel.openReelsExporter() }
            )

            SectionLabel("Today")

            DailyHabitCard(viewModel = viewModel)

            SectionLabel("Your space")

            RealTimeLightMeterCard(viewModel = viewModel)

            SectionLabel("Restore")

            MindfulBreathingCard(viewModel = viewModel)

            // Clears the bottom navigation bar so the last card is fully reachable.
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

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

/** Greeting plus the seasonal badge, on their own line where the chip has room to sit. */
@Composable
private fun GreetingRow() {
    val greeting = remember {
        when (java.time.LocalTime.now().hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Winding down"
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.width(8.dp))
        SeasonalBadgeChip(season = BotanicalSeason.SPRING_BLOOM)
    }
}

/** Quiet group heading — enough structure to scan the scroll without adding chrome. */
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun ReassessmentPrompt(onRetake: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Your assessment is a month old. Retake it to keep your guidance accurate.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = onRetake, shape = RoundedCornerShape(12.dp)) {
                Text("Retake", fontSize = 13.sp, maxLines = 1, softWrap = false)
            }
        }
    }
}

@Composable
private fun RealTimeLightMeterCard(viewModel: GardenViewModel) {
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = null,
                    tint = MaterialTheme.extendedColors.premiumGold
                )
                Text(
                    text = "Daylight sensor",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${currentLux.toInt()} lux",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Text(
                text = "${lightZone.label} • ${lightZone.description}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            LinearProgressIndicator(
                progress = { (currentLux / 2000f).coerceIn(0.05f, 1.0f) },
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
