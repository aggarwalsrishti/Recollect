package com.example.recollect.data.cloud

import com.example.recollect.data.room_database.SaveItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class CloudRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private fun currentUid(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Please sign in first.")
    }

    private fun userItems() =
        firestore.collection("users")
            .document(currentUid())
            .collection("items")

    suspend fun saveItem(item: SaveItem): String {
        val uid = currentUid()

        if (
            item.ownerUid.isNotBlank() &&
            item.ownerUid != "LOCAL" &&
            item.ownerUid != uid
        ) {
            throw IllegalStateException(
                "You cannot upload another user's item."
            )
        }

        val document = if (item.cloudId.isNullOrBlank()) {
            userItems().document()
        } else {
            userItems().document(item.cloudId)
        }

        val data = hashMapOf<String, Any>(
            "title" to item.title,
            "content" to item.content,
            "type" to item.type,
            "tags" to item.tags,
            "createdAt" to item.createdAt,
            "updatedAt" to item.updatedAt,
            "isArchived" to item.isArchived,
            "ownerUid" to uid
        )

        document.set(data).await()
        return document.id
    }

    suspend fun getItems(): List<SaveItem> {
        val uid = currentUid()
        val snapshot = userItems().get().await()

        return snapshot.documents.map { document ->
            SaveItem(
                id = 0,
                title = document.getString("title") ?: "",
                content = document.getString("content") ?: "",
                type = document.getString("type") ?: "NOTE",
                tags = document.getString("tags") ?: "",
                createdAt = document.getLong("createdAt")
                    ?: System.currentTimeMillis(),
                updatedAt = document.getLong("updatedAt")
                    ?: System.currentTimeMillis(),
                isArchived = document.getBoolean("isArchived") ?: false,
                ownerUid = uid,
                cloudId = document.id,
                syncStatus = "SYNCED",
                isDeleted = false
            )
        }.sortedByDescending { it.updatedAt }
    }

    suspend fun deleteItem(item: SaveItem) {
        val cloudId = item.cloudId

        if (cloudId.isNullOrBlank()) {
            throw IllegalStateException(
                "This item has not been saved to the cloud."
            )
        }

        userItems().document(cloudId).delete().await()
    }
}