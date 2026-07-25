package com.example.ui.screens.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ViralShareEngineTest {

    @Test
    fun shareCardData_defaultValuesCorrect() {
        val data = ShareCardData()
        assertEquals("FloraFlow AI Vibe Check", data.title)
        assertEquals(88, data.score)
        assertEquals(34, data.scoreDelta)
        assertEquals(3, data.topUpgrades.size)
    }

    @Test
    fun shareCardData_customDataConstructed() {
        val data = ShareCardData(
            vibeTag = "Cyberpunk Desk",
            score = 65,
            scoreDelta = 35,
            topUpgrades = listOf("Upgrade 1", "Upgrade 2")
        )
        assertEquals("Cyberpunk Desk", data.vibeTag)
        assertEquals(65, data.score)
        assertEquals(35, data.scoreDelta)
        assertEquals(2, data.topUpgrades.size)
    }
}
