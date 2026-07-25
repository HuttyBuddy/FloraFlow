package com.example

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
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.SmartToy
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

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        com.example.analytics.AnalyticsHelper.initialize(applicationContext)

        try {
            val workRequest = PeriodicWorkRequestBuilder<CareSyncWorker>(12, TimeUnit.HOURS).build()
            WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                "CareSyncWork",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to schedule CareSyncWorker: ${e.message}")
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
                        score = assessmentScore ?: 88,
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
                    SanctuaryCardDeckScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
}


