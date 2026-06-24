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
    fun updateArPlant3DPosition_updatesCorrectPlant() {
        // Setup initial plant
        viewModel.addArPlant("Rose", "🌹", customX = 1f, customY = 2f, customZ = 3f)
        val initialPlants = viewModel.arPlacedPlants.value
        assertEquals(1, initialPlants.size)
        val plantId = initialPlants.first().id

        // Act
        viewModel.updateArPlant3DPosition(plantId, x = 10f, y = 20f, z = 30f)

        // Assert
        val updatedPlants = viewModel.arPlacedPlants.value
        assertEquals(1, updatedPlants.size)
        val updatedPlant = updatedPlants.first()
        assertEquals(10f, updatedPlant.positionX)
        assertEquals(20f, updatedPlant.positionY)
        assertEquals(30f, updatedPlant.positionZ)
    }

    @Test
    fun updateArPlant3DPosition_ignoresNonexistentId() {
        // Setup initial plant
        viewModel.addArPlant("Rose", "🌹", customX = 1f, customY = 2f, customZ = 3f)
        val initialPlants = viewModel.arPlacedPlants.value
        assertEquals(1, initialPlants.size)
        val initialPlant = initialPlants.first()

        // Act - Update with non-existent ID
        viewModel.updateArPlant3DPosition(999, x = 10f, y = 20f, z = 30f)

        // Assert
        val updatedPlants = viewModel.arPlacedPlants.value
        assertEquals(1, updatedPlants.size)
        val updatedPlant = updatedPlants.first()

        // Ensure values remain unchanged
        assertEquals(initialPlant.positionX, updatedPlant.positionX)
        assertEquals(initialPlant.positionY, updatedPlant.positionY)
        assertEquals(initialPlant.positionZ, updatedPlant.positionZ)
    }
}
