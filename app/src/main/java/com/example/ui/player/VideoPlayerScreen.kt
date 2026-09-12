package com.example.ui.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Build
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.model.AspectRatioMode
import com.example.model.VideoItem
import com.example.player.VTPlayerState
import com.example.ui.theme.VTAccentBlue
import com.example.ui.theme.VTAccentGreen
import com.example.ui.theme.VTBlack
import com.example.ui.theme.VTDarkBorder
import com.example.ui.theme.VTDarkCard
import com.example.ui.theme.VTWhite
import com.example.ui.viewmodel.VTViewModel
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    viewModel: VTViewModel,
    video: VideoItem,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val playerManager = viewModel.playerManager
    val playerState by playerManager.playerState.collectAsState()

    var showSyncDialog by remember { mutableStateOf(false) }
    var showFrameStepper by remember { mutableStateOf(false) }

    // Keep screen on during playback
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            // Restore portrait on exit if rotated
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Auto-hide controls after 4 seconds of playback
    LaunchedEffect(playerState.areControlsVisible, playerState.isPlaying) {
        if (playerState.areControlsVisible && playerState.isPlaying && !playerState.isControlsLocked) {
            delay(4000)
            playerManager.setControlsVisibility(false)
        }
    }

    // Intercept back button to close player
    BackHandler {
        onBack()
    }

    // Audio manager for gesture volume control
    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    val maxVolume = remember {
        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
    }

    // HUD gesture display states
    var hudVolume by remember { mutableStateOf<Float?>(null) }
    var hudBrightness by remember { mutableStateOf<Float?>(null) }
    var hudSeekDelta by remember { mutableStateOf<Long?>(null) }
    var hudSeekTarget by remember { mutableStateOf("") }

    // Reset HUD timer helper
    var hudHideTrigger by remember { mutableLongStateOf(0L) }
    LaunchedEffect(hudHideTrigger) {
        if (hudHideTrigger > 0L) {
            delay(1200)
            hudVolume = null
            hudBrightness = null
            hudSeekDelta = null
        }
    }

    // Aspect ratio resize mode for PlayerView
    val resizeMode = when (playerState.aspectRatioMode) {
        AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        AspectRatioMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
        AspectRatioMode.CROP_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        AspectRatioMode.RATIO_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        AspectRatioMode.RATIO_4_3 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        AspectRatioMode.RATIO_1_1 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VTBlack)
    ) {
        // Video Surface via AndroidView
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false
                    this.resizeMode = resizeMode
                    player = playerManager.getExoPlayer()
                }
            },
            update = { playerView ->
                playerView.player = playerManager.getExoPlayer()
                playerView.resizeMode = resizeMode
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = playerState.gestureZoomScale,
                    scaleY = playerState.gestureZoomScale
                )
        )

        // Gestures Layer (Pointer input for double tap, drag volume/brightness/seek, pinch zoom)
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Transform gesture for Pinch-to-zoom
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        if (!playerState.isControlsLocked) {
                            val newScale = (playerState.gestureZoomScale * zoom).coerceIn(0.75f, 3.0f)
                            playerManager.setZoomScale(newScale)
                        }
                    }
                }
                // Tap gestures for single tap (toggle controls) and double tap (seek or play/pause)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            if (playerState.isControlsLocked) {
                                playerManager.setControlsVisibility(true)
                            } else {
                                playerManager.toggleControlsVisibility()
                            }
                        },
                        onDoubleTap = { offset ->
                            if (!playerState.isControlsLocked) {
                                val width = size.width
                                when {
                                    offset.x < width * 0.35f -> {
                                        // Left double tap: seek -10s
                                        playerManager.seekBy(-10000)
                                        hudSeekDelta = -10L
                                        hudSeekTarget = playerState.formattedCurrentTime
                                        hudHideTrigger = System.currentTimeMillis()
                                    }
                                    offset.x > width * 0.65f -> {
                                        // Right double tap: seek +10s
                                        playerManager.seekBy(10000)
                                        hudSeekDelta = 10L
                                        hudSeekTarget = playerState.formattedCurrentTime
                                        hudHideTrigger = System.currentTimeMillis()
                                    }
                                    else -> {
                                        // Center double tap: toggle play/pause
                                        playerManager.togglePlayPause()
                                    }
                                }
                            }
                        }
                    )
                }
                // Drag gestures for left (brightness), right (volume), horizontal (seek)
                .pointerInput(Unit) {
                    var dragStartOffset = androidx.compose.ui.geometry.Offset.Zero
                    var initialDragType: Int = 0 // 1: brightness, 2: volume, 3: horizontal seek
                    var totalDragX = 0f
                    var initialPosition = 0L

                    detectDragGestures(
                        onDragStart = { offset ->
                            dragStartOffset = offset
                            totalDragX = 0f
                            initialPosition = playerState.currentPosition
                            initialDragType = 0
                        },
                        onDragEnd = {
                            if (initialDragType == 3 && hudSeekDelta != null) {
                                playerManager.seekTo(initialPosition + (hudSeekDelta!! * 1000L))
                            }
                            hudHideTrigger = System.currentTimeMillis()
                            initialDragType = 0
                        },
                        onDragCancel = {
                            hudHideTrigger = System.currentTimeMillis()
                            initialDragType = 0
                        },
                        onDrag = { change, dragAmount ->
                            if (playerState.isControlsLocked) return@detectDragGestures
                            change.consume()

                            val screenWidth = size.width
                            val screenHeight = size.height

                            // Determine drag type if not locked yet
                            if (initialDragType == 0) {
                                if (kotlin.math.abs(dragAmount.x) > kotlin.math.abs(dragAmount.y) * 1.5f) {
                                    initialDragType = 3 // Horizontal seek
                                } else if (kotlin.math.abs(dragAmount.y) > 10f) {
                                    initialDragType = if (dragStartOffset.x < screenWidth / 2) 1 else 2
                                }
                            }

                            when (initialDragType) {
                                1 -> {
                                    // Brightness (Left Side)
                                    val delta = -dragAmount.y / screenHeight
                                    val window = activity?.window
                                    val currentBrightness = window?.attributes?.screenBrightness ?: 0.5f
                                    val normalized = if (currentBrightness < 0) 0.5f else currentBrightness
                                    val newBrightness = (normalized + delta).coerceIn(0.01f, 1.0f)
                                    window?.let { w ->
                                        val lp = w.attributes
                                        lp.screenBrightness = newBrightness
                                        w.attributes = lp
                                    }
                                    hudBrightness = newBrightness
                                    hudHideTrigger = 0L
                                }
                                2 -> {
                                    // Volume (Right Side)
                                    val delta = -dragAmount.y / (screenHeight * 0.75f)
                                    val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                                    val newVol = (currentVol + delta * maxVolume).coerceIn(0f, maxVolume)
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol.toInt(), 0)
                                    hudVolume = newVol / maxVolume
                                    hudHideTrigger = 0L
                                }
                                3 -> {
                                    // Horizontal Seek
                                    totalDragX += dragAmount.x
                                    val deltaSec = (totalDragX / 15f).toLong()
                                    hudSeekDelta = deltaSec
                                    val targetMs = (initialPosition + deltaSec * 1000L).coerceIn(0L, playerState.duration)
                                    hudSeekTarget = formatMs(targetMs)
                                    hudHideTrigger = 0L
                                }
                            }
                        }
                    )
                }
        )

        // Buffering Indicator
        if (playerState.isBuffering) {
            CircularProgressIndicator(
                color = VTWhite,
                strokeWidth = 4.dp,
                modifier = Modifier
                    .size(56.dp)
                    .align(Alignment.Center)
            )
        }

        // On-Screen HUD Overlays (Volume, Brightness, Seek)
        hudVolume?.let { level ->
            VolumeBrightnessHUD(
                isVolume = true,
                level = level,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        hudBrightness?.let { level ->
            VolumeBrightnessHUD(
                isVolume = false,
                level = level,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        hudSeekDelta?.let { delta ->
            SeekHUD(
                deltaSec = delta,
                targetPositionFormatted = hudSeekTarget,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Lock/Unlock floating toggle (Always visible when locked, so user can unlock!)
        if (playerState.isControlsLocked) {
            IconButton(
                onClick = { playerManager.toggleControlsLock() },
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .size(52.dp)
                    .align(Alignment.TopStart)
                    .clip(CircleShape)
                    .background(VTBlack.copy(alpha = 0.85f))
                    .border(2.dp, VTWhite, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Unlock Controls",
                    tint = VTWhite,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Player UI Controls Overlay
        AnimatedVisibility(
            visible = playerState.areControlsVisible && !playerState.isControlsLocked,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(VTBlack.copy(alpha = 0.55f))
            ) {
                // Top App Bar
                PlayerTopBar(
                    videoTitle = video.title,
                    resolution = video.resolution,
                    aspectRatioMode = playerState.aspectRatioMode,
                    isLocked = playerState.isControlsLocked,
                    onBack = onBack,
                    onAudioTracksClick = { playerManager.setShowTracksDialog(true) },
                    onSubtitlesClick = { playerManager.setShowTracksDialog(true) },
                    onAspectRatioClick = {
                        val modes = AspectRatioMode.values()
                        val nextIdx = (playerState.aspectRatioMode.ordinal + 1) % modes.size
                        playerManager.setAspectRatioMode(modes[nextIdx])
                    },
                    onLockClick = { playerManager.toggleControlsLock() },
                    onSpeedClick = { playerManager.setShowSpeedDialog(true) },
                    onSyncClick = { showSyncDialog = true },
                    onEqualizerClick = { playerManager.setShowEqualizerDialog(true) },
                    onFrameStepperClick = { showFrameStepper = true },
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // Center Play / Pause / Skip Controls
                PlayerCenterControls(
                    isPlaying = playerState.isPlaying,
                    onPlayPause = { playerManager.togglePlayPause() },
                    onRewind = { playerManager.seekBy(-10000) },
                    onForward = { playerManager.seekBy(10000) },
                    onPrevious = { playerManager.previous() },
                    onNext = { playerManager.next() },
                    modifier = Modifier.align(Alignment.Center)
                )

                // Bottom Controls (Seekbar, Time, Quick Action Chips)
                PlayerBottomControls(
                    playerState = playerState,
                    onSeek = { targetMs -> playerManager.seekTo(targetMs) },
                    onSpeedClick = { playerManager.setShowSpeedDialog(true) },
                    onRotateClick = {
                        activity?.let { act ->
                            act.requestedOrientation = if (
                                act.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            ) {
                                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            } else {
                                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            }
                        }
                    },
                    onPipClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            try {
                                val params = android.app.PictureInPictureParams.Builder().build()
                                activity?.enterPictureInPictureMode(params)
                            } catch (_: Exception) {}
                        }
                    },
                    onBackgroundPlaybackToggle = { playerManager.toggleBackgroundPlayback() },
                    onAbRepeatClick = {
                        when {
                            playerState.repeatPointA == null -> playerManager.setRepeatPointA()
                            playerState.repeatPointB == null -> playerManager.setRepeatPointB()
                            else -> playerManager.clearAbRepeat()
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }

    // Modal Dialogs
    if (playerState.showSpeedDialog) {
        SpeedDialog(
            currentSpeed = playerState.playbackSpeed,
            onSpeedSelected = { playerManager.setPlaybackSpeed(it) },
            onDismiss = { playerManager.setShowSpeedDialog(false) }
        )
    }

    if (playerState.showTracksDialog) {
        TracksDialog(
            playerManager = playerManager,
            playerState = playerState,
            onExternalSubtitleSelected = { uri ->
                // Reload with external subtitle
                viewModel.playVideo(video, playerState.playlist, uri)
                playerManager.setShowTracksDialog(false)
            },
            onDismiss = { playerManager.setShowTracksDialog(false) }
        )
    }

    if (playerState.showEqualizerDialog) {
        AudioBoostDialog(
            playerManager = playerManager,
            playerState = playerState,
            onDismiss = { playerManager.setShowEqualizerDialog(false) }
        )
    }

    if (showSyncDialog) {
        SubtitleAndAudioSyncDialog(
            playerManager = playerManager,
            playerState = playerState,
            onDismiss = { showSyncDialog = false }
        )
    }

    if (showFrameStepper) {
        FrameByFrameDialog(
            playerManager = playerManager,
            playerState = playerState,
            onDismiss = { showFrameStepper = false }
        )
    }
}

@Composable
fun PlayerTopBar(
    videoTitle: String,
    resolution: String,
    aspectRatioMode: AspectRatioMode,
    isLocked: Boolean,
    onBack: () -> Unit,
    onAudioTracksClick: () -> Unit,
    onSubtitlesClick: () -> Unit,
    onAspectRatioClick: () -> Unit,
    onLockClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onSyncClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    onFrameStepperClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("player_back_button")
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = VTWhite,
                modifier = Modifier.size(28.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = videoTitle,
                color = VTWhite,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(VTWhite)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "VT PLAYER",
                        color = VTBlack,
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = resolution,
                    color = Color(0xFFCCCCCC),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Quick Audio Track Selector
        IconButton(onClick = onAudioTracksClick) {
            Icon(Icons.Filled.Audiotrack, contentDescription = "Audio Tracks", tint = VTWhite)
        }

        // Subtitles Button
        IconButton(onClick = onSubtitlesClick) {
            Icon(Icons.Filled.Subtitles, contentDescription = "Subtitles", tint = VTWhite)
        }

        // Aspect Ratio Toggle
        IconButton(onClick = onAspectRatioClick) {
            Icon(Icons.Filled.AspectRatio, contentDescription = "Aspect Ratio", tint = VTWhite)
        }

        // Lock Screen Controls
        IconButton(onClick = onLockClick) {
            Icon(
                if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                contentDescription = "Lock Controls",
                tint = VTWhite
            )
        }

        // More Menu
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More Options", tint = VTWhite)
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier
                    .background(VTBlack)
                    .border(1.dp, VTWhite, RoundedCornerShape(8.dp))
            ) {
                DropdownMenuItem(
                    text = { Text("Playback Speed", color = VTWhite, fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Filled.Speed, contentDescription = null, tint = VTWhite) },
                    onClick = {
                        showMenu = false
                        onSpeedClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Audio/Subtitle Sync", color = VTWhite, fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Filled.Sync, contentDescription = null, tint = VTWhite) },
                    onClick = {
                        showMenu = false
                        onSyncClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Audio Boost (+12dB)", color = VTWhite, fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = VTWhite) },
                    onClick = {
                        showMenu = false
                        onEqualizerClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Frame-by-Frame", color = VTWhite, fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Filled.Tune, contentDescription = null, tint = VTWhite) },
                    onClick = {
                        showMenu = false
                        onFrameStepperClick()
                    }
                )
            }
        }
    }
}

@Composable
fun PlayerCenterControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous Video
        IconButton(
            onClick = onPrevious,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = VTWhite, modifier = Modifier.size(32.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Rewind 10s
        IconButton(
            onClick = onRewind,
            modifier = Modifier.size(52.dp)
        ) {
            Icon(Icons.Filled.FastRewind, contentDescription = "Rewind 10s", tint = VTWhite, modifier = Modifier.size(36.dp))
        }

        Spacer(modifier = Modifier.width(20.dp))

        // Big Play / Pause Button with High Contrast Solid White Circle
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(VTWhite)
                .clickable(onClick = onPlayPause)
                .testTag("player_play_pause_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = VTBlack,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.width(20.dp))

        // Fast Forward 10s
        IconButton(
            onClick = onForward,
            modifier = Modifier.size(52.dp)
        ) {
            Icon(Icons.Filled.FastForward, contentDescription = "Forward 10s", tint = VTWhite, modifier = Modifier.size(36.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Next Video
        IconButton(
            onClick = onNext,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = VTWhite, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun PlayerBottomControls(
    playerState: VTPlayerState,
    onSeek: (Long) -> Unit,
    onSpeedClick: () -> Unit,
    onRotateClick: () -> Unit,
    onPipClick: () -> Unit,
    onBackgroundPlaybackToggle: () -> Unit,
    onAbRepeatClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Time stamps + Seekbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = playerState.formattedCurrentTime,
                color = VTWhite,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp
            )
            Text(
                text = playerState.formattedDuration,
                color = Color(0xFFCCCCCC),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        // Custom Slider with White thumb and high-contrast track
        var isDraggingSeek by remember { mutableStateOf(false) }
        var tempPosition by remember { mutableFloatStateOf(0f) }

        Slider(
            value = if (isDraggingSeek) tempPosition else playerState.currentPosition.toFloat(),
            onValueChange = {
                isDraggingSeek = true
                tempPosition = it
            },
            onValueChangeFinished = {
                isDraggingSeek = false
                onSeek(tempPosition.toLong())
            },
            valueRange = 0f..(playerState.duration.coerceAtLeast(1L).toFloat()),
            colors = SliderDefaults.colors(
                thumbColor = VTWhite,
                activeTrackColor = VTWhite,
                inactiveTrackColor = Color(0xFF444444)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("player_seek_bar")
        )

        // Bottom Action Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speed Chip
            FilterChip(
                selected = playerState.playbackSpeed != 1.0f,
                onClick = onSpeedClick,
                label = {
                    Text(
                        "${playerState.playbackSpeed}x",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = VTWhite,
                    selectedLabelColor = VTBlack,
                    containerColor = VTDarkCard,
                    labelColor = VTWhite
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = playerState.playbackSpeed != 1.0f,
                    borderColor = VTWhite,
                    selectedBorderColor = VTWhite
                )
            )

            // A-B Repeat Chip
            val abLabel = when {
                playerState.repeatPointA == null -> "A-B Repeat"
                playerState.repeatPointB == null -> "A: Set -> B?"
                else -> "A-B Active"
            }
            FilterChip(
                selected = playerState.isAbRepeatActive,
                onClick = onAbRepeatClick,
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Repeat, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(abLabel, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = VTWhite,
                    selectedLabelColor = VTBlack,
                    containerColor = VTDarkCard,
                    labelColor = VTWhite
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = playerState.isAbRepeatActive,
                    borderColor = VTWhite,
                    selectedBorderColor = VTWhite
                )
            )

            // Background Audio Playback Chip
            FilterChip(
                selected = playerState.isBackgroundPlaybackEnabled,
                onClick = onBackgroundPlaybackToggle,
                label = {
                    Icon(
                        Icons.Filled.Headphones,
                        contentDescription = "Background Audio",
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = VTWhite,
                    selectedLabelColor = VTBlack,
                    containerColor = VTDarkCard,
                    labelColor = VTWhite
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = playerState.isBackgroundPlaybackEnabled,
                    borderColor = VTWhite,
                    selectedBorderColor = VTWhite
                )
            )

            // Picture in Picture
            IconButton(
                onClick = onPipClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Filled.PictureInPictureAlt,
                    contentDescription = "Picture in Picture",
                    tint = VTWhite,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Screen Orientation Lock / Rotate
            IconButton(
                onClick = onRotateClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Filled.ScreenRotation,
                    contentDescription = "Screen Rotation",
                    tint = VTWhite,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSecs = (ms / 1000).coerceAtLeast(0)
    val hours = totalSecs / 3600
    val minutes = (totalSecs % 3600) / 60
    val seconds = totalSecs % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
