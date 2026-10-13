
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
import androidx.compose.foundation.layout.WindowInsets
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            // Title and type
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

            Spacer(modifier = Modifier.height(8.dp))

            // Created time
            Text(
                text = "Created: ${formatDateTime(item.createdAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Last updated time
            Text(
                text = "Last updated: ${formatDateTime(item.updatedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Image preview
            if (item.type.equals("IMAGE", ignoreCase = true)) {
                if (item.content.isNotBlank()) {
                    AsyncImage(
                        model = item.content,
                        contentDescription = item.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clickable {
                                showFullImage = true
                            },
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Tap image to view full screen",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "No image selected.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Link preview
            else if (item.type.equals("LINK", ignoreCase = true)) {
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

                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(url)
                            )

                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // The link could not be opened.
                        }
                    }
                )
            }

            // Notes and snippets
            else {
                Text(
                    text = item.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (showFullText) {
                        Int.MAX_VALUE
                    } else {
                        4
                    },
                    overflow = if (showFullText) {
                        TextOverflow.Clip
                    } else {
                        TextOverflow.Ellipsis
                    }
                )

                if (item.content.length > 150) {
                    TextButton(
                        onClick = {
                            showFullText = !showFullText
                        }
                    ) {
                        Text(
                            text = if (showFullText) {
                                "Show less"
                            } else {
                                "Read more"
                            }
                        )
                    }
                }
            }

            // Tags
            if (item.tags.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Tags: ${item.tags}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Edit and delete buttons
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

            Spacer(modifier = Modifier.height(8.dp))

            // Archive or restore button
            OutlinedButton(
                onClick = onArchive,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (item.isArchived) {
                        "Restore to Inbox"
                    } else {
                        "Archive"
                    }
                )
            }

            // Cloud save button
            if (isSignedIn && item.cloudId.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onSaveToCloud,
                    enabled = !isSavingToCloud,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isSavingToCloud) {
                            "Saving to cloud..."
                        } else {
                            "☁ Save to cloud"
                        }
                    )
                }
            } else if (!item.cloudId.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "☁ Saved to cloud",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    // TRUE FULLSCREEN IMAGE VIEWER
    if (showFullImage) {
        Dialog(
            onDismissRequest = {
                showFullImage = false
            },
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
                // Image fills the entire screen.
                AsyncImage(
                    model = item.content,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // Close button in the top-right corner.
                IconButton(
                    onClick = {
                        showFullImage = false
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(
                            top = 36.dp,
                            end = 16.dp
                        )
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
