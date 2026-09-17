package com.example.player

import com.example.model.AspectRatioMode
import com.example.model.AudioDecoderMode
import com.example.model.DecoderMode
import com.example.model.EqualizerPreset
import com.example.model.TrackItem
import com.example.model.VideoItem

data class VTPlayerState(
    val currentVideo: VideoItem? = null,
    val playlist: List<VideoItem> = emptyList(),
    val currentIndex: Int = 0,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isControlsLocked: Boolean = false,
    val areControlsVisible: Boolean = true,
    val aspectRatioMode: AspectRatioMode = AspectRatioMode.FIT,
    val isPipActive: Boolean = false,
    val isBackgroundPlaybackEnabled: Boolean = false,
    // Gestures HUD feedback
    val gestureBrightness: Float? = null, // 0f..1f
    val gestureVolume: Float? = null,     // 0f..1f
    val gestureSeekDeltaSec: Long? = null,
    val gestureZoomScale: Float = 1.0f,
    val gesturePanX: Float = 0.0f,
    val gesturePanY: Float = 0.0f,
    // A-B Repeat
    val isAbRepeatActive: Boolean = false,
    val repeatPointA: Long? = null,
    val repeatPointB: Long? = null,
    // Tracks
    val audioTracks: List<TrackItem> = emptyList(),
    val subtitleTracks: List<TrackItem> = emptyList(),
    val selectedAudioTrackId: String? = null,
    val selectedSubtitleTrackId: String? = null,
    // Delay offsets
    val subtitleDelayMs: Long = 0L,
    val audioDelayMs: Long = 0L,
    // Audio boost / Equalizer / Pipeline
    val isAudioBoostActive: Boolean = false,
    val volumePercent: Int = 100, // 0%–100% normal; up to 200% with LoudnessEnhancer volume boost
    val decoderMode: DecoderMode = DecoderMode.SW,
    val audioDecoderMode: AudioDecoderMode = AudioDecoderMode.AUTO,
    val audioPassthroughEnabled: Boolean = false,
    val leftRightBalance: Float = 0.0f, // -1.0f (Left) to +1.0f (Right), 0.0f = Center
    val equalizerPreset: EqualizerPreset = EqualizerPreset.FLAT,
    val equalizerBands: List<Int> = listOf(0, 0, 0, 0, 0), // 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz
    val preservePitch: Boolean = true,
    val audioPitch: Float = 1.0f,
    val hardwareDecoding: Boolean = false,
    val playerSessionId: Long = 0L,
    // Dialog overlays
    val showSpeedDialog: Boolean = false,
    val showTracksDialog: Boolean = false,
    val showSubtitleStyleDialog: Boolean = false,
    val showEqualizerDialog: Boolean = false,
    val showDecoderDialog: Boolean = false,
    val showAudioSettingsDialog: Boolean = false,
    val showMoreMenu: Boolean = false,
    val resumePrompt: ResumePromptData? = null
) {
    val formattedCurrentTime: String
        get() = formatTime(currentPosition)

    val formattedDuration: String
        get() = formatTime(duration)

    val formattedRemainingTime: String
        get() {
            val rem = (duration - currentPosition).coerceAtLeast(0L)
            return "-${formatTime(rem)}"
        }

    private fun formatTime(ms: Long): String {
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
}

data class ResumePromptData(
    val video: VideoItem,
    val savedPositionMs: Long,
    val totalDurationMs: Long
)
