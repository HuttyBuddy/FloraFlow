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

        // Record 9 positive interactions
        repeat(9) {
            viewModel.recordPositiveInteraction()
            assertFalse(viewModel.showInAppRatePrompt.value)
        }

        // 10th positive interaction triggers prompt
        viewModel.recordPositiveInteraction()
        assertTrue(viewModel.showInAppRatePrompt.value)

        // Dismiss prompt
        viewModel.dismissRatePrompt()
        assertFalse(viewModel.showInAppRatePrompt.value)

        // Further positive interactions should not trigger it anymore
        viewModel.recordPositiveInteraction()
        viewModel.recordPositiveInteraction()
        assertFalse(viewModel.showInAppRatePrompt.value)
    }
}
