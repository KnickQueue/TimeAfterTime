package com.example.kronosclock.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration

@Database(entities = [Watch::class], version = 2, exportSchema = false)
abstract class WatchDatabase : RoomDatabase() {
    abstract fun watchDao(): WatchDao

    companion object {
        @Volatile private var INSTANCE: WatchDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new columns introduced in v2 (nullable so backfill isn’t required)
                db.execSQL("ALTER TABLE watches ADD COLUMN lastSyncedEpochMs INTEGER")
                db.execSQL("ALTER TABLE watches ADD COLUMN lastOffsetMs INTEGER")
            }
        }

        fun getInstance(context: Context): WatchDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WatchDatabase::class.java,
                    "watch-db"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { INSTANCE = it }
            }
    }
}
