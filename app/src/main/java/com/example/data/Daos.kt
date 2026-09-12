package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackRecordDao {
    @Query("SELECT * FROM playback_records WHERE isHidden = 0 ORDER BY lastPlayedTimestamp DESC")
    fun getRecentVideos(): Flow<List<PlaybackRecordEntity>>

    @Query("SELECT * FROM playback_records WHERE isFavorite = 1 AND isHidden = 0 ORDER BY lastPlayedTimestamp DESC")
    fun getFavoriteVideos(): Flow<List<PlaybackRecordEntity>>

    @Query("SELECT * FROM playback_records WHERE isHidden = 1 ORDER BY lastPlayedTimestamp DESC")
    fun getHiddenVideos(): Flow<List<PlaybackRecordEntity>>

    @Query("SELECT * FROM playback_records WHERE uri = :uri LIMIT 1")
    suspend fun getRecordByUri(uri: String): PlaybackRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: PlaybackRecordEntity): Long

    @Query("UPDATE playback_records SET lastPosition = :position, lastPlayedTimestamp = :timestamp WHERE uri = :uri")
    suspend fun updatePlaybackPosition(uri: String, position: Long, timestamp: Long)

    @Query("UPDATE playback_records SET isFavorite = :isFavorite WHERE uri = :uri")
    suspend fun setFavorite(uri: String, isFavorite: Boolean)

    @Query("UPDATE playback_records SET isHidden = :isHidden WHERE uri = :uri")
    suspend fun setHidden(uri: String, isHidden: Boolean)

    @Query("DELETE FROM playback_records WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)

    @Query("DELETE FROM playback_records")
    suspend fun clearAllHistory()
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createPlaylist(playlist: PlaylistEntity): Long

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY id ASC")
    fun getItemsForPlaylist(playlistId: Long): Flow<List<PlaylistItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addItemToPlaylist(item: PlaylistItemEntity): Long

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND videoUri = :videoUri")
    suspend fun removeItemFromPlaylist(playlistId: Long, videoUri: String)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun clearPlaylistItems(playlistId: Long)
}

@Dao
interface PlayerPreferencesDao {
    @Query("SELECT * FROM player_preferences WHERE id = 1 LIMIT 1")
    fun getPreferencesFlow(): Flow<PlayerPreferencesEntity?>

    @Query("SELECT * FROM player_preferences WHERE id = 1 LIMIT 1")
    suspend fun getPreferences(): PlayerPreferencesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePreferences(prefs: PlayerPreferencesEntity)
}
