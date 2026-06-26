package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.GardenDao
import com.example.data.database.GardenDatabase
import com.example.data.model.GardenLayout
import com.example.data.model.MoodLog
import com.example.data.model.Plant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32]) // Target safe generic Android resources SDK
class GardenDatabaseTest {

    private lateinit var db: GardenDatabase
    private lateinit var dao: GardenDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GardenDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.gardenDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndRetrieveLayout() = runTest {
        val layout = GardenLayout(
            name = "Indoor Oasis",
            style = "Indoor Area",
            climate = "Temperate",
            gridString = "0,0,Bonsai"
        )
        val id = dao.insertLayout(layout)
        val layouts = dao.getAllLayouts().first()
        
        assertEquals(1, layouts.size)
        assertEquals("Indoor Oasis", layouts[0].name)
        assertEquals(id.toInt(), layouts[0].id)
    }

    @Test
    fun updateLayoutGridString() = runTest {
        val layout = GardenLayout(
            name = "Cottage Woods",
            style = "Cottage",
            climate = "Tropical",
            gridString = ""
        )
        val id = dao.insertLayout(layout).toInt()
        
        val updatedGrid = "0,0,Rose|1,1,Fern"
        dao.updateLayoutGrid(id, updatedGrid)
        
        val retrieved = dao.getLayoutById(id).first()
        assertNotNull(retrieved)
        assertEquals(updatedGrid, retrieved?.gridString)
    }

    @Test
    fun deleteLayoutCascadeBehavior() = runTest {
        val layout = GardenLayout(
            name = "Urban Balcony",
            style = "Urban",
            climate = "Mediterranean"
        )
        val layoutId = dao.insertLayout(layout).toInt()
        val retrievedBefore = dao.getLayoutById(layoutId).first()
        assertNotNull(retrievedBefore)

        dao.deleteLayout(retrievedBefore!!)
        
        val retrievedAfter = dao.getLayoutById(layoutId).first()
        assertNull(retrievedAfter)
    }

    @Test
    fun testPlantLifecycleForLayout() = runTest {
        val layoutId = 42
        val plant1 = Plant(
            layoutId = layoutId,
            name = "Peace Lily",
            type = "Flower",
            growthProgress = 30
        )
        val plant2 = Plant(
            layoutId = layoutId,
            name = "Aloe Vera",
            type = "Succulent",
            growthProgress = 50
        )

        dao.insertPlant(plant1)
        dao.insertPlant(plant2)

        val plants = dao.getPlantsForLayout(layoutId).first()
        assertEquals(2, plants.size)
        assertEquals("Peace Lily", plants[0].name)
        assertEquals("Aloe Vera", plants[1].name)

        // Update growth progress
        val updatedPlant = plants[0].copy(growthProgress = 95)
        dao.updatePlant(updatedPlant)

        val plantsAfterUpdate = dao.getPlantsForLayout(layoutId).first()
        assertEquals(95, plantsAfterUpdate[0].growthProgress)

        // Delete one plant by Id
        dao.deletePlantById(plantsAfterUpdate[1].id)
        val plantsAfterDeleteOne = dao.getPlantsForLayout(layoutId).first()
        assertEquals(1, plantsAfterDeleteOne.size)
        assertEquals("Peace Lily", plantsAfterDeleteOne[0].name)

        // Delete all remaining plants for the layout
        dao.deletePlantsByLayout(layoutId)
        val emptyPlantsList = dao.getPlantsForLayout(layoutId).first()
        assertTrue(emptyPlantsList.isEmpty())
    }

    @Test
    fun moodLogLifecycle() = runTest {
        val moodLog = MoodLog(
            mood = "Refreshed",
            moodScore = 5,
            activityMinutes = 20,
            notes = "Beautiful morning session",
            waterCompleted = true,
            pruneCompleted = false,
            outdoorsCompleted = true
        )

        val logId = dao.insertMoodLog(moodLog).toInt()
        val logs = dao.getAllMoodLogs().first()

        assertEquals(1, logs.size)
        assertEquals("Refreshed", logs[0].mood)
        assertEquals(5, logs[0].moodScore)
        assertEquals("Beautiful morning session", logs[0].notes)
        assertTrue(logs[0].waterCompleted)
        assertFalse(logs[0].pruneCompleted)
        assertTrue(logs[0].outdoorsCompleted)

        dao.deleteMoodLogById(logId)
        val currentLogs = dao.getAllMoodLogs().first()
        assertTrue(currentLogs.isEmpty())
    }

    @Test
    fun benchmarkPlantInsertion() = runTest {
        val layoutId = 99
        val plants = (1..50).map { i ->
            Plant(
                layoutId = layoutId,
                name = "Plant $i",
                type = "TestType"
            )
        }

        val startTime = System.currentTimeMillis()
        for (plant in plants) {
            dao.insertPlant(plant)
        }
        val endTime = System.currentTimeMillis()
        println("Baseline individual insert time for 50 plants: ${endTime - startTime} ms")

        val startBatchTime = System.currentTimeMillis()
        dao.insertPlants(plants)
        val endBatchTime = System.currentTimeMillis()
        println("Optimized batch insert time for 50 plants: ${endBatchTime - startBatchTime} ms")
    }

    @Test
    fun benchmarkGetAllPlants() = runTest {
        // Setup data: 10 layouts, 10 plants each
        val layoutIds = (1..10).map { i ->
            dao.insertLayout(GardenLayout(name = "Layout $i", style = "Test", climate = "Test")).toInt()
        }
        val plants = mutableListOf<Plant>()
        for (layoutId in layoutIds) {
            for (i in 1..10) {
                plants.add(Plant(layoutId = layoutId, name = "Plant $i in $layoutId", type = "Test"))
            }
        }
        dao.insertPlants(plants)

        // Benchmark N+1 Query Approach
        val startNPlusOneTime = System.currentTimeMillis()
        val layouts = dao.getAllLayouts().first()
        val nPlusOnePlants = mutableListOf<Plant>()
        for (layout in layouts) {
            val layoutPlants = dao.getPlantsForLayout(layout.id).first()
            nPlusOnePlants.addAll(layoutPlants)
        }
        val endNPlusOneTime = System.currentTimeMillis()
        val timeNPlusOne = endNPlusOneTime - startNPlusOneTime

        // Benchmark Single Query Approach
        val startSingleQueryTime = System.currentTimeMillis()
        val singleQueryPlants = dao.getAllPlants().first()
        val endSingleQueryTime = System.currentTimeMillis()
        val timeSingleQuery = endSingleQueryTime - startSingleQueryTime

        println("Baseline N+1 Query time to fetch 100 plants: $timeNPlusOne ms")
        println("Optimized Single Query time to fetch 100 plants: $timeSingleQuery ms")

        // Assert correctness
        assertEquals(100, nPlusOnePlants.size)
        assertEquals(100, singleQueryPlants.size)
    }
}
