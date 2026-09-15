package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VTAccentGreen
import com.example.ui.theme.VTAccentRed
import com.example.ui.viewmodel.VTViewModel

@Composable
fun SettingsTab(
    viewModel: VTViewModel,
    onOpenStream: () -> Unit = {},
    onOpenVault: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val prefs by viewModel.preferences.collectAsState()
    val colorScheme = MaterialTheme.colorScheme

    var showThemeDialog by remember { mutableStateOf(false) }
    var showFontSizeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showOrientationDialog by remember { mutableStateOf(false) }
    var showDecoderDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "VT Player Settings",
            color = colorScheme.onBackground,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp
        )
        Text(
            text = "Playback, gestures, decoder, audio & themes",
            color = colorScheme.onBackground.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Stream & Private Vault Section (Prominent Access)
            item {
                SettingsSectionHeader("Stream & Private Vault", Icons.Filled.Security)
                SettingsCard {
                    SettingsFeatureRow(
                        title = "Network Stream",
                        subtitle = "Play online HLS (.m3u8), DASH & MP4 video links",
                        icon = Icons.Filled.Podcasts,
                        onClick = onOpenStream
                    )
                    HorizontalDivider(color = colorScheme.outlineVariant)
                    SettingsFeatureRow(
                        title = "Private Vault",
                        subtitle = "PIN protected secure video locker & hidden files",
                        icon = Icons.Filled.Lock,
                        onClick = onOpenVault
                    )
                }
            }

            // Theme & Appearance (Item 10: White & Black high contrast theme)
            item {
                SettingsSectionHeader("Theme & Appearance", Icons.Filled.BrightnessMedium)
                SettingsCard {
                    SettingsRow(
                        title = "High Contrast Theme",
                        subtitle = when (prefs.themeMode) {
                            "DARK" -> "High Contrast Pure Black"
                            "LIGHT" -> "High Contrast Pure White"
                            else -> "System Default (Auto)"
                        },
                        onClick = { showThemeDialog = true }
                    )
                    HorizontalDivider(color = colorScheme.outlineVariant)
                    SettingsRow(
                        title = "Font Size / फ़ॉन्ट साइज़",
                        subtitle = when (prefs.fontSize) {
                            "SMALL" -> "Small (Compact text)"
                            "LARGE" -> "Large (High readability)"
                            else -> "Medium (Standard default)"
                        },
                        onClick = { showFontSizeDialog = true }
                    )
                    HorizontalDivider(color = colorScheme.outlineVariant)
                    SettingsRow(
                        title = "Language / भाषा",
                        subtitle = if (prefs.language == "HI") "हिन्दी (Hindi)" else "English",
                        onClick = { showLanguageDialog = true }
                    )
                }
            }

            // Playback Options
            item {
                SettingsSectionHeader("Default Playback & Screen", Icons.Filled.PlayCircleOutline)
                SettingsCard {
                    SettingsRow(
                        title = "Movie Screen Orientation",
                        subtitle = when (prefs.movieOrientation) {
                            "VERTICAL" -> "Vertical (Portrait)"
                            "AUTO" -> "Auto Sensor (Follows device)"
                            else -> "Horizontal (Landscape - Default)"
                        },
                        onClick = { showOrientationDialog = true }
                    )
                    HorizontalDivider(color = colorScheme.outlineVariant)
                    SettingsSwitchRow(
                        title = "Clean Movie Screen (Hide Signal & Time)",
                        subtitle = "Do not show top mobile signal, battery and time line during video playback",
                        checked = prefs.hideStatusBarInPlayer,
                        onCheckedChange = { chk ->
                            viewModel.setHideStatusBarInPlayer(chk)
                        }
                    )
                    HorizontalDivider(color = colorScheme.outlineVariant)
                    SettingsSwitchRow(
                        title = "Resume From Last Position",
                        subtitle = "Remember playback timestamps automatically",
                        checked = prefs.resumePlayback,
                        onCheckedChange = { chk ->
                            viewModel.updatePreferences { it.copy(resumePlayback = chk) }
                        }
                    )
                    HorizontalDivider(color = colorScheme.outlineVariant)
                    SettingsSwitchRow(
                        title = "Background Audio Playback",
                        subtitle = "Continue playing sound when screen is off or in background",
                        checked = prefs.backgroundPlayback,
                        onCheckedChange = { chk ->
                            viewModel.updatePreferences { it.copy(backgroundPlayback = chk) }
                        }
                    )
                }
            }

            // Gesture Controls
            item {
                SettingsSectionHeader("Gesture Controls", Icons.Filled.Gesture)
                SettingsCard {
                    SettingsSwitchRow(
                        title = "Left Swipe: Brightness",
                        subtitle = "Vertical swipe on left screen adjusts brightness HUD",
                        checked = prefs.brightnessGesture,
                        onCheckedChange = { chk ->
                            viewModel.updatePreferences { it.copy(brightnessGesture = chk) }
                        }
                    )
                    HorizontalDivider(color = colorScheme.outlineVariant)
                    SettingsSwitchRow(
                        title = "Right Swipe: Volume",
                        subtitle = "Vertical swipe on right screen adjusts volume HUD",
                        checked = prefs.volumeGesture,
                        onCheckedChange = { chk ->
                            viewModel.updatePreferences { it.copy(volumeGesture = chk) }
                        }
                    )
                    HorizontalDivider(color = colorScheme.outlineVariant)
                    SettingsSwitchRow(
                        title = "Horizontal Swipe: Seek",
                        subtitle = "Drag horizontally to scrub video with time HUD",
                        checked = prefs.swipeSeekGesture,
                        onCheckedChange = { chk ->
                            viewModel.updatePreferences { it.copy(swipeSeekGesture = chk) }
                        }
                    )
                }
            }

            // Three Decoding Engines (SW, HW, and HW+)
            item {
                SettingsSectionHeader("Three Decoding Engines (SW, HW, and HW+)", Icons.Filled.Memory)
                SettingsCard {
                    SettingsRow(
                        title = "Default Video Decoder",
                        subtitle = when (prefs.defaultDecoderMode) {
                            "HW" -> "HW (Hardware Standard, 0%–100% Volume)"
                            "HW_PLUS" -> "HW+ (+100% Audio Boost, 101%–120% up to +20 dB)"
                            else -> "SW (Software Standard, 0%–100% Volume - First Screen)"
                        },
                        onClick = { showDecoderDialog = true }
                    )
                }
            }

            // Audio & Equalizer
            item {
                SettingsSectionHeader("Audio & Subtitle Enhancements", Icons.Filled.GraphicEq)
                SettingsCard {
                    SettingsSwitchRow(
                        title = "Audio Boost (+12dB)",
                        subtitle = "Loudness enhancer for low volume clips",
                        checked = prefs.audioBoost,
                        onCheckedChange = { chk ->
                            viewModel.updatePreferences { it.copy(audioBoost = chk) }
                        }
                    )
                }
            }

            // Data & History
            item {
                SettingsSectionHeader("Data & Storage", Icons.Filled.DeleteSweep)
                SettingsCard {
                    SettingsRow(
                        title = "Clear Playback History",
                        subtitle = "Reset all recently played videos and positions",
                        onClick = { showClearHistoryDialog = true },
                        isDestructive = true
                    )
                }
            }

            // App About
            item {
                SettingsCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colorScheme.primary)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("VT", color = colorScheme.onPrimary, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("VT Player v1.0.0", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = colorScheme.onSurface)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Ultra-fast media engine with gesture controls, multi-track audio, subtitle sync, A-B loop, PiP, URL streaming and private vault.",
                            fontSize = 12.sp,
                            color = colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }

    // Theme Selection Dialog
    if (showThemeDialog) {
        val themes = listOf(
            "SYSTEM" to "System Default",
            "DARK" to "High Contrast Dark (Pure Black)",
            "LIGHT" to "High Contrast Light (Pure White)"
        )
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.5.dp, colorScheme.outline, RoundedCornerShape(16.dp)),
            title = { Text("Choose Theme", fontWeight = FontWeight.Bold, color = colorScheme.onSurface) },
            text = {
                Column {
                    themes.forEach { (mode, label) ->
                        val isSelected = (mode == prefs.themeMode)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.updatePreferences { it.copy(themeMode = mode) }
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium, color = colorScheme.onSurface)
                            if (isSelected) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showThemeDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary, contentColor = colorScheme.onPrimary)
                ) {
                    Text("Close")
                }
            }
        )
    }

    // Font Size Selection Dialog
    if (showFontSizeDialog) {
        val fontSizes = listOf(
            "SMALL" to ("Small (छोटा)" to "Compact text, fits more content per screen"),
            "MEDIUM" to ("Medium (मध्यम)" to "Standard balanced font size for all screens"),
            "LARGE" to ("Large (बड़ा)" to "High legibility, larger text and subtitles")
        )
        AlertDialog(
            onDismissRequest = { showFontSizeDialog = false },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.5.dp, colorScheme.outline, RoundedCornerShape(16.dp)),
            title = {
                Text(
                    text = "Select Font Size",
                    fontWeight = FontWeight.ExtraBold,
                    color = colorScheme.onSurface,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    fontSizes.forEach { (key, info) ->
                        val (label, description) = info
                        val isSelected = (key == prefs.fontSize)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) colorScheme.primaryContainer else Color.Transparent)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) colorScheme.primary else colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    viewModel.setFontSize(key)
                                    showFontSizeDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = label,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = when (key) {
                                        "SMALL" -> 14.sp
                                        "LARGE" -> 19.sp
                                        else -> 16.sp
                                    },
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = description,
                                    fontSize = 11.sp,
                                    color = colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showFontSizeDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary
                    )
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Language Dialog
    if (showLanguageDialog) {
        val langs = listOf("EN" to "English", "HI" to "हिन्दी (Hindi)")
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.5.dp, colorScheme.outline, RoundedCornerShape(16.dp)),
            title = { Text("Select Language", fontWeight = FontWeight.Bold, color = colorScheme.onSurface) },
            text = {
                Column {
                    langs.forEach { (code, name) ->
                        val isSelected = (code == prefs.language)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.updatePreferences { it.copy(language = code) }
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(name, fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium, color = colorScheme.onSurface)
                            if (isSelected) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showLanguageDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary, contentColor = colorScheme.onPrimary)
                ) {
                    Text("Close")
                }
            }
        )
    }

    // Clear History Confirmation
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.5.dp, colorScheme.outline, RoundedCornerShape(16.dp)),
            title = { Text("Clear Playback History?", fontWeight = FontWeight.Bold, color = colorScheme.onSurface) },
            text = {
                Text("This will remove all resume progress and recently watched records.", color = colorScheme.onSurface.copy(alpha = 0.8f))
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearHistory()
                        showClearHistoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VTAccentRed, contentColor = androidx.compose.ui.graphics.Color.White)
                ) {
                    Text("Clear All", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showClearHistoryDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.surfaceVariant, contentColor = colorScheme.onSurfaceVariant)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Movie Screen Orientation Dialog
    if (showOrientationDialog) {
        val orientations = listOf(
            "HORIZONTAL" to ("Horizontal (Landscape)" to "Plays movies horizontally. Recommended for widescreen cinema viewing"),
            "AUTO" to ("Auto Sensor" to "Screen flips horizontally or vertically depending on how you hold the device"),
            "VERTICAL" to ("Vertical (Portrait)" to "Plays videos upright vertically")
        )
        AlertDialog(
            onDismissRequest = { showOrientationDialog = false },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.5.dp, colorScheme.outline, RoundedCornerShape(16.dp)),
            title = {
                Text(
                    text = "Movie Screen Orientation",
                    fontWeight = FontWeight.ExtraBold,
                    color = colorScheme.onSurface,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    orientations.forEach { (key, info) ->
                        val (label, description) = info
                        val isSelected = (key == prefs.movieOrientation)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) colorScheme.primaryContainer else Color.Transparent)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) colorScheme.primary else colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    viewModel.setMovieOrientation(key)
                                    showOrientationDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = label,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = description,
                                    fontSize = 11.sp,
                                    color = colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showOrientationDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary, contentColor = colorScheme.onPrimary)
                ) {
                    Text("Close")
                }
            }
        )
    }

    // Video Decoder Selection Dialog: Three Decoding Engines (SW, HW, and HW+)
    if (showDecoderDialog) {
        val decoders = listOf(
            "SW" to ("SW (Software Standard - First Screen)" to "CPU software decoder fixed to 0% – 100% (Standard device music volume). Maximum codec compatibility."),
            "HW" to ("HW (Hardware Standard)" to "Standard system hardware decoder fixed to 0% – 100% (Standard device music volume). Low power consumption."),
            "HW_PLUS" to ("HW+ (Hardware+ Audio Boost)" to "Hardware acceleration with 101% – 120% volume: +100% Audio Boost driven by the Loudness Enhancer audio effect pipeline, providing up to +20 dB amplification.")
        )
        AlertDialog(
            onDismissRequest = { showDecoderDialog = false },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.5.dp, colorScheme.outline, RoundedCornerShape(16.dp)),
            title = {
                Text(
                    text = "Three Decoding Engines (SW, HW, and HW+)",
                    fontWeight = FontWeight.ExtraBold,
                    color = colorScheme.onSurface,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    decoders.forEach { (key, info) ->
                        val (label, description) = info
                        val isSelected = (key == prefs.defaultDecoderMode)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) colorScheme.primaryContainer else Color.Transparent)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) colorScheme.primary else colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    viewModel.updatePreferences { it.copy(defaultDecoderMode = key) }
                                    showDecoderDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = label,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = description,
                                    fontSize = 11.sp,
                                    color = colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDecoderDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary, contentColor = colorScheme.onPrimary)
                ) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String, icon: ImageVector) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = colorScheme.onBackground, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, color = colorScheme.onBackground, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
    }
}

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, colorScheme.outline, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            content()
        }
    }
}

@Composable
fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (isDestructive) VTAccentRed else colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = colorScheme.onSurface.copy(alpha = 0.65f)
            )
        }
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = colorScheme.onSurface.copy(alpha = 0.65f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colorScheme.onPrimary,
                checkedTrackColor = colorScheme.primary,
                uncheckedThumbColor = colorScheme.outline,
                uncheckedTrackColor = colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
fun SettingsFeatureRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colorScheme.onSurface.copy(alpha = 0.45f),
            modifier = Modifier.size(20.dp)
        )
    }
}
