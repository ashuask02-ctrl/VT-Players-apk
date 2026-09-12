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
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
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
    level: Float, // 0.0 .. 1.0
    modifier: Modifier = Modifier
) {
    val icon = if (isVolume) {
        when {
            level <= 0.01f -> Icons.Filled.VolumeMute
            level < 0.5f -> Icons.Filled.VolumeDown
            else -> Icons.Filled.VolumeUp
        }
    } else {
        if (level < 0.5f) Icons.Filled.BrightnessLow else Icons.Filled.BrightnessHigh
    }

    val title = if (isVolume) "Volume" else "Brightness"
    val percent = (level * 100).toInt().coerceIn(0, 100)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(VTBlack.copy(alpha = 0.85f))
            .border(1.5.dp, VTWhite, RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = VTWhite,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "$percent%",
                color = VTWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { level },
                modifier = Modifier
                    .width(100.dp)
                    .height(6.dp)
                    .clip(CircleShape),
                color = VTWhite,
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
