package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.GardenLayout
import com.example.data.model.MoodLog
import com.example.data.model.Plant
import com.example.data.model.CareTask
import com.example.data.model.RestorationLog
import com.example.data.model.CommunityPost
import com.example.data.model.CommunityComment

@Database(entities = [GardenLayout::class, Plant::class, MoodLog::class, CommunityPost::class, CommunityComment::class, CareTask::class, RestorationLog::class], version = 5, exportSchema = false)
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
