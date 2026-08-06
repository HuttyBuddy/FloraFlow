package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import com.example.ui.screens.settings.SettingsDialog
import com.example.ui.screens.dashboard.SanctuaryCardDeckScreen
import com.example.ui.screens.settings.InAppRatePromptDialog
import com.example.ui.screens.walkthrough.WalkthroughOverlay
import com.example.ui.screens.help.HelpDialog
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.ui.layout.boundsInRoot
import com.example.ui.viewmodel.WalkthroughStep
import com.example.ui.viewmodel.ScreenRect
import androidx.compose.material3.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import com.example.ui.screens.*
import com.example.ui.screens.restoration.RestorationJournalScreen
import com.example.ui.screens.restorativevalidation.RestorativeValidationRoute
import com.example.ui.screens.restorativevalidation.RestorativeValidationViewModel
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.feedback.FeedbackDialog
import androidx.activity.viewModels
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.RestorativeValidationTypography
import com.example.ui.theme.extendedColors
import com.example.ui.theme.spacing
import com.example.ui.viewmodel.GardenViewModel
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit
import com.example.ui.screens.dashboard.CareSyncWorker
import kotlinx.coroutines.delay

internal enum class StartupMode {
    PRODUCTION,
    RESTORATIVE_VALIDATION;

    val runsProductionStartup: Boolean
        get() = this == PRODUCTION

    companion object {
        fun from(restorativeValidation: Boolean): StartupMode = if (restorativeValidation) {
            RESTORATIVE_VALIDATION
        } else {
            PRODUCTION
        }
    }
}

class MainActivity : ComponentActivity() {
    private val startupMode = StartupMode.from(BuildConfig.RESTORATIVE_VALIDATION)
    private val viewModel: GardenViewModel by viewModels()

    override fun onResume() {
        super.onResume()
        if (!startupMode.runsProductionStartup) return
        viewModel.billingManager.queryPurchases()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // An invite link opened while the app is already running still counts.
        handleInviteIntent(intent)
    }

