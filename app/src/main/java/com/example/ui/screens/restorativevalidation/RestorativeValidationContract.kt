package com.example.ui.screens.restorativevalidation

import com.example.data.model.ClimatePlants
import com.example.data.model.PlantTemplate
import com.squareup.moshi.JsonClass

internal const val RESTORATIVE_SCHEMA_VERSION = 1
internal const val MAX_VALIDATION_PLANTS = 3

enum class RestorativeStep {
    PROMISE,
    LIGHT,
    SPACE,
    PLANTS,
    PLAN,
    SAVED,
}

enum class LightChoice {
    LOW,
    MEDIUM,
    BRIGHT,
    UNSURE,
}

enum class AvailableSpace {
    TABLETOP,
    SMALL_CORNER,
    OPEN_CORNER,
}

enum class PlantSource {
    OWNED,
    RECOMMENDED,
}

enum class CareDifficulty {
    BEGINNER,
    MODERATE,
}

enum class PlacementRole {
    FLOOR_ANCHOR,
    TABLETOP_ACCENT,
    TRAILING_EDGE,
}

enum class SafetyStatus(val presentation: String) {
    NON_TOXIC("Recorded as non-toxic in the FloraFlow catalog"),
    TOXIC("Known toxicity warning"),
    UNKNOWN("Safety information unavailable"),
}

enum class JourneyStatus {
    DRAFT,
    SAVED,
}

enum class InputMode {
    OWNED_PLANTS,
    RECOMMEND_FROM_SCRATCH,
}

enum class ResearchExportStatus {
    IDLE,
    PREPARING,
    READY_TO_SHARE,
    AWAITING_DOCUMENT,
    SHARE_OPTIONS_OPENED,
    WRITING_DOCUMENT,
    DOCUMENT_SAVED,
    PICKER_CANCELLED,
    FAILED,
}

data class ResearchExportState(
    val disclosureVisible: Boolean = false,
    val consentConfirmed: Boolean = false,
    val status: ResearchExportStatus = ResearchExportStatus.IDLE,
    val preparedJson: String? = null,
    val suggestedFileName: String = "floraflow-test-record.json",
    val destinationDisplay: String? = null,
    val stableCode: String? = null,
    val retryAllowed: Boolean = true,
)

@JsonClass(generateAdapter = true)
data class ValidationPlant(
    val slug: String,
    val canonicalName: String,
    val supportedLights: Set<LightChoice>,
    val supportedSpaces: Set<AvailableSpace>,
    val careDifficulty: CareDifficulty,
    val placementRole: PlacementRole,
    val safetyStatus: SafetyStatus,
    val suitabilityReason: String,
    val stableOrder: Int,
)

@JsonClass(generateAdapter = true)
data class PlantSelectionRecord(
    val slug: String,
    val canonicalName: String,
    val source: PlantSource,
)

@JsonClass(generateAdapter = true)
data class PlacementRecord(
    val number: Int,
    val plantSlug: String,
    val role: PlacementRole,
)

@JsonClass(generateAdapter = true)
data class RestorativeJourneyRecord(
    val schemaVersion: Int = RESTORATIVE_SCHEMA_VERSION,
    val experimentId: String,
    val experimentSpaceId: String? = null,
    val status: JourneyStatus,
    val light: LightChoice? = null,
    val availableSpace: AvailableSpace? = null,
    val inputMode: InputMode? = null,
    val plants: List<PlantSelectionRecord> = emptyList(),
    val placements: List<PlacementRecord> = emptyList(),
    val intendedNextStep: String? = null,
    val nextStepDecisionRecorded: Boolean = false,
    val createdAtEpochMillis: Long,
    val savedAtEpochMillis: Long? = null,
)

@JsonClass(generateAdapter = true)
data class ValidationEventRecord(
    val eventId: String,
    val experimentId: String,
    val name: String,
    val deduplicationKey: String,
    val timestampEpochMillis: Long,
    val properties: Map<String, String> = emptyMap(),
)

