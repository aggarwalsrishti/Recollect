package com.example.recollect.data.room_database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_items")
data class SaveItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String,
    val type: String = "NOTE",
    val tags: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
    val ownerUid: String = "",
    val cloudId: String? = null,

    // SYNCED, PENDING, or FAILED
    val syncStatus: String = "SYNCED",

    // Keep deleted cloud items until their deletion syncs.
    val isDeleted: Boolean = false
)