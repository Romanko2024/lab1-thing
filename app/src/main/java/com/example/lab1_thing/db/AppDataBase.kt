package com.example.lab1_thing.db

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.example.lab1_thing.dao.AppDao
import com.example.lab1_thing.entity.ArtistEntity
import com.example.lab1_thing.entity.TrackEntity

@Database(
    entities = [ArtistEntity::class, TrackEntity::class],
    version = 1
)
abstract class AppDataBase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDataBase? = null

        fun getDatabase(context: Context): AppDataBase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder<AppDataBase>(
                    context.applicationContext,
                    "lab1_thing.db3"
                )
                    .setDriver(AndroidSQLiteDriver())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}