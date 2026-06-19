package com.example

import com.example.ui.screens.checkPlantSynergy
import com.example.ui.screens.checkPlantConflict
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionPlantingConflictTest {

    @Test
    fun testCompanionSynergy() {
        // marigold & ivy synergy
        assertTrue(checkPlantSynergy("marigold", "ivy"))
        assertTrue(checkPlantSynergy("ivy", "basil"))
        
        // ivy & cactus is no longer a synergy
        assertFalse(checkPlantSynergy("ivy", "cactus"))
        
        // same species has no synergy
        assertFalse(checkPlantSynergy("lavender", "lavender"))
    }

    @Test
    fun testCompanionConflict() {
        // ivy & cactus is a conflict
        assertTrue(checkPlantConflict("ivy", "cactus"))
        assertTrue(checkPlantConflict("rose", "cactus"))
        assertTrue(checkPlantConflict("mint", "lavender"))
        
        // marigold & ivy has no conflict
        assertFalse(checkPlantConflict("marigold", "ivy"))
        
        // same species has no conflict
        assertFalse(checkPlantConflict("lavender", "lavender"))
    }
}
