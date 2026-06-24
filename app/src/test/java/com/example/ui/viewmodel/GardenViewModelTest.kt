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
}
