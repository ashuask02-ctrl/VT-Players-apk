package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.StreamPreset
import com.example.ui.viewmodel.MainTab
import com.example.ui.viewmodel.VTViewModel

@Composable
fun VTDrawerContent(
    viewModel: VTViewModel,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val featuredStreams = viewModel.featuredStreams

    ModalDrawerSheet(
        drawerContainerColor = colorScheme.surface,
        drawerContentColor = colorScheme.onSurface,
        modifier = modifier
            .width(330.dp)
            .fillMaxHeight()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Header: VT Player branding + Close Button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colorScheme.primary)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "VT",
                                color = colorScheme.onPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "VT PLAYER",
                                color = colorScheme.onSurface,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "HD Video & Stream Player",
                                color = colorScheme.onSurface.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    IconButton(
                        onClick = onCloseDrawer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close Menu",
                            tint = colorScheme.onSurface
                        )
                    }
                }

                Divider(
                    color = colorScheme.outline.copy(alpha = 0.4f),
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }

            // ⭐ FEATURED STREAMS (Top of the Menu)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "FEATURED STREAMS",
                            color = colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Tap to stream instantly (HLS, 4K, CGI)",
                            color = colorScheme.onSurface.copy(alpha = 0.55f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // List of Featured Streams
            items(featuredStreams) { preset ->
                FeaturedStreamDrawerItem(
                    preset = preset,
                    onClick = {
                        viewModel.playFeaturedStream(preset)
                        onCloseDrawer()
                    }
                )
            }

            // Divider before navigation actions
            item {
                Divider(
                    color = colorScheme.outline.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
                )
                Text(
                    text = "DEVICE & TOOLS",
                    color = colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }

            // 🔄 Auto-Scan Device Storage
            item {
                DrawerNavigationTile(
                    icon = Icons.Filled.Refresh,
                    title = "Auto-Scan Device Storage",
                    subtitle = "Re-index video files & folders from storage",
                    onClick = {
                        viewModel.refreshVideos()
                        onCloseDrawer()
                    }
                )
            }

            // 🌐 Network & URL Stream
            item {
                DrawerNavigationTile(
                    icon = Icons.Filled.Link,
                    title = "Network & URL Stream",
                    subtitle = "Stream custom HTTP/HTTPS/HLS URL",
                    onClick = {
                        viewModel.setTab(MainTab.STREAM)
                        onCloseDrawer()
                    }
                )
            }

            // 📁 Video Folders
            item {
                DrawerNavigationTile(
                    icon = Icons.Filled.Folder,
                    title = "Video Folders",
                    subtitle = "View categorized device folders",
                    onClick = {
                        viewModel.setTab(MainTab.FOLDERS)
                        onCloseDrawer()
                    }
                )
            }

            // 📋 Playlists
            item {
                DrawerNavigationTile(
                    icon = Icons.Filled.PlaylistPlay,
                    title = "Playlists",
                    subtitle = "Manage custom video collections",
                    onClick = {
                        viewModel.setTab(MainTab.PLAYLISTS)
                        onCloseDrawer()
                    }
                )
            }

            // 🔒 Private Vault
            item {
                DrawerNavigationTile(
                    icon = Icons.Filled.Lock,
                    title = "Private Video Vault",
                    subtitle = "PIN protected hidden storage",
                    onClick = {
                        viewModel.setTab(MainTab.VAULT)
                        onCloseDrawer()
                    }
                )
            }

            // ⚙️ Settings
            item {
                DrawerNavigationTile(
                    icon = Icons.Filled.Settings,
                    title = "Player Settings",
                    subtitle = "Decoder, Audio Boost & Themes",
                    onClick = {
                        viewModel.setTab(MainTab.SETTINGS)
                        onCloseDrawer()
                    }
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun FeaturedStreamDrawerItem(
    preset: StreamPreset,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("featured_stream_${preset.title.take(10)}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Play Circle Icon
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Play",
                tint = colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = preset.title,
                color = colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = preset.description,
                color = colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Tag Chip
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(colorScheme.primary.copy(alpha = 0.18f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = preset.tag,
                color = colorScheme.primary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
fun DrawerNavigationTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Text(
                text = subtitle,
                color = colorScheme.onSurface.copy(alpha = 0.55f),
                fontSize = 10.sp
            )
        }
    }
}
