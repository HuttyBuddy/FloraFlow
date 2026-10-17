package com.example.ui.viewmodel

import android.os.Build
import com.example.data.database.GardenDatabase
import com.example.data.model.Plant
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Before

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class PerformanceTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun benchmarkBatchVsSingleInsert() = runBlocking {
        val context = RuntimeEnvironment.getApplication()

        // Use an actual in-memory database
        val db = androidx.room.Room.inMemoryDatabaseBuilder(
            context, GardenDatabase::class.java
        ).allowMainThreadQueries().build()

        val dao = db.gardenDao()

        // Generate list
        val list = mutableListOf<Plant>()
        for(i in 1..500) {
            list.add(Plant(
                layoutId = 1,
                name = "Plant " + i,
                type = "Type " + i,
                soilType = "Soil " + i,
                sunlight = "Sun " + i,
                growthProgress = 15,
                matureSize = "Medium",
                wateringNeeds = "Moderate",
                bloomTime = "Summer",
                pestsDiseases = "Minor pests"
            ))
        }

        // Test N+1
        val timeNPlusOne = measureTimeMillis {
            for(plant in list) {
                 dao.insertPlant(plant)
            }
        }

        // Clear DB
        dao.deletePlantsByLayout(1)

        // Test Batched
        val timeBatched = measureTimeMillis {
            dao.insertPlants(list)
        }

        println("BENCHMARK_RESULT_N_PLUS_ONE: " + timeNPlusOne + " ms")
        println("BENCHMARK_RESULT_BATCHED: " + timeBatched + " ms")

        db.close()
    }
}
