package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.GardenViewModel
import com.example.ui.viewmodel.WalkthroughStep
import com.example.ui.viewmodel.ScreenRect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class WalkthroughTest {

    private lateinit var viewModel: GardenViewModel

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        viewModel = GardenViewModel(app)
    }

    @Test
    fun testWalkthroughLifecycle() = runTest {
        // Initial state should be null
        assertNull(viewModel.currentWalkthroughStep.value)

        // Start Walkthrough
        viewModel.startWalkthrough()
        assertEquals(WalkthroughStep.WELCOME, viewModel.currentWalkthroughStep.value)
        assertTrue(viewModel.walkthroughTargets.value.isEmpty())

        // Update target
        val welcomeRect = ScreenRect(0f, 0f, 100f, 100f)
        viewModel.updateWalkthroughTarget(WalkthroughStep.WELCOME, welcomeRect)
        assertEquals(welcomeRect, viewModel.walkthroughTargets.value[WalkthroughStep.WELCOME])

        // Step transition: WELCOME -> DASHBOARD_GARDEN
        viewModel.nextWalkthroughStep()
        assertEquals(WalkthroughStep.DASHBOARD_GARDEN, viewModel.currentWalkthroughStep.value)

        // Update DASHBOARD_GARDEN target
        val dashboardRect = ScreenRect(10f, 20f, 110f, 120f)
        viewModel.updateWalkthroughTarget(WalkthroughStep.DASHBOARD_GARDEN, dashboardRect)
        assertEquals(dashboardRect, viewModel.walkthroughTargets.value[WalkthroughStep.DASHBOARD_GARDEN])

        // Skip Walkthrough
        viewModel.skipWalkthrough()
        assertNull(viewModel.currentWalkthroughStep.value)
        assertTrue(viewModel.walkthroughTargets.value.isEmpty())
    }

    @Test
    fun testWalkthroughFullSequence() = runTest {
        viewModel.startWalkthrough()
        assertEquals(WalkthroughStep.WELCOME, viewModel.currentWalkthroughStep.value)

        // WELCOME -> DASHBOARD_GARDEN
        viewModel.nextWalkthroughStep()
        assertEquals(WalkthroughStep.DASHBOARD_GARDEN, viewModel.currentWalkthroughStep.value)

        // DASHBOARD_GARDEN -> DASHBOARD_STATS
        viewModel.nextWalkthroughStep()
        assertEquals(WalkthroughStep.DASHBOARD_STATS, viewModel.currentWalkthroughStep.value)

        // DASHBOARD_STATS -> PLANNER_TAB
        viewModel.nextWalkthroughStep()
        assertEquals(WalkthroughStep.PLANNER_TAB, viewModel.currentWalkthroughStep.value)

        // PLANNER_TAB -> AI_ADVISOR_TAB
        viewModel.nextWalkthroughStep()
        assertEquals(WalkthroughStep.AI_ADVISOR_TAB, viewModel.currentWalkthroughStep.value)

        // AI_ADVISOR_TAB -> Finish (null)
        viewModel.nextWalkthroughStep()
        assertNull(viewModel.currentWalkthroughStep.value)
        assertTrue(viewModel.walkthroughTargets.value.isEmpty())
    }
}
