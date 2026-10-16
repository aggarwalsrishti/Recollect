
package com.example.recollect.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.recollect.data.room_database.SaveItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatDateTime(timestamp: Long): String {
    val formatter = SimpleDateFormat(
        "dd MMM yyyy, hh:mm a",
        Locale.getDefault()
    )
    return formatter.format(Date(timestamp))
}

/*
 * New items store an absolute path inside Recollect's files directory.
 * This also supports older content:// and https:// image references.
 */
private fun imageModel(content: String): Any {
    return when {
        content.startsWith("content://") ||
                content.startsWith("file://") ||
                content.startsWith("http://") ||
                content.startsWith("https://") -> Uri.parse(content)

        else -> File(content)
    }
}

@Composable
fun SaveItemCard(
    item: SaveItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    onSaveToCloud: () -> Unit = {},
    isSavingToCloud: Boolean = false,
    isSignedIn: Boolean = false
) {
    val context = LocalContext.current

    var showFullText by remember { mutableStateOf(false) }
    var showFullImage by remember { mutableStateOf(false) }

    val isImage = item.type.equals("IMAGE", ignoreCase = true)
    val imageExists = remember(item.content) {
        when {
            item.content.startsWith("content://") ||
                    item.content.startsWith("file://") ||
                    item.content.startsWith("http://") ||
                    item.content.startsWith("https://") -> true

            else -> File(item.content).exists()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = item.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = item.type,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Created: ${formatDateTime(item.createdAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(3.dp))

            Text(
                text = "Last updated: ${formatDateTime(item.updatedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            if (isImage) {
                if (item.content.isNotBlank() && imageExists) {
                    AsyncImage(
                        model = imageModel(item.content),
                        contentDescription = item.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clickable { showFullImage = true },
                        contentScale = ContentScale.Crop
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Tap image to view full screen",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Image is unavailable. Edit this item to select the image again.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else if (item.type.equals("LINK", ignoreCase = true)) {
                Text(
                    text = item.content,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable {
                        try {
                            val url = if (
                                item.content.startsWith("http://") ||
                                item.content.startsWith("https://")
                            ) {
                                item.content
                            } else {
                                "https://${item.content}"
                            }

                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            )
                        } catch (_: Exception) {
                            // The link could not be opened.
                        }
                    }
                )
            } else {
                Text(
                    text = item.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (showFullText) Int.MAX_VALUE else 4,
                    overflow = if (showFullText) {
                        TextOverflow.Clip
                    } else {
                        TextOverflow.Ellipsis
                    }
                )

                if (item.content.length > 150) {
                    TextButton(
                        onClick = { showFullText = !showFullText }
                    ) {
                        Text(if (showFullText) "Show less" else "Read more")
                    }
                }
            }

            if (item.tags.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Tags: ${item.tags}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Edit")
                }

                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Delete")
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = onArchive,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (item.isArchived) "Restore to Inbox" else "Archive")
            }

            if (isSignedIn && item.cloudId.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = onSaveToCloud,
                    enabled = !isSavingToCloud,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (isSavingToCloud) "Saving to cloud..."
                        else "☁ Save to cloud"
                    )
                }
            } else if (!item.cloudId.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))

                Text(
                    text = "☁ Saved to cloud",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (showFullImage && isImage && imageExists) {
        Dialog(
            onDismissRequest = { showFullImage = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = imageModel(item.content),
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                IconButton(
                    onClick = { showFullImage = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 36.dp, end = 16.dp)
                        .background(
                            color = Color.DarkGray.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(50)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close fullscreen image",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
