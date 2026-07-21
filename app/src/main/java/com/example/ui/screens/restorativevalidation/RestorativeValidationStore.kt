package com.example.ui.screens.restorativevalidation

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Moshi
import java.io.FileNotFoundException
import java.io.IOException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

internal const val RESTORATIVE_DATASTORE_FILE = "restorative_validation.preferences_pb"

sealed interface StoreReadResult<out T> {
    data object Missing : StoreReadResult<Nothing>
    data class Available<T>(val value: T) : StoreReadResult<T>
    data class RetryableFailure(val stableCode: String) : StoreReadResult<Nothing>
    data class TerminalFailure(val stableCode: String) : StoreReadResult<Nothing>
}

sealed interface StoreWriteResult {
    data object Success : StoreWriteResult
    data object Duplicate : StoreWriteResult
    data class RetryableFailure(val stableCode: String) : StoreWriteResult
    data class TerminalFailure(val stableCode: String) : StoreWriteResult
}

data class PreparedValidationExport(
    val envelope: SanitizedExportEnvelope,
    val utf8Json: ByteArray,
)

sealed interface PrepareExportResult {
    data class Ready(val export: PreparedValidationExport) : PrepareExportResult
    data class RetryableFailure(val stableCode: String) : PrepareExportResult
    data class TerminalFailure(val stableCode: String) : PrepareExportResult
}

interface RestorativeValidationStore {
    suspend fun readJourney(): StoreReadResult<RestorativeJourneyRecord>
    suspend fun writeJourney(record: RestorativeJourneyRecord): StoreWriteResult
    suspend fun readLedger(): StoreReadResult<ValidationEventLedger>
    suspend fun appendEvent(event: ValidationEventRecord): StoreWriteResult
    suspend fun prepareExport(exportedAtEpochMillis: Long): PrepareExportResult
}

