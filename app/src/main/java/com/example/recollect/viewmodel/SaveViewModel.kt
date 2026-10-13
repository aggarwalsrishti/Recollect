
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

    private val authListener =
        FirebaseAuth.AuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid ?: "LOCAL"
            ownerUid.value = uid

            if (uid != "LOCAL") {
                viewModelScope.launch {
                    try {
                        repository.assignUnownedItems(uid)
                    } catch (e: Exception) {
                        _statusMessage.value =
                            e.message ?: "Could not load local items."
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
                } catch (e: Exception) {
                    _statusMessage.value =
                        e.message ?: "Could not prepare local items."
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

    fun addItem(
        title: String,
        content: String,
        type: String,
        tags: String,
        cloud: Boolean
    ) {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: "LOCAL"

            var item = SaveItem(
                title = title,
                content = content,
                type = type,
                tags = tags,
                ownerUid = uid
            )

            if (cloud && uid != "LOCAL") {
                try {
                    val cloudId = cloudRepository.saveItem(item)
                    item = item.copy(cloudId = cloudId)
                    _statusMessage.value = "Item saved locally and to cloud."
                } catch (e: Exception) {
                    _statusMessage.value =
                        "Saved locally, but cloud upload failed: " +
                                (e.message ?: "Please try again.")
                }
            } else {
                _statusMessage.value =
                    if (uid == "LOCAL" && cloud) {
                        "Saved locally. Sign in to upload it to cloud."
                    } else {
                        "Saved locally."
                    }
            }

            repository.insert(item)
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
                updatedAt = System.currentTimeMillis()
            )

            repository.update(updatedItem)

            if (!updatedItem.cloudId.isNullOrBlank()) {
                try {
                    cloudRepository.saveItem(updatedItem)
                    _statusMessage.value = "Item updated locally and in cloud."
                } catch (e: Exception) {
                    _statusMessage.value =
                        "Item updated locally, but cloud update failed."
                }
            } else {
                _statusMessage.value = "Item updated locally."
            }
        }
    }

    fun deleteItem(item: SaveItem) {
        viewModelScope.launch {
            try {
                if (!item.cloudId.isNullOrBlank()) {
                    cloudRepository.deleteItem(item)
                }

                repository.delete(item)
                _statusMessage.value = "Item deleted."
            } catch (e: Exception) {
                _statusMessage.value =
                    "Could not delete item: " +
                            (e.message ?: "Please try again.")
            }
        }
    }

    fun toggleArchive(item: SaveItem) {
        viewModelScope.launch {
            val updatedItem = item.copy(
                isArchived = !item.isArchived,
                updatedAt = System.currentTimeMillis()
            )

            repository.update(updatedItem)

            if (!updatedItem.cloudId.isNullOrBlank()) {
                try {
                    cloudRepository.saveItem(updatedItem)
                    _statusMessage.value =
                        if (updatedItem.isArchived) {
                            "Item archived."
                        } else {
                            "Item restored to inbox."
                        }
                } catch (e: Exception) {
                    _statusMessage.value =
                        "Changed locally, but cloud sync failed."
                }
            } else {
                _statusMessage.value =
                    if (updatedItem.isArchived) {
                        "Item archived locally."
                    } else {
                        "Item restored to inbox."
                    }
            }
        }
    }


    suspend fun saveItemToCloud(item: SaveItem): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException(
                "Please sign in to save items to cloud."
            )

        if (
            item.ownerUid.isNotBlank() &&
            item.ownerUid != "LOCAL" &&
            item.ownerUid != uid
        ) {
            throw IllegalStateException(
                "You cannot upload another user's item."
            )
        }

        // Create one timestamp and use it for both local and cloud data.
        val itemForCloud = item.copy(
            ownerUid = uid,
            updatedAt = System.currentTimeMillis()
        )

        val cloudId = cloudRepository.saveItem(itemForCloud)

        repository.update(
            itemForCloud.copy(cloudId = cloudId)
        )

        val result = "Saved \"${item.title}\" to cloud successfully."
        _statusMessage.value = result

        return result
    }


    suspend fun refreshCloudItems(): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Please sign in first.")

        repository.assignUnownedItems(uid)

        val cloudItems = cloudRepository.getItems()
        val localItems = repository.getAllItems(uid).first()

        var added = 0
        var updated = 0

        for (cloudItem in cloudItems) {
            val existing = localItems.firstOrNull {
                it.cloudId == cloudItem.cloudId
            }


            if (existing == null) {
                repository.insert(cloudItem)
                added++
            } else {
                val contentChanged =
                    existing.title != cloudItem.title ||
                            existing.content != cloudItem.content ||
                            existing.type != cloudItem.type ||
                            existing.tags != cloudItem.tags ||
                            existing.isArchived != cloudItem.isArchived

                if (contentChanged) {
                    repository.update(
                        cloudItem.copy(id = existing.id)
                    )
                    updated++
                }
            }

        }


        val result = "Cloud refresh complete: $added added, $updated updated."
        _statusMessage.value = result
        return result
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authListener)
        super.onCleared()
    }
}
