package com.example.ui.screens.restorativevalidation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RestorativeValidationViewModelTest {
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `missing journey opens promise then persists a stable experiment`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val store = FakeStore()
        val ids = ArrayDeque(listOf("experiment-1", "event-1"))
        val viewModel = RestorativeValidationViewModel(
            store = store,
            clock = ValidationClock { 100L },
            idProvider = ValidationIdProvider { ids.removeFirst() },
            workerDispatcher = dispatcher,
        )

        advanceUntilIdle()
        assertEquals(RestorativeStep.PROMISE, viewModel.uiState.value.step)

        viewModel.startJourney()
        advanceUntilIdle()

        assertEquals(RestorativeStep.LIGHT, viewModel.uiState.value.step)
        assertEquals("experiment-1", store.journey?.experimentId)
        assertEquals(100L, store.journey?.createdAtEpochMillis)
        assertEquals(listOf("journey_started"), store.events.map { it.name })
    }

    @Test
    fun `complete journey creates and saves a deterministic starter plan`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val store = FakeStore()
        var id = 0
        val viewModel = RestorativeValidationViewModel(
            store = store,
            clock = ValidationClock { 200L },
            idProvider = ValidationIdProvider { "id-${++id}" },
            workerDispatcher = dispatcher,
        )
        advanceUntilIdle()

        viewModel.startJourney()
        viewModel.onIntent(RestorativeIntent.SelectLight(LightChoice.MEDIUM))
        viewModel.onIntent(RestorativeIntent.GeneratePlan)
        viewModel.onIntent(RestorativeIntent.SelectSpace(AvailableSpace.SMALL_CORNER))
        viewModel.onIntent(RestorativeIntent.GeneratePlan)
        viewModel.onIntent(RestorativeIntent.SelectInputMode(InputMode.RECOMMEND_FROM_SCRATCH))
        viewModel.onIntent(RestorativeIntent.GeneratePlan)
        advanceUntilIdle()

        assertEquals(RestorativeStep.PLAN, viewModel.uiState.value.step)
        assertNotNull(viewModel.uiState.value.plan)
        assertFalse(viewModel.uiState.value.isBusy)

        viewModel.onIntent(RestorativeIntent.SavePlan)
        advanceUntilIdle()

        assertEquals(RestorativeStep.SAVED, viewModel.uiState.value.step)
        assertEquals(JourneyStatus.SAVED, store.journey?.status)
        assertEquals(200L, store.journey?.savedAtEpochMillis)
        assertTrue(store.events.any { it.name == "starter_plan_created" })
        assertTrue(store.events.any { it.name == "starter_plan_saved" })
    }

    @Test
    fun `saved journey restores directly to its return destination`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val plan = RestorativeRecommendationEngine.createPlan(
            light = LightChoice.BRIGHT,
            availableSpace = AvailableSpace.TABLETOP,
            ownedPlantSlugs = emptyList(),
        ) as RecommendationResult.Match
        val store = FakeStore(
            journey = RestorativeJourneyRecord(
                experimentId = "returning",
                status = JourneyStatus.SAVED,
                light = LightChoice.BRIGHT,
                availableSpace = AvailableSpace.TABLETOP,
                inputMode = InputMode.RECOMMEND_FROM_SCRATCH,
                plants = plan.plan.plants,
                placements = plan.plan.placements,
                createdAtEpochMillis = 10L,
                savedAtEpochMillis = 20L,
            )
        )

        val viewModel = RestorativeValidationViewModel(
            store = store,
            workerDispatcher = dispatcher,
        )
        advanceUntilIdle()

        assertEquals(RestorativeStep.SAVED, viewModel.uiState.value.step)
        assertEquals(plan.plan, viewModel.uiState.value.plan)
    }

    private class FakeStore(
        var journey: RestorativeJourneyRecord? = null,
    ) : RestorativeValidationStore {
        val events = mutableListOf<ValidationEventRecord>()

        override suspend fun readJourney(): StoreReadResult<RestorativeJourneyRecord> =
            journey?.let { StoreReadResult.Available(it) } ?: StoreReadResult.Missing

        override suspend fun writeJourney(record: RestorativeJourneyRecord): StoreWriteResult {
            journey = record
            return StoreWriteResult.Success
        }

        override suspend fun readLedger(): StoreReadResult<ValidationEventLedger> =
            StoreReadResult.Missing

        override suspend fun appendEvent(event: ValidationEventRecord): StoreWriteResult {
            if (events.any { it.deduplicationKey == event.deduplicationKey }) return StoreWriteResult.Duplicate
            events += event
            return StoreWriteResult.Success
        }

        override suspend fun prepareExport(exportedAtEpochMillis: Long): PrepareExportResult =
            PrepareExportResult.TerminalFailure("NOT_USED")
    }
}
