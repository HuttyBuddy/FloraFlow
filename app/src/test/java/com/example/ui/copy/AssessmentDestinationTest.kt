package com.example.ui.copy

import org.junit.Assert.assertEquals
import org.junit.Test

class AssessmentDestinationTest {
    @Test
    fun livingPlants_opens_the_plant_library() {
        assertEquals(2, AssessmentDestination.tabFor("LIVING PLANTS"))
    }

    @Test
    fun acousticCalm_opens_restoration() {
        assertEquals(4, AssessmentDestination.tabFor("ACOUSTIC CALM"))
    }

    @Test
    fun light_opens_the_space_planner() {
        assertEquals(1, AssessmentDestination.tabFor("NATURAL LIGHT"))
    }
}
