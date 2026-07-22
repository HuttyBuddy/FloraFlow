package com.example

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupModeTest {
    @Test
    fun `validation mode blocks every production startup boundary`() {
        val mode = StartupMode.from(restorativeValidation = true)

        assertFalse(mode.runsProductionStartup)
    }

    @Test
    fun `ordinary builds retain production startup behavior`() {
        val mode = StartupMode.from(restorativeValidation = false)

        assertTrue(mode.runsProductionStartup)
    }
}
