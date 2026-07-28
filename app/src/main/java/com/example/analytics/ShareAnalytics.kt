package com.example.analytics

import android.os.Bundle

/**
 * Instrumentation for the viral loop.
 *
 * The growth plan targets a share of installs coming from organic word-of-mouth, which is
 * unmeasurable without these events: they establish how many users reach a share surface,
 * how many complete a share, which archetype and which surface travel, and — paired with
 * [com.example.ui.screens.share.ShareLinks] attribution — how many installs each share
 * produced. Every parameter here is app-generated; no user content is logged.
 */
object ShareAnalytics {

    /** Where a share was initiated from, so surfaces can be compared against each other. */
    object Surface {
        const val ONBOARDING_RESULT = "onboarding_result"
        const val VIBE_CHECK = "vibe_check"
        const val DASHBOARD = "dashboard"
        const val REELS_EXPORTER = "reels_exporter"
        const val SETTINGS_INVITE = "settings_invite"
    }

    /** What kind of asset was shared. */
    object Asset {
        const val STORY_CARD = "story_card"
        const val REEL_VIDEO = "reel_video"
        const val INVITE_LINK = "invite_link"
    }

    /** A share surface was opened — the denominator for share conversion. */
    fun logShareSurfaceViewed(surface: String) {
        AnalyticsHelper.logEvent(
            "share_surface_viewed",
            Bundle().apply { putString("surface", surface) }
        )
    }

    /**
     * The user tapped share and the system chooser was launched. This is the last event we
     * can observe — Android does not report which app the user picked, or whether they
     * completed the post. Install attribution via [shareUrl] codes closes that gap.
     */
    fun logShareInitiated(
        surface: String,
        asset: String,
        archetype: String? = null,
        score: Int? = null
    ) {
        AnalyticsHelper.logEvent(
            "share_initiated",
            Bundle().apply {
                putString("surface", surface)
                putString("asset", asset)
                archetype?.let { putString("archetype", it) }
                score?.let { putInt("score", it) }
            }
        )
    }

    /** A share asset failed to render or the chooser could not be launched. */
    fun logShareFailed(surface: String, asset: String, reason: String) {
        AnalyticsHelper.logEvent(
            "share_failed",
            Bundle().apply {
                putString("surface", surface)
                putString("asset", asset)
                putString("reason", reason.take(100))
            }
        )
    }

    /** A Reel finished encoding. [hasAudio] flags whether the AAC track made it in. */
    fun logReelGenerated(archetype: String, durationMs: Long, hasAudio: Boolean) {
        AnalyticsHelper.logEvent(
            "reel_generated",
            Bundle().apply {
                putString("archetype", archetype)
                putLong("duration_ms", durationMs)
                putInt("has_audio", if (hasAudio) 1 else 0)
            }
        )
    }

    fun logReelGenerationFailed(reason: String) {
        AnalyticsHelper.logEvent(
            "reel_generation_failed",
            Bundle().apply { putString("reason", reason.take(100)) }
        )
    }

    /**
     * This install arrived from another user's invite link — the numerator of k-factor.
     * Logged once, the first time attribution is recorded.
     */
    fun logInviteAccepted(source: String) {
        AnalyticsHelper.logEvent(
            "invite_accepted",
            Bundle().apply { putString("source", source) }
        )
    }
}
