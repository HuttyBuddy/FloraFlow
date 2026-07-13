package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class StarterLayoutTest {
    @Test
    fun gridFor_places_two_starter_plants_in_opposite_corners() {
        assertEquals(
            "0,0,Snake Plant|4,4,Lavender",
            StarterLayout.gridFor(listOf("Snake Plant", "Lavender"))
        )
    }

    @Test
    fun gridFor_handles_a_single_starter_plant() {
        assertEquals("0,0,Snake Plant", StarterLayout.gridFor(listOf("Snake Plant")))
    }
}
