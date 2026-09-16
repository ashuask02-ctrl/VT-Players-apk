package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.example.VTPlayerApplication
import com.example.data.PlaybackRecordEntity
import com.example.data.PlayerPreferencesEntity
import com.example.data.PlaylistEntity
import com.example.data.VTRepository
import com.example.model.DecoderMode
import com.example.model.FolderItem
import com.example.model.SortOrder
import com.example.model.StreamPreset
import com.example.model.VideoItem
import com.example.player.VTPlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MainTab {
    VIDEOS,
    FOLDERS,
    PLAYLISTS,
    STREAM,
    VAULT,
    SETTINGS
}

data class VaultState(
    val isUnlocked: Boolean = false,
    val hasPinSet: Boolean = false,
    val pinError: String? = null
)

@OptIn(UnstableApi::class)
class VTViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VTRepository = VTRepository(
        application,
        (application as VTPlayerApplication).database
    )

    val playerManager = VTPlayerManager(application, viewModelScope)

    // Current navigation tab
    private val _currentTab = MutableStateFlow(MainTab.VIDEOS)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    // Video playing state - when non-null, Player Screen or Mini Player is active
    private val _activePlayingVideo = MutableStateFlow<VideoItem?>(null)
    val activePlayingVideo: StateFlow<VideoItem?> = _activePlayingVideo.asStateFlow()

    // Fast in-memory cache for exact stopped positions to guarantee instantaneous resume
    private val _lastStoppedPositions = java.util.concurrent.ConcurrentHashMap<String, Long>()

    // Mini Player Mode
    private val _isMiniPlayer = MutableStateFlow(false)
    val isMiniPlayer: StateFlow<Boolean> = _isMiniPlayer.asStateFlow()

    fun minimizeToMiniPlayer() {
        if (_activePlayingVideo.value != null) {
            _isMiniPlayer.value = true
        }
    }

    fun expandToFullScreen() {
        if (_activePlayingVideo.value != null) {
            _isMiniPlayer.value = false
        }
    }

    fun dismissMiniPlayer() {
        closePlayer(forcePause = true)
    }

    // Search & Sorting
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _selectedFolder = MutableStateFlow<String?>(null)
    val selectedFolder: StateFlow<String?> = _selectedFolder.asStateFlow()

    // Raw scanned videos
    private val _scannedVideos = MutableStateFlow<List<VideoItem>>(emptyList())

    // Database Flows
    val preferences: StateFlow<PlayerPreferencesEntity> = repository.preferencesFlow
        .combine(MutableStateFlow(Unit)) { prefs, _ ->
            prefs ?: PlayerPreferencesEntity()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, PlayerPreferencesEntity())

    val recentRecords: StateFlow<List<PlaybackRecordEntity>> = repository.recentRecordsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteRecords: StateFlow<List<PlaybackRecordEntity>> = repository.favoriteRecordsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hiddenRecords: StateFlow<List<PlaybackRecordEntity>> = repository.hiddenRecordsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistEntity>> = repository.playlistsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Vault State
    private val _vaultState = MutableStateFlow(VaultState())
    val vaultState: StateFlow<VaultState> = _vaultState.asStateFlow()

    // Filter Params helper
    private data class FilterParams(
        val query: String,
        val sort: SortOrder,
        val folder: String?
    )

    private val filterParamsFlow = combine(_searchQuery, _sortOrder, _selectedFolder) { q, s, f ->
        FilterParams(q, s, f)
    }

    // Filtered & Sorted Videos Flow
    val filteredVideos: StateFlow<List<VideoItem>> = combine(
        _scannedVideos,
        hiddenRecords,
        favoriteRecords,
        recentRecords,
        filterParamsFlow
    ) { rawList, hiddenList, favList, recList, params ->
        val hiddenUris = hiddenList.map { it.uri }.toSet()
        val favUris = favList.map { it.uri }.toSet()
        val posMap = recList.associate { it.uri to it.lastPosition }

        var list = rawList.filter { !hiddenUris.contains(it.uriString) }

        if (params.folder != null) {
            list = list.filter { it.folderName == params.folder }
        }

        if (params.query.isNotBlank()) {
            list = list.filter { it.title.contains(params.query.trim(), ignoreCase = true) }
        }

        val mapped = list.map { item ->
            item.copy(
                isFavorite = favUris.contains(item.uriString),
                lastPosition = posMap[item.uriString] ?: 0L
            )
        }

        when (params.sort) {
            SortOrder.DATE_DESC -> mapped.sortedByDescending { it.dateModified }
            SortOrder.DATE_ASC -> mapped.sortedBy { it.dateModified }
            SortOrder.NAME_ASC -> mapped.sortedBy { it.title.lowercase() }
            SortOrder.NAME_DESC -> mapped.sortedByDescending { it.title.lowercase() }
            SortOrder.SIZE_DESC -> mapped.sortedByDescending { it.size }
            SortOrder.DURATION_DESC -> mapped.sortedByDescending { it.duration }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Folders grouping
    val folderItems: StateFlow<List<FolderItem>> = combine(
        _scannedVideos,
        hiddenRecords
    ) { rawList, hiddenList ->
        val hiddenUris = hiddenList.map { it.uri }.toSet()
        val visibleVideos = rawList.filter { !hiddenUris.contains(it.uriString) }

        visibleVideos.groupBy { it.folderName }
            .map { (folderName, videos) ->
                FolderItem(
                    name = folderName,
                    videoCount = videos.size,
                    totalDuration = videos.sumOf { it.duration },
                    sampleVideo = videos.firstOrNull()
                )
            }.sortedByDescending { it.videoCount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshVideos()
        checkVaultPinStatus()

        // Auto-save playback position to database periodically during playback
        playerManager.onPositionSaved = { uri, pos, dur ->
            val vid = _activePlayingVideo.value
            if (vid != null && vid.uriString == uri) {
                viewModelScope.launch(Dispatchers.IO) {
                    repository.recordPlayback(
                        uri = uri,
                        title = vid.title,
                        duration = dur,
                        position = pos,
                        folderName = vid.folderName
                    )
                }
            }
        }
    }

    fun refreshVideos() {
        viewModelScope.launch {
            repository.scanDeviceVideos().collect { scanned ->
                _scannedVideos.value = scanned
            }
        }
    }

    // Featured streams for the top Three-line (≡) menu
    val featuredStreams: List<StreamPreset>
        get() = repository.sampleStreams

    fun playFeaturedStream(preset: StreamPreset) {
        val video = VideoItem(
            id = preset.url.hashCode().toLong(),
            title = preset.title,
            uriString = preset.url,
            duration = 596000L,
            size = 45000000L,
            folderName = "Featured Streams",
            resolution = preset.tag,
            dateModified = System.currentTimeMillis()
        )
        playVideo(video, listOf(video))
    }

    private fun checkVaultPinStatus() {
        viewModelScope.launch {
            val prefs = repository.getPreferences()
            _vaultState.update { it.copy(hasPinSet = prefs.pinCode.isNotBlank()) }
        }
    }

    fun setTab(tab: MainTab) {
        _currentTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun setSelectedFolder(folder: String?) {
        _selectedFolder.value = folder
    }

    // Playback actions
    fun playVideo(video: VideoItem, playlist: List<VideoItem> = listOf(video), externalSubUri: Uri? = null) {
        _activePlayingVideo.value = video
        _isMiniPlayer.value = false
        viewModelScope.launch {
            val prefs = repository.getPreferences()
            val preferredDecoder = when (prefs.defaultDecoderMode) {
                "HW" -> DecoderMode.HW
                "HW_PLUS" -> DecoderMode.HW_PLUS
                else -> DecoderMode.SW
            }
            if (playerManager.playerState.value.decoderMode != preferredDecoder) {
                playerManager.setDecoderMode(preferredDecoder)
            }

            val inMemoryPos = _lastStoppedPositions[video.uriString]
            val record = repository.getRecordByUri(video.uriString)
            val savedPos = inMemoryPos ?: record?.lastPosition ?: video.lastPosition
            val totalDur = if ((record?.duration ?: 0L) > 0) record!!.duration else video.duration
            val startIdx = playlist.indexOfFirst { it.uriString == video.uriString }.coerceAtLeast(0)

            // Has user previously watched more than 1 second, and not already within 3 seconds of end?
            val hasResumePoint = savedPos > 1000L && (totalDur <= 0L || savedPos < (totalDur - 3000L))
            val resumePos = if (hasResumePoint) savedPos else 0L

            playerManager.playVideo(
                video = video,
                playlist = if (playlist.isEmpty()) listOf(video) else playlist,
                startIndex = startIdx,
                resumePosition = resumePos,
                externalSubtitleUri = externalSubUri
            )

            playerManager.setPlaybackSpeed(prefs.defaultSpeed)

            if (hasResumePoint) {
                playerManager.setResumePrompt(
                    com.example.player.ResumePromptData(
                        video = video,
                        savedPositionMs = savedPos,
                        totalDurationMs = totalDur
                    )
                )
            } else {
                playerManager.clearResumePrompt()
            }
        }
    }

    fun clearSavedPosition(uri: String) {
        _lastStoppedPositions.remove(uri)
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getRecordByUri(uri)
            if (existing != null) {
                repository.recordPlayback(
                    uri = uri,
                    title = existing.title,
                    duration = existing.duration,
                    position = 0L,
                    folderName = existing.folderName
                )
            }
        }
    }

    fun playNetworkStream(url: String, title: String = "Streaming Media") {
        val streamItem = VideoItem(
            id = System.currentTimeMillis(),
            title = title,
            uriString = url,
            folderName = "Network Stream",
            resolution = "Live / Stream"
        )
        playVideo(streamItem, listOf(streamItem))
    }

    fun closePlayer(forcePause: Boolean = true) {
        // Save current playback position
        val state = playerManager.playerState.value
        val current = state.currentVideo
        val exo = playerManager.getExoPlayer()
        val exactPos = exo?.currentPosition?.takeIf { it > 0L } ?: state.currentPosition
        val exactDur = exo?.duration?.takeIf { it > 0L } ?: state.duration

        if (current != null && exactPos > 0L) {
            _lastStoppedPositions[current.uriString] = exactPos
            viewModelScope.launch(Dispatchers.IO) {
                repository.recordPlayback(
                    uri = current.uriString,
                    title = current.title,
                    duration = exactDur,
                    position = exactPos,
                    folderName = current.folderName
                )
            }
        }

        // Always pause and stop playback immediately when Player Screen or Mini Player is closed
        playerManager.stop()
        _isMiniPlayer.value = false
        _activePlayingVideo.value = null
    }

    fun toggleFavorite(video: VideoItem) {
        viewModelScope.launch {
            val newFav = !video.isFavorite
            repository.toggleFavorite(video.uriString, video.title, newFav)
        }
    }

    fun hideVideo(video: VideoItem) {
        viewModelScope.launch {
            repository.setVideoHidden(video.uriString, video.title, true)
        }
    }

    fun unhideVideo(uri: String, title: String) {
        viewModelScope.launch {
            repository.setVideoHidden(uri, title, false)
        }
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createPlaylist(name.trim())
        }
    }

    fun addVideoToPlaylist(playlistId: Long, video: VideoItem) {
        viewModelScope.launch {
            repository.addVideoToPlaylist(playlistId, video)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // Vault / PIN Security
    fun setupVaultPin(pin: String) {
        viewModelScope.launch {
            val prefs = repository.getPreferences()
            repository.savePreferences(prefs.copy(pinCode = pin))
            _vaultState.update {
                it.copy(
                    hasPinSet = true,
                    isUnlocked = true,
                    pinError = null
                )
            }
        }
    }

    fun unlockVault(enteredPin: String) {
        viewModelScope.launch {
            val prefs = repository.getPreferences()
            if (prefs.pinCode == enteredPin) {
                _vaultState.update { it.copy(isUnlocked = true, pinError = null) }
            } else {
                _vaultState.update { it.copy(pinError = "Incorrect PIN") }
            }
        }
    }

    fun lockVault() {
        _vaultState.update { it.copy(isUnlocked = false, pinError = null) }
    }

    // Settings
    fun updatePreferences(updateBlock: (PlayerPreferencesEntity) -> PlayerPreferencesEntity) {
        viewModelScope.launch {
            val current = repository.getPreferences()
            val updated = updateBlock(current)
            repository.savePreferences(updated)
            // Apply decoder mode if changed
            if (updated.defaultDecoderMode != current.defaultDecoderMode) {
                val mode = when (updated.defaultDecoderMode) {
                    "HW" -> DecoderMode.HW
                    "HW_PLUS" -> DecoderMode.HW_PLUS
                    else -> DecoderMode.SW
                }
                playerManager.setDecoderMode(mode)
            } else if (updated.useHardwareDecoder != current.useHardwareDecoder) {
                val mode = if (updated.useHardwareDecoder) DecoderMode.HW else DecoderMode.SW
                playerManager.setDecoderMode(mode)
            }
        }
    }

    fun setFontSize(size: String) {
        updatePreferences { it.copy(fontSize = size) }
    }

    fun setHideStatusBarInPlayer(hide: Boolean) {
        updatePreferences { it.copy(hideStatusBarInPlayer = hide) }
    }

    fun setMovieOrientation(orientation: String) {
        updatePreferences { it.copy(movieOrientation = orientation) }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}

class VTViewModelFactory(
    private val application: Application
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VTViewModel::class.java)) {
            return VTViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

