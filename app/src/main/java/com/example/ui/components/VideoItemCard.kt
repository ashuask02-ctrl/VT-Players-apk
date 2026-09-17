package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VideoItem
import com.example.ui.theme.VTAccentRed
import com.example.ui.theme.VTBlack
import com.example.ui.theme.VTWhite

@Composable
fun VideoItemCard(
    video: VideoItem,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onHideToVault: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }

    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, colorScheme.outline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("video_item_${video.id}"),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Video Play Thumbnail / Badge
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorScheme.primary)
                        .border(1.dp, colorScheme.outline, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        tint = colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp)
                    )

                    // Down-left side overlay symbol ▶ on thumbnail
                    if (video.hasResumeProgress) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .background(VTBlack.copy(alpha = 0.8f), RoundedCornerShape(topEnd = 4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "▶",
                                color = VTWhite,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title & Info
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp)
                ) {
                    Text(
                        text = video.title,
                        color = colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = video.formattedDuration,
                            color = colorScheme.onSurface.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "•",
                            color = colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                        Text(
                            text = video.formattedSize,
                            color = colorScheme.onSurface.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "•",
                            color = colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                        Text(
                            text = video.folderName,
                            color = colorScheme.onSurface.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Favorite Quick Button
                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (video.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (video.isFavorite) VTAccentRed else colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Dropdown Menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Options",
                            tint = colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier
                            .background(colorScheme.surface)
                            .border(1.dp, colorScheme.outline, RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add to Playlist", fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Filled.PlaylistAdd, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onAddToPlaylist()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Move to Private Vault", fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onHideToVault()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Video Details", fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showDetails = true
                            }
                        )
                    }
                }
            }

            // Down side of video item: show symbol ▶ on the down-left side
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Down side Left side: Show symbol ▶
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (video.hasResumeProgress) colorScheme.primary.copy(alpha = 0.15f)
                            else colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .border(
                            1.dp,
                            if (video.hasResumeProgress) colorScheme.primary else colorScheme.outline.copy(alpha = 0.4f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "▶",
                        color = if (video.hasResumeProgress) colorScheme.primary else colorScheme.onSurface,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (video.hasResumeProgress) "Resume ${video.formattedLastPosition}" else "Play",
                        color = if (video.hasResumeProgress) colorScheme.primary else colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp
                    )
                }

                if (video.hasResumeProgress && video.duration > 0) {
                    val progressPercent = ((video.lastPosition.toFloat() / video.duration.toFloat()) * 100).toInt().coerceIn(1, 99)
                    Text(
                        text = "$progressPercent% watched",
                        color = colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                }
            }

            // Progress bar if video has resume position
            if (video.hasResumeProgress && video.duration > 0) {
                val progress = (video.lastPosition.toFloat() / video.duration.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.5.dp),
                    color = colorScheme.primary,
                    trackColor = colorScheme.outlineVariant
                )
            }
        }
    }

    if (showDetails) {
        VideoDetailsDialog(video = video, onDismiss = { showDetails = false })
    }
}

@Composable
fun VideoDetailsDialog(video: VideoItem, onDismiss: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.border(1.5.dp, colorScheme.outline, RoundedCornerShape(16.dp)),
        title = {
            Text("Video Details", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = colorScheme.onSurface)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                DetailRow("Title", video.title)
                DetailRow("Folder", video.folderName)
                DetailRow("Duration", video.formattedDuration)
                DetailRow("File Size", video.formattedSize)
                DetailRow("Resolution", video.resolution)
                DetailRow("Location", video.uriString)
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary, contentColor = colorScheme.onPrimary)
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    val colorScheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = colorScheme.onSurface.copy(alpha = 0.6f))
        Text(text = value, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
