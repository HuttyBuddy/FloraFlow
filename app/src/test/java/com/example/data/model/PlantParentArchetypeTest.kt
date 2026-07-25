package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PlantParentArchetypeTest {

    @Test
    fun calculateArchetype_sanctuaryMasterForHighScore() {
        val archetype = PlantParentArchetype.calculateArchetype(18, emptyList(), 0)
        assertEquals(PlantParentArchetype.SANCTUARY_MASTER, archetype)
    }

    @Test
    fun calculateArchetype_jungleMaximalistForPlantCategory() {
        val archetype = PlantParentArchetype.calculateArchetype(14, listOf("LIVING PLANTS"), 0)
        assertEquals(PlantParentArchetype.JUNGLE_MAXIMALIST, archetype)
    }

    @Test
    fun calculateArchetype_cyberpunkBotanistForLightCategory() {
        val archetype = PlantParentArchetype.calculateArchetype(10, listOf("NATURAL LIGHT"), 0)
        assertEquals(PlantParentArchetype.CYBERPUNK_BOTANIST, archetype)
    }

    @Test
    fun calculateArchetype_serialOverwatererForHighStreak() {
        val archetype = PlantParentArchetype.calculateArchetype(9, emptyList(), 7)
        assertEquals(PlantParentArchetype.SERIAL_OVERWATERER, archetype)
    }

    @Test
    fun calculateArchetype_cactusSurvivorForLowScore() {
        val archetype = PlantParentArchetype.calculateArchetype(5, emptyList(), 0)
        assertEquals(PlantParentArchetype.CACTUS_SURVIVOR, archetype)
    }
}
