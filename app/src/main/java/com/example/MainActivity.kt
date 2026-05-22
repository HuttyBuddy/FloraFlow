package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GardenViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Direct instantiation of shared ViewModel
        val viewModel = GardenViewModel(this.application)

        setContent {
            MyApplicationTheme {
                var currentTab by remember { mutableStateOf(0) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
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
                                onClick = { currentTab = 4 },
                                icon = { Icon(Icons.Default.Videocam, contentDescription = "AR Lens") },
                                label = { Text("AR Lens", style = MaterialTheme.typography.bodySmall) },
                                modifier = Modifier.testTag("nav_tab_ar")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
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
