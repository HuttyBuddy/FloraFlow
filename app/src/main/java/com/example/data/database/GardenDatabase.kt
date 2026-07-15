package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.GardenLayout
import com.example.data.model.MoodLog
import com.example.data.model.Plant
import com.example.data.model.CareTask
import com.example.data.model.RestorationLog
import com.example.data.model.AssessmentResult

@Database(entities = [GardenLayout::class, Plant::class, MoodLog::class, CareTask::class, RestorationLog::class, AssessmentResult::class], version = 9, exportSchema = true)
abstract class GardenDatabase : RoomDatabase() {
    abstract fun gardenDao(): GardenDao

    companion object {
        @Volatile
        private var INSTANCE: GardenDatabase? = null

        // Every future @Database version bump MUST add a corresponding
        // Migration(from, to) here and a matching test in
        // GardenDatabaseMigrationTest. There is deliberately no
        // fallbackToDestructiveMigration — a missing migration should fail
        // loudly in dev/CI (via the migration test using the exported schema
        // in app/schemas/), not silently wipe every user's garden data in
        // production.
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add restoration_logs table
                db.execSQL("CREATE TABLE IF NOT EXISTS `restoration_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `nriScore` INTEGER NOT NULL, `layoutId` INTEGER, `completedTasks` TEXT NOT NULL, `soundscapeTrack` TEXT NOT NULL)")
                // Add assessment_results table
                db.execSQL("CREATE TABLE IF NOT EXISTS `assessment_results` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `score` INTEGER NOT NULL, `lowestCategories` TEXT NOT NULL)")
            }
        }
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS community_posts")
                db.execSQL("DROP TABLE IF EXISTS community_comments")
            }
        }
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE assessment_results ADD COLUMN layoutId INTEGER")
            }
        }
        val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)

        fun getDatabase(context: Context): GardenDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GardenDatabase::class.java,
                    "garden_database"
                )
                .addMigrations(*ALL_MIGRATIONS)
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun resetDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
