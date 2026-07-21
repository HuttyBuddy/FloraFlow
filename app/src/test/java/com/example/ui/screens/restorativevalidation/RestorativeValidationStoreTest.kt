package com.example.ui.screens.restorativevalidation

import android.content.ContentResolver
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import kotlin.io.path.createTempDirectory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class RestorativeValidationStoreTest {
    private lateinit var directory: File
    private lateinit var scope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var store: PreferencesRestorativeValidationStore

    @Before
    fun setUp() {
        directory = createTempDirectory(prefix = "restorative-store-").toFile()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(directory, RESTORATIVE_DATASTORE_FILE) },
        )
        store = PreferencesRestorativeValidationStore(dataStore)
    }

    @After
    fun tearDown() {
        scope.cancel()
        directory.deleteRecursively()
    }

    @Test
    fun `journey record round trips without production persistence`() = runTest {
        val record = savedJourney()

        assertEquals(StoreWriteResult.Success, store.writeJourney(record))
        assertEquals(StoreReadResult.Available(record), store.readJourney())
    }

    @Test
    fun `journey and ledger remain isolated and duplicate event is suppressed`() = runTest {
        val journey = savedJourney()
        val event = ValidationEventRecord(
            eventId = "event-1",
            experimentId = journey.experimentId,
            name = "starter_plan_saved",
            deduplicationKey = validationEventDeduplicationKey(journey.experimentId, "starter_plan_saved"),
            timestampEpochMillis = 300L,
            properties = mapOf("intended_change_confirmed" to "false"),
        )

        store.writeJourney(journey)
        assertEquals(StoreWriteResult.Success, store.appendEvent(event))
        assertEquals(StoreWriteResult.Duplicate, store.appendEvent(event.copy(eventId = "event-2")))

        assertEquals(StoreReadResult.Available(journey), store.readJourney())
        val ledger = (store.readLedger() as StoreReadResult.Available).value
        assertEquals(listOf(event), ledger.events)
    }

    @Test
    fun `malformed journey is terminal and is not replaced`() = runTest {
        val key = stringPreferencesKey("restorative_journey_record")
        dataStore.edit { it[key] = "{not-json" }

        assertEquals(
            StoreReadResult.TerminalFailure("JOURNEY_MALFORMED"),
            store.readJourney(),
        )
        assertEquals("{not-json", dataStore.data.first()[key])
    }

    @Test
    fun `unsupported ledger schema is terminal and append preserves evidence`() = runTest {
        val key = stringPreferencesKey("restorative_event_ledger")
        val unsupported = """{"schemaVersion":99,"experimentId":"experiment-1","events":[]}"""
        dataStore.edit { it[key] = unsupported }
        val event = ValidationEventRecord(
            eventId = "event-1",
            experimentId = "experiment-1",
            name = "restorative_space_started",
            deduplicationKey = "experiment-1:restorative_space_started",
            timestampEpochMillis = 100L,
        )

        assertEquals(
            StoreWriteResult.TerminalFailure("LEDGER_SCHEMA_UNSUPPORTED"),
            store.appendEvent(event),
        )
        assertEquals(unsupported, dataStore.data.first()[key])
    }

    @Test
    fun `prepared export is deterministic sanitized and utf8 exact`() = runTest {
        val journey = savedJourney()
        val event = ValidationEventRecord(
            eventId = "event-1",
            experimentId = journey.experimentId,
            name = "starter_plan_saved",
            deduplicationKey = "experiment-1:starter_plan_saved",
            timestampEpochMillis = 300L,
            properties = linkedMapOf("z" to "last", "a" to "first"),
        )
        store.writeJourney(journey)
        store.appendEvent(event)

        val first = (store.prepareExport(400L) as PrepareExportResult.Ready).export
        val second = (store.prepareExport(400L) as PrepareExportResult.Ready).export

        assertEquals(first.envelope.canonicalPayloadSha256, second.envelope.canonicalPayloadSha256)
        assertArrayEquals(first.utf8Json, second.utf8Json)
        val json = first.utf8Json.decodeToString()
        assertTrue(json.contains("\"experimentId\":\"experiment-1\""))
        assertTrue(json.contains("\"canonicalPayloadSha256\""))
        assertTrue(!json.contains("email", ignoreCase = true))
        assertTrue(!json.contains("location", ignoreCase = true))
        assertTrue(!json.contains("health", ignoreCase = true))
    }

    @Test
    fun `document writer writes exact bytes and maps named failures`() = runTest {
        val destination = "content://floraflow/test-record.json"
        val output = ByteArrayOutputStream()
        val resolver = mock<ContentResolver> {
            on { openOutputStream(Uri.parse(destination), "wt") } doReturn output
        }
        val writer = ContentResolverExportDocumentWriter(
            contentResolver = resolver,
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
        val bytes = "{\"ok\":true}".encodeToByteArray()

        assertEquals(DocumentWriteResult.Saved(destination), writer.write(destination, bytes))
        assertArrayEquals(bytes, output.toByteArray())

        val deniedResolver = mock<ContentResolver> {
            on { openOutputStream(any(), any()) } doThrow SecurityException("denied")
        }
        val missingResolver = mock<ContentResolver> {
            on { openOutputStream(any(), any()) } doThrow FileNotFoundException("missing")
        }
        assertEquals(
            DocumentWriteResult.Failure("EXPORT_DESTINATION_DENIED"),
            ContentResolverExportDocumentWriter(deniedResolver, StandardTestDispatcher(testScheduler))
                .write(destination, bytes),
        )
        assertEquals(
            DocumentWriteResult.Failure("EXPORT_DESTINATION_NOT_FOUND"),
            ContentResolverExportDocumentWriter(missingResolver, StandardTestDispatcher(testScheduler))
                .write(destination, bytes),
        )
    }

    private fun savedJourney() = RestorativeJourneyRecord(
        experimentId = "experiment-1",
        status = JourneyStatus.SAVED,
        light = LightChoice.MEDIUM,
        availableSpace = AvailableSpace.SMALL_CORNER,
        inputMode = InputMode.OWNED_PLANTS,
        plants = listOf(PlantSelectionRecord("snake-plant", "Snake Plant", PlantSource.OWNED)),
        placements = listOf(PlacementRecord(1, "snake-plant", PlacementRole.FLOOR_ANCHOR)),
        intendedNextStep = null,
        createdAtEpochMillis = 100L,
        savedAtEpochMillis = 200L,
    )
}
