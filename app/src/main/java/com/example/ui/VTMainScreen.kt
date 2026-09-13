package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.VTDrawerContent
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
import kotlinx.coroutines.launch

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
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // Auto-Scan Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.refreshVideos()
        }
    }

    // Auto-Scan on launch
    LaunchedEffect(Unit) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            viewModel.refreshVideos()
        } else {
            permissionLauncher.launch(permission)
        }
    }

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
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = activePlayingVideo == null,
                    drawerContent = {
                        VTDrawerContent(
                            viewModel = viewModel,
                            onCloseDrawer = {
                                coroutineScope.launch { drawerState.close() }
                            }
                        )
                    }
                ) {
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
                                        selected = currentTab == MainTab.SETTINGS || currentTab == MainTab.STREAM || currentTab == MainTab.VAULT,
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
                                    onPlayVideo = { video, playlist -> viewModel.playVideo(video, playlist) },
                                    onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                                )
                                MainTab.FOLDERS -> FolderListTab(
                                    viewModel = viewModel,
                                    onFolderSelected = { folder ->
                                        viewModel.setSelectedFolder(folder)
                                        viewModel.setTab(MainTab.VIDEOS)
                                    },
                                    onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                                )
                                MainTab.PLAYLISTS -> PlaylistsTab(
                                    viewModel = viewModel,
                                    onPlayVideo = { video, playlist -> viewModel.playVideo(video, playlist) }
                                )
                                MainTab.STREAM -> StreamTab(
                                    viewModel = viewModel,
                                    onPlayVideo = { video, playlist -> viewModel.playVideo(video, playlist) },
                                    onBack = { viewModel.setTab(MainTab.SETTINGS) }
                                )
                                MainTab.VAULT -> VaultTab(
                                    viewModel = viewModel,
                                    onPlayVideo = { video, playlist -> viewModel.playVideo(video, playlist) },
                                    onBack = { viewModel.setTab(MainTab.SETTINGS) }
                                )
                                MainTab.SETTINGS -> SettingsTab(
                                    viewModel = viewModel,
                                    onOpenStream = { viewModel.setTab(MainTab.STREAM) },
                                    onOpenVault = { viewModel.setTab(MainTab.VAULT) }
                                )
                            }
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
