package com.example.player

import android.content.Context
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.example.model.AspectRatioMode
import com.example.model.TrackItem
import com.example.model.VideoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@UnstableApi
class VTPlayerManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private var exoPlayer: ExoPlayer? = null
    private var trackSelector: DefaultTrackSelector = DefaultTrackSelector(context)
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var equalizer: Equalizer? = null

    private val _playerState = MutableStateFlow(VTPlayerState())
    val playerState: StateFlow<VTPlayerState> = _playerState.asStateFlow()

    private var progressJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val isBuffering = playbackState == Player.STATE_BUFFERING
            val duration = exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L
            _playerState.update {
                it.copy(
                    isBuffering = isBuffering,
                    duration = if (duration != C.TIME_UNSET) duration else it.duration
                )
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playerState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onTracksChanged(tracks: Tracks) {
            updateAvailableTracks(tracks)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val idx = exoPlayer?.currentMediaItemIndex ?: 0
            val state = _playerState.value
            if (state.playlist.isNotEmpty() && idx in state.playlist.indices) {
                _playerState.update {
                    it.copy(
                        currentIndex = idx,
                        currentVideo = state.playlist[idx]
                    )
                }
            }
        }
    }

    init {
        initPlayer(useHardware = true)
    }

    fun initPlayer(useHardware: Boolean = true) {
        exoPlayer?.release()
        exoPlayer = null
        loudnessEnhancer?.release()
        loudnessEnhancer = null

        trackSelector = DefaultTrackSelector(context)

        val renderersFactory = DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(
                if (useHardware) DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
                else DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
            )
        }

        exoPlayer = ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .setSeekForwardIncrementMs(10000)
            .setSeekBackIncrementMs(10000)
            .setUsePlatformDiagnostics(false)
            .build()
            .apply {
                playWhenReady = true
                addListener(playerListener)
            }

        _playerState.update { it.copy(hardwareDecoding = useHardware) }
        startProgressTracking()
    }

    fun getExoPlayer(): ExoPlayer? = exoPlayer

    private fun updateAvailableTracks(tracks: Tracks) {
        val audioList = mutableListOf<TrackItem>()
        val subList = mutableListOf<TrackItem>()
        var selectedAudioId: String? = null
        var selectedSubId: String? = null

        for (group in tracks.groups) {
            val trackType = group.type
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                val isSelected = group.isTrackSelected(i)
                val id = "${group.mediaTrackGroup.id}_$i"
                val label = format.label ?: format.language ?: "Track ${i + 1}"
                val lang = format.language ?: "Unknown"

                if (trackType == C.TRACK_TYPE_AUDIO) {
                    audioList.add(TrackItem(id, label, lang, isSelected))
                    if (isSelected) selectedAudioId = id
                } else if (trackType == C.TRACK_TYPE_TEXT) {
                    subList.add(TrackItem(id, label, lang, isSelected))
                    if (isSelected) selectedSubId = id
                }
            }
        }

        _playerState.update {
            it.copy(
                audioTracks = audioList,
                subtitleTracks = subList,
                selectedAudioTrackId = selectedAudioId,
                selectedSubtitleTrackId = selectedSubId
            )
        }
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = coroutineScope.launch(Dispatchers.Main) {
            while (isActive) {
                exoPlayer?.let { player ->
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    val dur = player.duration.coerceAtLeast(0L)
                    val state = _playerState.value

                    // Check A-B repeat
                    if (state.isAbRepeatActive && state.repeatPointA != null && state.repeatPointB != null) {
                        if (pos >= state.repeatPointB) {
                            player.seekTo(state.repeatPointA)
                        }
                    }

                    _playerState.update {
                        it.copy(
                            currentPosition = pos,
                            duration = if (dur != C.TIME_UNSET && dur > 0) dur else it.duration
                        )
                    }
                }
                delay(300)
            }
        }
    }

    fun playVideo(
        video: VideoItem,
        playlist: List<VideoItem> = listOf(video),
        startIndex: Int = 0,
        resumePosition: Long = 0L,
        externalSubtitleUri: Uri? = null
    ) {
        val player = exoPlayer ?: return
        player.stop()
        player.clearMediaItems()

        val mediaItems = playlist.map { item ->
            val builder = MediaItem.Builder()
                .setUri(item.uriString)
                .setMediaId(item.id.toString())

            // If this is the active video and has external subtitle
            if (item.id == video.id && externalSubtitleUri != null) {
                val subConfig = MediaItem.SubtitleConfiguration.Builder(externalSubtitleUri)
                    .setMimeType(androidx.media3.common.MimeTypes.APPLICATION_SUBRIP)
                    .setLanguage("en")
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()
                builder.setSubtitleConfigurations(listOf(subConfig))
            }
            builder.build()
        }

        player.setMediaItems(mediaItems, startIndex, resumePosition)
        player.prepare()
        player.play()

        _playerState.update {
            it.copy(
                currentVideo = video,
                playlist = playlist,
                currentIndex = startIndex,
                currentPosition = resumePosition,
                isPlaying = true
            )
        }
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs.coerceAtLeast(0L))
        _playerState.update { it.copy(currentPosition = positionMs) }
    }

    fun seekBy(deltaMs: Long) {
        val player = exoPlayer ?: return
        val newPos = (player.currentPosition + deltaMs).coerceIn(0L, (player.duration.coerceAtLeast(0L)))
        player.seekTo(newPos)
        _playerState.update { it.copy(currentPosition = newPos) }
    }

    fun next() {
        if (exoPlayer?.hasNextMediaItem() == true) {
            exoPlayer?.seekToNextMediaItem()
        }
    }

    fun previous() {
        if (exoPlayer?.hasPreviousMediaItem() == true) {
            exoPlayer?.seekToPreviousMediaItem()
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.playbackParameters = PlaybackParameters(speed)
        _playerState.update { it.copy(playbackSpeed = speed) }
    }

    fun stepFrame(forward: Boolean) {
        // Approximate 1 frame at 24/30 fps (~41ms)
        val stepMs = if (forward) 41L else -41L
        seekBy(stepMs)
    }

    fun setAspectRatioMode(mode: AspectRatioMode) {
        _playerState.update { it.copy(aspectRatioMode = mode) }
    }

    fun setZoomScale(scale: Float) {
        _playerState.update { it.copy(gestureZoomScale = scale.coerceIn(0.5f, 3.0f)) }
    }

    fun toggleControlsLock() {
        _playerState.update { it.copy(isControlsLocked = !it.isControlsLocked) }
    }

    fun setControlsVisibility(visible: Boolean) {
        if (_playerState.value.isControlsLocked) return
        _playerState.update { it.copy(areControlsVisible = visible) }
    }

    fun toggleControlsVisibility() {
        if (_playerState.value.isControlsLocked) return
        _playerState.update { it.copy(areControlsVisible = !it.areControlsVisible) }
    }

    // Gestures HUD
    fun setGestureBrightness(value: Float?) {
        _playerState.update { it.copy(gestureBrightness = value) }
    }

    fun setGestureVolume(value: Float?) {
        _playerState.update { it.copy(gestureVolume = value) }
    }

    fun setGestureSeekDelta(deltaSec: Long?) {
        _playerState.update { it.copy(gestureSeekDeltaSec = deltaSec) }
    }

    // A-B Repeat
    fun setRepeatPointA() {
        val current = exoPlayer?.currentPosition ?: 0L
        _playerState.update { it.copy(repeatPointA = current) }
    }

    fun setRepeatPointB() {
        val current = exoPlayer?.currentPosition ?: 0L
        _playerState.update {
            it.copy(
                repeatPointB = current,
                isAbRepeatActive = (it.repeatPointA != null && current > it.repeatPointA)
            )
        }
    }

    fun clearAbRepeat() {
        _playerState.update {
            it.copy(
                repeatPointA = null,
                repeatPointB = null,
                isAbRepeatActive = false
            )
        }
    }

    // Track selections
    fun selectAudioTrack(trackItem: TrackItem) {
        val tracks = exoPlayer?.currentTracks ?: return
        for (group in tracks.groups) {
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (i in 0 until group.length) {
                    val id = "${group.mediaTrackGroup.id}_$i"
                    if (id == trackItem.id) {
                        trackSelector.setParameters(
                            trackSelector.buildUponParameters()
                                .setOverrideForType(
                                    TrackSelectionOverride(group.mediaTrackGroup, i)
                                )
                        )
                        _playerState.update { it.copy(selectedAudioTrackId = trackItem.id) }
                        return
                    }
                }
            }
        }
    }

    fun selectSubtitleTrack(trackItem: TrackItem?) {
        if (trackItem == null) {
            // Disable subtitles
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            )
            _playerState.update { it.copy(selectedSubtitleTrackId = null) }
            return
        }

        val tracks = exoPlayer?.currentTracks ?: return
        trackSelector.setParameters(
            trackSelector.buildUponParameters()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
        )

        for (group in tracks.groups) {
            if (group.type == C.TRACK_TYPE_TEXT) {
                for (i in 0 until group.length) {
                    val id = "${group.mediaTrackGroup.id}_$i"
                    if (id == trackItem.id) {
                        trackSelector.setParameters(
                            trackSelector.buildUponParameters()
                                .setOverrideForType(
                                    TrackSelectionOverride(group.mediaTrackGroup, i)
                                )
                        )
                        _playerState.update { it.copy(selectedSubtitleTrackId = trackItem.id) }
                        return
                    }
                }
            }
        }
    }

    // Audio delay & Subtitle delay
    fun setAudioDelayMs(ms: Long) {
        _playerState.update { it.copy(audioDelayMs = ms) }
    }

    fun setSubtitleDelayMs(ms: Long) {
        _playerState.update { it.copy(subtitleDelayMs = ms) }
    }

    // Audio boost toggle
    fun toggleAudioBoost() {
        val active = !_playerState.value.isAudioBoostActive
        val audioSessionId = exoPlayer?.audioSessionId ?: C.AUDIO_SESSION_ID_UNSET
        if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
            try {
                if (loudnessEnhancer == null) {
                    loudnessEnhancer = LoudnessEnhancer(audioSessionId)
                }
                loudnessEnhancer?.setTargetGain(if (active) 1200 else 0) // +12 dB
                loudnessEnhancer?.enabled = active
            } catch (_: Exception) {}
        }
        _playerState.update { it.copy(isAudioBoostActive = active) }
    }

    fun toggleBackgroundPlayback() {
        _playerState.update { it.copy(isBackgroundPlaybackEnabled = !it.isBackgroundPlaybackEnabled) }
    }

    fun setPipActive(active: Boolean) {
        _playerState.update { it.copy(isPipActive = active) }
    }

    // Dialog toggles
    fun setShowSpeedDialog(show: Boolean) = _playerState.update { it.copy(showSpeedDialog = show) }
    fun setShowTracksDialog(show: Boolean) = _playerState.update { it.copy(showTracksDialog = show) }
    fun setShowSubtitleStyleDialog(show: Boolean) = _playerState.update { it.copy(showSubtitleStyleDialog = show) }
    fun setShowEqualizerDialog(show: Boolean) = _playerState.update { it.copy(showEqualizerDialog = show) }
    fun setShowMoreMenu(show: Boolean) = _playerState.update { it.copy(showMoreMenu = show) }

    fun release() {
        progressJob?.cancel()
        loudnessEnhancer?.release()
        equalizer?.release()
        exoPlayer?.release()
        exoPlayer = null
    }
}
