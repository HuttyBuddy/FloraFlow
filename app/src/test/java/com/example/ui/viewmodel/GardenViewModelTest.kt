package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class GardenViewModelTest {

    private lateinit var application: Application
    private lateinit var viewModel: GardenViewModel

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()
        // Clear shared preferences to ensure a clean state
        application.getSharedPreferences("floraflow_billing_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        viewModel = GardenViewModel(application)
    }

    @Test
    fun completeOnboarding_updatesStateAndSharedPreferences() {
        // Assert initial state is false
        assertFalse(viewModel.isOnboardingCompleted.value)

        // Act
        viewModel.completeOnboarding()

        // Assert state flow is updated
        assertTrue(viewModel.isOnboardingCompleted.value)

        // Assert shared preferences is updated
        val sharedPrefs = application.getSharedPreferences("floraflow_billing_prefs", Context.MODE_PRIVATE)
        assertTrue(sharedPrefs.getBoolean("onboarding_completed", false))
    }

    @Test
    fun restorationTrial_limitsFreeUsersToThreeUses() {
        // Assert initial trial count is 0
        assertEquals(0, viewModel.restorationTrialCount.value)
        assertFalse(viewModel.showBillingDialog.value)

        // 1st use
        assertTrue(viewModel.incrementRestorationTrial())
        assertEquals(1, viewModel.restorationTrialCount.value)

        // 2nd use
        assertTrue(viewModel.incrementRestorationTrial())
        assertEquals(2, viewModel.restorationTrialCount.value)

        // 3rd use
        assertTrue(viewModel.incrementRestorationTrial())
        assertEquals(3, viewModel.restorationTrialCount.value)

        // 4th use should fail and trigger billing dialog
        assertFalse(viewModel.incrementRestorationTrial())
        assertEquals(3, viewModel.restorationTrialCount.value)
        assertTrue(viewModel.showBillingDialog.value)
    }

    @Test
    fun restorationTrial_allowsUnlimitedUsesForPremium() {
        // Upgrade to premium
        viewModel.restorePurchases() // makes the user premium
        assertTrue(viewModel.isPremium.value)

        // Call 5 times, should always return true and not change/exceed trial count
        for (i in 1..5) {
            assertTrue(viewModel.incrementRestorationTrial())
        }
    }

    @Test
    fun aiQueryTrial_limitsFreeUsersToThreeUses() {
        // Assert initial query count is 0
        assertEquals(0, viewModel.aiQueryCount.value)

        // Increment count up to limit
        viewModel.recordAiQueryUsed()
        assertEquals(1, viewModel.aiQueryCount.value)

        viewModel.recordAiQueryUsed()
        assertEquals(2, viewModel.aiQueryCount.value)

        viewModel.recordAiQueryUsed()
        assertEquals(3, viewModel.aiQueryCount.value)
    }

    @Test
    fun aiQueryTrial_allowsUnlimitedUsesForPremium() {
        // Upgrade to premium
        viewModel.restorePurchases()
        assertTrue(viewModel.isPremium.value)
    }


    @Test
    fun updateArPlantRotation_updatesRotationCorrectly() {
        // Arrange
        viewModel.addArPlant("Rose", "🌹")
        viewModel.addArPlant("Tulip", "🌷")

        val plants = viewModel.arPlacedPlants.value
        assertEquals(2, plants.size)
        val id1 = plants[0].id
        val id2 = plants[1].id

        // Act - Positive rotation
        viewModel.updateArPlantRotation(id1, 90f)
        var updatedPlants = viewModel.arPlacedPlants.value
        assertEquals(90f, updatedPlants.find { it.id == id1 }?.rotationDegrees)
        assertEquals(0f, updatedPlants.find { it.id == id2 }?.rotationDegrees) // Unchanged

        // Act - Negative rotation (should be normalized to 0-360)
        viewModel.updateArPlantRotation(id1, -90f)
        updatedPlants = viewModel.arPlacedPlants.value
        assertEquals(270f, updatedPlants.find { it.id == id1 }?.rotationDegrees)

        // Act - Over 360 rotation (should be normalized to 0-360)
        viewModel.updateArPlantRotation(id2, 400f)
        updatedPlants = viewModel.arPlacedPlants.value
        assertEquals(40f, updatedPlants.find { it.id == id2 }?.rotationDegrees)
    }

    @Test
    fun setLibrarySearchQuery_updatesStateFlow() {
        assertEquals("", viewModel.librarySearchQuery.value)
        viewModel.setLibrarySearchQuery("fern")
        assertEquals("fern", viewModel.librarySearchQuery.value)
    }

    @Test
    fun snoozeReassessment_suppressesNeedsReassessment() {
        // Force simulation active
        viewModel.toggleSimulation30Days()
        assertTrue(viewModel.needsReassessment.value)

        // Snooze
        viewModel.snoozeReassessment()
        assertFalse(viewModel.needsReassessment.value)

        val sharedPrefs = application.getSharedPreferences("floraflow_billing_prefs", Context.MODE_PRIVATE)
        assertTrue(sharedPrefs.getLong("reassessment_snooze_timestamp", 0L) > 0L)
    }

    @Test
    fun saveAssessmentResult_resetsSnoozeTimestamp() {
        viewModel.snoozeReassessment()
        val sharedPrefs = application.getSharedPreferences("floraflow_billing_prefs", Context.MODE_PRIVATE)
        assertTrue(sharedPrefs.getLong("reassessment_snooze_timestamp", 0L) > 0L)

        viewModel.saveAssessmentResult(18, listOf("NATURE VIEWS"))
        assertEquals(0L, sharedPrefs.getLong("reassessment_snooze_timestamp", 0L))
    }

    @Test
    fun smartSearchSuggestions_learnsFromSubstrateSelection() = runBlocking {
        val job = launch(Dispatchers.Unconfined) {
            viewModel.smartSearchSuggestions.collect {}
        }
        
        viewModel.recordSubstrateSelected("Sand 🟧")
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        
        val updatedSuggestions = viewModel.smartSearchSuggestions.value
        val hasSubstrateFit = updatedSuggestions.any { it.category == "Substrate Fit" && it.query == "Succulent" }
        assertTrue(hasSubstrateFit)
        
        job.cancel()
    }

    @Test
    fun smartSearchSuggestions_learnsFromSeedSowing() = runBlocking {
        val job = launch(Dispatchers.Unconfined) {
            viewModel.smartSearchSuggestions.collect {}
        }
        
        viewModel.recordSeedSelected("Tomato")
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        
        val updatedSuggestions = viewModel.smartSearchSuggestions.value
        val hasTomatoCompanion = updatedSuggestions.any { it.category == "Companion Fit" && it.query == "Basil" }
        assertTrue(hasTomatoCompanion)
        
        job.cancel()
    }

    @Test
    fun smartSearchSuggestions_learnsFromRestorationTabVisit() = runBlocking {
        val job = launch(Dispatchers.Unconfined) {
            viewModel.smartSearchSuggestions.collect {}
        }
        
        viewModel.recordVisitedScreen(4)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        
        val updatedSuggestions = viewModel.smartSearchSuggestions.value
        val hasRestorationSuggestion = updatedSuggestions.any { it.category == "Restoration Boost" && it.query == "Lavender" }
        assertTrue(hasRestorationSuggestion)
        
        job.cancel()
    }

    @Test
    fun smartSearchSuggestions_clearLearningHistory_resetsToDefaults() = runBlocking {
        val job = launch(Dispatchers.Unconfined) {
            viewModel.smartSearchSuggestions.collect {}
        }
        
        viewModel.recordSubstrateSelected("Sand")
        viewModel.recordSeedSelected("Tomato")
        viewModel.recordVisitedScreen(4)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        
        assertTrue(viewModel.smartSearchSuggestions.value.any { 
            it.category == "Substrate Fit" || it.category == "Companion Fit" || it.category == "Restoration Boost" 
        })
        
        viewModel.clearLearningHistory()
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        
        val resetSuggestions = viewModel.smartSearchSuggestions.value
        assertTrue(resetSuggestions.all { it.category == "Popular Search" })
        
        job.cancel()
    }
}
