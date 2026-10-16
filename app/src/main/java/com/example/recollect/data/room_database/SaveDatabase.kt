package com.example.recollect.data.room_database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SaveItem::class],
    version = 5,
    exportSchema = false
)
abstract class SaveDatabase : RoomDatabase() {

    abstract fun saveItemDao(): SaveItemDao

    companion object {

        @Volatile
        private var INSTANCE: SaveDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE saved_items ADD COLUMN ownerUid TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE saved_items ADD COLUMN cloudId TEXT DEFAULT NULL"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE saved_items ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE saved_items ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun getDatabase(context: Context): SaveDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SaveDatabase::class.java,
                    "recollect_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5
                    )
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}