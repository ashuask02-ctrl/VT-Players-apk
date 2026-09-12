package com.example.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.model.FolderItem
import com.example.model.StreamPreset
import com.example.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class VTRepository(
    private val context: Context,
    private val database: VTDatabase
) {
    private val recordDao = database.playbackRecordDao()
    private val playlistDao = database.playlistDao()
    private val prefsDao = database.preferencesDao()

    val preferencesFlow: Flow<PlayerPreferencesEntity?> = prefsDao.getPreferencesFlow()
    val playlistsFlow: Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()
    val recentRecordsFlow: Flow<List<PlaybackRecordEntity>> = recordDao.getRecentVideos()
    val favoriteRecordsFlow: Flow<List<PlaybackRecordEntity>> = recordDao.getFavoriteVideos()
    val hiddenRecordsFlow: Flow<List<PlaybackRecordEntity>> = recordDao.getHiddenVideos()

    // Default sample streams for immediate testing & network player demonstration
    val sampleStreams = listOf(
        StreamPreset(
            title = "Big Buck Bunny (HLS 1080p)",
            url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            description = "Open source animation - HLS multi-bitrate stream with audio tracks",
            tag = "HLS Stream"
        ),
        StreamPreset(
            title = "Tears of Steel (Sci-Fi 4K)",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            description = "High definition CGI sci-fi short film (MP4 format)",
            tag = "MP4 Video"
        ),
        StreamPreset(
            title = "Elephants Dream (Action CGI)",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            description = "Rich multi-channel audio testing video",
            tag = "Multi Audio"
        ),
        StreamPreset(
            title = "Sintel Movie (Fantasy)",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
            description = "Cinematic fantasy short with dialogue for subtitle testing",
            tag = "Cinema"
        ),
        StreamPreset(
            title = "For Bigger Blazes (Action Demo)",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            description = "Fast motion sports action for gesture and frame-rate tests",
            tag = "Demo Clip"
        ),
        StreamPreset(
            title = "We Are Going On Bullrun (Automotive)",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
            description = "High contrast rally car footage",
            tag = "Action"
        )
    )

    suspend fun getPreferences(): PlayerPreferencesEntity {
        return prefsDao.getPreferences() ?: PlayerPreferencesEntity().also {
            prefsDao.savePreferences(it)
        }
    }

    suspend fun savePreferences(prefs: PlayerPreferencesEntity) {
        prefsDao.savePreferences(prefs)
    }

    suspend fun recordPlayback(
        uri: String,
        title: String,
        duration: Long,
        position: Long,
        folderName: String
    ) {
        withContext(Dispatchers.IO) {
            val existing = recordDao.getRecordByUri(uri)
            if (existing != null) {
                recordDao.updatePlaybackPosition(uri, position, System.currentTimeMillis())
            } else {
                recordDao.insertOrUpdate(
                    PlaybackRecordEntity(
                        uri = uri,
                        title = title,
                        duration = duration,
                        lastPosition = position,
                        lastPlayedTimestamp = System.currentTimeMillis(),
                        folderName = folderName
                    )
                )
            }
        }
    }

    suspend fun toggleFavorite(uri: String, title: String, isFav: Boolean) {
        withContext(Dispatchers.IO) {
            val existing = recordDao.getRecordByUri(uri)
            if (existing != null) {
                recordDao.setFavorite(uri, isFav)
            } else {
                recordDao.insertOrUpdate(
                    PlaybackRecordEntity(
                        uri = uri,
                        title = title,
                        isFavorite = isFav
                    )
                )
            }
        }
    }

    suspend fun setVideoHidden(uri: String, title: String, isHidden: Boolean) {
        withContext(Dispatchers.IO) {
            val existing = recordDao.getRecordByUri(uri)
            if (existing != null) {
                recordDao.setHidden(uri, isHidden)
            } else {
                recordDao.insertOrUpdate(
                    PlaybackRecordEntity(
                        uri = uri,
                        title = title,
                        isHidden = isHidden
                    )
                )
            }
        }
    }

    suspend fun clearHistory() {
        withContext(Dispatchers.IO) {
            recordDao.clearAllHistory()
        }
    }

    suspend fun createPlaylist(name: String): Long {
        return withContext(Dispatchers.IO) {
            playlistDao.createPlaylist(PlaylistEntity(name = name))
        }
    }

    suspend fun addVideoToPlaylist(playlistId: Long, video: VideoItem) {
        withContext(Dispatchers.IO) {
            playlistDao.addItemToPlaylist(
                PlaylistItemEntity(
                    playlistId = playlistId,
                    videoUri = video.uriString,
                    title = video.title,
                    duration = video.duration
                )
            )
        }
    }

    fun getPlaylistItems(playlistId: Long): Flow<List<PlaylistItemEntity>> {
        return playlistDao.getItemsForPlaylist(playlistId)
    }

    /**
     * Scans local videos from device MediaStore and blends with sample streams if empty.
     */
    fun scanDeviceVideos(): Flow<List<VideoItem>> = flow {
        val list = mutableListOf<VideoItem>()
        try {
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_MODIFIED,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Video.Media.RELATIVE_PATH else MediaStore.Video.Media.DATA
            )

            val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val titleCol = cursor.getColumnIndex(MediaStore.Video.Media.TITLE)
                val nameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val durationCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                val dateCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
                val pathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
                } else {
                    cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                }

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    val title = (if (titleCol >= 0) cursor.getString(titleCol) else null)
                        ?: (if (nameCol >= 0) cursor.getString(nameCol) else null)
                        ?: "Video #$id"
                    val duration = if (durationCol >= 0) cursor.getLong(durationCol) else 0L
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    val dateModified = if (dateCol >= 0) cursor.getLong(dateCol) * 1000L else System.currentTimeMillis()
                    val rawPath = if (pathCol >= 0) cursor.getString(pathCol) ?: "Internal" else "Internal"
                    val folderName = rawPath.trim('/').split('/').lastOrNull()?.ifBlank { "Videos" } ?: "Videos"

                    list.add(
                        VideoItem(
                            id = id,
                            title = title,
                            uriString = contentUri.toString(),
                            duration = duration,
                            size = size,
                            folderName = folderName,
                            dateModified = dateModified
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Permission not granted or query failure
        }

        // Always include curated high-contrast demo videos so the player is fully usable immediately
        sampleStreams.forEachIndexed { index, preset ->
            list.add(
                VideoItem(
                    id = -(index + 100L),
                    title = preset.title,
                    uriString = preset.url,
                    duration = 596000L,
                    size = 45000000L,
                    folderName = "Featured Streams",
                    resolution = "1080p FHD",
                    dateModified = System.currentTimeMillis() - (index * 86400000L)
                )
            )
        }

        emit(list)
    }.flowOn(Dispatchers.IO)
}
