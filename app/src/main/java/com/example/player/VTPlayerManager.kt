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
import androidx.media3.ui.PlayerView
import com.example.model.AspectRatioMode
import com.example.model.AudioDecoderMode
import com.example.model.DecoderMode
import com.example.model.EqualizerPreset
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
    private var pendingResumePosition: Long = 0L

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val isBuffering = playbackState == Player.STATE_BUFFERING
            val duration = exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L
            if (playbackState == Player.STATE_READY) {
                exoPlayer?.currentTracks?.let { updateAvailableTracks(it) }
                if (pendingResumePosition > 0L) {
                    val target = pendingResumePosition
                    pendingResumePosition = 0L
                    val cur = exoPlayer?.currentPosition ?: 0L
                    if (kotlin.math.abs(cur - target) > 800L) {
                        exoPlayer?.seekTo(target)
                    }
                }
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

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                lastUserSeekTimestamp = 0L
                _playerState.update { it.copy(currentPosition = newPosition.positionMs) }
            }
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
            val currentMode = _playerState.value.decoderMode
            val fallbackMode = if (currentMode == DecoderMode.SW) DecoderMode.HW else DecoderMode.SW
            coroutineScope.launch(Dispatchers.Main) {
                val curPos = _playerState.value.currentPosition
                val curVid = _playerState.value.currentVideo
                val curPlaylist = _playerState.value.playlist
                val curIdx = _playerState.value.currentIndex
                initPlayer(decoderMode = fallbackMode)
                attachedPlayerView?.player = exoPlayer
                if (curVid != null) {
                    playVideo(curVid, curPlaylist, curIdx, curPos)
                }
            }
        }
    }

    init {
        initPlayer(decoderMode = DecoderMode.SW, audioDecoderMode = AudioDecoderMode.AUTO)
    }

    fun initPlayer(useHardware: Boolean) {
        initPlayer(if (useHardware) DecoderMode.HW else DecoderMode.SW)
    }

    fun initPlayer(
        decoderMode: DecoderMode = _playerState.value.decoderMode,
        audioDecoderMode: AudioDecoderMode = _playerState.value.audioDecoderMode
    ) {
        attachedPlayerView?.player = null
        exoPlayer?.release()
        exoPlayer = null
        loudnessEnhancer?.release()
        loudnessEnhancer = null
        equalizer?.release()
        equalizer = null

        trackSelector = DefaultTrackSelector(context)

        val renderersFactory = DefaultRenderersFactory(context).apply {
            setEnableDecoderFallback(true)
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            // Zero joining time eliminates the 5-second video start delay in all modes (especially SW)
            setAllowedVideoJoiningTimeMs(0L)
            if (decoderMode == DecoderMode.HW_PLUS) {
                forceEnableMediaCodecAsynchronousQueueing()
            }
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

                if (mimeType.startsWith("video/", ignoreCase = true)) {
                    when (decoderMode) {
                        DecoderMode.SW -> {
                            // SW Decoder: Prioritize software decoding codecs (e.g. c2.android.*, OMX.google.*, softwareOnly)
                            val swList = defaultList.filter {
                                it.softwareOnly ||
                                it.name.startsWith("c2.android.", ignoreCase = true) ||
                                it.name.startsWith("OMX.google.", ignoreCase = true) ||
                                it.name.contains(".sw.", ignoreCase = true)
                            }
                            if (swList.isNotEmpty()) {
                                swList + defaultList.filterNot { swList.contains(it) }
                            } else {
                                defaultList
                            }
                        }
                        DecoderMode.HW, DecoderMode.HW_PLUS -> {
                            // HW & HW+ Decoder: Prioritize hardware accelerated MediaCodec decoders
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
                } else if (mimeType.startsWith("audio/", ignoreCase = true)) {
                    // Audio Decoder: Platform MediaCodec vs Software FFmpeg-compatible mode
                    if (audioDecoderMode == AudioDecoderMode.SW) {
                        val swAudioList = defaultList.filter {
                            it.softwareOnly ||
                            it.name.startsWith("c2.android.", ignoreCase = true) ||
                            it.name.startsWith("OMX.google.", ignoreCase = true) ||
                            it.name.contains(".sw.", ignoreCase = true)
                        }
                        if (swAudioList.isNotEmpty()) {
                            swAudioList + defaultList.filterNot { swAudioList.contains(it) }
                        } else {
                            defaultList
                        }
                    } else {
                        defaultList
                    }
                } else {
                    defaultList
                }
            }
        }

        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setUsage(C.USAGE_MEDIA)
            .setAllowedCapturePolicy(C.ALLOW_CAPTURE_BY_ALL)
            .build()

        // Responsive load control: start playback after 250ms buffering rather than waiting 2500ms
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 1500,
                /* maxBufferMs = */ 20000,
                /* bufferForPlaybackMs = */ 250,
                /* bufferForPlaybackAfterRebufferMs = */ 500
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        exoPlayer = ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setSeekForwardIncrementMs(10000)
            .setSeekBackIncrementMs(10000)
            .setUsePlatformDiagnostics(false)
            .build()
            .apply {
                playWhenReady = true
                setSeekParameters(androidx.media3.exoplayer.SeekParameters.CLOSEST_SYNC)
                addListener(playerListener)
            }

        _playerState.update {
            it.copy(
                decoderMode = decoderMode,
                audioDecoderMode = audioDecoderMode,
                hardwareDecoding = (decoderMode != DecoderMode.SW),
                playerSessionId = System.currentTimeMillis()
            )
        }
        attachedPlayerView?.player = exoPlayer
        startProgressTracking()
        applyEqualizerSettings()
    }

    private var attachedPlayerView: PlayerView? = null

    fun attachPlayerView(view: PlayerView?) {
        if (view != null) {
            attachedPlayerView = view
            view.player = exoPlayer
        }
    }

    fun detachPlayerView(view: PlayerView?) {
        if (view != null && attachedPlayerView == view) {
            attachedPlayerView = null
            view.player = null
        }
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

    private var lastUserSeekTimestamp: Long = 0L

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = coroutineScope.launch(Dispatchers.Main) {
            var saveCounter = 0
            while (isActive) {
                exoPlayer?.let { player ->
                    val dur = player.duration.coerceAtLeast(0L)
                    val state = _playerState.value

                    // Prevent temporary stale pre-seek position during buffering from reverting UI
                    val isSeekingGracePeriod = (System.currentTimeMillis() - lastUserSeekTimestamp) < 600L &&
                            (player.playbackState == Player.STATE_BUFFERING || player.isLoading)
                    val pos = if (isSeekingGracePeriod) {
                        state.currentPosition
                    } else {
                        player.currentPosition.coerceAtLeast(0L)
                    }

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

        pendingResumePosition = resumePosition
        // setMediaItems already starts at startIndex and resumePosition; no duplicate seek needed
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

        startProgressTracking()
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun stop() {
        progressJob?.cancel()
        pendingResumePosition = 0L
        val player = exoPlayer ?: return
        player.pause()
        player.stop()
        _playerState.update { it.copy(isPlaying = false) }
    }

    fun seekTo(positionMs: Long) {
        val totalDur = if ((exoPlayer?.duration ?: 0L) > 0L) exoPlayer!!.duration else _playerState.value.duration.coerceAtLeast(0L)
        val target = positionMs.coerceIn(0L, totalDur)
        lastUserSeekTimestamp = System.currentTimeMillis()
        _playerState.update { it.copy(currentPosition = target) }
        exoPlayer?.seekTo(target)
    }

    fun seekBy(deltaMs: Long) {
        val player = exoPlayer ?: return
        val totalDur = if (player.duration > 0) player.duration else _playerState.value.duration.coerceAtLeast(0L)
        val newPos = (player.currentPosition + deltaMs).coerceIn(0L, totalDur)
        lastUserSeekTimestamp = System.currentTimeMillis()
        _playerState.update { it.copy(currentPosition = newPos) }
        player.seekTo(newPos)
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

    fun setPlaybackSpeed(
        speed: Float,
        preservePitch: Boolean = _playerState.value.preservePitch,
        pitchFactor: Float = _playerState.value.audioPitch
    ) {
        val clampedSpeed = speed.coerceIn(0.25f, 4.0f)
        val actualPitch = if (preservePitch) 1.0f else (clampedSpeed * pitchFactor).coerceIn(0.5f, 2.0f)
        try {
            exoPlayer?.playbackParameters = PlaybackParameters(clampedSpeed, actualPitch)
        } catch (_: Exception) {}
        _playerState.update {
            it.copy(
                playbackSpeed = clampedSpeed,
                preservePitch = preservePitch,
                audioPitch = pitchFactor
            )
        }
    }

    fun setPreservePitch(preserve: Boolean) {
        setPlaybackSpeed(_playerState.value.playbackSpeed, preservePitch = preserve)
    }

    fun setAudioPitch(pitch: Float) {
        val clampedPitch = pitch.coerceIn(0.5f, 2.0f)
        setPlaybackSpeed(_playerState.value.playbackSpeed, preservePitch = false, pitchFactor = clampedPitch)
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

    // Audio boost toggle (Exclusive to HW+ mode)
    fun toggleAudioBoost() {
        val isHwPlus = _playerState.value.decoderMode == DecoderMode.HW_PLUS
        if (!isHwPlus) return
        val currentVol = _playerState.value.volumePercent
        if (currentVol <= 100) {
            setVolumeWithBoost(150)
        } else {
            setVolumeWithBoost(100)
        }
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
    fun setShowDecoderDialog(show: Boolean) = _playerState.update { it.copy(showDecoderDialog = show) }
    fun setShowAudioSettingsDialog(show: Boolean) = _playerState.update { it.copy(showAudioSettingsDialog = show) }
    fun setShowMoreMenu(show: Boolean) = _playerState.update { it.copy(showMoreMenu = show) }

    fun setResumePrompt(data: ResumePromptData?) {
        _playerState.update { it.copy(resumePrompt = data) }
    }

    fun clearResumePrompt() {
        _playerState.update { it.copy(resumePrompt = null) }
    }

    // Audio Decoder selection (System MediaCodec vs Software FFmpeg mode)
    fun setAudioDecoderMode(mode: AudioDecoderMode) {
        if (_playerState.value.audioDecoderMode == mode) return
        val curPos = if (exoPlayer != null && (exoPlayer?.currentPosition ?: 0L) > 0L) exoPlayer!!.currentPosition else _playerState.value.currentPosition
        val curVid = _playerState.value.currentVideo
        val curPlaylist = _playerState.value.playlist
        val curIdx = exoPlayer?.currentMediaItemIndex ?: _playerState.value.currentIndex
        val wasPlaying = exoPlayer?.isPlaying ?: _playerState.value.isPlaying
        val curSpeed = _playerState.value.playbackSpeed

        try {
            exoPlayer?.stop()
            exoPlayer?.clearMediaItems()
        } catch (_: Exception) {}

        initPlayer(decoderMode = _playerState.value.decoderMode, audioDecoderMode = mode)
        attachedPlayerView?.player = exoPlayer

        if (curVid != null) {
            playVideo(curVid, curPlaylist, curIdx, curPos.coerceAtLeast(0L))
            setPlaybackSpeed(curSpeed)
            if (!wasPlaying) exoPlayer?.pause()
        }
        setVolumeWithBoost(_playerState.value.volumePercent)
    }

    // Audio Passthrough for AC3/E-AC3/DTS compatible external receivers
    fun setAudioPassthrough(enabled: Boolean) {
        _playerState.update { it.copy(audioPassthroughEnabled = enabled) }
        try {
            // Apply track selection parameters for direct passthrough
            val mimeTypes = if (enabled) {
                arrayOf("audio/ac3", "audio/eac3", "audio/vnd.dts", "audio/true-hd")
            } else {
                emptyArray<String>()
            }
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .setPreferredAudioMimeTypes(*mimeTypes)
            )
        } catch (_: Exception) {}
    }

    // Left/Right Balance: -1.0 (Full Left) to +1.0 (Full Right), 0.0 (Center)
    fun setLeftRightBalance(balance: Float) {
        val clamped = balance.coerceIn(-1.0f, 1.0f)
        _playerState.update { it.copy(leftRightBalance = clamped) }
        val left = if (clamped > 0f) 1f - clamped else 1f
        val right = if (clamped < 0f) 1f + clamped else 1f
        val avgVolume = ((left + right) / 2f).coerceIn(0.1f, 1.0f)
        try {
            exoPlayer?.volume = avgVolume
        } catch (_: Exception) {}
    }

    // Multi-band Equalizer
    fun setEqualizerPreset(preset: EqualizerPreset) {
        val bands = when (preset) {
            EqualizerPreset.FLAT -> listOf(0, 0, 0, 0, 0)
            EqualizerPreset.BASS_BOOST -> listOf(8, 4, 0, 0, 0)
            EqualizerPreset.VOCAL -> listOf(-2, 2, 6, 4, 1)
            EqualizerPreset.ROCK -> listOf(6, 3, -1, 4, 6)
            EqualizerPreset.POP -> listOf(4, 3, 2, 4, 3)
            EqualizerPreset.CLASSICAL -> listOf(5, 3, 1, 3, 5)
            EqualizerPreset.CUSTOM -> _playerState.value.equalizerBands
        }
        _playerState.update {
            it.copy(
                equalizerPreset = preset,
                equalizerBands = bands
            )
        }
        applyEqualizerSettings()
    }

    fun setEqualizerBand(bandIndex: Int, levelDb: Int) {
        val currentBands = _playerState.value.equalizerBands.toMutableList()
        if (bandIndex in currentBands.indices) {
            currentBands[bandIndex] = levelDb.coerceIn(-12, 12)
            _playerState.update {
                it.copy(
                    equalizerPreset = EqualizerPreset.CUSTOM,
                    equalizerBands = currentBands
                )
            }
            applyEqualizerSettings()
        }
    }

    private fun applyEqualizerSettings() {
        val audioSessionId = exoPlayer?.audioSessionId ?: C.AUDIO_SESSION_ID_UNSET
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) return
        try {
            if (equalizer == null) {
                equalizer = Equalizer(0, audioSessionId).apply { enabled = true }
            }
            val eq = equalizer ?: return
            val numBands = eq.numberOfBands.toInt()
            val bands = _playerState.value.equalizerBands
            for (i in 0 until minOf(numBands, bands.size)) {
                val levelMb = (bands[i] * 100).toShort().coerceIn(eq.bandLevelRange[0], eq.bandLevelRange[1])
                eq.setBandLevel(i.toShort(), levelMb)
            }
        } catch (_: Exception) {}
    }

    // Volume & Boost: 0% – 100% for SW and HW; 0% – 150% for HW+ (Audio Boost up to +20 dB)
    fun setVolumeWithBoost(percent: Int) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val maxStreamVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
        val isHwPlus = _playerState.value.decoderMode == DecoderMode.HW_PLUS
        val maxAllowed = if (isHwPlus) 150 else 100
        val clampedPercent = percent.coerceIn(0, maxAllowed)

        if (clampedPercent <= 100) {
            // Standard volume range (0% - 100%): Device music volume stream
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
            // HW+ Volume Boost range (101% - 150%): Maximize standard stream volume & drive Loudness Enhancer up to +20 dB
            try {
                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, maxStreamVolume, 0)
            } catch (_: Exception) {}

            val boostFraction = (clampedPercent - 100) / 50f // 0.02f .. 1.0f
            val gainMb = (boostFraction * 2000).toInt().coerceIn(100, 2000)

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
        // Requested order sequence: SW -> HW -> HW+
        val nextMode = when (currentMode) {
            DecoderMode.SW -> DecoderMode.HW
            DecoderMode.HW -> DecoderMode.HW_PLUS
            DecoderMode.HW_PLUS -> DecoderMode.SW
        }
        setDecoderMode(nextMode)
        return nextMode
    }

    fun setDecoderMode(newMode: DecoderMode) {
        val currentMode = _playerState.value.decoderMode
        if (currentMode == newMode) return

        // Switching between HW and HW+ uses the same hardware video codec! Seamless instant switch.
        if ((currentMode == DecoderMode.HW && newMode == DecoderMode.HW_PLUS) ||
            (currentMode == DecoderMode.HW_PLUS && newMode == DecoderMode.HW)) {
            _playerState.update {
                it.copy(
                    decoderMode = newMode,
                    hardwareDecoding = true
                )
            }
            // If switching from HW+ back to HW, enforce 100% audio limit immediately
            if (newMode == DecoderMode.HW && _playerState.value.volumePercent > 100) {
                setVolumeWithBoost(100)
            }
            return
        }

        val currentPos = if (exoPlayer != null && (exoPlayer?.currentPosition ?: 0L) > 0L) {
            exoPlayer!!.currentPosition
        } else {
            _playerState.value.currentPosition
        }
        val currentVid = _playerState.value.currentVideo
        val currentPlaylist = _playerState.value.playlist
        val currentIndex = exoPlayer?.currentMediaItemIndex ?: _playerState.value.currentIndex
        val wasPlaying = exoPlayer?.isPlaying ?: _playerState.value.isPlaying
        val currentSpeed = _playerState.value.playbackSpeed
        val targetVolume = if (newMode == DecoderMode.HW_PLUS) {
            _playerState.value.volumePercent
        } else {
            _playerState.value.volumePercent.coerceAtMost(100)
        }

        // Switching between SW and HW/HW+ safely re-initializes and reconnects the surface
        try {
            exoPlayer?.stop()
            exoPlayer?.clearMediaItems()
        } catch (_: Exception) {}

        initPlayer(decoderMode = newMode, audioDecoderMode = _playerState.value.audioDecoderMode)
        attachedPlayerView?.player = exoPlayer

        if (currentVid != null) {
            playVideo(
                video = currentVid,
                playlist = currentPlaylist,
                startIndex = currentIndex,
                resumePosition = currentPos.coerceAtLeast(0L)
            )
            setPlaybackSpeed(currentSpeed)
            if (!wasPlaying) {
                exoPlayer?.pause()
            }
        }

        setVolumeWithBoost(targetVolume)
    }

    fun toggleHardwareDecoding() {
        cycleDecoderMode()
    }

    fun release() {
        progressJob?.cancel()
        loudnessEnhancer?.release()
        equalizer?.release()
        attachedPlayerView?.player = null
        attachedPlayerView = null
        exoPlayer?.release()
        exoPlayer = null
    }
}
