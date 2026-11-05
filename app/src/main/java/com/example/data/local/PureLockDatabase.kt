package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BlocklistDomainEntity::class,
        BlockedEventEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PureLockDatabase : RoomDatabase() {

    abstract fun blocklistDao(): BlocklistDao
    abstract fun blockedEventDao(): BlockedEventDao

    companion object {
        @Volatile
        private var INSTANCE: PureLockDatabase? = null

        fun getInstance(context: Context): PureLockDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PureLockDatabase::class.java,
                    "purelock_local.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
