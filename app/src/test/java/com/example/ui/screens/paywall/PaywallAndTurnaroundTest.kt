package com.example.ui.screens.paywall

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GardenViewModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class PaywallAndTurnaroundTest {

    private lateinit var viewModel: GardenViewModel

    @Before
    fun setup() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        viewModel = GardenViewModel(application)
    }

    @Test
    fun gardenViewModel_paywallTriggersAndSubscriptionState() {
        assertFalse(viewModel.showPaywallDialog.value)
        assertFalse(viewModel.isPremium.value)

        viewModel.triggerPaywall()
        assertTrue(viewModel.showPaywallDialog.value)

        viewModel.dismissPaywall()
        assertFalse(viewModel.showPaywallDialog.value)

        viewModel.subscribePro(isAnnual = true)
        assertTrue(viewModel.isPremium.value)
        assertFalse(viewModel.showPaywallDialog.value)
    }
}
