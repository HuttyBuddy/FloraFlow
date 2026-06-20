package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.GardenLayout
import com.example.data.model.MoodLog
import com.example.data.model.Plant
import com.example.data.model.CareTask
import com.example.ui.screens.community.CommunityPost
import com.example.ui.screens.community.CommunityComment

@Database(entities = [GardenLayout::class, Plant::class, MoodLog::class, CommunityPost::class, CommunityComment::class, CareTask::class], version = 3, exportSchema = false)
abstract class GardenDatabase : RoomDatabase() {
    abstract fun gardenDao(): GardenDao

    companion object {
        @Volatile
        private var INSTANCE: GardenDatabase? = null

        fun getDatabase(context: Context): GardenDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GardenDatabase::class.java,
                    "garden_database"
                )
                .fallbackToDestructiveMigration(true)
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
