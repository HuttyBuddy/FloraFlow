package com.example.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [33], manifest = org.robolectric.annotation.Config.NONE)
class AnalyticsHelperTest {

    @Mock
    private lateinit var mockFirebaseAnalytics: FirebaseAnalytics

    private var autoCloseable: AutoCloseable? = null

    @Before
    fun setup() {
        autoCloseable = MockitoAnnotations.openMocks(this)
        AnalyticsHelper.firebaseAnalytics = mockFirebaseAnalytics
    }

    @After
    fun tearDown() {
        AnalyticsHelper.firebaseAnalytics = null
        autoCloseable?.close()
    }

    @Test
    fun testLogEvent() {
        val bundle = Bundle().apply { putString("key", "value") }
        AnalyticsHelper.logEvent("test_event", bundle)
        verify(mockFirebaseAnalytics).logEvent(eq("test_event"), eq(bundle))
    }

    @Test
    fun testLogEvent_withoutParams() {
        AnalyticsHelper.logEvent("test_event_no_params")
        verify(mockFirebaseAnalytics).logEvent(eq("test_event_no_params"), eq(null))
    }

    @Test
    fun testLogAssessmentComplete() {
        AnalyticsHelper.logAssessmentComplete(85)

        val captor = argumentCaptor<Bundle>()
        verify(mockFirebaseAnalytics).logEvent(eq("assessment_complete"), captor.capture())
        assertEquals(85, captor.firstValue.getInt("score"))
    }

    @Test
    fun testLogPaywallView() {
        AnalyticsHelper.logPaywallView("home_screen")

        val captor = argumentCaptor<Bundle>()
        verify(mockFirebaseAnalytics).logEvent(eq("paywall_view"), captor.capture())
        assertEquals("home_screen", captor.firstValue.getString("source"))
    }

    @Test
    fun testLogTrialStart() {
        AnalyticsHelper.logTrialStart()
        verify(mockFirebaseAnalytics).logEvent(eq("trial_start"), eq(null))
    }

    @Test
    fun testLogPurchaseSuccess() {
        AnalyticsHelper.logPurchaseSuccess("premium_monthly")

        val captor = argumentCaptor<Bundle>()
        verify(mockFirebaseAnalytics).logEvent(eq("purchase_success"), captor.capture())
        assertEquals("premium_monthly", captor.firstValue.getString("sku"))
    }

    @Test
    fun testLogAiQuery() {
        AnalyticsHelper.logAiQuery()
        verify(mockFirebaseAnalytics).logEvent(eq("ai_query"), eq(null))
    }

    @Test
    fun testLogRestorationSession() {
        AnalyticsHelper.logRestorationSession(120)

        val captor = argumentCaptor<Bundle>()
        verify(mockFirebaseAnalytics).logEvent(eq("restoration_session"), captor.capture())
        assertEquals(120, captor.firstValue.getInt("duration_seconds"))
    }
}