    /** Records the inviter's code from an incoming invite link, logging it once. */
    private fun handleInviteIntent(intent: Intent?) {
        val code = com.example.ui.screens.share.ShareLinks.handleIncomingIntent(this, intent)
        if (code != null) {
            com.example.analytics.ShareAnalytics.logInviteAccepted("deep_link")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        com.example.analytics.AnalyticsHelper.initialize(applicationContext)

        // Attribute this install to whoever shared the link that brought the user here.
        // Deep link covers "app already installed"; the Play install referrer covers the
        // more common viral path of link -> Play Store -> install -> first launch.
        handleInviteIntent(intent)
        com.example.ui.screens.share.InstallAttribution.checkInstallReferrer(applicationContext)

        try {
            val workRequest = PeriodicWorkRequestBuilder<CareSyncWorker>(12, TimeUnit.HOURS).build()
            WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                "CareSyncWork",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        } catch (e: Exception) {
            if (com.example.BuildConfig.DEBUG) {
                android.util.Log.e("MainActivity", "Failed to schedule CareSyncWorker: ${e.message}")
            }
        }

        var initialValidationActive by mutableStateOf(!startupMode.runsProductionStartup)

        setContent {
            val showRestorativeValidationFlow by viewModel.showRestorativeValidationFlow.collectAsState()
            val isValidationActive = initialValidationActive || showRestorativeValidationFlow
            val isDarkThemeOverridden by viewModel.isDarkTheme.collectAsState()
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val useDarkTheme = isDarkThemeOverridden ?: systemDark

            if (isValidationActive) {
                MyApplicationTheme(typography = RestorativeValidationTypography) {
                    val validationViewModel: RestorativeValidationViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = RestorativeValidationViewModel.factory(applicationContext),
                    )

                    androidx.compose.runtime.LaunchedEffect(showRestorativeValidationFlow) {
                        if (showRestorativeValidationFlow) {
                            validationViewModel.resetAndStartNewAssessment()
                        }
                    }

                    RestorativeValidationRoute(
                        viewModel = validationViewModel,
                        onExit = {
                            val draft = validationViewModel.uiState.value.draft
                            if (draft.light != null || draft.availableSpace != null) {
                                val score = com.example.ui.screens.restorativevalidation.RestorativeRecommendationEngine.calculateScore(draft)
                                val categories = listOf("Natural Daylight", "Acoustic Masking")
                                viewModel.saveAssessmentResult(score, categories)
                            }
                            initialValidationActive = false
                            viewModel.dismissRestorativeCornerAssessment()
                        },
                    )
                }
            } else {
                val currentTab by viewModel.currentTab.collectAsState()
            val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()

            // The app bar remains light in the Restoration tab, so keep dark status-bar
            // icons there instead of switching them to low-contrast light icons.
            val forceDarkStatusBar = useDarkTheme

            val statusBarStyle = if (forceDarkStatusBar) {
                androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
            }
            val navigationBarStyle = if (useDarkTheme) {
                androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
            }

            DisposableEffect(useDarkTheme, currentTab, isOnboardingCompleted) {
                enableEdgeToEdge(
                    statusBarStyle = statusBarStyle,
                    navigationBarStyle = navigationBarStyle
                )
                onDispose {}
            }

            MyApplicationTheme(darkTheme = useDarkTheme) {
                // Composed regardless of onboarding state — it self-hides via
                // showBillingDialog, but must exist during onboarding too since
                // the post-assessment paywall can trigger it before the main
                // Scaffold (and its previous BillingDialog instance) exists.
                val showPaywallDialog by viewModel.showPaywallDialog.collectAsState()

                com.example.ui.screens.paywall.PaywallDialog(
                    visible = showPaywallDialog,
                    onDismiss = { viewModel.dismissPaywall() },
                    onSubscribe = { isAnnual -> viewModel.subscribePro(isAnnual) }
                )

                BillingDialog(viewModel = viewModel)

                val showReelsExporterOverlay by viewModel.showReelsExporterOverlay.collectAsState()
                val assessmentScore by viewModel.assessmentScore.collectAsState()
                val lowestCategories by viewModel.lowestCategories.collectAsState()
                val binauralHz by viewModel.binauralFrequencyHz.collectAsState()

                if (showReelsExporterOverlay) {
                    val archetype = com.example.data.model.PlantParentArchetype.calculateArchetype(
                        score = assessmentScore ?: 14,
                        lowestCategories = lowestCategories
                    )
                    com.example.ui.screens.share.ReelsExporterOverlay(
                        // The assessment scores out of 20; the reel renders "N%", so a raw
                        // 14 was previously shown as "14%".
                        score = assessmentScore?.let { (it.coerceIn(0, 20) * 100) / 20 } ?: 88,
                        archetype = archetype,
                        frequencyHz = binauralHz,
                        onDismiss = { viewModel.closeReelsExporter() }
                    )
                }

                val showRoomVibeCheckScreen by viewModel.showRoomVibeCheckScreen.collectAsState()

                if (showRoomVibeCheckScreen) {
                    com.example.ui.screens.vibecheck.RoomVibeCheckScreen(
                        onBack = { viewModel.closeRoomVibeCheck() },
                        onTriggerPaywall = { viewModel.triggerPaywall() }
                    )
                } else if (!isOnboardingCompleted) {
                    OnboardingScreen(viewModel = viewModel)
                } else {
                    Scaffold(
                        bottomBar = {
                            // No fixed height: NavigationBar sizes itself and adds the
                            // system gesture inset. Pinning it to 80.dp left no room for
                            // that inset, so the labels were clipped by the screen edge.
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 0.dp
                            ) {
                                // Short labels: five items share the width, and
                                // "Dashboard"/"Restoration" could not fit their slot.
                                val tabs = listOf(
                                    Triple("Home", Icons.Default.Dashboard, 0),
                                    Triple("Plants", Icons.AutoMirrored.Filled.MenuBook, 1),
                                    Triple("Design", Icons.Default.Spa, 2),
                                    // A robot head read as generic chatbot, not as a plant
                                    // advisor in a calm biophilic app.
                                    Triple("Counsel", Icons.Default.Eco, 3),
                                    Triple("Restore", Icons.Default.SelfImprovement, 4)
                                )
                                tabs.forEach { (label, icon, index) ->
                                    NavigationBarItem(
                                        selected = currentTab == index,
                                        onClick = { viewModel.setCurrentTab(index) },
                                        icon = { Icon(icon, contentDescription = label) },
                                        label = {
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            unselectedIconColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                        )
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                            when (currentTab) {
                                0 -> SanctuaryCardDeckScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                                1 -> LibraryScreen(viewModel = viewModel, switchToChatTab = { viewModel.setCurrentTab(3) }, modifier = Modifier.fillMaxSize())
                                2 -> PlannerScreen(viewModel = viewModel, switchToChatTab = { viewModel.setCurrentTab(3) }, modifier = Modifier.fillMaxSize())
                                3 -> AiStudioScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                                4 -> RestorationJournalScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                                else -> SanctuaryCardDeckScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }
        }
    }
}
}


