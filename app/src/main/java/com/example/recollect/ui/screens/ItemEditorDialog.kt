
package com.example.recollect.ui.screens

import android.content.Context
import android.net.Uri
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Copies an image into Recollect's private files directory.
 * The returned path remains available after the app restarts.
 */
private suspend fun saveImageInternally(
    context: Context,
    imageUri: Uri
): String = withContext(Dispatchers.IO) {

    val imageDirectory = File(context.filesDir, "saved_images")

    if (!imageDirectory.exists() && !imageDirectory.mkdirs()) {
        throw IOException("Could not create image storage directory.")
    }

    val imageFile = File(
        imageDirectory,
        "image_${System.currentTimeMillis()}_${(1000..9999).random()}.img"
    )

    try {
        val inputStream = context.contentResolver.openInputStream(imageUri)
            ?: throw IOException("Could not open the selected image.")

        inputStream.use { input ->
            imageFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        if (!imageFile.exists() || imageFile.length() == 0L) {
            throw IOException("The image file is empty.")
        }

        imageFile.absolutePath
    } catch (e: Exception) {
        imageFile.delete()
        throw e
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditorDialog(
    item: SaveItem?,
    onSave: (String, String, String, String) -> Unit,
    onCancel: () -> Unit,
    sharedText: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val types = listOf("NOTE", "LINK", "SNIPPET", "IMAGE")

    val initialSharedText = sharedText?.trim().orEmpty()

    val sharedIsImage =
        initialSharedText.startsWith("content://") ||
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

                initialSharedText.isNotBlank() ->
                    initialSharedText.lineSequence().first().take(50)

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

    var isCopyingImage by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    /*
     * Copy an image into internal storage before saving its path.
     */
    fun copyImage(uri: Uri) {
        if (isCopyingImage) return

        isCopyingImage = true
        errorMessage = null

        scope.launch {
            try {
                val permanentPath = saveImageInternally(context, uri)

                content = permanentPath
                type = "IMAGE"

                if (title.isBlank() || title == "Shared image") {
                    title = "Saved image"
                }
            } catch (e: Exception) {
                errorMessage =
                    e.message ?: "Couldn't save this image. Please select it again."
            } finally {
                isCopyingImage = false
            }
        }
    }

    /*
     * Automatically copy an image shared from another app.
     */
    LaunchedEffect(sharedText, item) {
        if (
            item == null &&
            sharedIsImage &&
            content == initialSharedText
        ) {
            copyImage(Uri.parse(initialSharedText))
        }
    }

    /*
     * Let the user select an image from the phone.
     */
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            copyImage(uri)
        }
    }

    val isRemoteImage =
        content.startsWith("http://", ignoreCase = true) ||
                content.startsWith("https://", ignoreCase = true)

    val localImageExists = remember(content) {
        isRemoteImage || (content.isNotBlank() && File(content).isFile)
    }

    val canSaveImage =
        type != "IMAGE" || (content.isNotBlank() && localImageExists)

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
                        onClick = {
                            type = option
                            errorMessage = null
                        },
                        enabled = !isCopyingImage,
                        label = { Text(option) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (type == "IMAGE") {
                Text(
                    text = when {
                        isCopyingImage ->
                            "Saving image to Recollect..."

                        content.isBlank() ->
                            "Choose a picture from your device."

                        isRemoteImage ->
                            "An image URL is attached."

                        localImageExists ->
                            "Image stored in Recollect."

                        else ->
                            "The image file is missing. Please select it again."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        imagePicker.launch(arrayOf("image/*"))
                    },
                    enabled = !isCopyingImage,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (content.isNotBlank()) {
                            "Choose another image"
                        } else {
                            "Choose image from phone"
                        }
                    )
                }

                if (isRemoteImage) {
                    OutlinedTextField(
                        value = content,
                        onValueChange = {
                            content = it
                            errorMessage = null
                        },
                        label = { Text("Image URL") },
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

            if (errorMessage != null) {
                Spacer(Modifier.height(8.dp))

                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
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
                enabled = title.isNotBlank() &&
                        content.isNotBlank() &&
                        !isCopyingImage &&
                        errorMessage == null &&
                        canSaveImage,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (isCopyingImage) {
                        "Saving image..."
                    } else {
                        "Save item"
                    }
                )
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}
