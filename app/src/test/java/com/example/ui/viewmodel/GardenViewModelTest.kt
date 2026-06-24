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
    fun updateArPlantPosition_modifiesPlantCoordinatesCorrectly() {
        // Arrange: Add a plant to AR
        viewModel.addArPlant(name = "Test Plant", emoji = "🌿", customX = 10f, customY = 20f)
        val initialPlants = viewModel.arPlacedPlants.value
        assertEquals(1, initialPlants.size)

        val plantId = initialPlants[0].id
        assertEquals(10f, initialPlants[0].positionX)
        assertEquals(20f, initialPlants[0].positionY)

        // Act: Update position
        viewModel.updateArPlantPosition(id = plantId, dx = 5f, dy = -10f)

        // Assert: Verify position is correctly updated
        val updatedPlants = viewModel.arPlacedPlants.value
        assertEquals(1, updatedPlants.size)
        assertEquals(15f, updatedPlants[0].positionX)
        assertEquals(10f, updatedPlants[0].positionY)
    }
}
