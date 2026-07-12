package com.example.ui.copy

import org.junit.Assert.assertEquals
import org.junit.Test

class AppDestinationTest {
    @Test
    fun primary_labels_describe_user_jobs() {
        assertEquals("Today", AppDestination.TODAY.label)
        assertEquals("Plan", AppDestination.PLAN.label)
        assertEquals("My Garden", AppDestination.MY_GARDEN.label)
        assertEquals("Garden Counsel", AppDestination.COUNSEL.accessibilityLabel)
    }
}
