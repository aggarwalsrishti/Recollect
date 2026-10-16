package com.example.recollect.repository

import com.example.recollect.data.room_database.SaveItem
import com.example.recollect.data.room_database.SaveItemDao

class SaveRepository(
    private val dao: SaveItemDao
) {
    fun getAllItems(uid: String) = dao.getAllItems(uid)

    suspend fun getAllItemsOnce(uid: String): List<SaveItem> {
        return dao.getAllItemsOnce(uid)
    }

    suspend fun getPendingItems(uid: String): List<SaveItem> {
        return dao.getPendingItems(uid)
    }

    suspend fun assignUnownedItems(uid: String) {
        dao.assignUnownedItems(uid)
    }

    suspend fun insert(item: SaveItem): Long {
        return dao.insert(item)
    }

    suspend fun update(item: SaveItem) {
        dao.update(item)
    }

    suspend fun delete(item: SaveItem) {
        dao.delete(item)
    }

    suspend fun deleteById(itemId: Int, uid: String) {
        dao.deleteById(itemId, uid)
    }

    suspend fun updateSyncStatus(
        itemId: Int,
        uid: String,
        status: String
    ) {
        dao.updateSyncStatus(itemId, uid, status)
    }
}