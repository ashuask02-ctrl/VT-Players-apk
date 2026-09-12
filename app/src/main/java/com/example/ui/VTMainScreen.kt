package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.player.VideoPlayerScreen
import com.example.ui.screens.FolderListTab
import com.example.ui.screens.PlaylistsTab
import com.example.ui.screens.SettingsTab
import com.example.ui.screens.StreamTab
import com.example.ui.screens.VaultTab
import com.example.ui.screens.VideoListTab
import com.example.ui.theme.VTPlayerTheme
import com.example.ui.viewmodel.MainTab
import com.example.ui.viewmodel.VTViewModel

@Composable
fun VTMainScreen(
    viewModel: VTViewModel,
    modifier: Modifier = Modifier
) {
    val prefs by viewModel.preferences.collectAsState()
    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (prefs.themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemDark
    }

    val fontScale = when (prefs.fontSize) {
        "SMALL" -> 0.85f
        "LARGE" -> 1.20f
        else -> 1.0f // "MEDIUM"
    }
    val currentDensity = LocalDensity.current

    VTPlayerTheme(darkTheme = isDarkTheme) {
        CompositionLocalProvider(
            LocalDensity provides Density(
                density = currentDensity.density,
                fontScale = currentDensity.fontScale * fontScale
            )
        ) {
            val currentTab by viewModel.currentTab.collectAsState()
            val activePlayingVideo by viewModel.activePlayingVideo.collectAsState()
            val colorScheme = MaterialTheme.colorScheme

            Box(modifier = modifier.fillMaxSize()) {
            Scaffold(
                bottomBar = {
                    if (activePlayingVideo == null) {
                        NavigationBar(
                            containerColor = colorScheme.surface,
                            contentColor = colorScheme.onSurface,
                            tonalElevation = 0.dp,
                            modifier = Modifier
                                .border(1.dp, colorScheme.outline)
                                .windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            NavigationBarItem(
                                selected = currentTab == MainTab.VIDEOS,
                                onClick = { viewModel.setTab(MainTab.VIDEOS) },
                                icon = { Icon(Icons.Filled.VideoLibrary, contentDescription = "Videos") },
                                label = { Text("Videos", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = colorScheme.onPrimary,
                                    selectedTextColor = colorScheme.onSurface,
                                    indicatorColor = colorScheme.primary,
                                    unselectedIconColor = colorScheme.onSurface.copy(alpha = 0.6f),
                                    unselectedTextColor = colorScheme.onSurface.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.testTag("nav_videos")
                            )

                            NavigationBarItem(
                                selected = currentTab == MainTab.FOLDERS,
                                onClick = { viewModel.setTab(MainTab.FOLDERS) },
                                icon = { Icon(Icons.Filled.Folder, contentDescription = "Folders") },
                                label = { Text("Folders", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = colorScheme.onPrimary,
                                    selectedTextColor = colorScheme.onSurface,
                                    indicatorColor = colorScheme.primary,
                                    unselectedIconColor = colorScheme.onSurface.copy(alpha = 0.6f),
                                    unselectedTextColor = colorScheme.onSurface.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.testTag("nav_folders")
                            )

                            NavigationBarItem(
                                selected = currentTab == MainTab.PLAYLISTS,
                                onClick = { viewModel.setTab(MainTab.PLAYLISTS) },
                                icon = { Icon(Icons.Filled.PlaylistPlay, contentDescription = "Playlists") },
                                label = { Text("Playlists", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = colorScheme.onPrimary,
                                    selectedTextColor = colorScheme.onSurface,
                                    indicatorColor = colorScheme.primary,
                                    unselectedIconColor = colorScheme.onSurface.copy(alpha = 0.6f),
                                    unselectedTextColor = colorScheme.onSurface.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.testTag("nav_playlists")
                            )

                            NavigationBarItem(
                                selected = currentTab == MainTab.STREAM,
                                onClick = { viewModel.setTab(MainTab.STREAM) },
                                icon = { Icon(Icons.Filled.Podcasts, contentDescription = "Stream") },
                                label = { Text("Stream", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = colorScheme.onPrimary,
                                    selectedTextColor = colorScheme.onSurface,
                                    indicatorColor = colorScheme.primary,
                                    unselectedIconColor = colorScheme.onSurface.copy(alpha = 0.6f),
                                    unselectedTextColor = colorScheme.onSurface.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.testTag("nav_stream")
                            )

                            NavigationBarItem(
                                selected = currentTab == MainTab.VAULT,
                                onClick = { viewModel.setTab(MainTab.VAULT) },
                                icon = { Icon(Icons.Filled.Lock, contentDescription = "Vault") },
                                label = { Text("Vault", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = colorScheme.onPrimary,
                                    selectedTextColor = colorScheme.onSurface,
                                    indicatorColor = colorScheme.primary,
                                    unselectedIconColor = colorScheme.onSurface.copy(alpha = 0.6f),
                                    unselectedTextColor = colorScheme.onSurface.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.testTag("nav_vault")
                            )

                            NavigationBarItem(
                                selected = currentTab == MainTab.SETTINGS,
                                onClick = { viewModel.setTab(MainTab.SETTINGS) },
                                icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                                label = { Text("Settings", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = colorScheme.onPrimary,
                                    selectedTextColor = colorScheme.onSurface,
                                    indicatorColor = colorScheme.primary,
                                    unselectedIconColor = colorScheme.onSurface.copy(alpha = 0.6f),
                                    unselectedTextColor = colorScheme.onSurface.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.testTag("nav_settings")
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (currentTab) {
                        MainTab.VIDEOS -> VideoListTab(
                            viewModel = viewModel,
                            onPlayVideo = { video, playlist -> viewModel.playVideo(video, playlist) }
                        )
                        MainTab.FOLDERS -> FolderListTab(
                            viewModel = viewModel,
                            onFolderSelected = { folder ->
                                viewModel.setSelectedFolder(folder)
                                viewModel.setTab(MainTab.VIDEOS)
                            }
                        )
                        MainTab.PLAYLISTS -> PlaylistsTab(
                            viewModel = viewModel,
                            onPlayVideo = { video, playlist -> viewModel.playVideo(video, playlist) }
                        )
                        MainTab.STREAM -> StreamTab(
                            viewModel = viewModel,
                            onPlayVideo = { video, playlist -> viewModel.playVideo(video, playlist) }
                        )
                        MainTab.VAULT -> VaultTab(
                            viewModel = viewModel,
                            onPlayVideo = { video, playlist -> viewModel.playVideo(video, playlist) }
                        )
                        MainTab.SETTINGS -> SettingsTab(
                            viewModel = viewModel
                        )
                    }
                }
            }

            // Immersive Video Player Overlay
            activePlayingVideo?.let { playingVideo ->
                VideoPlayerScreen(
                    viewModel = viewModel,
                    video = playingVideo,
                    onBack = { viewModel.closePlayer() }
                )
            }
        }
    }
}
}
