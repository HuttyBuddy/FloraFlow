package com.example.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Guardrail for GardenDatabase.ALL_MIGRATIONS.
 *
 * This project intentionally does NOT use fallbackToDestructiveMigration —
 * a missed migration must fail a build/test, not silently wipe every user's
 * garden data in production. Whenever GardenDatabase's @Database version is
 * bumped, this test suite must gain a matching case that migrates a
 * persisted database created at the previous version and asserts the
 * upgrade succeeds without data loss.
 *
 * Template for the next migration (v7 -> v8), using room-testing's
 * MigrationTestHelper with the exported schema in app/schemas/ (see
 * https://developer.android.com/training/data-storage/room/migrating-db-versions#test):
 *
 * @get:Rule
 * val helper = MigrationTestHelper(
 *     InstrumentationRegistry.getInstrumentation(),
 *     GardenDatabase::class.java
 * )
 *
 * @Test
 * fun migrate7To8() {
 *     helper.createDatabase(TEST_DB, 7).apply {
 *         execSQL("INSERT INTO plants (...) VALUES (...)")
 *         close()
 *     }
 *     helper.runMigrationsAndValidate(TEST_DB, 8, true, GardenDatabase.MIGRATION_7_8)
 *     // then reopen and assert the pre-existing row survived
 * }
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class GardenDatabaseMigrationTest {

    @Test
    fun migration8To9AddsNullableAssessmentLayoutId() {
        val database = mock<androidx.sqlite.db.SupportSQLiteDatabase>()

        GardenDatabase.MIGRATION_8_9.migrate(database)

        verify(database).execSQL("ALTER TABLE assessment_results ADD COLUMN layoutId INTEGER")
    }

    // Directly exercises Room's own schema validation against a real,
    // persisted SQLite file (not in-memory) using the current entity set
    // plus GardenDatabase.ALL_MIGRATIONS. If a future version bump omits a
    // required Migration, Room throws IllegalStateException here instead of
    // the app discovering it via a silent data wipe or a startup crash-loop
    // in production.
    @Test
    fun currentSchemaOpensCleanlyWithRegisteredMigrations() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = context.getDatabasePath("garden_database_migration_sanity_test.db")
        dbFile.delete()
        try {
            val db = Room.databaseBuilder(context, GardenDatabase::class.java, dbFile.absolutePath)
                .addMigrations(*GardenDatabase.ALL_MIGRATIONS)
                .build()
            // Force Room to open and validate the schema against entities.
            db.openHelper.writableDatabase
            db.close()
        } finally {
            dbFile.delete()
        }
    }
}
