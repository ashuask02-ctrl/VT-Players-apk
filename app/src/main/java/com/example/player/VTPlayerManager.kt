package com.example.player

import android.content.Context
import android.media.AudioManager
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.example.model.AspectRatioMode
import com.example.model.DecoderMode
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

    var onPositionSaved: ((uri: String, position: Long, duration: Long) -> Unit)? = null

    private var progressJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val isBuffering = playbackState == Player.STATE_BUFFERING
            val duration = exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L
            if (playbackState == Player.STATE_READY) {
                exoPlayer?.currentTracks?.let { updateAvailableTracks(it) }
            }
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

        override fun onPlayerError(error: PlaybackException) {
            android.util.Log.e("VTPlayerManager", "Player error with mode ${_playerState.value.decoderMode}: ${error.errorCodeName}", error)
            // If an error occurs in SW mode, automatically recover smoothly by switching to HW_PLUS
            if (_playerState.value.decoderMode == DecoderMode.SW) {
                coroutineScope.launch(Dispatchers.Main) {
                    val curPos = _playerState.value.currentPosition
                    val curVid = _playerState.value.currentVideo
                    val curPlaylist = _playerState.value.playlist
                    val curIdx = _playerState.value.currentIndex
                    initPlayer(decoderMode = DecoderMode.HW_PLUS)
                    if (curVid != null) {
                        playVideo(curVid, curPlaylist, curIdx, curPos)
                    }
                }
            }
        }
    }

    init {
        initPlayer(decoderMode = DecoderMode.HW_PLUS)
    }

    fun initPlayer(useHardware: Boolean) {
        initPlayer(if (useHardware) DecoderMode.HW_PLUS else DecoderMode.SW)
    }

    fun initPlayer(decoderMode: DecoderMode = _playerState.value.decoderMode) {
        exoPlayer?.release()
        exoPlayer = null
        loudnessEnhancer?.release()
        loudnessEnhancer = null

        trackSelector = DefaultTrackSelector(context)

        val renderersFactory = DefaultRenderersFactory(context).apply {
            setEnableDecoderFallback(true)
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            setMediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
                val defaultList = try {
                    MediaCodecSelector.DEFAULT.getDecoderInfos(
                        mimeType,
                        requiresSecureDecoder,
                        requiresTunnelingDecoder
                    )
                } catch (e: Exception) {
                    emptyList()
                }

                // Audio codecs are kept intact so multi-channel/AAC/MP3/AC3 audio decodes reliably in all modes
                if (!mimeType.startsWith("video/", ignoreCase = true)) {
                    return@setMediaCodecSelector defaultList
                }

                when (decoderMode) {
                    DecoderMode.SW -> {
                        // Software video decoder: Prioritize CPU software decoders
                        val swList = defaultList.filter {
                            it.softwareOnly ||
                            !it.hardwareAccelerated ||
                            it.name.startsWith("c2.android.", ignoreCase = true) ||
                            it.name.startsWith("OMX.google.", ignoreCase = true) ||
                            it.name.contains(".sw.", ignoreCase = true) ||
                            it.name.contains("sw", ignoreCase = true)
                        }
                        if (swList.isNotEmpty()) {
                            swList + defaultList.filterNot { swList.contains(it) }
                        } else {
                            defaultList
                        }
                    }
                    DecoderMode.HW, DecoderMode.HW_PLUS -> {
                        // Hardware accelerated decoder
                        val hwList = defaultList.filter {
                            it.hardwareAccelerated ||
                            (!it.softwareOnly &&
                             !it.name.startsWith("c2.android.", ignoreCase = true) &&
                             !it.name.startsWith("OMX.google.", ignoreCase = true) &&
                             !it.name.contains(".sw.", ignoreCase = true))
                        }
                        if (hwList.isNotEmpty()) {
                            hwList + defaultList.filterNot { hwList.contains(it) }
                        } else {
                            defaultList
                        }
                    }
                }
            }
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

        _playerState.update {
            it.copy(
                decoderMode = decoderMode,
                hardwareDecoding = (decoderMode != DecoderMode.SW),
                playerSessionId = System.currentTimeMillis()
            )
        }
        startProgressTracking()
    }

    fun getExoPlayer(): ExoPlayer? = exoPlayer

    companion object {
        fun formatLanguageLabel(lang: String?, fallbackLabel: String): String {
            val code = lang?.trim()?.lowercase(java.util.Locale.ROOT)
            return when {
                code == "hi" || code == "hin" -> "हिन्दी (Hindi)"
                code == "en" || code == "eng" -> "English"
                code == "es" || code == "spa" -> "Spanish"
                code == "fr" || code == "fra" -> "French"
                code == "de" || code == "deu" -> "German"
                code == "ja" || code == "jpn" -> "Japanese"
                code == "ko" || code == "kor" -> "Korean"
                code == "zh" || code == "zho" -> "Chinese"
                code == "ar" || code == "ara" -> "Arabic"
                code == "bn" || code == "ben" -> "Bengali"
                code == "ta" || code == "tam" -> "Tamil"
                code == "te" || code == "tel" -> "Telugu"
                code == "mr" || code == "mar" -> "Marathi"
                code == "gu" || code == "guj" -> "Gujarati"
                code == "pa" || code == "pan" -> "Punjabi"
                code == "ur" || code == "urd" -> "Urdu"
                code != null && code.length in 2..3 && code != "und" -> {
                    val display = java.util.Locale.forLanguageTag(code).displayLanguage
                    if (display.isNotBlank()) display else fallbackLabel
                }
                else -> fallbackLabel
            }
        }
    }

    private fun updateAvailableTracks(tracks: Tracks) {
        val audioList = mutableListOf<TrackItem>()
        val subList = mutableListOf<TrackItem>()
        var selectedAudioId: String? = null
        var selectedSubId: String? = null

        tracks.groups.forEachIndexed { groupIndex, group ->
            val trackType = group.type
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                val isSelected = group.isTrackSelected(i)
                val id = "${trackType}_${groupIndex}_$i"
                val defaultLabel = format.label ?: if (trackType == C.TRACK_TYPE_AUDIO) "Audio Track ${audioList.size + 1}" else "Subtitle ${subList.size + 1}"
                val rawLang = format.language ?: "Unknown"
                val label = if (rawLang.isNotBlank() && rawLang != "Unknown" && rawLang != "und") {
                    formatLanguageLabel(rawLang, defaultLabel)
                } else defaultLabel

                if (trackType == C.TRACK_TYPE_AUDIO) {
                    audioList.add(TrackItem(id, label, rawLang, isSelected))
                    if (isSelected) selectedAudioId = id
                } else if (trackType == C.TRACK_TYPE_TEXT) {
                    subList.add(TrackItem(id, label, rawLang, isSelected))
                    if (isSelected) selectedSubId = id
                }
            }
        }

        _playerState.update {
            it.copy(
                audioTracks = audioList,
                subtitleTracks = subList,
                selectedAudioTrackId = selectedAudioId ?: it.selectedAudioTrackId,
                selectedSubtitleTrackId = selectedSubId ?: it.selectedSubtitleTrackId
            )
        }
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = coroutineScope.launch(Dispatchers.Main) {
            var saveCounter = 0
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

                    // Periodically record position (~every 1.5 seconds)
                    saveCounter++
                    if (saveCounter >= 5) {
                        saveCounter = 0
                        state.currentVideo?.let { vid ->
                            if (pos > 1000L) {
                                onPositionSaved?.invoke(
                                    vid.uriString,
                                    pos,
                                    if (dur != C.TIME_UNSET && dur > 0) dur else vid.duration
                                )
                            }
                        }
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
        _playerState.update { it.copy(gestureZoomScale = scale.coerceIn(0.5f, 4.0f)) }
    }

    fun setZoomAndPan(scale: Float, panX: Float, panY: Float) {
        _playerState.update {
            it.copy(
                gestureZoomScale = scale.coerceIn(0.5f, 4.0f),
                gesturePanX = panX,
                gesturePanY = panY
            )
        }
    }

    fun resetZoomAndPan() {
        _playerState.update {
            it.copy(
                gestureZoomScale = 1.0f,
                gesturePanX = 0f,
                gesturePanY = 0f
            )
        }
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
        tracks.groups.forEachIndexed { groupIndex, group ->
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (i in 0 until group.length) {
                    val id = "${C.TRACK_TYPE_AUDIO}_${groupIndex}_$i"
                    if (id == trackItem.id || "${group.mediaTrackGroup.id}_$i" == trackItem.id) {
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
        tracks.groups.forEachIndexed { groupIndex, group ->
            if (group.type == C.TRACK_TYPE_TEXT) {
                for (i in 0 until group.length) {
                    val id = "${C.TRACK_TYPE_TEXT}_${groupIndex}_$i"
                    if (id == trackItem.id || "${group.mediaTrackGroup.id}_$i" == trackItem.id) {
                        trackSelector.setParameters(
                            trackSelector.buildUponParameters()
                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
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

    fun selectAudioByLanguage(langPrefix: String): Boolean {
        val target = langPrefix.lowercase(java.util.Locale.ROOT)
        val match = _playerState.value.audioTracks.firstOrNull {
            it.language.lowercase().startsWith(target) ||
            it.label.lowercase().contains(target) ||
            (target == "hi" && (it.language.lowercase() in listOf("hi", "hin") || it.label.lowercase().contains("hindi") || it.label.contains("हिन्दी"))) ||
            (target == "en" && (it.language.lowercase() in listOf("en", "eng") || it.label.lowercase().contains("english")))
        }
        if (match != null) {
            selectAudioTrack(match)
            return true
        }
        return false
    }

    fun selectSubtitleByLanguage(langPrefix: String): Boolean {
        val target = langPrefix.lowercase(java.util.Locale.ROOT)
        val match = _playerState.value.subtitleTracks.firstOrNull {
            it.language.lowercase().startsWith(target) ||
            it.label.lowercase().contains(target) ||
            (target == "hi" && (it.language.lowercase() in listOf("hi", "hin") || it.label.lowercase().contains("hindi") || it.label.contains("हिन्दी"))) ||
            (target == "en" && (it.language.lowercase() in listOf("en", "eng") || it.label.lowercase().contains("english")))
        }
        if (match != null) {
            selectSubtitleTrack(match)
            return true
        }
        return false
    }

    fun cycleAudioTrack(): String {
        val tracks = _playerState.value.audioTracks
        if (tracks.isEmpty()) return "Audio: Default"
        val currentIdx = tracks.indexOfFirst { it.id == _playerState.value.selectedAudioTrackId }
        val nextIdx = (currentIdx + 1) % tracks.size
        val nextTrack = tracks[nextIdx]
        selectAudioTrack(nextTrack)
        return nextTrack.label
    }

    fun cycleSubtitleTrack(): String {
        val tracks = _playerState.value.subtitleTracks
        if (tracks.isEmpty()) return "Subtitles: None"
        if (_playerState.value.selectedSubtitleTrackId == null) {
            selectSubtitleTrack(tracks.first())
            return tracks.first().label
        }
        val currentIdx = tracks.indexOfFirst { it.id == _playerState.value.selectedSubtitleTrackId }
        if (currentIdx == tracks.size - 1) {
            selectSubtitleTrack(null)
            return "Subtitles: Off"
        } else {
            val nextTrack = tracks[currentIdx + 1]
            selectSubtitleTrack(nextTrack)
            return nextTrack.label
        }
    }

    fun getActiveAudioTrackLabel(): String {
        val state = _playerState.value
        val current = state.audioTracks.firstOrNull { it.id == state.selectedAudioTrackId }
        return current?.label ?: if (state.audioTracks.isNotEmpty()) state.audioTracks.first().label else "Audio: Default"
    }

    fun getActiveSubtitleTrackLabel(): String {
        val state = _playerState.value
        if (state.selectedSubtitleTrackId == null) return "Sub: Off"
        val current = state.subtitleTracks.firstOrNull { it.id == state.selectedSubtitleTrackId }
        return current?.label ?: "Sub: Active"
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

    fun setResumePrompt(data: ResumePromptData?) {
        _playerState.update { it.copy(resumePrompt = data) }
    }

    fun clearResumePrompt() {
        _playerState.update { it.copy(resumePrompt = null) }
    }

    fun setVolumeWithBoost(percent: Int) {
        val clampedPercent = percent.coerceIn(0, 200)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val maxStreamVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15

        if (clampedPercent <= 100) {
            val targetStreamVol = kotlin.math.round((clampedPercent / 100f) * maxStreamVolume).toInt()
                .coerceIn(0, maxStreamVolume)
            try {
                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, targetStreamVol, 0)
            } catch (_: Exception) {}

            try {
                loudnessEnhancer?.setTargetGain(0)
                loudnessEnhancer?.enabled = false
            } catch (_: Exception) {}

            _playerState.update {
                it.copy(
                    volumePercent = clampedPercent,
                    isAudioBoostActive = false
                )
            }
        } else {
            // Full 100% Volume Boost (101% to 200% total volume, up to +20 dB amplification)
            try {
                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, maxStreamVolume, 0)
            } catch (_: Exception) {}

            val boostFraction = (clampedPercent - 100) / 100f // 0f..1f
            val gainMb = (boostFraction * 2000).toInt() // up to +2000 mB (+20 dB amplification)

            val audioSessionId = exoPlayer?.audioSessionId ?: C.AUDIO_SESSION_ID_UNSET
            if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                try {
                    if (loudnessEnhancer == null) {
                        loudnessEnhancer = LoudnessEnhancer(audioSessionId)
                    }
                    loudnessEnhancer?.setTargetGain(gainMb)
                    loudnessEnhancer?.enabled = true
                } catch (_: Exception) {}
            }

            _playerState.update {
                it.copy(
                    volumePercent = clampedPercent,
                    isAudioBoostActive = true
                )
            }
        }
    }

    fun cycleDecoderMode(): DecoderMode {
        val currentMode = _playerState.value.decoderMode
        val nextMode = when (currentMode) {
            DecoderMode.HW -> DecoderMode.HW_PLUS
            DecoderMode.HW_PLUS -> DecoderMode.SW
            DecoderMode.SW -> DecoderMode.HW
        }
        setDecoderMode(nextMode)
        return nextMode
    }

    fun setDecoderMode(newMode: DecoderMode) {
        val currentMode = _playerState.value.decoderMode
        if (currentMode == newMode) return

        val currentPos = exoPlayer?.currentPosition ?: _playerState.value.currentPosition
        val currentVid = _playerState.value.currentVideo
        val currentPlaylist = _playerState.value.playlist
        val currentIndex = _playerState.value.currentIndex
        val wasPlaying = exoPlayer?.isPlaying ?: _playerState.value.isPlaying
        val currentSpeed = _playerState.value.playbackSpeed
        val currentVolume = _playerState.value.volumePercent

        // Switching between HW and HW+ keeps the same hardware video codec!
        if ((currentMode == DecoderMode.HW && newMode == DecoderMode.HW_PLUS) ||
            (currentMode == DecoderMode.HW_PLUS && newMode == DecoderMode.HW)) {
            _playerState.update {
                it.copy(
                    decoderMode = newMode,
                    hardwareDecoding = true
                )
            }
            return
        }

        // Switching between SW and HW/HW+ safely re-initializes with the appropriate MediaCodecSelector
        try {
            exoPlayer?.pause()
        } catch (_: Exception) {}

        initPlayer(decoderMode = newMode)

        if (currentVid != null) {
            playVideo(
                video = currentVid,
                playlist = currentPlaylist,
                startIndex = currentIndex,
                resumePosition = currentPos
            )
            setPlaybackSpeed(currentSpeed)
            if (!wasPlaying) {
                exoPlayer?.pause()
            }
        }

        setVolumeWithBoost(currentVolume)
    }

    fun toggleHardwareDecoding() {
        cycleDecoderMode()
    }

    fun release() {
        progressJob?.cancel()
        loudnessEnhancer?.release()
        equalizer?.release()
        exoPlayer?.release()
        exoPlayer = null
    }
}
