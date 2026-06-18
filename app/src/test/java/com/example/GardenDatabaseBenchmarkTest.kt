package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.GardenDao
import com.example.data.database.GardenDatabase
import com.example.data.model.Plant
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.system.measureTimeMillis

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class GardenDatabaseBenchmarkTest {

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
    fun benchmarkInsertPlantsBatch() = runTest {
        val layoutId = 1
        val plants = (1..1000).map { i ->
            Plant(
                layoutId = layoutId,
                name = "Plant $i",
                type = "Type",
                growthProgress = 10
            )
        }

        val time = measureTimeMillis {
            dao.insertPlants(plants)
        }
        println("Batch Insert Time: $time ms")
    }
}
