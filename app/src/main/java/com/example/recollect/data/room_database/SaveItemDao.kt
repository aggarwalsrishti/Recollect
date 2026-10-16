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
    suspend fun insert(item: SaveItem): Long

    @Update
    suspend fun update(item: SaveItem)

    @Delete
    suspend fun delete(item: SaveItem)

    @Query("""
        SELECT * FROM saved_items
        WHERE ownerUid = :uid
        AND isDeleted = 0
        ORDER BY updatedAt DESC
    """)
    fun getAllItems(uid: String): Flow<List<SaveItem>>

    @Query("""
        SELECT * FROM saved_items
        WHERE ownerUid = :uid
        AND syncStatus IN ('PENDING', 'FAILED')
        ORDER BY updatedAt ASC
    """)
    suspend fun getPendingItems(uid: String): List<SaveItem>

    @Query("""
        UPDATE saved_items
        SET syncStatus = :status
        WHERE id = :itemId AND ownerUid = :uid
    """)
    suspend fun updateSyncStatus(
        itemId: Int,
        uid: String,
        status: String
    )

    @Query("""
        UPDATE saved_items
        SET ownerUid = :uid
        WHERE ownerUid = '' OR ownerUid = 'LOCAL'
    """)
    suspend fun assignUnownedItems(uid: String)

    @Query("""
        SELECT * FROM saved_items
        WHERE ownerUid = :uid
    """)
    suspend fun getAllItemsOnce(uid: String): List<SaveItem>

    @Query("""
        DELETE FROM saved_items
        WHERE id = :itemId AND ownerUid = :uid
    """)
    suspend fun deleteById(itemId: Int, uid: String)
}