@JsonClass(generateAdapter = true)
data class ValidationEventLedger(
    val schemaVersion: Int = RESTORATIVE_SCHEMA_VERSION,
    val experimentId: String,
    val events: List<ValidationEventRecord> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class SanitizedExportEnvelope(
    val schemaVersion: Int = RESTORATIVE_SCHEMA_VERSION,
    val experimentId: String,
    val journey: RestorativeJourneyRecord,
    val ledger: ValidationEventLedger,
    val exportedAtEpochMillis: Long,
    val canonicalPayloadSha256: String,
)

data class RestorativeDraft(
    val experimentId: String? = null,
    val light: LightChoice? = null,
    val availableSpace: AvailableSpace? = null,
    val inputMode: InputMode? = null,
    val ownedPlantSlugs: List<String> = emptyList(),
)

data class RestorativeUiState(
    val step: RestorativeStep = RestorativeStep.PROMISE,
    val draft: RestorativeDraft = RestorativeDraft(),
    val plan: StarterPlan? = null,
    val isBusy: Boolean = false,
    val error: RestorativeError? = null,
    val intendedNextStep: String? = null,
    val nextStepDecisionRecorded: Boolean = false,
    val isReturnWithinWindow: Boolean = false,
    val placementGuidanceExpanded: Boolean = false,
    val exitRequested: Boolean = false,
    val researchExport: ResearchExportState = ResearchExportState(),
) {
    val canContinue: Boolean
        get() = when (step) {
            RestorativeStep.PROMISE -> true
            RestorativeStep.LIGHT -> draft.light != null
            RestorativeStep.SPACE -> draft.availableSpace != null
            RestorativeStep.PLANTS -> draft.inputMode != null
            RestorativeStep.PLAN -> plan != null && !isBusy
            RestorativeStep.SAVED -> false
        }

    val progress: Float
        get() = when (step) {
            RestorativeStep.PROMISE -> 0f
            RestorativeStep.LIGHT -> 0.2f
            RestorativeStep.SPACE -> 0.4f
            RestorativeStep.PLANTS -> 0.6f
            RestorativeStep.PLAN -> 0.8f
            RestorativeStep.SAVED -> 1f
        }
}

sealed interface RestorativeError {
    data object NoMatch : RestorativeError
    data class InternalFailure(val stableCode: String) : RestorativeError
    data class PersistenceFailure(val stableCode: String) : RestorativeError
    data class TerminalEvidenceFailure(val stableCode: String) : RestorativeError
}

sealed interface RestorativeIntent {
    data class Start(val experimentId: String) : RestorativeIntent
    data class SelectLight(val light: LightChoice) : RestorativeIntent
    data class SelectSpace(val availableSpace: AvailableSpace) : RestorativeIntent
    data class SelectInputMode(val inputMode: InputMode) : RestorativeIntent
    data class ToggleOwnedPlant(val slug: String) : RestorativeIntent
    data class PlanReady(val plan: StarterPlan) : RestorativeIntent
    data object GeneratePlan : RestorativeIntent
    data object SavePlan : RestorativeIntent
    data object SaveComplete : RestorativeIntent
    data object Back : RestorativeIntent
    data object RetryLoad : RestorativeIntent
    data object SaveDraftAndExit : RestorativeIntent
    data object DiscardDraftAndExit : RestorativeIntent
    data class SelectNextStep(val value: String?) : RestorativeIntent
    data object OpenPlacementGuidance : RestorativeIntent
    data object EditSpace : RestorativeIntent
    data object EditPlants : RestorativeIntent
    data object OpenExportDisclosure : RestorativeIntent
    data class SetExportConsent(val confirmed: Boolean) : RestorativeIntent
    data object CancelExportDisclosure : RestorativeIntent
    data object PrepareExport : RestorativeIntent
    data object ShareLaunchSucceeded : RestorativeIntent
    data class ShareLaunchFailed(val stableCode: String) : RestorativeIntent
    data class DocumentDestinationSelected(val destination: String?) : RestorativeIntent
    data class DocumentLaunchFailed(val stableCode: String) : RestorativeIntent
    data object RetryExport : RestorativeIntent
    data object DismissExportStatus : RestorativeIntent
}

data class StarterPlan(
    val plants: List<PlantSelectionRecord>,
    val placements: List<PlacementRecord>,
)

sealed interface RecommendationResult {
    data class Match(val plan: StarterPlan) : RecommendationResult
    data object NoMatch : RecommendationResult
}

sealed interface RecordSchemaResult {
    data class Supported(val record: RestorativeJourneyRecord) : RecordSchemaResult
    data class Unsupported(val schemaVersion: Int) : RecordSchemaResult
}

internal fun validateRecordSchema(record: RestorativeJourneyRecord): RecordSchemaResult =
    if (record.schemaVersion == RESTORATIVE_SCHEMA_VERSION) {
        RecordSchemaResult.Supported(record)
    } else {
        RecordSchemaResult.Unsupported(record.schemaVersion)
    }

/**
 * PROMISE -> LIGHT -> SPACE -> PLANTS -> PLAN -> SAVED
 *                ^         ^          |
 *                +---------+----------+  Back preserves prior answers.
 *
 * Async generation and persistence complete through explicit result intents so repeated taps
 * cannot skip a legal state or create duplicate records.
 */
object RestorativeReducer {
    fun reduce(state: RestorativeUiState, intent: RestorativeIntent): RestorativeUiState {
        if (
            state.isBusy &&
            intent !is RestorativeIntent.PlanReady &&
            intent != RestorativeIntent.SaveComplete
        ) return state

        return when (intent) {
            is RestorativeIntent.Start -> if (state.step == RestorativeStep.PROMISE) {
                state.copy(
                    step = RestorativeStep.LIGHT,
                    draft = state.draft.copy(experimentId = intent.experimentId),
                    error = null,
                )
            } else state

            is RestorativeIntent.SelectLight -> if (state.step == RestorativeStep.LIGHT) {
                state.copy(draft = state.draft.copy(light = intent.light), error = null)
            } else state

            is RestorativeIntent.SelectSpace -> if (state.step == RestorativeStep.SPACE) {
                state.copy(draft = state.draft.copy(availableSpace = intent.availableSpace), error = null)
            } else state

            is RestorativeIntent.SelectInputMode -> if (state.step == RestorativeStep.PLANTS) {
                state.copy(
                    draft = state.draft.copy(
                        inputMode = intent.inputMode,
                        ownedPlantSlugs = if (intent.inputMode == InputMode.RECOMMEND_FROM_SCRATCH) {
                            emptyList()
                        } else state.draft.ownedPlantSlugs,
                    ),
                    error = null,
                )
            } else state

            is RestorativeIntent.ToggleOwnedPlant -> toggleOwnedPlant(state, intent.slug)
            RestorativeIntent.GeneratePlan -> advanceOrGenerate(state)
            is RestorativeIntent.PlanReady -> if (state.step == RestorativeStep.PLANTS && state.isBusy) {
                state.copy(step = RestorativeStep.PLAN, plan = intent.plan, isBusy = false, error = null)
            } else state

            RestorativeIntent.SavePlan -> if (state.step == RestorativeStep.PLAN && state.plan != null) {
                state.copy(isBusy = true, error = null)
            } else state

            RestorativeIntent.SaveComplete -> if (state.step == RestorativeStep.PLAN && state.isBusy) {
                state.copy(step = RestorativeStep.SAVED, isBusy = false, error = null)
            } else state

            RestorativeIntent.Back -> back(state)
            RestorativeIntent.RetryLoad,
            RestorativeIntent.SaveDraftAndExit,
            RestorativeIntent.DiscardDraftAndExit -> state

            is RestorativeIntent.SelectNextStep -> if (state.step == RestorativeStep.SAVED) {
                state.copy(
                    intendedNextStep = intent.value,
                    nextStepDecisionRecorded = true,
                    error = null,
                )
            } else state

            RestorativeIntent.OpenPlacementGuidance -> if (state.step == RestorativeStep.SAVED) {
                state.copy(placementGuidanceExpanded = !state.placementGuidanceExpanded)
            } else state

            RestorativeIntent.EditSpace -> if (state.step == RestorativeStep.PLAN) {
                state.copy(step = RestorativeStep.SPACE, plan = null, error = null)
            } else state

            RestorativeIntent.EditPlants -> if (state.step == RestorativeStep.PLAN) {
                state.copy(step = RestorativeStep.PLANTS, plan = null, error = null)
            } else state

            RestorativeIntent.OpenExportDisclosure,
            is RestorativeIntent.SetExportConsent,
            RestorativeIntent.CancelExportDisclosure,
            RestorativeIntent.PrepareExport,
            RestorativeIntent.ShareLaunchSucceeded,
            is RestorativeIntent.ShareLaunchFailed,
            is RestorativeIntent.DocumentDestinationSelected,
            is RestorativeIntent.DocumentLaunchFailed,
            RestorativeIntent.RetryExport,
            RestorativeIntent.DismissExportStatus -> state
        }
    }

    private fun advanceOrGenerate(state: RestorativeUiState): RestorativeUiState = when (state.step) {
        RestorativeStep.LIGHT -> if (state.draft.light != null) state.copy(step = RestorativeStep.SPACE) else state
        RestorativeStep.SPACE -> if (state.draft.availableSpace != null) state.copy(step = RestorativeStep.PLANTS) else state
        RestorativeStep.PLANTS -> if (state.canContinue) state.copy(isBusy = true, error = null) else state
        else -> state
    }

    private fun toggleOwnedPlant(state: RestorativeUiState, slug: String): RestorativeUiState {
        if (state.step != RestorativeStep.PLANTS || state.draft.inputMode != InputMode.OWNED_PLANTS) {
            return state
        }
        val selected = state.draft.ownedPlantSlugs
        val updated = when {
            slug in selected -> selected - slug
            selected.size >= MAX_VALIDATION_PLANTS -> selected
            ValidationPlantCatalog.find(slug) == null -> selected
            else -> selected + slug
        }
        return state.copy(draft = state.draft.copy(ownedPlantSlugs = updated))
    }

    private fun back(state: RestorativeUiState): RestorativeUiState {
        if (state.isBusy) return state
        val prior = when (state.step) {
            RestorativeStep.PROMISE -> return state
            RestorativeStep.LIGHT -> RestorativeStep.PROMISE
            RestorativeStep.SPACE -> RestorativeStep.LIGHT
            RestorativeStep.PLANTS -> RestorativeStep.SPACE
            RestorativeStep.PLAN -> RestorativeStep.PLANTS
            RestorativeStep.SAVED -> RestorativeStep.PLAN
        }
        return state.copy(step = prior, plan = if (state.step == RestorativeStep.PLAN) null else state.plan, error = null)
    }
}

object ValidationPlantCatalog {
    private val allLights = setOf(LightChoice.LOW, LightChoice.MEDIUM, LightChoice.BRIGHT)
    private val floorSpaces = setOf(AvailableSpace.SMALL_CORNER, AvailableSpace.OPEN_CORNER)
    private val compactSpaces = AvailableSpace.entries.toSet()

    val entries: List<ValidationPlant> = listOf(
        ValidationPlant(
            slug = "snake-plant",
            canonicalName = "Snake Plant",
            supportedLights = allLights,
            supportedSpaces = floorSpaces,
            careDifficulty = CareDifficulty.BEGINNER,
            placementRole = PlacementRole.FLOOR_ANCHOR,
            safetyStatus = SafetyStatus.UNKNOWN,
            suitabilityReason = "Handles a wide range of indirect light and needs little water.",
            stableOrder = 10,
        ),
        ValidationPlant(
            slug = "zz-plant",
            canonicalName = "ZZ Plant",
            supportedLights = allLights,
            supportedSpaces = floorSpaces,
            careDifficulty = CareDifficulty.BEGINNER,
            placementRole = PlacementRole.FLOOR_ANCHOR,
            safetyStatus = SafetyStatus.UNKNOWN,
            suitabilityReason = "Fits a compact floor position and tolerates lower indirect light.",
            stableOrder = 20,
        ),
        ValidationPlant(
            slug = "parlor-palm",
            canonicalName = "Parlor Palm",
            supportedLights = setOf(LightChoice.LOW, LightChoice.MEDIUM),
            supportedSpaces = floorSpaces,
            careDifficulty = CareDifficulty.MODERATE,
            placementRole = PlacementRole.FLOOR_ANCHOR,
            safetyStatus = SafetyStatus.NON_TOXIC,
            suitabilityReason = "Creates a soft floor-level anchor in low or medium indirect light.",
            stableOrder = 30,
        ),
        ValidationPlant(
            slug = "spider-plant",
            canonicalName = "Spider Plant",
            supportedLights = setOf(LightChoice.MEDIUM, LightChoice.BRIGHT),
            supportedSpaces = compactSpaces,
            careDifficulty = CareDifficulty.BEGINNER,
            placementRole = PlacementRole.TABLETOP_ACCENT,
            safetyStatus = SafetyStatus.UNKNOWN,
            suitabilityReason = "Works as a compact accent where filtered light is available.",
            stableOrder = 40,
        ),
        ValidationPlant(
            slug = "chinese-money-plant",
            canonicalName = "Chinese Money Plant",
            supportedLights = setOf(LightChoice.BRIGHT),
            supportedSpaces = setOf(AvailableSpace.TABLETOP, AvailableSpace.SMALL_CORNER),
            careDifficulty = CareDifficulty.MODERATE,
            placementRole = PlacementRole.TABLETOP_ACCENT,
            safetyStatus = SafetyStatus.UNKNOWN,
            suitabilityReason = "Adds a small rounded accent in bright indirect light.",
            stableOrder = 50,
        ),
        ValidationPlant(
            slug = "string-of-pearls",
            canonicalName = "String of Pearls",
            supportedLights = setOf(LightChoice.BRIGHT),
            supportedSpaces = compactSpaces,
            careDifficulty = CareDifficulty.MODERATE,
            placementRole = PlacementRole.TRAILING_EDGE,
            safetyStatus = SafetyStatus.UNKNOWN,
            suitabilityReason = "Uses a bright shelf or raised edge without taking floor space.",
            stableOrder = 60,
        ),
    ).sortedBy(ValidationPlant::stableOrder)

    init {
        require(entries.map { it.slug }.distinct().size == entries.size) { "Validation plant slugs must be unique." }
        require(entries.map { it.canonicalName }.distinct().size == entries.size) {
            "Validation plant canonical names must be unique."
        }
        val bundledNames = ClimatePlants.ALL_TEMPLATES.asSequence().filter(PlantTemplate::isIndoor).map { it.name }.toSet()
        require(entries.all { it.canonicalName in bundledNames }) {
            "Every validation plant must link to an existing indoor PlantTemplate."
        }
    }

    fun find(slug: String): ValidationPlant? = entries.firstOrNull { it.slug == slug }

    fun bundledTemplate(entry: ValidationPlant): PlantTemplate = ClimatePlants.ALL_TEMPLATES.first {
        it.isIndoor && it.name == entry.canonicalName
    }
}

object RestorativeRecommendationEngine {
    fun recommend(
        light: LightChoice,
        availableSpace: AvailableSpace,
        excludedSlugs: Set<String> = emptySet(),
        limit: Int = MAX_VALIDATION_PLANTS,
    ): List<ValidationPlant> {
        if (limit <= 0) return emptyList()
        return ValidationPlantCatalog.entries.asSequence()
            .filterNot { it.slug in excludedSlugs }
            .filter { light == LightChoice.UNSURE || light in it.supportedLights }
            .filter { availableSpace in it.supportedSpaces }
            .take(limit.coerceAtMost(MAX_VALIDATION_PLANTS))
            .toList()
    }

    fun createPlan(
        light: LightChoice,
        availableSpace: AvailableSpace,
        ownedPlantSlugs: List<String>,
    ): RecommendationResult {
        val owned = ownedPlantSlugs.asSequence()
            .distinct()
            .mapNotNull(ValidationPlantCatalog::find)
            .take(MAX_VALIDATION_PLANTS)
            .toList()
        val recommendations = recommend(
            light = light,
            availableSpace = availableSpace,
            excludedSlugs = owned.mapTo(mutableSetOf()) { it.slug },
            limit = MAX_VALIDATION_PLANTS - owned.size,
        )
        val finalPlants = owned + recommendations
        if (finalPlants.isEmpty()) return RecommendationResult.NoMatch

        val selections = finalPlants.map { plant ->
            PlantSelectionRecord(
                slug = plant.slug,
                canonicalName = plant.canonicalName,
                source = if (plant in owned) PlantSource.OWNED else PlantSource.RECOMMENDED,
            )
        }
        val placements = finalPlants.mapIndexed { index, plant ->
            PlacementRecord(number = index + 1, plantSlug = plant.slug, role = plant.placementRole)
        }
        return RecommendationResult.Match(StarterPlan(selections, placements))
    }

    fun calculateScore(draft: RestorativeDraft): Int {
        var score = 5 // Base Score

        score += when (draft.light) {
            LightChoice.BRIGHT -> 5
            LightChoice.MEDIUM -> 4
            LightChoice.LOW -> 3
            else -> 2
        }

        score += when (draft.availableSpace) {
            AvailableSpace.OPEN_CORNER -> 5
            AvailableSpace.SMALL_CORNER -> 4
            AvailableSpace.TABLETOP -> 3
            else -> 2
        }

        score += if (draft.ownedPlantSlugs.size >= 2) 5 else if (draft.ownedPlantSlugs.isNotEmpty()) 4 else 3

        return score.coerceIn(1, 20)
    }
}

internal fun validationEventDeduplicationKey(
    experimentId: String,
    eventName: String,
    actionIdentifier: String? = null,
): String = listOfNotNull(experimentId, eventName, actionIdentifier).joinToString(":")
