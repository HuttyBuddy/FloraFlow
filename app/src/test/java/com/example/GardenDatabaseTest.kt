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
            name = "Zen Oasis",
            style = "Zen",
            climate = "Temperate",
            gridString = "0,0,Bonsai"
        )
        val id = dao.insertLayout(layout)
        val layouts = dao.getAllLayouts().first()
        
        assertEquals(1, layouts.size)
        assertEquals("Zen Oasis", layouts[0].name)
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
            notes = "Beautiful morning session"
        )

        val logId = dao.insertMoodLog(moodLog).toInt()
        val logs = dao.getAllMoodLogs().first()

        assertEquals(1, logs.size)
        assertEquals("Refreshed", logs[0].mood)
        assertEquals(5, logs[0].moodScore)
        assertEquals("Beautiful morning session", logs[0].notes)

        dao.deleteMoodLogById(logId)
        val currentLogs = dao.getAllMoodLogs().first()
        assertTrue(currentLogs.isEmpty())
    }
}
