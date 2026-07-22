package com.example.ui.screens.restorativevalidation

import android.content.ActivityNotFoundException
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
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

        composeRule.onNodeWithContentDescription("Back").performClick()
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
        composeRule.onNodeWithContentDescription("Close").performClick()
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

    @Test
    fun `research disclosure names included excluded and requires consent`() {
        val intents = mutableListOf<RestorativeIntent>()
        val state = savedState().copy(
            researchExport = ResearchExportState(disclosureVisible = true),
        )
        composeRule.setContent {
            MyApplicationTheme { RestorativeValidationScreen(state, {}, intents::add) }
        }

        composeRule.onNodeWithText("Export test record").assertIsDisplayed()
        composeRule.onNodeWithText("Included").assertExists()
        composeRule.onNodeWithText("Not included").assertExists()
        composeRule.onNodeWithText("Choose destination").assertIsNotEnabled()
        composeRule.onNodeWithText("The participant has agreed to this export.").performScrollTo().performClick()

        assertEquals(listOf(RestorativeIntent.SetExportConsent(true)), intents)
    }

    @Test
    fun `export disclosure actions remain reachable at two hundred percent text`() {
        val state = savedState().copy(
            researchExport = ResearchExportState(
                disclosureVisible = true,
                consentConfirmed = true,
            ),
        )
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 2f)) {
                MyApplicationTheme { RestorativeValidationScreen(state, {}, {}) }
            }
        }

        composeRule.onNodeWithText("Choose destination").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `ready export launches share once and reports only options opened`() {
        val intents = mutableListOf<RestorativeIntent>()
        val payloads = mutableListOf<String>()
        val state = savedState().copy(
            researchExport = ResearchExportState(
                status = ResearchExportStatus.READY_TO_SHARE,
                preparedJson = "{\"safe\":true}",
            ),
        )
        composeRule.setContent {
            MyApplicationTheme {
                RestorativeValidationScreen(
                    state = state,
                    onStart = {},
                    onIntent = intents::add,
                    launchShare = payloads::add,
                )
            }
        }
        composeRule.waitForIdle()

        assertEquals(listOf("{\"safe\":true}"), payloads)
        assertEquals(listOf(RestorationExportScreenIntent.shareOpened), intents)
    }

    @Test
    fun `unavailable share target requests document fallback`() {
        val intents = mutableListOf<RestorativeIntent>()
        val state = savedState().copy(
            researchExport = ResearchExportState(
                status = ResearchExportStatus.READY_TO_SHARE,
                preparedJson = "{\"safe\":true}",
            ),
        )
        composeRule.setContent {
            MyApplicationTheme {
                RestorativeValidationScreen(
                    state = state,
                    onStart = {},
                    onIntent = intents::add,
                    launchShare = { throw ActivityNotFoundException() },
                )
            }
        }
        composeRule.waitForIdle()

        assertEquals(
            listOf(RestorativeIntent.ShareLaunchFailed("SHARE_TARGET_UNAVAILABLE")),
            intents,
        )
    }

    @Test
    fun `document fallback launches with stable json filename`() {
        val filenames = mutableListOf<String>()
        val state = savedState().copy(
            researchExport = ResearchExportState(status = ResearchExportStatus.AWAITING_DOCUMENT),
        )
        composeRule.setContent {
            MyApplicationTheme {
                RestorativeValidationScreen(
                    state = state,
                    onStart = {},
                    onIntent = {},
                    launchCreateDocument = filenames::add,
                )
            }
        }
        composeRule.waitForIdle()

        assertEquals(listOf("floraflow-test-record.json"), filenames)
    }

    private fun savedState(): RestorativeUiState {
        val plan = RestorativeRecommendationEngine.createPlan(
            light = LightChoice.BRIGHT,
            availableSpace = AvailableSpace.TABLETOP,
            ownedPlantSlugs = emptyList(),
        ) as RecommendationResult.Match
        return RestorativeUiState(
            step = RestorativeStep.SAVED,
            draft = RestorativeDraft(
                experimentId = "experiment-1",
                light = LightChoice.BRIGHT,
                availableSpace = AvailableSpace.TABLETOP,
                inputMode = InputMode.RECOMMEND_FROM_SCRATCH,
            ),
            plan = plan.plan,
        )
    }

    private object RestorationIntentAliasForTest {
        val back = RestorativeIntent.Back
    }

    private object RestorationExportScreenIntent {
        val shareOpened = RestorativeIntent.ShareLaunchSucceeded
    }
}
