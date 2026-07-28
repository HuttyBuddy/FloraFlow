package com.example.ui.screens.share

import android.content.Context
import android.net.Uri
import android.util.Log
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.example.analytics.ShareAnalytics

/**
 * Attributes new installs to the share that caused them.
 *
 * Two paths reach a new user, and both need covering:
 *  - **Deep link** — the app is already installed and the link opens it directly.
 *  - **Install referrer** — the far more common viral path. The link goes to the Play
 *    Store, the user installs, and on first launch there is no deep link to read. Play
 *    hands back the `referrer` string instead, which carries the inviter's code.
 *
 * Without the second path, the majority of genuinely viral installs are invisible, and the
 * organic-install target in the growth plan can't be measured.
 */
object InstallAttribution {

    private const val TAG = "InstallAttribution"

    private const val PREFS = "floraflow_share_links"
    private const val KEY_REFERRER_CHECKED = "install_referrer_checked"

    /**
     * Reads the Play install referrer once per install and records any invite code found.
     * Safe to call on every launch — it no-ops after the first successful read.
     *
     * Expects a `utm_content=CODE` or bare `CODE` referrer, which is what the invite links
     * should append when they redirect to the Play Store listing.
     */
    fun checkInstallReferrer(context: Context) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_REFERRER_CHECKED, false)) return

        val client = InstallReferrerClient.newBuilder(appContext).build()
        client.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                try {
                    if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                        val referrer = client.installReferrer.installReferrer
                        // Mark as checked only on a real answer, so a transient
                        // unavailable-service response gets retried next launch.
                        prefs.edit().putBoolean(KEY_REFERRER_CHECKED, true).apply()

                        parseReferrerCode(referrer)?.let { code ->
                            if (ShareLinks.recordReferrer(appContext, code)) {
                                ShareAnalytics.logInviteAccepted("install_referrer")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Install referrer read failed: ${e.message}")
                } finally {
                    try {
                        client.endConnection()
                    } catch (_: Exception) {
                    }
                }
            }

            override fun onInstallReferrerServiceDisconnected() {
                // Nothing to do: the next launch retries because we never marked it checked.
            }
        })
    }

    /**
     * Pulls an invite code out of a Play referrer string.
     *
     * Handles `utm_content=CODE` / `utm_term=CODE` inside a query-style referrer, and a
     * referrer that is nothing but the code.
     */
    internal fun parseReferrerCode(referrer: String?): String? {
        if (referrer.isNullOrBlank()) return null

        // Uri parses a bare "a=1&b=2" string once given a scheme and authority to hang off.
        val asQuery = Uri.parse("https://" + ShareLinks.HOST + "/?" + referrer)
        val fromParams = asQuery.getQueryParameter("utm_content")
            ?: asQuery.getQueryParameter("utm_term")
            ?: asQuery.getQueryParameter("ref")

        return (fromParams ?: referrer).trim().uppercase().takeIf(ShareLinks::isValidCode)
    }
}
