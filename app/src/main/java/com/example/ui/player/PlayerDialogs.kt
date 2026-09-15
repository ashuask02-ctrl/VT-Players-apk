package com.example.ui.player

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.example.model.AspectRatioMode
import com.example.model.DecoderMode
import com.example.player.VTPlayerManager
import com.example.player.VTPlayerState
import com.example.ui.theme.VTAccentBlue
import com.example.ui.theme.VTAccentGreen
import com.example.ui.theme.VTBlack
import com.example.ui.theme.VTDarkBorder
import com.example.ui.theme.VTDarkCard
import com.example.ui.theme.VTWhite

@Composable
fun SpeedDialog(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f, 4.0f)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VTBlack,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.border(1.5.dp, VTWhite, RoundedCornerShape(18.dp)),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Speed, contentDescription = null, tint = VTWhite)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Playback Speed (0.25x – 4x)", color = VTWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                speeds.forEach { speed ->
                    val isSelected = (speed == currentSpeed)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) VTWhite else Color.Transparent)
                            .clickable {
                                onSpeedSelected(speed)
                                onDismiss()
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${speed}x" + if (speed == 1.0f) " (Normal)" else "",
                            color = if (isSelected) VTBlack else VTWhite,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            fontSize = 16.sp
                        )
                        if (isSelected) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = VTBlack)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = VTWhite, contentColor = VTBlack)
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@OptIn(UnstableApi::class)
@Composable
fun TracksDialog(
    playerManager: VTPlayerManager,
    playerState: VTPlayerState,
    onExternalSubtitleSelected: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    val subtitleFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onExternalSubtitleSelected(uri)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VTBlack,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.border(1.5.dp, VTWhite, RoundedCornerShape(18.dp)),
        title = {
            Text("Audio & Subtitles", color = VTWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Audio Tracks Section
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Audiotrack, contentDescription = null, tint = VTWhite, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Audio Tracks", color = VTWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))

                // Quick Language Switcher for Audio (English & Hindi)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { playerManager.selectAudioByLanguage("en") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VTDarkCard,
                            contentColor = VTWhite
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, VTDarkBorder, RoundedCornerShape(8.dp))
                    ) {
                        Text("English", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { playerManager.selectAudioByLanguage("hi") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VTDarkCard,
                            contentColor = VTWhite
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, VTDarkBorder, RoundedCornerShape(8.dp))
                    ) {
                        Text("हिन्दी (Hindi)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (playerState.audioTracks.isEmpty()) {
                    Text("Default Audio Track (Embedded Stereo)", color = Color(0xFFAAAAAA), fontSize = 13.sp)
                } else {
                    playerState.audioTracks.forEach { track ->
                        val isSelected = track.id == playerState.selectedAudioTrackId
                        val isHindi = track.language.contains("hi", ignoreCase = true) || track.label.contains("hindi", ignoreCase = true) || track.label.contains("हिन्दी")
                        val isEnglish = track.language.contains("en", ignoreCase = true) || track.label.contains("english", ignoreCase = true)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) VTWhite else VTDarkCard)
                                .clickable { playerManager.selectAudioTrack(track) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.label,
                                    color = if (isSelected) VTBlack else VTWhite,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                                val langTag = when {
                                    isHindi -> "हिन्दी • Hindi"
                                    isEnglish -> "English"
                                    track.language.isNotBlank() && track.language != "Unknown" -> track.language
                                    else -> "Embedded Audio"
                                }
                                Text(
                                    text = langTag,
                                    color = if (isSelected) Color(0xFF444444) else Color(0xFFAAAAAA),
                                    fontSize = 11.sp
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = VTBlack, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = VTDarkBorder)
                Spacer(modifier = Modifier.height(14.dp))

                // Subtitle Tracks Section
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Subtitles, contentDescription = null, tint = VTWhite, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Subtitles", color = VTWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))

                // Quick Language Switcher for Subtitles (English, Hindi & Off)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { playerManager.selectSubtitleByLanguage("en") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VTDarkCard,
                            contentColor = VTWhite
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, VTDarkBorder, RoundedCornerShape(8.dp))
                    ) {
                        Text("English", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { playerManager.selectSubtitleByLanguage("hi") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VTDarkCard,
                            contentColor = VTWhite
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, VTDarkBorder, RoundedCornerShape(8.dp))
                    ) {
                        Text("हिन्दी", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { playerManager.selectSubtitleTrack(null) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (playerState.selectedSubtitleTrackId == null) VTWhite else VTDarkCard,
                            contentColor = if (playerState.selectedSubtitleTrackId == null) VTBlack else VTWhite
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, VTDarkBorder, RoundedCornerShape(8.dp))
                    ) {
                        Text("Off", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Off Option
                val isSubDisabled = playerState.selectedSubtitleTrackId == null
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSubDisabled) VTWhite else VTDarkCard)
                        .clickable { playerManager.selectSubtitleTrack(null) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Subtitles Off (कोई सबटाइटल नहीं)",
                        color = if (isSubDisabled) VTBlack else Color(0xFFCCCCCC),
                        fontWeight = if (isSubDisabled) FontWeight.ExtraBold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                    if (isSubDisabled) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = VTBlack, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                playerState.subtitleTracks.forEach { track ->
                    val isSelected = track.id == playerState.selectedSubtitleTrackId
                    val isHindi = track.language.contains("hi", ignoreCase = true) || track.label.contains("hindi", ignoreCase = true) || track.label.contains("हिन्दी")
                    val isEnglish = track.language.contains("en", ignoreCase = true) || track.label.contains("english", ignoreCase = true)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) VTWhite else VTDarkCard)
                            .clickable { playerManager.selectSubtitleTrack(track) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.label,
                                color = if (isSelected) VTBlack else VTWhite,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                            val langTag = when {
                                isHindi -> "हिन्दी • Hindi"
                                isEnglish -> "English"
                                track.language.isNotBlank() && track.language != "Unknown" -> track.language
                                else -> "Embedded Subtitle"
                            }
                            Text(
                                text = langTag,
                                color = if (isSelected) Color(0xFF444444) else Color(0xFFAAAAAA),
                                fontSize = 11.sp
                            )
                        }
                        if (isSelected) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = VTBlack, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { subtitleFilePicker.launch("*/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = VTDarkCard, contentColor = VTWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, VTDarkBorder, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Load External Subtitle (.srt / .vtt)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = VTWhite, contentColor = VTBlack)
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@OptIn(UnstableApi::class)
@Composable
fun SubtitleAndAudioSyncDialog(
    playerManager: VTPlayerManager,
    playerState: VTPlayerState,
    onDismiss: () -> Unit
) {
    var subDelay by remember { mutableStateOf(playerState.subtitleDelayMs) }
    var audioDelay by remember { mutableStateOf(playerState.audioDelayMs) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VTBlack,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.border(1.5.dp, VTWhite, RoundedCornerShape(18.dp)),
        title = {
            Text("Sync Delay & Calibrate", color = VTWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Subtitle Delay
                Text("Subtitle Delay: ${subDelay}ms", color = VTWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            subDelay -= 50
                            playerManager.setSubtitleDelayMs(subDelay)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VTDarkCard, contentColor = VTWhite)
                    ) {
                        Text("-50ms", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            subDelay = 0L
                            playerManager.setSubtitleDelayMs(0L)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VTDarkCard, contentColor = VTWhite)
                    ) {
                        Text("Reset", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            subDelay += 50
                            playerManager.setSubtitleDelayMs(subDelay)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VTDarkCard, contentColor = VTWhite)
                    ) {
                        Text("+50ms", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = VTDarkBorder)
                Spacer(modifier = Modifier.height(16.dp))

                // Audio Delay
                Text("Audio Delay: ${audioDelay}ms", color = VTWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            audioDelay -= 50
                            playerManager.setAudioDelayMs(audioDelay)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VTDarkCard, contentColor = VTWhite)
                    ) {
                        Text("-50ms", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            audioDelay = 0L
                            playerManager.setAudioDelayMs(0L)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VTDarkCard, contentColor = VTWhite)
                    ) {
                        Text("Reset", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            audioDelay += 50
                            playerManager.setAudioDelayMs(audioDelay)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VTDarkCard, contentColor = VTWhite)
                    ) {
                        Text("+50ms", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = VTWhite, contentColor = VTBlack)
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@OptIn(UnstableApi::class)
@Composable
fun AudioBoostDialog(
    playerManager: VTPlayerManager,
    playerState: VTPlayerState,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VTBlack,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.border(1.5.dp, VTWhite, RoundedCornerShape(18.dp)),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = VTWhite)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Audio Volume (${playerState.decoderMode.code})",
                    color = VTWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Video Decoder: ${playerState.decoderMode.label}",
                    color = when (playerState.decoderMode) {
                        DecoderMode.SW -> VTAccentBlue
                        DecoderMode.HW -> VTAccentGreen
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Volume: ${playerState.volumePercent}%", color = VTWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Slider(
                    value = playerState.volumePercent.toFloat().coerceIn(0f, 100f),
                    onValueChange = { playerManager.setVolumeWithBoost(it.toInt()) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = VTWhite,
                        activeTrackColor = VTWhite,
                        inactiveTrackColor = Color(0xFF333333)
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                val presets = listOf(25, 50, 75, 100)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { preset ->
                        val isSelected = playerState.volumePercent == preset
                        FilterChip(
                            selected = isSelected,
                            onClick = { playerManager.setVolumeWithBoost(preset) },
                            label = {
                                Text(
                                    text = "$preset%",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VTWhite,
                                selectedLabelColor = VTBlack,
                                containerColor = Color(0xFF1E1E1E),
                                labelColor = VTWhite
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Status: Standard Device Volume (0%–100%)",
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "SW and HW volume fixed to 0% – 100% (Standard device music volume).",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Decoder:", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    DecoderMode.entries.forEach { mode ->
                        val isCurrent = playerState.decoderMode == mode
                        FilterChip(
                            selected = isCurrent,
                            onClick = { playerManager.setDecoderMode(mode) },
                            label = { Text(mode.code, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VTWhite,
                                selectedLabelColor = VTBlack,
                                containerColor = Color(0xFF1E1E1E),
                                labelColor = VTWhite
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = VTWhite, contentColor = VTBlack)
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@OptIn(UnstableApi::class)
@Composable
fun FrameByFrameDialog(
    playerManager: VTPlayerManager,
    playerState: VTPlayerState,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VTBlack,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.border(1.5.dp, VTWhite, RoundedCornerShape(18.dp)),
        title = {
            Text("Frame-by-Frame Stepper", color = VTWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Current: ${playerState.formattedCurrentTime}",
                    color = VTWhite,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { playerManager.stepFrame(forward = false) },
                        colors = ButtonDefaults.buttonColors(containerColor = VTDarkCard, contentColor = VTWhite),
                        modifier = Modifier.border(1.dp, VTDarkBorder, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Filled.FastRewind, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("-1 Frame", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { playerManager.stepFrame(forward = true) },
                        colors = ButtonDefaults.buttonColors(containerColor = VTDarkCard, contentColor = VTWhite),
                        modifier = Modifier.border(1.dp, VTDarkBorder, RoundedCornerShape(8.dp))
                    ) {
                        Text("+1 Frame", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Filled.FastForward, contentDescription = null)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = VTWhite, contentColor = VTBlack)
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}
