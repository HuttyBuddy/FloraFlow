package com.example.ui.screens.settings

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.GardenViewModel
import com.example.ui.viewmodel.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class SettingsActionsTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Application>()
        val sharedPrefs = context.getSharedPreferences("floraflow_billing_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().clear().apply()
    }

    @Test
    fun testOpenAndCloseRoomVibeCheck() {
        val viewModel = GardenViewModel(context as Application)
        assertFalse(viewModel.showRoomVibeCheckScreen.value)

        viewModel.openRoomVibeCheck()
        assertTrue(viewModel.showRoomVibeCheckScreen.value)

        viewModel.closeRoomVibeCheck()
        assertFalse(viewModel.showRoomVibeCheckScreen.value)
    }

    @Test
    fun testTriggerAndDismissPaywall() {
        val viewModel = GardenViewModel(context as Application)
        assertFalse(viewModel.showPaywallDialog.value)

        viewModel.triggerPaywall()
        assertTrue(viewModel.showPaywallDialog.value)

        viewModel.dismissPaywall()
        assertFalse(viewModel.showPaywallDialog.value)
    }

    @Test
    fun testStartWalkthroughAction() {
        val viewModel = GardenViewModel(context as Application)
        assertFalse(viewModel.walkthroughActive.value)

        viewModel.startWalkthrough()
        assertTrue(viewModel.walkthroughActive.value)
    }

    @Test
    fun testStartRestorativeCornerAssessmentAction() {
        val viewModel = GardenViewModel(context as Application)
        assertFalse(viewModel.showRestorativeValidationFlow.value)

        viewModel.startRestorativeCornerAssessment()
        assertTrue(viewModel.showRestorativeValidationFlow.value)
    }

    @Test
    fun testToggleSimulation30DaysAction() {
        val viewModel = GardenViewModel(context as Application)
        assertFalse(viewModel.simulate30Days.value)

        viewModel.toggleSimulation30Days()
        assertTrue(viewModel.simulate30Days.value)

        viewModel.toggleSimulation30Days()
        assertFalse(viewModel.simulate30Days.value)
    }
}
