package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "playback_records",
    indices = [Index(value = ["uri"], unique = true)]
)
data class PlaybackRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val title: String,
    val duration: Long = 0L,
    val lastPosition: Long = 0L,
    val lastPlayedTimestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val folderName: String = "Videos"
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_items",
    indices = [Index(value = ["playlistId"])]
)
data class PlaylistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val videoUri: String,
    val title: String,
    val duration: Long = 0L
)

@Entity(tableName = "player_preferences")
data class PlayerPreferencesEntity(
    @PrimaryKey val id: Int = 1,
    val defaultSpeed: Float = 1.0f,
    val resumePlayback: Boolean = true,
    val backgroundPlayback: Boolean = false,
    val useHardwareDecoder: Boolean = true,
    val doubleTapAction: String = "SEEK", // "SEEK" or "PLAY_PAUSE"
    val brightnessGesture: Boolean = true,
    val volumeGesture: Boolean = true,
    val swipeSeekGesture: Boolean = true,
    val subtitleSize: Float = 18f,
    val subtitleColor: Long = 0xFFFFFFFF,
    val subtitleBgColor: Long = 0xAA000000,
    val audioDelayMs: Long = 0L,
    val subtitleDelayMs: Long = 0L,
    val audioBoost: Boolean = false,
    val pinCode: String = "",
    val themeMode: String = "SYSTEM", // "SYSTEM", "DARK", "LIGHT"
    val language: String = "EN"
)
