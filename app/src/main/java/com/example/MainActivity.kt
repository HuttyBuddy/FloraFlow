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

        if (!startupMode.runsProductionStartup) {
            enableEdgeToEdge()
            setContent {
                MyApplicationTheme(typography = RestorativeValidationTypography) {
                    val validationViewModel: RestorativeValidationViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = RestorativeValidationViewModel.factory(applicationContext),
                    )
                    RestorativeValidationRoute(
                        viewModel = validationViewModel,
                        onExit = ::finish,
                    )
                }
            }
            return
        }

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

        setContent {
            val isDarkThemeOverridden by viewModel.isDarkTheme.collectAsState()
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val useDarkTheme = isDarkThemeOverridden ?: systemDark

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
                BillingDialog(viewModel = viewModel)

                if (!isOnboardingCompleted) {
                    OnboardingScreen(viewModel = viewModel)
                } else {
                        var showFeedbackDialog by remember { mutableStateOf(false) }
                        var showSettingsDialog by remember { mutableStateOf(false) }
                        var showHelpDialog by remember { mutableStateOf(false) }
                        val isPremium by viewModel.isPremium.collectAsState()

                        val showInAppRatePrompt by viewModel.showInAppRatePrompt.collectAsState()
                        val showSubscriptionManagement by viewModel.showSubscriptionManagement.collectAsState()
                        SubscriptionManagementDialog(
                            visible = showSubscriptionManagement,
                            onDismiss = { viewModel.setSubscriptionManagementVisible(false) },
                            viewModel = viewModel,
                        )

                        FeedbackDialog(
                            visible = showFeedbackDialog,
                            onDismiss = { showFeedbackDialog = false },
                            viewModel = viewModel
                        )

                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            topBar = {
                                AppTopBar(
                                    isPremium = isPremium,
                                    onUpgradeClick = { viewModel.upgradeToPremium() },
                                    onManageSubscriptionClick = { viewModel.setSubscriptionManagementVisible(true) },
                                    onFeedbackClick = { showFeedbackDialog = true },
                                    onHelpClick = { showHelpDialog = true },
                                    onSettingsClick = { showSettingsDialog = true }
                                )
                            },
                            bottomBar = {
                                AppBottomBar(
                                    currentTab = currentTab,
                                    onTabSelected = { viewModel.setCurrentTab(it) },
                                    onWalkthroughTargetUpdate = { step, rect -> viewModel.updateWalkthroughTarget(step, rect) }
                                )
                            }
                        ) { innerPadding ->
                            val safeBottomPadding = innerPadding.calculateBottomPadding()
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(
                                        start = 0.dp,
                                        top = innerPadding.calculateTopPadding(),
                                        end = 0.dp,
                                        bottom = safeBottomPadding
                                    )
                            ) {
                                when (currentTab) {
                                    0 -> DashboardScreen(
                                        viewModel = viewModel
                                    )
                                    1 -> PlannerScreen(
                                        viewModel = viewModel,
                                        switchToChatTab = { viewModel.setCurrentTab(3) }
                                    )
                                    2 -> LibraryScreen(
                                        viewModel = viewModel,
                                        switchToChatTab = { viewModel.setCurrentTab(3) }
                                    )
                                    3 -> AiStudioScreen(
                                        viewModel = viewModel
                                    )
                                    4 -> RestorationJournalScreen(
                                        viewModel = viewModel
                                    )
                                }
                            }
                        }

                        SettingsDialog(
                            visible = showSettingsDialog,
                            onDismiss = { showSettingsDialog = false },
                            onFeedbackClick = {
                                showSettingsDialog = false
                                showFeedbackDialog = true
                            },
                            onHelpClick = {
                                showHelpDialog = true
                            },
                            viewModel = viewModel
                        )

                        HelpDialog(
                            visible = showHelpDialog,
                            onDismiss = { showHelpDialog = false },
                            viewModel = viewModel
                        )



                        InAppRatePromptDialog(
                            visible = showInAppRatePrompt,
                            onDismiss = { viewModel.dismissRatePrompt() },
                            viewModel = viewModel
                        )

                        WalkthroughOverlay(
                            viewModel = viewModel,
                            currentTab = currentTab,
                            onTabChange = { viewModel.setCurrentTab(it) }
                        )
                    }
                }
            }
        }
    }



