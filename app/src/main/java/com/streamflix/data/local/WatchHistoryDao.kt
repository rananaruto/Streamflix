package com.streamflix.data.local

import androidx.room.*
import com.streamflix.data.model.WatchHistoryEntry
import kotlinx.coroutines.flow.Flow

/**
 * Watch History DAO - Manages watch history entries
 */
@Dao
interface WatchHistoryDao {

    @Query("SELECT * FROM watch_history ORDER BY watchedAt DESC")
    fun getAll(): Flow<List<WatchHistoryEntry>>

    @Query("SELECT * FROM watch_history WHERE mediaId = :mediaId")
    suspend fun getByMediaId(mediaId: String): WatchHistoryEntry?

    @Query("SELECT * FROM watch_history WHERE mediaId = :mediaId")
    fun getByMediaIdFlow(mediaId: String): Flow<WatchHistoryEntry?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WatchHistoryEntry)

    @Update
    suspend fun update(entry: WatchHistoryEntry)

    @Query("DELETE FROM watch_history WHERE mediaId = :mediaId")
    suspend fun deleteByMediaId(mediaId: String)

    @Query("DELETE FROM watch_history")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM watch_history")
    fun getCount(): Flow<Int>

    @Query("SELECT SUM(position) FROM watch_history")
    fun getTotalWatchTime(): Flow<Long?>
}
