package com.example.feedback

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.GardenViewModel
import com.example.ui.viewmodel.FeedbackSubmission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class FeedbackTest {

    private lateinit var context: Context
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext<Application>()
        // Clear shared preferences before each test to ensure a clean state
        val sharedPrefs = context.getSharedPreferences("floraflow_billing_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().clear().apply()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun waitForFeedbackSuccess(viewModel: GardenViewModel) {
        var attempts = 0
        while (attempts < 150 && !viewModel.feedbackSuccess.value) {
            Thread.sleep(20)
            attempts++
        }
        assertTrue("Feedback submission timed out / failed to complete", viewModel.feedbackSuccess.value)
    }

    @Test
    fun testInitialFeedbackState() {
        val viewModel = GardenViewModel(context as Application)
        
        // Assert defaults
        assertFalse(viewModel.isSubmittingFeedback.value)
        assertFalse(viewModel.feedbackSuccess.value)
        assertTrue(viewModel.feedbackSubmissions.value.isEmpty())
    }

    @Test
    fun testSubmitFeedbackFlow() = runTest {
        val viewModel = GardenViewModel(context as Application)
        
        // Submit feedback
        viewModel.submitFeedback(
            category = "✨ Feature Request",
            rating = 5,
            comments = "Loving the new spatial AR feature! Please add water lilies.",
            email = "user@example.com"
        )
        
        // Wait for background coroutine on Dispatchers.IO to finish network attempt & set success
        waitForFeedbackSuccess(viewModel)
        
        // Check final states
        assertFalse(viewModel.isSubmittingFeedback.value)
        assertEquals(1, viewModel.feedbackSubmissions.value.size)
        
        val submission = viewModel.feedbackSubmissions.value[0]
        assertEquals("✨ Feature Request", submission.category)
        assertEquals(5, submission.rating)
        assertEquals("Loving the new spatial AR feature! Please add water lilies.", submission.comments)
        assertEquals("user@example.com", submission.email)
    }

    @Test
    fun testFeedbackPersistenceAndRestoration() = runTest {
        // 1. Submit feedback in first VM instance
        var viewModel = GardenViewModel(context as Application)
        viewModel.submitFeedback(
            category = "🐛 Bug Report",
            rating = 2,
            comments = "App freezes when placing 15 virtual cherry trees.",
            email = "test@example.com"
        )
        waitForFeedbackSuccess(viewModel)
        
        // 2. Re-instantiate ViewModel and verify stored feedback is restored
        viewModel = GardenViewModel(context as Application)
        assertEquals(1, viewModel.feedbackSubmissions.value.size)
        
        val restored = viewModel.feedbackSubmissions.value[0]
        assertEquals("🐛 Bug Report", restored.category)
        assertEquals(2, restored.rating)
        assertEquals("App freezes when placing 15 virtual cherry trees.", restored.comments)
        assertEquals("test@example.com", restored.email)
    }

    @Test
    fun testResetFeedbackSuccess() = runTest {
        val viewModel = GardenViewModel(context as Application)
        
        viewModel.submitFeedback(
            category = "💬 General Comment",
            rating = 4,
            comments = "Great layout choices.",
            email = ""
        )
        waitForFeedbackSuccess(viewModel)
        assertTrue(viewModel.feedbackSuccess.value)
        
        // Reset success state
        viewModel.resetFeedbackSuccess()
        assertFalse(viewModel.feedbackSuccess.value)
    }
}
