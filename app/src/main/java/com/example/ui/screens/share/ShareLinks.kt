package com.example.ui.screens.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.security.SecureRandom

/**
 * Owns FloraFlow's outbound links and the per-install referral code stamped into them.
 *
 * Every shared asset — story card, Reel, invite — carries the same code so an install can
 * be traced back to the share that caused it. Without this there is no way to measure the
 * organic-install share the growth plan targets, or to tell which archetype travels.
 */
object ShareLinks {

    const val HOST = "floraflow.app"
    const val HANDLE = "@floraflow"

    /** Short invite path, kept brief so it stays legible burned into a 1080px card. */
    private const val JOIN_PATH = "j"

    private const val PREFS = "floraflow_share_links"
    private const val KEY_SHARE_CODE = "share_code"
    private const val KEY_REFERRED_BY = "referred_by"

    /** Ambiguous glyphs (O/0, I/1) are excluded — codes get read off screenshots by hand. */
    private const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private const val CODE_LENGTH = 6

    /**
     * This install's referral code, generated once and then stable. Stable matters: a code
     * that changed per share would make repeat sharers look like distinct sources.
     */
    fun shareCode(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(KEY_SHARE_CODE, null)?.let { return it }

        val random = SecureRandom()
        val code = buildString {
            repeat(CODE_LENGTH) { append(CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)]) }
        }
        prefs.edit().putString(KEY_SHARE_CODE, code).apply()
        return code
    }

    /** Full URL for share sheets and QR codes. */
    fun shareUrl(code: String?): String =
        if (code.isNullOrBlank()) "https://$HOST" else "https://$HOST/$JOIN_PATH/$code"

    /** Protocol-less form for burning into an image, where "https://" is just noise. */
    fun displayLink(code: String?): String =
        if (code.isNullOrBlank()) HOST else "$HOST/$JOIN_PATH/$code"

    /**
     * Extracts the inviter's code from an incoming link.
     *
     * Accepts the current `/j/CODE` form and the legacy `?by=CODE` query the Settings
     * invite used to emit, so links already in the wild keep working.
     */
    fun parseInviteCode(uri: Uri?): String? {
        if (uri == null) return null
        if (uri.host?.removePrefix("www.")?.equals(HOST, ignoreCase = true) != true) return null

        val segments = uri.pathSegments.orEmpty()
        val fromPath = if (segments.size >= 2 && segments[0].equals(JOIN_PATH, ignoreCase = true)) {
            segments[1]
        } else {
            null
        }
        val raw = fromPath
            ?: uri.getQueryParameter("by")
            ?: uri.getQueryParameter("ref")

        return raw?.trim()?.uppercase()?.takeIf(::isValidCode)
    }

    /** True for strings that could have been produced by [shareCode]. */
    internal fun isValidCode(candidate: String): Boolean =
        candidate.isNotEmpty() &&
            candidate.length <= CODE_LENGTH * 2 &&
            candidate.all { it in CODE_ALPHABET }

    /**
     * Records who invited this user, first write wins. Returns true when this was a new
     * attribution, so the caller knows to log the acquisition event exactly once.
     */
    fun recordReferrer(context: Context, inviterCode: String): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // Don't let a user attribute themselves by opening their own link.
        if (inviterCode == prefs.getString(KEY_SHARE_CODE, null)) return false
        if (prefs.getString(KEY_REFERRED_BY, null) != null) return false
        prefs.edit().putString(KEY_REFERRED_BY, inviterCode).apply()
        return true
    }

    fun referredBy(context: Context): String? =
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_REFERRED_BY, null)

    /** Handles an inbound intent's data, attributing the install if it carries an invite. */
    fun handleIncomingIntent(context: Context, intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_VIEW) return null
        val code = parseInviteCode(intent.data) ?: return null
        return if (recordReferrer(context, code)) code else null
    }
}
