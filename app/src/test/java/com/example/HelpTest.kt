package com.example

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.help.HelpDialog
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
class HelpTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: GardenViewModel

    @Before
    fun setUp() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        viewModel = GardenViewModel(application)
    }

    @Test
    fun testHelpDialogRenderingAndTabs() {
        composeTestRule.setContent {
            MyApplicationTheme {
                HelpDialog(
                    visible = true,
                    onDismiss = {},
                    viewModel = viewModel
                )
            }
        }
        composeTestRule.waitForIdle()

        // Verify Help Dialog is shown
        composeTestRule.onNodeWithTag("help_dialog_card").assertExists()

        // Capture initial screenshot
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/help_dialog_faqs.png")

        // Check if search exists
        composeTestRule.onNodeWithTag("faq_search_input").assertExists()

        // Switch to Video Tutorials tab
        composeTestRule.onNodeWithTag("help_tab_tutorials").performClick()
        composeTestRule.waitForIdle()

        // Verify tutorial videos are shown
        composeTestRule.onNodeWithTag("video_card_1").assertExists()
        composeTestRule.onNodeWithTag("video_card_2").assertExists()

        // Capture tutorials screenshot
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/help_dialog_tutorials.png")
    }

    @Test
    fun testFaqFilteringAndSearch() {
        composeTestRule.setContent {
            MyApplicationTheme {
                HelpDialog(
                    visible = true,
                    onDismiss = {},
                    viewModel = viewModel
                )
            }
        }
        composeTestRule.waitForIdle()

        // Expand first FAQ item
        composeTestRule.onNodeWithTag("faq_card_1").performClick()
        composeTestRule.waitForIdle()

        // Search for specific word e.g. "AR"
        composeTestRule.onNodeWithTag("faq_search_input").performTextInput("AR Lens")
        composeTestRule.waitForIdle()

        // Verify only AR related card is visible (card 3 is AR Lens FAQ)
        composeTestRule.onNodeWithTag("faq_card_3").assertExists()
        composeTestRule.onNodeWithTag("faq_card_1").assertDoesNotExist()

        // Filter by category chip e.g. "AI Advisor"
        composeTestRule.onNodeWithTag("faq_search_input").performTextClearance()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("faq_chip_AI Advisor").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Verify AI Advisor faq (id = 2) is visible and others are not
        composeTestRule.onNodeWithTag("faq_card_2").performScrollTo().assertExists()
        composeTestRule.onNodeWithTag("faq_card_1").assertDoesNotExist()
    }

    @Test
    fun testVideoPlayerLifecycle() {
        composeTestRule.setContent {
            MyApplicationTheme {
                HelpDialog(
                    visible = true,
                    onDismiss = {},
                    viewModel = viewModel
                )
            }
        }
        composeTestRule.waitForIdle()

        // Switch to Video Tutorials tab
        composeTestRule.onNodeWithTag("help_tab_tutorials").performClick()
        composeTestRule.waitForIdle()

        // Click first video card to launch player
        composeTestRule.onNodeWithTag("video_card_1").performClick()
        composeTestRule.waitForIdle()

        // Verify video player dialog is open
        composeTestRule.onNodeWithTag("video_player_dialog").assertExists()

        // Click close on the player
        composeTestRule.onNodeWithTag("video_player_close").performClick()
        composeTestRule.waitForIdle()

        // Verify player is closed
        composeTestRule.onNodeWithTag("video_player_dialog").assertDoesNotExist()
    }
}
