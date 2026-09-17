package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlaybackRecordEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        PlayerPreferencesEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class VTDatabase : RoomDatabase() {
    abstract fun playbackRecordDao(): PlaybackRecordDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun preferencesDao(): PlayerPreferencesDao

    companion object {
        @Volatile
        private var INSTANCE: VTDatabase? = null

        fun getDatabase(context: Context): VTDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VTDatabase::class.java,
                    "vt_player_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
