package com.example.ui.copy

import org.junit.Assert.assertEquals
import org.junit.Test

class AssessmentFocusTest {
    @Test
    fun light_priority_returns_a_placement_action() {
        assertEquals(
            "Choose the brightest suitable spot before placing plants.",
            AssessmentFocus.actionFor("NATURAL LIGHT"),
        )
    }

    @Test
    fun living_plants_priority_returns_a_planting_action() {
        assertEquals(
            "Add two easy-care plants to start your living layer.",
            AssessmentFocus.actionFor("LIVING PLANTS"),
        )
    }
}
