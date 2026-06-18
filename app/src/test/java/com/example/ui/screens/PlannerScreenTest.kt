package com.example.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class PlannerScreenTest {

    @Test
    fun getEmojiForPlantName_exactMatches() {
        assertEquals("🌹", getEmojiForPlantName("rose"))
        assertEquals("🌵", getEmojiForPlantName("cactus"))
        assertEquals("🌵", getEmojiForPlantName("aloe"))
        assertEquals("🌼", getEmojiForPlantName("marigold"))
        assertEquals("🌼", getEmojiForPlantName("aster"))
        assertEquals("🪻", getEmojiForPlantName("lavender"))
        assertEquals("🌸", getEmojiForPlantName("orchid"))
        assertEquals("🌺", getEmojiForPlantName("hibiscus"))
        assertEquals("🌺", getEmojiForPlantName("bougainvillea"))
        assertEquals("🍅", getEmojiForPlantName("tomato"))
        assertEquals("🍠", getEmojiForPlantName("sweet potato"))
        assertEquals("🍠", getEmojiForPlantName("yam"))
        assertEquals("🍋", getEmojiForPlantName("fig"))
        assertEquals("🌳", getEmojiForPlantName("olive"))
        assertEquals("🌳", getEmojiForPlantName("maple"))
        assertEquals("🌳", getEmojiForPlantName("tree"))
        assertEquals("🌿", getEmojiForPlantName("fern"))
        assertEquals("🌿", getEmojiForPlantName("monstera"))
        assertEquals("🌿", getEmojiForPlantName("shoot"))
        assertEquals("🌱", getEmojiForPlantName("basil"))
        assertEquals("🌹", getEmojiForPlantName("rosemary")) // Note: 'rosemary' contains 'rose', so it evaluates to '🌹' based on the logic!
        assertEquals("🌱", getEmojiForPlantName("herb"))
        assertEquals("🌱", getEmojiForPlantName("thyme"))
    }

    @Test
    fun getEmojiForPlantName_caseVariations() {
        assertEquals("🌹", getEmojiForPlantName("ROSE"))
        assertEquals("🌵", getEmojiForPlantName("Cactus"))
        assertEquals("🪻", getEmojiForPlantName("LAVENDER"))
        assertEquals("🍠", getEmojiForPlantName("Sweet Potato"))
    }

    @Test
    fun getEmojiForPlantName_partialMatches() {
        assertEquals("🌹", getEmojiForPlantName("red rose"))
        assertEquals("🌹", getEmojiForPlantName("rosebush"))
        assertEquals("🍅", getEmojiForPlantName("cherry tomato"))
        assertEquals("🌳", getEmojiForPlantName("apple tree"))
        assertEquals("🌿", getEmojiForPlantName("fern leaf"))
    }

    @Test
    fun getEmojiForPlantName_defaultFallbacks() {
        assertEquals("🌱", getEmojiForPlantName("unknown plant"))
        assertEquals("🌱", getEmojiForPlantName("grass"))
        assertEquals("🌱", getEmojiForPlantName(""))
        assertEquals("🌱", getEmojiForPlantName("12345"))
    }
}
