package com.example.ui.player

import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.model.VideoItem
import com.example.player.VTPlayerManager
import com.example.ui.theme.VTAccentBlue
import com.example.ui.theme.VTBlack
import com.example.ui.theme.VTWhite
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
fun MiniPlayer(
    video: VideoItem,
    playerManager: VTPlayerManager,
    onExpand: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playerState by playerManager.playerState.collectAsState()
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val miniWidthDp = 220.dp
    val miniHeightDp = 124.dp

    val miniWidthPx = with(density) { miniWidthDp.toPx() }
    val miniHeightPx = with(density) { miniHeightDp.toPx() }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val parentWidthPx = constraints.maxWidth.toFloat()
        val parentHeightPx = constraints.maxHeight.toFloat()

        // Padding from edges
        val marginPx = with(density) { 16.dp.toPx() }
        val bottomNavOffsetPx = with(density) { 86.dp.toPx() }

        val defaultX = (parentWidthPx - miniWidthPx - marginPx).coerceAtLeast(marginPx)
        val defaultY = (parentHeightPx - miniHeightPx - bottomNavOffsetPx).coerceAtLeast(marginPx)

        var offsetX by remember { mutableFloatStateOf(defaultX) }
        var offsetY by remember { mutableFloatStateOf(defaultY) }

        // Swipe to dismiss horizontal offset & opacity
        val swipeOffsetX = remember { Animatable(0f) }
        var isDismissing by remember { mutableStateOf(false) }
        var showControls by remember { mutableStateOf(true) }

        val dismissThresholdPx = miniWidthPx * 0.45f

        val dragModifier = Modifier.pointerInput(Unit) {
            detectDragGestures(
                onDrag = { change, dragAmount ->
                    change.consume()
                    if (!isDismissing) {
                        // If horizontal swipe is significant compared to vertical, treat as swipe-to-dismiss gesture
                        val newSwipe = swipeOffsetX.value + dragAmount.x
                        if (abs(newSwipe) > 15f) {
                            coroutineScope.launch {
                                swipeOffsetX.snapTo(newSwipe)
                            }
                        }

                        // Free floating Drag & Move in all directions
                        val newX = (offsetX + dragAmount.x).coerceIn(marginPx, (parentWidthPx - miniWidthPx - marginPx).coerceAtLeast(marginPx))
                        val newY = (offsetY + dragAmount.y).coerceIn(marginPx, (parentHeightPx - miniHeightPx - marginPx).coerceAtLeast(marginPx))
                        offsetX = newX
                        offsetY = newY
                    }
                },
                onDragEnd = {
                    if (abs(swipeOffsetX.value) > dismissThresholdPx) {
                        isDismissing = true
                        coroutineScope.launch {
                            val target = if (swipeOffsetX.value > 0) parentWidthPx else -parentWidthPx
                            swipeOffsetX.animateTo(target, animationSpec = tween(220))
                            try {
                                playerManager.getExoPlayer()?.pause()
                            } catch (_: Exception) {}
                            onClose()
                        }
                    } else {
                        coroutineScope.launch {
                            swipeOffsetX.animateTo(0f, animationSpec = tween(180))
                        }
                    }
                },
                onDragCancel = {
                    coroutineScope.launch {
                        swipeOffsetX.animateTo(0f, animationSpec = tween(180))
                    }
                }
            )
        }

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (offsetX + swipeOffsetX.value).roundToInt(),
                        y = offsetY.roundToInt()
                    )
                }
                .size(miniWidthDp, miniHeightDp)
                .graphicsLayer {
                    val progress = (abs(swipeOffsetX.value) / miniWidthPx).coerceIn(0f, 1f)
                    alpha = 1f - (progress * 0.65f)
                    scaleX = 1f - (progress * 0.12f)
                    scaleY = 1f - (progress * 0.12f)
                }
                .shadow(12.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(VTBlack)
                .border(1.5.dp, VTAccentBlue, RoundedCornerShape(14.dp))
                .testTag("mini_player_surface")
        ) {
            // Live Video Surface via AndroidView
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        useController = false
                        this.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        subtitleView?.setUserDefaultStyle()
                        subtitleView?.setUserDefaultTextSize()
                        playerManager.attachPlayerView(this)
                    }
                },
                update = { playerView ->
                    playerManager.attachPlayerView(playerView)
                },
                onRelease = { playerView ->
                    playerManager.detachPlayerView(playerView)
                },
                modifier = Modifier.fillMaxSize()
            )

            // Transparent Gradient scrim with drag & tap-to-expand
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.70f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .then(dragModifier)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onExpand()
                    }
            )

            // Top bar controls: Expand icon and Close (×) icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onExpand,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .testTag("mini_player_expand_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Fullscreen,
                        contentDescription = "Expand to Full Screen",
                        tint = VTWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Close (×) button: pauses playback immediately and closes
                IconButton(
                    onClick = {
                        try {
                            playerManager.getExoPlayer()?.pause()
                        } catch (_: Exception) {}
                        onClose()
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.75f))
                        .border(1.dp, VTWhite.copy(alpha = 0.5f), CircleShape)
                        .testTag("mini_player_close_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close Mini Player",
                        tint = VTWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Center Play/Pause Control Button
            IconButton(
                onClick = { playerManager.togglePlayPause() },
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(1.dp, VTWhite.copy(alpha = 0.4f), CircleShape)
                    .testTag("mini_player_play_pause_btn")
            ) {
                Icon(
                    imageVector = if (playerState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                    tint = VTWhite,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Bottom title & Progress Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
            ) {
                Text(
                    text = if (playerState.isPausedByCall) "📞 In Call • ${video.title}" else video.title,
                    color = VTWhite,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )

                val progress = if (playerState.duration > 0L) {
                    (playerState.currentPosition.toFloat() / playerState.duration.toFloat()).coerceIn(0f, 1f)
                } else 0f

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = VTAccentBlue,
                    trackColor = Color.White.copy(alpha = 0.25f)
                )
            }
        }
    }
}
