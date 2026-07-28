package com.example.ui.screens.dashboard

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GardenViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class SanctuaryCardDeckScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: GardenViewModel

    @Before
    fun setup() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        viewModel = GardenViewModel(application)
    }

    @Test
    fun sanctuaryDashboard_displaysCoreScreenAsOneScroll() {
        composeTestRule.setContent {
            MyApplicationTheme {
                SanctuaryCardDeckScreen(viewModel = viewModel)
            }
        }

        // The three-page swipe deck was replaced by a single vertical scroll, so the
        // whole dashboard is reachable without discovering a horizontal gesture.
        composeTestRule.onNodeWithTag("sanctuary_card_deck_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("sanctuary_dashboard_scroll").assertExists()
        composeTestRule.onNodeWithTag("sanctuary_card_pager").assertDoesNotExist()
    }

    /** The profile card is the dashboard's anchor and must render without a swipe. */
    @Test
    fun sanctuaryDashboard_showsProfileCardWithoutSwiping() {
        composeTestRule.setContent {
            MyApplicationTheme {
                SanctuaryCardDeckScreen(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithTag("biophilic_profile_card").assertExists()
    }

    /**
     * The dashboard no longer carries its own AI entry point, weather, companion or
     * manual streak widgets — Counsel is a tab of its own, and the streak is derived.
     */
    @Test
    fun sanctuaryDashboard_doesNotDuplicateOtherTabs() {
        composeTestRule.setContent {
            MyApplicationTheme {
                SanctuaryCardDeckScreen(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithTag("ai_counsel_fab").assertDoesNotExist()
        composeTestRule.onNodeWithTag("care_streak_card").assertDoesNotExist()
    }

    @Test
    fun sanctuaryCardDeck_launchesSettingsDialog() {
        composeTestRule.setContent {
            MyApplicationTheme {
                SanctuaryCardDeckScreen(viewModel = viewModel)
            }
        }

        // Click settings gear button
        composeTestRule.onNodeWithTag("deck_settings_btn").performClick()
        composeTestRule.onNodeWithTag("settings_dialog").assertIsDisplayed()
    }

}
