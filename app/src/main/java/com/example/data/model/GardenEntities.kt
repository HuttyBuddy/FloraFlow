package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

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
    val growthIndex: Int = 0 // average growth progress of the garden plants during the session
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

@Entity(tableName = "restoration_logs")
data class RestorationLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val nriScore: Int,
    val layoutId: Int?,
    val completedTasks: String, // comma-separated list of task names
    val soundscapeTrack: String
)

