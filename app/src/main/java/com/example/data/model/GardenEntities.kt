package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * A display-level classification derived from an existing layout style.
 * It preserves the current Room schema while allowing the product to lead
 * with the user's real space, indoors or outdoors.
 */
enum class SpaceType(val label: String) {
    INDOOR("Indoor space"),
    OUTDOOR("Outdoor space");

    companion object {
        fun fromStyle(style: String): SpaceType = when (style.trim().lowercase()) {
            "indoor area", "urban balcony" -> INDOOR
            else -> OUTDOOR
        }
    }

    fun stylesFor(styles: List<String>): List<String> = styles.filter { fromStyle(it) == this }
}

@Entity(tableName = "garden_layouts")
data class GardenLayout(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val style: String, // e.g. Indoor Area, Cottage, Desert, Urban Balcony, Sanctuary
    val climate: String, // e.g. Temperate, Arid, Tropical, Mediterranean, Mountainous
    val gridWidth: Int = 5,
    val gridHeight: Int = 5,
    val layoutDetails: String = "",
    val gridString: String = "" // e.g. "0,0,Rose|1,2,Cactus|..."
)

@Entity(tableName = "plants")
data class Plant(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val layoutId: Int, // Refers to GardenLayout
    val name: String,
    val type: String, // e.g. Flower, Shrub, Succulent, Herb, Fern
    val careSpring: String = "Water once a week, expose to partial shade.",
    val careSummer: String = "Water daily, monitor for intense afternoon sun.",
    val careAutumn: String = "Lessen watering, compost soil.",
    val careWinter: String = "Prune dead stems, protect from frost.",
    val soilType: String = "Sandy loam, well-draining",
    val sunlight: String = "Full sun to partial shade",
    val growthProgress: Int = 20, // 0 - 100
    val plantedTimestamp: Long = System.currentTimeMillis(),
    val matureSize: String = "Medium (2-3 ft)",
    val wateringNeeds: String = "Moderate",
    val bloomTime: String = "Spring - Autumn",
    val pestsDiseases: String = "Aphids, Powdery Mildew"
)

@Entity(tableName = "mood_logs", indices = [Index(value = ["timestamp"])])
data class MoodLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val mood: String, // Peaceful, Energized, Refreshed, Stressed, Overwhelmed, Happy
    val moodScore: Int, // 1 to 5 scale
    val activityMinutes: Int, // time spent gardening
    val notes: String = "",
    val growthIndex: Int = 0, // average growth progress of the garden plants during the session
    val waterCompleted: Boolean = false,
    val pruneCompleted: Boolean = false,
    val outdoorsCompleted: Boolean = false
)

@Entity(tableName = "care_tasks", indices = [Index(value = ["plantId"]), Index(value = ["dueDate"])])
data class CareTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val plantId: Int, // Refers to Plant.id
    val plantName: String,
    val taskType: String, // e.g. WATER, FERTILIZE, PRUNE, MIST
    val dueDate: Long, // timestamp
    val completedDate: Long? = null,
    val isRecurring: Boolean = true,
    val intervalDays: Int = 7
)

/** Selects the one care action that most needs the user's attention today. */
object DailyFocus {
    fun nextTask(tasks: List<CareTask>, now: Long = System.currentTimeMillis()): CareTask? =
        tasks.minWithOrNull(
            compareBy<CareTask> { if (it.dueDate <= now) 0 else 1 }
                .thenBy { it.dueDate }
        )
}

/** Finds distinct, catalog-backed plant names in Garden Counsel advice. */
object PlantRecommendationMatcher {
    fun extract(message: String, candidates: List<String>): List<String> {
        val remaining = message.lowercase()
        return candidates
            .sortedByDescending { it.length }
            .filter { remaining.contains(it.lowercase()) }
            .fold(mutableListOf()) { matches, candidate ->
                if (matches.none { existing -> existing.contains(candidate, ignoreCase = true) }) {
                    matches += candidate
                }
                matches
            }
    }
}


// Helper class for UI Grid mapping
data class GridPlantItem(
    val x: Int,
    val y: Int,
    val plantName: String
) {
    override fun toString(): String {
        return "$x,$y,$plantName"
    }

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

// Convert string database field to list of items
fun parseGridString(gridString: String): List<GridPlantItem> {
    if (gridString.isBlank()) return emptyList()
    return gridString.split("|").mapNotNull { GridPlantItem.fromString(it) }
}

// Convert list of items back to database string field
fun toGridString(items: List<GridPlantItem>): String {
    return items.joinToString("|") { it.toString() }
}

object GridPlacement {
    fun firstAvailable(items: List<GridPlantItem>, width: Int = 5, height: Int = 5): Pair<Int, Int>? {
        val occupied = items.map { it.x to it.y }.toSet()
        for (x in 0 until width) {
            for (y in 0 until height) {
                if (x to y !in occupied) return x to y
            }
        }
        return null
    }
}

@Entity(tableName = "restoration_logs")
data class RestorationLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val nriScore: Int,
    val layoutId: Int?,
    val completedTasks: String, // comma-separated list of task names
    val soundscapeTrack: String
)

/** A visible, gentle starting point for a newly created 5×5 space. */
object StarterLayout {
    fun gridFor(plantNames: List<String>): String = buildList {
        plantNames.getOrNull(0)?.let { add(GridPlantItem(0, 0, it)) }
        plantNames.getOrNull(1)?.let { add(GridPlantItem(4, 4, it)) }
    }.let(::toGridString)
}

