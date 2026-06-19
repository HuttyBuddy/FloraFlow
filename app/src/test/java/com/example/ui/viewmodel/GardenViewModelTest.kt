package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
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
}
