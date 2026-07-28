package com.example.ui.screens.share

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShareLinksTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("floraflow_share_links", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun shareCode_isStableAcrossCalls() {
        val first = ShareLinks.shareCode(context)
        val second = ShareLinks.shareCode(context)
        // A code that changed per share would make one sharer look like many sources.
        assertEquals(first, second)
        assertEquals(6, first.length)
    }

    @Test
    fun shareCode_avoidsAmbiguousGlyphs() {
        val code = ShareLinks.shareCode(context)
        // O/0 and I/1 are indistinguishable when read off a screenshot.
        assertFalse("code contained ambiguous glyph: $code", code.any { it in "O0I1" })
    }

    @Test
    fun shareUrl_usesShortJoinPath() {
        assertEquals("https://floraflow.app/j/AB12CD", ShareLinks.shareUrl("AB12CD"))
        assertEquals("floraflow.app/j/AB12CD", ShareLinks.displayLink("AB12CD"))
    }

    @Test
    fun shareUrl_fallsBackToBareHostWithoutCode() {
        assertEquals("https://floraflow.app", ShareLinks.shareUrl(null))
        assertEquals("floraflow.app", ShareLinks.displayLink(""))
    }

    @Test
    fun parseInviteCode_readsJoinPath() {
        assertEquals(
            "AB12CD",
            ShareLinks.parseInviteCode(Uri.parse("https://floraflow.app/j/AB12CD"))
        )
    }

    /** Links already in the wild used ?by=CODE; they must keep working. */
    @Test
    fun parseInviteCode_readsLegacyQueryForm() {
        assertEquals(
            "AB12CD",
            ShareLinks.parseInviteCode(Uri.parse("https://floraflow.app/referral?by=AB12CD"))
        )
    }

    @Test
    fun parseInviteCode_normalisesCase() {
        assertEquals(
            "AB12CD",
            ShareLinks.parseInviteCode(Uri.parse("https://www.floraflow.app/j/ab12cd"))
        )
    }

    @Test
    fun parseInviteCode_rejectsForeignHost() {
        assertNull(ShareLinks.parseInviteCode(Uri.parse("https://evil.example/j/AB12CD")))
    }

    @Test
    fun parseInviteCode_rejectsMalformedCode() {
        // "0" and "!" are outside the code alphabet.
        assertNull(ShareLinks.parseInviteCode(Uri.parse("https://floraflow.app/j/AB0!CD")))
        assertNull(ShareLinks.parseInviteCode(Uri.parse("https://floraflow.app/j/")))
        assertNull(ShareLinks.parseInviteCode(null))
    }

    @Test
    fun recordReferrer_firstWriteWins() {
        assertTrue(ShareLinks.recordReferrer(context, "AAAAAA"))
        assertFalse(ShareLinks.recordReferrer(context, "BBBBBB"))
        assertEquals("AAAAAA", ShareLinks.referredBy(context))
    }

    /** Opening your own link must not credit you with your own install. */
    @Test
    fun recordReferrer_rejectsSelfAttribution() {
        val own = ShareLinks.shareCode(context)
        assertFalse(ShareLinks.recordReferrer(context, own))
        assertNull(ShareLinks.referredBy(context))
    }

    @Test
    fun parseReferrerCode_readsUtmContent() {
        assertEquals(
            "AB12CD",
            InstallAttribution.parseReferrerCode("utm_source=floraflow&utm_content=AB12CD")
        )
    }

    @Test
    fun parseReferrerCode_readsBareCode() {
        assertEquals("AB12CD", InstallAttribution.parseReferrerCode("AB12CD"))
    }

    @Test
    fun parseReferrerCode_rejectsOrganicPlayReferrer() {
        // The default Play referrer for an organic install carries no usable code.
        assertNull(InstallAttribution.parseReferrerCode("utm_source=google-play&utm_medium=organic"))
        assertNull(InstallAttribution.parseReferrerCode(""))
        assertNull(InstallAttribution.parseReferrerCode(null))
    }

    @Test
    fun distinctInstalls_getDistinctCodes() {
        val first = ShareLinks.shareCode(context)
        context.getSharedPreferences("floraflow_share_links", Context.MODE_PRIVATE)
            .edit().clear().commit()
        val second = ShareLinks.shareCode(context)
        // 32^6 keyspace — a collision here means the generator isn't random.
        assertNotEquals(first, second)
    }
}
