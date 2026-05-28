package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import androidx.activity.viewModels
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GardenViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: GardenViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkThemeOverridden by viewModel.isDarkTheme.collectAsState()
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val useDarkTheme = isDarkThemeOverridden ?: systemDark

            MyApplicationTheme(darkTheme = useDarkTheme) {
                var showSplash by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(1000) // Defer heavy layout loading with 1 second of warm organic splash/load timer
                    showSplash = false
                }

                if (showSplash) {
                    SplashWarmUpScreen()
                } else {
                    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()

                    if (!isOnboardingCompleted) {
                        OnboardingScreen(viewModel = viewModel)
                    } else {
                    var currentTab by remember { mutableStateOf(0) }
                    val isPremium by viewModel.isPremium.collectAsState()

                    // Universal sandbox Billing & Subscription Management Checkout Dialog
                    BillingDialog(viewModel = viewModel)

                    val showSubscriptionManagement by viewModel.showSubscriptionManagement.collectAsState()
                    SubscriptionManagementDialog(
                        visible = showSubscriptionManagement,
                        onDismiss = { viewModel.setSubscriptionManagementVisible(false) },
                        viewModel = viewModel
                    )

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            @OptIn(ExperimentalMaterial3Api::class)
                            CenterAlignedTopAppBar(
                                title = {
                                    Text(
                                        text = "FloraFlow",
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
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
                                        onClick = { viewModel.toggleTheme(systemDark) },
                                        modifier = Modifier.testTag("theme_toggle_button")
                                    ) {
                                        Icon(
                                            imageVector = if (useDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                            contentDescription = "Toggle Light/Dark Theme"
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        },
                        bottomBar = {
                            NavigationBar(
                                modifier = Modifier.testTag("app_navigation_bar"),
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = 8.dp
                            ) {
                                NavigationBarItem(
                                    selected = currentTab == 0,
                                    onClick = { currentTab = 0 },
                                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                                    label = { Text("Dashboard", style = MaterialTheme.typography.bodySmall) },
                                    modifier = Modifier.testTag("nav_tab_dashboard")
                                )
                                NavigationBarItem(
                                    selected = currentTab == 1,
                                    onClick = { currentTab = 1 },
                                    icon = { Icon(Icons.Default.Explore, contentDescription = "2D Planner") },
                                    label = { Text("2D Planner", style = MaterialTheme.typography.bodySmall) },
                                    modifier = Modifier.testTag("nav_tab_planner")
                                )
                                NavigationBarItem(
                                    selected = currentTab == 2,
                                    onClick = { currentTab = 2 },
                                    icon = { Icon(Icons.Default.Spa, contentDescription = "Greenhouse") },
                                    label = { Text("Greenhouse", style = MaterialTheme.typography.bodySmall) },
                                    modifier = Modifier.testTag("nav_tab_greenhouse")
                                )
                                NavigationBarItem(
                                    selected = currentTab == 3,
                                    onClick = { currentTab = 3 },
                                    icon = { Icon(Icons.Default.SmartToy, contentDescription = "AI Advisor") },
                                    label = { Text("AI Advisor", style = MaterialTheme.typography.bodySmall) },
                                    modifier = Modifier.testTag("nav_tab_ai")
                                )
                                NavigationBarItem(
                                    selected = currentTab == 4,
                                    onClick = {
                                        currentTab = 4
                                        if (!isPremium) {
                                            viewModel.upgradeToPremium()
                                        }
                                    },
                                    icon = { Icon(Icons.Default.Videocam, contentDescription = "AR Lens") },
                                    label = { Text("AR Lens", style = MaterialTheme.typography.bodySmall) },
                                    modifier = Modifier.testTag("nav_tab_ar")
                                )
                            }
                        }
                    ) { innerPadding ->
                        val safeBottomPadding = if (innerPadding.calculateBottomPadding() < 90.dp) 90.dp else innerPadding.calculateBottomPadding() + 12.dp
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
                                    switchToChatTab = { currentTab = 3 }
                                )
                                2 -> LibraryScreen(
                                    viewModel = viewModel,
                                    switchToChatTab = { currentTab = 3 }
                                )
                                3 -> AiStudioScreen(
                                    viewModel = viewModel
                                )
                                4 -> ArLensScreen(
                                    viewModel = viewModel
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
fun SplashWarmUpScreen() {
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
            Surface(
                modifier = Modifier.size(110.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "🌿", fontSize = 54.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "FloraFlow",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Therapeutic Space & Advisor",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
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
