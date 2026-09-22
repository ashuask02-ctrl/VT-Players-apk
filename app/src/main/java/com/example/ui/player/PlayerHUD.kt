package com.example.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VTBlack
import com.example.ui.theme.VTWhite

@Composable
fun VolumeBrightnessHUD(
    isVolume: Boolean,
    level: Float = 1f, // 0.0 .. 1.0
    volumePercent: Int = 100, // 0 .. 100 for SW/HW, 0 .. 150 for HW+
    maxVolumePercent: Int = 100,
    modifier: Modifier = Modifier
) {
    val isBoost = isVolume && volumePercent > 100
    val boostDb = if (isBoost) (((volumePercent - 100) / 50f) * 20f).toInt().coerceIn(1, 20) else 0
    val boostColor = Color(0xFFFFB300)
    val accentColor = if (isBoost) boostColor else VTWhite

    val icon = if (isVolume) {
        when {
            volumePercent <= 0 -> Icons.Filled.VolumeMute
            volumePercent <= 50 -> Icons.Filled.VolumeDown
            else -> Icons.Filled.VolumeUp
        }
    } else {
        if (level < 0.5f) Icons.Filled.BrightnessLow else Icons.Filled.BrightnessHigh
    }

    val title = when {
        isBoost -> "Audio Boost (+20dB)"
        isVolume -> "Volume"
        else -> "Brightness"
    }
    val displayPercent = if (isVolume) volumePercent.coerceIn(0, 150) else (level * 100).toInt().coerceIn(0, 100)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(VTBlack.copy(alpha = 0.88f))
            .border(1.5.dp, accentColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 22.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "$displayPercent%",
                color = accentColor,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold
            )
            if (isBoost) {
                Spacer(modifier = Modifier.height(2.dp))
                val boostPercent = ((volumePercent - 100) / 50f * 100).toInt().coerceIn(2, 100)
                Text(
                    text = "+$boostPercent% BOOST (+${boostDb}dB)",
                    color = boostColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            val progressFraction = if (isVolume) {
                if (isBoost) {
                    (volumePercent / 150f).coerceIn(0f, 1f)
                } else {
                    (volumePercent / 100f).coerceIn(0f, 1f)
                }
            } else {
                level.coerceIn(0f, 1f)
            }
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .width(110.dp)
                    .height(6.dp)
                    .clip(CircleShape),
                color = accentColor,
                trackColor = Color(0xFF333333)
            )
        }
    }
}

@Composable
fun SeekHUD(
    deltaSec: Long,
    targetPositionFormatted: String,
    modifier: Modifier = Modifier
) {
    val isForward = deltaSec >= 0
    val icon = if (isForward) Icons.Filled.FastForward else Icons.Filled.FastRewind
    val sign = if (isForward) "+" else ""

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(VTBlack.copy(alpha = 0.85f))
            .border(1.5.dp, VTWhite, RoundedCornerShape(16.dp))
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = "Seek HUD",
                tint = VTWhite,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "$sign${deltaSec}s",
                    color = VTWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Target: $targetPositionFormatted",
                    color = Color(0xFFCCCCCC),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ZoomScrollHUD(
    zoomScale: Float,
    isPanning: Boolean,
    modifier: Modifier = Modifier
) {
    val percent = (zoomScale * 100).toInt()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(VTBlack.copy(alpha = 0.88f))
            .border(1.5.dp, VTWhite, RoundedCornerShape(16.dp))
            .padding(horizontal = 22.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (zoomScale <= 1.05f && zoomScale >= 0.95f) Icons.Filled.FitScreen else Icons.Filled.ZoomIn,
                contentDescription = "Screen Size",
                tint = VTWhite,
                modifier = Modifier.size(34.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Screen Size: $percent%",
                color = VTWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (zoomScale > 1.05f) "Two fingers: Pinch to zoom • Drag to scroll" else "Two fingers: Pinch to adjust screen size",
                color = Color(0xFFCCCCCC),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun CallStateHUD(
    isPausedByCall: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isPausedByCall,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(VTBlack.copy(alpha = 0.90f))
                .border(1.5.dp, VTWhite, RoundedCornerShape(20.dp))
                .padding(horizontal = 20.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.PhoneInTalk,
                    contentDescription = "Phone Call",
                    tint = VTWhite,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Call in progress",
                        color = VTWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Playback will resume when call ends",
                        color = Color(0xFFCCCCCC),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

