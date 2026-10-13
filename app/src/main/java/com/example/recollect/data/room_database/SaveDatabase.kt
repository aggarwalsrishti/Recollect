
package com.example.recollect.data.room_database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SaveItem::class],
    version = 3
)
abstract class SaveDatabase : RoomDatabase() {

    abstract fun saveItemDao(): SaveItemDao

    companion object {

        @Volatile
        private var INSTANCE: SaveDatabase? = null

        // Migration from database version 1 to version 2.
        // Adds the Firebase account owner to each saved item.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE saved_items ADD COLUMN ownerUid TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        // Migration from database version 2 to version 3.
        // Adds the Firestore document ID to each saved item.
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE saved_items ADD COLUMN cloudId TEXT DEFAULT NULL"
                )
            }
        }

        fun getDatabase(context: Context): SaveDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    SaveDatabase::class.java,
                    "recollect_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3
                    )
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
