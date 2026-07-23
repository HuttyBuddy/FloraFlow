package com.example.ui.screens.dashboard

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ui.screens.restorativevalidation.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GardenViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class RestorativeAssessmentFixTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: GardenViewModel

    @Before
    fun setup() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        viewModel = GardenViewModel(application)
    }

    @Test
    fun calculateScore_generatesDynamicScoreFromDraftAnswers() {
        val brightDraft = RestorativeDraft(
            light = LightChoice.BRIGHT,
            availableSpace = AvailableSpace.OPEN_CORNER,
            ownedPlantSlugs = listOf("peace-lily", "monstera")
        )
        val brightScore = RestorativeRecommendationEngine.calculateScore(brightDraft)
        assertEquals(20, brightScore)

        val lowDraft = RestorativeDraft(
            light = LightChoice.LOW,
            availableSpace = AvailableSpace.TABLETOP,
            ownedPlantSlugs = emptyList()
        )
        val lowScore = RestorativeRecommendationEngine.calculateScore(lowDraft)
        assertEquals(14, lowScore)
    }

    @Test
    fun placementGuidance_togglesOpenAndClosed() {
        var state = RestorativeUiState(step = RestorativeStep.SAVED, placementGuidanceExpanded = false)
        val openIntentState = RestorativeReducer.reduce(state, RestorativeIntent.OpenPlacementGuidance)
        assertTrue(openIntentState.placementGuidanceExpanded)

        val closeIntentState = RestorativeReducer.reduce(openIntentState, RestorativeIntent.OpenPlacementGuidance)
        assertFalse(closeIntentState.placementGuidanceExpanded)
    }

    @Test
    fun biophilicProfileCard_rendersUnassessedStateWhenScoreZeroOrNull() {
        composeTestRule.setContent {
            MyApplicationTheme {
                BiophilicProfileCard(
                    score = 0,
                    lowestCategories = emptyList(),
                    onRetakeClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Not Assessed").assertIsDisplayed()
    }

    @Test
    fun saveAssessmentResult_updatesViewModelScoreAndState() {
        viewModel.saveAssessmentResult(18, listOf("Natural Daylight"))
        assertEquals(18, viewModel.assessmentScore.value)
    }

    @Test
    fun resetAndStartNewAssessment_resetsStateForRetake() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val valViewModel = RestorativeValidationViewModel.factory(application).create(RestorativeValidationViewModel::class.java)
        
        valViewModel.resetAndStartNewAssessment()
        val state = valViewModel.uiState.value
        
        assertFalse(state.exitRequested)
        assertEquals(RestorativeStep.PROMISE, state.step)
    }
}
