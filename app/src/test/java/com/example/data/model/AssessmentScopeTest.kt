package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AssessmentScopeTest {
    @Test
    fun latestForLayout_returns_only_the_newest_result_for_that_space() {
        val results = listOf(
            AssessmentResult(id = 1, timestamp = 100, score = 8, lowestCategories = "NATURAL LIGHT", layoutId = 4),
            AssessmentResult(id = 2, timestamp = 200, score = 14, lowestCategories = "LIVING PLANTS", layoutId = 4),
            AssessmentResult(id = 3, timestamp = 300, score = 20, lowestCategories = "NATURE VIEWS", layoutId = 9)
        )

        assertEquals(2, AssessmentScope.latestForLayout(results, 4)?.id)
        assertNull(AssessmentScope.latestForLayout(results, null))
    }

    @Test
    fun needsAssessment_is_true_when_the_active_space_has_no_result() {
        val results = listOf(
            AssessmentResult(id = 1, score = 12, lowestCategories = "NATURAL LIGHT", layoutId = 4)
        )

        assertEquals(true, AssessmentScope.needsAssessment(results, 9))
        assertEquals(false, AssessmentScope.needsAssessment(results, 4))
    }
}
