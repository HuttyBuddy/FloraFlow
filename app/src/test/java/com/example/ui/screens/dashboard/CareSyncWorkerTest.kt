package com.example.ui.screens.dashboard

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.data.database.GardenDatabase
import com.example.data.repository.WeatherRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockConstruction
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class CareSyncWorkerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testDoWorkFailureReturnsRetry() = runBlocking {
        // Mock WeatherRepository to throw an exception upon instantiation or method call
        val exception = RuntimeException("Simulated network/repository failure")

        mockConstruction(WeatherRepository::class.java) { mock, context ->
            runBlocking {
                org.mockito.Mockito.`when`(mock.fetchWeather()).thenThrow(exception)
            }
        }.use {
            val worker = TestListenableWorkerBuilder<CareSyncWorker>(this@CareSyncWorkerTest.context).build()
            val result = worker.doWork()

            assertEquals(ListenableWorker.Result.retry(), result)
        }
    }
}
