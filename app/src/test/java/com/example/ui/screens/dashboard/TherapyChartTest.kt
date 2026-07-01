package com.example.ui.screens.dashboard

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import com.example.data.model.MoodLog
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TherapyChartTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun therapyChart_displaysLegend_whenLogsPresent() {
        val sampleLogs = listOf(
            MoodLog(mood = "Happy", moodScore = 5, activityMinutes = 30, growthIndex = 80)
        )

        composeTestRule.setContent {
            InteractiveTherapyChart(logs = sampleLogs)
        }

        composeTestRule.onNodeWithText("Therapy Rating").assertIsDisplayed()
        composeTestRule.onNodeWithText("Garden Growth Index").assertIsDisplayed()
    }

    @Test
    fun therapyChart_doesNotDisplayLegend_whenLogsEmpty() {
        composeTestRule.setContent {
            InteractiveTherapyChart(logs = emptyList())
        }

        composeTestRule.onNodeWithText("Therapy Rating").assertDoesNotExist()
        composeTestRule.onNodeWithText("Garden Growth Index").assertDoesNotExist()
    }
}
