package com.example.util

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Play In-App Review, shown only at genuine success moments.
 *
 * Ratings are the single biggest driver of store-listing conversion, and the
 * app previously never asked — so reviews skewed toward users motivated enough
 * to open the Play Store manually (usually unhappy ones).
 *
 * Guardrails (beyond Play's own quota):
 * - At most once per app version.
 * - Only after at least [MIN_MOMENTS] recorded success moments (assessment
 *   finished, 3rd plant added, restoration session completed...).
 * - The ViewModel decides *when* via [recordSuccessMoment]; the Activity
 *   decides *whether to show now* via [maybeRequestReview].
 */
object ReviewHelper {

    private const val TAG = "ReviewHelper"
    private const val PREFS = "floraflow_growth"
    private const val KEY_MOMENTS = "success_moments"
    private const val KEY_ASKED_VERSION = "review_asked_version_code"
    private const val MIN_MOMENTS = 2

    /** Call from the ViewModel when the user hits a success moment. */
    fun recordSuccessMoment(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val moments = prefs.getInt(KEY_MOMENTS, 0) + 1
        prefs.edit().putInt(KEY_MOMENTS, moments).apply()
    }

    /**
     * Launches the Play review flow if the guardrails pass. Must be called
     * with a foreground Activity. Safe to call often — it no-ops unless all
     * conditions are met, and Play itself may still decline to show UI.
     */
    fun maybeRequestReview(activity: Activity) {
        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val versionCode = try {
            val pi = activity.packageManager.getPackageInfo(activity.packageName, 0)
            if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode else @Suppress("DEPRECATION") pi.versionCode.toLong()
        } catch (e: Exception) {
            Log.w(TAG, "Could not read version code: ${e.message}")
            return
        }
        if (prefs.getLong(KEY_ASKED_VERSION, -1L) == versionCode) return
        if (prefs.getInt(KEY_MOMENTS, 0) < MIN_MOMENTS) return

        // Mark as asked *before* launching so a repeat trigger can't nag —
        // Play enforces its own display quota on top of this.
        prefs.edit().putLong(KEY_ASKED_VERSION, versionCode).apply()

        try {
            val manager = ReviewManagerFactory.create(activity)
            manager.requestReviewFlow().addOnCompleteListener { request ->
                if (!request.isSuccessful) {
                    Log.w(TAG, "requestReviewFlow failed: ${request.exception?.message}")
                    return@addOnCompleteListener
                }
                manager.launchReviewFlow(activity, request.result)
                    .addOnCompleteListener {
                        // Finished (shown or silently skipped by Play). Nothing to do.
                    }
            }
        } catch (e: Exception) {
            Log.w(TAG, "In-app review failed: ${e.message}")
        }
    }
}
