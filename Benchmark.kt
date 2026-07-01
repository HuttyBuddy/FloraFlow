import kotlin.system.measureTimeMillis

data class GridPlantItem(val x: Int, val y: Int, val plantName: String) {
    companion object {
        fun fromString(str: String): GridPlantItem? {
            val parts = str.split(",")
            if (parts.size >= 3) {
                val x = parts[0].toIntOrNull()
                val y = parts[1].toIntOrNull()
                val name = parts.subList(2, parts.size).joinToString(",")
                if (x != null && y != null) {
                    return GridPlantItem(x, y, name)
                }
            }
            return null
        }
    }
}

fun parseGridString(gridString: String): List<GridPlantItem> {
    if (gridString.isBlank()) return emptyList()
    return gridString.split("|").mapNotNull { GridPlantItem.fromString(it) }
}

fun originalCalculatePlantAges(currentIndex: Int, snapshots: List<Pair<Long, String>>): Array<IntArray> {
    val ages = Array(5) { IntArray(5) { 0 } }
    for (r in 0..4) {
        for (c in 0..4) {
            var age = 0
            for (i in 0..currentIndex) {
                val snap = snapshots.getOrNull(i)
                val snapGrid = parseGridString(snap?.second ?: "")
                if (snapGrid.any { it.x == r && it.y == c }) {
                    age++
                } else {
                    age = 0 // Reset if it was empty or uprooted
                }
            }
            ages[r][c] = age
        }
    }
    return ages
}

fun optimizedCalculatePlantAges(currentIndex: Int, snapshots: List<Pair<Long, String>>): Array<IntArray> {
    val ages = Array(5) { IntArray(5) { 0 } }
    for (i in 0..currentIndex) {
        val snap = snapshots.getOrNull(i)
        val snapGrid = parseGridString(snap?.second ?: "")
        val present = Array(5) { BooleanArray(5) }
        for (item in snapGrid) {
            if (item.x in 0..4 && item.y in 0..4) {
                present[item.x][item.y] = true
            }
        }
        for (r in 0..4) {
            for (c in 0..4) {
                if (present[r][c]) {
                    ages[r][c]++
                } else {
                    ages[r][c] = 0
                }
            }
        }
    }
    return ages
}

fun main() {
    val snapshots = mutableListOf<Pair<Long, String>>()
    for (i in 0..100) {
        snapshots.add(Pair(i.toLong(), "0,0,Rose|1,1,Cactus|2,2,Ivy|3,3,Fern|4,4,Maple"))
    }

    // Warmup
    for (i in 0..10) {
        originalCalculatePlantAges(100, snapshots)
        optimizedCalculatePlantAges(100, snapshots)
    }

    val timeOriginal = measureTimeMillis {
        for (i in 0..100) {
            originalCalculatePlantAges(100, snapshots)
        }
    }

    val timeOptimized = measureTimeMillis {
        for (i in 0..100) {
            optimizedCalculatePlantAges(100, snapshots)
        }
    }

    println("Original Time: $timeOriginal ms")
    println("Optimized Time: $timeOptimized ms")
}
