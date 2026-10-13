
package com.example.recollect.data.room_database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SaveItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SaveItem)

    @Update
    suspend fun update(item: SaveItem)

    @Delete
    suspend fun delete(item: SaveItem)

    @Query(
        """
        SELECT * FROM saved_items
        WHERE ownerUid = :uid
        ORDER BY updatedAt DESC
        """
    )
    fun getAllItems(uid: String): Flow<List<SaveItem>>

    @Query(
        """
        UPDATE saved_items
        SET ownerUid = :uid
        WHERE ownerUid = '' OR ownerUid = 'LOCAL'
        """
    )
    suspend fun assignUnownedItems(uid: String)
}
