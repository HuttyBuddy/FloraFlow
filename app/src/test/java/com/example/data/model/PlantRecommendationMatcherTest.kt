package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PlantRecommendationMatcherTest {
    @Test
    fun extract_returns_supported_plants_mentioned_in_advice() {
        val matches = PlantRecommendationMatcher.extract(
            message = "Try a Snake Plant near the window and add Lavender by the desk.",
            candidates = listOf("Snake Plant", "Lavender", "Rose"),
        )

        assertEquals(listOf("Snake Plant", "Lavender"), matches)
    }

    @Test
    fun extract_prefers_the_longest_name_when_candidates_overlap() {
        val matches = PlantRecommendationMatcher.extract(
            message = "A Golden Pothos works well here.",
            candidates = listOf("Pothos", "Golden Pothos"),
        )

        assertEquals(listOf("Golden Pothos"), matches)
    }
}
