package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import com.example.ui.screens.settings.SettingsDialog
import com.example.ui.screens.community.CommunityDialog
import com.example.ui.screens.settings.InAppRatePromptDialog
import com.example.ui.screens.walkthrough.WalkthroughOverlay
import com.example.ui.screens.help.HelpDialog
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import com.example.ui.viewmodel.WalkthroughStep
import com.example.ui.viewmodel.ScreenRect
import androidx.compose.material3.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import com.example.ui.screens.restoration.RestorationJournalScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.feedback.FeedbackDialog
import androidx.activity.viewModels
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GardenViewModel
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit
import com.example.ui.screens.dashboard.CareSyncWorker

class MainActivity : ComponentActivity() {
    private val viewModel: GardenViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

            val statusBarStyle = if (useDarkTheme) {
                androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
            }
            val navigationBarStyle = if (useDarkTheme) {
                androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
            }

            DisposableEffect(useDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = statusBarStyle,
                    navigationBarStyle = navigationBarStyle
                )
                onDispose {}
            }

            MyApplicationTheme(darkTheme = useDarkTheme) {
                var showSplash by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    showSplash = false
                }

                if (showSplash) {
                    SplashWarmUpScreen()
                } else {
                    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()

                    if (!isOnboardingCompleted) {
                        OnboardingScreen(viewModel = viewModel)
                    } else {
                        val currentTab by viewModel.currentTab.collectAsState()
                        var showFeedbackDialog by remember { mutableStateOf(false) }
                        var showSettingsDialog by remember { mutableStateOf(false) }
                        var showCommunityDialog by remember { mutableStateOf(false) }
                        var showHelpDialog by remember { mutableStateOf(false) }
                        val isPremium by viewModel.isPremium.collectAsState()

                        // Universal sandbox Billing & Subscription Management Checkout Dialog
                        BillingDialog(viewModel = viewModel)

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
                                @OptIn(ExperimentalMaterial3Api::class)
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
                                            Spacer(modifier = Modifier.width(8.dp))
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
                                                    viewModel.setSubscriptionManagementVisible(true)
                                                } else {
                                                    viewModel.upgradeToPremium()
                                                }
                                            },
                                            modifier = Modifier.testTag("premium_key_status_crown")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.WorkspacePremium,
                                                contentDescription = "Subscription Info Status",
                                                tint = if (isPremium) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        IconButton(
                                            onClick = { showCommunityDialog = true },
                                            modifier = Modifier.testTag("community_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Forum,
                                                contentDescription = "Community Forum"
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
                                                        showFeedbackDialog = true
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
                                                        showHelpDialog = true
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
                                                        showSettingsDialog = true
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
                                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            },
                            bottomBar = {
                                val density = androidx.compose.ui.platform.LocalDensity.current
                                val labelFontSize = remember(density) { with(density) { 8.5.dp.toSp() } }
                                val uniformTextStyle = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = labelFontSize,
                                    letterSpacing = 0.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                
                                val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                                val screenWidth = configuration.screenWidthDp.dp
                                val tabWidth = screenWidth / 5
                                val indicatorOffset by androidx.compose.animation.core.animateDpAsState(
                                    targetValue = tabWidth * currentTab,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                    label = "tabIndicatorOffset"
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(72.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .navigationBarsPadding()
                                        .testTag("app_navigation_bar")
                                ) {
                                    // Sliding indicator pill
                                    Box(
                                        modifier = Modifier
                                            .padding(vertical = 10.dp, horizontal = 6.dp)
                                            .width(tabWidth - 12.dp)
                                            .fillMaxHeight()
                                            .offset(x = indicatorOffset)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Tab 0: Dashboard
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable(
                                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    viewModel.setCurrentTab(0)
                                                }
                                                .testTag("nav_tab_dashboard"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val isSelected = currentTab == 0
                                            val scale by animateFloatAsState(targetValue = if (isSelected) 1.06f else 1.0f, label = "tabScale0")
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier.scale(scale)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Dashboard,
                                                    contentDescription = "Dashboard",
                                                    modifier = Modifier.size(22.dp),
                                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Dashboard",
                                                    style = uniformTextStyle,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }

                                        // Tab 1: My Plot
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable(
                                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    viewModel.setCurrentTab(1)
                                                }
                                                .testTag("nav_tab_planner")
                                                .onGloballyPositioned { coordinates ->
                                                    val rect = coordinates.boundsInRoot()
                                                    viewModel.updateWalkthroughTarget(
                                                        WalkthroughStep.PLANNER_TAB,
                                                        ScreenRect(rect.left, rect.top, rect.right, rect.bottom)
                                                    )
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val isSelected = currentTab == 1
                                            val scale by animateFloatAsState(targetValue = if (isSelected) 1.06f else 1.0f, label = "tabScale1")
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier.scale(scale)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Explore,
                                                    contentDescription = "My Plot",
                                                    modifier = Modifier.size(22.dp),
                                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "My Plot",
                                                    style = uniformTextStyle,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }

                                        // Tab 2: Greenhouse
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable(
                                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    viewModel.setCurrentTab(2)
                                                }
                                                .testTag("nav_tab_greenhouse"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val isSelected = currentTab == 2
                                            val scale by animateFloatAsState(targetValue = if (isSelected) 1.06f else 1.0f, label = "tabScale2")
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier.scale(scale)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Spa,
                                                    contentDescription = "Greenhouse",
                                                    modifier = Modifier.size(22.dp),
                                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Greenhouse",
                                                    style = uniformTextStyle,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }

                                        // Tab 3: Garden Counsel
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable(
                                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    viewModel.setCurrentTab(3)
                                                }
                                                .testTag("nav_tab_ai")
                                                .onGloballyPositioned { coordinates ->
                                                    val rect = coordinates.boundsInRoot()
                                                    viewModel.updateWalkthroughTarget(
                                                        WalkthroughStep.AI_ADVISOR_TAB,
                                                        ScreenRect(rect.left, rect.top, rect.right, rect.bottom)
                                                    )
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val isSelected = currentTab == 3
                                            val scale by animateFloatAsState(targetValue = if (isSelected) 1.06f else 1.0f, label = "tabScale3")
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier.scale(scale)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.SmartToy,
                                                    contentDescription = "Garden Counsel",
                                                    modifier = Modifier.size(22.dp),
                                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Garden Counsel",
                                                    style = uniformTextStyle,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }

                                        // Tab 4: Restoration
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable(
                                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    viewModel.setCurrentTab(4)
                                                }
                                                .testTag("nav_tab_ar")
                                                .onGloballyPositioned { coordinates ->
                                                    val rect = coordinates.boundsInRoot()
                                                    viewModel.updateWalkthroughTarget(
                                                        WalkthroughStep.AR_LENS_TAB,
                                                        ScreenRect(rect.left, rect.top, rect.right, rect.bottom)
                                                    )
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val isSelected = currentTab == 4
                                            val scale by animateFloatAsState(targetValue = if (isSelected) 1.06f else 1.0f, label = "tabScale4")
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier.scale(scale)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.SelfImprovement,
                                                    contentDescription = "Restoration",
                                                    modifier = Modifier.size(22.dp),
                                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Restoration",
                                                    style = uniformTextStyle,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }
                                }
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
                                        viewModel = viewModel,
                                        onCommunityClick = { showCommunityDialog = true }
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

                        CommunityDialog(
                            visible = showCommunityDialog,
                            onDismiss = { showCommunityDialog = false },
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
}

@Composable
fun SplashWarmUpScreen() {
    var startAnimation by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = 0.6f, // Nice slightly bouncy bloom effect
            stiffness = Spring.StiffnessMedium
        ),
        label = "logoScale"
    )
    
    LaunchedEffect(Unit) {
        startAnimation = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Elegant pulsing organic icon container
            Image(
                painter = painterResource(id = R.drawable.ic_logo_heart),
                contentDescription = "FloraFlow Logo",
                modifier = Modifier
                    .size(110.dp)
                    .scale(scale)
                    .clip(RoundedCornerShape(28.dp))
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "FloraFlow",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Cultivating Calm through Mindful Gardening",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Syncing botanical resources...",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
