
package com.example.recollect.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.recollect.data.room_database.SaveItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditorDialog(
    item: SaveItem?,
    onSave: (String, String, String, String) -> Unit,
    onCancel: () -> Unit,
    sharedText: String? = null
) {
    val initialSharedText = sharedText?.trim().orEmpty()

    val sharedIsImage = initialSharedText.startsWith("content://") ||
            initialSharedText.startsWith("file://")

    val sharedIsUrl =
        initialSharedText.startsWith("https://", ignoreCase = true) ||
                initialSharedText.startsWith("http://", ignoreCase = true)

    var title by remember(item, sharedText) {
        mutableStateOf(
            item?.title ?: when {
                sharedIsImage -> "Shared image"
                sharedIsUrl -> initialSharedText
                    .substringAfter("://")
                    .substringBefore("/")
                    .ifBlank { "Saved link" }
                initialSharedText.isNotBlank() -> initialSharedText
                    .lineSequence()
                    .first()
                    .take(50)
                else -> ""
            }
        )
    }

    var content by remember(item, sharedText) {
        mutableStateOf(item?.content ?: initialSharedText)
    }

    var type by remember(item, sharedText) {
        mutableStateOf(
            item?.type ?: when {
                sharedIsImage -> "IMAGE"
                sharedIsUrl -> "LINK"
                initialSharedText.isBlank() -> "NOTE"
                else -> "SNIPPET"
            }
        )
    }

    var tags by remember(item, sharedText) {
        mutableStateOf(item?.tags ?: "")
    }

    val context = LocalContext.current
    val types = listOf("NOTE", "LINK", "SNIPPET", "IMAGE")

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Some document providers do not support persistent access.
            } catch (_: IllegalArgumentException) {
                // The provider may not support persistable URI permissions.
            }

            content = uri.toString()
            type = "IMAGE"

            if (title.isBlank()) {
                title = "Saved image"
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onCancel
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = when {
                    item != null -> "Edit item"
                    initialSharedText.isNotBlank() -> "Save shared item"
                    else -> "Save something"
                },
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Item type",
                style = MaterialTheme.typography.titleSmall
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                items(types) { option ->
                    FilterChip(
                        selected = type == option,
                        onClick = { type = option },
                        label = { Text(option) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (type == "IMAGE") {
                Text(
                    text = when {
                        content.startsWith("content://") ||
                                content.startsWith("file://") ->
                            "An image URI is attached to this item."
                        content.startsWith("https://", ignoreCase = true) ->
                            "An image URL is attached to this item."
                        else ->
                            "Choose a picture from your device."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        imagePicker.launch(arrayOf("image/*"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (
                            content.startsWith("content://") ||
                            content.startsWith("file://") ||
                            content.startsWith("https://", ignoreCase = true)
                        ) {
                            "Choose another image"
                        } else {
                            "Choose image from phone"
                        }
                    )
                }

                if (content.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Image URI or URL") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            } else {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = {
                        Text(
                            when (type) {
                                "LINK" -> "URL"
                                "SNIPPET" -> "Snippet content"
                                else -> "Note content"
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text("Tags (comma separated)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    onSave(
                        title.trim(),
                        content.trim(),
                        type,
                        tags.trim()
                    )
                },
                enabled = title.isNotBlank() && content.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save item")
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}
