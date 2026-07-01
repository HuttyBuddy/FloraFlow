package com.example.ui.screens.settings

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.GardenViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class SettingsDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: GardenViewModel

    @Before
    fun setUp() {
        org.robolectric.shadows.ShadowLog.stream = System.out
        val app = ApplicationProvider.getApplicationContext<Application>()
        // Ensure clean state
        val sharedPrefs = app.getSharedPreferences("floraflow_billing_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().clear().apply()
        viewModel = GardenViewModel(app)
    }

    @Test
    fun testSimulate30DaysToggleUpdatesText() {

        composeTestRule.setContent {
            com.example.ui.theme.MyApplicationTheme {
                SettingsDialog(
                    visible = true,
                    onDismiss = {},
                    onFeedbackClick = {},
                    onHelpClick = {},
                    viewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithTag("settings_dialog").assertExists()


        // Initially it should say "Simulate 30 Days Elapsed"
        composeTestRule.onNodeWithText("Simulate 30 Days Elapsed", useUnmergedTree = true).assertExists()

        composeTestRule.waitForIdle()
        // Click the toggle to enable 30-day simulation
        composeTestRule.onNodeWithText("Simulate 30 Days Elapsed", useUnmergedTree = true).performScrollTo()
        composeTestRule.onNodeWithText("Simulate 30 Days Elapsed", useUnmergedTree = true).performClick()

        composeTestRule.waitForIdle()
        // After click, the text should update to "Disable 30-Day Simulation"
        composeTestRule.onNodeWithText("Disable 30-Day Simulation", useUnmergedTree = true).assertExists()

        composeTestRule.waitForIdle()
        // Click the toggle again to disable 30-day simulation
        composeTestRule.onNodeWithText("Disable 30-Day Simulation", useUnmergedTree = true).performScrollTo()
        composeTestRule.onNodeWithText("Disable 30-Day Simulation", useUnmergedTree = true).performClick()

        composeTestRule.waitForIdle()
        // Text should revert to original state
        composeTestRule.onNodeWithText("Simulate 30 Days Elapsed", useUnmergedTree = true).assertExists()
    }
}
