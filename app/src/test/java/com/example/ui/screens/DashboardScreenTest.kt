package com.example.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Terrain
import com.example.ui.screens.dashboard.imageIcons
import com.example.ui.screens.dashboard.getRitualForCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardScreenTest {

    @Test
    fun imageIcons_returnsEcoIcon_whenNameIsEco() {
        assertEquals(Icons.Default.Eco, imageIcons("eco"))
    }

    @Test
    fun imageIcons_returnsSpaIcon_whenNameIsSpa() {
        assertEquals(Icons.Default.Spa, imageIcons("spa"))
    }

    @Test
    fun imageIcons_returnsTerrainIcon_whenNameIsOther() {
        assertEquals(Icons.Default.Terrain, imageIcons("other"))
        assertEquals(Icons.Default.Terrain, imageIcons(""))
        assertEquals(Icons.Default.Terrain, imageIcons("random"))
    }

    @Test
    fun getRitualForCategory_returnsCorrectRitual_forNatureViews() {
        val ritual = getRitualForCategory("NATURE VIEWS")
        assertEquals("NATURE VIEWS", ritual.category)
        assertEquals("Look out window at green spaces for 2m", ritual.text)
        assertEquals("[Ritual: Nature Views Window Break]", ritual.noteMarker)
    }

    @Test
    fun getRitualForCategory_returnsGeneralRitual_forUnknownCategory() {
        val ritual = getRitualForCategory("UNKNOWN")
        assertEquals("GENERAL", ritual.category)
        assertEquals("[Ritual: General Deep Breaths]", ritual.noteMarker)
    }
}
