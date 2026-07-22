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
        assertEquals(listOf("restorative_space_started"), store.events.map { it.name })
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
        assertTrue(store.events.any { it.name == "starter_plan_rendered" })
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

    @Test
    fun `retryable load can recover without creating a new experiment`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val store = FakeStore().apply {
            readResultOverride = StoreReadResult.RetryableFailure("JOURNEY_READ_IO")
        }
        val viewModel = RestorativeValidationViewModel(store = store, workerDispatcher = dispatcher)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.error is RestorativeError.PersistenceFailure)

        store.readResultOverride = null
        viewModel.onIntent(RestorativeIntent.RetryLoad)
        advanceUntilIdle()

        assertEquals(RestorativeStep.PROMISE, viewModel.uiState.value.step)
        assertEquals(null, viewModel.uiState.value.error)
        assertTrue(store.events.isEmpty())
    }

    @Test
    fun `discard removes the draft and requests exit`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        var id = 0
        val store = FakeStore()
        val viewModel = RestorativeValidationViewModel(
            store = store,
            idProvider = ValidationIdProvider { "id-${++id}" },
            workerDispatcher = dispatcher,
        )
        advanceUntilIdle()
        viewModel.startJourney()
        advanceUntilIdle()

        viewModel.onIntent(RestorativeIntent.DiscardDraftAndExit)
        advanceUntilIdle()

        assertEquals(null, store.journey)
        assertTrue(store.events.isEmpty())
        assertTrue(viewModel.uiState.value.exitRequested)
    }

    @Test
    fun `saved return separates passive reopen from deliberate next step`() = runTest {
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
                experimentSpaceId = "space-1",
                status = JourneyStatus.SAVED,
                light = LightChoice.BRIGHT,
                availableSpace = AvailableSpace.TABLETOP,
                inputMode = InputMode.RECOMMEND_FROM_SCRATCH,
                plants = plan.plan.plants,
                placements = plan.plan.placements,
                createdAtEpochMillis = 10L,
                savedAtEpochMillis = 100L,
            )
        )
        var id = 0
        val viewModel = RestorativeValidationViewModel(
            store = store,
            clock = ValidationClock { 200L },
            idProvider = ValidationIdProvider { "event-${++id}" },
            workerDispatcher = dispatcher,
        )
        advanceUntilIdle()

        assertEquals(listOf("app_reopened_72h"), store.events.map { it.name })
        viewModel.onIntent(RestorativeIntent.SelectNextStep("Move a chair into the corner"))
        viewModel.onIntent(RestorativeIntent.OpenPlacementGuidance)
        advanceUntilIdle()

        assertEquals("Move a chair into the corner", store.journey?.intendedNextStep)
        assertTrue(store.journey?.nextStepDecisionRecorded == true)
        assertEquals(2, store.events.count { it.name == "meaningful_return_72h" })
        assertTrue(store.events.filter { it.name == "meaningful_return_72h" }.all {
            it.properties["researcher_prompted"] == "false"
        })
    }

    private class FakeStore(
        var journey: RestorativeJourneyRecord? = null,
    ) : RestorativeValidationStore {
        val events = mutableListOf<ValidationEventRecord>()
        var readResultOverride: StoreReadResult<RestorativeJourneyRecord>? = null

        override suspend fun readJourney(): StoreReadResult<RestorativeJourneyRecord> =
            readResultOverride ?: journey?.let { StoreReadResult.Available(it) } ?: StoreReadResult.Missing

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

        override suspend fun discardDraft(): StoreWriteResult {
            journey = null
            events.clear()
            return StoreWriteResult.Success
        }
    }
}
