package com.example.ui.screens.restorativevalidation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
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
    private val documentWriter: ExportDocumentWriter? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RestorativeUiState(isBusy = true))
    val uiState: StateFlow<RestorativeUiState> = _uiState.asStateFlow()
    private var journeyCreatedAtEpochMillis: Long = 0L
    private var currentRecord: RestorativeJourneyRecord? = null
    private var pendingExperimentSpaceId: String? = null
    private var preparedExport: PreparedValidationExport? = null
    private var pendingDocumentDestination: String? = null

    init {
        viewModelScope.launch { restoreJourney() }
    }

    fun onIntent(intent: RestorativeIntent) {
        when (intent) {
            RestorativeIntent.GeneratePlan -> generateOrAdvance()
            RestorativeIntent.SavePlan -> savePlan()
            is RestorativeIntent.Start -> start(intent.experimentId)
            RestorativeIntent.RetryLoad -> retryLoad()
            RestorativeIntent.SaveDraftAndExit -> saveDraftAndExit()
            RestorativeIntent.DiscardDraftAndExit -> discardDraftAndExit()
            is RestorativeIntent.SelectNextStep -> selectNextStep(intent.value)
            RestorativeIntent.OpenPlacementGuidance -> openPlacementGuidance()
            RestorativeIntent.OpenExportDisclosure -> openExportDisclosure()
            is RestorativeIntent.SetExportConsent -> setExportConsent(intent.confirmed)
            RestorativeIntent.CancelExportDisclosure -> cancelExportDisclosure()
            RestorativeIntent.PrepareExport -> prepareResearchExport()
            RestorativeIntent.ShareLaunchSucceeded -> markShareOptionsOpened()
            is RestorativeIntent.ShareLaunchFailed -> fallBackToDocument(intent.stableCode)
            is RestorativeIntent.DocumentDestinationSelected -> selectDocumentDestination(intent.destination)
            is RestorativeIntent.DocumentLaunchFailed -> markExportFailure(intent.stableCode)
            RestorativeIntent.RetryExport -> retryExport()
            RestorativeIntent.DismissExportStatus -> dismissExportStatus()
            is RestorativeIntent.PlanReady,
            RestorativeIntent.SaveComplete -> Unit // Completion intents are owned by this ViewModel.
            else -> updateAndPersist(intent)
        }
    }

    fun startJourney() = onIntent(RestorativeIntent.Start(idProvider.nextId()))

    fun resetAndStartNewAssessment() {
        val newExperimentId = idProvider.nextId()
        journeyCreatedAtEpochMillis = clock.nowEpochMillis()
        val newState = RestorativeUiState(
            step = RestorativeStep.PROMISE,
            draft = RestorativeDraft(experimentId = newExperimentId),
            plan = null,
            isBusy = false,
            error = null,
            exitRequested = false
        )
        _uiState.value = newState
        viewModelScope.launch {
            persistDraft(newState)
        }
    }

    private suspend fun restoreJourney() {
        _uiState.value = when (val result = store.readJourney()) {
            StoreReadResult.Missing -> RestorativeUiState()
            is StoreReadResult.Available -> {
                currentRecord = result.value
                val restored = stateFrom(result.value)
                if (result.value.status == JourneyStatus.SAVED && restored.isReturnWithinWindow) {
                    appendEvent(
                        experimentId = result.value.experimentId,
                        name = "app_reopened_72h",
                        properties = savedEventProperties(result.value) + mapOf(
                            "notification_received" to "false",
                            "researcher_prompted" to "false",
                        ),
                    )
                }
                restored
            }
            is StoreReadResult.RetryableFailure -> RestorativeUiState(
                error = RestorativeError.PersistenceFailure(result.stableCode),
            )
            is StoreReadResult.TerminalFailure -> RestorativeUiState(
                error = RestorativeError.TerminalEvidenceFailure(result.stableCode),
            )
        }
    }

    private fun retryLoad() {
        if (_uiState.value.error !is RestorativeError.PersistenceFailure) return
        _uiState.value = _uiState.value.copy(isBusy = true, error = null)
        viewModelScope.launch { restoreJourney() }
    }

    private fun start(experimentId: String) {
        val previous = _uiState.value
        val updated = RestorativeReducer.reduce(previous, RestorativeIntent.Start(experimentId))
        if (updated === previous) return
        journeyCreatedAtEpochMillis = clock.nowEpochMillis()
        _uiState.value = updated
        viewModelScope.launch {
            persistDraft(updated)
            appendEvent(
                experimentId = experimentId,
                name = "restorative_space_started",
                properties = mapOf(
                    "entry_source" to "gate_2_validation",
                    "app_version" to BuildConfig.VERSION_NAME,
                ),
            )
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
        val generationStartedAt = clock.nowEpochMillis()
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
                    updated.draft.experimentId?.let {
                        appendEvent(
                            experimentId = it,
                            name = "starter_plan_rendered",
                            properties = mapOf(
                                "input_mode" to requireNotNull(updated.draft.inputMode).name,
                                "owned_plant_count" to recommendation.plan.plants.count { plant ->
                                    plant.source == PlantSource.OWNED
                                }.toString(),
                                "recommendation_count" to recommendation.plan.plants.count { plant ->
                                    plant.source == PlantSource.RECOMMENDED
                                }.toString(),
                                "duration_ms" to (clock.nowEpochMillis() - generationStartedAt)
                                    .coerceAtLeast(0L)
                                    .toString(),
                            ),
                        )
                    }
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
        val experimentSpaceId = currentRecord?.experimentSpaceId
            ?: pendingExperimentSpaceId
            ?: idProvider.nextId().also { pendingExperimentSpaceId = it }
        val record = recordFrom(busy).copy(
            experimentSpaceId = experimentSpaceId,
            status = JourneyStatus.SAVED,
            plants = plan.plants,
            placements = plan.placements,
            intendedNextStep = null,
            savedAtEpochMillis = now,
        )
        viewModelScope.launch {
            when (val result = store.writeJourney(record)) {
                StoreWriteResult.Success, StoreWriteResult.Duplicate -> {
                    currentRecord = record
                    pendingExperimentSpaceId = record.experimentSpaceId
                    appendEvent(
                        experimentId = experimentId,
                        name = "starter_plan_saved",
                        properties = savedEventProperties(record) + mapOf(
                            "intended_change_confirmed" to "false",
                        ),
                    )
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
        val record = recordFrom(state)
        val result = store.writeJourney(record)
        when (result) {
            StoreWriteResult.Success, StoreWriteResult.Duplicate -> currentRecord = record
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

    private suspend fun appendEvent(
        experimentId: String,
        name: String,
        properties: Map<String, String> = emptyMap(),
        actionIdentifier: String? = null,
    ) {
        store.appendEvent(
            ValidationEventRecord(
                eventId = idProvider.nextId(),
                experimentId = experimentId,
                name = name,
                deduplicationKey = validationEventDeduplicationKey(experimentId, name, actionIdentifier),
                timestampEpochMillis = clock.nowEpochMillis(),
                properties = mapOf("experiment_id" to experimentId) + properties,
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
            intendedNextStep = record.intendedNextStep,
            nextStepDecisionRecorded = record.nextStepDecisionRecorded,
            isReturnWithinWindow = record.status == JourneyStatus.SAVED && record.savedAtEpochMillis?.let {
                clock.nowEpochMillis() in it..(it + RETURN_WINDOW_MILLIS)
            } == true,
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
            experimentSpaceId = currentRecord?.experimentSpaceId,
            status = JourneyStatus.DRAFT,
            light = state.draft.light,
            availableSpace = state.draft.availableSpace,
            inputMode = state.draft.inputMode,
            plants = state.plan?.plants ?: selectedOwned,
            placements = state.plan?.placements.orEmpty(),
            intendedNextStep = state.intendedNextStep,
            nextStepDecisionRecorded = state.nextStepDecisionRecorded,
            createdAtEpochMillis = journeyCreatedAtEpochMillis.takeIf { it > 0L } ?: clock.nowEpochMillis(),
            savedAtEpochMillis = currentRecord?.savedAtEpochMillis,
        )
    }

    private fun saveDraftAndExit() {
        val state = _uiState.value
        if (state.draft.experimentId == null || state.step == RestorativeStep.SAVED) return
        viewModelScope.launch {
            persistDraft(state)
            if (_uiState.value.error == null) {
                _uiState.value = _uiState.value.copy(exitRequested = true)
            }
        }
    }

    private fun discardDraftAndExit() {
        val state = _uiState.value
        if (state.draft.experimentId == null || state.step == RestorativeStep.SAVED) return
        viewModelScope.launch {
            when (val result = store.discardDraft()) {
                StoreWriteResult.Success, StoreWriteResult.Duplicate -> {
                    currentRecord = null
                    _uiState.value = state.copy(exitRequested = true)
                }
                is StoreWriteResult.RetryableFailure -> _uiState.value = state.copy(
                    error = RestorativeError.PersistenceFailure(result.stableCode),
                )
                is StoreWriteResult.TerminalFailure -> _uiState.value = state.copy(
                    error = RestorativeError.TerminalEvidenceFailure(result.stableCode),
                )
            }
        }
    }

    private fun selectNextStep(value: String?) {
        val previous = _uiState.value
        val updated = RestorativeReducer.reduce(previous, RestorativeIntent.SelectNextStep(value))
        if (updated == previous) return
        _uiState.value = updated
        val record = currentRecord ?: return failInternal("NEXT_STEP_RECORD_MISSING")
        val saved = record.copy(
            intendedNextStep = value,
            nextStepDecisionRecorded = true,
        )
        viewModelScope.launch {
            when (val result = store.writeJourney(saved)) {
                StoreWriteResult.Success, StoreWriteResult.Duplicate -> {
                    currentRecord = saved
                    if (updated.isReturnWithinWindow) recordMeaningfulReturn(saved, "update_next_step")
                }
                is StoreWriteResult.RetryableFailure -> _uiState.value = updated.copy(
                    error = RestorativeError.PersistenceFailure(result.stableCode),
                )
                is StoreWriteResult.TerminalFailure -> _uiState.value = updated.copy(
                    error = RestorativeError.TerminalEvidenceFailure(result.stableCode),
                )
            }
        }
    }

    private fun openPlacementGuidance() {
        val previous = _uiState.value
        val updated = RestorativeReducer.reduce(previous, RestorativeIntent.OpenPlacementGuidance)
        if (updated == previous) return
        _uiState.value = updated
        currentRecord?.takeIf { updated.isReturnWithinWindow }?.let { record ->
            viewModelScope.launch { recordMeaningfulReturn(record, "open_placement_guidance") }
        }
    }

    private suspend fun recordMeaningfulReturn(record: RestorativeJourneyRecord, action: String) {
        appendEvent(
            experimentId = record.experimentId,
            name = "meaningful_return_72h",
            actionIdentifier = action,
            properties = savedEventProperties(record) + mapOf(
                "return_action" to action,
                "notification_received" to "false",
                "researcher_prompted" to "false",
            ),
        )
    }

    private fun savedEventProperties(record: RestorativeJourneyRecord): Map<String, String> = mapOf(
        "experiment_space_id" to (record.experimentSpaceId ?: record.experimentId),
        "experiment_id" to record.experimentId,
    )

    private fun openExportDisclosure() {
        if (
            _uiState.value.step != RestorativeStep.SAVED ||
            _uiState.value.researchExport.status != ResearchExportStatus.IDLE
        ) return
        _uiState.value = _uiState.value.copy(
            researchExport = ResearchExportState(disclosureVisible = true),
        )
    }

    private fun setExportConsent(confirmed: Boolean) {
        val export = _uiState.value.researchExport
        if (!export.disclosureVisible || export.status != ResearchExportStatus.IDLE) return
        _uiState.value = _uiState.value.copy(
            researchExport = export.copy(consentConfirmed = confirmed),
        )
    }

    private fun cancelExportDisclosure() {
        val export = _uiState.value.researchExport
        if (!export.disclosureVisible || export.status != ResearchExportStatus.IDLE) return
        preparedExport = null
        pendingDocumentDestination = null
        _uiState.value = _uiState.value.copy(researchExport = ResearchExportState())
    }

    private fun prepareResearchExport() {
        val state = _uiState.value
        val export = state.researchExport
        if (
            state.step != RestorativeStep.SAVED ||
            !export.disclosureVisible ||
            !export.consentConfirmed ||
            export.status != ResearchExportStatus.IDLE
        ) return
        _uiState.value = state.copy(
            researchExport = export.copy(status = ResearchExportStatus.PREPARING, stableCode = null),
        )
        viewModelScope.launch {
            val result = withContext(workerDispatcher) {
                store.prepareExport(clock.nowEpochMillis())
            }
            when (result) {
                is PrepareExportResult.Ready -> {
                    preparedExport = result.export
                    _uiState.value = _uiState.value.copy(
                        researchExport = export.copy(
                            disclosureVisible = false,
                            status = ResearchExportStatus.READY_TO_SHARE,
                            preparedJson = result.export.utf8Json.decodeToString(),
                        ),
                    )
                }
                is PrepareExportResult.RetryableFailure -> markExportFailure(result.stableCode)
                is PrepareExportResult.TerminalFailure -> markExportFailure(
                    stableCode = result.stableCode,
                    retryAllowed = false,
                )
            }
        }
    }

    private fun markShareOptionsOpened() {
        val export = _uiState.value.researchExport
        if (export.status != ResearchExportStatus.READY_TO_SHARE) return
        _uiState.value = _uiState.value.copy(
            researchExport = export.copy(
                status = ResearchExportStatus.SHARE_OPTIONS_OPENED,
                preparedJson = null,
            ),
        )
    }

    private fun fallBackToDocument(stableCode: String) {
        val export = _uiState.value.researchExport
        if (export.status != ResearchExportStatus.READY_TO_SHARE) return
        _uiState.value = _uiState.value.copy(
            researchExport = export.copy(
                status = ResearchExportStatus.AWAITING_DOCUMENT,
                preparedJson = null,
                stableCode = stableCode,
            ),
        )
    }

    private fun selectDocumentDestination(destination: String?) {
        val export = _uiState.value.researchExport
        if (export.status != ResearchExportStatus.AWAITING_DOCUMENT) return
        if (destination == null) {
            _uiState.value = _uiState.value.copy(
                researchExport = export.copy(
                    status = ResearchExportStatus.PICKER_CANCELLED,
                    stableCode = null,
                ),
            )
            return
        }
        pendingDocumentDestination = destination
        writePreparedDocument(destination)
    }

    private fun writePreparedDocument(destination: String) {
        val export = _uiState.value.researchExport
        val bytes = preparedExport?.utf8Json
        val writer = documentWriter
        if (bytes == null || writer == null) {
            markExportFailure("EXPORT_PREPARED_PAYLOAD_MISSING")
            return
        }
        _uiState.value = _uiState.value.copy(
            researchExport = export.copy(status = ResearchExportStatus.WRITING_DOCUMENT, stableCode = null),
        )
        viewModelScope.launch {
            when (val result = writer.write(destination, bytes)) {
                is DocumentWriteResult.Saved -> _uiState.value = _uiState.value.copy(
                    researchExport = _uiState.value.researchExport.copy(
                        status = ResearchExportStatus.DOCUMENT_SAVED,
                        destinationDisplay = result.destination.substringAfterLast('/'),
                    ),
                )
                is DocumentWriteResult.Failure -> markExportFailure(result.stableCode)
            }
        }
    }

    private fun markExportFailure(stableCode: String, retryAllowed: Boolean = true) {
        _uiState.value = _uiState.value.copy(
            researchExport = _uiState.value.researchExport.copy(
                disclosureVisible = false,
                status = ResearchExportStatus.FAILED,
                preparedJson = null,
                stableCode = stableCode,
                retryAllowed = retryAllowed,
            ),
        )
    }

    private fun retryExport() {
        val export = _uiState.value.researchExport
        if (export.status == ResearchExportStatus.PICKER_CANCELLED) {
            _uiState.value = _uiState.value.copy(
                researchExport = export.copy(status = ResearchExportStatus.AWAITING_DOCUMENT),
            )
            return
        }
        if (export.status != ResearchExportStatus.FAILED || !export.retryAllowed) return
        val destination = pendingDocumentDestination
        when {
            destination != null && preparedExport != null -> writePreparedDocument(destination)
            preparedExport != null -> _uiState.value = _uiState.value.copy(
                researchExport = export.copy(
                    status = ResearchExportStatus.AWAITING_DOCUMENT,
                    stableCode = null,
                ),
            )
            else -> {
                _uiState.value = _uiState.value.copy(
                    researchExport = export.copy(
                        disclosureVisible = true,
                        consentConfirmed = true,
                        status = ResearchExportStatus.IDLE,
                        stableCode = null,
                    ),
                )
                prepareResearchExport()
            }
        }
    }

    private fun dismissExportStatus() {
        val status = _uiState.value.researchExport.status
        if (status in setOf(ResearchExportStatus.PREPARING, ResearchExportStatus.READY_TO_SHARE, ResearchExportStatus.WRITING_DOCUMENT)) return
        preparedExport = null
        pendingDocumentDestination = null
        _uiState.value = _uiState.value.copy(researchExport = ResearchExportState())
    }

    private fun failInternal(code: String) {
        _uiState.value = _uiState.value.copy(
            isBusy = false,
            error = RestorativeError.InternalFailure(code),
        )
    }

    companion object {
        private const val RETURN_WINDOW_MILLIS = 72L * 60L * 60L * 1000L

        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(RestorativeValidationViewModel::class.java))
                return RestorativeValidationViewModel(
                    store = RestorativeValidationStoreProvider.get(context),
                    documentWriter = ContentResolverExportDocumentWriter(context.contentResolver),
                ) as T
            }
        }
    }
}
