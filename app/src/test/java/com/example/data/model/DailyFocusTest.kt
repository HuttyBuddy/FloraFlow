package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyFocusTest {
    @Test
    fun nextTask_prioritizes_an_overdue_task_over_a_future_task() {
        val now = 1_000L
        val future = CareTask(id = 1, plantId = 1, plantName = "Pothos", taskType = "WATER", dueDate = 2_000L)
        val overdue = CareTask(id = 2, plantId = 2, plantName = "Lavender", taskType = "PRUNE", dueDate = 500L)

        assertEquals(overdue, DailyFocus.nextTask(listOf(future, overdue), now))
    }

    @Test
    fun nextTask_returns_the_earliest_due_task_when_nothing_is_overdue() {
        val early = CareTask(id = 1, plantId = 1, plantName = "Fern", taskType = "MIST", dueDate = 1_500L)
        val late = CareTask(id = 2, plantId = 2, plantName = "Rose", taskType = "WATER", dueDate = 2_000L)

        assertEquals(early, DailyFocus.nextTask(listOf(late, early), 1_000L))
    }
}
