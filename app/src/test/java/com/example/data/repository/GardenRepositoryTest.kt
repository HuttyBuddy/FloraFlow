package com.example.data.repository

import com.example.data.database.GardenDao
import com.example.data.model.Plant
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GardenRepositoryTest {

    private lateinit var gardenDao: GardenDao
    private lateinit var repository: GardenRepository

    @Before
    fun setUp() {
        gardenDao = mock()
        repository = GardenRepository(gardenDao)
    }

    @Test
    fun getPlantsForLayout_returnsFlowFromDao() = runBlocking {
        val layoutId = 1
        val mockPlants = listOf<Plant>(mock(), mock())

        whenever(gardenDao.getPlantsForLayout(layoutId)).thenReturn(flowOf(mockPlants))

        val result = repository.getPlantsForLayout(layoutId).first()

        assertEquals(2, result.size)
    }
}