@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    isPremium: Boolean,
    onUpgradeClick: () -> Unit,
    onManageSubscriptionClick: () -> Unit,
    onFeedbackClick: () -> Unit,
    onHelpClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_logo_heart),
                    contentDescription = "FloraFlow Logo",
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                Text(
                    text = "FloraFlow",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        actions = {
            IconButton(
                onClick = {
                    if (isPremium) {
                        onManageSubscriptionClick()
                    } else {
                        onUpgradeClick()
                    }
                },
                modifier = Modifier.testTag("premium_key_status_crown")
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = "Subscription Info Status",
                    tint = if (isPremium) MaterialTheme.extendedColors.premiumGold else MaterialTheme.colorScheme.onSurface
                )
            }

            var showMenu by remember { mutableStateOf(false) }
            Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.testTag("more_options_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options"
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Share App Feedback") },
                        onClick = {
                            showMenu = false
                            onFeedbackClick()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Feedback,
                                contentDescription = "Feedback"
                            )
                        },
                        modifier = Modifier.testTag("feedback_button")
                    )
                    DropdownMenuItem(
                        text = { Text("Help & Support") },
                        onClick = {
                            showMenu = false
                            onHelpClick()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = "Help"
                            )
                        },
                        modifier = Modifier.testTag("help_button")
                    )
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        onClick = {
                            showMenu = false
                            onSettingsClick()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        },
                        modifier = Modifier.testTag("settings_button")
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
private fun RowScope.AppBottomBarTab(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tabIndex: Int,
    currentTab: Int,
    testTag: String,
    onTabSelected: (Int) -> Unit,
    onGloballyPositioned: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null
) {
    val isSelected = currentTab == tabIndex
    var modifier = Modifier
        .weight(1f)
        .fillMaxHeight()
        .clip(RoundedCornerShape(12.dp))
        .semantics {
            role = Role.Tab
            selected = isSelected
        }
        .clickable(role = Role.Tab) {
            onTabSelected(tabIndex)
        }
        .testTag(testTag)

    if (onGloballyPositioned != null) {
        modifier = modifier.onGloballyPositioned(onGloballyPositioned)
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val scale by animateFloatAsState(targetValue = if (isSelected) 1.06f else 1.0f, label = "tabScale$tabIndex")
        val uniformTextStyle = MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.sp,
            letterSpacing = 0.sp,
            fontWeight = FontWeight.Medium
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.scale(scale)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(22.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = uniformTextStyle,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AppBottomBar(
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    onWalkthroughTargetUpdate: (WalkthroughStep, ScreenRect) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .height(80.dp)
            .testTag("app_navigation_bar")
    ) {
        val barWidth = maxWidth
        val tabWidth = barWidth / 5
        val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
        val isLtr = layoutDirection == androidx.compose.ui.unit.LayoutDirection.Ltr

        val indicatorOffset by androidx.compose.animation.core.animateDpAsState(
            targetValue = tabWidth * currentTab,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "tabIndicatorOffset"
        )

        // Sliding indicator pill
        Box(
            modifier = Modifier
                .padding(vertical = 17.5.dp, horizontal = 6.dp)
                .width(tabWidth - 12.dp)
                .fillMaxHeight()
                .offset {
                    val xOffset = indicatorOffset.roundToPx()
                    IntOffset(x = if (isLtr) xOffset else -xOffset, y = 0)
                }
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppBottomBarTab(
                title = "Dashboard",
                icon = Icons.Default.Dashboard,
                tabIndex = 0,
                currentTab = currentTab,
                testTag = "nav_tab_dashboard",
                onTabSelected = onTabSelected
            )
            AppBottomBarTab(
                title = "My Plot",
                icon = Icons.Default.Explore,
                tabIndex = 1,
                currentTab = currentTab,
                testTag = "nav_tab_planner",
                onTabSelected = onTabSelected,
                onGloballyPositioned = { coordinates ->
                    val rect = coordinates.boundsInRoot()
                    onWalkthroughTargetUpdate(WalkthroughStep.PLANNER_TAB, ScreenRect(rect.left, rect.top, rect.right, rect.bottom))
                }
            )
            AppBottomBarTab(
                title = "Greenhouse",
                icon = Icons.Default.Spa,
                tabIndex = 2,
                currentTab = currentTab,
                testTag = "nav_tab_greenhouse",
                onTabSelected = onTabSelected
            )
            AppBottomBarTab(
                title = "Counsel",
                icon = Icons.Default.SmartToy,
                tabIndex = 3,
                currentTab = currentTab,
                testTag = "nav_tab_ai",
                onTabSelected = onTabSelected,
                onGloballyPositioned = { coordinates ->
                    val rect = coordinates.boundsInRoot()
                    onWalkthroughTargetUpdate(WalkthroughStep.AI_ADVISOR_TAB, ScreenRect(rect.left, rect.top, rect.right, rect.bottom))
                }
            )
            AppBottomBarTab(
                title = "Restoration",
                icon = Icons.Default.SelfImprovement,
                tabIndex = 4,
                currentTab = currentTab,
                testTag = "nav_tab_ar",
                onTabSelected = onTabSelected,
                onGloballyPositioned = { coordinates ->
                    val rect = coordinates.boundsInRoot()
                    onWalkthroughTargetUpdate(WalkthroughStep.AR_LENS_TAB, ScreenRect(rect.left, rect.top, rect.right, rect.bottom))
                }
            )
        }
    }
}
