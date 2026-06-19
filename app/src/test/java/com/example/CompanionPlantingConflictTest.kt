package com.example

import com.example.ui.screens.checkPlantSynergy
import com.example.ui.screens.checkPlantConflict
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionPlantingConflictTest {

    @Test
    fun testCompanionSynergy() {
        // marigold & tomato synergy
        assertTrue(checkPlantSynergy("marigold", "tomato"))
        assertTrue(checkPlantSynergy("tomato", "basil"))
        
        // tomato & potato is no longer a synergy
        assertFalse(checkPlantSynergy("tomato", "potato"))
        
        // same species has no synergy
        assertFalse(checkPlantSynergy("lavender", "lavender"))
    }

    @Test
    fun testCompanionConflict() {
        // tomato & potato is a conflict
        assertTrue(checkPlantConflict("tomato", "potato"))
        assertTrue(checkPlantConflict("rose", "cactus"))
        assertTrue(checkPlantConflict("mint", "lavender"))
        
        // marigold & tomato has no conflict
        assertFalse(checkPlantConflict("marigold", "tomato"))
        
        // same species has no conflict
        assertFalse(checkPlantConflict("lavender", "lavender"))
    }
}
