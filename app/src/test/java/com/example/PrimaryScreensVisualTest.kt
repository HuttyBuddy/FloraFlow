package com.example

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GardenViewModel
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [32])
class PrimaryScreensVisualTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: GardenViewModel

    @Before
    fun setUp() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        viewModel = GardenViewModel(application)
    }

    @Test
    fun testOnboardingScreenVisual() {
        composeTestRule.setContent {
            MyApplicationTheme {
                OnboardingScreen(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/onboarding_screen.png")
    }

    @Test
    fun testDashboardScreenVisual() {
        viewModel.completeOnboarding()
        composeTestRule.setContent {
            MyApplicationTheme {
                DashboardScreen(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/dashboard_screen.png")
    }

    @Test
    fun testPlannerScreenVisual() {
        viewModel.completeOnboarding()
        composeTestRule.setContent {
            MyApplicationTheme {
                PlannerScreen(viewModel = viewModel, switchToChatTab = {})
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/planner_screen.png")
    }

    @Test
    fun testLibraryScreenVisual() {
        viewModel.completeOnboarding()
        composeTestRule.setContent {
            MyApplicationTheme {
                LibraryScreen(viewModel = viewModel, switchToChatTab = {})
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/library_screen.png")
    }

    @Test
    fun testAiStudioScreenVisual() {
        viewModel.completeOnboarding()
        composeTestRule.setContent {
            MyApplicationTheme {
                AiStudioScreen(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/ai_studio_screen.png")
    }

    @Test
    fun testArLensScreenVisual() {
        viewModel.completeOnboarding()
        viewModel.restorePurchases()
        composeTestRule.setContent {
            MyApplicationTheme {
                ArLensScreen(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/ar_lens_screen.png")
    }
}
