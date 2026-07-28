package com.example.ui.screens.share

import android.graphics.Paint
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.PlantParentArchetype
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ViralShareEngineTest {

    @Test
    fun shareCardData_defaultValuesCorrect() {
        val data = ShareCardData()
        assertEquals(PlantParentArchetype.JUNGLE_MAXIMALIST, data.archetype)
        assertEquals(88, data.score)
        assertEquals(3, data.topUpgrades.size)
        assertNull(data.shareCode)
    }

    @Test
    fun shareCardData_customDataConstructed() {
        val data = ShareCardData(
            archetype = PlantParentArchetype.CYBERPUNK_BOTANIST,
            vibeTag = "Cyberpunk Desk",
            score = 65,
            topUpgrades = listOf("Upgrade 1", "Upgrade 2"),
            shareCode = "AB12CD"
        )
        assertEquals(PlantParentArchetype.CYBERPUNK_BOTANIST, data.archetype)
        assertEquals("Cyberpunk Desk", data.vibeTag)
        assertEquals(65, data.score)
        assertEquals(2, data.topUpgrades.size)
        assertEquals("AB12CD", data.shareCode)
    }

    /** The archetype is the hook — the caption must lead with it, not with the score. */
    @Test
    fun shareText_leadsWithArchetypeAndCarriesInviteLink() {
        val text = ViralShareEngine.shareText(
            ShareCardData(
                archetype = PlantParentArchetype.SANCTUARY_MASTER,
                score = 94,
                shareCode = "XY7ZQ2"
            )
        )
        assertTrue("expected archetype title, got: $text", text.contains("Sanctuary Master"))
        assertTrue("expected score, got: $text", text.contains("94%"))
        assertTrue("expected invite link, got: $text", text.contains("floraflow.app/j/XY7ZQ2"))
    }

    @Test
    fun wrapText_breaksOnWordBoundariesNotMidWord() {
        val paint = Paint().apply { textSize = 32f }
        val source = "Group Monstera and Peace Lily together for humidity synergy"
        val lines = ViralShareEngine.wrapText(source, paint, maxWidth = 20f)

        assertTrue("expected multiple lines, got $lines", lines.size > 1)
        // Reassembling must reproduce the input: nothing dropped, no word split.
        assertEquals(source, lines.joinToString(" "))
    }

    @Test
    fun wrapText_emptyInputProducesNoLines() {
        val paint = Paint().apply { textSize = 32f }
        assertEquals(emptyList<String>(), ViralShareEngine.wrapText("   ", paint, 500f))
    }

    /** A word longer than the line must still be emitted rather than dropped or looped on. */
    @Test
    fun wrapText_overlongWordStillEmitted() {
        val paint = Paint().apply { textSize = 32f }
        val lines = ViralShareEngine.wrapText("Chlorophytum-comosum-variegatum", paint, 40f)
        assertEquals(1, lines.size)
        assertEquals("Chlorophytum-comosum-variegatum", lines[0])
    }

    @Test
    fun ellipsize_leavesShortTextUntouched() {
        val paint = Paint().apply { textSize = 20f }
        assertEquals("Add a Snake Plant", ViralShareEngine.ellipsize("Add a Snake Plant", paint, 5000f))
    }

    @Test
    fun ellipsize_truncatesLongTextWithinBudget() {
        val paint = Paint().apply { textSize = 32f }
        val result = ViralShareEngine.ellipsize(
            "Group Monstera and Peace Lily together for humidity synergy",
            paint,
            maxWidth = 20f
        )
        assertTrue("expected an ellipsis, got: $result", result.endsWith("…"))
        assertTrue("still too wide: $result", paint.measureText(result) <= 20f)
    }

    @Test
    fun storyCard_rendersAtStoryResolution() {
        val bitmap = ViralShareEngine.generate9by16StoryBitmap(
            ApplicationProvider.getApplicationContext(),
            ShareCardData(shareCode = "AB12CD")
        )
        assertNotNull(bitmap)
        // 1080x1920 is the Stories/Reels canvas; the old 512x512 card looked pixelated.
        assertEquals(1080, bitmap.width)
        assertEquals(1920, bitmap.height)
    }
}
