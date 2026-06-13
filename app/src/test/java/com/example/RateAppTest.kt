package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.GardenViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class RateAppTest {

    private lateinit var viewModel: GardenViewModel

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        viewModel = GardenViewModel(app)
    }

    @Test
    fun testRatingPromptFlow() = runTest {
        // Initially, prompt should not be shown
        assertFalse(viewModel.showInAppRatePrompt.value)

        // First positive interaction
        viewModel.recordPositiveInteraction()
        assertFalse(viewModel.showInAppRatePrompt.value)

        // Second positive interaction triggers prompt
        viewModel.recordPositiveInteraction()
        assertTrue(viewModel.showInAppRatePrompt.value)

        // Dismiss via Maybe Later
        viewModel.dismissRatePrompt()
        assertFalse(viewModel.showInAppRatePrompt.value)

        // Count should be reset, recording one more positive interaction won't trigger yet
        viewModel.recordPositiveInteraction()
        assertFalse(viewModel.showInAppRatePrompt.value)

        // Recording a second one triggers it again
        viewModel.recordPositiveInteraction()
        assertTrue(viewModel.showInAppRatePrompt.value)

        // Decline prompt
        viewModel.declineRatePrompt()
        assertFalse(viewModel.showInAppRatePrompt.value)

        // Further interactions should not trigger it anymore
        viewModel.recordPositiveInteraction()
        viewModel.recordPositiveInteraction()
        viewModel.recordPositiveInteraction()
        assertFalse(viewModel.showInAppRatePrompt.value)
    }
}
