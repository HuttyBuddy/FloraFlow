package com.example.data.repository

import com.example.data.database.GardenDao
import com.example.data.model.GardenLayout
import com.example.data.model.MoodLog
import com.example.data.model.Plant
import kotlinx.coroutines.flow.Flow

class GardenRepository(private val gardenDao: GardenDao) {

    // --- Garden Layouts ---
    val allLayouts: Flow<List<GardenLayout>> = gardenDao.getAllLayouts()

    fun getLayoutById(id: Int): Flow<GardenLayout?> = gardenDao.getLayoutById(id)

    suspend fun insertLayout(layout: GardenLayout): Long = gardenDao.insertLayout(layout)

    suspend fun updateLayoutGrid(id: Int, newGridString: String) = gardenDao.updateLayoutGrid(id, newGridString)

    suspend fun deleteLayout(layout: GardenLayout) = gardenDao.deleteLayout(layout)

    // --- Plants ---
    fun getPlantsForLayout(layoutId: Int): Flow<List<Plant>> = gardenDao.getPlantsForLayout(layoutId)

    suspend fun insertPlant(plant: Plant): Long = gardenDao.insertPlant(plant)

    suspend fun insertPlants(plants: List<Plant>) = gardenDao.insertPlants(plants)

    suspend fun updatePlant(plant: Plant) = gardenDao.updatePlant(plant)

    suspend fun deletePlantById(plantId: Int) = gardenDao.deletePlantById(plantId)

    suspend fun deletePlantsByLayout(layoutId: Int) = gardenDao.deletePlantsByLayout(layoutId)

    // --- Mood Logs ---
    val allMoodLogs: Flow<List<MoodLog>> = gardenDao.getAllMoodLogs()

    suspend fun insertMoodLog(moodLog: MoodLog): Long = gardenDao.insertMoodLog(moodLog)

    suspend fun deleteMoodLogById(id: Int) = gardenDao.deleteMoodLogById(id)
}
