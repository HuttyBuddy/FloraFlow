package com.example.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlannerScreenTest {

    @Test
    fun `checkPlantSynergy returns false for identical plants`() {
        assertFalse("Exact same plant should not have synergy", checkPlantSynergy("Rose", "Rose"))
        assertFalse("Case insensitive same plant should not have synergy", checkPlantSynergy("ROSE", "rose"))
        assertFalse("Same plant with spaces should not have synergy", checkPlantSynergy("Red Rose", "red rose"))
    }

    @Test
    fun `checkPlantSynergy returns true for explicit companion rules`() {
        assertTrue("Rose and maple should have synergy", checkPlantSynergy("rose", "maple"))
        assertTrue("Rose and lavender should have synergy", checkPlantSynergy("rose", "lavender"))
        assertTrue("Cactus and aloe should have synergy", checkPlantSynergy("cactus", "aloe"))
    }

    @Test
    fun `checkPlantSynergy is commutative (order independent)`() {
        assertTrue("Maple and rose should have synergy", checkPlantSynergy("maple", "rose"))
        assertTrue("Aloe and cactus should have synergy", checkPlantSynergy("aloe", "cactus"))
    }

    @Test
    fun `checkPlantSynergy returns true for partial string matches`() {
        assertTrue("Red Rose and Silver Maple should have synergy", checkPlantSynergy("Red Rose", "Silver Maple"))
        assertTrue("Cherry Tree and English Lavender should have synergy", checkPlantSynergy("Cherry Tree", "English Lavender"))
    }

    @Test
    fun `checkPlantSynergy returns false for unrelated plants`() {
        assertFalse("Tomato and fern should not have synergy", checkPlantSynergy("tomato", "fern"))
        assertFalse("Cactus and maple should not have synergy", checkPlantSynergy("cactus", "maple"))
    }
}
