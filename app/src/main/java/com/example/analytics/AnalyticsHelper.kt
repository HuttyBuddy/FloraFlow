package com.example.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

object AnalyticsHelper {
    private var firebaseAnalytics: FirebaseAnalytics? = null

    fun initialize(context: Context) {
        if (firebaseAnalytics == null) {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context)
            // Honor a previously saved opt-out as early as possible.
            try {
                val optedOut = context
                    .getSharedPreferences("floraflow_billing_prefs", Context.MODE_PRIVATE)
                    .getBoolean("analytics_opt_out", false)
                firebaseAnalytics?.setAnalyticsCollectionEnabled(!optedOut)
            } catch (_: Exception) { }
        }
    }

    /** Enables or disables Firebase Analytics collection (Crashlytics is
     * unaffected — it has no per-app runtime toggle). */
    fun setCollectionEnabled(enabled: Boolean) {
        firebaseAnalytics?.setAnalyticsCollectionEnabled(enabled)
    }

    fun logEvent(name: String, params: Bundle? = null) {
        firebaseAnalytics?.logEvent(name, params)
    }

    fun logAssessmentComplete() {
        // NOTE: the assessment score itself is never transmitted (privacy policy).
        logEvent("assessment_complete")
    }

    fun logPaywallView(source: String) {
        val bundle = Bundle().apply {
            putString("source", source)
        }
        logEvent("paywall_view", bundle)
    }

    fun logTrialStart() {
        logEvent("trial_start")
    }

    fun logPurchaseSuccess(sku: String) {
        val bundle = Bundle().apply {
            putString("sku", sku)
        }
        logEvent("purchase_success", bundle)
    }

    fun logAiQuery() {
        logEvent("ai_query")
    }

    fun logRestorationSession(durationSeconds: Int) {
        val bundle = Bundle().apply {
            putInt("duration_seconds", durationSeconds)
        }
        logEvent("restoration_session", bundle)
    }
}
