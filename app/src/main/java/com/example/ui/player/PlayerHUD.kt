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
    volumePercent: Int = 100, // 0 .. 200
    maxVolumePercent: Int = 100, // 100 for SW/HW, 200 for HW+
    modifier: Modifier = Modifier
) {
    val isBoosted = isVolume && volumePercent > 100
    val icon = if (isVolume) {
        when {
            volumePercent <= 0 -> Icons.Filled.VolumeMute
            volumePercent <= 50 -> Icons.Filled.VolumeDown
            else -> Icons.Filled.VolumeUp
        }
    } else {
        if (level < 0.5f) Icons.Filled.BrightnessLow else Icons.Filled.BrightnessHigh
    }

    val title = if (isVolume) {
        if (isBoosted) "HW+ Audio Boost" else "Volume"
    } else "Brightness"

    val displayPercent = if (isVolume) volumePercent.coerceIn(0, maxVolumePercent) else (level * 100).toInt().coerceIn(0, 100)
    val accentColor = if (isBoosted) Color(0xFFFFB300) else VTWhite

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(VTBlack.copy(alpha = 0.88f))
            .border(1.5.dp, if (isBoosted) Color(0xFFFFB300) else VTWhite, RoundedCornerShape(16.dp))
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
            if (isBoosted) {
                Spacer(modifier = Modifier.height(2.dp))
                val boostAmount = volumePercent - 100
                val boostDb = ((boostAmount / 20f) * 20f).toInt().coerceIn(1, 20)
                Text(
                    text = "+$boostAmount% BOOST (+${boostDb}dB)",
                    color = Color(0xFFFFD54F),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            val maxVolF = if (isVolume) maxVolumePercent.toFloat().coerceAtLeast(100f) else 1f
            val progressFraction = if (isVolume) {
                (volumePercent / maxVolF).coerceIn(0f, 1f)
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