internal class PreferencesRestorativeValidationStore(
    private val dataStore: DataStore<Preferences>,
    moshi: Moshi = Moshi.Builder().build(),
) : RestorativeValidationStore {
    private val journeyAdapter = moshi.adapter(RestorativeJourneyRecord::class.java)
    private val ledgerAdapter = moshi.adapter(ValidationEventLedger::class.java)
    private val canonicalPayloadAdapter = moshi.adapter(CanonicalExportPayload::class.java)
    private val envelopeAdapter = moshi.adapter(SanitizedExportEnvelope::class.java)

    override suspend fun readJourney(): StoreReadResult<RestorativeJourneyRecord> = readRecord(
        key = JOURNEY_KEY,
        decode = journeyAdapter::fromJson,
        schemaVersion = RestorativeJourneyRecord::schemaVersion,
        malformedCode = "JOURNEY_MALFORMED",
        unsupportedCode = "JOURNEY_SCHEMA_UNSUPPORTED",
        ioCode = "JOURNEY_READ_IO",
        corruptionCode = "JOURNEY_DATASTORE_CORRUPT",
    )

    override suspend fun writeJourney(record: RestorativeJourneyRecord): StoreWriteResult {
        if (record.schemaVersion != RESTORATIVE_SCHEMA_VERSION) {
            return StoreWriteResult.TerminalFailure("JOURNEY_SCHEMA_UNSUPPORTED")
        }
        val encoded = try {
            journeyAdapter.toJson(record)
        } catch (_: JsonDataException) {
            return StoreWriteResult.TerminalFailure("JOURNEY_ENCODE_FAILED")
        }
        return writeString(JOURNEY_KEY, encoded, "JOURNEY_WRITE_IO", "JOURNEY_DATASTORE_CORRUPT")
    }

    override suspend fun readLedger(): StoreReadResult<ValidationEventLedger> = readRecord(
        key = LEDGER_KEY,
        decode = ledgerAdapter::fromJson,
        schemaVersion = ValidationEventLedger::schemaVersion,
        malformedCode = "LEDGER_MALFORMED",
        unsupportedCode = "LEDGER_SCHEMA_UNSUPPORTED",
        ioCode = "LEDGER_READ_IO",
        corruptionCode = "LEDGER_DATASTORE_CORRUPT",
    )

    override suspend fun appendEvent(event: ValidationEventRecord): StoreWriteResult {
        return try {
            var duplicate = false
            var terminalCode: String? = null
            dataStore.edit { preferences ->
                val existingJson = preferences[LEDGER_KEY]
                val existing = if (existingJson == null) {
                    ValidationEventLedger(experimentId = event.experimentId)
                } else {
                    val decoded = decodeLedger(existingJson)
                    when {
                        decoded == null -> {
                            terminalCode = "LEDGER_MALFORMED"
                            return@edit
                        }
                        decoded.schemaVersion != RESTORATIVE_SCHEMA_VERSION -> {
                            terminalCode = "LEDGER_SCHEMA_UNSUPPORTED"
                            return@edit
                        }
                        decoded.experimentId != event.experimentId -> {
                            terminalCode = "LEDGER_EXPERIMENT_MISMATCH"
                            return@edit
                        }
                        else -> decoded
                    }
                }
                if (terminalCode != null) return@edit
                if (existing.events.any { it.deduplicationKey == event.deduplicationKey }) {
                    duplicate = true
                    return@edit
                }
                preferences[LEDGER_KEY] = ledgerAdapter.toJson(
                    existing.copy(events = existing.events + event)
                )
            }
            when {
                terminalCode != null -> StoreWriteResult.TerminalFailure(terminalCode)
                duplicate -> StoreWriteResult.Duplicate
                else -> StoreWriteResult.Success
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            StoreWriteResult.RetryableFailure("LEDGER_WRITE_IO")
        } catch (_: CorruptionException) {
            StoreWriteResult.TerminalFailure("LEDGER_DATASTORE_CORRUPT")
        } catch (_: JsonDataException) {
            StoreWriteResult.TerminalFailure("LEDGER_ENCODE_FAILED")
        }
    }

    override suspend fun prepareExport(exportedAtEpochMillis: Long): PrepareExportResult {
        val journey = when (val result = readJourney()) {
            is StoreReadResult.Available -> result.value
            StoreReadResult.Missing -> return PrepareExportResult.TerminalFailure("EXPORT_JOURNEY_MISSING")
            is StoreReadResult.RetryableFailure -> return PrepareExportResult.RetryableFailure(result.stableCode)
            is StoreReadResult.TerminalFailure -> return PrepareExportResult.TerminalFailure(result.stableCode)
        }
        val ledger = when (val result = readLedger()) {
            is StoreReadResult.Available -> result.value
            StoreReadResult.Missing -> ValidationEventLedger(experimentId = journey.experimentId)
            is StoreReadResult.RetryableFailure -> return PrepareExportResult.RetryableFailure(result.stableCode)
            is StoreReadResult.TerminalFailure -> return PrepareExportResult.TerminalFailure(result.stableCode)
        }
        if (ledger.experimentId != journey.experimentId) {
            return PrepareExportResult.TerminalFailure("EXPORT_EXPERIMENT_MISMATCH")
        }

        return try {
            val canonicalLedger = ledger.copy(
                events = ledger.events.map { event ->
                    event.copy(properties = event.properties.toSortedMap())
                }
            )
            val canonicalPayload = CanonicalExportPayload(
                schemaVersion = RESTORATIVE_SCHEMA_VERSION,
                experimentId = journey.experimentId,
                journey = journey,
                ledger = canonicalLedger,
                exportedAtEpochMillis = exportedAtEpochMillis,
            )
            val hash = sha256(canonicalPayloadAdapter.toJson(canonicalPayload).encodeToByteArray())
            val envelope = SanitizedExportEnvelope(
                experimentId = journey.experimentId,
                journey = journey,
                ledger = canonicalLedger,
                exportedAtEpochMillis = exportedAtEpochMillis,
                canonicalPayloadSha256 = hash,
            )
            PrepareExportResult.Ready(
                PreparedValidationExport(
                    envelope = envelope,
                    utf8Json = envelopeAdapter.toJson(envelope).encodeToByteArray(),
                )
            )
        } catch (_: JsonDataException) {
            PrepareExportResult.TerminalFailure("EXPORT_SERIALIZATION_FAILED")
        } catch (_: NoSuchAlgorithmException) {
            PrepareExportResult.TerminalFailure("EXPORT_HASH_UNAVAILABLE")
        }
    }

    private suspend fun <T> readRecord(
        key: Preferences.Key<String>,
        decode: (String) -> T?,
        schemaVersion: (T) -> Int,
        malformedCode: String,
        unsupportedCode: String,
        ioCode: String,
        corruptionCode: String,
    ): StoreReadResult<T> = try {
        val encoded = dataStore.data.first()[key] ?: return StoreReadResult.Missing
        val decoded = try {
            decode(encoded)
        } catch (_: JsonDataException) {
            null
        } catch (_: JsonEncodingException) {
            null
        } ?: return StoreReadResult.TerminalFailure(malformedCode)
        if (schemaVersion(decoded) != RESTORATIVE_SCHEMA_VERSION) {
            StoreReadResult.TerminalFailure(unsupportedCode)
        } else {
            StoreReadResult.Available(decoded)
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: IOException) {
        StoreReadResult.RetryableFailure(ioCode)
    } catch (_: CorruptionException) {
        StoreReadResult.TerminalFailure(corruptionCode)
    }

    private suspend fun writeString(
        key: Preferences.Key<String>,
        value: String,
        ioCode: String,
        corruptionCode: String,
    ): StoreWriteResult = try {
        dataStore.edit { it[key] = value }
        StoreWriteResult.Success
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: IOException) {
        StoreWriteResult.RetryableFailure(ioCode)
    } catch (_: CorruptionException) {
        StoreWriteResult.TerminalFailure(corruptionCode)
    }

    private fun decodeLedger(json: String): ValidationEventLedger? = try {
        ledgerAdapter.fromJson(json)
    } catch (_: JsonDataException) {
        null
    } catch (_: JsonEncodingException) {
        null
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private companion object {
        val JOURNEY_KEY = stringPreferencesKey("restorative_journey_record")
        val LEDGER_KEY = stringPreferencesKey("restorative_event_ledger")
    }
}

@JsonClass(generateAdapter = true)
internal data class CanonicalExportPayload(
    val schemaVersion: Int,
    val experimentId: String,
    val journey: RestorativeJourneyRecord,
    val ledger: ValidationEventLedger,
    val exportedAtEpochMillis: Long,
)

internal object RestorativeValidationStoreProvider {
    @Volatile
    private var instance: RestorativeValidationStore? = null

    fun get(context: Context): RestorativeValidationStore = instance ?: synchronized(this) {
        instance ?: run {
            val applicationContext = context.applicationContext
            PreferencesRestorativeValidationStore(
                dataStore = PreferenceDataStoreFactory.create(
                    scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
                    produceFile = { applicationContext.preferencesDataStoreFile(RESTORATIVE_DATASTORE_FILE) },
                )
            ).also { instance = it }
        }
    }
}

sealed interface DocumentWriteResult {
    data class Saved(val destination: String) : DocumentWriteResult
    data class Failure(val stableCode: String) : DocumentWriteResult
}

interface ExportDocumentWriter {
    suspend fun write(destination: String, bytes: ByteArray): DocumentWriteResult
}

internal class ContentResolverExportDocumentWriter(
    private val contentResolver: ContentResolver,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ExportDocumentWriter {
    override suspend fun write(destination: String, bytes: ByteArray): DocumentWriteResult =
        withContext(ioDispatcher) {
            try {
                val uri = Uri.parse(destination)
                val stream = contentResolver.openOutputStream(uri, "wt")
                    ?: return@withContext DocumentWriteResult.Failure("EXPORT_STREAM_UNAVAILABLE")
                stream.use { it.write(bytes) }
                DocumentWriteResult.Saved(destination)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: FileNotFoundException) {
                DocumentWriteResult.Failure("EXPORT_DESTINATION_NOT_FOUND")
            } catch (_: SecurityException) {
                DocumentWriteResult.Failure("EXPORT_DESTINATION_DENIED")
            } catch (_: IOException) {
                DocumentWriteResult.Failure("EXPORT_WRITE_IO")
            }
        }
}
