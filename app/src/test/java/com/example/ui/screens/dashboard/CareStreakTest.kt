package com.example.ui.screens.dashboard

import com.example.data.model.MoodLog
import com.example.ui.screens.dashboard.components.calculateStreak
import com.example.ui.screens.dashboard.components.calculateStreakFromTimestamps
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * The care streak is derived from what the user actually did — completed care tasks and
 * logged rituals — rather than from a "Log care" button that incremented a counter on
 * every tap and started from a hardcoded 3.
 */
class CareStreakTest {

    private fun daysAgo(days: Long): Long =
        LocalDate.now(ZoneId.systemDefault())
            .minusDays(days)
            .atTime(12, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    private fun moodLog(days: Long) = MoodLog(
        mood = "Peaceful",
        moodScore = 5,
        activityMinutes = 3,
        notes = "[Ritual: General Deep Breaths]",
        timestamp = daysAgo(days)
    )

    @Test
    fun noActivity_isZero() {
        assertEquals(0, calculateStreak(emptyList()))
    }

    @Test
    fun ritualsOnConsecutiveDays_countUp() {
        val logs = listOf(moodLog(0), moodLog(1), moodLog(2))
        assertEquals(3, calculateStreak(logs))
    }

    /** Watering a plant is care, so a completed task alone must sustain a streak. */
    @Test
    fun completedCareTasksAloneSustainStreak() {
        val completions = listOf(daysAgo(0), daysAgo(1), daysAgo(2))
        assertEquals(3, calculateStreak(emptyList(), completions))
    }

    /** A task and a ritual on the same day are one day of care, not two. */
    @Test
    fun taskAndRitualOnSameDayCountOnce() {
        val logs = listOf(moodLog(0), moodLog(1))
        val completions = listOf(daysAgo(0), daysAgo(1))
        assertEquals(2, calculateStreak(logs, completions))
    }

    /** Mixed sources should chain across each other to form one continuous run. */
    @Test
    fun tasksAndRitualsInterleaveIntoOneStreak() {
        val logs = listOf(moodLog(0), moodLog(2))
        val completions = listOf(daysAgo(1), daysAgo(3))
        assertEquals(4, calculateStreak(logs, completions))
    }

    @Test
    fun gapBreaksStreak() {
        val logs = listOf(moodLog(0), moodLog(1), moodLog(4), moodLog(5))
        assertEquals(2, calculateStreak(logs))
    }

    /** A streak stays alive through today until yesterday's activity ages out. */
    @Test
    fun yesterdayOnlyStillCounts() {
        assertEquals(1, calculateStreak(listOf(moodLog(1))))
    }

    @Test
    fun staleActivityResetsToZero() {
        assertEquals(0, calculateStreak(listOf(moodLog(2), moodLog(3))))
    }

    @Test
    fun duplicateTimestampsOnOneDayCountOnce() {
        val sameDay = listOf(daysAgo(0), daysAgo(0), daysAgo(0))
        assertEquals(1, calculateStreakFromTimestamps(sameDay))
    }
}
