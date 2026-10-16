package com.example.recollect.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.recollect.data.cloud.CloudRepository
import com.example.recollect.data.room_database.SaveDatabase
import com.example.recollect.data.room_database.SaveItem
import com.example.recollect.repository.SaveRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SaveViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()

    private val dao =
        SaveDatabase.getDatabase(application).saveItemDao()

    private val repository = SaveRepository(dao)
    private val cloudRepository = CloudRepository()

    private val ownerUid = MutableStateFlow(
        auth.currentUser?.uid ?: "LOCAL"
    )

    private val _statusMessage = MutableStateFlow("")
    val statusMessage = _statusMessage.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val uid = firebaseAuth.currentUser?.uid ?: "LOCAL"
        ownerUid.value = uid

        if (uid != "LOCAL") {
            viewModelScope.launch {
                try {
                    repository.assignUnownedItems(uid)
                    syncPendingItems()
                } catch (e: Exception) {
                    _statusMessage.value =
                        "Could not prepare sync: ${e.message ?: "Please try again."}"
                }
            }
        }
    }

    init {
        auth.addAuthStateListener(authListener)

        val uid = auth.currentUser?.uid
        if (uid != null) {
            viewModelScope.launch {
                try {
                    repository.assignUnownedItems(uid)
                    syncPendingItems()
                } catch (e: Exception) {
                    _statusMessage.value =
                        "Could not prepare local items: ${e.message ?: "Please try again."}"
                }
            }
        }
    }

    val allItems = ownerUid
        .flatMapLatest { uid ->
            repository.getAllItems(uid)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    companion object {
        private const val SYNCED = "SYNCED"
        private const val PENDING = "PENDING"
        private const val FAILED = "FAILED"
        private const val LOCAL = "LOCAL"
    }

    fun addItem(
        title: String,
        content: String,
        type: String,
        tags: String,
        cloud: Boolean
    ) {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: LOCAL

            // Save to Room before attempting any network operation.
            val item = SaveItem(
                title = title,
                content = content,
                type = type,
                tags = tags,
                ownerUid = uid,
                syncStatus = if (cloud) PENDING else SYNCED
            )

            try {
                val generatedId = repository.insert(item)
                val savedItem = item.copy(id = generatedId.toInt())

                if (cloud && uid != LOCAL) {
                    _statusMessage.value =
                        "Saved locally. Syncing to cloud…"
                    syncPendingItems()
                } else if (cloud) {
                    _statusMessage.value =
                        "Saved locally. Sign in to sync this item."
                } else {
                    _statusMessage.value = "Saved locally."
                }
            } catch (e: Exception) {
                _statusMessage.value =
                    "Could not save item locally: ${e.message ?: "Please try again."}"
            }
        }
    }

    fun updateItem(
        item: SaveItem,
        title: String,
        content: String,
        type: String,
        tags: String
    ) {
        viewModelScope.launch {
            val updatedItem = item.copy(
                title = title,
                content = content,
                type = type,
                tags = tags,
                updatedAt = System.currentTimeMillis(),
                syncStatus = if (
                    !item.cloudId.isNullOrBlank() ||
                    item.syncStatus == PENDING ||
                    item.syncStatus == FAILED
                ) PENDING else SYNCED
            )

            try {
                repository.update(updatedItem)
                _statusMessage.value = "Item updated locally."

                if (updatedItem.syncStatus == PENDING) {
                    syncPendingItems()
                }
            } catch (e: Exception) {
                _statusMessage.value =
                    "Could not update item: ${e.message ?: "Please try again."}"
            }
        }
    }

    fun deleteItem(item: SaveItem) {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: LOCAL

            try {
                if (item.cloudId.isNullOrBlank()) {
                    // It has never been uploaded, so no cloud deletion is needed.
                    repository.delete(item)
                    _statusMessage.value = "Item deleted."
                } else {
                    // Keep a tombstone in Room until cloud deletion succeeds.
                    val deletedItem = item.copy(
                        isDeleted = true,
                        updatedAt = System.currentTimeMillis(),
                        syncStatus = PENDING
                    )
                    repository.update(deletedItem)

                    if (uid != LOCAL) {
                        syncPendingItems()
                    } else {
                        _statusMessage.value =
                            "Deletion saved locally. Sign in to finish cloud deletion."
                    }
                }
            } catch (e: Exception) {
                _statusMessage.value =
                    "Could not delete item: ${e.message ?: "Please try again."}"
            }
        }
    }

    fun toggleArchive(item: SaveItem) {
        viewModelScope.launch {
            val updatedItem = item.copy(
                isArchived = !item.isArchived,
                updatedAt = System.currentTimeMillis(),
                syncStatus = if (
                    !item.cloudId.isNullOrBlank() ||
                    item.syncStatus == PENDING ||
                    item.syncStatus == FAILED
                ) PENDING else SYNCED
            )

            try {
                repository.update(updatedItem)

                _statusMessage.value = if (updatedItem.isArchived) {
                    "Item archived locally."
                } else {
                    "Item restored locally."
                }

                if (updatedItem.syncStatus == PENDING) {
                    syncPendingItems()
                }
            } catch (e: Exception) {
                _statusMessage.value =
                    "Could not update archive status: ${e.message ?: "Please try again."}"
            }
        }
    }

    suspend fun saveItemToCloud(item: SaveItem): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Please sign in to save items to cloud.")

        if (
            item.ownerUid.isNotBlank() &&
            item.ownerUid != LOCAL &&
            item.ownerUid != uid
        ) {
            throw IllegalStateException("You cannot upload another user's item.")
        }

        val pendingItem = item.copy(
            ownerUid = uid,
            updatedAt = System.currentTimeMillis(),
            syncStatus = PENDING
        )

        // Persist the pending change first.
        repository.update(pendingItem)

        val cloudId = cloudRepository.saveItem(pendingItem)

        repository.update(
            pendingItem.copy(
                cloudId = cloudId,
                syncStatus = SYNCED
            )
        )

        val result = "Saved \"${item.title}\" to cloud successfully."
        _statusMessage.value = result
        return result
    }

    /**
     * Public retry entry point.
     * Call this after a connection is restored or from a Retry button.
     */
    fun retryPendingSync() {
        viewModelScope.launch {
            syncPendingItems()
        }
    }

    /**
     * Processes pending uploads, updates and deletions.
     * Failed operations remain in Room for another attempt.
     */
    private suspend fun syncPendingItems() {
        val uid = auth.currentUser?.uid ?: return

        try {
            repository.assignUnownedItems(uid)
            val pendingItems = repository.getPendingItems(uid)

            if (pendingItems.isEmpty()) {
                return
            }

            var succeeded = 0
            var failed = 0

            for (item in pendingItems) {
                try {
                    if (item.isDeleted) {
                        if (!item.cloudId.isNullOrBlank()) {
                            cloudRepository.deleteItem(item)
                        }

                        repository.deleteById(item.id, uid)
                    } else {
                        val cloudId = cloudRepository.saveItem(item)

                        repository.update(
                            item.copy(
                                cloudId = cloudId,
                                ownerUid = uid,
                                syncStatus = SYNCED
                            )
                        )
                    }

                    succeeded++
                } catch (e: Exception) {
                    repository.updateSyncStatus(
                        itemId = item.id,
                        uid = uid,
                        status = FAILED
                    )
                    failed++
                }
            }

            _statusMessage.value = when {
                failed == 0 ->
                    "Sync complete. $succeeded item(s) synced."
                succeeded == 0 ->
                    "Sync failed for $failed item(s). Your local changes are saved; retry when connected."
                else ->
                    "$succeeded item(s) synced; $failed failed and will need retrying."
            }
        } catch (e: Exception) {
            _statusMessage.value =
                "Sync could not run: ${e.message ?: "Check your connection and retry."}"
        }
    }

    suspend fun refreshCloudItems(): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Please sign in first.")

        repository.assignUnownedItems(uid)

        val cloudItems = cloudRepository.getItems()
        val localItems = repository.getAllItemsOnce(uid)

        var added = 0
        var updated = 0

        for (cloudItem in cloudItems) {
            val existing = localItems.firstOrNull {
                it.cloudId == cloudItem.cloudId
            }

            if (existing == null) {
                repository.insert(cloudItem)
                added++
                continue
            }

            // Never replace a pending or failed local edit with cloud data.
            if (
                existing.syncStatus == PENDING ||
                existing.syncStatus == FAILED ||
                existing.isDeleted
            ) {
                continue
            }

            val cloudIsNewer = cloudItem.updatedAt > existing.updatedAt

            if (cloudIsNewer) {
                repository.update(
                    cloudItem.copy(
                        id = existing.id,
                        syncStatus = SYNCED,
                        isDeleted = false
                    )
                )
                updated++
            } else if (existing.updatedAt > cloudItem.updatedAt) {
                // Local copy is newer; queue it for upload.
                repository.update(
                    existing.copy(syncStatus = PENDING)
                )
            }
        }

        // Try to send any local changes discovered during refresh.
        syncPendingItems()

        val result = "Cloud refresh complete: $added added, $updated updated."
        _statusMessage.value = result
        return result
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authListener)
        super.onCleared()
    }
}