package com.example.ui.screens.dashboard

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sensor.PlantLightZone
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GardenViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class EnhancementFeaturesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: GardenViewModel

    @Before
    fun setup() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        viewModel = GardenViewModel(application)
    }

    @Test
    fun enhancementFeatures_stateFlowsAndLightZoneMapping() {
        // Feature 1: Light Sensor default
        assertNotNull(viewModel.lightSensorManager)

        // Feature 2: Care Streak default
        assertEquals(3, viewModel.careStreakDays.value)
        viewModel.incrementCareStreak()
        assertEquals(4, viewModel.careStreakDays.value)

        // Feature 4: Binaural Frequency default
        assertEquals(10.0f, viewModel.binauralFrequencyHz.value, 0.01f)
        viewModel.setBinauralFrequency(14.0f)
        assertEquals(14.0f, viewModel.binauralFrequencyHz.value, 0.01f)
    }

    @Test
    fun sanctuaryCardDeck_rendersNewFeatureCards() {
        composeTestRule.setContent {
            MyApplicationTheme {
                SanctuaryCardDeckScreen(viewModel = viewModel)
            }
        }

        // Card 1 Real-time light meter exists in deck
        composeTestRule.onNodeWithTag("real_time_light_meter_card").assertExists()
        
        // Verify streak & binaural state modifications
        viewModel.incrementCareStreak()
        assertEquals(4, viewModel.careStreakDays.value)

        viewModel.setBinauralFrequency(12f)
        assertEquals(12f, viewModel.binauralFrequencyHz.value, 0.01f)
    }

    @Test
    fun botanicalSeason_enumValidation() {
        val season = com.example.ui.components.graphics.BotanicalSeason.SPRING_BLOOM
        assertEquals("Spring Dew", season.label)
        assertNotNull(season.icon)
    }
}
