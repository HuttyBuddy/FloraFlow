package com.example.data.mindfulcare

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(tableName = "mindful_care_sessions", indices = [Index(value = ["timestamp"])])
data class MindfulCareSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val routineTitle: String,
    val durationSeconds: Int,
    val startingMoodScore: Int,
    val endingMoodScore: Int,
    val soundscapeUsed: String,
    val notes: String = ""
)
