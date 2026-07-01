package com.example.ui.screens

import com.example.data.model.GridPlantItem
import org.junit.Test
import kotlin.system.measureTimeMillis

class PerformanceBenchmarkTest {

    @Test
    fun benchmarkGridLookups() {
        // Create a full 5x5 grid
        val activeGridItems = mutableListOf<GridPlantItem>()
        for (r in 0..4) {
            for (c in 0..4) {
                activeGridItems.add(GridPlantItem(r, c, "Rose"))
            }
        }

        val iterations = 100000 // 100k times to simulate many recompositions

        val time = measureTimeMillis {
            val gridMap = activeGridItems.associateBy { (it.x shl 16) or it.y }
            for (i in 0 until iterations) {
                for (r in 0..4) {
                    for (c in 0..4) {
                        val item = gridMap[(r shl 16) or c]
                        var hasSynergy = false
                        var hasConflict = false
                        if (item != null) {
                            val n1 = gridMap[((r - 1) shl 16) or c]
                            val n2 = gridMap[((r + 1) shl 16) or c]
                            val n3 = gridMap[(r shl 16) or (c - 1)]
                            val n4 = gridMap[(r shl 16) or (c + 1)]

                            if (n1 != null) {
                                if (!hasSynergy) hasSynergy = checkPlantSynergy(item.plantName, n1.plantName)
                                if (!hasConflict) hasConflict = checkPlantConflict(item.plantName, n1.plantName)
                            }
                            if (n2 != null && !(hasSynergy && hasConflict)) {
                                if (!hasSynergy) hasSynergy = checkPlantSynergy(item.plantName, n2.plantName)
                                if (!hasConflict) hasConflict = checkPlantConflict(item.plantName, n2.plantName)
                            }
                            if (n3 != null && !(hasSynergy && hasConflict)) {
                                if (!hasSynergy) hasSynergy = checkPlantSynergy(item.plantName, n3.plantName)
                                if (!hasConflict) hasConflict = checkPlantConflict(item.plantName, n3.plantName)
                            }
                            if (n4 != null && !(hasSynergy && hasConflict)) {
                                if (!hasSynergy) hasSynergy = checkPlantSynergy(item.plantName, n4.plantName)
                                if (!hasConflict) hasConflict = checkPlantConflict(item.plantName, n4.plantName)
                            }
                        }
                    }
                }
            }
        }
        println("Improved Time V3: $time ms")
    }
}
