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
    fun testHelpDialogRendering() {
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

}
