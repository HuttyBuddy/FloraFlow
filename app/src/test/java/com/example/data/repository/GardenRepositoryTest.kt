package com.example.data.repository

import com.example.data.database.GardenDao
import com.example.data.model.AssessmentResult
import com.example.data.model.CareTask
import com.example.data.model.GardenLayout
import com.example.data.model.MoodLog
import com.example.data.model.Plant
import com.example.data.model.RestorationLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class GardenRepositoryTest {

    private lateinit var gardenDao: GardenDao
    private lateinit var repository: GardenRepository

    @Before
    fun setUp() {
        gardenDao = mock()
        whenever(gardenDao.getAllLayouts()).thenReturn(flowOf(emptyList<GardenLayout>()))
        whenever(gardenDao.getAllPlants()).thenReturn(flowOf(emptyList<Plant>()))
        whenever(gardenDao.getAllMoodLogs()).thenReturn(flowOf(emptyList<MoodLog>()))
        whenever(gardenDao.getAllCareTasks()).thenReturn(flowOf(emptyList<CareTask>()))
        whenever(gardenDao.getPendingCareTasks()).thenReturn(flowOf(emptyList<CareTask>()))
        whenever(gardenDao.getAllRestorationLogs()).thenReturn(flowOf(emptyList<RestorationLog>()))
        whenever(gardenDao.getAllAssessmentResults()).thenReturn(flowOf(emptyList<AssessmentResult>()))
        repository = GardenRepository(gardenDao)
    }

    @Test
    fun testAllLayouts() = runBlocking {
        val layouts = listOf(
            GardenLayout(id = 1, name = "Garden 1", style = "Cottage", climate = "Temperate"),
            GardenLayout(id = 2, name = "Garden 2", style = "Desert", climate = "Arid")
        )
        whenever(gardenDao.getAllLayouts()).thenReturn(flowOf(layouts))
        repository = GardenRepository(gardenDao)
        val result = repository.allLayouts.first()
        assertEquals(2, result.size)
        assertEquals("Garden 1", result[0].name)
    }

    @Test
    fun testGetLayoutById() = runBlocking {
        val layout = GardenLayout(id = 1, name = "Garden 1", style = "Cottage", climate = "Temperate")
        whenever(gardenDao.getLayoutById(1)).thenReturn(flowOf(layout))

        val result = repository.getLayoutById(1).first()
        assertEquals("Garden 1", result?.name)
    }

    @Test
    fun testInsertLayout() = runBlocking {
        val layout = GardenLayout(id = 1, name = "Garden 1", style = "Cottage", climate = "Temperate")
        whenever(gardenDao.insertLayout(layout)).thenReturn(1L)

        val result = repository.insertLayout(layout)
        assertEquals(1L, result)
        verify(gardenDao).insertLayout(layout)
        Unit
    }

    @Test
    fun testUpdateLayoutGrid() = runBlocking {
        repository.updateLayoutGrid(1, "0,0,Rose")
        verify(gardenDao).updateLayoutGrid(1, "0,0,Rose")
        Unit
    }

    @Test
    fun testUpdateLayoutClimate() = runBlocking {
        repository.updateLayoutClimate(1, "Tropical")
        verify(gardenDao).updateLayoutClimate(1, "Tropical")
        Unit
    }

    @Test
    fun testDeleteLayout() = runBlocking {
        val layout = GardenLayout(id = 1, name = "Garden 1", style = "Cottage", climate = "Temperate")
        repository.deleteLayout(layout)
        verify(gardenDao).deleteLayout(layout)
        Unit
    }

    @Test
    fun testAllPlants() = runBlocking {
        val plants = listOf(
            Plant(id = 1, layoutId = 1, name = "Rose", type = "Flower"),
            Plant(id = 2, layoutId = 1, name = "Tulip", type = "Flower")
        )
        whenever(gardenDao.getAllPlants()).thenReturn(flowOf(plants))
        repository = GardenRepository(gardenDao)
        val result = repository.allPlants.first()
        assertEquals(2, result.size)
        assertEquals("Rose", result[0].name)
    }

    @Test
    fun testGetPlantsForLayout() = runBlocking {
        val plants = listOf(
            Plant(id = 1, layoutId = 1, name = "Rose", type = "Flower")
        )
        whenever(gardenDao.getPlantsForLayout(1)).thenReturn(flowOf(plants))

        val result = repository.getPlantsForLayout(1).first()
        assertEquals(1, result.size)
        assertEquals("Rose", result[0].name)
    }

    @Test
    fun testInsertPlant() = runBlocking {
        val plant = Plant(id = 1, layoutId = 1, name = "Rose", type = "Flower")
        whenever(gardenDao.insertPlant(plant)).thenReturn(1L)

        val result = repository.insertPlant(plant)
        assertEquals(1L, result)
        verify(gardenDao).insertPlant(plant)
        Unit
    }

    @Test
    fun testInsertPlants() = runBlocking {
        val plants = listOf(Plant(id = 1, layoutId = 1, name = "Rose", type = "Flower"))
        repository.insertPlants(plants)
        verify(gardenDao).insertPlants(plants)
        Unit
    }

    @Test
    fun testUpdatePlant() = runBlocking {
        val plant = Plant(id = 1, layoutId = 1, name = "Rose", type = "Flower")
        repository.updatePlant(plant)
        verify(gardenDao).updatePlant(plant)
        Unit
    }

    @Test
    fun testDeletePlantById() = runBlocking {
        repository.deletePlantById(1)
        verify(gardenDao).deletePlantById(1)
        Unit
    }

    @Test
    fun testDeletePlantsByLayout() = runBlocking {
        repository.deletePlantsByLayout(1)
        verify(gardenDao).deletePlantsByLayout(1)
        Unit
    }

    @Test
    fun testAllMoodLogs() = runBlocking {
        val logs = listOf(
            MoodLog(id = 1, mood = "Happy", moodScore = 5, activityMinutes = 30)
        )
        whenever(gardenDao.getAllMoodLogs()).thenReturn(flowOf(logs))
        repository = GardenRepository(gardenDao)
        val result = repository.allMoodLogs.first()
        assertEquals(1, result.size)
        assertEquals("Happy", result[0].mood)
    }

    @Test
    fun testInsertMoodLog() = runBlocking {
        val log = MoodLog(id = 1, mood = "Happy", moodScore = 5, activityMinutes = 30)
        whenever(gardenDao.insertMoodLog(log)).thenReturn(1L)

        val result = repository.insertMoodLog(log)
        assertEquals(1L, result)
        verify(gardenDao).insertMoodLog(log)
        Unit
    }

    @Test
    fun testDeleteMoodLogById() = runBlocking {
        repository.deleteMoodLogById(1)
        verify(gardenDao).deleteMoodLogById(1)
        Unit
    }

    @Test
    fun testAllCareTasks() = runBlocking {
        val tasks = listOf(
            CareTask(id = 1, plantId = 1, plantName = "Rose", taskType = "WATER", dueDate = 1000L)
        )
        whenever(gardenDao.getAllCareTasks()).thenReturn(flowOf(tasks))
        repository = GardenRepository(gardenDao)
        val result = repository.allCareTasks.first()
        assertEquals(1, result.size)
        assertEquals("WATER", result[0].taskType)
    }

    @Test
    fun testPendingCareTasks() = runBlocking {
        val tasks = listOf(
            CareTask(id = 1, plantId = 1, plantName = "Rose", taskType = "WATER", dueDate = 1000L)
        )
        whenever(gardenDao.getPendingCareTasks()).thenReturn(flowOf(tasks))
        repository = GardenRepository(gardenDao)
        val result = repository.pendingCareTasks.first()
        assertEquals(1, result.size)
        assertEquals("WATER", result[0].taskType)
    }

    @Test
    fun testGetCareTasksForPlant() = runBlocking {
        val tasks = listOf(
            CareTask(id = 1, plantId = 1, plantName = "Rose", taskType = "WATER", dueDate = 1000L)
        )
        whenever(gardenDao.getCareTasksForPlant(1)).thenReturn(flowOf(tasks))

        val result = repository.getCareTasksForPlant(1).first()
        assertEquals(1, result.size)
        assertEquals("WATER", result[0].taskType)
    }

    @Test
    fun testInsertCareTask() = runBlocking {
        val task = CareTask(id = 1, plantId = 1, plantName = "Rose", taskType = "WATER", dueDate = 1000L)
        whenever(gardenDao.insertCareTask(task)).thenReturn(1L)

        val result = repository.insertCareTask(task)
        assertEquals(1L, result)
        verify(gardenDao).insertCareTask(task)
        Unit
    }

    @Test
    fun testInsertCareTasks() = runBlocking {
        val tasks = listOf(CareTask(id = 1, plantId = 1, plantName = "Rose", taskType = "WATER", dueDate = 1000L))
        repository.insertCareTasks(tasks)
        verify(gardenDao).insertCareTasks(tasks)
        Unit
    }

    @Test
    fun testUpdateCareTask() = runBlocking {
        val task = CareTask(id = 1, plantId = 1, plantName = "Rose", taskType = "WATER", dueDate = 1000L)
        repository.updateCareTask(task)
        verify(gardenDao).updateCareTask(task)
        Unit
    }

    @Test
    fun testCompleteCareTask() = runBlocking {
        repository.completeCareTask(1, 2000L)
        verify(gardenDao).completeCareTask(1, 2000L)
        Unit
    }

    @Test
    fun testDeleteCareTaskById() = runBlocking {
        repository.deleteCareTaskById(1)
        verify(gardenDao).deleteCareTaskById(1)
        Unit
    }

    @Test
    fun testDeleteCareTasksByPlant() = runBlocking {
        repository.deleteCareTasksByPlant(1)
        verify(gardenDao).deleteCareTasksByPlant(1)
        Unit
    }

    @Test
    fun testDeleteCareTasksByLayout() = runBlocking {
        repository.deleteCareTasksByLayout(1)
        verify(gardenDao).deleteCareTasksByLayout(1)
        Unit
    }

    @Test
    fun testAllRestorationLogs() = runBlocking {
        val logs = listOf(RestorationLog(id = 1, nriScore = 5, layoutId = 1, completedTasks = "", soundscapeTrack = ""))
        whenever(gardenDao.getAllRestorationLogs()).thenReturn(flowOf(logs))
        repository = GardenRepository(gardenDao)
        val result = repository.allRestorationLogs.first()
        assertEquals(1, result.size)
    }

    @Test
    fun testInsertRestorationLog() = runBlocking {
        val log = RestorationLog(id = 1, nriScore = 5, layoutId = 1, completedTasks = "", soundscapeTrack = "")
        whenever(gardenDao.insertRestorationLog(log)).thenReturn(1L)
        val result = repository.insertRestorationLog(log)
        assertEquals(1L, result)
        verify(gardenDao).insertRestorationLog(log)
        Unit
    }

    @Test
    fun testAllAssessmentResults() = runBlocking {
        val results = listOf(AssessmentResult(id = 1, score = 100, lowestCategories = "None"))
        whenever(gardenDao.getAllAssessmentResults()).thenReturn(flowOf(results))
        repository = GardenRepository(gardenDao)
        val result = repository.allAssessmentResults.first()
        assertEquals(1, result.size)
    }

    @Test
    fun testInsertAssessmentResult() = runBlocking {
        val res = AssessmentResult(id = 1, score = 100, lowestCategories = "None")
        whenever(gardenDao.insertAssessmentResult(res)).thenReturn(1L)
        val result = repository.insertAssessmentResult(res)
        assertEquals(1L, result)
        verify(gardenDao).insertAssessmentResult(res)
        Unit
    }
}
