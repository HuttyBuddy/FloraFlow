package com.example.ui.screens.dashboard

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BiophilicProfileCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testRetakeAssessmentButtonClicks() {
        var clicked = false
        composeTestRule.setContent {
            BiophilicProfileCard(
                score = 10,
                lowestCategories = listOf("NATURE VIEWS"),
                onRetakeClick = { clicked = true }
            )
        }

        composeTestRule.onNodeWithTag("biophilic_retake_assessment_btn").performClick()
        assertTrue(clicked)
    }

    @Test
    fun testStepCheckboxesToggle() {
        var toggledIndex = -1
        composeTestRule.setContent {
            BiophilicProfileCard(
                score = 5,
                lowestCategories = listOf("NATURE VIEWS", "LIVING PLANTS", "NATURAL LIGHT"),
                onRetakeClick = {},
                onStepToggle = { index -> toggledIndex = index }
            )
        }

        composeTestRule.onNodeWithTag("step_checkbox_1").performClick()
        assertEquals(1, toggledIndex)

        composeTestRule.onNodeWithTag("step_checkbox_2").performClick()
        assertEquals(2, toggledIndex)

        composeTestRule.onNodeWithTag("step_checkbox_3").performClick()
        assertEquals(3, toggledIndex)
    }

    @Test
    fun testRedZoneShowsForLowScore() {
        composeTestRule.setContent {
            BiophilicProfileCard(
                score = 5,
                lowestCategories = emptyList(),
                onRetakeClick = {}
            )
        }

        composeTestRule.onNodeWithText("Red Zone — Active State").assertExists()
        composeTestRule.onNodeWithText("Lack of biophilic elements may increase sensory stress.").assertExists()
    }

    @Test
    fun testYellowZoneShowsForMediumScore() {
        composeTestRule.setContent {
            BiophilicProfileCard(
                score = 12,
                lowestCategories = emptyList(),
                onRetakeClick = {}
            )
        }

        composeTestRule.onNodeWithText("Yellow Zone — Active State").assertExists()
        composeTestRule.onNodeWithText("A few elements are missing, quietly draining focus/energy.").assertExists()
    }

    @Test
    fun testGreenZoneShowsForHighScore() {
        composeTestRule.setContent {
            BiophilicProfileCard(
                score = 18,
                lowestCategories = emptyList(),
                onRetakeClick = {}
            )
        }

        composeTestRule.onNodeWithText("Green Zone — Active State").assertExists()
        composeTestRule.onNodeWithText("Environment supports nervous system calm and natural focus.").assertExists()
    }

    @Test
    fun testChecklistTitleVisibleWithCategories() {
        composeTestRule.setContent {
            BiophilicProfileCard(
                score = 10,
                lowestCategories = listOf("NATURE VIEWS"),
                onRetakeClick = {}
            )
        }

        composeTestRule.onNodeWithText("Personalized Next Steps Checklist").assertExists()
    }
}
