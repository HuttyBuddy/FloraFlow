package com.example.ui

import com.example.ui.copy.RestorationLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

class RestorationLanguageTest {
    @Test
    fun assessmentIntro_describes_a_practical_space_check() {
        assertEquals(
            "Answer 10 questions about light, plants, sound, views, and air.",
            RestorationLanguage.assessmentIntro,
        )
    }

    @Test
    fun scoreLabel_uses_restoration_language_not_medical_language() {
        assertEquals("Space Restoration Score", RestorationLanguage.scoreLabel)
    }

    @Test
    fun sessionDescription_frames_audio_as_relaxation_not_treatment() {
        assertEquals(
            "Layered nature sounds for a calm, intentional pause.",
            RestorationLanguage.sessionDescription,
        )
    }
}
