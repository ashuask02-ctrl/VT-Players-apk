package com.example.ui.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Rect
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.example.model.DecoderMode
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
    var hudVolumePercent by remember { mutableStateOf<Int?>(null) }
    var hudBrightness by remember { mutableStateOf<Float?>(null) }
    var hudSeekDelta by remember { mutableStateOf<Long?>(null) }
    var hudSeekTarget by remember { mutableStateOf("") }
    var hudZoomScale by remember { mutableStateOf<Float?>(null) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }

    // Tap tracking for single & double tap without conflicting with scroll gestures
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var lastTapOffset by remember { mutableStateOf(Offset.Zero) }

    // Double tap ripple animation states
    var doubleTapLeftAnim by remember { mutableStateOf(false) }
    var doubleTapRightAnim by remember { mutableStateOf(false) }
    var showCameraFlash by remember { mutableStateOf(false) }
    var hudMessage by remember { mutableStateOf<String?>(null) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    LaunchedEffect(doubleTapLeftAnim) {
        if (doubleTapLeftAnim) {
            delay(600)
            doubleTapLeftAnim = false
        }
    }

    LaunchedEffect(doubleTapRightAnim) {
        if (doubleTapRightAnim) {
            delay(600)
            doubleTapRightAnim = false
        }
    }

    LaunchedEffect(showCameraFlash) {
        if (showCameraFlash) {
            delay(120)
            showCameraFlash = false
        }
    }

    LaunchedEffect(hudMessage) {
        if (hudMessage != null) {
            delay(2200)
            hudMessage = null
        }
    }

    fun takeScreenshot() {
        val act = activity ?: return
        showCameraFlash = true
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val window = act.window ?: return
                val view = window.decorView
                val bitmap = Bitmap.createBitmap(
                    view.width.coerceAtLeast(1),
                    view.height.coerceAtLeast(1),
                    Bitmap.Config.ARGB_8888
                )
                val location = IntArray(2)
                view.getLocationInWindow(location)
                PixelCopy.request(
                    window,
                    Rect(location[0], location[1], location[0] + view.width, location[1] + view.height),
                    bitmap,
                    { copyResult ->
                        if (copyResult == PixelCopy.SUCCESS) {
                            hudMessage = "📷 Screenshot Saved"
                        } else {
                            hudMessage = "📷 Screenshot Captured"
                        }
                    },
                    Handler(Looper.getMainLooper())
                )
            } else {
                hudMessage = "📷 Screenshot Captured"
            }
        } catch (_: Exception) {
            hudMessage = "📷 Screenshot Captured"
        }
    }

    // Reset HUD timer helper
    var hudHideTrigger by remember { mutableLongStateOf(0L) }
    LaunchedEffect(hudHideTrigger) {
        if (hudHideTrigger > 0L) {
            delay(1200)
            hudVolumePercent = null
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
                    subtitleView?.setUserDefaultStyle()
                    subtitleView?.setUserDefaultTextSize()
                    playerManager.attachPlayerView(this)
                }
            },
            update = { playerView ->
                playerManager.attachPlayerView(playerView)
                playerView.resizeMode = resizeMode
            },
            onRelease = {
                playerManager.attachPlayerView(null)
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

                        // Initialize starting volume percentage from player state
                        val initialVolPercent = playerState.volumePercent

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
                                            // SW and HW volume fixed to 0% – 100% (Standard device music volume)
                                            // HW+ volume fixed to 101% – 150%: +100% Audio Boost driven by Loudness Enhancer
                                            val dragUpDistance = downOffset.y - currentPos.y
                                            val fraction = dragUpDistance / (screenHeight * 0.55f)
                                            val isHwPlus = playerState.decoderMode == DecoderMode.HW_PLUS
                                            val targetPercent = if (isHwPlus) {
                                                (initialVolPercent + (fraction * 50).toInt()).coerceIn(101, 150)
                                            } else {
                                                (initialVolPercent + (fraction * 100).toInt()).coerceIn(0, 100)
                                            }
                                            playerManager.setVolumeWithBoost(targetPercent)
                                            hudVolumePercent = targetPercent
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
                                                doubleTapLeftAnim = true
                                                hudSeekDelta = -10L
                                                hudSeekTarget = playerState.formattedCurrentTime
                                                hudHideTrigger = System.currentTimeMillis()
                                            }
                                            downOffset.x > screenWidth * 0.65f -> {
                                                // Right double tap: seek +10s
                                                playerManager.seekBy(10000)
                                                doubleTapRightAnim = true
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
        hudVolumePercent?.let { percent ->
            VolumeBrightnessHUD(
                isVolume = true,
                volumePercent = percent,
                maxVolumePercent = playerState.decoderMode.maxVolumePercent,
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

        // Double Tap Seek Ripple Overlays (10 sec backward / forward)
        if (doubleTapLeftAnim) {
            DoubleTapSeekRipple(
                isLeft = true,
                seconds = 10,
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }
        if (doubleTapRightAnim) {
            DoubleTapSeekRipple(
                isLeft = false,
                seconds = 10,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        // Camera Shutter Flash
        if (showCameraFlash) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.85f))
            )
        }

        // HUD Toast Notification Banner
        hudMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 70.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(VTBlack.copy(alpha = 0.85f))
                    .border(1.dp, VTAccentBlue, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = msg,
                    color = VTWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
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
                    video = video,
                    playerState = playerState,
                    onBack = onBack,
                    onAudioTracksClick = { playerManager.setShowTracksDialog(true) },
                    onSubtitlesClick = { playerManager.setShowTracksDialog(true) },
                    onToggleHardwareDecoding = {
                        val nextMode = playerManager.cycleDecoderMode()
                        val msg = when (nextMode) {
                            DecoderMode.SW -> "Video Decoder: SW (Software Standard, 0%–100% Volume)"
                            DecoderMode.HW -> "Video Decoder: HW (Hardware Standard, 0%–100% Volume)"
                            DecoderMode.HW_PLUS -> "Video Decoder: HW+ (Hardware+ Accelerated, 101%–150% +100% Audio Boost up to +20 dB)"
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    onSpeedClick = { playerManager.setShowSpeedDialog(true) },
                    onScreenshotClick = { takeScreenshot() },
                    onAudioModeClick = { playerManager.toggleBackgroundPlayback() },
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
                    onSyncClick = { showSyncDialog = true },
                    onEqualizerClick = { playerManager.setShowEqualizerDialog(true) },
                    onFrameStepperClick = { showFrameStepper = true },
                    onSleepTimerClick = { showSleepTimerDialog = true },
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // Bottom Controls (Transparent B&W progress bar, Middle controls: Previous, Play/Stop, Next, Lock, Aspect Ratio, Fullscreen)
                PlayerBottomControls(
                    playerState = playerState,
                    isPlaying = playerState.isPlaying,
                    isLocked = playerState.isControlsLocked,
                    aspectRatioMode = playerState.aspectRatioMode,
                    onSeek = { targetMs -> playerManager.seekTo(targetMs) },
                    onLockClick = { playerManager.toggleControlsLock() },
                    onPrevious = { playerManager.previous() },
                    onPlayPause = { playerManager.togglePlayPause() },
                    onNext = { playerManager.next() },
                    onAspectRatioClick = {
                        panX = 0f
                        panY = 0f
                        playerManager.resetZoomAndPan()
                        val modes = AspectRatioMode.values()
                        val nextIdx = (playerState.aspectRatioMode.ordinal + 1) % modes.size
                        playerManager.setAspectRatioMode(modes[nextIdx])
                    },
                    onFullscreenClick = {
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
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }

    // ▶️ सबसे खास: Resume Dialog
    playerState.resumePrompt?.let { prompt ->
        AlertDialog(
            onDismissRequest = { playerManager.clearResumePrompt() },
            containerColor = Color(0xFF161616),
            shape = RoundedCornerShape(16.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(VTAccentBlue.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = null,
                        tint = VTAccentBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Resume Video?",
                    color = VTWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = prompt.video.title,
                        color = VTWhite,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF242424))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total: ${if (prompt.totalDurationMs > 0) formatMs(prompt.totalDurationMs) else prompt.video.formattedDuration}",
                            color = Color(0xFFCCCCCC),
                            fontSize = 12.sp
                        )
                        Text(text = " • ", color = Color.Gray, fontSize = 12.sp)
                        Text(
                            text = "आपने देखा: ${formatMs(prompt.savedPositionMs)}",
                            color = VTAccentBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Resume from ${formatMs(prompt.savedPositionMs)}?",
                        color = VTWhite,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        playerManager.seekTo(prompt.savedPositionMs)
                        playerManager.clearResumePrompt()
                        if (!playerState.isPlaying) {
                            playerManager.getExoPlayer()?.play()
                        }
                        hudMessage = "Resumed from ${formatMs(prompt.savedPositionMs)}"
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VTAccentBlue,
                        contentColor = VTWhite
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("resume_confirm_button")
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("RESUME", fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        playerManager.seekTo(0L)
                        viewModel.clearSavedPosition(prompt.video.uriString)
                        playerManager.clearResumePrompt()
                        if (!playerState.isPlaying) {
                            playerManager.getExoPlayer()?.play()
                        }
                        hudMessage = "Playing from start"
                    },
                    border = BorderStroke(1.dp, Color(0xFF555555)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VTWhite),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("resume_start_over_button")
                ) {
                    Icon(Icons.Filled.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("START OVER", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Sleep Timer Dialog
    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            containerColor = VTDarkCard,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text("Sleep Timer", color = VTWhite, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    listOf(15, 30, 45, 60, 90).forEach { mins ->
                        Text(
                            text = "$mins minutes",
                            color = VTWhite,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    hudMessage = "Sleep timer set for $mins mins"
                                    showSleepTimerDialog = false
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSleepTimerDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = VTDarkBorder)
                ) {
                    Text("Close", color = VTWhite)
                }
            }
        )
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
    video: VideoItem,
    playerState: VTPlayerState,
    onBack: () -> Unit,
    onAudioTracksClick: () -> Unit,
    onSubtitlesClick: () -> Unit,
    onToggleHardwareDecoding: () -> Unit,
    onSpeedClick: () -> Unit,
    onScreenshotClick: () -> Unit,
    onAudioModeClick: () -> Unit,
    onRotateClick: () -> Unit,
    onSyncClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    onFrameStepperClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ← Back button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(42.dp)
                .testTag("player_back_button")
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = VTWhite,
                modifier = Modifier.size(26.dp)
            )
        }

        // Video Title & Resolution / File size / Duration
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp)
        ) {
            Text(
                text = video.title,
                color = VTWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val res = if (video.resolution.isNotBlank()) video.resolution else "1080p"
            val size = if (video.formattedSize.isNotBlank()) video.formattedSize else "--"
            val dur = if (playerState.duration > 0L) playerState.formattedDuration else video.formattedDuration
            Text(
                text = "$res • $size • $dur",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Action Icons: Audio, Subtitle, HW, 1X Speed, Screenshot, Audio, Rotate, More
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            // 🎵 Audio Track
            IconButton(
                onClick = onAudioTracksClick,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    Icons.Filled.Audiotrack,
                    contentDescription = "Audio Track",
                    tint = if (playerState.selectedAudioTrackId != null) VTAccentBlue else VTWhite,
                    modifier = Modifier.size(22.dp)
                )
            }

            // 📝 Subtitle
            IconButton(
                onClick = onSubtitlesClick,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    Icons.Filled.Subtitles,
                    contentDescription = "Subtitle",
                    tint = if (playerState.selectedSubtitleTrackId != null) VTAccentBlue else VTWhite,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Decoder Mode Toggle Badge (SW / HW / HW+)
            val decoderMode = playerState.decoderMode
            val badgeBgColor = when (decoderMode) {
                DecoderMode.HW -> VTAccentGreen.copy(alpha = 0.25f)
                DecoderMode.SW -> VTAccentBlue.copy(alpha = 0.25f)
                DecoderMode.HW_PLUS -> Color(0xFFFFB300).copy(alpha = 0.25f)
            }
            val badgeBorderColor = when (decoderMode) {
                DecoderMode.HW -> VTAccentGreen
                DecoderMode.SW -> VTAccentBlue
                DecoderMode.HW_PLUS -> Color(0xFFFFB300)
            }
            val badgeTextColor = when (decoderMode) {
                DecoderMode.HW -> VTAccentGreen
                DecoderMode.SW -> VTAccentBlue
                DecoderMode.HW_PLUS -> Color(0xFFFFB300)
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeBgColor)
                    .border(1.dp, badgeBorderColor, RoundedCornerShape(6.dp))
                    .clickable(onClick = onToggleHardwareDecoding)
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = decoderMode.code,
                    color = badgeTextColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp
                )
            }

            // 1X Speed Toggle
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF222222))
                    .border(1.dp, Color(0xFF666666), RoundedCornerShape(6.dp))
                    .clickable(onClick = onSpeedClick)
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${playerState.playbackSpeed}X",
                    color = VTWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            // 📷 Screenshot
            IconButton(
                onClick = onScreenshotClick,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    Icons.Filled.PhotoCamera,
                    contentDescription = "Screenshot",
                    tint = VTWhite,
                    modifier = Modifier.size(22.dp)
                )
            }

            // 🎧 Audio (Background Audio Playback)
            IconButton(
                onClick = onAudioModeClick,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    Icons.Filled.Headphones,
                    contentDescription = "Audio Mode",
                    tint = if (playerState.isBackgroundPlaybackEnabled) VTAccentBlue else VTWhite,
                    modifier = Modifier.size(22.dp)
                )
            }

            // 🔄 Rotate
            IconButton(
                onClick = onRotateClick,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    Icons.Filled.ScreenRotation,
                    contentDescription = "Rotate",
                    tint = VTWhite,
                    modifier = Modifier.size(22.dp)
                )
            }

            // ⋮ More
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "More Options",
                        tint = VTWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier
                        .background(VTBlack)
                        .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
                ) {
                    DropdownMenuItem(
                        text = { Text("Audio & Boost (${playerState.decoderMode.code})", color = VTWhite, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = VTWhite) },
                        onClick = {
                            showMenu = false
                            onEqualizerClick()
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
                        text = { Text("Frame-by-Frame", color = VTWhite, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Filled.Tune, contentDescription = null, tint = VTWhite) },
                        onClick = {
                            showMenu = false
                            onFrameStepperClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Sleep Timer", color = VTWhite, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Filled.History, contentDescription = null, tint = VTWhite) },
                        onClick = {
                            showMenu = false
                            onSleepTimerClick()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerBottomControls(
    playerState: VTPlayerState,
    isPlaying: Boolean,
    isLocked: Boolean,
    aspectRatioMode: AspectRatioMode,
    onSeek: (Long) -> Unit,
    onLockClick: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onAspectRatioClick: () -> Unit,
    onFullscreenClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isDraggingSeek by remember { mutableStateOf(false) }
    var tempPosition by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // Row 1: Transparent only black and white progress bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Current time (pure white)
            Text(
                text = if (isDraggingSeek) formatMs(tempPosition.toLong()) else playerState.formattedCurrentTime,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.padding(end = 10.dp)
            )

            // Progress/seek bar (transparent background, only black and white)
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
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("player_seek_bar")
            )

            // Remaining time (black & white: translucent white)
            Text(
                text = playerState.formattedRemainingTime,
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 10.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Row 2: 🔒 Lock on left, THREE SYMBOLS in the middle position (Reverse, Play Stop, Forward), and Aspect Ratio / Fullscreen on right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 🔒 Lock button (Left)
            IconButton(
                onClick = onLockClick,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("player_lock_button")
            ) {
                Icon(
                    imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    contentDescription = "Lock Controls",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Middle Position: The THREE symbols: [Previous] [Play Stop] [Next]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(22.dp),
                modifier = Modifier.testTag("player_middle_controls")
            ) {
                // ⏮️ Previous symbol (Black & white only)
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .testTag("player_previous_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // ⏸️ / ▶️ Play Stop symbol (Not color! Only black and white show)
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(onClick = onPlayPause)
                        .testTag("player_play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Stop / Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // ⏭️ Next symbol (Black & white only)
                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .testTag("player_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Right side: ↔️ Aspect Ratio & ⛶ Fullscreen
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // ↔️ Aspect Ratio
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onAspectRatioClick)
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.AspectRatio,
                        contentDescription = "Aspect Ratio",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = when (aspectRatioMode) {
                            AspectRatioMode.FIT -> "Fit"
                            AspectRatioMode.STRETCH -> "Stretch"
                            AspectRatioMode.CROP_ZOOM -> "Crop"
                            AspectRatioMode.RATIO_16_9 -> "16:9"
                            AspectRatioMode.RATIO_4_3 -> "4:3"
                            AspectRatioMode.RATIO_1_1 -> "1:1"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                // ⛶ Fullscreen
                IconButton(
                    onClick = onFullscreenClick,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DoubleTapSeekRipple(
    isLeft: Boolean,
    seconds: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth(0.38f)
            .clip(
                if (isLeft) RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50)
                else RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50)
            )
            .background(Color.White.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (isLeft) Icons.Filled.FastRewind else Icons.Filled.FastForward,
                contentDescription = null,
                tint = VTWhite,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isLeft) "-$seconds sec" else "+$seconds sec",
                color = VTWhite,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp
            )
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
