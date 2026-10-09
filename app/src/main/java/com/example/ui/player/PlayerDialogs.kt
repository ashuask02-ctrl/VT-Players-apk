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
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.model.AudioDecoderMode
import com.example.model.DecoderMode
import com.example.model.EqualizerPreset
import com.example.model.HwMovieQuality
import com.example.model.MovieColorFilter
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
    preservePitch: Boolean = true,
    onSpeedSelected: (Float) -> Unit,
    onPreservePitchChanged: (Boolean) -> Unit = {},
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
                // Audio Pitch Control
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(VTDarkCard)
                        .border(1.dp, VTDarkBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Audio Pitch Lock", color = VTWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = if (preservePitch) "Preserving natural vocal pitch" else "Pitch shifts with speed",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = preservePitch,
                        onCheckedChange = onPreservePitchChanged,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VTBlack,
                            checkedTrackColor = VTWhite,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = VTDarkBorder
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

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

@Composable
fun DecoderSelectionDialog(
    currentMode: DecoderMode,
    onModeSelected: (DecoderMode) -> Unit,
    onDismiss: () -> Unit
) {
    val decoders = listOf(
        Triple(
            DecoderMode.SW,
            "SW Decoder — Software decoding",
            "CPU-based software decoding pipeline. Maximum format and codec compatibility."
        ),
        Triple(
            DecoderMode.HW,
            "HW Decoder — Hardware decoding",
            "Hardware-accelerated MediaCodec SoC decoding. Power and battery efficient."
        ),
        Triple(
            DecoderMode.HW_PLUS,
            "HW+ Decoder — Hardware decoder + MediaCodec pipeline",
            "Hardware decoder + asynchronous MediaCodec pipeline and direct surface buffer routing."
        )
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VTBlack,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.border(1.5.dp, VTWhite, RoundedCornerShape(18.dp)),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Memory, contentDescription = null, tint = VTWhite)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Video Decoder Selection", color = VTWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "SW, HW, and HW+ are video decoding modes. The audio pipeline and volume boost operate independently.",
                    color = Color(0xFFAAAAAA),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                decoders.forEach { (mode, title, desc) ->
                    val isSelected = (mode == currentMode)
                    val accentColor = when (mode) {
                        DecoderMode.SW -> VTAccentBlue
                        DecoderMode.HW -> VTAccentGreen
                        DecoderMode.HW_PLUS -> Color(0xFFFFB300)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) VTWhite else VTDarkCard)
                            .border(1.dp, if (isSelected) accentColor else VTDarkBorder, RoundedCornerShape(10.dp))
                            .clickable {
                                onModeSelected(mode)
                                onDismiss()
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSelected) VTBlack else accentColor)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = mode.code,
                                        color = if (isSelected) VTWhite else VTBlack,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = title,
                                    color = if (isSelected) VTBlack else VTWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = desc,
                                color = if (isSelected) Color(0xFF333333) else Color(0xFFAAAAAA),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                        if (isSelected) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = VTBlack,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
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

@Composable
fun MovieQualityDialog(
    playerManager: VTPlayerManager,
    playerState: VTPlayerState,
    onQualitySelected: (HwMovieQuality) -> Unit,
    onColorFilterSelected: (MovieColorFilter) -> Unit,
    onDecoderModeSelected: (DecoderMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VTBlack,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.border(1.5.dp, VTWhite, RoundedCornerShape(18.dp)),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.HighQuality, contentDescription = null, tint = VTWhite)
                Spacer(modifier = Modifier.width(8.dp))
                Text("HW Movie Quality & Video Engine", color = VTWhite, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Section 1: Active Decoding Engine (SW / HW / HW+)
                Text("Decoding Engine", color = VTWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val decoders = listOf(DecoderMode.SW, DecoderMode.HW, DecoderMode.HW_PLUS)
                    decoders.forEach { mode ->
                        val isSelected = (mode == playerState.decoderMode)
                        val accentColor = when (mode) {
                            DecoderMode.SW -> VTAccentBlue
                            DecoderMode.HW -> VTAccentGreen
                            DecoderMode.HW_PLUS -> Color(0xFFFFB300)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) VTWhite else VTDarkCard)
                                .border(1.5.dp, if (isSelected) accentColor else VTDarkBorder, RoundedCornerShape(8.dp))
                                .clickable { onDecoderModeSelected(mode) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = mode.code,
                                    color = if (isSelected) VTBlack else accentColor,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = when (mode) {
                                        DecoderMode.SW -> "Software"
                                        DecoderMode.HW -> "Hardware"
                                        DecoderMode.HW_PLUS -> "HW+ Boost"
                                    },
                                    color = if (isSelected) Color(0xFF333333) else Color.Gray,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFF2A2A2A))
                Spacer(modifier = Modifier.height(14.dp))

                // Section 2: HW Movie Quality Presets (Resolution & Bitrate)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Tune, contentDescription = null, tint = VTWhite, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("HW Movie Quality (Resolution & Bitrate)", color = VTWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))

                HwMovieQuality.entries.forEach { quality ->
                    val isSelected = (quality == playerState.hwMovieQuality)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) VTWhite else VTDarkCard)
                            .border(1.dp, if (isSelected) VTWhite else VTDarkBorder, RoundedCornerShape(10.dp))
                            .clickable {
                                onQualitySelected(quality)
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSelected) VTBlack else Color(0xFF333333))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = quality.badgeLabel,
                                        color = VTWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = quality.title,
                                    color = if (isSelected) VTBlack else VTWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = quality.description,
                                color = if (isSelected) Color(0xFF333333) else Color(0xFFAAAAAA),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                        if (isSelected) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = VTBlack,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFF2A2A2A))
                Spacer(modifier = Modifier.height(14.dp))

                // Section 3: Movie Picture & Color Enhancements
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Palette, contentDescription = null, tint = VTWhite, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Movie Picture Profile", color = VTWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))

                MovieColorFilter.entries.forEach { filter ->
                    val isSelected = (filter == playerState.movieColorFilter)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) VTWhite else VTDarkCard)
                            .border(1.dp, if (isSelected) VTWhite else VTDarkBorder, RoundedCornerShape(10.dp))
                            .clickable {
                                onColorFilterSelected(filter)
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = filter.title,
                                color = if (isSelected) VTBlack else VTWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = filter.description,
                                color = if (isSelected) Color(0xFF333333) else Color(0xFFAAAAAA),
                                fontSize = 11.sp
                            )
                        }
                        if (isSelected) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = VTBlack,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Section 4: Individual Video Tracks / Stream Resolutions (if stream has multiple qualities)
                if (playerState.videoTracks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFF2A2A2A))
                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Embedded Video Tracks", color = VTWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    val isAuto = (playerState.selectedVideoTrackId == null)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isAuto) VTWhite else VTDarkCard)
                            .border(1.dp, if (isAuto) VTWhite else VTDarkBorder, RoundedCornerShape(10.dp))
                            .clickable {
                                playerManager.selectVideoTrack(null)
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Auto (Select by HW Quality setting)",
                            color = if (isAuto) VTBlack else VTWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        if (isAuto) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = VTBlack, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    playerState.videoTracks.forEach { track ->
                        val isTrackSelected = (track.id == playerState.selectedVideoTrackId)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isTrackSelected) VTWhite else VTDarkCard)
                                .border(1.dp, if (isTrackSelected) VTWhite else VTDarkBorder, RoundedCornerShape(10.dp))
                                .clickable {
                                    playerManager.selectVideoTrack(track)
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = track.label,
                                color = if (isTrackSelected) VTBlack else VTWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            if (isTrackSelected) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = VTBlack, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
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
fun AudioSettingsDialog(
    playerManager: VTPlayerManager,
    playerState: VTPlayerState,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Track & Codec", "Volume & Boost", "Delay & Sync", "Equalizer")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VTBlack,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .border(1.5.dp, VTWhite, RoundedCornerShape(18.dp))
            .fillMaxWidth(),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = VTWhite)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Decoder & Audio Settings", color = VTWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Tab Navigation
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = VTDarkCard,
                    contentColor = VTWhite,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = VTWhite
                        )
                    }
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (selectedTab) {
                    // TAB 0: AUDIO TRACK & AUDIO DECODER
                    0 -> {
                        // Audio Tracks Section
                        Text("Audio Track Selection", color = VTWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        // Quick language switcher
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { playerManager.selectAudioByLanguage("en") },
                                colors = ButtonDefaults.buttonColors(containerColor = VTDarkCard, contentColor = VTWhite),
                                modifier = Modifier.weight(1f).border(1.dp, VTDarkBorder, RoundedCornerShape(8.dp))
                            ) {
                                Text("English", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { playerManager.selectAudioByLanguage("hi") },
                                colors = ButtonDefaults.buttonColors(containerColor = VTDarkCard, contentColor = VTWhite),
                                modifier = Modifier.weight(1f).border(1.dp, VTDarkBorder, RoundedCornerShape(8.dp))
                            ) {
                                Text("हिन्दी (Hindi)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (playerState.audioTracks.isEmpty()) {
                            Text("Default Audio Track (Embedded Stereo)", color = Color(0xFFAAAAAA), fontSize = 12.5.sp)
                        } else {
                            playerState.audioTracks.forEach { track ->
                                val isSelected = track.id == playerState.selectedAudioTrackId
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
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.5.sp
                                        )
                                        Text(
                                            text = if (track.language.isNotBlank()) track.language else "Embedded Audio",
                                            color = if (isSelected) Color(0xFF444444) else Color(0xFFAAAAAA),
                                            fontSize = 11.sp
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Filled.Check, contentDescription = null, tint = VTBlack, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = VTDarkBorder)

                        // Audio Decoder Selection
                        Text("Audio Decoder Mode", color = VTWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = "Platform hardware decoder vs Software FFmpeg-compatible mode",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val audioDecoders = listOf(
                            AudioDecoderMode.AUTO to ("System MediaCodec (Auto)" to "Hardware-accelerated system audio decoding pipeline."),
                            AudioDecoderMode.SW to ("Software (FFmpeg Mode)" to "Software audio decoder for AC3, E-AC3, DTS, TrueHD, Vorbis, and multi-channel streams.")
                        )

                        audioDecoders.forEach { (mode, info) ->
                            val (title, desc) = info
                            val isSelected = playerState.audioDecoderMode == mode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) VTWhite else VTDarkCard)
                                    .border(1.dp, if (isSelected) VTWhite else VTDarkBorder, RoundedCornerShape(8.dp))
                                    .clickable { playerManager.setAudioDecoderMode(mode) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        color = if (isSelected) VTBlack else VTWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = desc,
                                        color = if (isSelected) Color(0xFF333333) else Color(0xFFAAAAAA),
                                        fontSize = 11.sp
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = VTBlack, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = VTDarkBorder)

                        // Audio Passthrough Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(VTDarkCard)
                                .border(1.dp, VTDarkBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Audio Passthrough", color = VTWhite, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                Text(
                                    text = "Send direct AC3/E-AC3/DTS bitstream to HDMI/Optical receiver",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = playerState.audioPassthroughEnabled,
                                onCheckedChange = { playerManager.setAudioPassthrough(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = VTBlack,
                                    checkedTrackColor = VTWhite,
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = VTDarkBorder
                                )
                            )
                        }
                    }

                    // TAB 1: VOLUME & VOLUME BOOST
                    1 -> {
                        val isHwPlus = playerState.decoderMode == DecoderMode.HW_PLUS
                        val maxVolume = if (isHwPlus) 150f else 100f
                        val currentVol = playerState.volumePercent.coerceAtMost(maxVolume.toInt())
                        val boostDb = if (isHwPlus && currentVol > 100) {
                            (((currentVol - 100) / 50f) * 20f).toInt().coerceIn(1, 20)
                        } else 0

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Volume: $currentVol%",
                                color = if (currentVol > 100) Color(0xFFFFB300) else VTWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            if (isHwPlus && currentVol > 100) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFFFB300))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "+$boostDb dB BOOST ACTIVE",
                                        color = VTBlack,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(VTDarkCard)
                                        .border(1.dp, VTDarkBorder, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = if (isHwPlus) "HW+ (0–150%)" else "${playerState.decoderMode.code} (0–100%)",
                                        color = Color.LightGray,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Slider(
                            value = currentVol.toFloat(),
                            onValueChange = { playerManager.setVolumeWithBoost(it.toInt()) },
                            valueRange = 0f..maxVolume,
                            colors = SliderDefaults.colors(
                                thumbColor = if (currentVol > 100) Color(0xFFFFB300) else VTWhite,
                                activeTrackColor = if (currentVol > 100) Color(0xFFFFB300) else VTWhite,
                                inactiveTrackColor = Color(0xFF333333)
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Preset chips
                        val volPresets = if (isHwPlus) listOf(50, 100, 125, 150) else listOf(25, 50, 75, 100)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            volPresets.forEach { preset ->
                                val isSelected = currentVol == preset
                                val labelText = when (preset) {
                                    100 -> "100%"
                                    125 -> "125% (+10dB)"
                                    150 -> "150% (+20dB)"
                                    else -> "$preset%"
                                }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { playerManager.setVolumeWithBoost(preset) },
                                    label = {
                                        Text(
                                            text = labelText,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (preset > 100) Color(0xFFFFB300) else VTWhite,
                                        selectedLabelColor = VTBlack,
                                        containerColor = VTDarkCard,
                                        labelColor = VTWhite
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick Boost Button / Mode Banner
                        if (isHwPlus) {
                            Button(
                                onClick = { playerManager.toggleAudioBoost() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (playerState.isAudioBoostActive) Color(0xFFFFB300) else VTDarkCard,
                                    contentColor = if (playerState.isAudioBoostActive) VTBlack else VTWhite
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, if (playerState.isAudioBoostActive) Color(0xFFFFB300) else VTDarkBorder, RoundedCornerShape(8.dp))
                            ) {
                                Icon(Icons.Filled.VolumeUp, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (playerState.isAudioBoostActive) "Disable Audio Boost (Return to 100%)" else "Enable Volume Boost (150%, +20 dB)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(VTDarkCard)
                                    .border(1.dp, VTDarkBorder, RoundedCornerShape(8.dp))
                                    .clickable { playerManager.setDecoderMode(DecoderMode.HW_PLUS) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Volume Boost (101% – 150%) is enabled in HW+ mode",
                                        color = VTWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "Tap here to switch to HW+ and unlock +20 dB amplification",
                                        color = Color(0xFFFFB300),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (isHwPlus) {
                                "• 0% – 100%: Standard system media volume.\n• 101% – 150%: Dedicated Loudness Enhancer delivering up to +20 dB amplification."
                            } else {
                                "• 0% – 100%: Audio limit strictly maintained at 100% for ${playerState.decoderMode.code} mode.\n• Switch to HW+ to access 101% – 150% (+20 dB) Volume Boost."
                            },
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }

                    // TAB 2: DELAY, PITCH & BALANCE
                    2 -> {
                        // Audio Delay (Sync)
                        Text("Audio Delay (Lip-sync)", color = VTWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Synchronize audio and video timeline", color = Color.Gray, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Current Delay: ${playerState.audioDelayMs} ms",
                            color = VTWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { playerManager.setAudioDelayMs(playerState.audioDelayMs - 50) },
                                colors = ButtonDefaults.buttonColors(containerColor = VTDarkCard, contentColor = VTWhite),
                                modifier = Modifier.weight(1f).border(1.dp, VTDarkBorder, RoundedCornerShape(8.dp))
                            ) {
                                Text("-50ms", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { playerManager.setAudioDelayMs(0L) },
                                colors = ButtonDefaults.buttonColors(containerColor = VTDarkCard, contentColor = VTWhite),
                                modifier = Modifier.weight(1f).border(1.dp, VTDarkBorder, RoundedCornerShape(8.dp))
                            ) {
                                Text("Reset", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { playerManager.setAudioDelayMs(playerState.audioDelayMs + 50) },
                                colors = ButtonDefaults.buttonColors(containerColor = VTDarkCard, contentColor = VTWhite),
                                modifier = Modifier.weight(1f).border(1.dp, VTDarkBorder, RoundedCornerShape(8.dp))
                            ) {
                                Text("+50ms", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = VTDarkBorder)

                        // Left / Right Balance
                        Text("Left / Right Stereo Balance", color = VTWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        val balancePct = (playerState.leftRightBalance * 100).toInt()
                        val balanceText = when {
                            balancePct == 0 -> "Center (50L / 50R)"
                            balancePct < 0 -> "Left ${-balancePct}%"
                            else -> "Right ${balancePct}%"
                        }
                        Text(text = "Balance: $balanceText", color = Color.LightGray, fontSize = 12.sp)

                        Slider(
                            value = playerState.leftRightBalance,
                            onValueChange = { playerManager.setLeftRightBalance(it) },
                            valueRange = -1.0f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = VTWhite,
                                activeTrackColor = VTWhite,
                                inactiveTrackColor = Color(0xFF333333)
                            )
                        )

                        Button(
                            onClick = { playerManager.setLeftRightBalance(0.0f) },
                            colors = ButtonDefaults.buttonColors(containerColor = VTDarkCard, contentColor = VTWhite),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, VTDarkBorder, RoundedCornerShape(8.dp))
                        ) {
                            Text("Center Balance (Reset)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = VTDarkBorder)

                        // Pitch Lock Control
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(VTDarkCard)
                                .border(1.dp, VTDarkBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Pitch Control on Speed Change", color = VTWhite, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                Text(
                                    text = "Keeps natural vocal tone when playing faster or slower",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = playerState.preservePitch,
                                onCheckedChange = { playerManager.setPreservePitch(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = VTBlack,
                                    checkedTrackColor = VTWhite,
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = VTDarkBorder
                                )
                            )
                        }
                    }

                    // TAB 3: EQUALIZER
                    3 -> {
                        Text("Equalizer Presets", color = VTWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        val presets = listOf(
                            EqualizerPreset.FLAT to "Flat",
                            EqualizerPreset.BASS_BOOST to "Bass Boost",
                            EqualizerPreset.VOCAL to "Vocal",
                            EqualizerPreset.ROCK to "Rock",
                            EqualizerPreset.POP to "Pop",
                            EqualizerPreset.CLASSICAL to "Classical"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            presets.take(3).forEach { (preset, label) ->
                                val isSelected = playerState.equalizerPreset == preset
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { playerManager.setEqualizerPreset(preset) },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = VTWhite,
                                        selectedLabelColor = VTBlack,
                                        containerColor = VTDarkCard,
                                        labelColor = VTWhite
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            presets.drop(3).forEach { (preset, label) ->
                                val isSelected = playerState.equalizerPreset == preset
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { playerManager.setEqualizerPreset(preset) },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = VTWhite,
                                        selectedLabelColor = VTBlack,
                                        containerColor = VTDarkCard,
                                        labelColor = VTWhite
                                    )
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = VTDarkBorder)

                        Text("Frequency Bands (-12 dB to +12 dB)", color = VTWhite, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        val bandFrequencies = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
                        val bands = playerState.equalizerBands

                        bandFrequencies.forEachIndexed { idx, freqLabel ->
                            val currentLevel = if (idx in bands.indices) bands[idx] else 0
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = freqLabel,
                                    color = Color.LightGray,
                                    fontSize = 11.5.sp,
                                    modifier = Modifier.width(55.dp)
                                )
                                Slider(
                                    value = currentLevel.toFloat(),
                                    onValueChange = { playerManager.setEqualizerBand(idx, it.toInt()) },
                                    valueRange = -12f..12f,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = VTWhite,
                                        activeTrackColor = VTWhite,
                                        inactiveTrackColor = Color(0xFF333333)
                                    )
                                )
                                Text(
                                    text = "${if (currentLevel > 0) "+" else ""}$currentLevel dB",
                                    color = VTWhite,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(48.dp)
                                )
                            }
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
fun AudioBoostDialog(
    playerManager: VTPlayerManager,
    playerState: VTPlayerState,
    onDismiss: () -> Unit
) {
    AudioSettingsDialog(
        playerManager = playerManager,
        playerState = playerState,
        onDismiss = onDismiss
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

@Composable
fun PortableScreenAdjustDialog(
    playerState: com.example.player.VTPlayerState,
    onAutoAdjustToggle: (Boolean) -> Unit,
    onAutoOrientationToggle: (Boolean) -> Unit,
    onResizeModeSelected: (com.example.model.AspectRatioMode) -> Unit,
    onScaleSelected: (Float) -> Unit,
    onOpenPortable: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VTBlack,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.border(1.5.dp, VTDarkBorder, RoundedCornerShape(16.dp)),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.AspectRatio,
                    contentDescription = null,
                    tint = VTAccentBlue,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Screen Adjustments & Portable Video",
                    fontWeight = FontWeight.ExtraBold,
                    color = VTWhite,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Auto Adjust Portable Screen
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(VTDarkCard)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Auto-Adjust Portable Screen",
                            fontWeight = FontWeight.Bold,
                            color = VTWhite,
                            fontSize = 14.sp
                        )
                        Text(
                            "Adapts mini player proportions to video (16:9, vertical 9:16, 4:3, or 21:9)",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = playerState.autoAdjustPortableScreen,
                        onCheckedChange = onAutoAdjustToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VTAccentBlue,
                            checkedTrackColor = VTAccentBlue.copy(alpha = 0.5f)
                        )
                    )
                }

                // Auto Screen Orientation to Video
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(VTDarkCard)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Auto Screen Orientation",
                            fontWeight = FontWeight.Bold,
                            color = VTWhite,
                            fontSize = 14.sp
                        )
                        Text(
                            "Landscape for movies, Portrait for vertical videos",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = playerState.autoScreenOrientationToVideo,
                        onCheckedChange = onAutoOrientationToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VTAccentBlue,
                            checkedTrackColor = VTAccentBlue.copy(alpha = 0.5f)
                        )
                    )
                }

                // Portable Window Size
                Text("Portable Window Size Preset", fontWeight = FontWeight.Bold, color = VTWhite, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val scales = listOf(0.85f to "Compact (85%)", 1.0f to "Standard (100%)", 1.25f to "Large (125%)")
                    scales.forEach { (scale, label) ->
                        val isSelected = (kotlin.math.abs(playerState.portableWindowScale - scale) < 0.1f)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) VTAccentBlue.copy(alpha = 0.3f) else VTDarkCard)
                                .border(1.dp, if (isSelected) VTAccentBlue else VTDarkBorder, RoundedCornerShape(8.dp))
                                .clickable { onScaleSelected(scale) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) VTAccentBlue else VTWhite
                            )
                        }
                    }
                }

                // Screen Fit / Resize Mode
                Text("Screen Fit / Zoom Mode", fontWeight = FontWeight.Bold, color = VTWhite, fontSize = 13.sp)
                val modes = listOf(
                    com.example.model.AspectRatioMode.FIT to ("Auto Fit" to "Shows entire video without crop (letterbox)"),
                    com.example.model.AspectRatioMode.CROP_ZOOM to ("Auto Fill / Zoom" to "Fills the screen border-to-border"),
                    com.example.model.AspectRatioMode.STRETCH to ("Stretch" to "Stretches to fill dimensions")
                )
                modes.forEach { (mode, info) ->
                    val (label, desc) = info
                    val isSelected = (playerState.portableResizeMode == mode)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) VTAccentBlue.copy(alpha = 0.2f) else VTDarkCard)
                            .border(1.dp, if (isSelected) VTAccentBlue else VTDarkBorder, RoundedCornerShape(8.dp))
                            .clickable { onResizeModeSelected(mode) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(label, fontWeight = FontWeight.Bold, color = VTWhite, fontSize = 13.sp)
                            Text(desc, color = Color.Gray, fontSize = 10.5.sp)
                        }
                        if (isSelected) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = VTAccentBlue, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Quick Launch to Portable Player
                Button(
                    onClick = {
                        onDismiss()
                        onOpenPortable()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = VTAccentBlue, contentColor = VTWhite),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.PictureInPictureAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Switch to Portable Mini Player", fontWeight = FontWeight.Bold)
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
