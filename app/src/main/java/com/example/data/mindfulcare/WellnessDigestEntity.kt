package com.example.data.mindfulcare

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(tableName = "wellness_digests", indices = [Index(value = ["weekStartDate"])])
data class WellnessDigestEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val weekStartDate: Long,
    val cognitiveLoadReductionPct: Int,
    val streakDaysCount: Int,
    val totalCareMinutes: Int,
    val averageNriScore: Int,
    val summaryHighlights: String
)
