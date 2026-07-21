package com.example.ui.screens.restorativevalidation

import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RestorativeValidationContractTest {

    @Test
    fun `catalog entries link to unique bundled indoor templates`() {
        val entries = ValidationPlantCatalog.entries

        assertEquals(entries.size, entries.map { it.slug }.distinct().size)
        assertEquals(entries.size, entries.map { it.canonicalName }.distinct().size)
        entries.forEach { entry ->
            val template = ValidationPlantCatalog.bundledTemplate(entry)
            assertTrue(template.isIndoor)
            assertEquals(entry.canonicalName, template.name)
        }
    }

    @Test
    fun `every supported light and space combination is deterministic and bounded`() {
        LightChoice.entries.forEach { light ->
            AvailableSpace.entries.forEach { space ->
                val first = RestorativeRecommendationEngine.recommend(light, space)
                val second = RestorativeRecommendationEngine.recommend(light, space)

                assertEquals(first, second)
                assertTrue(first.size <= MAX_VALIDATION_PLANTS)
                assertEquals(first.size, first.map { it.slug }.distinct().size)
                assertEquals(first.sortedBy { it.stableOrder }, first)
            }
        }
    }

    @Test
    fun `excluded plants are never recommended`() {
        val excluded = setOf("snake-plant", "zz-plant")

        val result = RestorativeRecommendationEngine.recommend(
            light = LightChoice.LOW,
            availableSpace = AvailableSpace.OPEN_CORNER,
            excludedSlugs = excluded,
        )

        assertTrue(result.none { it.slug in excluded })
    }

    @Test
    fun `owned plants remain first and duplicates are removed`() {
        val result = RestorativeRecommendationEngine.createPlan(
            light = LightChoice.BRIGHT,
            availableSpace = AvailableSpace.SMALL_CORNER,
            ownedPlantSlugs = listOf("spider-plant", "spider-plant", "snake-plant"),
        ) as RecommendationResult.Match

        assertEquals(listOf("spider-plant", "snake-plant"), result.plan.plants.take(2).map { it.slug })
        assertTrue(result.plan.plants.take(2).all { it.source == PlantSource.OWNED })
        assertEquals(result.plan.plants.size, result.plan.plants.map { it.slug }.distinct().size)
        assertEquals((1..result.plan.plants.size).toList(), result.plan.placements.map { it.number })
    }

    @Test
    fun `one owned plant remains a valid plan when recommendations are excluded by the limit`() {
        val result = RestorativeRecommendationEngine.createPlan(
            light = LightChoice.BRIGHT,
            availableSpace = AvailableSpace.TABLETOP,
            ownedPlantSlugs = listOf("snake-plant", "zz-plant", "parlor-palm"),
        ) as RecommendationResult.Match

        assertEquals(3, result.plan.plants.size)
        assertTrue(result.plan.plants.all { it.source == PlantSource.OWNED })
    }

    @Test
    fun `zero qualified plants is represented as no match`() {
        val result = RestorativeRecommendationEngine.recommend(
            light = LightChoice.LOW,
            availableSpace = AvailableSpace.TABLETOP,
        )

        assertTrue(result.isEmpty())
        assertSame(
            RecommendationResult.NoMatch,
            RestorativeRecommendationEngine.createPlan(
                light = LightChoice.LOW,
                availableSpace = AvailableSpace.TABLETOP,
                ownedPlantSlugs = emptyList(),
            ),
        )
    }

    @Test
    fun `unknown safety never becomes a positive safety claim`() {
        val unknownEntries = ValidationPlantCatalog.entries.filter { it.safetyStatus == SafetyStatus.UNKNOWN }

        assertTrue(unknownEntries.isNotEmpty())
        assertTrue(unknownEntries.all { it.safetyStatus.presentation == "Safety information unavailable" })
        assertEquals(
            "Recorded as non-toxic in the FloraFlow catalog",
            ValidationPlantCatalog.find("parlor-palm")?.safetyStatus?.presentation,
        )
    }

    @Test
    fun `reducer follows legal path and preserves answers on back`() {
        var state = RestorativeUiState()
        state = RestorativeReducer.reduce(state, RestorativeIntent.Start("experiment-1"))
        state = RestorativeReducer.reduce(state, RestorativeIntent.SelectLight(LightChoice.MEDIUM))
        state = RestorativeReducer.reduce(state, RestorativeIntent.GeneratePlan)
        state = RestorativeReducer.reduce(state, RestorativeIntent.SelectSpace(AvailableSpace.SMALL_CORNER))
        state = RestorativeReducer.reduce(state, RestorativeIntent.GeneratePlan)
        state = RestorativeReducer.reduce(
            state,
            RestorativeIntent.SelectInputMode(InputMode.RECOMMEND_FROM_SCRATCH),
        )
        state = RestorativeReducer.reduce(state, RestorativeIntent.Back)

        assertEquals(RestorativeStep.SPACE, state.step)
        assertEquals(LightChoice.MEDIUM, state.draft.light)
        assertEquals(AvailableSpace.SMALL_CORNER, state.draft.availableSpace)
        assertEquals(InputMode.RECOMMEND_FROM_SCRATCH, state.draft.inputMode)
    }

    @Test
    fun `illegal and repeated intents do not skip steps`() {
        val initial = RestorativeUiState()

        val illegalSpace = RestorativeReducer.reduce(
            initial,
            RestorativeIntent.SelectSpace(AvailableSpace.OPEN_CORNER),
        )
        val illegalGenerate = RestorativeReducer.reduce(initial, RestorativeIntent.GeneratePlan)

        assertSame(initial, illegalSpace)
        assertSame(initial, illegalGenerate)
    }

    @Test
    fun `busy state ignores duplicate generation and back`() {
        val busy = RestorativeUiState(
            step = RestorativeStep.PLANTS,
            draft = RestorativeDraft(
                experimentId = "experiment-1",
                light = LightChoice.BRIGHT,
                availableSpace = AvailableSpace.TABLETOP,
                inputMode = InputMode.RECOMMEND_FROM_SCRATCH,
            ),
            isBusy = true,
        )

        assertSame(busy, RestorativeReducer.reduce(busy, RestorativeIntent.GeneratePlan))
        assertSame(busy, RestorativeReducer.reduce(busy, RestorativeIntent.Back))
    }

    @Test
    fun `save completion is the only legal transition out of busy plan state`() {
        val plan = StarterPlan(
            plants = listOf(PlantSelectionRecord("snake-plant", "Snake Plant", PlantSource.RECOMMENDED)),
            placements = listOf(PlacementRecord(1, "snake-plant", PlacementRole.FLOOR_ANCHOR)),
        )
        val busy = RestorativeUiState(step = RestorativeStep.PLAN, plan = plan, isBusy = true)

        val saved = RestorativeReducer.reduce(busy, RestorativeIntent.SaveComplete)

        assertEquals(RestorativeStep.SAVED, saved.step)
        assertFalse(saved.isBusy)
        assertEquals(plan, saved.plan)
    }

    @Test
    fun `owned plant selection is unique bounded and catalog backed`() {
        var state = RestorativeUiState(
            step = RestorativeStep.PLANTS,
            draft = RestorativeDraft(inputMode = InputMode.OWNED_PLANTS),
        )

        listOf("snake-plant", "snake-plant", "zz-plant", "spider-plant", "parlor-palm", "missing").forEach {
            state = RestorativeReducer.reduce(state, RestorativeIntent.ToggleOwnedPlant(it))
        }

        assertEquals(listOf("zz-plant", "spider-plant", "parlor-palm"), state.draft.ownedPlantSlugs)
        assertEquals(MAX_VALIDATION_PLANTS, state.draft.ownedPlantSlugs.size)
    }

    @Test
    fun `journey record round trips and unknown fields are ignored`() {
        val moshi = Moshi.Builder().build()
        val adapter = moshi.adapter(RestorativeJourneyRecord::class.java)
        val record = RestorativeJourneyRecord(
            experimentId = "experiment-1",
            status = JourneyStatus.SAVED,
            light = LightChoice.MEDIUM,
            availableSpace = AvailableSpace.SMALL_CORNER,
            inputMode = InputMode.OWNED_PLANTS,
            plants = listOf(PlantSelectionRecord("snake-plant", "Snake Plant", PlantSource.OWNED)),
            placements = listOf(PlacementRecord(1, "snake-plant", PlacementRole.FLOOR_ANCHOR)),
            intendedNextStep = "Move one plant beside the chair",
            createdAtEpochMillis = 100L,
            savedAtEpochMillis = 200L,
        )

        val encoded = adapter.toJson(record)
        val decoded = adapter.fromJson(encoded.dropLast(1) + ",\"futureField\":true}")

        assertEquals(record, decoded)
        assertNotNull(decoded)
    }

    @Test
    fun `unsupported schema is classified without replacing the record`() {
        val record = RestorativeJourneyRecord(
            schemaVersion = RESTORATIVE_SCHEMA_VERSION + 1,
            experimentId = "experiment-1",
            status = JourneyStatus.DRAFT,
            createdAtEpochMillis = 100L,
        )

        val result = validateRecordSchema(record)

        assertTrue(result is RecordSchemaResult.Unsupported)
        assertEquals(RESTORATIVE_SCHEMA_VERSION + 1, (result as RecordSchemaResult.Unsupported).schemaVersion)
    }

    @Test
    fun `event deduplication key is stable and action aware`() {
        assertEquals(
            "experiment-1:starter_plan_saved",
            validationEventDeduplicationKey("experiment-1", "starter_plan_saved"),
        )
        assertEquals(
            "experiment-1:meaningful_return_72h:open-placement-guidance",
            validationEventDeduplicationKey(
                "experiment-1",
                "meaningful_return_72h",
                "open-placement-guidance",
            ),
        )
    }

    @Test
    fun `progress and continuation reflect the current step`() {
        val light = RestorativeUiState(step = RestorativeStep.LIGHT)
        val selectedLight = light.copy(draft = light.draft.copy(light = LightChoice.UNSURE))

        assertFalse(light.canContinue)
        assertTrue(selectedLight.canContinue)
        assertEquals(0.2f, light.progress)
    }
}
