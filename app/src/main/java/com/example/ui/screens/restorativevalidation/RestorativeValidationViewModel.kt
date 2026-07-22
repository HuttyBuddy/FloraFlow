package com.example.ui.screens.restorativevalidation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun interface ValidationClock {
    fun nowEpochMillis(): Long
}

internal fun interface ValidationIdProvider {
    fun nextId(): String
}

class RestorativeValidationViewModel internal constructor(
    private val store: RestorativeValidationStore,
    private val clock: ValidationClock = ValidationClock(System::currentTimeMillis),
    private val idProvider: ValidationIdProvider = ValidationIdProvider { UUID.randomUUID().toString() },
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RestorativeUiState(isBusy = true))
    val uiState: StateFlow<RestorativeUiState> = _uiState.asStateFlow()
    private var journeyCreatedAtEpochMillis: Long = 0L

    init {
        viewModelScope.launch { restoreJourney() }
    }

    fun onIntent(intent: RestorativeIntent) {
        when (intent) {
            RestorativeIntent.GeneratePlan -> generateOrAdvance()
            RestorativeIntent.SavePlan -> savePlan()
            is RestorativeIntent.Start -> start(intent.experimentId)
            is RestorativeIntent.PlanReady,
            RestorativeIntent.SaveComplete -> Unit // Completion intents are owned by this ViewModel.
            else -> updateAndPersist(intent)
        }
    }

    fun startJourney() = onIntent(RestorativeIntent.Start(idProvider.nextId()))

    private suspend fun restoreJourney() {
        _uiState.value = when (val result = store.readJourney()) {
            StoreReadResult.Missing -> RestorativeUiState()
            is StoreReadResult.Available -> stateFrom(result.value)
            is StoreReadResult.RetryableFailure -> RestorativeUiState(
                error = RestorativeError.PersistenceFailure(result.stableCode),
            )
            is StoreReadResult.TerminalFailure -> RestorativeUiState(
                error = RestorativeError.TerminalEvidenceFailure(result.stableCode),
            )
        }
    }

    private fun start(experimentId: String) {
        val previous = _uiState.value
        val updated = RestorativeReducer.reduce(previous, RestorativeIntent.Start(experimentId))
        if (updated === previous) return
        journeyCreatedAtEpochMillis = clock.nowEpochMillis()
        _uiState.value = updated
        viewModelScope.launch {
            persistDraft(updated)
            appendEvent(experimentId, "journey_started")
        }
    }

    private fun updateAndPersist(intent: RestorativeIntent) {
        val previous = _uiState.value
        val updated = RestorativeReducer.reduce(previous, intent)
        if (updated === previous || updated == previous) return
        _uiState.value = updated
        if (updated.draft.experimentId != null) {
            viewModelScope.launch { persistDraft(updated) }
        }
    }

    private fun generateOrAdvance() {
        val previous = _uiState.value
        val updated = RestorativeReducer.reduce(previous, RestorativeIntent.GeneratePlan)
        if (updated === previous || updated == previous) return
        _uiState.value = updated

        if (previous.step != RestorativeStep.PLANTS) {
            viewModelScope.launch { persistDraft(updated) }
            return
        }

        val light = updated.draft.light ?: return failInternal("PLAN_LIGHT_MISSING")
        val space = updated.draft.availableSpace ?: return failInternal("PLAN_SPACE_MISSING")
        viewModelScope.launch {
            val recommendation = withContext(workerDispatcher) {
                RestorativeRecommendationEngine.createPlan(light, space, updated.draft.ownedPlantSlugs)
            }
            when (recommendation) {
                is RecommendationResult.Match -> {
                    val planned = RestorativeReducer.reduce(
                        _uiState.value,
                        RestorativeIntent.PlanReady(recommendation.plan),
                    )
                    _uiState.value = planned
                    persistDraft(planned)
                    updated.draft.experimentId?.let { appendEvent(it, "starter_plan_created") }
                }
                RecommendationResult.NoMatch -> _uiState.value = updated.copy(
                    isBusy = false,
                    error = RestorativeError.NoMatch,
                )
            }
        }
    }

    private fun savePlan() {
        val previous = _uiState.value
        val busy = RestorativeReducer.reduce(previous, RestorativeIntent.SavePlan)
        if (busy === previous || busy == previous) return
        _uiState.value = busy
        val experimentId = busy.draft.experimentId ?: return failInternal("SAVE_EXPERIMENT_MISSING")
        val plan = busy.plan ?: return failInternal("SAVE_PLAN_MISSING")
        val now = clock.nowEpochMillis()
        val record = recordFrom(busy).copy(
            status = JourneyStatus.SAVED,
            plants = plan.plants,
            placements = plan.placements,
            intendedNextStep = "Place one plant where you can pause beside it.",
            savedAtEpochMillis = now,
        )
        viewModelScope.launch {
            when (val result = store.writeJourney(record)) {
                StoreWriteResult.Success, StoreWriteResult.Duplicate -> {
                    appendEvent(experimentId, "starter_plan_saved")
                    _uiState.value = RestorativeReducer.reduce(_uiState.value, RestorativeIntent.SaveComplete)
                }
                is StoreWriteResult.RetryableFailure -> _uiState.value = busy.copy(
                    isBusy = false,
                    error = RestorativeError.PersistenceFailure(result.stableCode),
                )
                is StoreWriteResult.TerminalFailure -> _uiState.value = busy.copy(
                    isBusy = false,
                    error = RestorativeError.TerminalEvidenceFailure(result.stableCode),
                )
            }
        }
    }

    private suspend fun persistDraft(state: RestorativeUiState) {
        val result = store.writeJourney(recordFrom(state))
        when (result) {
            StoreWriteResult.Success, StoreWriteResult.Duplicate -> Unit
            is StoreWriteResult.RetryableFailure -> _uiState.value = _uiState.value.copy(
                isBusy = false,
                error = RestorativeError.PersistenceFailure(result.stableCode),
            )
            is StoreWriteResult.TerminalFailure -> _uiState.value = _uiState.value.copy(
                isBusy = false,
                error = RestorativeError.TerminalEvidenceFailure(result.stableCode),
            )
        }
    }

    private suspend fun appendEvent(experimentId: String, name: String) {
        store.appendEvent(
            ValidationEventRecord(
                eventId = idProvider.nextId(),
                experimentId = experimentId,
                name = name,
                deduplicationKey = validationEventDeduplicationKey(experimentId, name),
                timestampEpochMillis = clock.nowEpochMillis(),
            )
        )
    }

    private fun stateFrom(record: RestorativeJourneyRecord): RestorativeUiState {
        journeyCreatedAtEpochMillis = record.createdAtEpochMillis
        val plan = if (record.plants.isNotEmpty() && record.placements.isNotEmpty()) {
            StarterPlan(record.plants, record.placements)
        } else null
        val step = when {
            record.status == JourneyStatus.SAVED -> RestorativeStep.SAVED
            plan != null -> RestorativeStep.PLAN
            record.inputMode != null -> RestorativeStep.PLANTS
            record.availableSpace != null -> RestorativeStep.PLANTS
            record.light != null -> RestorativeStep.SPACE
            else -> RestorativeStep.LIGHT
        }
        return RestorativeUiState(
            step = step,
            draft = RestorativeDraft(
                experimentId = record.experimentId,
                light = record.light,
                availableSpace = record.availableSpace,
                inputMode = record.inputMode,
                ownedPlantSlugs = record.plants.filter { it.source == PlantSource.OWNED }.map { it.slug },
            ),
            plan = plan,
        )
    }

    private fun recordFrom(state: RestorativeUiState): RestorativeJourneyRecord {
        val experimentId = requireNotNull(state.draft.experimentId)
        val selectedOwned = state.draft.ownedPlantSlugs.mapNotNull { slug ->
            ValidationPlantCatalog.find(slug)?.let {
                PlantSelectionRecord(it.slug, it.canonicalName, PlantSource.OWNED)
            }
        }
        return RestorativeJourneyRecord(
            experimentId = experimentId,
            status = JourneyStatus.DRAFT,
            light = state.draft.light,
            availableSpace = state.draft.availableSpace,
            inputMode = state.draft.inputMode,
            plants = state.plan?.plants ?: selectedOwned,
            placements = state.plan?.placements.orEmpty(),
            createdAtEpochMillis = journeyCreatedAtEpochMillis.takeIf { it > 0L } ?: clock.nowEpochMillis(),
        )
    }

    private fun failInternal(code: String) {
        _uiState.value = _uiState.value.copy(
            isBusy = false,
            error = RestorativeError.InternalFailure(code),
        )
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(RestorativeValidationViewModel::class.java))
                return RestorativeValidationViewModel(
                    store = RestorativeValidationStoreProvider.get(context),
                ) as T
            }
        }
    }
}
