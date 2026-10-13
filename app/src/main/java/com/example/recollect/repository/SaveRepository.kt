
package com.example.recollect.repository

import com.example.recollect.data.room_database.SaveItem
import com.example.recollect.data.room_database.SaveItemDao

class SaveRepository(
    private val dao: SaveItemDao
) {
    fun getAllItems(uid: String) = dao.getAllItems(uid)

    suspend fun assignUnownedItems(uid: String) {
        dao.assignUnownedItems(uid)
    }

    suspend fun insert(item: SaveItem) {
        dao.insert(item)
    }

    suspend fun update(item: SaveItem) {
        dao.update(item)
    }

    suspend fun delete(item: SaveItem) {
        dao.delete(item)
    }
}
