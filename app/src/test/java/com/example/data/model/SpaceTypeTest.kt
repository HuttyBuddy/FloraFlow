package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SpaceTypeTest {
    @Test
    fun fromStyle_classifies_room_and_balcony_styles_as_indoor() {
        assertEquals(SpaceType.INDOOR, SpaceType.fromStyle("Indoor Area"))
        assertEquals(SpaceType.INDOOR, SpaceType.fromStyle("Urban Balcony"))
    }

    @Test
    fun fromStyle_classifies_garden_styles_as_outdoor() {
        assertEquals(SpaceType.OUTDOOR, SpaceType.fromStyle("Cottage Garden"))
        assertEquals(SpaceType.OUTDOOR, SpaceType.fromStyle("Desert Landscape"))
    }

    @Test
    fun stylesFor_limits_the_picker_to_the_selected_space_type() {
        val styles = listOf("Indoor Area", "Cottage Garden", "Modern Patio")

        assertEquals(listOf("Indoor Area"), SpaceType.INDOOR.stylesFor(styles))
        assertEquals(listOf("Cottage Garden", "Modern Patio"), SpaceType.OUTDOOR.stylesFor(styles))
    }
}
