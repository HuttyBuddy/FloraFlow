package com.example.ui.screens.restorativevalidation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class RestorativeValidationScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `visible back dispatches the typed back intent`() {
        val intents = mutableListOf<RestorativeIntent>()
        val state = RestorativeUiState(
            step = RestorativeStep.SPACE,
            draft = RestorativeDraft(experimentId = "experiment-1", light = LightChoice.MEDIUM),
        )
        composeRule.setContent {
            MyApplicationTheme {
                RestorativeValidationScreen(state = state, onStart = {}, onIntent = intents::add)
            }
        }

        composeRule.onNodeWithText("Back").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(RestorationIntentAliasForTest.back), intents)
    }

    @Test
    fun `meaningful draft close explains save discard and cancel choices`() {
        val state = RestorativeUiState(
            step = RestorativeStep.LIGHT,
            draft = RestorativeDraft(experimentId = "experiment-1"),
        )
        composeRule.setContent {
            MyApplicationTheme { RestorativeValidationScreen(state, {}, {}) }
        }

        composeRule.onNodeWithText("Choose one option above to continue.").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Close").performClick()
        composeRule.onNodeWithText("Save draft and exit").assertIsDisplayed()
        composeRule.onNodeWithText("Discard").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun `terminal evidence handoff cannot restart or discard`() {
        val state = RestorativeUiState(
            error = RestorativeError.TerminalEvidenceFailure("JOURNEY_MALFORMED"),
        )
        composeRule.setContent {
            MyApplicationTheme { RestorativeValidationScreen(state, {}, {}) }
        }

        composeRule.onNodeWithText("Please hand the phone back to the researcher.").assertIsDisplayed()
        composeRule.onNodeWithText("Start my corner").assertDoesNotExist()
        composeRule.onNodeWithText("Discard").assertDoesNotExist()
        composeRule.onNodeWithText("Show researcher details").performClick()
        composeRule.onNodeWithText("Local code: JOURNEY_MALFORMED").assertIsDisplayed()
    }

    @Test
    fun `saved return emits deliberate next step and placement actions`() {
        val intents = mutableListOf<RestorativeIntent>()
        val plan = RestorativeRecommendationEngine.createPlan(
            light = LightChoice.BRIGHT,
            availableSpace = AvailableSpace.TABLETOP,
            ownedPlantSlugs = emptyList(),
        ) as RecommendationResult.Match
        val state = RestorativeUiState(
            step = RestorativeStep.SAVED,
            draft = RestorativeDraft(experimentId = "experiment-1"),
            plan = plan.plan,
            isReturnWithinWindow = true,
        )
        composeRule.setContent {
            MyApplicationTheme { RestorativeValidationScreen(state, {}, intents::add) }
        }

        composeRule.onNodeWithText("Move a chair into the corner").performScrollTo().performClick()
        composeRule.onNodeWithText("View placement guidance").performScrollTo().performClick()

        assertEquals(
            listOf(
                RestorativeIntent.SelectNextStep("Move a chair into the corner"),
                RestorativeIntent.OpenPlacementGuidance,
            ),
            intents,
        )
    }

    private object RestorationIntentAliasForTest {
        val back = RestorativeIntent.Back
    }
}
