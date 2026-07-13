package com.example.ui

import com.example.ui.copy.AssessmentStepCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssessmentStepCopyTest {
    @Test
    fun naturalLightDetail_keeps_the_score_and_gives_a_practical_action() {
        val detail = AssessmentStepCopy.detailFor("NATURAL LIGHT", 1)

        assertTrue(detail.contains("1/2"))
        assertTrue(detail.contains("brightest suitable natural light"))
    }

    @Test
    fun unknownCategory_has_a_clear_fallback_action() {
        assertEquals(
            "Choose one small biophilic change to make your space feel more alive this week.",
            AssessmentStepCopy.detailFor("UNKNOWN", 0)
        )
    }
}
