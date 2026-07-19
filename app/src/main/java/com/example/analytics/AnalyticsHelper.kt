package com.example.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import androidx.annotation.VisibleForTesting

object AnalyticsHelper {
    @VisibleForTesting
    internal var firebaseAnalytics: FirebaseAnalytics? = null

    fun initialize(context: Context) {
        if (firebaseAnalytics == null) {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        }
    }

    fun logEvent(name: String, params: Bundle? = null) {
        firebaseAnalytics?.logEvent(name, params)
    }

    fun logAssessmentComplete(score: Int) {
        val bundle = Bundle().apply {
            putInt("score", score)
        }
        logEvent("assessment_complete", bundle)
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
