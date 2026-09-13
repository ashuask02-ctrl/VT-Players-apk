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
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.FitScreen
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val prefs by viewModel.preferences.collectAsState()

    var showSyncDialog by remember { mutableStateOf(false) }
    var showFrameStepper by remember { mutableStateOf(false) }

    // Keep screen on during playback, auto-switch to horizontal (landscape) for movie playback, and hide status bar
    DisposableEffect(prefs.hideStatusBarInPlayer, prefs.movieOrientation) {
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Movie Play Screen in VT Player: show Horizontal (Landscape) by default, not Vertical
        when (prefs.movieOrientation) {
            "VERTICAL" -> activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            "AUTO" -> activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
            else -> activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }

        val insetsController = window?.let { w ->
            WindowCompat.getInsetsController(w, w.decorView)
        }

        if (prefs.hideStatusBarInPlayer) {
            insetsController?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            // Hide status bar (mobile signal, battery, and time line) and system navigation bar
            insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
            // Restore normal / portrait orientation on exit so library screens stay upright
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
    var hudZoomScale by remember { mutableStateOf<Float?>(null) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }

    // Tap tracking for single & double tap without conflicting with scroll gestures
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var lastTapOffset by remember { mutableStateOf(Offset.Zero) }

    // Reset HUD timer helper
    var hudHideTrigger by remember { mutableLongStateOf(0L) }
    LaunchedEffect(hudHideTrigger) {
        if (hudHideTrigger > 0L) {
            delay(1200)
            hudVolume = null
            hudBrightness = null
            hudSeekDelta = null
            hudZoomScale = null
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
                    subtitleView?.setUserDefaultStyle()
                    subtitleView?.setUserDefaultTextSize()
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
                    scaleY = playerState.gestureZoomScale,
                    translationX = panX,
                    translationY = panY
                )
        )

        // Gestures Layer (Unified single pointer handler for tap, double-tap, vertical drag volume/brightness, horizontal seek, and 2-finger zoom)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(prefs.volumeGesture, prefs.brightnessGesture, prefs.swipeSeekGesture) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downOffset = down.position
                        val startTime = System.currentTimeMillis()
                        var isDrag = false
                        var dragMode = 0 // 0: undecided, 1: brightness, 2: volume, 3: seek, 4: pinch_zoom
                        val touchSlop = viewConfiguration.touchSlop
                        val screenWidth = size.width.toFloat()
                        val screenHeight = size.height.toFloat()

                        // Initialize continuous floating-point volume starting from current stream volume
                        val initialVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                        var currentFloatVol = initialVol

                        val window = activity?.window
                        val currentWinBrightness = window?.attributes?.screenBrightness ?: -1f
                        val initialBrightness = if (currentWinBrightness < 0f) 0.5f else currentWinBrightness
                        var currentFloatBrightness = initialBrightness

                        val initialPosition = playerState.currentPosition
                        var seekDeltaSec = 0L

                        do {
                            val event = awaitPointerEvent()
                            val activePointers = event.changes.filter { it.pressed }

                            if (playerState.isControlsLocked) {
                                continue
                            }

                            if (activePointers.size >= 2) {
                                // Multi-finger pinch-to-zoom & pan
                                isDrag = true
                                dragMode = 4
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()

                                val currentScale = playerState.gestureZoomScale
                                val newScale = (currentScale * zoomChange).coerceIn(0.5f, 4.0f)
                                val maxPanX = if (newScale > 1f) (screenWidth * (newScale - 1f)) / 2f + 120f else 0f
                                val maxPanY = if (newScale > 1f) (screenHeight * (newScale - 1f)) / 2f + 120f else 0f
                                val newPanX = if (newScale > 1f) (panX + panChange.x).coerceIn(-maxPanX, maxPanX) else 0f
                                val newPanY = if (newScale > 1f) (panY + panChange.y).coerceIn(-maxPanY, maxPanY) else 0f
                                panX = newPanX
                                panY = newPanY
                                playerManager.setZoomAndPan(newScale, newPanX, newPanY)
                                hudZoomScale = newScale
                                hudHideTrigger = 0L

                                event.changes.forEach { it.consume() }
                            } else if (activePointers.size == 1 && dragMode != 4) {
                                val pointer = activePointers.first()
                                val currentPos = pointer.position
                                val totalDragX = currentPos.x - downOffset.x
                                val totalDragY = currentPos.y - downOffset.y

                                if (!isDrag) {
                                    val absX = kotlin.math.abs(totalDragX)
                                    val absY = kotlin.math.abs(totalDragY)
                                    if (absX > touchSlop || absY > touchSlop) {
                                        isDrag = true
                                        if (absX > absY) {
                                            if (prefs.swipeSeekGesture) dragMode = 3
                                        } else {
                                            if (downOffset.x < screenWidth * 0.5f) {
                                                if (prefs.brightnessGesture) dragMode = 1
                                            } else {
                                                if (prefs.volumeGesture) dragMode = 2
                                            }
                                        }
                                    }
                                }

                                if (isDrag) {
                                    pointer.consume()
                                    when (dragMode) {
                                        1 -> {
                                            // Brightness (Left Side): Drag UP (downOffset.y > currentPos.y) increases, Drag DOWN decreases
                                            val dragUpDistance = downOffset.y - currentPos.y
                                            val fraction = dragUpDistance / (screenHeight * 0.55f)
                                            currentFloatBrightness = (initialBrightness + fraction).coerceIn(0.01f, 1.0f)
                                            window?.let { w ->
                                                val lp = w.attributes
                                                lp.screenBrightness = currentFloatBrightness
                                                w.attributes = lp
                                            }
                                            hudBrightness = currentFloatBrightness
                                            hudHideTrigger = 0L
                                        }
                                        2 -> {
                                            // Volume (Right Side): Drag UP (downOffset.y > currentPos.y) increases, Drag DOWN decreases
                                            val dragUpDistance = downOffset.y - currentPos.y
                                            val fraction = dragUpDistance / (screenHeight * 0.55f)
                                            currentFloatVol = (initialVol + fraction * maxVolume).coerceIn(0f, maxVolume)
                                            val intVol = kotlin.math.round(currentFloatVol).toInt().coerceIn(0, maxVolume.toInt())
                                            try {
                                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, intVol, 0)
                                            } catch (_: Exception) {}
                                            hudVolume = (currentFloatVol / maxVolume).coerceIn(0f, 1f)
                                            hudHideTrigger = 0L
                                        }
                                        3 -> {
                                            // Horizontal Seek
                                            val fraction = totalDragX / (screenWidth * 0.40f)
                                            seekDeltaSec = (fraction * 90f).toLong()
                                            hudSeekDelta = seekDeltaSec
                                            val targetMs = (initialPosition + seekDeltaSec * 1000L).coerceIn(0L, playerState.duration)
                                            hudSeekTarget = formatMs(targetMs)
                                            hudHideTrigger = 0L
                                        }
                                    }
                                }
                            }
                        } while (event.changes.any { it.pressed })

                        // When user lifts finger:
                        if (isDrag) {
                            if (dragMode == 3 && seekDeltaSec != 0L) {
                                val finalTarget = (initialPosition + (seekDeltaSec * 1000L)).coerceIn(0L, playerState.duration)
                                playerManager.seekTo(finalTarget)
                            }
                            hudHideTrigger = System.currentTimeMillis()
                        } else {
                            // Finger lifted without dragging: It's a TAP
                            val tapDuration = System.currentTimeMillis() - startTime
                            if (tapDuration < 400L) {
                                val now = System.currentTimeMillis()
                                val timeSinceLastTap = now - lastTapTime
                                val distSq = (downOffset.x - lastTapOffset.x) * (downOffset.x - lastTapOffset.x) +
                                    (downOffset.y - lastTapOffset.y) * (downOffset.y - lastTapOffset.y)

                                if (timeSinceLastTap < 350L && distSq < (touchSlop * 4f) * (touchSlop * 4f)) {
                                    // DOUBLE TAP
                                    lastTapTime = 0L
                                    if (!playerState.isControlsLocked) {
                                        when {
                                            downOffset.x < screenWidth * 0.35f -> {
                                                // Left double tap: seek -10s
                                                playerManager.seekBy(-10000)
                                                hudSeekDelta = -10L
                                                hudSeekTarget = playerState.formattedCurrentTime
                                                hudHideTrigger = System.currentTimeMillis()
                                            }
                                            downOffset.x > screenWidth * 0.65f -> {
                                                // Right double tap: seek +10s
                                                playerManager.seekBy(10000)
                                                hudSeekDelta = 10L
                                                hudSeekTarget = playerState.formattedCurrentTime
                                                hudHideTrigger = System.currentTimeMillis()
                                            }
                                            else -> {
                                                // Center double tap: reset screen size if zoomed, else toggle play/pause
                                                if (playerState.gestureZoomScale > 1.05f || panX != 0f || panY != 0f) {
                                                    panX = 0f
                                                    panY = 0f
                                                    playerManager.resetZoomAndPan()
                                                    hudZoomScale = 1.0f
                                                    hudHideTrigger = System.currentTimeMillis()
                                                } else {
                                                    playerManager.togglePlayPause()
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // SINGLE TAP
                                    lastTapTime = now
                                    lastTapOffset = downOffset
                                    if (playerState.isControlsLocked) {
                                        playerManager.setControlsVisibility(true)
                                    } else {
                                        playerManager.toggleControlsVisibility()
                                    }
                                }
                            }
                        }
                    }
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
        hudZoomScale?.let { scale ->
            ZoomScrollHUD(
                zoomScale = scale,
                isPanning = panX != 0f || panY != 0f,
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
                        panX = 0f
                        panY = 0f
                        playerManager.resetZoomAndPan()
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
                            val current = act.requestedOrientation
                            val isLandscape = current == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE ||
                                current == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE ||
                                current == ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE ||
                                current == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                            act.requestedOrientation = if (isLandscape) {
                                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            } else {
                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
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
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
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
