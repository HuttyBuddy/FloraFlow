package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.GardenDao
import com.example.data.database.GardenDatabase
import com.example.data.model.GardenLayout
import com.example.data.model.Plant
import com.example.data.repository.GardenRepository
import com.example.data.repository.WeatherRepository
import com.example.ui.screens.dashboard.CareScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import android.content.ContextWrapper
import android.content.Intent
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class CareSchedulerTest {

    private lateinit var context: Context
    private lateinit var db: GardenDatabase
    private lateinit var dao: GardenDao
    private lateinit var gardenRepository: GardenRepository
    private lateinit var weatherRepository: WeatherRepository
    private lateinit var scheduler: CareScheduler

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, GardenDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.gardenDao()
        gardenRepository = GardenRepository(dao)
        weatherRepository = WeatherRepository(context)
        scheduler = CareScheduler(context, gardenRepository, weatherRepository)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testGenerateMissingCareTasks() = runTest {
        // 1. Create layout
        val layout = GardenLayout(name = "Test Oasis", style = "Zen", climate = "Temperate")
        val layoutId = gardenRepository.insertLayout(layout).toInt()

        // 2. Add plant
        val plant = Plant(
            layoutId = layoutId,
            name = "Starlight Rose",
            type = "Flower",
            wateringNeeds = "Moderate" // Should yield 4 days interval
        )
        val plantId = gardenRepository.insertPlant(plant).toInt()

        // 3. Set weather to clear
        weatherRepository.simulateWeather("Clear", 22.0, 50.0)

        // 4. Sync care tasks
        scheduler.syncCareSchedules()

        // 5. Verify tasks generated
        val tasks = gardenRepository.allCareTasks.first()
        val plantTasks = tasks.filter { it.plantId == plantId }
        
        // Should generate WATER, FERTILIZE, PRUNE
        assertEquals(3, plantTasks.size)
        
        val waterTask = plantTasks.first { it.taskType == "WATER" }
        assertEquals(4, waterTask.intervalDays)
        assertTrue(waterTask.isRecurring)
    }

    @Test
    fun testRainDelaysWateringTask() = runTest {
        val layout = GardenLayout(name = "Test Oasis", style = "Zen", climate = "Temperate")
        val layoutId = gardenRepository.insertLayout(layout).toInt()
        val plant = Plant(layoutId = layoutId, name = "Fern", type = "Fern", wateringNeeds = "High") // Interval = 1 day
        val plantId = gardenRepository.insertPlant(plant).toInt()

        // 1. Set weather to Rain
        weatherRepository.simulateWeather("Rain", 15.0, 90.0)

        // 2. Sync schedules
        scheduler.syncCareSchedules()

        // 3. Verify task due date has been shifted by 1 day
        val tasks = gardenRepository.allCareTasks.first()
        val waterTask = tasks.first { it.plantId == plantId && it.taskType == "WATER" }
        
        val expectedMinDueTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(2) - TimeUnit.MINUTES.toMillis(5)
        assertTrue("Watering task should be delayed due to rain", waterTask.dueDate >= expectedMinDueTime)
    }

    @Test
    fun testCompleteTaskSchedulesNext() = runTest {
        val layout = GardenLayout(name = "Test Oasis", style = "Zen", climate = "Temperate")
        val layoutId = gardenRepository.insertLayout(layout).toInt()
        val plant = Plant(layoutId = layoutId, name = "Cactus", type = "Succulent", wateringNeeds = "Low") // Interval = 10 days
        val plantId = gardenRepository.insertPlant(plant).toInt()

        weatherRepository.simulateWeather("Clear", 25.0, 40.0)
        scheduler.syncCareSchedules()

        val initialTasks = gardenRepository.allCareTasks.first()
        val waterTask = initialTasks.first { it.plantId == plantId && it.taskType == "WATER" }

        // Complete the task
        scheduler.completeTask(waterTask)

        // Verify it shows as completed, and next task scheduled
        val updatedTasks = gardenRepository.allCareTasks.first()
        val completedTask = updatedTasks.first { it.id == waterTask.id }
        assertNotNull(completedTask.completedDate)

        val nextTask = updatedTasks.first { it.plantId == plantId && it.taskType == "WATER" && it.completedDate == null }
        val expectedNextDue = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(10)
        // Check next due time is approximately in 10 days
        assertTrue(nextTask.dueDate >= expectedNextDue - TimeUnit.MINUTES.toMillis(5))
    }

    @Test
    fun testSyncCareSchedulesIgnoresBroadcastException() = runTest {
        // Create a custom ContextWrapper that throws an exception when sendBroadcast is called
        val contextWrapper = object : ContextWrapper(context) {
            override fun sendBroadcast(intent: Intent?) {
                throw RuntimeException("Simulated broadcast exception")
            }
        }

        // Instantiate CareScheduler with the custom context
        val failingBroadcastScheduler = CareScheduler(contextWrapper, gardenRepository, weatherRepository)

        // This should not crash despite sendBroadcast throwing an exception
        try {
            failingBroadcastScheduler.syncCareSchedules()
            // If we get here, the test passed because the exception was caught in CareScheduler
        } catch (e: Exception) {
            org.junit.Assert.fail("Exception should have been caught inside syncCareSchedules(): ${e.message}")
        }
    }
}
