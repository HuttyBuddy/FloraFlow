package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import com.example.data.model.GardenLayout
import com.example.data.model.MoodLog
import com.example.data.model.Plant
import com.example.data.model.CareTask
import com.example.data.model.RestorationLog
import com.example.data.model.CommunityPost
import com.example.data.model.CommunityComment
import com.example.data.model.AssessmentResult

@Database(entities = [GardenLayout::class, Plant::class, MoodLog::class, CommunityPost::class, CommunityComment::class, CareTask::class, RestorationLog::class, AssessmentResult::class], version = 7, exportSchema = true)
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
        // production. Example for the next bump (v7 -> v8):
        //
        // val MIGRATION_7_8 = object : Migration(7, 8) {
        //     override fun migrate(db: SupportSQLiteDatabase) {
        //         db.execSQL("ALTER TABLE plants ADD COLUMN newColumn TEXT NOT NULL DEFAULT ''")
        //     }
        // }
        val ALL_MIGRATIONS: Array<Migration> = arrayOf()

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
