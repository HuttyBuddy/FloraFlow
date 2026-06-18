package com.example.ui.screens

import com.example.data.model.GridPlantItem
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlannerScreenTest {

    @Test
    fun `checkPlantSynergy - same plant returns false`() {
        assertFalse(checkPlantSynergy("Rose", "Rose"))
        assertFalse(checkPlantSynergy("maple", "MAPLE"))
    }

    @Test
    fun `checkPlantSynergy - valid combinations return true`() {
        assertTrue(checkPlantSynergy("Rose", "Maple"))
        assertTrue(checkPlantSynergy("maple", "rose"))
        assertTrue(checkPlantSynergy("Rose", "Lavender"))
        assertTrue(checkPlantSynergy("Lavender", "Cherry"))
        assertTrue(checkPlantSynergy("Cactus", "Aloe"))
        assertTrue(checkPlantSynergy("Marigold", "Tomato"))
        assertTrue(checkPlantSynergy("Tomato", "Potato"))
        assertTrue(checkPlantSynergy("Aster", "Thyme"))
        assertTrue(checkPlantSynergy("Columbine", "Fern"))
        assertTrue(checkPlantSynergy("Rosemary", "Lavender"))
        assertTrue(checkPlantSynergy("Rosemary", "Thyme"))

        // Tests the `contains` logic in the method
        assertTrue(checkPlantSynergy("red rose", "japanese maple"))
    }

    @Test
    fun `checkPlantSynergy - invalid combinations return false`() {
        assertFalse(checkPlantSynergy("Rose", "Cactus"))
        assertFalse(checkPlantSynergy("Tomato", "Fern"))
    }

    @Test
    fun `hasNeighborSynergy - no plant at coordinates returns false`() {
        val gridItems = listOf(
            GridPlantItem(0, 0, "Rose")
        )
        assertFalse(hasNeighborSynergy(1, 1, gridItems))
    }

    @Test
    fun `hasNeighborSynergy - plant with no neighbors returns false`() {
        val gridItems = listOf(
            GridPlantItem(1, 1, "Rose")
        )
        assertFalse(hasNeighborSynergy(1, 1, gridItems))
    }

    @Test
    fun `hasNeighborSynergy - plant with non-synergistic neighbors returns false`() {
        val gridItems = listOf(
            GridPlantItem(1, 1, "Rose"),
            GridPlantItem(0, 1, "Cactus"),
            GridPlantItem(2, 1, "Tomato"),
            GridPlantItem(1, 0, "Fern"),
            GridPlantItem(1, 2, "Potato")
        )
        assertFalse(hasNeighborSynergy(1, 1, gridItems))
    }

    @Test
    fun `hasNeighborSynergy - plant with a synergistic neighbor returns true`() {
        val gridItems1 = listOf(
            GridPlantItem(1, 1, "Rose"),
            GridPlantItem(0, 1, "Maple") // Top neighbor
        )
        assertTrue(hasNeighborSynergy(1, 1, gridItems1))

        val gridItems2 = listOf(
            GridPlantItem(1, 1, "Rose"),
            GridPlantItem(2, 1, "Lavender") // Bottom neighbor
        )
        assertTrue(hasNeighborSynergy(1, 1, gridItems2))

        val gridItems3 = listOf(
            GridPlantItem(1, 1, "Marigold"),
            GridPlantItem(1, 0, "Tomato") // Left neighbor
        )
        assertTrue(hasNeighborSynergy(1, 1, gridItems3))

        val gridItems4 = listOf(
            GridPlantItem(1, 1, "Tomato"),
            GridPlantItem(1, 2, "Potato") // Right neighbor
        )
        assertTrue(hasNeighborSynergy(1, 1, gridItems4))
    }

    @Test
    fun `hasNeighborSynergy - diagonal neighbors don't provide synergy`() {
        val gridItems = listOf(
            GridPlantItem(1, 1, "Rose"),
            GridPlantItem(0, 0, "Maple"), // Top-Left
            GridPlantItem(0, 2, "Maple"), // Top-Right
            GridPlantItem(2, 0, "Maple"), // Bottom-Left
            GridPlantItem(2, 2, "Maple")  // Bottom-Right
        )
        assertFalse(hasNeighborSynergy(1, 1, gridItems))
    }

    @Test
    fun `hasNeighborSynergy - bounds edge cases`() {
        // Top-Left corner (0,0)
        val gridItemsTopLeft = listOf(
            GridPlantItem(0, 0, "Rose"),
            GridPlantItem(0, 1, "Maple") // Right neighbor
        )
        assertTrue(hasNeighborSynergy(0, 0, gridItemsTopLeft))

        // Bottom-Right corner (assuming 5x5, so 4,4)
        val gridItemsBottomRight = listOf(
            GridPlantItem(4, 4, "Tomato"),
            GridPlantItem(3, 4, "Marigold") // Top neighbor
        )
        assertTrue(hasNeighborSynergy(4, 4, gridItemsBottomRight))
    }
}
