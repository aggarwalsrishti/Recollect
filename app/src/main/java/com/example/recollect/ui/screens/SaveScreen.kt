package com.example.recollect.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.recollect.data.room_database.SaveItem
import com.example.recollect.ui.components.SaveItemCard
import com.example.recollect.viewmodel.SaveViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun SaveScreen(
    viewModel: SaveViewModel,
    onSignOut: () -> Unit,
    sharedText: String? = null,
    onSharedTextHandled: () -> Unit = {},
    sharedImageUri: Uri? = null,
    onSharedImageHandled: () -> Unit = {}
) {
    val items by viewModel.allItems.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    var selectedItem by remember {
        mutableStateOf<SaveItem?>(null)
    }
    var showEditor by remember {
        mutableStateOf(false)
    }

    var selectedLocation by remember { mutableStateOf("INBOX") }
    var selectedType by remember { mutableStateOf("ALL") }
    var searchText by remember { mutableStateOf("") }

    var cloudEnabled by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf("") }
    var uploadingItemId by remember { mutableStateOf<Int?>(null) }

    val scope = rememberCoroutineScope()
    val user = FirebaseAuth.getInstance().currentUser

    // Open the editor when another app shares content with Recollect.
    LaunchedEffect(sharedText, sharedImageUri) {
        if (!sharedText.isNullOrBlank() || sharedImageUri != null) {
            selectedItem = null
            showEditor = true
        }
    }

    val locations = listOf("ALL", "INBOX", "ARCHIVED")
    val types = listOf("ALL", "NOTE", "LINK", "SNIPPET", "IMAGE")

    val visibleItems = items.filter { item ->
        val locationMatches = when (selectedLocation) {
            "ALL" -> true
            "INBOX" -> !item.isArchived
            "ARCHIVED" -> item.isArchived
            else -> true
        }

        val typeMatches =
            selectedType == "ALL" || item.type == selectedType

        val searchMatches =
            item.title.contains(searchText, ignoreCase = true) ||
                    item.content.contains(searchText, ignoreCase = true) ||
                    item.tags.contains(searchText, ignoreCase = true)

        locationMatches && typeMatches && searchMatches
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    selectedItem = null
                    showEditor = true
                }
            ) {
                Text("+ Save item")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Recollect",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Text(user?.email ?: "Local inbox")
                }

                TextButton(onClick = onSignOut) {
                    Text("Sign out")
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search title, content or tags") },
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            Text("Location", style = MaterialTheme.typography.titleSmall)

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                items(locations) { location ->
                    FilterChip(
                        selected = selectedLocation == location,
                        onClick = { selectedLocation = location },
                        label = {
                            Text(
                                when (location) {
                                    "ALL" -> "All"
                                    "INBOX" -> "Inbox"
                                    else -> "Archived"
                                }
                            )
                        }
                    )
                }
            }

            Text("Item type", style = MaterialTheme.typography.titleSmall)

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                items(types) { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = {
                            Text(
                                when (type) {
                                    "ALL" -> "All types"
                                    else -> type.lowercase()
                                        .replaceFirstChar { it.uppercase() }
                                }
                            )
                        }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (cloudEnabled) {
                            "Cloud saving enabled"
                        } else {
                            "Local-only saving"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "This setting affects new items.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Switch(
                    checked = cloudEnabled,
                    onCheckedChange = { cloudEnabled = it }
                )
            }

            Spacer(Modifier.height(8.dp))

            if (user != null) {
                OutlinedButton(
                    onClick = { viewModel.retryPendingSync() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Retry Sync")
                }
                Spacer(Modifier.height(8.dp))
            }

            if (statusMessage.isNotBlank()) {
                Text(
                    statusMessage,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (message.isNotBlank() && message != statusMessage) {
                Spacer(Modifier.height(4.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(8.dp))

            if (visibleItems.isEmpty()) {
                Text(
                    text = when {
                        searchText.isNotBlank() ->
                            "No items match your search and filters."
                        selectedLocation == "ARCHIVED" ->
                            "Nothing archived in this category yet."
                        selectedLocation == "ALL" ->
                            "No items found."
                        else ->
                            "Your inbox is empty. Save your first useful thing."
                    }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(
                        items = visibleItems,
                        key = { "${it.ownerUid}-${it.id}-${it.title}" }
                    ) { item ->
                        SaveItemCard(
                            item = item,
                            onEdit = {
                                selectedItem = item
                                showEditor = true
                            },
                            onDelete = {
                                viewModel.deleteItem(item)
                            },
                            onArchive = {
                                viewModel.toggleArchive(item)
                            },
                            isSignedIn = user != null,
                            isSavingToCloud = uploadingItemId == item.id,
                            onSaveToCloud = {
                                scope.launch {
                                    uploadingItemId = item.id
                                    message = ""

                                    try {
                                        message = viewModel.saveItemToCloud(item)
                                    } catch (e: Exception) {
                                        message = e.message
                                            ?: "Could not save item to cloud."
                                    } finally {
                                        uploadingItemId = null
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showEditor) {
        val incomingImage = sharedImageUri
        val incomingText = sharedText

        ItemEditorDialog(
            item = selectedItem,
            sharedText = if (selectedItem == null) {
                incomingImage?.toString() ?: incomingText
            } else {
                null
            },
            onSave = { title, content, type, tags ->
                if (selectedItem == null) {
                    val finalType = if (incomingImage != null) {
                        "IMAGE"
                    } else {
                        type
                    }

                    val finalContent = incomingImage?.toString() ?: content

                    viewModel.addItem(
                        title = title,
                        content = finalContent,
                        type = finalType,
                        tags = tags,
                        cloud = cloudEnabled
                    )
                } else {
                    viewModel.updateItem(
                        selectedItem!!,
                        title,
                        content,
                        type,
                        tags
                    )
                }

                showEditor = false
                selectedItem = null

                if (!incomingText.isNullOrBlank()) {
                    onSharedTextHandled()
                }
                if (incomingImage != null) {
                    onSharedImageHandled()
                }
            },
            onCancel = {
                showEditor = false
                selectedItem = null

                if (!incomingText.isNullOrBlank()) {
                    onSharedTextHandled()
                }
                if (incomingImage != null) {
                    onSharedImageHandled()
                }
            }
        )
    }
}