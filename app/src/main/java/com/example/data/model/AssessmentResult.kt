package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assessment_results")
data class AssessmentResult(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val score: Int,
    val lowestCategories: String, // Comma-separated list of categories
    val layoutId: Int? = null
)

object AssessmentScope {
    fun latestForLayout(results: List<AssessmentResult>, layoutId: Int?): AssessmentResult? =
        layoutId?.let { id -> results.filter { it.layoutId == id }.maxByOrNull { it.timestamp } }
}
